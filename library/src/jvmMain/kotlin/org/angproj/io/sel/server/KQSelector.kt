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
import jnr.ffi.*
import jnr.ffi.provider.jffi.NativeRuntime
import org.angproj.io.sel.*
import org.angproj.io.sel.channel.ChannelSelectionKey
import org.angproj.io.sel.channel.SelectChannelOperation
import org.angproj.io.sel.driver.Dispenser
import org.angproj.io.sel.driver.task
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


public class KQSelector: AbstractSelector() {

    /* ---- NATIVE START ---- */

    private var kqfd = -1
    private val pipefd = intArrayOf(-1, -1)

    private val runtime: Runtime? = NativeRuntime.getSystemRuntime()
    private val changeBuf: Pointer
    private val eventBuf: Pointer
    private val io: EventIO = EventIO.instance
    private val ZERO_TIMESPEC = Native.Timespec(0, 0)

    private val fds = Dispenser(object {
        public val keyFD: MutableMap<ChannelSelectionKey<*>, Int> = mutableMapOf()
        val inverseKeyFD: MutableMap<Int, ChannelSelectionKey<*>> = mutableMapOf()
    })

    /* ---- NATIVE FINISH ---- */

    init {
        changeBuf = Memory.allocateDirect(runtime, EventIO.MAX_EVENTS * io.size())
        eventBuf = Memory.allocateDirect(runtime, EventIO.MAX_EVENTS * io.size())

        Native.libc().pipe(pipefd)

        kqfd = Native.libc().kqueue()
        io.put(changeBuf, 0, pipefd[0], EvFilt.EVFILT_READ.value, Ev.EV_ADD.value)
        Native.libc().kevent(kqfd, changeBuf, 1, null, 0, ZERO_TIMESPEC)
    }

    override fun provider(): SelectorProvider = object: SelectorProvider {
        override fun openSelector(): AbstractSelector {
            TODO("Not yet implemented")
        }
    }

    override fun selectNow(): Int {
        var selectCount = 0
        task { selectCount = select(0.milliseconds) }
        return selectCount
    }

    override suspend fun select(timeout: Duration): Int {
        val numCancelled = cleanCancelled()
        val pollReady = pollReadyCountImpl(numCancelled, timeout.inWholeMilliseconds)

        var updatedKeyCount = 0
        fds.dispense {
            ( 0..< pollReady).forEach { idx ->
                val fd = io.getFD(eventBuf, idx)

                when(fd) {
                    pipefd[0] -> wakeupReceived()
                    else -> {
                        val key = it.inverseKeyFD[fd] ?: error("Key for $fd not found")
                        val filt = io.getFilter(eventBuf, idx)
                        if(EvFilt.EVFILT_READ.value == filt) when {
                            key.canMakeReady(SelectChannelOperation.OP_READ) -> key.readyOps(SelectChannelOperation.OP_READ)
                            key.canMakeReady(SelectChannelOperation.OP_ACCEPT) -> key.readyOps(SelectChannelOperation.OP_ACCEPT)
                        }
                        if(EvFilt.EVFILT_WRITE.value == filt) when {
                            key.canMakeReady(SelectChannelOperation.OP_WRITE) -> key.readyOps(SelectChannelOperation.OP_WRITE)
                            key.canMakeReady(SelectChannelOperation.OP_CONNECT) -> key.readyOps(SelectChannelOperation.OP_CONNECT)
                        }
                        updatedKeyCount++
                    }
                }
            }
        }

        val numChanged = selectChanged()
        val numInvoked = if(numChanged > 0) invokeSelected() else 0
        return numInvoked
    }

