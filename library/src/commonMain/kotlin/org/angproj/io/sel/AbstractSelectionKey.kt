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
package org.angproj.io.sel

import org.angproj.io.sel.driver.task


/**
 * A fully functional abstract base for selection keys, using [SelectOperation] for operation sets.
 */
public abstract class AbstractSelectionKey<A, E : SelectOperation<*>>(
    protected val selector: AbstractSelector,
    protected val item: SelectableItem,
    protected val handler: suspend AbstractSelectionKey<A, E>.() -> Unit
) : SelectionKey<A, E> {

    private var attachment: A? = null

    private var _interestOps: Int = 0

    private var _readyOps: Int = 0

    private var _valid: Boolean = true

    override fun doHandle() {
        task { handler() }
    }

    override fun selector(): Selector = selector

    override fun item(): SelectableItem = item

    override fun attach(obj: A) {
        attachment = obj
    }

    override fun attachment(): A = attachment ?: throw IllegalStateException("No attachment for selection key")

    override fun interestOps(): Int {
        ensureValid()
        return _interestOps
    }

    override fun interestOps(vararg ops: E): AbstractSelectionKey<A, E> {
        ensureValid()
        require(ops.isNotEmpty()) { "Invalid interest ops for channel" }
        _interestOps = ops.sumOf { it.toInt() }
        return this
    }

    override fun readyOps(): Int {
        ensureValid()
        return _readyOps
    }

    /**
     * Sets the ready operations. Should be called by the selector implementation.
     */
    protected fun setReadyOps(ops: Int) {
        _readyOps = ops
    }

    override fun isHandleable(op: E): Boolean = (readyOps() and op.toInt()) != 0

    override fun isValid(): Boolean = _valid

    override fun cancel() {
        if (_valid) {
            _valid = false
            task { selector.deregister(this@AbstractSelectionKey) }
        }
    }

    private fun ensureValid() {
        if (!_valid) throw CancelledKeyException()
    }
}