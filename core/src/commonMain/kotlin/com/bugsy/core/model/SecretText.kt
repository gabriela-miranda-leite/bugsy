package com.bugsy.core.model

/**
 * Texto que contém valores reais de campos sensíveis. Só existe em memória e nunca aparece em
 * `toString()` (logs, depuradores, `println` de um [DebugRequest]).
 */
class SecretText(val value: String) {
    override fun toString(): String = "***"
    override fun equals(other: Any?): Boolean = other is SecretText && other.value == value
    override fun hashCode(): Int = value.hashCode()
}
