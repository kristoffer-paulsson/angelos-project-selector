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

public interface Awakable<E> {

    /**
     * The coroutine job representing the implementation's execution.
     *
     * This job can be used to manage the lifecycle of the waitress, such as cancellation or monitoring its status.
     */
    public val job: Job

    /**
     * Wakes up the coroutine, allowing it to execute its action.
     *
     * This method signals the coroutine to perform its action, which is typically defined in the
     * implementation of the coroutine. The action may be executed once for each wake-up event.
     *
     * @return Some result of the wake-up performed.
     */
    public fun wakeUp(): E
}

public interface Steward : Awakable<Int> {

    /**
     * Signals the steward to wake up and execute its action.
     *
     * Each call increments the internal wake-up counter. The steward will execute its action
     * once for each registered wake-up event, ensuring all events are processed in order.
     *
     * @return The current count of remaining wake-up events after this call.
     */
    override fun wakeUp(): Int
}

public interface Waitress : Awakable<Boolean> {

    /**
     * Signals the waitress to wake up and execute its action.
     *
     * If the waitress is dormant, this call will wake it up and allow it to perform its action.
     * If the waitress is already awake and processing, the call is ignored and returns `false`.
     *
     * @return `true` if the waitress was successfully woken up, `false` if it was already awake.
     */
    override fun wakeUp(): Boolean
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

public fun attend(action: suspend CoroutineScope.() -> Unit): Steward = object : Steward {
    private val mutex: Mutex = Mutex()
    private var counter = 0

    override val job: Job = CoroutineScope(Dispatchers.Default).async {
        while (isActive) {
            sleep()
            while(counter > 0) {
                counter--
                action()
            }
            yield()
        }
    }

    init { job.start() }

    private suspend fun sleep(): Unit = mutex.lock()
    override fun wakeUp(): Int {
        counter++
        if(mutex.isLocked)
            mutex.unlock()
        return counter
    }
}

public fun answer(action: suspend CoroutineScope.() -> Unit): Waitress = object : Waitress {
    private val mutex: Mutex = Mutex()

    override val job: Job = CoroutineScope(Dispatchers.Default).async {
        while (isActive) {
            sleep()
            action()
            yield()
        }
    }

    init { job.start() }

    private suspend fun sleep(): Unit = mutex.lock()
    override fun wakeUp(): Boolean = when(mutex.isLocked) {
        false -> false
        else -> { mutex.unlock(); true }
    }
}