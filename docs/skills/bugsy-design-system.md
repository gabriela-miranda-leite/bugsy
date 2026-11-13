---
name: "bugsy-design-system"
description: "Use before creating or touching any Bugsy UI file — provides the full dark-theme color palette, typography, spacing constants, BugsyTheme wrapper, and methodColors/statusColor helpers for the debug overlay."
---

# Bugsy — design system

Tudo em `ui/src/commonMain/kotlin/com/bugsy/ui/theme/`. Nunca use esses tokens fora de builds debug.

Fonte da verdade: Figma `G8aLo2h9267KWTcJMSxQrA`. Todo valor abaixo foi extraído 1:1 dos frames
(não é aproximação) — ver node `0:1` para a lista completa de telas.

## BugsyColors.kt

```kotlin
object BugsyColors {
    // Surfaces
    val Background      = Color(0xFF09090D)  // cURL / JSON code block
    val Surface         = Color(0xFF0C0C10)  // bottom sheet (header + conteúdo)
    val SurfaceCard     = Color(0xFF121216)  // metric cards, search bar input
    val SurfaceElevated = Color(0xFF17171D)  // botão "X" (dismiss)

    // Borders
    val Border        = Color(0xFF27272E)  // cards, inputs, code block
    val BorderSubtle  = Color(0xFF202027)  // separadores de row / key-value
    val BorderHeader  = Color(0xFF25252D)  // divisores de header (tab bar, back row)
    val BorderStrong  = Color(0xFF303039)  // borda do botão "Clear"
    val BorderButton  = Color(0xFF34343F)  // borda do botão "Copy" / "Copy all"
    val DragHandle    = Color(0xFF404047)

    // Text
    val TextPrimary   = Color(0xFFF6F6F8)
    val TextSecondary = Color(0xFFD0D0D7)  // valores em destaque: URLs, headers, kv values
    val TextMuted     = Color(0xFFB4B4C0)  // corpo de code block (cURL, JSON)
    val TextDimmed    = Color(0xFFBCBCC8)  // URL na lista de requests
    val TextDisabled  = Color(0xFF8B8B98)  // labels, section titles, duração, placeholders, tab inativa

    // Brand
    val Brand = Color(0xFFFF710B)  // laranja Bugsy — badge, tab ativa, copy button

    // HTTP method badges (textColor to backgroundColor) — HeadText/HeadBg sem exemplo no Figma
    val GetText    = Color(0xFF60A5FA);  val GetBg    = Color(0xFF112142)
    val PostText   = Color(0xFF42DF89);  val PostBg   = Color(0xFF052012)
    val PutText    = Color(0xFFFFBB25);  val PutBg    = Color(0xFF2B1A00)
    val DeleteText = Color(0xFFFF686E);  val DeleteBg = Color(0xFF2E0809)
    val PatchText  = Color(0xFFC084FC);  val PatchBg  = Color(0xFF200A38)
    val HeadText   = Color(0xFFA1A1AA);  val HeadBg   = Color(0xFF1C1C22)

    // Status codes — Status3xx sem exemplo no Figma
    val Status2xx  = Color(0xFF3FE58A)
    val Status3xx  = Color(0xFFFBBF24)
    val Status4xx  = Color(0xFFFF686E)
    val Status5xx  = Color(0xFFFF2854)
}
```

## Fontes (BugsyFonts.kt)

Roboto e Roboto Mono são empacotadas de verdade (não usar `FontFamily.Default`/`Monospace` do
sistema). São fontes variáveis do google/fonts (`ofl/roboto`, `ofl/robotomono`), instanciadas como
arquivos estáticos (Regular/Medium/Bold) via `fontTools.varLib.instancer` e colocadas em
`ui/src/commonMain/composeResources/font/`. Licença SIL OFL em `ui/licenses/`.

```kotlin
@Composable fun robotoFontFamily(): FontFamily      // Normal, Medium, Bold
@Composable fun robotoMonoFontFamily(): FontFamily  // Normal, Medium
```

## BugsyTypography.kt

