---
name: "bugsy-ui-components"
description: "Use quando for construir os composables reutilizáveis do Bugsy: MethodBadge, RequestRow, DragHandle, BugsyTabBar, BugsySearchBar, MetricCard, KeyValueRow, ToggleRow, CodeBlock, ExpandableSection, EmptyState."
---

# Bugsy — componentes reutilizáveis

Todos em `ui/src/commonMain/kotlin/com/bugsy/ui/components/`. Leia o skill `bugsy-design-system`
antes — os tokens vêm de lá. Fonte da verdade: Figma `G8aLo2h9267KWTcJMSxQrA`.

> `ToggleRow` está descrito abaixo como estava planejado, mas **ainda não foi implementado** — não
> existe `ToggleRow.kt` no código hoje. `DebugFlag` (model) existe mas não está conectado a nenhuma
> tela. Não assumir que esse componente existe sem checar o diretório primeiro.

## MethodBadge

Pílula colorida para o método HTTP. Texto em **Roboto Bold sans-serif** (não monoespaçada — isso
já foi um bug corrigido: o Figma usa `fontFamily: Roboto, fontStyle: Bold, fontSize: 12` pro texto
do badge).

```kotlin
@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = methodColors(method)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(BugsySpacing.cornerBadge))
            .defaultMinSize(minWidth = BugsySpacing.badgeMinWidth)
            .padding(horizontal = BugsySpacing.badgePaddingHorizontal, vertical = BugsySpacing.badgePaddingVertical),
    ) {
        Text(
            text = method.uppercase(),
            style = TextStyle(fontFamily = robotoFontFamily(), fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = textColor,
            maxLines = 1,
        )
    }
}
```

## RequestRow

Layout: `MethodBadge | URL (weight 1, ellipsis) | StatusCode | Duration`. Bottom-border via
`drawBehind` (não `Divider`) para não interferir no click.

```kotlin
@Composable
fun RequestRow(request: DebugRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.rowItemSpacing),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = BugsySpacing.rowHorizontal, vertical = BugsySpacing.rowVertical)
            .drawBehind {
                val s = BugsySpacing.strokeBorder.toPx()
                drawLine(BugsyColors.BorderSubtle, Offset(0f, size.height - s / 2), Offset(size.width, size.height - s / 2), s)
            },
    ) {
        MethodBadge(request.method)
        Text(
            text = formatUrl(request.url), // strip scheme + query params
            style = BugsyTypography.Code.copy(color = BugsyColors.TextDimmed), // #BCBCC8 — url é a cor CLARA, não a mais escura
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = request.statusCode?.toString() ?: "—",
            style = BugsyTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
            color = statusColor(request.statusCode),
        )
        Text(
            text = formatDuration(request.durationMs),
            style = BugsyTypography.Caption,
            color = BugsyColors.TextDisabled, // #8B8B98 — duration é mais apagado que a url
            modifier = Modifier.widthIn(min = BugsySpacing.durationColumnMinWidth),
            textAlign = TextAlign.End,
        )
    }
}
```

## DragHandle

Usa um token próprio (`BugsyColors.DragHandle` = `#404047`), **não** `TextDisabled` — é uma cor
genuinamente diferente de qualquer tom de texto no Figma.

```kotlin
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().padding(top = BugsySpacing.dragHandlePaddingTop, bottom = BugsySpacing.dragHandlePaddingBottom),
    ) {
        Box(Modifier.width(BugsySpacing.dragHandleWidth).height(BugsySpacing.dragHandleHeight)
            .background(BugsyColors.DragHandle, RoundedCornerShape(BugsySpacing.dragHandleCorner)))
    }
}
```

## BugsyTabBar

ActiveIndicator é uma linha de 2dp na bottom do item — não usar `TabRow` do Material. **Cuidado**:
cada item precisa de `Modifier.width(IntrinsicSize.Min)` no `Box` externo, senão o indicador
`fillMaxWidth()` do item ativo mede contra a largura da Row inteira em vez da largura do próprio
texto — isso já foi um bug real (o primeiro tab ocupava a tela toda e empurrava os outros pra fora).
O divisor de baixo é `BorderHeader` (`#25252D`), não o `Border` genérico de cards.

