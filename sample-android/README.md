# Bugsy — sample-android

🇧🇷 Português · [🇺🇸 English](README.en.md)

App de exemplo Android que mostra a integração com dois artefatos:

```kotlin
debugImplementation(project(":ui"))            // Bugsy completo
releaseImplementation(project(":bugsy-noop"))  // mesma API, sem instrumentação
```

O código do app usa só a API pública (`Bugsy.initialize`, `Bugsy.show`, `BugsyAndroidOverlay()`, `OkHttpClient.Builder.addBugsyInterceptor()`), que existe nos dois artefatos, então o mesmo código compila nas duas variantes. Em **release** o app depende apenas do `:bugsy-noop`: nenhuma classe de captura, repositório ou overlay entra no APK, sem depender de regras R8/ProGuard (por isso não há `consumer-rules.pro`).

## Verificando o release

```sh
./gradlew :sample-android:assembleRelease :sample-android:assembleDebug
# classes de instrumentação no dex (esperado: 0 no release)
unzip -o sample-android/build/outputs/apk/release/sample-android-release-unsigned.apk 'classes*.dex' -d /tmp/rel
cat /tmp/rel/classes*.dex | grep -a -c DebugRepository
# o que sobrou do Bugsy depois do R8
grep '^com.bugsy' sample-android/build/outputs/mapping/release/mapping.txt
# dependências da variante release
./gradlew :sample-android:dependencies --configuration releaseRuntimeClasspath | grep -i bugsy
```

## Como abrir o Bugsy no debug

O sample usa `Trigger.Notification`: com o app aberto aparece uma notificação fixa ("Toque para abrir o painel de debug") que abre o overlay; ela some quando o app vai para segundo plano. No Android 13+ o app precisa da permissão `POST_NOTIFICATIONS` (o sample a pede ao abrir). Os outros gatilhos são `Trigger.Shake` (padrão) e `Trigger.FloatingButton`. O botão **Abrir Bugsy** chama `Bugsy.show()` diretamente.

## Testando uma versão de pré-release

Em vez de depender dos módulos locais (`project(":ui")` / `project(":bugsy-noop")`), o sample pode consumir artefatos já publicados no `mavenLocal` — útil pra validar o `.aar` de verdade (com R8/shrink no release) antes de um release oficial. É o mesmo mecanismo usado pelo workflow `pre-release.yml`.

```sh
./gradlew :core:publishToMavenLocal :ui:publishToMavenLocal :bugsy-noop:publishToMavenLocal \
  -PVERSION_NAME="0.x.x-rc.1" -PbugsySkipSigning=true

./gradlew :sample-android:assembleDebug -PbugsyFromMavenLocal="0.x.x-rc.1"
```

Sem a flag `-PbugsyFromMavenLocal`, o build volta a usar os módulos locais via `project()`.

## Capturando requests

Adicione o interceptor ao client do app: `OkHttpClient.Builder().addBugsyInterceptor().build()`. Em release o artefato `bugsy-noop` devolve o builder sem alterações.

## Rodando no emulador

```sh
~/Library/Android/sdk/emulator/emulator -avd Pixel_9      # deixe esse terminal aberto
./gradlew :sample-android:installDebug                    # em outro terminal
```
