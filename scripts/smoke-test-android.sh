#!/usr/bin/env bash
# Smoke test de pré-release: instala o APK do sample num emulador já de pé,
# abre a MainActivity e confere que o processo não morreu (sem FATAL EXCEPTION).
#   scripts/smoke-test-android.sh <apk-path>
# Assume um emulador/device já conectado via adb (ex: reactivecircus/android-emulator-runner).
set -euo pipefail

APK="${1:?uso: $0 <apk-path>}"
PACKAGE="dev.bugsy.sample"
ACTIVITY="dev.bugsy.sample.MainActivity"

echo "== Instalando $APK"
adb install -r -t "$APK"

echo "== Limpando logcat e abrindo $ACTIVITY"
adb logcat -c
adb shell am start -W -n "$PACKAGE/$ACTIVITY"

sleep 5

echo "== Checando se o processo ainda está vivo"
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
  echo "FALHOU: processo $PACKAGE não está rodando depois do launch (crash no startup?)"
  adb logcat -d | tail -200
  exit 1
fi

echo "== Checando logcat por crash"
if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "FALHOU: FATAL EXCEPTION encontrada no logcat"
  adb logcat -d | grep -A 30 "FATAL EXCEPTION"
  exit 1
fi

echo "ok: $PACKAGE instalado, abriu e continua rodando"
