package com.bugsy.ui.theme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Formatter = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.US) }

actual fun formatLocalHms(epochMillis: Long): String = Formatter.get()!!.format(Date(epochMillis))
