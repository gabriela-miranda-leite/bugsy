package com.bugsy.core

import android.os.Build
import java.util.Locale

internal actual fun readPlatformDeviceInfo(): PlatformDeviceInfo {
    val context = BugsyContext.applicationContext
    val packageInfo = context?.let { ctx ->
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
    }
    val versionCode = packageInfo?.let {
        if (Build.VERSION.SDK_INT >= 28) it.longVersionCode.toString() else @Suppress("DEPRECATION") it.versionCode.toString()
    }
    return PlatformDeviceInfo(
        appVersionName = packageInfo?.versionName ?: UNKNOWN_INFO,
        appVersionCode = versionCode ?: UNKNOWN_INFO,
        buildType = if (context == null) UNKNOWN_INFO else if (BugsyContext.isHostDebuggable()) "debug" else "release",
        deviceModel = androidDeviceModel(Build.MANUFACTURER, Build.MODEL),
        osName = "Android",
        osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        locale = Locale.getDefault().toLanguageTag(),
    )
}

/** "Pixel 8" already starts with its maker on some devices ("Google Pixel 8"), and not on others ("SM-S918B"). */
internal fun androidDeviceModel(manufacturer: String?, model: String?): String {
    val maker = manufacturer?.trim().orEmpty()
    val name = model?.trim().orEmpty()
    return when {
        name.isEmpty() -> maker.ifEmpty { UNKNOWN_INFO }
        maker.isEmpty() || name.startsWith(maker, ignoreCase = true) -> name
        else -> "${maker.replaceFirstChar { it.uppercase() }} $name"
    }
}
