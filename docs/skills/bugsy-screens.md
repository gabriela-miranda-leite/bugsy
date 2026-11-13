---
name: "bugsy-screens"
description: "Use quando for implementar as telas do Bugsy: DebugOverlay (raiz), RequestListScreen, RequestDetailScreen, FlagsScreen, DeviceInfoScreen."
---

# Bugsy — telas

Todas em `ui/src/commonMain/kotlin/com/bugsy/ui/screens/`. Leia `bugsy-design-system` e
`bugsy-ui-components` antes. Fonte da verdade: Figma `G8aLo2h9267KWTcJMSxQrA` (node `0:1` lista os
9 frames: Requests, Device, Request details, Flags, App, e as 4 variantes "— cleared").

## DebugOverlay — composable raiz

Entry point do overlay. Envolve tudo com `BugsyTheme`. Gerencia qual tab está ativa. O header/tabs
(`DragHandle` + `BugsySheetHeader` + `BugsyTabBar`) ficam **sempre visíveis** acima do conteúdo —
inclusive quando `RequestListScreen` navega para `RequestDetailScreen` (a navegação é interna à
própria tela, o `DebugOverlay` nunca sabe disso).

```kotlin
@Composable
fun DebugOverlay(
    viewModel: DebugViewModel,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BugsyTab.Requests) }
    val requests by viewModel.requests.collectAsState()

    BugsyTheme {
        Column(Modifier.fillMaxWidth().background(BugsyColors.Surface)) {
            DragHandle()
            BugsySheetHeader(
                requestCount = requests.size,
                onClear = viewModel::clearRequests,
                onDismiss = onDismiss,
            )
            BugsyTabBar(
                selected = selectedTab,
                requestCount = requests.size,
                onSelect = { selectedTab = it },
            )
            when (selectedTab) {
                BugsyTab.Requests -> RequestListScreen(requests = requests)
                BugsyTab.Flags -> FlagsScreen()
                BugsyTab.Device -> DeviceInfoScreen(info = viewModel.deviceInfo)
            }
        }
    }
}
```

## BugsySheetHeader

Header com ícone de bug, título "Bugsy" (Roboto Bold 24 no Figma — usa `BugsyTypography.Title` no
código), badge de contagem, botão "Clear" e botão "X" (dismiss).

```kotlin
@Composable
fun BugsySheetHeader(requestCount: Int, onClear: () -> Unit, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = BugsySpacing.sheetHorizontal, vertical = BugsySpacing.headerPaddingVertical),
    ) {
        Icon(Icons.Outlined.BugReport, stringResource(Res.string.cd_bugsy_icon), tint = BugsyColors.Brand)
        Text(stringResource(Res.string.bugsy_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
        if (requestCount > 0) {
            Box(Modifier.background(BugsyColors.Brand, CircleShape).padding(/* countBadgePadding* */)) {
                Text(requestCount.toString(), style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium), color = Color.White)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onClear,
            border = BorderStroke(BugsySpacing.strokeBorder, BugsyColors.BorderStrong), // #303039 — não é o Border genérico
            shape = RoundedCornerShape(BugsySpacing.cornerButton),
            contentPadding = PaddingValues(horizontal = BugsySpacing.clearButtonPaddingHorizontal, vertical = BugsySpacing.clearButtonPaddingVertical),
        ) {
            Text(stringResource(Res.string.action_clear), style = BugsyTypography.Caption, color = BugsyColors.TextMuted)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(BugsySpacing.dismissButtonSize).background(BugsyColors.SurfaceElevated, CircleShape),
        ) {
            Icon(Icons.Outlined.Close, stringResource(Res.string.cd_close), tint = BugsyColors.TextMuted)
        }
    }
}
```

## RequestListScreen

Navegação inline — sem NavHost. `selected` guarda qual request abrir no detail (substitui a lista
inteira pelo `RequestDetailScreen`, sem afetar o header/tabs do `DebugOverlay` acima). Filtragem por
URL e método em memória (não vai para o ViewModel).

```kotlin
@Composable
fun RequestListScreen(requests: List<DebugRequest>) {
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }

    selected?.let { req ->
        RequestDetailScreen(request = req, onBack = { selected = null })
        return
    }

    val filtered = remember(requests, filter) { filterRequests(requests, filter) }

    Column {
        Box(Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin)) {
            BugsySearchBar(value = filter, onValueChange = { filter = it })
        }
        if (filtered.isEmpty()) {
            if (filter.isNotBlank()) {
                EmptyState(title = stringResource(Res.string.empty_no_requests_filtered, filter))
            } else {
                // Figma: título + subtítulo, não uma única linha
                EmptyState(
                    title = stringResource(Res.string.empty_no_requests_title),       // "No requests"
                    subtitle = stringResource(Res.string.empty_no_requests_subtitle),  // "New requests will appear here."
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { req ->
                    RequestRow(request = req, onClick = { selected = req })
                }
            }
        }
    }
}
```

## RequestDetailScreen

