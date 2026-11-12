package com.bugsy.core.model

data class DeviceInfo(
    val osName: String,
    val osVersion: String,
    val deviceModel: String,
    val manufacturer: String,
    val appVersionName: String,
    val appVersionCode: String,
    val appPackageId: String,
    val screenResolution: String,
    val locale: String,
    val timezone: String
)
