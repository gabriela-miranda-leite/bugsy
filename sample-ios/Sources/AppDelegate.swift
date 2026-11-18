import UIKit
import Bugsy

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Precisa rodar antes de qualquer URLSession ser criada.
        Bugsy.shared.initialize(
            config: BugsyConfig(
                trigger: .shake,
                shakeThresholdG: BugsyConfig.companion.DEFAULT_SHAKE_THRESHOLD_G,
                // Flags estáticas só para validar o FlagsScreen no app de exemplo — num app real isso viria
                // do provedor de feature flags de verdade (remote config, etc).
                flagsProvider: {
                    [
                        DebugFlag(key: "darkModeEnabled", label: "darkModeEnabled", value: false, isOverridden: false, defaultValue: false),
                        DebugFlag(key: "featureNewCheckout", label: "featureNewCheckout", value: true, isOverridden: false, defaultValue: true),
                        DebugFlag(key: "apiEnvironment", label: "apiEnvironment", value: "sandbox", isOverridden: false, defaultValue: "sandbox"),
                    ]
                },
                allowRevealingSecrets: true,
                // Dados estáticos de exemplo pra seção "Account" da aba Device — num app real isso viria
                // da sessão logada (usuário atual, conta, etc).
                customInfoProvider: {
                    [
                        KotlinPair(first: "user_id", second: "u_42"),
                        KotlinPair(first: "email", second: "dev@bugsy.dev"),
                        KotlinPair(first: "document", second: "123.456.789-00"),
                    ]
                },
                slowRequestThresholdMs: BugsyConfig.companion.DEFAULT_SLOW_REQUEST_THRESHOLD_MS
            )
        )

        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window

        BugsyIosOverlayKt.startBugsyIosOverlay()
        return true
    }
}
