# Versão do Bugsy Desktop

_[Also available in English](versao-do-desktop.en.md)._

A versão do Bugsy Desktop sai da versão da biblioteca, então há um lugar só para mudar (DEB-70): `VERSION_NAME` no
`gradle.properties`. Quem publica uma versão da biblioteca já deixa o desktop na versão certa.

## A regra

Os instaladores não aceitam o texto da biblioteca como está: o `jpackage` (DMG e MSI) só aceita `MAJOR.MINOR.PATCH`, o
macOS rejeita um MAJOR igual a 0, e um sufixo como `-alpha` não é um número. Então:

1. o sufixo (`-alpha`, `-rc1`, ...) é tirado;
2. um MAJOR igual a 0 vira 1.

| `VERSION_NAME` | versão do desktop |
|---|---|
| `0.3.0-alpha` | `1.3.0` |
| `0.4.1` | `1.4.1` |
| `1.0.0` | `1.0.0` |
| `2.1.0-rc1` | `2.1.0` |

Enquanto a biblioteca está na série 0, o desktop mostra 1 no lugar do 0, e a partir da `1.0.0` os números são iguais.

Os instaladores do Windows também limitam as partes (MAJOR e MINOR até 255, PATCH até 65535). Um `VERSION_NAME` fora do
formato `X.Y.Z` ou `X.Y.Z-sufixo`, ou fora desses limites, faz o build falhar com uma mensagem que cita `VERSION_NAME`.

## Onde a versão aparece

- **Instaladores e `.app`:** a versão derivada (por exemplo `1.3.0`).
- **Barra de status do app:** as duas, `v1.3.0 · Bugsy 0.3.0-alpha`, para ver qual versão da biblioteca o desktop acompanha.

O app lê as duas de `bugsy-version.properties`, gerado no build a partir de `VERSION_NAME`. Rodando fora do Gradle ele mostra
`dev` e `unknown`, em vez de inventar um número.

## Onde está o código

A regra está em `buildSrc/src/main/kotlin/DesktopVersion.kt`, com testes em `buildSrc/src/test/kotlin/DesktopVersionTest.kt`
(rodados no CI com `./gradlew :buildSrc:test`). O build do desktop só a usa.
