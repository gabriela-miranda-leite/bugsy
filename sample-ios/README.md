# Bugsy — sample-ios

App de exemplo em **Swift puro** (sem KMP) que integra o Bugsy via `Bugsy.xcframework`.

## Rodando

1. Gere o framework (debug):

   ```sh
   ./gradlew :ui:assembleBugsyDebugXCFramework :ui:iosSimulatorArm64AggregateResources
   ```

   O XCFramework fica em `ui/build/XCFrameworks/debug/Bugsy.xcframework`, que já é a referência do projeto. A segunda tarefa gera as strings e fontes do overlay; sem ela o app pode usar recursos desatualizados e crashar ao abrir o overlay. Para rodar em device, use `iosArm64AggregateResources`.
2. Abra `sample-ios/BugsySample.xcodeproj` no Xcode e rode no simulador.
3. Toque em **Fazer request (URLSession.shared)** e depois em **Abrir Bugsy** (ou chacoalhe o dispositivo: *Device > Shake* no simulador). A request aparece na aba Requests, com `Authorization` mascarado como `***`.

## Recursos do overlay

O overlay é Compose Multiplatform e lê strings e fontes do bundle do app em `compose-resources/`. O projeto tem uma Run Script phase ("Copy Compose Resources") que copia esses arquivos de `ui/build`. Um app que consome o `Bugsy.xcframework` precisa de um passo equivalente.

## Modo automático

Com `BUGSY_SAMPLE_AUTORUN=1` no ambiente, o app faz a request e abre o overlay sozinho após 3 s:

```sh
SIMCTL_CHILD_BUGSY_SAMPLE_AUTORUN=1 xcrun simctl launch <UDID> dev.bugsy.sample
```

## Integração

Um único import (`import Bugsy`) expõe o core e o overlay.

```swift
// AppDelegate.application(_:didFinishLaunchingWithOptions:)
Bugsy.shared.initialize(
    config: BugsyConfig(
        trigger: .shake,
        shakeThresholdG: BugsyConfig.companion.DEFAULT_SHAKE_THRESHOLD_G,
        flagsProvider: { [] }
    )
)
// ...depois de criar a window:
BugsyIosOverlayKt.startBugsyIosOverlay()
```

- `Bugsy.initialize` deve rodar **antes** de qualquer `URLSession` ser criada: ele registra o `NSURLProtocol` do Bugsy, que intercepta `URLSession.shared` e sessões com a configuração padrão. Sessões com `URLSessionConfiguration` customizada precisam incluir a classe do Bugsy em `protocolClasses`.
- O registro só acontece em binários **debug** do framework (`Platform.isDebugBinary`). Em release, `initialize` não registra o `URLProtocol`. Use o framework de `assembleBugsyReleaseXCFramework` nos builds de release.
