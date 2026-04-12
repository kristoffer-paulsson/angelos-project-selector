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
package org.angproj.io.sel

import org.angproj.io.sel.channel.SelectChannelOperation
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class SelectionKeyMockitoTest {

    protected open val selectionKey: SelectionKey<String, SelectChannelOperation> = mock<SelectionKey<String, SelectChannelOperation>>()

    @Test
    fun testDoHandle() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.doHandle()).then {
            verify(selectionKey).doHandle()
        }
    }

    @Test
    fun testSelector() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()
        val selector = mock<Selector> {}

        whenever(selectionKey.selector()).thenReturn(selector)

        assertTrue { selectionKey.selector() === selector }
    }

    @Test
    fun testAttach() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()
        val attachment = "Hello, World"

        whenever(selectionKey.attach(attachment)).then {
            selectionKey.attachment() == attachment
        }

        whenever(selectionKey.attach(attachment)).then {
            verify(selectionKey).attach(attachment)
        }
    }

    @Test
    fun testAttachment() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()
        val attachment = "Hello, World"

        whenever(selectionKey.attachment()).thenReturn(attachment)

        assertEquals(selectionKey.attachment(), attachment)
    }

    @Test
    fun testItem() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()
        val item = mock<SelectableItem> {}

        whenever(selectionKey.item()).thenReturn(item)

        assertTrue { selectionKey.item() === item }
    }

    @Test
    fun testInterestOps() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.interestOps(SelectChannelOperation.OP_READ)).thenReturn(selectionKey)

        assertTrue { selectionKey.interestOps(SelectChannelOperation.OP_READ) === selectionKey }
    }

    @Test
    fun testInterestOpsEmpty() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.interestOps()).thenReturn(0)

        assertTrue { selectionKey.interestOps() == 0 }
    }

    @Test
    fun testIsHandleable() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.isHandleable(SelectChannelOperation.OP_READ)).thenReturn(true)

        assertTrue { selectionKey.isHandleable(SelectChannelOperation.OP_READ) }
    }

    @Test
    fun testIsValid() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.isValid()).thenReturn(true)

        assertTrue { selectionKey.isValid() }
    }

    @Test
    fun testCancel() {
        //val selectionKey = mock<SelectionKey<String, SelectChannelOperation>>()

        whenever(selectionKey.cancel()).then {
                verify(selectionKey).cancel()
        }
    }
}