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
- **The QR flow.** The QR code carries a random 128-bit secret that only the desktop screen shows. The phone uses
  it to encrypt the token and the fingerprint (AES-256-GCM, bound to the pairing code), so the pairing datagram
  does not contain either in clear. Copying that datagram and replaying it fails: the code is single use.
- **Disconnect.** "Disconnect (generates a new token)" restarts the server, so it also creates a new certificate.

## What is not protected yet

- **iOS phones have no TLS yet.** The iOS server is a hand-written WebSocket over POSIX sockets, and does not do
  TLS. The desktop then falls back to plain `ws://`: the token and the traffic cross the network in clear.
- **The 6-digit code and device list flows.** The code is broadcast in clear to find the desktop, and the list flow
  has no secret at all, so the token is sent in clear in the pairing datagram. The fingerprint is sent in clear
  too, so someone who can actively intercept the pairing could swap it. Use the QR code on networks you do not trust.
- **Manual connection.** The desktop has no fingerprint, so it trusts the certificate it sees the first time
  (trust on first use). That protects against listening, but not against someone actively impersonating the phone on
  that very first connection.
- **A downgrade.** A desktop that does not know the phone's fingerprint tries `wss://` first and then `ws://`. An
  attacker who can block TLS could push it to plain `ws://` (only when it was not paired with a fingerprint).

## Rule of thumb

On a network you do not control, pair with the QR code from an Android phone. On your own network, any flow is fine.
