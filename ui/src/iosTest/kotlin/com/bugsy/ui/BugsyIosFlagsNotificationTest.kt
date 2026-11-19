package com.bugsy.ui

import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.model.DebugFlag
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSince1970
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A Swift host app learns that a flag changed (from the overlay or Bugsy Desktop) through a notification (flags change on the phone). */
class BugsyIosFlagsNotificationTest {
    private fun observe(onKey: (String?) -> Unit): Any =
        NSNotificationCenter.defaultCenter.addObserverForName(BUGSY_FLAGS_DID_CHANGE_NOTIFICATION, null, NSOperationQueue.mainQueue) { notification ->
            onKey(notification?.userInfo?.get("key") as? String)
        }

    @Test
    fun `the notification says which flag changed`() {
        var key: String? = null
        val observer = observe { key = it }
        try {
            postFlagsDidChange("darkModeEnabled")
            spinUntil { key != null }
            assertEquals("darkModeEnabled", key)
        } finally {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    @Test
    fun `a flag changed through the flag manager reaches the host app`() {
        Bugsy.initialize(BugsyConfig(flagsProvider = { listOf(DebugFlag(key = "darkModeEnabled", label = "darkModeEnabled", value = false, isOverridden = false)) }))
        var key: String? = null
        val observer = observe { key = it }
        try {
            startBugsyIosOverlay()
            // Changed straight away, with no wait after the start: the subscription has to exist already.
            Bugsy.flagManager.override("darkModeEnabled", true)
            spinUntil { key != null }
            assertEquals("darkModeEnabled", key)
        } finally {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            Bugsy.destroy()
        }
    }

    @Test
    fun `nothing is posted before a flag changes`() {
        var key: String? = null
        val observer = observe { key = it }
        try {
            spinUntil(seconds = 0.3) { false }
            assertNull(key)
        } finally {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    /** Lets the main queue run (the notification is delivered there) until [done] or the time is up. */
    private fun spinUntil(seconds: Double = 5.0, done: () -> Boolean) {
        val deadline = NSDate().timeIntervalSince1970 + seconds
        while (!done() && NSDate().timeIntervalSince1970 < deadline) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.05))
        }
    }
}
