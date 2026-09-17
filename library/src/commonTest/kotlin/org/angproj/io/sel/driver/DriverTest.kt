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

import org.angproj.io.sel.notify.NotifySelectionKey
import org.angproj.io.sel.notify.SelectNotifyOperation
import org.angproj.io.sel.notify.SelectableNotify
import kotlin.test.Test

class DriverTest {
    @Test
    fun testOpenSelector() {
        val selector = Driver.openSelector()
        var loop = 0

        val selectable = object {}

        val sn = SelectableNotify()
        task {
            val key = selector.register(sn, SelectNotifyOperation.OP_NOTIFY, attachment = selectable) {
                NotifySelectionKey(this, it) {
                    println("Hello, world!")
                }
            } as NotifySelectionKey
            key.interestOps(SelectNotifyOperation.OP_NOTIFY)
            key.postReadyOps(SelectNotifyOperation.OP_NOTIFY)
            selector.selectNow()
            selector.close()
        }

        /*while (selector.isOpen()) {
            key.interestOps(SelectNotifyOperation.OP_NOTIFY)
            key.postReadyOps(SelectNotifyOperation.OP_NOTIFY)
            //println(key.isNotifiable())

            /*suspend {
                selector.selectedKeys {
                    println(key in it)
                }
            }*/
            selector.selectNow()
            //println(key.isNotifiable())
            loop++
            if (loop >= 100) { selector.close() }
        }*/

    }
}