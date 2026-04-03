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

import kotlin.time.Duration


public abstract class AbstractSelector : Selector {

    abstract override fun close()

    abstract override fun isOpen(): Boolean

    abstract override fun keys(): Set<SelectionKey<*, *>>

    abstract override fun provider(): SelectorProvider

    abstract override fun select(): Int

    abstract override fun select(timeout: Duration): Int

    abstract override fun selectedKeys(): Set<SelectionKey<*, *>>

    abstract override fun selectNow(): Int

    abstract override fun wakeup(): Selector

    //protected void	begin()

    protected abstract fun cancelledKeys(): Set<SelectionKey<*, *>>

    internal abstract fun deregister(key: AbstractSelectionKey<*, *>)

    //protected void	end()

    protected abstract fun implCloseSelector()

    protected abstract fun<A> register(channel: AbstractSelectableItem, ops: Int, att: A): SelectionKey<A, *>
}