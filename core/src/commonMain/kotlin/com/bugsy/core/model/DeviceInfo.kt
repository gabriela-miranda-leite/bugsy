package com.bugsy.core.model

data class DeviceInfo(
    val appVersionName: String,
    val appVersionCode: String,
    val environment: String,
    val buildType: String,
    val deviceModel: String,
    val osName: String,
    val osVersion: String,
    val locale: String,
    val bugsyVersion: String,
    val kotlinVersion: String,
    val composeVersion: String,
)
