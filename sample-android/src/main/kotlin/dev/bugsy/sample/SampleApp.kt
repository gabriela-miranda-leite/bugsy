package dev.bugsy.sample

import android.app.Application
import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.Trigger
import com.bugsy.core.model.DebugFlag

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Bugsy.initialize(
            BugsyConfig(
                trigger = Trigger.Notification,
                // Static flags just to validate the FlagsScreen in the sample app — in a real app this would come
                // from the actual feature flag provider (remote config, etc).
                flagsProvider = {
                    listOf(
                        DebugFlag(key = "darkModeEnabled", label = "darkModeEnabled", value = false, isOverridden = false),
                        DebugFlag(key = "featureNewCheckout", label = "featureNewCheckout", value = true, isOverridden = false),
                        DebugFlag(key = "apiEnvironment", label = "apiEnvironment", value = "sandbox", isOverridden = false),
                    )
                },
                // Static sample data for the "Account" section of the Device tab — in a real app this would come
                // from the logged-in session (current user, account, etc).
                customInfoProvider = {
                    listOf(
                        "user_id" to "u_42",
                        "email" to "dev@bugsy.dev",
                        "document" to "123.456.789-00",
                    )
                },
            ),
        )
    }
}
