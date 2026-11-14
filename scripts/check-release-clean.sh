#!/usr/bin/env bash
# Gate de release do Bugsy: prova que um build de RELEASE não tem rastro da instrumentação de debug.
#   scripts/check-release-clean.sh <apk-release> [<binario-ios-release>]
# Falha (exit 1) se achar qualquer classe/símbolo de instrumentação ou permissão de debug.
set -euo pipefail

APK="${1:?uso: $0 <apk-release> [<binario-ios-release>]}"
IOS_BINARY="${2:-}"
FORBIDDEN_CLASSES='DebugRepository|RequestSecrets|RequestCapture|OkHttpDebugInterceptor|SharedOkHttpInterceptor|NotificationTrigger|FloatingButtonController|ShakeDetector|HeaderRedactor|BodyFieldMasker|CurlGenerator|DebugOverlay|SettingsScreen|BugsySettings'
FORBIDDEN_SYMBOLS='UrlProtocolInterceptor|BugsyUrlProtocol|MotionShakeHook|DebugRepository|RequestSecrets|RequestCapture|NotificationTrigger|FloatingButtonController|ShakeDetector|SettingsScreen'
failed=0

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

echo "== Android: classes no dex"
unzip -q -o "$APK" -d "$workdir"
for dex in "$workdir"/*.dex; do
  hits="$(grep -a -o -E "$FORBIDDEN_CLASSES" "$dex" | sort | uniq -c || true)"
  if [ -n "$hits" ]; then echo "FALHOU ($(basename "$dex")):"; echo "$hits"; failed=1; fi
done
[ "$failed" -eq 0 ] && echo "ok: nenhuma classe de instrumentação"

echo "== Android: permissões de debug no manifest"
AAPT2="$(ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "$AAPT2" ]; then
  if "$AAPT2" dump permissions "$APK" | grep -E 'SYSTEM_ALERT_WINDOW|POST_NOTIFICATIONS' >/dev/null; then
    echo "FALHOU: o manifest do release declara permissões do Bugsy"; failed=1
  else
    echo "ok: sem SYSTEM_ALERT_WINDOW/POST_NOTIFICATIONS do Bugsy"
  fi
else
  echo "aviso: aapt2 não encontrado, permissões não verificadas"
fi

if [ -n "$IOS_BINARY" ]; then
  echo "== iOS: símbolos no binário"
  hits="$(nm -a "$IOS_BINARY" 2>/dev/null | grep -E "$FORBIDDEN_SYMBOLS" | head -5 || true)"
  if [ -n "$hits" ]; then echo "FALHOU:"; echo "$hits"; failed=1; else echo "ok: nenhum símbolo de instrumentação"; fi
fi

if [ "$failed" -ne 0 ]; then echo "GATE DE RELEASE REPROVADO"; exit 1; fi
echo "GATE DE RELEASE APROVADO"
