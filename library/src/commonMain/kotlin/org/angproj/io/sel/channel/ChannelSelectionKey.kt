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
    attachment: A
) : AbstractSelectionKey<A, SelectChannelOperation>(selector, item, attachment) {

    public fun isAcceptable(): Boolean = (readyOps() and SelectChannelOperation.OP_ACCEPT.toInt()) != 0

    public fun isConnectable(): Boolean = (readyOps() and SelectChannelOperation.OP_CONNECT.toInt()) != 0

    public fun isReadable(): Boolean = (readyOps() and SelectChannelOperation.OP_READ.toInt()) != 0

    public fun isWritable(): Boolean = (readyOps() and SelectChannelOperation.OP_WRITE.toInt()) != 0
}