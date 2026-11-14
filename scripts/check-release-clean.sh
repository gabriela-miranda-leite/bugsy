#!/usr/bin/env bash
# Gate de release do Bugsy: prova que um build de RELEASE não tem rastro da instrumentação de debug.
#   scripts/check-release-clean.sh --apk <apk-release> [--ios <binario-ios-release>]
#   scripts/check-release-clean.sh --ios <binario-ios-release> [--apk <apk-release>]
# Pelo menos uma das duas flags é obrigatória; cada uma roda só o check da sua plataforma.
# Falha (exit 1) se achar qualquer classe/símbolo de instrumentação ou permissão de debug.
set -euo pipefail

usage() { echo "uso: $0 --apk <apk-release> [--ios <binario-ios-release>]" >&2; exit 1; }

APK=""
IOS_BINARY=""
while [ $# -gt 0 ]; do
  case "$1" in
    --apk) APK="${2:?--apk precisa de um caminho}"; shift 2 ;;
    --ios) IOS_BINARY="${2:?--ios precisa de um caminho}"; shift 2 ;;
    *) usage ;;
  esac
done
[ -n "$APK" ] || [ -n "$IOS_BINARY" ] || usage

FORBIDDEN_CLASSES='DebugRepository|RequestSecrets|RequestCapture|OkHttpDebugInterceptor|SharedOkHttpInterceptor|NotificationTrigger|FloatingButtonController|ShakeDetector|HeaderRedactor|BodyFieldMasker|CurlGenerator|DebugOverlay|SettingsScreen|BugsySettings'
FORBIDDEN_SYMBOLS='UrlProtocolInterceptor|BugsyUrlProtocol|MotionShakeHook|DebugRepository|RequestSecrets|RequestCapture|NotificationTrigger|FloatingButtonController|ShakeDetector|SettingsScreen'
failed=0

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

if [ -n "$APK" ]; then
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
fi

if [ -n "$IOS_BINARY" ]; then
  echo "== iOS: símbolos no binário"
  hits="$(nm -a "$IOS_BINARY" 2>/dev/null | grep -E "$FORBIDDEN_SYMBOLS" | head -5 || true)"
  if [ -n "$hits" ]; then echo "FALHOU:"; echo "$hits"; failed=1; else echo "ok: nenhum símbolo de instrumentação"; fi
fi

if [ "$failed" -ne 0 ]; then echo "GATE DE RELEASE REPROVADO"; exit 1; fi
echo "GATE DE RELEASE APROVADO"
