# Bugsy Desktop

_[Also available in English](README.en.md)._

Cliente desktop (macOS, Windows, Linux) do Bugsy: pareia com o celular pela rede local e mostra
requests, mocks, flags e informações de device numa tela maior, sem precisar do cabo nem de ADB.

## Download e instalação

[![Download](https://img.shields.io/github/v/release/gabriela-miranda-leite/bugsy?label=download&color=0A7BBB)](https://github.com/gabriela-miranda-leite/bugsy/releases/latest)

Baixe o instalador do seu sistema na
[página de Releases](https://github.com/gabriela-miranda-leite/bugsy/releases/latest):

- **macOS**: `Bugsy-<versão>.dmg`
- **Windows**: `Bugsy-<versão>.msi`
- **Linux**: `bugsy_<versão>-1_amd64.deb`

O app ainda não é assinado nem notarizado — isso exige um certificado de desenvolvedor pago (Apple
Developer Program e um code signing certificate no Windows), o que está fora do escopo por ora.
Sem eles, o sistema mostra um aviso de "app não confiável" na primeira abertura; o contorno é:

- **macOS**: clique com o botão direito (ou Control+clique) em `Bugsy.app` e escolha "Abrir",
  confirmando no diálogo do Gatekeeper. Abre normalmente nas próximas vezes.
- **Windows**: no aviso do SmartScreen, clique em "Mais informações" e depois em "Executar assim
  mesmo".
- **Linux**: instale com `sudo dpkg -i bugsy_<versão>-1_amd64.deb` (ou pelo gerenciador de pacotes
  gráfico); sem Gatekeeper/SmartScreen, abre direto.

## Como parear com o celular

Abra o overlay do Bugsy no celular e o Bugsy Desktop no computador. Os dois precisam estar na
**mesma rede local**. Há três formas de conectar:

1. **QR code** — a tela inicial do desktop mostra um QR code; escaneie-o na tela de pareamento do
   overlay. É o fluxo recomendado em redes que você não controla (o segredo do pareamento só passa
   pela própria tela, nunca pela rede).
2. **Código de 6 dígitos** — o mesmo painel mostra um código numérico com validade curta; digite-o
   no overlay do celular. Nos fluxos por código e por lista, confira sempre que o código de 4
   dígitos exibido nas duas telas é igual antes de confirmar — é o que protege contra outro
   dispositivo na rede se colocando no meio.
3. **Lista de dispositivos na rede** — o desktop descobre celulares com o overlay aberto na mesma
   rede e lista cada um; clique em "Connect" num dispositivo e aprove a solicitação no overlay do
   celular.

Há ainda uma **conexão manual** (IP, porta e token digitados à mão) como alternativa, para quando a
descoberta automática não funciona.

### Portas usadas

| Porta | Protocolo | Uso |
|---|---|---|
| `7790` | UDP | Broadcast de descoberta enviado pelo desktop |
| `7791` | UDP | Resposta de descoberta enviada pelo celular |
| `7788` | WebSocket (TLS) | Conexão de dados entre celular e desktop, depois do pareamento |

Veja [`docs/seguranca-do-pareamento-desktop.md`](../docs/seguranca-do-pareamento-desktop.md) para o
que cada fluxo de pareamento protege e o que não protege.

## Rodando a partir do código

```sh
./gradlew :bugsy-desktop:run
```

## Gerando os instaladores

```sh
./gradlew :bugsy-desktop:packageDmg   # macOS
./gradlew :bugsy-desktop:packageMsi   # Windows (precisa do WiX Toolset v3 instalado)
./gradlew :bugsy-desktop:packageDeb   # Linux (precisa do fakeroot instalado)
```

Cada tarefa só builda o formato nativo do sistema em que ela roda (é assim que o `jpackage`
funciona); os três instaladores de uma release são gerados em CI, um por sistema, e anexados à
GitHub Release automaticamente.

## Limitações conhecidas

- **Redes com isolamento de clientes** (comum em Wi-Fi de convidados/corporativo) bloqueiam o
  broadcast UDP usado pela descoberta automática — nelas, nem o QR code nem a lista de dispositivos
  funcionam; use a conexão manual.
- **Conexão manual não verifica o fingerprint do celular antes de conectar** (confia no certificado
  que vê na primeira conexão, "trust on first use"), diferente dos fluxos por QR, código e lista.

Veja [`docs/seguranca-do-pareamento-desktop.md`](../docs/seguranca-do-pareamento-desktop.md) para a
lista completa do que é e não é protegido no pareamento.
