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

import org.angproj.io.sel.SelectOperation

public enum class SelectChannelOperation(override val operation: Int): SelectOperation<SelectChannelOperation> {
    OP_READ(1),
    OP_WRITE(OP_READ.operation shl 1),
    OP_CONNECT(OP_WRITE.operation shl 1),
    OP_ACCEPT(OP_CONNECT.operation shl 1);
}