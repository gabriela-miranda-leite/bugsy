package com.bugsy.core.model

data class DebugFlag(
    val key: String,
    val label: String,
    val value: Any,
    val isOverridden: Boolean
)
