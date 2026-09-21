/**
 * Copyright (c) 2025-2026 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
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
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield

public abstract class Awakable<E> {

    protected val mutex: Mutex = Mutex()

    protected suspend fun sleep(): Unit = mutex.lock()

    /**
     * The coroutine job representing the implementation's execution.
     *
     * This job can be used to manage the lifecycle of the waitress, such as cancellation or monitoring its status.
     */
    public abstract val job: Job

    /**
     * Wakes up the coroutine, allowing it to execute its action.
     *
     * This method signals the coroutine to perform its action, which is typically defined in the
     * implementation of the coroutine. The action may be executed once for each wake-up event.
     *
     * @return Some result of the wake-up performed.
     */
    public abstract fun wakeUp(): E
}

public abstract class Steward(action: suspend CoroutineScope.() -> Unit) : Awakable<Int>() {
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

    /**
     * Signals the steward to wake up and execute its action.
     *
     * Each call increments the internal wake-up counter. The steward will execute its action
     * once for each registered wake-up event, ensuring all events are processed in order.
     *
     * @return The current count of remaining wake-up events after this call.
     */
    override fun wakeUp(): Int {
        counter++
        if(mutex.isLocked)
            mutex.unlock()
        return counter
    }
}

public fun attend(action: suspend CoroutineScope.() -> Unit): Steward = object : Steward(action) {

}

public abstract class Waitress(action: suspend CoroutineScope.() -> Unit) : Awakable<Boolean>() {

    override val job: Job = CoroutineScope(Dispatchers.Default).async {
        while (isActive) {
            sleep()
            action()
            yield()
        }
    }

    init { job.start() }

    /**
     * Signals the waitress to wake up and execute its action.
     *
     * If the waitress is dormant, this call will wake it up and allow it to perform its action.
     * If the waitress is already awake and processing, the call is ignored and returns `false`.
     *
     * @return `true` if the waitress was successfully woken up, `false` if it was already awake.
     */
    override fun wakeUp(): Boolean = when(mutex.isLocked) {
        false -> false
        else -> { mutex.unlock(); true }
    }
}

public fun answer(action: suspend CoroutineScope.() -> Unit): Waitress = object : Waitress(action) {

}