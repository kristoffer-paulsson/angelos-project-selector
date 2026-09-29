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

public interface Selector {
    public fun close()

    public fun isOpen(): Boolean

    public suspend fun keys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit)

    public fun provider(): SelectorProvider

    public fun select(timeout: Duration): Int

    public suspend fun selectedKeys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit)

    public suspend fun cancelledKeys(block: suspend (HashSet<AbstractSelectionKey<*,*>>) -> Unit)

    public fun selectNow(): Int

    public suspend fun wakeup(): Selector

    public suspend fun <A: Closeable, E : SelectOperation<*>> reportInterest(key: AbstractSelectionKey<A, E>)

    public suspend fun <I: AbstractSelectableItem, E : SelectOperation<*>, A: Closeable> register(
        item: I,
        vararg ops: E,
        attachment: A,
        build: AbstractSelector.(I) -> AbstractSelectionKey<A, E>
    ): AbstractSelectionKey<A, *>

    public companion object {
         //public fun	open(): Selector { return }
    }
}