```kotlin
@Composable
fun BugsyTabBar(selected: BugsyTab, requestCount: Int, onSelect: (BugsyTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.sheetHorizontal),
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.tabItemSpacing),
        ) {
            BugsyTab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Min) // <- essencial, ver aviso acima
                        .clickable { onSelect(tab) }
                        .padding(horizontal = BugsySpacing.tabItemPaddingHorizontal, vertical = BugsySpacing.tabItemPaddingVertical),
                ) {
                    if (active) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(BugsySpacing.tabIndicatorHeight).background(BugsyColors.Brand))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.tabBadgeSpacing)) {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = if (active) BugsyTypography.Body.copy(fontWeight = FontWeight.Medium) else BugsyTypography.Body,
                            color = if (active) BugsyColors.TextPrimary else BugsyColors.TextDisabled,
                        )
                        if (tab == BugsyTab.Requests && requestCount > 0) {
                            Box(Modifier.background(BugsyColors.Brand, CircleShape).padding(horizontal = BugsySpacing.countBadgePaddingHorizontal, vertical = BugsySpacing.countBadgePaddingVertical)) {
                                Text(requestCount.toString(), style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium), color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderHeader)
    }
}
```

## BugsySearchBar

`BasicTextField` com decoração manual — não usar `TextField`/`OutlinedTextField` do Material.

```kotlin
@Composable
fun BugsySearchBar(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary),
        singleLine = true,
        cursorBrush = SolidColor(BugsyColors.Brand),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
                    .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                    .padding(horizontal = BugsySpacing.searchBarPaddingHorizontal, vertical = BugsySpacing.searchBarPaddingVertical),
            ) {
                Icon(Icons.Outlined.Search, null, tint = BugsyColors.TextDisabled)
                Spacer(Modifier.width(BugsySpacing.searchBarIconSpacing))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(stringResource(Res.string.search_placeholder), style = BugsyTypography.Code.copy(color = BugsyColors.TextDisabled))
                    inner()
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
```

## SectionLabel

```kotlin
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = BugsyTypography.Label, color = BugsyColors.TextDisabled, modifier = modifier.padding(bottom = BugsySpacing.sm))
}
```

## KeyValueRow

Usado em headers de request/response e DeviceInfoScreen (este último não reusa o componente — usa
uma variante local com fonte diferente, ver `bugsy-screens`). O **valor** é `FontWeight.Medium`
(mais forte que a chave) — outro ponto que já foi bug (os dois usavam o mesmo peso).

```kotlin
@Composable
fun KeyValueRow(key: String, value: String, keyMinWidth: Dp = BugsySpacing.keyValueKeyMinWidthDefault, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm), modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.keyValueRowVertical)) {
            Text(key, style = BugsyTypography.Code.copy(color = BugsyColors.TextDisabled), modifier = Modifier.widthIn(min = keyMinWidth))
            Text(value, style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}
```

## MetricCard

Usado em 3-up row no RequestDetailScreen (Status / Duration / Time) — **não** um grid 2x2, são 3
numa linha só (já foi tentado um 4º card de "tamanho do corpo" e um grid 2x2, nenhum dos dois está
no Figma — revertido).

```kotlin
@Composable
fun MetricCard(label: String, value: String, valueColor: Color = BugsyColors.TextPrimary, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
            .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
            .padding(BugsySpacing.cardPadding),
    ) {
        Text(label, style = BugsyTypography.Caption, color = BugsyColors.TextDisabled)
        Spacer(Modifier.height(BugsySpacing.metricLabelSpacing))
        Text(value, style = TextStyle(fontSize = BugsySpacing.metricValueTextSize, fontWeight = FontWeight.Medium), color = valueColor)
    }
}
```

## CodeBlock

Suporta cURL (com botão "Copy" + feedback animado — ícone/cor trocam por 1.6s, sem toast) ou
qualquer bloco de código readonly. `SelectionContainer` permite copy manual do texto. A borda do
botão é `BorderButton` (`#34343F`), diferente da borda do box de conteúdo (`Border`, `#27272E`).

