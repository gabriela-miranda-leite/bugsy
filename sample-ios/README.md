# Bugsy — sample-ios

🇧🇷 Português · [🇺🇸 English](README.en.md)

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

## Sacudir (Shake)

O gatilho `Shake` funciona em aparelho de verdade (acelerômetro e gesto de sacudir do iOS) e também no Simulator, em **Device > Shake** (Ctrl+Cmd+Z): o Bugsy escuta o evento de movimento do UIKit além do acelerômetro, que não existe no Simulator. O Simulator só entrega o gesto se o app estiver em primeiro plano.

## Release

Em Release o app linka o `Bugsy.framework` do módulo `:bugsy-noop`: a mesma API (`Bugsy.shared.initialize`, `show`, `startBugsyIosOverlay`...), mas sem interceptor, repositório, overlay nem recursos. Nada é registrado, capturado, armazenado ou logado, e `show()` não abre nada. No simulador, o binário de Release não tem nenhum símbolo `UrlProtocol`, `DebugRepository`, `FlagManager` etc.

## Recursos do overlay

O overlay é Compose Multiplatform e lê strings e fontes do bundle do app em `compose-resources/`. O projeto tem uma Run Script phase ("Copy Compose Resources") que copia esses arquivos de `ui/build`. Um app que consome o `Bugsy.xcframework` precisa de um passo equivalente.

## Pareamento com o Bugsy Desktop

O pareamento lê um QR code com a câmera e encontra o desktop na rede local, então um app iOS que embute o Bugsy precisa de duas entradas no `Info.plist` (o sample as define como build settings `INFOPLIST_KEY_*`):

- `NSCameraUsageDescription`: exibida quando o overlay pede acesso à câmera para ler o QR code do desktop.
- `NSLocalNetworkUsageDescription`: exibida pelo iOS na primeira vez que o Bugsy envia ou recebe um broadcast de pareamento na rede local.

Sem elas o iOS encerra o app no primeiro pedido de câmera e bloqueia em silêncio o tráfego da rede local.

## Modo automático

Com `BUGSY_SAMPLE_AUTORUN=1` no ambiente, o app faz a request e abre o overlay sozinho após 3 s:

```sh
SIMCTL_CHILD_BUGSY_SAMPLE_AUTORUN=1 xcrun simctl launch <UDID> dev.bugsy.sample
```

## Testando uma versão de pré-release

Ainda não existe aqui o equivalente ao `-PbugsyFromMavenLocal` do `sample-android`: o projeto sempre linka o framework a partir do build local (debug ou release), nunca de um `.xcframework` publicado/baixado. Pra validar uma pré-release no iOS hoje, é preciso rebuildar o framework a partir do código-fonte na versão/tag desejada (ver "Rodando" acima) — não dá pra apontar direto pra um artefato já publicado.

## Integração

Um único import (`import Bugsy`) expõe o core e o overlay.

```swift
// AppDelegate.application(_:didFinishLaunchingWithOptions:)
Bugsy.shared.initialize(
    config: BugsyConfig(
        trigger: .shake,
        shakeThresholdG: BugsyConfig.companion.DEFAULT_SHAKE_THRESHOLD_G,
        flagsProvider: { [] },
        allowRevealingSecrets: true // false esconde o botão "Copiar com valores reais"
    )
)
// ...depois de criar a window:
BugsyIosOverlayKt.startBugsyIosOverlay()
```

- `Bugsy.initialize` deve rodar **antes** de qualquer `URLSession` ser criada: ele registra o `NSURLProtocol` do Bugsy, que intercepta `URLSession.shared` e sessões com a configuração padrão. Sessões com `URLSessionConfiguration` customizada precisam incluir a classe do Bugsy em `protocolClasses`.
- Em release, use o framework do `:bugsy-noop` (não o `ui`): ele não contém o interceptor. Como segunda barreira, o interceptor do framework completo só é criado em binários debug (`Platform.isDebugBinary`).
