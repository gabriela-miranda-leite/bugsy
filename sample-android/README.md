# Bugsy — sample-android

App de exemplo Android que mostra a integração com dois artefatos:

```kotlin
debugImplementation(project(":ui"))            // Bugsy completo
releaseImplementation(project(":bugsy-noop"))  // mesma API, sem instrumentação
```

O código do app usa só a API pública (`Bugsy.initialize`, `Bugsy.show`, `BugsyAndroidOverlay()`), que existe nos dois artefatos, então o mesmo código compila nas duas variantes. Em **release** o app depende apenas do `:bugsy-noop`: nenhuma classe de captura, repositório ou overlay entra no APK, sem depender de regras R8/ProGuard (por isso não há `consumer-rules.pro`).

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

Nota: o Bugsy ainda não expõe uma API pública para plugar o interceptor OkHttp no client do app, então este sample não captura requests em debug.
