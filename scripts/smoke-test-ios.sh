#!/usr/bin/env bash
# Smoke test de pré-release: instala o .app do sample num simulador iOS,
# abre o app e confere que o processo não morreu (sem crash no startup).
#   scripts/smoke-test-ios.sh <caminho-para-BugsySample.app>
# Assume um simulador já booted (xcrun simctl boot <udid> + simctl bootstatus).
set -euo pipefail

APP="${1:?uso: $0 <caminho-para-.app>}"
BUNDLE_ID="dev.bugsy.sample"

echo "== Instalando $APP"
xcrun simctl install booted "$APP"

echo "== Abrindo $BUNDLE_ID"
LAUNCH_OUTPUT="$(xcrun simctl launch booted "$BUNDLE_ID")"
echo "$LAUNCH_OUTPUT"
PID="$(echo "$LAUNCH_OUTPUT" | awk '{print $2}')"

if [ -z "$PID" ]; then
  echo "FALHOU: não consegui capturar o PID do launch"
  exit 1
fi

sleep 5

echo "== Checando se o processo (pid $PID) ainda está vivo"
if ! ps -p "$PID" >/dev/null 2>&1; then
  echo "FALHOU: processo $BUNDLE_ID (pid $PID) não está mais rodando (crash no startup?)"
  exit 1
fi

echo "ok: $BUNDLE_ID instalado, abriu e continua rodando (pid $PID)"
