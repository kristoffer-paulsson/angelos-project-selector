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
package org.angproj.io.sel.notify

import org.angproj.io.sel.AbstractSelectionKey
import org.angproj.io.sel.AbstractSelector

public class NotifySelectionKey<A>(
    selector: AbstractSelector,
    item: SelectableNotify,
    handler: suspend AbstractSelectionKey<A, SelectNotifyOperation>.() -> Unit,
) : AbstractSelectionKey<A, SelectNotifyOperation>(selector, item, handler) {
    public fun isNotifiable(): Boolean = isHandleable(SelectNotifyOperation.OP_NOTIFY)

    public fun isClosable(): Boolean = isHandleable(SelectNotifyOperation.OP_CLOSE)
}