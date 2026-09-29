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

import kotlinx.coroutines.sync.Mutex
import org.angproj.io.sel.AbstractSelectionKey
import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.Closeable
import org.angproj.io.sel.SelectOperation
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

        private val mutex: Mutex = Mutex()

        override suspend fun poll(): Int {
            val numCancelled = cleanCancelled()
            val numChanged = selectChanged()
            val numInvoked = if(numChanged > 0) invokeSelected() else 0
            wakeupReceived()
            return numInvoked
        }

        override fun pollReadyCountImpl(cancelledCount: Int, timeout: Long): Int = 0

        override fun provider(): SelectorProvider = this@Driver

        override fun select(timeout: Duration): Int {
            var selectCount = 0
            schedule(timeout) {
                selectCount = poll()
            }
            return selectCount
        }

        override fun selectNow(): Int {
            var selectCount = 0
            task { selectCount = poll() }
            return selectCount
        }

        override suspend fun wakeup(): Selector {
            if(mutex.isLocked)
                mutex.unlock()
            return this
        }

        override suspend fun wakeupReceived() {
            mutex.lock()
        }

        override suspend fun implCleanCancelled(
            key: AbstractSelectionKey<*, *>,
            altCnt: Int
        ): Int { return altCnt}

        override suspend fun implCloseSelector() {}

        override suspend fun <A : Closeable, E : SelectOperation<*>> deregisterImpl(
            key: AbstractSelectionKey<A, E>
        ) {}

        override suspend fun <A : Closeable, E : SelectOperation<*>> registerImpl(
            key: AbstractSelectionKey<A, E>
        ) {}
    }

    public fun openPipe() {

    }
}