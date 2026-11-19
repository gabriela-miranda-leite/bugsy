# Desktop pairing: what is protected and what is not

_[Também disponível em português](seguranca-do-pareamento-desktop.md)._

Bugsy Desktop and the phone talk over the local network (DEB-64). This page says what an attacker on the same
Wi-Fi can and cannot do (DEB-67).

## What is protected (Android phone)

- **The connection.** The phone only accepts the desktop over TLS (1.3 where Android has it, 1.2 otherwise). The
  certificate is self-signed, created in memory every time the Bugsy server starts, and never written to disk.
  Someone listening to the Wi-Fi sees encrypted bytes, not the token or the captured requests.
- **Certificate pinning.** The desktop does not trust a certificate authority. It accepts exactly one certificate:
  the one whose SHA-256 fingerprint the phone reported while pairing. A fake phone on the network is refused.
- **Pairing by QR code.** The QR code carries a random 128-bit secret that only the desktop screen shows. The phone
  uses it to encrypt the token and the fingerprint (AES-256-GCM, bound to the pairing code), so the pairing datagram
  contains neither in clear. Replaying a copied datagram fails: the code is single use.
- **Pairing by code and by device list.** The desktop sends a one-time public key (ECDH P-256) and the phone
  encrypts the token and the fingerprint to it (AES-256-GCM). Someone who only listens to the network does not see
  the token. Each code and each connection request has its own key, thrown away after use.
- **Guessing.** The session token has 12 characters (about 69 bits) from the system's secure random generator, and
  is compared in constant time. After 5 wrong tokens an address is locked out for 1 minute. On the desktop, 5 wrong
  codes from one address block only that address, without rotating the code on screen.
- **The local server.** The server behind TLS listens on `127.0.0.1` only and only serves requests that went through
  the TLS terminator, which adds an internal secret. Another app on the phone that connects directly is refused.
- **Account data.** The app can stop sending the Account rows (name, e-mail and so on) to the desktop with
  `BugsyConfig(shareCustomInfoWithDesktop = false)`. The default is to send them, so the desktop shows what the
  phone shows.
- **Disconnect.** "Disconnect (generates a new token)" restarts the server, so it also creates a new token and a
  new certificate.

## What is not protected yet

- **iOS phones have no TLS and no pairing encryption yet.** The iOS server is a hand-written WebSocket over POSIX
  sockets. The desktop falls back to plain `ws://`: the token and the traffic cross the network in clear. To limit
  the damage, iOS does not send the real values of secrets (only the already masked text) over that connection.
- **Someone actively in the middle, in the code and list flows.** The encryption in these flows protects against
  someone who only listens. Someone who can answer in the desktop's place on the network could hand over their own
  public key and read the token. The QR code does not have this weakness, because the secret goes through the
  screen. Use the QR code on networks you do not trust.
- **Manual connection.** The desktop has no fingerprint, so it trusts the certificate it sees the first time
  (trust on first use). That protects against listening, but not against someone actively impersonating the phone on
  that very first connection.
- **A downgrade.** A desktop that does not know the phone's fingerprint tries `wss://` first and then `ws://`. An
  attacker who can block TLS could push it to plain `ws://`.
- **Discovery on the network.** For the device list to work, the phone answers anyone on the network with its name
  and OS. The desktop also shows what it receives.
- **Disturbing pairing.** Someone on the network can still send attempts and make the code on screen rotate more
  often than normal (the per-address limit helps, but a source address on a local network can be forged).
- **No external review.** The cryptography uses only JDK primitives (AES-GCM, ECDH, SHA-256), but the design of the
  protocols has not been reviewed by a specialist.

## Rule of thumb

On a network you do not control, pair with the QR code from an Android phone. On your own network, any flow is fine.
