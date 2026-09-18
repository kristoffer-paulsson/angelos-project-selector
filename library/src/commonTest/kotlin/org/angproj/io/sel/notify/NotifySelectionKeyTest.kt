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

            val key = selector.register(selectable, SelectNotifyOperation.OP_NOTIFY, SelectNotifyOperation.OP_CLOSE, attachment = attachment) {
                NotifySelectionKey(this, it) {
                }
            } as NotifySelectionKey

            assertEquals(key.interestOps(), SelectNotifyOperation.entries.sumOf { it.toInt() })
            key.resetOps()
            assertEquals(0, key.interestOps())
            assertFailsWith<IllegalStateException> {
                key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            }
            key.interestOps(SelectNotifyOperation.OP_NOTIFY)
            assertTrue { key.isInterested(SelectNotifyOperation.OP_NOTIFY) }
            assertTrue { key.canMakeReady(SelectNotifyOperation.OP_NOTIFY) }
            assertFalse { key.canMakeReady(SelectNotifyOperation.OP_CLOSE) }
            assertFalse { key.isHandleable(SelectNotifyOperation.OP_NOTIFY) }
            key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            assertTrue { key.isHandleable(SelectNotifyOperation.OP_NOTIFY) }
            assertFailsWith<IllegalStateException> {
                key.readyOps(SelectNotifyOperation.OP_NOTIFY)
            }
            key.interestOps(SelectNotifyOperation.OP_CLOSE)
            assertTrue { key.isInterested(SelectNotifyOperation.OP_CLOSE) }
            key.readyOps(SelectNotifyOperation.OP_CLOSE)
            assertTrue { key.isHandleable(SelectNotifyOperation.OP_CLOSE) }
        }.join()
    }
}