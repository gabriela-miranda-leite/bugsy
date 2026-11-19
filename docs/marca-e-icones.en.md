# Brand and icons: one source

_[Também disponível em português](marca-e-icones.md)._

The Bugsy mark (the bug) lives in **one file**: [`assets/brand/bugsy-mark.svg`](../assets/brand/bugsy-mark.svg).
Every place that shows the logo starts from it, so if the brand changes one day, only that file changes (DEB-68).

## How to change the mark

1. Edit `assets/brand/bugsy-mark.svg` (the paths, the `data-brand-color` colour, the `data-brand-background` colour).
2. Run `./gradlew :bugsy-desktop:generateBrandAssets`.
3. Run the tests. If the mark visibly changed, re-record the snapshots (`./gradlew :ui:recordPaparazziDebug`).
4. Update `assets/logo.svg` (the full README logo, with the name) by hand: it is not generated, but a test fails if it
   stops containing the drawing in `bugsy-mark.svg`.

Step 2 has to run on a Mac to rebuild `icon.icns` (only macOS's `iconutil` makes it). On another system everything
else is generated and the `.icns` is left as it is, with a warning.

## Where each place comes from

| Where it shows up | How it comes from the source |
|---|---|
| Desktop: sidebar and connection screen (`BugsyIcon`) | reads `bugsy-mark.svg` itself (no copy) |
| Desktop: window and Dock (`bugsy-app-icon.png`) | generated |
| Desktop: installers (`bugsy-desktop/packaging/icon.icns`, `.ico`, `.png`) | generated |
| Phone overlay (`BugsyLogo` in `:ui`, `bugsy_icon.xml`) | generated |
| Android: notification icon and floating button (`core`, `bugsy_notification.xml`, `bugsy_mark.xml`) | generated |
| Android: sample app launcher icon (`ic_launcher_foreground.xml`) | generated |
| iOS: sample app icon (`AppIcon-1024.png`) | generated |
| README (`assets/logo.svg`) | by hand, but checked by a test |

Generated files start with a "GENERATED" notice: do not edit them, edit the source.

## What is checked

`BrandAssetsTest` (in `:bugsy-desktop`) fails when a generated vector differs from what the source produces, when a binary
icon is missing, when `assets/logo.svg` stops containing the mark, or when the mark's colours drift from `BugsyColors`.
