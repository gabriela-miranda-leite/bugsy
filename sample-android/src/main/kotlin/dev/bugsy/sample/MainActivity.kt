package dev.bugsy.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugsy.core.Bugsy
import com.bugsy.core.addBugsyInterceptor
import com.bugsy.ui.BugsyAndroidOverlay
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient.Builder().addBugsyInterceptor().build()
    private var status by mutableStateOf("Nenhuma request ainda")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+: sem esta permissão a notificação do Bugsy não aparece.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            // Prova de ponta a ponta de que o override do FlagsScreen chega até o app: ao ligar
            // "darkModeEnabled" no Bugsy, a tela vira preta sem precisar fechar e reabrir o app.
            val flags by Bugsy.flagManager.flags.collectAsState()
            val darkModeEnabled = flags.firstOrNull { it.key == "darkModeEnabled" }?.value == true
            val background = if (darkModeEnabled) Color.Black else Color.White
            val foreground = if (darkModeEnabled) Color.White else Color.Black

            Column(
                modifier = Modifier.fillMaxSize().background(background).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = ::makeRequest) { Text("Fazer request (OkHttp)") }
                Button(onClick = { Bugsy.show() }) { Text("Abrir Bugsy") }
                Text(status, color = foreground)
                Text(if (darkModeEnabled) "darkModeEnabled: ON" else "darkModeEnabled: off", color = foreground)
            }
            BugsyAndroidOverlay()
        }
    }

    private fun makeRequest() {
        thread {
            val request = Request.Builder()
                .url("https://httpbin.org/get")
                .header("Authorization", "Bearer sample-secret")
                .build()
            status = "Enviando..."
            status = runCatching { client.newCall(request).execute().use { "Status ${it.code}" } }
                .getOrElse { "Falhou: ${it.message}" }
        }
    }
}
