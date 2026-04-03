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

import org.angproj.io.sel.AbstractSelectableChannel
import org.angproj.io.sel.AbstractSelectionKey
import org.angproj.io.sel.AbstractSelector
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
        override fun close() {
            TODO("Not yet implemented")
        }

        override fun isOpen(): Boolean {
            TODO("Not yet implemented")
        }

        override fun keys(): Set<SelectionKey<*, *>> {
            TODO("Not yet implemented")
        }

        override fun provider(): SelectorProvider {
            TODO("Not yet implemented")
        }

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

        override fun wakeup(): Selector {
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

        override fun<A> register(
            channel: AbstractSelectableChannel,
            ops: Int,
            att: A
        ): SelectionKey<A, *> {
            TODO("Not yet implemented")
        }
    }
}