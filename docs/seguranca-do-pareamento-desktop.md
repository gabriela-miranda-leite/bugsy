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
- **O pareamento por QR.** O QR code leva um segredo aleatório de 128 bits que só a tela do desktop mostra. O celular
  o usa para cifrar o token e o fingerprint (AES-256-GCM, vinculado ao código do pareamento), então o pacote do
  pareamento não contém nenhum dos dois em texto puro. Reenviar um pacote copiado falha: o código é de uso único.
- **O pareamento por código e por lista.** O desktop manda uma chave pública efêmera (ECDH P-256) e o celular cifra
  o token e o fingerprint para ela (AES-256-GCM). Quem só escuta a rede não vê o token. Cada código e cada pedido
  de conexão tem a sua própria chave, descartada depois do uso.
- **Tentativas de adivinhar.** O token da sessão tem 12 caracteres (cerca de 69 bits), gerados com o gerador
  aleatório seguro do sistema, e é comparado em tempo constante. Depois de 5 tokens errados, aquele endereço fica
  bloqueado por 1 minuto. No desktop, 5 códigos errados de um mesmo endereço bloqueiam só esse endereço, sem trocar
  o código da tela.
- **O servidor local.** O servidor por trás do TLS escuta só em `127.0.0.1` e só atende pedidos que passam pelo
  terminador TLS, que acrescenta um segredo interno. Outro app do celular que conectar direto é recusado.
- **Desconectar.** "Disconnect (generates a new token)" reinicia o servidor, então também cria um token e um
  certificado novos.

## O que ainda não é protegido

- **Celulares iOS ainda não têm TLS nem a cifragem do pareamento.** O servidor do iOS é um WebSocket escrito à
  mão sobre sockets POSIX. O desktop cai para `ws://` simples: o token e o tráfego passam pela rede em texto puro.
  Para limitar o estrago, o iOS não envia os valores reais dos segredos (só o texto já mascarado) por essa conexão.
- **Quem se coloca no meio ativamente, nos fluxos por código e por lista.** A cifragem desses fluxos protege contra
  quem só escuta. Quem consegue responder no lugar do desktop na rede poderia entregar a própria chave pública e ler
  o token. O QR code não tem essa fraqueza, porque o segredo vai pela tela. Use o QR em redes em que você não confia.
- **Conexão manual.** O desktop não tem o fingerprint, então confia no certificado que vê na primeira vez
  (trust on first use). Isso protege contra escuta, mas não contra quem se passe ativamente pelo celular nessa
  primeira conexão.
- **Rebaixamento.** Um desktop que não conhece o fingerprint do celular tenta `wss://` primeiro e depois `ws://`.
  Um atacante que consiga bloquear o TLS poderia empurrá-lo para `ws://` simples.
- **Descoberta na rede.** Para a lista de dispositivos funcionar, o celular responde a qualquer um na rede com o
  nome e o sistema do aparelho. O desktop, na lista, também mostra o que recebe.
- **Atrapalhar o pareamento.** Quem está na rede ainda pode mandar tentativas e fazer o código da tela girar mais
  vezes do que o normal (o limite por endereço ajuda, mas o endereço de origem na rede local pode ser forjado).
- **Sem revisão externa.** A criptografia usa só as primitivas do JDK (AES-GCM, ECDH, SHA-256), mas o desenho dos
  protocolos não foi revisado por um especialista.

## Regra prática

Em uma rede que você não controla, pareie com o QR code a partir de um celular Android. Na sua própria rede,
qualquer fluxo serve.
