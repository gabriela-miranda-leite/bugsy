package com.bugsy.core.ws

import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** First frame sent by the desktop client, authenticating against the token shown in the mobile overlay. */
@Serializable
data class HandshakeMessage(val value: String)

/** Mobile -> desktop messages broadcast by the BugsyWsServer (DEB-58), discriminated by `type`. */
@Serializable
sealed class BugsyEvent {
    /** Sent once, right after a successful handshake, so a freshly connected desktop never starts out blank. */
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        val requests: List<DebugRequest>,
        val mockRules: List<MockRule>,
        val flags: List<DebugFlag>,
    ) : BugsyEvent()

    @Serializable
    @SerialName("request_recorded")
    data class RequestRecorded(val request: DebugRequest) : BugsyEvent()

    @Serializable
    @SerialName("mock_rule_changed")
    data class MockRuleChanged(val rule: MockRule, val action: String) : BugsyEvent()

    @Serializable
    @SerialName("flag_changed")
    data class FlagChanged(val flag: DebugFlag) : BugsyEvent()
}

/** Desktop -> mobile commands the BugsyWsServer (DEB-58) applies to the local `MockEngine`/`FlagManager`/`DebugRepository`. */
@Serializable
sealed class BugsyCommand {
    @Serializable
    @SerialName("add_mock_rule")
    data class AddMockRule(val rule: MockRule) : BugsyCommand()

    @Serializable
    @SerialName("toggle_mock_rule")
    data class ToggleMockRule(val id: String, val enabled: Boolean) : BugsyCommand()

    @Serializable
    @SerialName("delete_mock_rule")
    data class DeleteMockRule(val id: String) : BugsyCommand()

    @Serializable
    @SerialName("toggle_flag")
    data class ToggleFlag(val key: String, val enabled: Boolean) : BugsyCommand()

    @Serializable
    @SerialName("reset_flag")
    data class ResetFlag(val key: String) : BugsyCommand()

    @Serializable
    @SerialName("reset_all_flags")
    object ResetAllFlags : BugsyCommand()

    @Serializable
    @SerialName("clear_requests")
    object ClearRequests : BugsyCommand()
}
