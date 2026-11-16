package com.bugsy.core

import com.bugsy.core.model.MockMatch
import com.bugsy.core.model.MockRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class MockRuleChangeAction { ADDED, UPDATED, DELETED }

data class MockRuleChange(val rule: MockRule, val action: MockRuleChangeAction)

/**
 * Same API as the debug artifact's `MockEngine`, with no behavior: there are never any rules, so every request
 * always falls through to the real network.
 */
object MockEngine {
    val isNoop: Boolean = true

    val rulesFlow: StateFlow<List<MockRule>> = MutableStateFlow(emptyList())
    val matchLogsFlow: StateFlow<Map<String, List<MockMatch>>> = MutableStateFlow(emptyMap())
    val oneShotConsumedEvents: SharedFlow<MockRule> = MutableSharedFlow()
    val ruleChanges: SharedFlow<MockRuleChange> = MutableSharedFlow()

    fun addRule(rule: MockRule) = Unit

    fun removeRule(id: String) = Unit

    fun setActive(id: String, active: Boolean) = Unit

    fun clear() = Unit

    fun matchesFor(id: String): List<MockMatch> = emptyList()

    fun match(method: String, url: String, timestamp: Long): MockRule? = null
}
