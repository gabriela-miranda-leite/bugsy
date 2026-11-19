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

## Rodando num iPhone de verdade

O simulador não pede assinatura. Para um iPhone, o Xcode exige uma conta, mas **um Apple ID gratuito basta** (o mesmo do
iCloud); o Programa de Desenvolvedores pago não é necessário. É preciso fazer isso uma vez:

1. **Xcode > Settings > Accounts > "+" > Apple ID** e entre com o seu.
2. No target **BugsySample > Signing & Capabilities**, escolha **Team: Seu Nome (Personal Team)**.
3. Troque o **Bundle Identifier** (`dev.bugsy.sample`) por um seu, por exemplo `dev.bugsy.sample.seunome`. Com conta
   gratuita, o Xcode costuma recusar um identificador que não é seu.
4. Gere o framework **para aparelho** (o de simulador não serve):

   ```sh
   ./gradlew :ui:linkDebugFrameworkIosArm64 :ui:iosArm64AggregateResources
   ```

5. No iPhone, ligue o **Modo Desenvolvedor** (Ajustes > Privacidade e Segurança; o aparelho reinicia).
6. Rode pelo Xcode. Na primeira vez, o iPhone avisa que o certificado não é confiável: vá em **Ajustes > Geral > VPN e
   Gerenciamento de Dispositivo**, toque no seu Apple ID em "Apps do Desenvolvedor" e em **Confiar**. Rode de novo.
7. Aceite os pedidos de **câmera** (para ler o QR code) e de **Rede Local** (para o pareamento).

Com Apple ID gratuito o app instalado **expira em 7 dias**; é só rodar de novo pelo Xcode.

O Xcode grava o seu time e o seu identificador em `project.pbxproj`: **não inclua essas mudanças em um commit**.

### Erros comuns

| Mensagem | Causa e solução |
|---|---|
| `Signing for "BugsySample" requires a development team` | Falta escolher o time: passos 1 e 2. |
| `Unable to resolve module dependency: 'Bugsy'` | Falta o framework do destino escolhido: aparelho usa `...IosArm64` e simulador usa `...IosSimulatorArm64`. Gere o do destino. |
| `The Developer App Certificate is not trusted` | Falta confiar no certificado no iPhone: passo 6. |

No **simulador** a câmera e o broadcast são limitados: o pareamento por QR e por código não funciona. Use a conexão manual
com o desktop.

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
