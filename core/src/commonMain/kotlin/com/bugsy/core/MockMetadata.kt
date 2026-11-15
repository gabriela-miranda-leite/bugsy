package com.bugsy.core

/** Keys [MockEngine] writes into a mocked [com.bugsy.core.model.DebugRequest]'s `metadata`, read by the UI to tag it. */
object MockMetadata {
    const val KEY_MOCKED = "mocked"
    const val KEY_ONE_SHOT = "mockOneShot"
    const val VALUE_TRUE = "true"
}
