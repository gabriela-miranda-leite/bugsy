package com.bugsy.ui.theme

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeIntervalSince1970

private val Formatter = NSDateFormatter().apply {
    dateFormat = "HH:mm:ss"
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
}

// NSDate's own designated initializer counts seconds since 2001-01-01, not the Unix epoch.
actual fun formatLocalHms(epochMillis: Long): String =
    Formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochMillis / 1_000.0 - NSTimeIntervalSince1970))
