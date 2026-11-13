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
                flagsProvider: { [] }
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
