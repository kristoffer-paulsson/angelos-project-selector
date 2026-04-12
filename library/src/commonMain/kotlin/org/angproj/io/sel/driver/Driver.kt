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
package org.angproj.io.sel.driver

import org.angproj.io.sel.AbstractSelectableItem
import org.angproj.io.sel.AbstractSelectionKey
import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.SelectOperation
import org.angproj.io.sel.SelectionKey
import org.angproj.io.sel.Selector
import org.angproj.io.sel.SelectorProvider
import kotlin.time.Duration

public object Driver : SelectorProvider {

    private val innerSelector: Selector by lazy {
        buildSelector()
    }

    public fun openSelector(): Selector {
        return innerSelector
    }

    private fun buildSelector(): Selector = object : AbstractSelector() {

        private var _closed = false

        private val allKeys: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
        private val selected: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
        private val cancelled: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())

        override fun close() {
            if (!_closed) {
                _closed = true
                task { implCloseSelector() }
            }
        }

        override fun isOpen(): Boolean = !_closed

        override suspend fun keys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
            allKeys.dispense(block)
        }

        override fun provider(): SelectorProvider = this@Driver

        override fun select(): Int = select(Duration.ZERO)

        override fun select(timeout: Duration): Int {
            var selectCount = 0
            schedule(timeout) {
                selectedKeys { keys -> selectCount = keys.size }
                wakeup()
                selectCount -= cleanCancelled()
            }
            return selectCount
        }

        override suspend fun selectedKeys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
            selected.dispense(block)
        }

        override fun selectNow(): Int {
            var selectCount = 0
            task {
                selectedKeys { keys -> selectCount = keys.size }
                wakeup()
                selectCount -= cleanCancelled()
            }
            return selectCount
        }

        private suspend fun cleanCancelled(): Int {
            var cancelledCount = 0
            cancelledKeys { keys ->
                cancelledCount -= keys.size
                keys.clear()
            }
            return cancelledCount
        }

        override suspend fun wakeup(): Selector {
            selectedKeys { selKeys ->
                val keyIter = selKeys.iterator()
                while (keyIter.hasNext()) {
                    val key = keyIter.next()
                    selKeys.remove(key)
                    when(key.isValid()) {
                        true -> key.doHandle()
                        else -> cancelledKeys { cancelledKeys -> cancelledKeys.add(key) }
                    }
                    key.doHandle()
                }
            }
            return this
        }

        override suspend fun cancelledKeys(block: suspend (HashSet<SelectionKey<*,*>>) -> Unit) {
            cancelled.dispense(block)
        }

        override suspend fun deregister(key: AbstractSelectionKey<*, *>) {
            require(!key.isValid()) { "Key must be cancelled before deregistration" }
            cancelledKeys { keys -> keys.remove(key) }
            selectedKeys { keys -> keys.remove(key) }
            keys { keys -> keys.remove(key) }
        }

        override suspend fun implCloseSelector() {
            keys { keys ->
                keys.forEach { key ->
                    key.takeIf { it.isValid() }?.cancel()
                    keys.remove(key)
                }
            }
            cancelledKeys() { keys ->
                keys.clear()
            }
        }

        override suspend fun <A, E : SelectOperation<*>> register(
            item: AbstractSelectableItem,
            vararg ops: E,
            attachment: A,
            build: AbstractSelector.(AbstractSelectableItem) -> SelectionKey<A, E>
        ): SelectionKey<A, E> {
            check(isOpen()) { "Selector is closed" }

            val selectionKey = build(item)
            selectionKey.interestOps(*ops)
            selectionKey.attach(attachment)
            allKeys.dispense { keys ->
                keys.add(selectionKey)
            }
            return selectionKey
        }
    }
}