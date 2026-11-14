# Verifying artifact signatures

Every artifact Bugsy publishes to Maven Central (`bugsy-core`, `bugsy-ui`, `bugsy-noop`) is signed
with this GPG key:

```
Fingerprint: C026 9B73 BD9E 7E27 C142 0484 0924 C1EA 9E8E 5D86
Key ID:      0924C1EA9E8E5D86
Owner:       Gabriela Miranda <gabrielamiranda11.10@gmail.com>
```

Import it from a keyserver:

```sh
gpg --keyserver keyserver.ubuntu.com --recv-keys 0924C1EA9E8E5D86
```

Maven Central mirrors every artifact's `.asc` signature next to the jar/aar. To verify one
manually:

```sh
curl -sLO https://repo1.maven.org/maven2/io/github/gabriela-miranda-leite/bugsy-ui/0.1.0-alpha/bugsy-ui-0.1.0-alpha.module
curl -sLO https://repo1.maven.org/maven2/io/github/gabriela-miranda-leite/bugsy-ui/0.1.0-alpha/bugsy-ui-0.1.0-alpha.module.asc
gpg --verify bugsy-ui-0.1.0-alpha.module.asc bugsy-ui-0.1.0-alpha.module
```

A `Good signature from "Gabriela Miranda ..."` confirms the artifact matches what was published
and hasn't been tampered with in transit.
