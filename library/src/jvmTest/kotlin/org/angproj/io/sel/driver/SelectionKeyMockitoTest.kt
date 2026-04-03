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
package org.angproj.io.sel.driver

import org.angproj.io.sel.SelectChannelOperation
import org.angproj.io.sel.SelectionKey
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertTrue

class SelectionKeyMockitoTest {
    @Test
    fun testSelectionKey() {
        val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.isHandleable(SelectChannelOperation.OP_READ)).thenReturn(true)

        assertTrue { selectionKey.isHandleable(SelectChannelOperation.OP_READ) }
    }
}