    override suspend fun implCloseSelector() {
        /* ---- NATIVE START ---- */

        if (kqfd != -1) {
            Native.close(kqfd);
        }
        if (pipefd[0] != -1) {
            Native.close(pipefd[0]);
        }
        if (pipefd[1] != -1) {
            Native.close(pipefd[1]);
        }
        pipefd[0] = -1
        pipefd[1] = -1
        kqfd = -1

        fds.dispense {
            it.keyFD.clear()
            it.inverseKeyFD.clear()
        }

        /* ---- NATIVE FINISH ---- */
    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> reportInterestImpl(
        key: AbstractSelectionKey<A, E>
    ) {
        var count = 0

        fds.dispense {
            val fd = it.keyFD[key as ChannelSelectionKey] ?: error("$key not found")

            val writing = key.isInterested(SelectChannelOperation.OP_ACCEPT as E) || key.isInterested(SelectChannelOperation.OP_READ as E)
            val reading = key.isInterested(SelectChannelOperation.OP_CONNECT as E) || key.isInterested(SelectChannelOperation.OP_WRITE as E)

            var flags = 0
            // EvFilt.EVFILT_READ
            flags = when {
                reading -> Ev.EV_ADD.value or Ev.EV_ENABLE.value or Ev.EV_CLEAR.value
                else -> Ev.EV_DISABLE.value
            }
            if (flags != 0) {
                io.put(changeBuf, count++, fd, EvFilt.EVFILT_READ.value, flags)
            }

            // EvFilt.EVFILT_WRITE
            flags = when {
                writing -> Ev.EV_ADD.value or Ev.EV_ENABLE.value or Ev.EV_CLEAR.value
                else -> Ev.EV_DISABLE.value
            }
            if (flags != 0) {
                io.put(changeBuf, count++, fd, EvFilt.EVFILT_WRITE.value, flags)
            }
        }
        Native.libc().kevent(kqfd, changeBuf, count, null, 0, ZERO_TIMESPEC)
    }

    override suspend fun implCleanCancelled(
        key: AbstractSelectionKey<*, *>,
        altCnt: Int
    ): Int {
        var cnt = altCnt
        fds.dispense {
            val fd = it.keyFD[key] ?: error("Missing descriptor")
            io.put(changeBuf, cnt++, fd, EvFilt.EVFILT_READ.value, Ev.EV_DELETE.value)
            io.put(changeBuf, cnt++, fd, EvFilt.EVFILT_WRITE.value, Ev.EV_DELETE.value)
        }
        if (cnt >= EventIO.MAX_EVENTS) {
            Native.libc().kevent(kqfd, changeBuf, cnt, null, 0, ZERO_TIMESPEC)
            cnt = 0
        }
        return cnt
    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> deregisterImpl(
        key: AbstractSelectionKey<A, E>
    ) {
        fds.dispense {
            val fd = it.keyFD[key as ChannelSelectionKey<A>] ?: error("Missing descriptor")
            it.keyFD.remove(key)
            it.inverseKeyFD.remove(fd)
        }
    }

    override suspend fun <A : Closeable, E : SelectOperation<*>> registerImpl(key: AbstractSelectionKey<A, E>) {
        fds.dispense {
            val fd = (key.item() as NativeSelectableChannel).fd
            it.keyFD.putIfAbsent(key as ChannelSelectionKey<*>, fd) ?: error("Already existing descriptor")
            it.inverseKeyFD[fd] = key
        }
        reportInterestImpl(key)
    }

    override suspend fun wakeupReceived() {
        Native.libc().read(pipefd[0], ByteArray(1), 1)
    }

    override suspend fun wakeup(): Selector {
        Native.libc().write(pipefd[1], ByteArray(1), 1)
        return this
    }

    override suspend fun poll(timeout: Long): Int {
        TODO("Not yet implemented")
    }

    override fun pollReadyCountImpl(cancelledCount: Int, timeout: Long): Int {
        var ts: Native.Timespec? = null
        if (timeout >= 0) {
            val sec = TimeUnit.MILLISECONDS.toSeconds(timeout)
            val nsec = TimeUnit.MILLISECONDS.toNanos(timeout % 1000)
            ts = Native.Timespec(sec, nsec)
        }

        if (EventIO.DEBUG) System.err.printf("nchanged=%d\n", cancelledCount)
        var readyCount = 0
        do {
            readyCount = Native.libc().kevent(kqfd, changeBuf, cancelledCount, eventBuf, EventIO.MAX_EVENTS, ts)
        } while (readyCount < 0 && Errno.EINTR == Errno.valueOf(Native.getRuntime().lastError.toLong()))

        if (EventIO.DEBUG) System.err.println("kevent returned $readyCount events ready")

        return readyCount
    }

    public enum class EvFilt(public val value: Int) {
        EVFILT_READ(-1),
        EVFILT_WRITE(-2),
    }

    public enum class Ev(public val value: Int) {
        EV_ADD(0x0001),
        EV_DELETE(0x0002),
        EV_ENABLE(0x0004),
        EV_DISABLE(0x0008),
        EV_CLEAR(0x0020);
    }

    public class EventIO private constructor() {
        private val layout: EventLayout
        private val uintptr_t: Type?

        init {
            var is_freebsd_12_or_later = false
            if (Platform.getNativePlatform().getOS() == Platform.OS.FREEBSD) {
                var version = System.getProperty("os.version")
                if (version != null) {
                    var tr_i = -1
                    for (c in charArrayOf(' ', '_', '-', '+', '.')) {
                        val i = version.indexOf(c)
                        if (i >= 0 && (tr_i == -1 || tr_i > i)) tr_i = i
                    }
                    if (tr_i >= 0) version = version.substring(0, tr_i)
                    try {
                        val freebsd_major_version = version.toInt()
                        if (freebsd_major_version > 11) is_freebsd_12_or_later = true
                    } catch (e: NumberFormatException) {
                        if (DEBUG) e.printStackTrace()
                    }
                }
            }
            if (is_freebsd_12_or_later) {
                layout = FreeBSD12EventLayout(NativeRuntime.getSystemRuntime())
            } else {
                layout = LegacyEventLayout(NativeRuntime.getSystemRuntime())
            }
            uintptr_t = layout.getRuntime().findType(TypeAlias.uintptr_t)
        }

        public fun put(buf: Pointer, index: Int, fd: Int, filt: Int, flags: Int) {
            buf.putInt(uintptr_t, (index * layout.size()) + layout.ident.offset(), fd.toLong())
            buf.putShort((index * layout.size()) + layout.filter.offset(), filt.toShort())
            buf.putShort((index * layout.size()) + layout.flags.offset(), flags.toShort())
        }

        public fun size(): Int {
            return layout.size()
        }

        public fun getFD(ptr: Pointer, index: Int): Int {
            return ptr.getInt(uintptr_t, (index * layout.size()) + layout.ident.offset()).toInt()
        }

        public fun putFilter(buf: Pointer, index: Int, filter: Int) {
            buf.putShort((index * layout.size()) + layout.filter.offset(), filter.toShort())
        }

        public fun getFilter(buf: Pointer, index: Int): Int {
            return buf.getShort((index * layout.size()) + layout.filter.offset()).toInt()
        }

        public fun putFlags(buf: Pointer, index: Int, flags: Int) {
            buf.putShort((index * layout.size()) + layout.flags.offset(), flags.toShort())
        }

        private abstract class EventLayout(runtime: Runtime?) : StructLayout(runtime) {
            val ident: uintptr_t = uintptr_t()
            val filter: int16_t = int16_t()
            val flags: u_int16_t = u_int16_t()
            val fflags: u_int32_t = u_int32_t()
        }

        private class LegacyEventLayout(runtime: Runtime?) : EventLayout(runtime) {
            val data: intptr_t = intptr_t()
            val udata: Pointer = Pointer()
        }

        private class FreeBSD12EventLayout(runtime: Runtime?) : EventLayout(runtime) {
            val data: int64_t = int64_t()
            val udata: Pointer = Pointer()
            val ext: Array<u_int64_t?>? = array<u_int64_t?>(arrayOfNulls<u_int64_t>(4))
        }

        public companion object {
            public const val MAX_EVENTS: Int = 100
            public const val DEBUG: Boolean = false
            private val _instance: EventIO by lazy { EventIO() }

            public val instance: EventIO
                get() = _instance
        }
    }
}