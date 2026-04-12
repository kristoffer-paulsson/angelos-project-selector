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

    abstract override suspend fun keys(block: (HashSet<SelectionKey<*,*>>) -> Unit)

    abstract override fun provider(): SelectorProvider

    abstract override fun select(): Int

    abstract override fun select(timeout: Duration): Int

    abstract override suspend fun selectedKeys(block: (HashSet<SelectionKey<*,*>>) -> Unit)

    abstract override fun selectNow(): Int

    abstract override suspend fun wakeup(): Selector

    //protected void	begin()

    protected abstract suspend fun cancelledKeys(block: (HashSet<SelectionKey<*,*>>) -> Unit)

    internal abstract suspend fun deregister(key: AbstractSelectionKey<*, *>)

    //protected void	end()

    protected abstract suspend fun implCloseSelector()

    protected abstract suspend fun<A, E : SelectOperation<*>> register(
        item: AbstractSelectableItem,
        vararg ops: E,
        attachment: A,
        build: AbstractSelector.(AbstractSelectableItem) -> SelectionKey<A, E>
    ): SelectionKey<A, *>
}