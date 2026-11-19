<p align="center">
  <img src="assets/logo.svg" alt="Bugsy" width="360">
</p>

<p align="center">
<a href="https://central.sonatype.com/namespace/io.github.gabriela-miranda-leite"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.gabriela-miranda-leite/bugsy-ui"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/android.yml"><img alt="Android" src="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/android.yml/badge.svg"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/ios.yml"><img alt="iOS" src="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/ios.yml/badge.svg"></a>
<a href="https://codecov.io/gh/gabriela-miranda-leite/bugsy"><img alt="codecov" src="https://codecov.io/gh/gabriela-miranda-leite/bugsy/graph/badge.svg"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/blob/main/scripts/verify-signature.md"><img alt="GPG signed" src="https://img.shields.io/badge/artifacts-GPG%20signed-0A7BBB"></a>
<a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
</p>

<p align="center">
🇧🇷 Português · <a href="README.en.md">🇺🇸 English</a>
</p>

Bugsy é uma biblioteca **Kotlin Multiplatform (Android + iOS)** que funciona como um
"Flipper/Chucker" de bolso embutido no próprio app: um overlay de debug que captura requests de
rede, permite criar mocks para simular respostas (sucesso, erro, lentidão) sem depender do
backend, mostra feature flags, informações de device, e permite revelar/mascarar dados sensíveis,
com uma política de segurança "quando em dúvida, mascara" para nunca vazar segredos (tokens,
senhas, CPF) por acidente.

## Índice

