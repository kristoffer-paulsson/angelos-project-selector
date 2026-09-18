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

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.angproj.io.sel.notify.NotifySelectionKey
import org.angproj.io.sel.notify.NotifySelectionKeyTest.Attachment
import org.angproj.io.sel.notify.SelectNotifyOperation
import org.angproj.io.sel.notify.SelectableNotify
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class DriverTest {
    @Test
    fun testOpenSelector() = runTest {
        var loop = 0
        val selectable = SelectableNotify()
        val selector = Driver.openSelector()

        val key = selector.register(selectable, SelectNotifyOperation.OP_NOTIFY, attachment = object {}) {
            NotifySelectionKey(this, it) {
                when {
                    isHandleable(SelectNotifyOperation.OP_NOTIFY) -> {
                        println("Hello, world! $loop")

                    }
                    isHandleable(SelectNotifyOperation.OP_CLOSE) -> {
                        println("Time to stop! $loop")
                    }
                    else -> error("Unhandled $loop")
                }
                resetOps()
                interestOps(SelectNotifyOperation.OP_NOTIFY)
            }
        } as NotifySelectionKey

        loop {
            if(key.canMakeReady(SelectNotifyOperation.OP_NOTIFY)) {
                key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            }

            selector.selectNow()
            loop++
            yield()

            if(loop > 100){
                selector.close()
                coroutineContext.cancelChildren()
            }
        }.join()
    }
}