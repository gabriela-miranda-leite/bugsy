# Pareamento com o Desktop: o que é protegido e o que não é

_[Also available in English](seguranca-do-pareamento-desktop.en.md)._

O Bugsy Desktop e o celular conversam pela rede local (DEB-64). Esta página diz o que uma pessoa no mesmo
Wi-Fi consegue e não consegue fazer (DEB-67).

## O que é protegido

- **A conexão.** O celular só aceita o desktop por TLS. O certificado é autoassinado, criado a cada vez que o
  servidor do Bugsy inicia, e descartado quando ele para. Quem escuta o Wi-Fi vê bytes cifrados, não o token nem as
  requisições capturadas.
  - **Android:** TLS 1.3 onde o sistema tem, 1.2 nos demais. O certificado fica só em memória.
  - **iOS:** TLS 1.2 (o SecureTransport do iOS não faz TLS 1.3) com uma suíte de sigilo perfeito (ECDHE-ECDSA com
    AES-GCM). O certificado é montado em Kotlin e fica no Keychain enquanto o servidor roda; é apagado ao parar.
- **Fixação do certificado.** O desktop não confia em nenhuma autoridade certificadora. Ele aceita exatamente um
  certificado: aquele cujo fingerprint SHA-256 o celular informou durante o pareamento. Um celular falso na rede
  é recusado.
- **O pareamento por QR.** O QR code leva um segredo aleatório de 128 bits que só a tela do desktop mostra. O celular
  o usa para cifrar o token e o fingerprint, vinculados ao código do pareamento, então o pacote do pareamento não
  contém nenhum dos dois em texto puro. Reenviar um pacote copiado falha: o código é de uso único.
- **O pareamento por código e por lista.** O desktop manda uma chave pública efêmera (ECDH P-256) e o celular cifra
  o token e o fingerprint para ela. Quem só escuta a rede não vê o token. Cada código e cada pedido de conexão tem a
  sua própria chave, descartada depois do uso.
- **Contra quem se coloca no meio, nos fluxos por código e por lista.** Antes de qualquer token sair, o celular e o
  desktop mostram o mesmo código de 4 dígitos, calculado a partir das duas chaves públicas e do código ou nonce do
  pareamento. Quem responder no lugar do desktop precisa usar uma chave própria de cada lado, e os dois códigos
  ficam diferentes. O celular só envia o token depois que a pessoa confirma que são iguais ("Match" no pareamento
  por código, "Allow" na lista). Isso só protege se a pessoa de fato comparar.
- **Cifras.** Android e desktop usam AES-256-GCM. O iOS não tem AES-GCM público, então usa AES-256-CBC com
  HMAC-SHA256 (cifrar e depois autenticar), marcado no pacote. O desktop abre os dois formatos.
- **Tentativas de adivinhar.** O token da sessão tem 12 caracteres (cerca de 69 bits), gerados com o gerador
  aleatório seguro do sistema, e é comparado em tempo constante. Depois de 5 tokens errados, aquele endereço fica
  bloqueado por 1 minuto, no Android e no iOS. No desktop, 5 códigos errados de um mesmo endereço bloqueiam só esse
  endereço, sem trocar o código da tela.
- **O servidor local.** O servidor por trás do TLS escuta só em `127.0.0.1` e só atende pedidos que passam pelo
  terminador TLS, que acrescenta um segredo interno. Outro app do celular que conectar direto é recusado.
- **Desconectar.** "Disconnect (generates a new token)" reinicia o servidor, então também cria um token e um
  certificado novos.

## O que ainda não é protegido

- **A comparação do código depende da pessoa.** Se ela confirmar sem olhar, ou se acostumar a tocar em "Match"
  e "Allow", a proteção contra quem se coloca no meio some. O QR code não tem essa fraqueza, porque o segredo vai
  pela tela.
- **Parear com um desktop ou celular mais antigo.** Um lado sem a chave efêmera ou sem a confirmação volta ao
  pareamento de antes, com o token em texto puro no pacote. Quem consegue atrapalhar o pareamento poderia forçar
  esse caminho.
- **Conexão manual.** O desktop não tem o fingerprint, então confia no certificado que vê na primeira vez
  (trust on first use). Isso protege contra escuta, mas não contra quem se passe ativamente pelo celular nessa
  primeira conexão.
- **Rebaixamento.** Um desktop que não conhece o fingerprint do celular tenta `wss://` primeiro e depois `ws://`.
  Um atacante que consiga bloquear o TLS poderia empurrá-lo para `ws://` simples.
- **Descoberta na rede.** Para a lista de dispositivos funcionar, o celular responde a qualquer um na rede com o
  nome e o sistema do aparelho. O desktop, na lista, também mostra o que recebe.
- **Atrapalhar o pareamento.** Quem está na rede ainda pode mandar tentativas e fazer o código da tela girar mais
  vezes do que o normal (o limite por endereço ajuda, mas o endereço de origem na rede local pode ser forjado).
- **iOS validado só no simulador.** O TLS e a cifragem do iOS foram testados no simulador, com a pilha de segurança
  real do iOS, mas não em um iPhone físico.
- **Sem revisão externa.** A criptografia usa só as primitivas do sistema (AES, ECDH, SHA-256, HMAC), mas o desenho
  dos protocolos não foi revisado por um especialista.

## Regra prática

Em uma rede que você não controla, pareie com o QR code. Nos outros fluxos, compare sempre o código de 4 dígitos nas
duas telas. Na sua própria rede, qualquer fluxo serve.
