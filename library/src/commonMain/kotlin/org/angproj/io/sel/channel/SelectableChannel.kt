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

import org.angproj.io.sel.AbstractSelectableItem
import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.SelectorException

public abstract class SelectableChannel(protected val selector: AbstractSelector) : AbstractSelectableItem() {

    protected lateinit var _key: ChannelSelectionKey<*>

    public suspend fun<A> open(attachment: A) {
        if(::_key.isInitialized) throw SelectorException()
        _key = openImpl(attachment)
    }

    protected abstract suspend fun<A> openImpl(attachment: A): ChannelSelectionKey<A>

    override val isOpen: Boolean
        get() = ::_key.isInitialized && _key.isValid()

    override fun close() {
        if(isOpen) {
            _key.cancel()
            closeImpl()
        }
    }

    protected abstract fun closeImpl()
}