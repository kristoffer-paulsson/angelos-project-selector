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
package org.angproj.io.sel.channel

import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.Closeable
import org.angproj.io.sel.driver.Driver

public class Socket(selector: AbstractSelector = Driver.openSelector()) : SelectableChannel(selector) {

    public fun<A: Closeable> accept(attachment: A) {
        TODO()
    }

    override suspend fun <A: Closeable> openImpl(attachment: A): ChannelSelectionKey<A> {
        return selector.register(this, SelectChannelOperation.OP_ACCEPT, attachment = attachment) {
            ChannelSelectionKey(selector, it) {
                when{
                    isHandleable(SelectChannelOperation.OP_ACCEPT) -> {
                        clearOps(SelectChannelOperation.OP_ACCEPT)
                        accept(attachment())
                        interestOps(SelectChannelOperation.OP_ACCEPT)
                    }
                    else -> {
                        clearOps(SelectChannelOperation.OP_ACCEPT)
                        cancel()
                    }
                }
            }
        } as ChannelSelectionKey
    }

    override fun closeImpl() {
        TODO("Not yet implemented")
    }
}