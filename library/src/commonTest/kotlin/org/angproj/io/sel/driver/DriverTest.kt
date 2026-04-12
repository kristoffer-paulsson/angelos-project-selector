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

import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class DriverTest {
    @Test
    fun testOpenSelector() {
        val selector = Driver.openSelector()

        var loop = 0
        while (selector.isOpen()) {
            selector.select(1.milliseconds)
            loop++
            if (loop >= 100) { selector.close() }
        }

    }
}