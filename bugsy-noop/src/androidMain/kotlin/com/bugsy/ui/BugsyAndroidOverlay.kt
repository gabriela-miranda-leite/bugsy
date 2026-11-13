package com.bugsy.ui

/**
 * No-op: não compõe nada. É uma função comum (não @Composable) para o artefato não depender do Compose,
 * mas pode ser chamada de dentro de qualquer Composable do app, como a versão de debug.
 */
fun BugsyAndroidOverlay() = Unit
