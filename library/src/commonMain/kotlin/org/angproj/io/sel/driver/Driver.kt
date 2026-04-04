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

        private val allKeys: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
        private val selected: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())
        private val cancelled: Dispenser<HashSet<SelectionKey<*, *>>> = Dispenser(hashSetOf())

        override fun close() {
            TODO("Not yet implemented")
        }

        override fun isOpen(): Boolean {
            TODO("Not yet implemented")
        }

        override fun keys(): Set<SelectionKey<*, *>> {
            TODO("Not yet implemented")
        }

        override fun provider(): SelectorProvider = this@Driver

        override fun select(): Int {
            TODO("Not yet implemented")
        }

        override fun select(timeout: Duration): Int {
            TODO("Not yet implemented")
        }

        override fun selectedKeys(): Set<SelectionKey<*, *>> {
            TODO("Not yet implemented")
        }

        override fun selectNow(): Int {
            TODO("Not yet implemented")
        }

        override suspend fun wakeup(): Selector {
            TODO("Not yet implemented")
        }

        override fun cancelledKeys(): Set<SelectionKey<*, *>> {
            TODO("Not yet implemented")
        }

        override fun deregister(key: AbstractSelectionKey<*, *>) {
            TODO("Not yet implemented")
        }

        override fun implCloseSelector() {
            TODO("Not yet implemented")
        }

        override suspend fun <A, E : SelectOperation<*>> register(
            item: AbstractSelectableItem,
            vararg ops: E,
            attachment: A,
            build: AbstractSelector.(AbstractSelectableItem) -> SelectionKey<A, E>
        ): SelectionKey<A, E> {
            val selectionKey = build(item)
            selectionKey.interestOps(*ops)
            selectionKey.attach(attachment)
            allKeys.dispense {
                it.add(selectionKey)
            }
            return selectionKey
        }
    }
}