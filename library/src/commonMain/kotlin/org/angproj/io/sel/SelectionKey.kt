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

/**
 * A token representing the registration of a [SelectableItem] with a [Selector].
 *
 * A selection key is created each time a channel is registered with a selector.
 * It remains valid until it is cancelled, the channel is closed, or the selector is closed.
 *
 * A selection key contains:
 * - The selector with which it is registered
 * - The channel for which it was created
 * - The interest set, which determines the operations for which the channel will be tested
 * - The ready set, which identifies the operations for which the channel is ready
 * - An optional attachment object
 *
 * Selection keys are thread-safe and may be used by multiple threads.
 */
public interface SelectionKey<A, E: SelectOperation<*>> {

    /**
     * Returns the selector with which this key is registered.
     */
    public fun selector(): Selector

    /**
     * Attaches the given object to this key.
     *
     * @param obj the object to attach; may be null
     */
    public fun attach(obj: A)

    /**
     * Retrieves the current attachment.
     *
     * @return the attached object, or null if none
     */
    public fun attachment(): A

    /**
     * Returns the channel for which this key was created.
     */
    public fun item(): SelectableItem

    /**
     * Retrieves this key's interest set.
     *
     * @return the interest set
     */
    public fun interestOps(): Int

    /**
     * Sets this key's interest set.
     *
     * @param ops the new interest set
     * @return this selection key
     */
    public fun interestOps(ops: Int): SelectionKey<A, E>

    /**
     * Retrieves this key's ready set.
     *
     * @return the ready set
     */
    public fun readyOps(): Int

    /**
     *
     */
    public fun isHandleable(op: E): Boolean

    /**
     * Tells whether this key's channel is ready to accept a new socket connection.
     *
     * @return true if acceptable
     */
    public fun isAcceptable(): Boolean

    /**
     * Tells whether this key's channel is ready to complete its connection sequence.
     *
     * @return true if connectable
     */
    public fun isConnectable(): Boolean

    /**
     * Tells whether this key's channel is ready for reading.
     *
     * @return true if readable
     */
    public fun isReadable(): Boolean

    /**
     * Tells whether this key's channel is ready for writing.
     *
     * @return true if writable
     */
    public fun isWritable(): Boolean

    /**
     * Tells whether this key is valid.
     *
     * @return true if valid
     */
    public fun isValid(): Boolean

    /**
     * Cancels this key.
     */
    public fun cancel()
}