/**
 * Copyright (c) 2025 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class Dispenser<T>(private val dispensable: T) {

    private val mutex = Mutex()

    public suspend fun dispense(sync: (suspend (T) -> Unit)): Unit = mutex.withLock(dispensable) {
        sync(dispensable)
    }
}

public fun task(action: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(Dispatchers.Default).launch {
    action()
}.apply { start() }