/**
 * Copyright (c) 2026 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 *
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 *
 *      https://opensource.org/licenses/MIT
 *
 * SPDX-License-Identifier: MIT
 *
 * Contributors:
 *      Kristoffer Paulsson - initial implementation
 */
package org.angproj.io.sel.server

import jnr.constants.platform.Errno
import org.angproj.io.sel.*
import org.angproj.io.sel.channel.ChannelSelectionKey
import org.angproj.io.sel.channel.SelectChannelOperation
import org.angproj.io.sel.driver.Dispenser
import org.angproj.io.sel.driver.task
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


public class PollSelector: AbstractSelector() {

    /* ---- NATIVE START ---- */

    private val pipefd = intArrayOf(-1, -1)
    private var pollData: ByteBuffer
    private var nfds = 0

    private val wakeBuf = ByteBuffer.allocate(1)

    private val idx = Dispenser(object {
        val keyIndex: ArrayList<ChannelSelectionKey<*>> = arrayListOf()
        val inverseKeyIndex: MutableMap<Int, ChannelSelectionKey<*>> = mutableMapOf()
    })

    private val fds = Dispenser(object {
        val keyFD: MutableMap<ChannelSelectionKey<*>, Int> = mutableMapOf()
        val inverseKeyFD: MutableMap<Int, ChannelSelectionKey<*>> = mutableMapOf()
    })

    /* ---- NATIVE FINISH ---- */

    init {
        Native.libc().pipe(pipefd)
        // Register the wakeup pipe as the first element in the pollfd array
        pollData = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        putPollFD(0, pipefd[0])
        putPollEvents(0, OpP.POLLIN.value)
        nfds = 1
        //keyArray = arrayOfNulls(1)
    }

    private fun putPollFD(idx: Int, fd: Int) {
        pollData.putInt((idx * POLLFD_SIZE) + FD_OFFSET, fd)
    }

    private fun putPollEvents(idx: Int, events: Int) {
        pollData.putShort((idx * POLLFD_SIZE) + EVENTS_OFFSET, events.toShort())
    }

    private fun getPollFD(idx: Int): Int {
        return pollData.getInt((idx * POLLFD_SIZE) + FD_OFFSET)
    }

    private fun getPollEvents(idx: Int): Short {
        return pollData.getShort((idx * POLLFD_SIZE) + EVENTS_OFFSET)
    }

    private fun getPollRevents(idx: Int): Short {
        return pollData.getShort((idx * POLLFD_SIZE) + REVENTS_OFFSET)
    }

    private fun putPollRevents(idx: Int, events: Int) {
        pollData.putShort((idx * POLLFD_SIZE) + REVENTS_OFFSET, events.toShort())
    }

    override suspend fun poll(timeout: Long): Int {
        TODO("Not yet implemented")
    }

    override fun pollReadyCountImpl(cancelledCount: Int, timeout: Long): Int {
        var readyCount = 0

        do {
            readyCount = Native.libc().poll(pollData, nfds, timeout.toInt())
        } while (readyCount < 0 && Errno.EINTR.equals(Errno.valueOf(Native.getRuntime().lastError.toLong())))

        return readyCount
    }

    override fun provider(): SelectorProvider = object: SelectorProvider {
        override fun openSelector(): AbstractSelector {
            TODO("Not yet implemented")
        }
    }

    override suspend fun select(timeout: Duration): Int {
        val numCancelled = cleanCancelled()
        val pollReady = pollReadyCountImpl(numCancelled, timeout.inWholeMilliseconds)

        if (pollReady < 1) {
            return pollReady
        }

        if ((getPollRevents(0).toInt() and OpP.POLLIN.value) != 0) {
            wakeupReceived()
        }

        var updatedKeyCount = 0
        keys { k ->
            idx.dispense { i ->
                k.forEach { key ->
                    key as ChannelSelectionKey
                    val index = i.keyIndex.indexOf(key)
                    val revents = getPollRevents(index).toInt()
                    if (revents != 0) {
                        putPollRevents(index, 0)

                        if ((revents and (OpP.POLLHUP.value or OpP.POLLERR.value)) == 0) {
                            if((revents and OpP.POLLIN.value) != 0) when {
                                key.canMakeReady(SelectChannelOperation.OP_READ) -> key.readyOps(SelectChannelOperation.OP_READ)
                                key.canMakeReady(SelectChannelOperation.OP_ACCEPT) -> key.readyOps(SelectChannelOperation.OP_ACCEPT)
                            }
                            if((revents and OpP.POLLOUT.value) != 0) when {
                                key.canMakeReady(SelectChannelOperation.OP_WRITE) -> key.readyOps(SelectChannelOperation.OP_WRITE)
                                key.canMakeReady(SelectChannelOperation.OP_CONNECT) -> key.readyOps(SelectChannelOperation.OP_CONNECT)
                            }
                        }
                        ++updatedKeyCount
                    }
                }
            }
        }

        val numChanged = selectChanged()
        val numInvoked = if(numChanged > 0) invokeSelected() else 0
        return numInvoked
    }

