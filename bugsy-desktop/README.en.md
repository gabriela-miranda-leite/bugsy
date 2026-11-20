# Bugsy Desktop

_[Também disponível em português](README.md)._

Bugsy's desktop client (macOS, Windows, Linux): pairs with the phone over the local network and
shows requests, mocks, flags and device info on a bigger screen, with no cable or ADB needed.

## Download and installation

[![Download](https://img.shields.io/github/v/release/gabriela-miranda-leite/bugsy?label=download&color=0A7BBB)](https://github.com/gabriela-miranda-leite/bugsy/releases/latest)

Download the installer for your system from the
[Releases page](https://github.com/gabriela-miranda-leite/bugsy/releases/latest):

- **macOS**: `Bugsy-<version>.dmg`
- **Windows**: `Bugsy-<version>.msi`
- **Linux**: `bugsy_<version>-1_amd64.deb`

The app isn't signed or notarized yet — that requires a paid developer certificate (an Apple
Developer Program membership and a Windows code signing certificate), which is out of scope for
now. Without them, the OS shows an "untrusted app" warning on first launch; the workaround is:

- **macOS**: right-click (or Control-click) `Bugsy.app` and choose "Open", confirming the
  Gatekeeper dialog. It opens normally after that.
- **Windows**: on the SmartScreen warning, click "More info" and then "Run anyway".
- **Linux**: install with `sudo dpkg -i bugsy_<version>-1_amd64.deb` (or your package manager's
  GUI); with no Gatekeeper/SmartScreen, it opens right away.

## Pairing with your phone

Open the Bugsy overlay on the phone and Bugsy Desktop on the computer. Both need to be on the
**same local network**. There are three ways to connect:

1. **QR code** — the desktop's home screen shows a QR code; scan it on the overlay's pairing
   screen. This is the recommended flow on a network you don't control (the pairing secret only
   ever travels across the screen, never over the network).
2. **6-digit code** — the same panel shows a short-lived numeric code; type it into the phone's
   overlay. On the code and list flows, always check that the 4-digit code shown on both screens
   matches before confirming — that's what protects against another device on the network
   stepping in the middle.
3. **Device list** — the desktop discovers phones with the overlay open on the same network and
   lists each one; click "Connect" on a device and approve the request on the phone's overlay.

There's also a **manual connection** (IP, port and token typed by hand) as a fallback for when
automatic discovery doesn't work.

### Ports used

| Port | Protocol | Use |
|---|---|---|
| `7790` | UDP | Discovery broadcast sent by the desktop |
| `7791` | UDP | Discovery reply sent by the phone |
| `7788` | WebSocket (TLS) | Data connection between phone and desktop, after pairing |

See [`docs/seguranca-do-pareamento-desktop.en.md`](../docs/seguranca-do-pareamento-desktop.en.md)
for what each pairing flow protects and what it doesn't.

## Running from source

```sh
./gradlew :bugsy-desktop:run
```

## Generating the installers

```sh
./gradlew :bugsy-desktop:packageDmg   # macOS
./gradlew :bugsy-desktop:packageMsi   # Windows (needs the WiX Toolset v3 installed)
./gradlew :bugsy-desktop:packageDeb   # Linux (needs fakeroot installed)
```

Each task only builds the native format of the system it runs on (that's how `jpackage` works);
a release's three installers are built in CI, one per system, and attached to the GitHub Release
automatically.

## Known limitations

- **Networks with client isolation** (common on guest/corporate Wi-Fi) block the UDP broadcast
  automatic discovery relies on — on those, neither the QR code nor the device list work; use the
  manual connection instead.
- **Manual connection doesn't verify the phone's fingerprint before connecting** (it trusts
  whatever certificate it sees on the first connection, "trust on first use"), unlike the QR, code
  and list flows.

See [`docs/seguranca-do-pareamento-desktop.en.md`](../docs/seguranca-do-pareamento-desktop.en.md)
for the full list of what pairing does and doesn't protect against.