Cada estilo é `@Composable get()` (precisa de FontFamily carregada via resource, só pode ser lido
dentro de composição):

```kotlin
object BugsyTypography {
    // Chrome do UI — Roboto
    val Title: TextStyle      // 17sp Medium, letterSpacing -0.3
    val Body: TextStyle       // 13sp Regular
    val BodySmall: TextStyle  // 12sp Regular
    val Label: TextStyle      // 11sp Medium, letterSpacing 0.5
    val Caption: TextStyle    // 11sp Regular

    // Código — URLs, headers, cURL, response body — Roboto Mono
    val Code: TextStyle       // 12sp, lineHeight 20sp
    val CodeSmall: TextStyle  // 11sp, lineHeight 18sp
}
```

O badge de método HTTP (`MethodBadge`) usa Roboto **Bold** sans-serif 12sp — não monoespaçada.

## BugsySpacing.kt

Valores principais (ver arquivo para a lista completa — foram adicionados tokens conforme a
fidelidade ao Figma exigiu, como `emptyStateTitleSpacing` e `copyAllButtonHeight`):

```kotlin
object BugsySpacing {
    val xs = 4.dp;  val sm = 8.dp;  val md = 12.dp
    val lg = 16.dp; val xl = 20.dp; val xxl = 24.dp

    val rowVertical     = 9.dp    // padding vertical de RequestRow
    val rowHorizontal   = 16.dp   // padding horizontal de rows
    val sheetHorizontal = 18.dp   // header e tab bar
    val cardPadding     = 10.dp   // dentro de MetricCard
    val strokeBorder    = 0.5.dp
    val cornerBadge     = 4.dp
    val cornerCard      = 8.dp
    val cornerToggle    = 11.dp
}
```

## BugsyTheme.kt

```kotlin
@Composable
fun BugsyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background   = BugsyColors.Background,
            surface      = BugsyColors.Surface,
            onBackground = BugsyColors.TextPrimary,
            onSurface    = BugsyColors.TextPrimary,
            outline      = BugsyColors.Border,
        ),
        content = content,
    )
}
```

## Funções helper (BugsyColorUtils.kt)

```kotlin
fun methodColors(method: String): Pair<Color, Color> = when (method.uppercase()) {
    "GET"    -> BugsyColors.GetText    to BugsyColors.GetBg
    "POST"   -> BugsyColors.PostText   to BugsyColors.PostBg
    "PUT"    -> BugsyColors.PutText    to BugsyColors.PutBg
    "DELETE" -> BugsyColors.DeleteText to BugsyColors.DeleteBg
    "PATCH"  -> BugsyColors.PatchText  to BugsyColors.PatchBg
    else     -> BugsyColors.HeadText   to BugsyColors.HeadBg
}

fun statusColor(code: Int?): Color = when {
    code == null -> BugsyColors.TextDisabled
    code >= 500  -> BugsyColors.Status5xx
    code >= 400  -> BugsyColors.Status4xx
    code >= 300  -> BugsyColors.Status3xx
    else         -> BugsyColors.Status2xx
}

fun formatDuration(ms: Long): String =
    if (ms >= 1_000) "${(ms / 1_000.0)}s" else "${ms}ms"

fun formatBytes/* ... */ // não existe mais — tela de detalhe não mostra tamanho de body (ver bugsy-screens)
```

## Regras de uso

- Nunca hardcode hex fora deste arquivo — sempre `BugsyColors.*`
- Monospace (Roboto Mono) **obrigatório** para: URL, headers, chave/valor de header, cURL, response/request body, valores de device info
- Sans-serif (Roboto) para todo o chrome do UI (rótulos, tabs, botões, badge de método)
- `BugsyTheme {}` wrapa o composable raiz do overlay — não wrapa o app do consumidor
- `strokeBorder = 0.5.dp` em todos os borders de card, row separator e input
- Existem 5 tons de borda distintos no Figma (`Border`, `BorderSubtle`, `BorderHeader`,
  `BorderStrong`, `BorderButton`) — não reusar um pelo outro sem checar o frame correspondente