    override fun selectNow(): Int {
        var selectCount = 0
        task { selectCount = select(0.milliseconds) }
        return selectCount
    }

    override suspend fun implCleanCancelled(
        key: AbstractSelectionKey<*, *>,
        altCnt: Int
    ): Int { return altCnt }

    override suspend fun implCloseSelector() {

        if (pipefd[0] != -1) {
            Native.close(pipefd[0]);
        }
        if (pipefd[1] != -1) {
            Native.close(pipefd[1]);
        }

    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> reportInterestImpl(
        key: AbstractSelectionKey<A, E>
    ) {
        idx.dispense { i ->
            var events = 0
            if(key.isInterested(SelectChannelOperation.OP_ACCEPT as E) || key.isInterested(SelectChannelOperation.OP_READ as E)) {
                events = events or OpP.POLLIN.value
            }

            if(key.isInterested(SelectChannelOperation.OP_WRITE as E) || key.isInterested(SelectChannelOperation.OP_CONNECT as E)) {
                events = events or OpP.POLLOUT.value
            }

            putPollEvents(i.keyIndex.indexOf(key as ChannelSelectionKey<A>), events)
        }

    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> deregisterImpl(
        key: AbstractSelectionKey<A, E>
    ) {
        fds.dispense { f ->
            idx.dispense { i ->
                val index = i.keyIndex.indexOf(key as ChannelSelectionKey)
                if(index < i.keyIndex.lastIndex) {
                    val lastIndex = i.keyIndex.lastIndex
                    val last = i.keyIndex.removeAt(lastIndex)
                    i.keyIndex[index] = last
                    i.inverseKeyIndex.remove(lastIndex)

                    val fd = f.keyFD[key]
                    f.keyFD.remove(key)
                    f.inverseKeyFD.remove(fd)

                    putPollFD(index, getPollFD(lastIndex))
                    putPollEvents(index, getPollEvents(lastIndex).toInt())
                } else {
                    putPollFD(index, -1)
                    putPollEvents(index, 0)
                }
            }
        }
    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> registerImpl(key: AbstractSelectionKey<A, E>) {
        fds.dispense { f ->
            val ch = key.item() as NativeSelectableChannel
            val fd = ch.fd

            check(f.inverseKeyFD.containsKey(fd)) { "File descriptor already registered" }

            f.keyFD[key as ChannelSelectionKey<*>] = fd
            f.inverseKeyFD[fd] = key

            if((pollData.limit() / 8) <= f.keyFD.size) {
                val newBuf = ByteBuffer.allocateDirect((f.keyFD.size * 1.6).toInt() * 8)
                newBuf.put(pollData)
                newBuf.position(0)
                pollData = newBuf.order(ByteOrder.nativeOrder())
            }

            idx.dispense { i ->
                i.keyIndex.add(key)
                val index = i.keyIndex.lastIndex
                i.inverseKeyIndex[index] = key

                putPollFD(index, fd)
                putPollEvents(index, 0)
            }
        }
    }

    override suspend fun wakeupReceived() {
        Native.read(pipefd[0], wakeBuf)
    }

    override suspend fun wakeup(): Selector {
        try {
            Native.write(pipefd[1], wakeBuf)
        } catch (ioe: IOException) {
            throw RuntimeException(ioe)
        }

        return this
    }

    public enum class OpP(public val value: Int) {
        POLLIN(0x1),
        POLLOUT(0x4),
        POLLERR(0x8),
        POLLHUP(0x10);
    }

    public companion object {
        public const val POLLFD_SIZE: Int = 8
        public const val FD_OFFSET: Int = 0
        public const val EVENTS_OFFSET: Int = 4
        public const val REVENTS_OFFSET: Int = 6
    }
}