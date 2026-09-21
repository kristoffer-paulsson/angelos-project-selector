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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.toDuration

public class Dispenser<T>(private val dispensable: T) {

    private val mutex = Mutex()

    public suspend fun dispense(sync: (suspend (T) -> Unit)): Unit = mutex.withLock {
        sync(dispensable)
    }
}

public fun task(action: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(Dispatchers.Default).launch {
    action()
}.apply { start() }

public fun clock(
    unit: DurationUnit, ticks: Int, action: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(Dispatchers.Default).async {
    var start = TimeSource.Monotonic.markNow()
    var counter: Long = 0

    while (isActive) {
        action()

        counter++
        val elapsed = start.elapsedNow()

        if(counter == Long.MAX_VALUE) {
            start += elapsed
            counter = 0
        }

        delay((counter.toDouble() / ticks).toDuration(unit) - elapsed)
    }
}.apply { start() }.job

public fun loop(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(Dispatchers.Default).async {
    do {
        block()
        yield()
    } while (isActive)
}.apply { start() }.job

public fun schedule(
    inTime: Duration, action: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(Dispatchers.Default).launch {
    delay(inTime)
    if(isActive) action()
}.apply { start() }

public fun call(
    every: DurationUnit, monitor: Job, action: suspend CoroutineScope.(Duration) -> Unit
): Job = CoroutineScope(Dispatchers.Default).async {
    var start = TimeSource.Monotonic.markNow()
    var counter: Long = 0

    while (isActive) {
        counter++
        val elapsed = start.elapsedNow()

        if(counter == Long.MAX_VALUE) {
            start += elapsed
            counter = 0
        }

        delay(counter.toDuration(every) - elapsed)

        when {
            monitor.isCancelled -> action(elapsed).also { this@async.cancel() }
            monitor.isCompleted -> this@async.cancel()
        }
    }
}.apply { start() }.job