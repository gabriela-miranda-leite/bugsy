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
                // Flags estáticas só para validar o FlagsScreen no app de exemplo — num app real isso viria
                // do provedor de feature flags de verdade (remote config, etc).
                flagsProvider = {
                    listOf(
                        DebugFlag(key = "darkModeEnabled", label = "darkModeEnabled", value = false, isOverridden = false),
                        DebugFlag(key = "featureNewCheckout", label = "featureNewCheckout", value = true, isOverridden = false),
                        DebugFlag(key = "apiEnvironment", label = "apiEnvironment", value = "sandbox", isOverridden = false),
                    )
                },
                // Dados estáticos de exemplo pra seção "Account" da aba Device — num app real isso viria
                // da sessão logada (usuário atual, conta, etc).
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
