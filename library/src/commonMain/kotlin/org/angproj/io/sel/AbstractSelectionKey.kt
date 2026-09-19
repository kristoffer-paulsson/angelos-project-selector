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

    override fun selector(): AbstractSelector = selector

    override fun item(): SelectableItem = item

    override fun attach(obj: A) {
        check(attachment == null) { "Attachment already set" }
        attachment = obj
    }

    override fun attachment(): A = attachment ?: throw IllegalStateException("No attachment for selection key")

    override fun clearOps(vararg ops: E) {
        val mask = (ops.toSet().sumOf { it.toInt() }).inv()
        _interestOps = _interestOps and mask
        _readyOps = _readyOps and mask
    }

    override fun interestOps(): Int {
        ensureValid()
        return _interestOps
    }

    override fun interestOps(vararg ops: E): AbstractSelectionKey<A, E> {
        ensureValid()
        require(ops.isNotEmpty()) { "Not interested in any operations" }
        _interestOps = ops.toSet().sumOf { it.toInt() } or _interestOps
        return this
    }

    override fun readyOps(): Int {
        ensureValid()
        return _readyOps
    }

    override fun readyOps(op: E): AbstractSelectionKey<A, E> {
        ensureValid()
        check(canMakeReady(op)) { "Can't make ready" }
        _readyOps = op.toInt() or _readyOps
        return this
    }

    override fun isHandleable(op: E): Boolean = (_readyOps and op.toInt()) != 0

    override fun isInterested(op: E): Boolean = (_interestOps and op.toInt()) != 0

    override fun canMakeReady(op: E): Boolean = isInterested(op) && !isHandleable(op)

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