package com.bugsy.core

import com.bugsy.core.model.MockMatch
import com.bugsy.core.model.MockRule
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory mock rules the interceptor checks before letting a request hit the network. Rules persist for the
 * app session only (cleared on process death, never on disk).
 */
enum class MockRuleChangeAction { ADDED, UPDATED, DELETED }

/** One [MockEngine.ruleChanges] event: [rule] in its state *after* [action] (its last known state for DELETED). */
data class MockRuleChange(val rule: MockRule, val action: MockRuleChangeAction)

object MockEngine {
    /** `false` here; the `:bugsy-noop` module's `MockEngine` reports `true` so the UI can hide the Mocks tab. */
    val isNoop: Boolean = false

    private const val MAX_MATCHES_PER_RULE = 20

    private val mutex = Mutex()
    private val rules = mutableListOf<MockRule>()
    private val matchLogs = mutableMapOf<String, MutableList<MockMatch>>()

    private val _rules = MutableStateFlow<List<MockRule>>(emptyList())
    val rulesFlow: StateFlow<List<MockRule>> = _rules

    private val _matchLogs = MutableStateFlow<Map<String, List<MockMatch>>>(emptyMap())
    val matchLogsFlow: StateFlow<Map<String, List<MockMatch>>> = _matchLogs

    private val _oneShotConsumed = MutableSharedFlow<MockRule>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** Emits the rule each time a one-shot fires and gets removed — the UI toasts "Mock used up" off of this. */
    val oneShotConsumedEvents: SharedFlow<MockRule> = _oneShotConsumed

    private val _ruleChanges = MutableSharedFlow<MockRuleChange>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** Emits on every add/update/delete — the BugsyWsServer (DEB-58) broadcasts this as `mock_rule_changed`. */
    val ruleChanges: SharedFlow<MockRuleChange> = _ruleChanges

    fun addRule(rule: MockRule) {
        val isUpdate = runBlocking {
            mutex.withLock {
                val isUpdate = rules.any { it.id == rule.id }
                rules.removeAll { it.id == rule.id }
                rules.add(rule)
                _rules.value = rules.toList()
                isUpdate
            }
        }
        _ruleChanges.tryEmit(MockRuleChange(rule, if (isUpdate) MockRuleChangeAction.UPDATED else MockRuleChangeAction.ADDED))
    }

    fun removeRule(id: String) {
        val removed = runBlocking {
            mutex.withLock {
                val removed = rules.firstOrNull { it.id == id }
                rules.removeAll { it.id == id }
                _rules.value = rules.toList()
                removed
            }
        }
        if (removed != null) _ruleChanges.tryEmit(MockRuleChange(removed, MockRuleChangeAction.DELETED))
    }

    fun setActive(id: String, active: Boolean) {
        val updated = runBlocking {
            mutex.withLock {
                val index = rules.indexOfFirst { it.id == id }
                if (index >= 0) {
                    rules[index] = rules[index].copy(isActive = active)
                    _rules.value = rules.toList()
                    rules[index]
                } else {
                    null
                }
            }
        }
        if (updated != null) _ruleChanges.tryEmit(MockRuleChange(updated, MockRuleChangeAction.UPDATED))
    }

    fun clear() {
        runBlocking {
            mutex.withLock {
                rules.clear()
                matchLogs.clear()
                _rules.value = emptyList()
                _matchLogs.value = emptyMap()
            }
        }
    }

    /** The rule's recent matches, most recent first. Empty if it never fired or no longer exists. */
    fun matchesFor(id: String): List<MockMatch> = matchLogsFlow.value[id].orEmpty()

    /**
     * The first active rule matching [method] and [url], or `null` if none does (the caller should let the
     * request through to the real network). Bumps the rule's hit count and match log (logged against
     * [timestamp], since this module has no portable clock of its own — the caller already has one); a
     * one-shot rule is removed from [rulesFlow] after this (its final state, including the bump, is still
     * what's returned).
     */
    fun match(method: String, url: String, timestamp: Long): MockRule? {
        return runBlocking {
            mutex.withLock {
                val index = rules.indexOfFirst { it.matches(method, url) }
                if (index < 0) return@withLock null
                val fired = rules[index].copy(hitCount = rules[index].hitCount + 1)

                val log = matchLogs.getOrPut(fired.id) { mutableListOf() }
                log.add(0, MockMatch(url = url, timestamp = timestamp))
                while (log.size > MAX_MATCHES_PER_RULE) log.removeAt(log.lastIndex)
                _matchLogs.value = matchLogs.mapValues { it.value.toList() }

                if (fired.isOneShot) {
                    rules.removeAt(index)
                    _oneShotConsumed.tryEmit(fired)
                    _ruleChanges.tryEmit(MockRuleChange(fired, MockRuleChangeAction.DELETED))
                } else {
                    rules[index] = fired
                    _ruleChanges.tryEmit(MockRuleChange(fired, MockRuleChangeAction.UPDATED))
                }
                _rules.value = rules.toList()
                fired
            }
        }
    }

    private fun MockRule.matches(requestMethod: String, requestUrl: String): Boolean {
        if (!isActive) return false
        if (method != null && !method.equals(requestMethod, ignoreCase = true)) return false
        return try {
            // Patterns are authored against the path (e.g. "/api/v1/catalog", "^/put$"), same as what the UI
            // shows and pre-fills — not the full URL, which an anchored pattern could never match (it doesn't
            // start with "/", it starts with the scheme).
            Regex(urlPattern).containsMatchIn(requestUrl.pathOnly())
        } catch (e: Exception) {
            false
        }
    }

    /** "https://api.bugsy.dev/v1/users/42?x=1" -> "/v1/users/42" (scheme, host and query stripped). */
    private fun String.pathOnly(): String {
        val afterScheme = substringAfter("://", missingDelimiterValue = this)
        val withoutHost = afterScheme.substringAfter('/', missingDelimiterValue = "").let { if (it.isEmpty()) "/" else "/$it" }
        return withoutHost.substringBefore('?')
    }
}
