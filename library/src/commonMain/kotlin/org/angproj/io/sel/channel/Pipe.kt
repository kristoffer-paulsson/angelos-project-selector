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
package org.angproj.io.sel.channel

import kotlinx.coroutines.CoroutineScope
import org.angproj.io.sel.AbstractSelector
import org.angproj.io.sel.driver.Driver
import org.angproj.io.sel.driver.Steward

public class Pipe(selector: AbstractSelector = Driver.openSelector()): SelectableChannel(selector) {

    override suspend fun <A> openImpl(attachment: A): ChannelSelectionKey<A> {
        return selector.register(this, SelectChannelOperation.OP_WRITE, attachment = attachment) {
            ChannelSelectionKey(selector, it) {
                when{
                    isHandleable(SelectChannelOperation.OP_WRITE) -> {
                        clearOps(SelectChannelOperation.OP_WRITE)
                        sinkChannel().wakeUp()
                        interestOps(SelectChannelOperation.OP_READ)
                    }
                    isHandleable(SelectChannelOperation.OP_READ) -> {
                        clearOps(SelectChannelOperation.OP_READ)
                        sourceChannel().wakeUp()
                        interestOps(SelectChannelOperation.OP_WRITE)
                    }
                    else -> {
                        clearOps(SelectChannelOperation.OP_READ)
                        clearOps(SelectChannelOperation.OP_WRITE)
                        cancel()
                    }
                }
            }
        } as ChannelSelectionKey
    }

    override fun closeImpl() {
        if(::_source.isInitialized) { sourceChannel().job.cancel() }
        if(::_sink.isInitialized) { sinkChannel().job.cancel() }
    }

    private lateinit var _source: Source
    public fun sourceChannel(action: suspend CoroutineScope.() -> Unit): Source {
        check(!::_source.isInitialized && isOpen) { "Pipe has already been initialized" }
        _source = Source(_key, action)
        return _source
    }

    public fun sourceChannel(): Source {
        check(::_source.isInitialized) { "Pipe must be initialized" }
        return _source
    }

    private lateinit var _sink: Sink
    public fun sinkChannel(action: suspend CoroutineScope.() -> Unit): Sink {
        check(!::_sink.isInitialized && isOpen) { "Pipe has already been initialized" }
        _sink = Sink(_key, action)
        return _sink
    }

    public fun sinkChannel(): Sink {
        check(::_sink.isInitialized) { "Pipe must be initialized" }
        return _sink
    }
}

public abstract class Channel(
    protected val _key: ChannelSelectionKey<*>,
    action: suspend CoroutineScope.() -> Unit
) : Steward(action)

public class Sink(
    key: ChannelSelectionKey<*>,
    action: suspend CoroutineScope.() -> Unit
): Channel(key, action) {
    public fun notifyRead() {
        _key.readyOps(SelectChannelOperation.OP_READ)
    }
}

public class Source(
    key: ChannelSelectionKey<*>,
    action: suspend CoroutineScope.() -> Unit
): Channel(key, action) {
    public fun notifyWrite() {
        _key.readyOps(SelectChannelOperation.OP_WRITE)
    }
}