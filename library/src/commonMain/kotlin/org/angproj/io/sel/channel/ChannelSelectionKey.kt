/**
 * Copyright (c) 2025-2026 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
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
package org.angproj.io.sel.channel

import org.angproj.io.sel.AbstractSelectionKey
import org.angproj.io.sel.AbstractSelector

public class ChannelSelectionKey<A>(
    selector: AbstractSelector,
    item: SelectableChannel,
    handler: suspend AbstractSelectionKey<A, SelectChannelOperation>.() -> Unit,
) : AbstractSelectionKey<A, SelectChannelOperation>(selector, item, handler) {

    public fun isAcceptable(): Boolean = isHandleable(SelectChannelOperation.OP_ACCEPT)

    public fun isConnectable(): Boolean = isHandleable(SelectChannelOperation.OP_CONNECT)

    public fun isReadable(): Boolean = isHandleable(SelectChannelOperation.OP_READ)

    public fun isWritable(): Boolean = isHandleable(SelectChannelOperation.OP_WRITE)
}