- [Demonstração](#demonstração)
- [Instalação](#instalação)
  - [Kotlin / Android](#kotlin--android)
  - [Swift / iOS](#swift--ios)
  - [React Native](#react-native)
  - [Flutter](#flutter)
- [Uso básico](#uso-básico)
- [Mocks](#mocks)
- [Módulos](#módulos)
- [Segurança](#segurança)
- [Sobre o desenvolvimento](#sobre-o-desenvolvimento)

## Demonstração

| Android | iOS |
|---|---|
| <video src="https://github.com/user-attachments/assets/d0d36d73-6a73-4659-8ef9-784bb9906590" controls width="300"></video> | <video src="https://github.com/user-attachments/assets/e4013b78-c22d-4890-91e5-82c7855eb68d" controls width="300"></video> |

## Instalação

Publicado no **Maven Central** sob o group `io.github.gabriela-miranda-leite`.

### Kotlin / Android

3 artefatos consumidos separadamente por build type:

```kotlin
dependencies {
    // Overlay completo (traz :core transitivamente) só em debug.
    debugImplementation("io.github.gabriela-miranda-leite:bugsy-ui:0.1.0-alpha")

    // Mesma API pública, zero instrumentação, em release.
    releaseImplementation("io.github.gabriela-miranda-leite:bugsy-noop:0.1.0-alpha")
}
```

### Swift / iOS

Bugsy também roda em apps **Swift puro** (sem KMP), via `Bugsy.xcframework`:

```sh
# Debug: Bugsy completo
./gradlew :ui:linkDebugFrameworkIosSimulatorArm64 :ui:iosSimulatorArm64AggregateResources
# Release: artefato no-op (mesma API, sem instrumentação)
./gradlew :bugsy-noop:linkReleaseFrameworkIosSimulatorArm64
```

A tarefa de recursos (`...AggregateResources`) gera as strings e fontes do overlay e só existe no
alvo Debug — sem ela o app pode usar recursos desatualizados e crashar ao abrir o overlay. Para
rodar em device físico, troque `iosSimulatorArm64` por `iosArm64`.

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

`Bugsy.initialize` precisa rodar **antes** de qualquer `URLSession` ser criada (ele registra o
`NSURLProtocol` do Bugsy). Em release, linke o framework de `:bugsy-noop`, não o de `:ui` — ele
não contém o interceptor. Veja [`sample-ios/README.md`](sample-ios/README.md) para o passo a
passo completo, incluindo a cópia dos recursos do overlay no Xcode.

### React Native

Bugsy é uma lib nativa (Kotlin/Swift) e **não tem binding JS hoje** — não dá para `import` direto
num app React Native. Para usar em RN seria necessário escrever um módulo nativo (Turbo
Module/Native Module) encapsulando as chamadas `Bugsy.*` do lado Android e iOS. Esse wrapper
ainda não existe; é trabalho futuro.

### Flutter

Mesma situação do React Native: **não há plugin Flutter hoje**. Para integrar seria necessário um
Platform Channel (ou Federated Plugin) chamando o Bugsy nativo em cada plataforma. Esse plugin
ainda não existe; é trabalho futuro.

## Uso básico

```kotlin
// Application.onCreate()
Bugsy.initialize(
    trigger = Trigger.Shake,
    flagsProvider = { listOf(DebugFlag("darkModeEnabled", "Dark mode", false)) },
)

// Client OkHttp (Android)
OkHttpClient.Builder()
    .addBugsyInterceptor()
    .build()
```

No iOS, chame `startBugsyIosOverlay()` uma vez no startup do app para observar `Bugsy.isVisible`.

## Mocks

Na aba "Mocks" do overlay dá para criar regras que interceptam requests por método e padrão de
URL (regex) e respondem com um status, headers e body customizados, sem precisar subir ou alterar
o backend. Cada regra pode ter um delay simulado, ser desativada temporariamente ou configurada
para disparar só uma vez ("one shot"). É possível criar uma regra já preenchida a partir de um
request real capturado ("Create mock from this response"), e exportar/importar o conjunto de
regras como um arquivo JSON para compartilhar entre devices ou com o time.

| Android | iOS |
|---|---|
| <video src="https://github.com/user-attachments/assets/0beb8daa-9e65-485a-a59d-dea6fae9b89f" controls width="300"></video> | <video src="https://github.com/user-attachments/assets/08e92c81-3fd4-4ddc-a94e-95e9a4b42c5d" controls width="300"></video> |

## Módulos

```
settings.gradle.kts
 ├─ :core            (KMP lib — captura, redação, lógica)
 ├─ :ui              (KMP lib — overlay Compose Multiplatform, depende de :core)
 ├─ :bugsy-noop      (KMP lib — mesma API pública de core+ui, zero instrumentação)
 └─ :sample-android  (app Android de demonstração, não publicado)
```

- **`core`** — captura de requests (interceptor OkHttp no Android, `NSURLProtocol` no iOS),
  redação de headers e mascaramento de campos sensíveis do body, feature flags e os triggers
  (shake, botão flutuante, notificação) que abrem o overlay.
- **`ui`** — overlay Compose Multiplatform: lista de requests, detalhe de request/response,
  mock rules (criação, edição, import/export em JSON), flags e device info.
- **`bugsy-noop`** — artefato para builds de release: mesma API pública de `core`+`ui`, sem
  nenhuma instrumentação.

## Segurança

Headers (`authorization`, `cookie`, `x-api-key`, ...) e campos de body (`password`, `cpf`,
`token`, `secret`, ...) sensíveis são mascarados por padrão antes de qualquer dado chegar à UI ou
ao `cURL` exportado. Dados reais só ficam disponíveis em memória, nunca em log, e só se
explicitamente revelados na tela de settings do overlay.

Em release, `bugsy-noop` substitui `ui`/`core` por um artefato sem nenhum comportamento — o
gate real, porém, é que o interceptor/`NSURLProtocol` só é ativado se o host for debuggable,
mesmo quando o artefato completo está no classpath.

A conexão entre o celular e o Bugsy Desktop (Android) usa TLS com o certificado fixado, e o
pareamento por QR code cifra o token. O iOS e os pareamentos por código e por lista ainda não têm
essa proteção. O que é e o que não é protegido está em
[`docs/seguranca-do-pareamento-desktop.md`](docs/seguranca-do-pareamento-desktop.md)
([English version](docs/seguranca-do-pareamento-desktop.en.md)).

## Sobre o desenvolvimento

Este projeto foi desenvolvido com apoio de IA ([Claude Code](https://claude.com/claude-code)),
com revisão humana de cada PR. O fluxo de desenvolvimento, validação local e release está
documentado em [`docs/fluxo-de-desenvolvimento.md`](docs/fluxo-de-desenvolvimento.md)
([English version](docs/development-workflow.md)).
