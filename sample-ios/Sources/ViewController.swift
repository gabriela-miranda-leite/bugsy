import UIKit
import Bugsy

final class ViewController: UIViewController {
    private let statusLabel = UILabel()
    private let flagsLabel = UILabel()

    override func viewDidLoad() {
        super.viewDidLoad()

        let getButton = UIButton(type: .system)
        getButton.setTitle("GET", for: .normal)
        getButton.addTarget(self, action: #selector(makeGetRequest), for: .touchUpInside)

        let postButton = UIButton(type: .system)
        postButton.setTitle("POST", for: .normal)
        postButton.addTarget(self, action: #selector(makePostRequest), for: .touchUpInside)

        let putButton = UIButton(type: .system)
        putButton.setTitle("PUT", for: .normal)
        putButton.addTarget(self, action: #selector(makePutRequest), for: .touchUpInside)

        let deleteButton = UIButton(type: .system)
        deleteButton.setTitle("DELETE", for: .normal)
        deleteButton.addTarget(self, action: #selector(makeDeleteRequest), for: .touchUpInside)

        let showButton = UIButton(type: .system)
        showButton.setTitle("Abrir Bugsy", for: .normal)
        showButton.addTarget(self, action: #selector(showBugsy), for: .touchUpInside)

        statusLabel.text = "Nenhuma request ainda"
        statusLabel.textAlignment = .center

        flagsLabel.textAlignment = .center
        refreshDarkMode()

        // viewDidAppear não é confiável aqui: o overlay usa apresentação não-opaca
        // (UIModalPresentationOverFullScreen), e nesse caso o UIKit não garante
        // viewWillAppear/viewDidAppear nesta tela quando o overlay é fechado.
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(refreshDarkMode),
            name: NSNotification.Name(BugsyIosOverlayKt.BUGSY_OVERLAY_DID_DISMISS_NOTIFICATION),
            object: nil
        )

        let stack = UIStackView(arrangedSubviews: [getButton, postButton, putButton, deleteButton, showButton, statusLabel, flagsLabel])
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
        refreshDarkMode()
        // Modo automático para validar a integração sem interação: BUGSY_SAMPLE_AUTORUN=1
        guard ProcessInfo.processInfo.environment["BUGSY_SAMPLE_AUTORUN"] == "1" else { return }
        makeGetRequest()
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

    @objc private func makeGetRequest() {
        makeRequest(method: "GET", path: "get")
    }

    @objc private func makePostRequest() {
        makeRequest(method: "POST", path: "post")
    }

    @objc private func makePutRequest() {
        makeRequest(method: "PUT", path: "put")
    }

    @objc private func makeDeleteRequest() {
        makeRequest(method: "DELETE", path: "delete")
    }

    // Headers misturam sensíveis (Authorization, Cookie, X-Api-Key), mascarados por padrão pelo
    // Bugsy, com um comum (X-Device-Id) que deve aparecer sem alteração, pra exercitar os dois casos.
    // Body com user_id/document valida o mascaramento de campos sensíveis configurado via
    // Bugsy.shared.addSensitiveBodyField, e exercita cada método HTTP que o overlay deve exibir corretamente.
    private func makeRequest(method: String, path: String) {
        var request = URLRequest(url: URL(string: "https://httpbin.org/\(path)")!)
        request.httpMethod = method
        request.setValue("Bearer tok_fake_9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c_not_a_real_secret", forHTTPHeaderField: "Authorization")
        request.setValue("session_id=sess_9f8a7b6c5d4e; theme=dark", forHTTPHeaderField: "Cookie")
        request.setValue("fake-api-key-00112233445566778899", forHTTPHeaderField: "X-Api-Key")
        request.setValue("device-simulator-01", forHTTPHeaderField: "X-Device-Id")
        if method == "POST" || method == "PUT" {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = Data("""
            {"user_id":"u_42","document":"123.456.789-00","note":"sample \(method)"}
            """.utf8)
        }
        statusLabel.text = "Sending \(method)..."
        URLSession.shared.dataTask(with: request) { [weak self] _, response, error in
            DispatchQueue.main.async {
                let code = (response as? HTTPURLResponse)?.statusCode
                self?.statusLabel.text = code.map { "\(method) -> Status \($0)" } ?? (error?.localizedDescription ?? "Falhou")
            }
        }.resume()
    }

    @objc private func showBugsy() {
        Bugsy.shared.show()
    }

    @objc private func refreshDarkMode() {
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
