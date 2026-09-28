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

    private val allKeys: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
    private val selected: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
    private val cancelled: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())

    override suspend fun keys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
        allKeys.dispense(block)
    }

    override suspend fun selectedKeys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
        selected.dispense(block)
    }

    protected suspend fun cancelledKeys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
        cancelled.dispense(block)
    }

    private var _closed = false

    override fun close() {
        if (!_closed) {
            _closed = true
            task {
                implCloseSelector()
                keys { keys ->
                    keys.forEach { key ->
                        key.takeIf { it.isValid() }?.cancel()
                        keys.remove(key)
                    }
                }
                selectedKeys { keys -> keys.clear() }
                cancelledKeys { keys -> keys.clear() }
            }
        }
    }

    override fun isOpen(): Boolean = !_closed

    abstract override fun provider(): SelectorProvider

    abstract override fun select(timeout: Duration): Int

    abstract override fun selectNow(): Int

    abstract override suspend fun wakeup(): Selector

    //protected void	begin()

    internal suspend fun deregister(key: AbstractSelectionKey<*, *>) {
        require(!key.isValid()) { "Key must be cancelled before deregistration" }
        cancelledKeys { keys -> keys.remove(key) }
        selectedKeys { keys -> keys.remove(key) }
        keys { keys -> keys.remove(key) }
    }
    //protected void	end()

    protected abstract suspend fun implCloseSelector()

    override suspend fun <I: AbstractSelectableItem, E : SelectOperation<*>, A: Closeable> register(
        item: I,
        vararg ops: E,
        attachment: A,
        build: AbstractSelector.(I) -> AbstractSelectionKey<A, E>
    ): AbstractSelectionKey<A, *> {
        check(isOpen()) { "Selector is closed" }

        val selectionKey = build(item)
        selectionKey.interestOps(*ops)
        selectionKey.attach(attachment)
        keys { keys ->
            keys.add(selectionKey)
        }
        return selectionKey
    }
}