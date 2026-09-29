/**
 * Copyright (c) 2025 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
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
package org.angproj.io.sel

import org.angproj.io.sel.driver.Dispenser
import org.angproj.io.sel.driver.task
import kotlin.time.Duration


public abstract class AbstractSelector : Selector {

    protected abstract suspend fun poll(): Int

    protected abstract fun pollReadyCountImpl(cancelledCount: Int, timeout: Long): Int

    private val allKeys: Dispenser<HashSet<AbstractSelectionKey<*, *>>> = Dispenser(hashSetOf())
    private val selected: Dispenser<HashSet<AbstractSelectionKey<*, *>>> = Dispenser(hashSetOf())
    private val cancelled: Dispenser<HashSet<AbstractSelectionKey<*, *>>> = Dispenser(hashSetOf())

    override suspend fun keys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit) {
        allKeys.dispense(block)
    }

    override suspend fun selectedKeys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit) {
        selected.dispense(block)
    }

    override suspend fun cancelledKeys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit) {
        cancelled.dispense(block)
    }

    private var _closed = false

    override fun close() {
        if (!_closed) {
            _closed = true
            task {
                implCloseSelector()
                cleanCancelled()
                selectedKeys { keys -> keys.clear() }
                keys { keys ->
                    keys.forEach { if(it.isValid()) it.cancel() }
                    keys.clear()
                }
            }
        }
    }

    override fun isOpen(): Boolean = !_closed

    abstract override fun provider(): SelectorProvider

    abstract override fun select(timeout: Duration): Int

    abstract override fun selectNow(): Int

    abstract override suspend fun wakeup(): Selector

    protected abstract suspend fun wakeupReceived()

    //protected void	begin()

    protected suspend fun selectChanged(): Int {
        var changeCount = 0
        selectedKeys { sKeys ->
            keys { keys ->
                keys.forEach { key ->
                    if(key.readyOps() != 0 && key.isIdle()) {
                        sKeys.add(key)
                        changeCount++
                    }
                }
            }
        }
        return changeCount
    }

    protected suspend fun invokeSelected(): Int {
        var selectedCount = 0
        selectedKeys { sKeys ->
            sKeys.forEach { sKey -> if(sKey.isValid()) sKey.doHandle() }
            selectedCount = sKeys.count()
            sKeys.clear()
        }
        return selectedCount
    }

    protected suspend fun cleanCancelled(): Int {
        var cancelledCount = 0
        var internalCount = 0
        cancelledKeys { cKeys ->
            keys { keys -> keys.removeAll(cKeys) }
            cancelledCount -= cKeys.size
            if(isOpen()) cKeys.forEach { cKey -> internalCount = implCleanCancelled(cKey, internalCount) }
            cKeys.clear()
        }
        return cancelledCount
    }

    protected abstract suspend fun implCleanCancelled(key: AbstractSelectionKey<*, *>, altCnt: Int): Int

    protected abstract suspend fun implCloseSelector()

    override suspend fun <A: Closeable, E : SelectOperation<*>> reportInterest(
        key: AbstractSelectionKey<A, E>
    ): Unit = reportInterestImpl(key)

    protected abstract suspend fun <A: Closeable, E : SelectOperation<*>> reportInterestImpl(key: AbstractSelectionKey<A, E>)

    internal suspend fun<A: Closeable, E : SelectOperation<*>> deregister(key: AbstractSelectionKey<A, E>) {
        require(!key.isValid()) { "Key must be cancelled before deregistration" }
        key.attachment().close()
        deregisterImpl(key)
        if(isOpen()) cancelledKeys { keys -> keys.add(key) }
    }
    //protected void	end()

    protected abstract suspend fun<A: Closeable, E : SelectOperation<*>> deregisterImpl(key: AbstractSelectionKey<A, E>)

    override suspend fun <I: AbstractSelectableItem, E : SelectOperation<*>, A: Closeable> register(
        item: I,
        vararg ops: E,
        attachment: A,
        build: AbstractSelector.(I) -> AbstractSelectionKey<A, E>
    ): AbstractSelectionKey<A, E> {
        check(isOpen()) { "Selector is closed" }

        val selectionKey = build(item)
        selectionKey.interestOps(*ops)
        selectionKey.attach(attachment)
        keys { keys ->
            keys.add(selectionKey)
        }
        registerImpl(selectionKey)
        return selectionKey
    }

    protected abstract suspend fun<A: Closeable, E : SelectOperation<*>> registerImpl(key: AbstractSelectionKey<A, E>)
}