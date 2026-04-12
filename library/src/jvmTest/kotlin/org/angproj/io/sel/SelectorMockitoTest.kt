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

import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.microseconds

open class SelectorMockitoTest {

    protected open val selector: Selector = mock<Selector> {}

    @Test
    fun testClose() {
        whenever(selector.close()).then {
            verify(selector).close()
        }
    }

    @Test
    fun testIsOpen() {
        whenever(selector.isOpen()).thenReturn(true)

        assertTrue { selector.isOpen() }
    }

    @Test
    fun testKeys() {
        whenever(selector.keys()).thenReturn(setOf())

        assertTrue { selector.keys().isEmpty() }
    }

    @Test
    fun testProvider() {
        val provider = Mockito.mock(SelectorProvider::class.java)

        whenever(selector.provider()).thenReturn(provider)

        assertEquals(selector.provider(), provider)
    }

    @Test
    fun testSelect() {
        whenever(selector.select()).thenReturn(0)

        assertEquals(0, selector.select())
    }

    @Test
    fun testSelectDuration() {
        whenever(selector.select(2.microseconds)).thenReturn(0)

        assertEquals(0, selector.select(2.microseconds))
    }

    @Test
    fun testSelectedKeys() {
        whenever(selector.selectedKeys()).thenReturn(setOf())

        assertTrue { selector.selectedKeys().isEmpty() }
    }

    @Test
    fun testSelectNow() {
        whenever(selector.selectNow()).thenReturn(0)

        assertEquals(0, selector.selectNow())
    }

    @Test
    fun testWakeup(): Unit = runBlocking {
        whenever(selector.wakeup()).thenReturn(selector)

        assertEquals(selector.wakeup(), selector)
    }
}