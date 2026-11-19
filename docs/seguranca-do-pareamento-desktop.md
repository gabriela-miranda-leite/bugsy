# Pareamento com o Desktop: o que é protegido e o que não é

_[Also available in English](seguranca-do-pareamento-desktop.en.md)._

O Bugsy Desktop e o celular conversam pela rede local (DEB-64). Esta página diz o que uma pessoa no mesmo
Wi-Fi consegue e não consegue fazer (DEB-67).

## O que é protegido (celular Android)

- **A conexão.** O celular só aceita o desktop por TLS (1.3 onde o Android tem, 1.2 nos demais). O certificado é
  autoassinado, criado em memória a cada vez que o servidor do Bugsy inicia, e nunca é gravado em disco. Quem
  escuta o Wi-Fi vê bytes cifrados, não o token nem as requisições capturadas.
- **Fixação do certificado.** O desktop não confia em nenhuma autoridade certificadora. Ele aceita exatamente um
  certificado: aquele cujo fingerprint SHA-256 o celular informou durante o pareamento. Um celular falso na rede
  é recusado.
- **O fluxo por QR.** O QR code leva um segredo aleatório de 128 bits que só a tela do desktop mostra. O celular o
  usa para cifrar o token e o fingerprint (AES-256-GCM, vinculado ao código do pareamento), então o pacote do
  pareamento não contém nenhum dos dois em texto puro. Copiar esse pacote e reenviá-lo falha: o código é de uso
  único.
- **Desconectar.** "Disconnect (generates a new token)" reinicia o servidor, então também cria um certificado novo.

## O que ainda não é protegido

- **Celulares iOS ainda não têm TLS.** O servidor do iOS é um WebSocket escrito à mão sobre sockets POSIX, e não
  faz TLS. O desktop então cai para `ws://` simples: o token e o tráfego passam pela rede em texto puro.
- **Os fluxos por código de 6 dígitos e por lista de dispositivos.** O código é enviado em broadcast em texto puro
  para achar o desktop, e o fluxo por lista não tem nenhum segredo, então o token vai em texto puro no pacote do
  pareamento. O fingerprint também vai em texto puro, então quem consegue interceptar ativamente o pareamento
  poderia trocá-lo. Use o QR code em redes em que você não confia.
- **Conexão manual.** O desktop não tem o fingerprint, então confia no certificado que vê na primeira vez
  (trust on first use). Isso protege contra escuta, mas não contra quem se passe ativamente pelo celular nessa
  primeira conexão.
- **Rebaixamento.** Um desktop que não conhece o fingerprint do celular tenta `wss://` primeiro e depois `ws://`.
  Um atacante que consiga bloquear o TLS poderia empurrá-lo para `ws://` simples (só quando o pareamento não
  trouxe um fingerprint).

## Regra prática

Em uma rede que você não controla, pareie com o QR code a partir de um celular Android. Na sua própria rede,
qualquer fluxo serve.