Seções, na ordem real do Figma (o corpo da request **não** tem seção própria — só aparece dentro do
cURL):

URL+método → MetricCards (Status / Duration / Time, 3 numa linha só) → Request Headers (se houver)
→ Response Headers (se houver) → Response Body (expandable, colapsado por padrão se > 400 chars) →
cURL (label + botão "Copy" na mesma linha, acima da caixa do comando — **no final da lista**, não no
topo).

```kotlin
@Composable
fun RequestDetailScreen(request: DebugRequest, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Column {
        Row( // back bar
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    val s = BugsySpacing.strokeBorder.toPx()
                    drawLine(BugsyColors.BorderHeader, Offset(0f, size.height), Offset(size.width, size.height), s)
                }
                .padding(start = BugsySpacing.sm),
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = BugsyColors.Brand)
                Text(stringResource(Res.string.tab_requests), style = BugsyTypography.Body, color = BugsyColors.Brand)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
        ) {
            item { /* MethodBadge + url */ }
            item { /* Row: MetricCard Status, Duration, Time */ }

            if (request.requestHeaders.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.label_request_headers)) }
                items(request.requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (request.responseHeaders.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.label_response_headers)) }
                items(request.responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (request.responseBody?.isNotBlank() == true) {
                item {
                    val long = (request.responseBody?.length ?: 0) > 400
                    ExpandableSection(stringResource(Res.string.label_response_body), initiallyExpanded = !long) {
                        CodeBlock(prettyPrintJson(request.responseBody ?: "")) // indenta JSON com 2 espaços; texto inválido passa direto
                    }
                }
            }

            item { // cURL — por último
                CodeBlock(
                    text = request.curlCommand,
                    onCopy = { clipboard.setText(AnnotatedString(request.curlCommand)) },
                )
            }
        }
    }
}
```

`prettyPrintJson` (em `BugsyTheme`/`JsonPrettyPrint.kt`) é um parser JSON manual próprio — sem
dependência de `kotlinx.serialization`. Retorna o texto original inalterado se não for JSON válido.

## FlagsScreen

**Ainda não tem fonte de dados real** (ver DEB-3 para o registro de flags e persistência de
overrides). Hoje é só o empty state do Figma — título + subtítulo, igual ao de requests:

```kotlin
@Composable
fun FlagsScreen() {
    EmptyState(
        title = stringResource(Res.string.flags_empty_title),       // "No flags configured"
        subtitle = stringResource(Res.string.flags_empty_subtitle),  // "Feature flags will appear here."
    )
}
```

`DebugFlag` (model em `core`) existe mas não está conectado ainda — não inventar uma tela de lista
de flags sem o ticket pedir.

## DeviceInfoScreen

Recebe um `DeviceInfo` (model em `core/model/DeviceInfo.kt`) com os campos exatos do Figma — não os
campos antigos (`manufacturer`, `appPackageId`, `screenResolution`, `timezone` não existem mais,
não estavam no desenho). 3 seções (APP / DEVICE / RUNTIME) + botão "Copy all" no final.

```kotlin
data class DeviceInfo(
    val appVersionName: String, val appVersionCode: String,
    val environment: String, val buildType: String,
    val deviceModel: String, val osName: String, val osVersion: String, val locale: String,
    val bugsyVersion: String, val kotlinVersion: String, val composeVersion: String,
)

@Composable
fun DeviceInfoScreen(info: DeviceInfo) {
    val clipboard = LocalClipboardManager.current
    // APP: Version ("${appVersionName} (${appVersionCode})"), Environment, Build
    // DEVICE: Model, OS ("${osName} ${osVersion}"), Locale
    // RUNTIME: Bugsy, Kotlin, Compose
    // cada linha: label Roboto Regular 14 (TextDisabled) + valor Roboto Mono Medium 14 (TextSecondary)
    // divisor BorderSubtle entre linhas, SectionLabel antes de cada seção
    // botão final "Copy all" — border BorderButton, copia tudo serializado em texto puro
}
```

`DebugViewModel.deviceInfo` ainda é um placeholder hardcoded (comentado no código) — não existe
ainda um provider `expect/actual` por plataforma (Android `Context`/`Build`, iOS `UIDevice`) que
leia os valores reais. Isso é trabalho futuro, não assumir que os valores vêm de verdade do device.

## Regras de tela

- Navegação request list → detail é **local** (`var selected by remember`) — sem NavHost, sem back-stack externa
- Filtragem de requests **nunca** vai para o ViewModel — é estado efêmero de tela
- `LazyColumn` em todas as listas — nunca `Column` com `verticalScroll` para listas de tamanho dinâmico
- Response body **sempre** em `ExpandableSection` — corpos grandes não podem quebrar o scroll; request body não tem seção própria (ver RequestDetailScreen acima)
- `SelectionContainer` obrigatório em qualquer `CodeBlock` — usuário precisa conseguir selecionar e copiar
- `EmptyState` sempre título + subtítulo opcional (nunca uma frase única solta) quando o Figma mostrar as duas linhas
