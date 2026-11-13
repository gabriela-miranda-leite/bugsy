package dev.bugsy.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.core.Bugsy
import com.bugsy.ui.BugsyAndroidOverlay
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = ::makeRequest) { Text("Fazer request (OkHttp)") }
                Button(onClick = { Bugsy.show() }) { Text("Abrir Bugsy") }
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
            runCatching { client.newCall(request).execute().close() }
        }
    }
}
