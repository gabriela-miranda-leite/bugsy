import UIKit
import Bugsy

final class ViewController: UIViewController {
    private let statusLabel = UILabel()
    private let flagsLabel = UILabel()

    override func viewDidLoad() {
        super.viewDidLoad()

        let requestButton = UIButton(type: .system)
        requestButton.setTitle("Fazer request (URLSession.shared)", for: .normal)
        requestButton.addTarget(self, action: #selector(makeRequest), for: .touchUpInside)

        let showButton = UIButton(type: .system)
        showButton.setTitle("Abrir Bugsy", for: .normal)
        showButton.addTarget(self, action: #selector(showBugsy), for: .touchUpInside)

        statusLabel.text = "Nenhuma request ainda"
        statusLabel.textAlignment = .center

        flagsLabel.textAlignment = .center
        refreshDarkMode()

        let stack = UIStackView(arrangedSubviews: [requestButton, showButton, statusLabel, flagsLabel])
        stack.axis = .vertical
        stack.spacing = 16
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
        ])
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // O Bugsy é apresentado como um controller sobre este (presentViewController), então
        // voltamos aqui (viewDidAppear) quando o usuário fecha o overlay — é o ponto certo
        // para reler a flag e provar que o toggle do FlagsScreen mudou o app de verdade.
        refreshDarkMode()
        // Modo automático para validar a integração sem interação: BUGSY_SAMPLE_AUTORUN=1
        guard ProcessInfo.processInfo.environment["BUGSY_SAMPLE_AUTORUN"] == "1" else { return }
        makeRequest()
        let simulateShake = ProcessInfo.processInfo.environment["BUGSY_SAMPLE_SHAKE"] == "1"
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            if simulateShake {
                // Mesmo método que o sistema chama na janela quando o aparelho é sacudido (Device > Shake).
                self.view.window?.motionEnded(.motionShake, with: nil)
            } else {
                Bugsy.shared.show()
            }
        }
    }

    @objc private func makeRequest() {
        var request = URLRequest(url: URL(string: "https://httpbin.org/get")!)
        request.setValue("Bearer sample-secret", forHTTPHeaderField: "Authorization")
        URLSession.shared.dataTask(with: request) { [weak self] _, response, error in
            DispatchQueue.main.async {
                let code = (response as? HTTPURLResponse)?.statusCode
                self?.statusLabel.text = code.map { "Status \($0)" } ?? (error?.localizedDescription ?? "Falhou")
            }
        }.resume()
    }

    @objc private func showBugsy() {
        Bugsy.shared.show()
    }

    private func refreshDarkMode() {
        // Prova de ponta a ponta de que o override do FlagsScreen chega até o app: ao ligar
        // "darkModeEnabled" no Bugsy, a tela vira preta sem precisar fechar e reabrir o app.
        let isOn = (Bugsy.shared.flagManager.value(key: "darkModeEnabled") as? KotlinBoolean)?.boolValue ?? false
        view.backgroundColor = isOn ? .black : .white
        let foreground: UIColor = isOn ? .white : .black
        statusLabel.textColor = foreground
        flagsLabel.textColor = foreground
        flagsLabel.text = isOn ? "darkModeEnabled: ON" : "darkModeEnabled: off"
    }
}
