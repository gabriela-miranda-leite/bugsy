# Bugsy — sample-ios

App de exemplo em **Swift puro** (sem KMP) que integra o Bugsy via `Bugsy.xcframework`.

## Rodando

1. Gere o framework (debug):

   ```sh
   # Debug: Bugsy completo
   ./gradlew :ui:linkDebugFrameworkIosSimulatorArm64 :ui:iosSimulatorArm64AggregateResources
   # Release: artefato no-op (mesma API, sem instrumentação)
   ./gradlew :bugsy-noop:linkReleaseFrameworkIosSimulatorArm64
   ```

   O projeto escolhe o framework pela configuração: **Debug** usa `ui/build/bin/<target>/debugFramework` e **Release** usa `bugsy-noop/build/bin/<target>/releaseFramework`. A tarefa de recursos gera as strings e fontes do overlay (só Debug); sem ela o app pode usar recursos desatualizados e crashar ao abrir o overlay. Para device, troque `iosSimulatorArm64` por `iosArm64`.
2. Abra `sample-ios/BugsySample.xcodeproj` no Xcode e rode no simulador.
3. Toque em **Fazer request (URLSession.shared)** e depois em **Abrir Bugsy** (ou chacoalhe o dispositivo: *Device > Shake* no simulador). A request aparece na aba Requests, com `Authorization` mascarado como `***`.

## Release

Em Release o app linka o `Bugsy.framework` do módulo `:bugsy-noop`: a mesma API (`Bugsy.shared.initialize`, `show`, `startBugsyIosOverlay`...), mas sem interceptor, repositório, overlay nem recursos. Nada é registrado, capturado, armazenado ou logado, e `show()` não abre nada. No simulador, o binário de Release não tem nenhum símbolo `UrlProtocol`, `DebugRepository`, `FlagManager` etc.

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
- Em release, use o framework do `:bugsy-noop` (não o `ui`): ele não contém o interceptor. Como segunda barreira, o interceptor do framework completo só é criado em binários debug (`Platform.isDebugBinary`).
