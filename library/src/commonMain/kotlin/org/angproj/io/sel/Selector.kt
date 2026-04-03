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

    public fun keys(): Set<SelectionKey<*, *>>

    public fun provider(): SelectorProvider

    public fun select(): Int

    public fun select(timeout: Duration): Int

    public fun selectedKeys(): Set<SelectionKey<*, *>>

    public fun selectNow(): Int

    public fun wakeup(): Selector

    public companion object {
         //public fun	open(): Selector { return }
    }
}