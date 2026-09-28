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

import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.Selector
import org.angproj.io.sel.SelectorProvider
import kotlin.time.Duration

public object Driver : SelectorProvider {

    private val innerSelector: AbstractSelector by lazy {
        buildSelector()
    }

    override fun openSelector(): AbstractSelector {
        return innerSelector
    }

    private fun buildSelector(): AbstractSelector = object : AbstractSelector() {

        override fun provider(): SelectorProvider = this@Driver

        override fun select(timeout: Duration): Int {
            var selectCount = 0
            schedule(timeout) {
                selectCount = doWakeUp()
            }
            return selectCount
        }

        override fun selectNow(): Int {
            var selectCount = 0
            task { selectCount = doWakeUp() }
            return selectCount
        }

        private suspend fun doWakeUp(): Int {
            readySelector()
            var selectCount = 0
            selectedKeys { keys -> selectCount = keys.size }
            wakeup()
            selectCount -= cleanCancelled()
            return selectCount
        }

        private suspend fun cleanCancelled(): Int {
            var cancelledCount = 0
            cancelledKeys { keys ->
                cancelledCount -= keys.size
                keys.clear()
            }
            return cancelledCount
        }

        override suspend fun wakeup(): Selector {
            selectedKeys { selKeys ->
                val keyIter = selKeys.iterator()
                while (keyIter.hasNext()) {
                    val key = keyIter.next()
                    selKeys.remove(key)
                    when(key.isValid()) {
                        true -> key.doHandle()
                        else -> cancelledKeys { cancelledKeys -> cancelledKeys.add(key) }
                    }
                }
            }
            return this
        }

        override suspend fun implCloseSelector() {
            /*keys { keys ->
                keys.forEach { key ->
                    key.takeIf { it.isValid() }?.cancel()
                    keys.remove(key)
                }
            }
            cancelledKeys { keys ->
                keys.clear()
            }*/
        }

        private suspend fun readySelector(): Int {
            var readyCount = 0
            keys { keys ->
                keys.forEach { key ->
                    if(key.readyOps() != 0 && key.isIdle()) {
                        selectedKeys { keys -> keys.add(key) }
                        readyCount++
                    }
                }
            }
            return readyCount
        }
    }

    public fun openPipe() {

    }
}