package com.example.androidillo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class LoginRequest(val username: String, val password: String, val two_factor_code: String? = null)

@Serializable
data class AnalyzeResponse(
    val followers_count: Int,
    val following_count: Int,
    val not_following_back: List<String>,
    val fans: List<String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstagramAnalyzerApp()
        }
    }
}

@Composable
fun InstagramAnalyzerApp() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("") }
    var requireTwoFactor by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AnalyzeResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val client = remember {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )

        if (requireTwoFactor) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = twoFactorCode,
                onValueChange = { twoFactorCode = it },
                label = { Text("Código 2FA") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = LoginRequest(
                        username = username,
                        password = password,
                        two_factor_code = if (requireTwoFactor) twoFactorCode else null
                    )
                    val response: AnalyzeResponse = client.post("https://instalangran-backend.onrender.com/analyze") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(request)
                    }.body()

                    result = response
                    error = null
                    requireTwoFactor = false

                } catch (e: Exception) {
                    val message = e.localizedMessage ?: "Error desconocido"
                    if (message.contains("two factor", ignoreCase = true) ||
                        message.contains("verification code", ignoreCase = true)) {
                        requireTwoFactor = true
                        error = "Por favor ingresa el código 2FA que recibiste."
                    } else {
                        error = "Error: $message"
                    }
                }
            }
        }) {
            Text("Analizar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        result?.let {
            Text("Seguidores: ${it.followers_count}")
            Text("Seguidos: ${it.following_count}")

            Text("No te siguen: ${it.not_following_back.size}")
            it.not_following_back.forEach { user ->
                Text(" - $user")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Tú no los sigues: ${it.fans.size}")
            it.fans.forEach { user ->
                Text(" - $user")
            }
        }


        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
