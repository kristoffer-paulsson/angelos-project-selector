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

import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.angproj.io.sel.notify.NotifySelectionKey
import org.angproj.io.sel.notify.SelectNotifyOperation
import org.angproj.io.sel.notify.SelectableNotify
import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class DriverTest {
    @Test
    fun testOpenSelector() = runTest {
        var loop = 0
        val selectable = SelectableNotify()
        val selector = Driver.openSelector()
        val time = TimeSource.Monotonic.markNow()

        val key = selector.register(selectable, SelectNotifyOperation.OP_NOTIFY, attachment = object {}) {
            NotifySelectionKey(this, it) {
                when{
                    isHandleable(SelectNotifyOperation.OP_NOTIFY) -> {
                        loop++
                        println("Hello, world! $loop, " + time.elapsedNow())
                        clearOps(SelectNotifyOperation.OP_NOTIFY)
                       when {
                            loop < 100 -> interestOps(SelectNotifyOperation.OP_NOTIFY)
                            isValid() -> interestOps(SelectNotifyOperation.OP_CLOSE)
                            else -> Unit
                        }
                    }
                    isHandleable(SelectNotifyOperation.OP_CLOSE) -> {
                        clearOps(SelectNotifyOperation.OP_CLOSE)
                        cancel()
                        selector.close()
                    }
                    else -> error("Unhandled $loop " + readyOps())
                }
            }
        } as NotifySelectionKey

        var ticks = 0
        clock(DurationUnit.SECONDS, 100) {
            key.takeIf {
                it.canMakeReady(SelectNotifyOperation.OP_NOTIFY)
            }?.readyOps(SelectNotifyOperation.OP_NOTIFY)
            key.takeIf {
                it.canMakeReady(SelectNotifyOperation.OP_CLOSE)
            }?.readyOps(SelectNotifyOperation.OP_CLOSE)

            selector.selectNow()
            ticks++
            takeUnless { selector.isOpen() }?.let {
                cancel()
            }
        }.join()
        println("Ticks: $ticks")
    }
}