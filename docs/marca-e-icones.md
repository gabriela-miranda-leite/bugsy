# Marca e ícones: uma fonte só

_[Also available in English](marca-e-icones.en.md)._

O desenho do Bugsy (o besouro) vive em **um arquivo**: [`assets/brand/bugsy-mark.svg`](../assets/brand/bugsy-mark.svg).
Todo lugar que mostra a logo parte dele, então, se a marca mudar um dia, é só mudar esse arquivo (DEB-68).

## Como mudar a marca

1. Edite `assets/brand/bugsy-mark.svg` (os caminhos, a cor `data-brand-color`, o fundo `data-brand-background`).
2. Rode `./gradlew :bugsy-desktop:generateBrandAssets`.
3. Rode os testes. Se a marca mudou de forma visível, regrave os snapshots (`./gradlew :ui:recordPaparazziDebug`).
4. Atualize à mão o `assets/logo.svg` (a logo completa do README, com o nome): ele não é gerado, mas um teste falha se
   ele deixar de conter o desenho de `bugsy-mark.svg`.

O passo 2 precisa rodar num Mac para refazer o `icon.icns` (só o `iconutil` do macOS o gera). Em outro sistema o resto
é gerado e o `.icns` fica como está, com um aviso.

## De onde vem cada lugar

| Onde aparece | Como vem da fonte |
|---|---|
| Desktop: menu lateral e tela de conexão (`BugsyIcon`) | lê o próprio `bugsy-mark.svg` (sem cópia) |
| Desktop: janela e Dock (`bugsy-app-icon.png`) | gerado |
| Desktop: instaladores (`bugsy-desktop/packaging/icon.icns`, `.ico`, `.png`) | gerados |
| Celular, overlay (`BugsyLogo` em `:ui`, `bugsy_icon.xml`) | gerado |
| Android: ícone da notificação e botão flutuante (`core`, `bugsy_notification.xml`, `bugsy_mark.xml`) | gerados |
| Android: ícone do app de exemplo (`ic_launcher_foreground.xml`) | gerado |
| iOS: ícone do app de exemplo (`AppIcon-1024.png`) | gerado |
| README (`assets/logo.svg`) | à mão, mas conferido por teste |

Os arquivos gerados começam com o aviso "GENERATED": não os edite, edite a fonte.

## O que é conferido

`BrandAssetsTest` (no `:bugsy-desktop`) falha quando um vetor gerado difere do que a fonte produz, quando um ícone
binário some, quando o `assets/logo.svg` deixa de conter a marca, ou quando as cores da marca saem de `BugsyColors`.
