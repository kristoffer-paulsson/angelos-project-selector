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
    attachment: A
) : AbstractSelectionKey<A, SelectNotifyOperation>(selector, item, attachment) {
    public fun isNotifiable(): Boolean = (readyOps() and SelectNotifyOperation.OP_NOTIFY.toInt()) != 0

    public fun isClosable(): Boolean = (readyOps() and SelectNotifyOperation.OP_CLOSE.toInt()) != 0
}