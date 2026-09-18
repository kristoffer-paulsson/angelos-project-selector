package org.angproj.io.sel.notify

import kotlinx.coroutines.test.runTest
import org.angproj.io.sel.Closeable
import org.angproj.io.sel.driver.Driver
import org.angproj.io.sel.driver.task
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class NotifySelectionKeyTest {

    class Attachment : Closeable {
        override fun close() {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun testAttachment() = runTest {
        task {
            val attachment = Attachment()
            val selectable = SelectableNotify()
            val selector = Driver.openSelector()

            val key = selector.register(selectable, SelectNotifyOperation.OP_NOTIFY, attachment = attachment) {
                NotifySelectionKey(this, it) {
                }
            } as NotifySelectionKey

            assertSame(selector, key.selector())
            assertSame(key.item(), selectable)
            assertSame(key.attachment(), attachment)

            assertFailsWith<IllegalStateException> {
                key.attach(Attachment())
            }
        }.join()
    }

    @Test
    fun testInterestOps() = runTest {
        task {
            val attachment = Attachment()
            val selectable = SelectableNotify()
            val selector = Driver.openSelector()

            // Both ops set in interest
            val key = selector.register(selectable, SelectNotifyOperation.OP_NOTIFY, SelectNotifyOperation.OP_CLOSE, attachment = attachment) {
                NotifySelectionKey(this, it) {
                }
            } as NotifySelectionKey

            // Assert both ops as set
            assertEquals(key.interestOps(), SelectNotifyOperation.entries.sumOf { it.toInt() })
            key.resetOps() // reset ops in interest and ready
            // Assert interest not set
            assertEquals(0, key.interestOps())

            assertFailsWith<IllegalStateException> {
                // assert exception when ready and not interested op
                key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            }
            // Set notify op on interest
            key.interestOps(SelectNotifyOperation.OP_NOTIFY)
            assertTrue { // Assert notify is on interest
                key.isInterested(SelectNotifyOperation.OP_NOTIFY) }
            assertTrue { // Assert notify can make ready
                key.canMakeReady(SelectNotifyOperation.OP_NOTIFY) }
            assertFalse {  // Assert close can not make ready
                key.canMakeReady(SelectNotifyOperation.OP_CLOSE) }
            assertFalse { // Assert notify is not ready
                key.isHandleable(SelectNotifyOperation.OP_NOTIFY) }
            // Set notify as ready
            key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            assertTrue { // Assert notify is ready
                key.isHandleable(SelectNotifyOperation.OP_NOTIFY) }
            assertFailsWith<IllegalStateException> {
                // Assert exception at notify when already ready
                key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            }
            // Set interest to close
            key.interestOps(SelectNotifyOperation.OP_CLOSE)
            assertTrue { // Assert close is in interest
                key.isInterested(SelectNotifyOperation.OP_CLOSE) }
            // Set ready to close
            key.readyOps(SelectNotifyOperation.OP_CLOSE)
            assertTrue { // Assert close is ready
                key.isHandleable(SelectNotifyOperation.OP_CLOSE) }
        }.join()
    }
}