```kotlin
@Composable
fun CodeBlock(text: String, onCopy: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier) {
        if (onCopy != null) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = BugsySpacing.sm)) {
                SectionLabel(stringResource(Res.string.label_curl))
                val tint = if (copied) BugsyColors.Status2xx else BugsyColors.Brand
                OutlinedButton(
                    onClick = { onCopy(); copied = true; scope.launch { delay(1_600); copied = false } },
                    border = BorderStroke(BugsySpacing.strokeBorder, if (copied) BugsyColors.Status2xx else BugsyColors.BorderButton),
                    contentPadding = PaddingValues(horizontal = BugsySpacing.codeBlockButtonPaddingHorizontal, vertical = BugsySpacing.codeBlockButtonPaddingVertical),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonSmall),
                ) {
                    Icon(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, null, tint = tint)
                    Spacer(Modifier.width(BugsySpacing.codeBlockButtonIconSpacing))
                    Text(stringResource(if (copied) Res.string.action_copied else Res.string.action_copy), style = BugsyTypography.Caption, color = tint)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth()
                .background(BugsyColors.Background, RoundedCornerShape(BugsySpacing.cornerCard))
                .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                .padding(BugsySpacing.md),
        ) {
            SelectionContainer {
                Text(text, style = BugsyTypography.CodeSmall.copy(color = BugsyColors.TextMuted))
            }
        }
    }
}
```

No `RequestDetailScreen`, o body é passado por `prettyPrintJson(text)` antes de chegar no
`CodeBlock` — indenta JSON com 2 espaços, devolve o texto original se não for JSON válido.

## ExpandableSection

```kotlin
@Composable
fun ExpandableSection(title: String, initiallyExpanded: Boolean = false, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = BugsySpacing.sm)) {
            SectionLabel(title, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = stringResource(if (expanded) Res.string.cd_collapse else Res.string.cd_expand),
                tint = BugsyColors.TextDisabled,
            )
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}
```

## EmptyState

**Título + subtítulo opcional** — não uma única linha. O Figma sempre mostra as duas linhas
centralizadas (título 20sp Medium branco, subtítulo 14sp apagado) para toda tela vazia (requests,
flags). Uma versão anterior deste componente só aceitava uma `message` — foi corrigido.

```kotlin
@Composable
fun EmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.emptyStateTitleSpacing),
        modifier = modifier.fillMaxWidth().padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.emptyStatePaddingVertical),
    ) {
        Text(title, style = BugsyTypography.Body.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = BugsyColors.TextPrimary, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, style = BugsyTypography.BodySmall, color = BugsyColors.TextDisabled, textAlign = TextAlign.Center)
        }
    }
}
```

## ToggleRow (planejado, não implementado ainda)

Descrito aqui só como referência de intenção futura — `ToggleRow.kt` **não existe** no código.
Não escrever `import com.bugsy.ui.components.ToggleRow` sem checar primeiro.

```kotlin
// @Composable fun ToggleRow(flag: DebugFlag, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) { ... }
// Use Switch do Material3 com cores sobrescritas quando for implementar.
```

## Regras

- Nunca use `TextField`/`OutlinedTextField` do Material no Bugsy — só `BasicTextField` com `decorationBox`
- `HorizontalDivider` sempre com `thickness = BugsySpacing.strokeBorder`; a cor depende do contexto —
  ver os 5 tokens de borda em `bugsy-design-system` (não existe um único "Border" certo para tudo)
- `SelectionContainer` obrigatório em qualquer `CodeBlock` — usuário precisa conseguir copiar manualmente
- Fonte: Roboto sans-serif pro chrome (labels, tabs, botões, badge de método); Roboto Mono pro conteúdo (URL, headers, cURL, JSON, device info) — carregadas via `robotoFontFamily()`/`robotoMonoFontFamily()`, nunca `FontFamily.Default`/`FontFamily.Monospace` do sistema
