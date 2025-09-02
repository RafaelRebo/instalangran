package com.example.androidillo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

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
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("results") {
            val jsonResult = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("result")
            jsonResult?.let {
                val result = Json.decodeFromString<AnalyzeResponse>(it)
                ResultScreen(result, navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("") }
    var requireTwoFactor by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val client = remember {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    val textFieldShape = RoundedCornerShape(16.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.ojo_vago),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido",
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "Por favor, introduce tus credenciales:",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = textFieldShape,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFFF0F0F0),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = textFieldShape,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFFF0F0F0),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (requireTwoFactor) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = twoFactorCode,
                    onValueChange = { twoFactorCode = it },
                    label = { Text("Código 2FA") },
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = textFieldShape,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color(0xFFF0F0F0),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            val request = LoginRequest(
                                username = username,
                                password = password,
                                two_factor_code = if (requireTwoFactor) twoFactorCode else null
                            )
                            val response: AnalyzeResponse =
                                client.post("https://insta-backend-208344789626.europe-west1.run.app/analyze") {
                                    contentType(ContentType.Application.Json)
                                    setBody(request)
                                }.body()

                            error = null
                            requireTwoFactor = false

                            val jsonResult = Json.encodeToString(response)
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("result", jsonResult)

                            navController.navigate("results")

                        } catch (e: Exception) {
                            val message = e.localizedMessage ?: "Error desconocido"
                            if (message.contains("two factor", ignoreCase = true) ||
                                message.contains("verification code", ignoreCase = true)
                            ) {
                                requireTwoFactor = true
                                error = "Por favor ingresa el código 2FA que recibiste."
                            } else {
                                error = "Error: $message"
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.95f).height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Analizar", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ResultScreen(result: AnalyzeResponse, navController: NavHostController) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.ojo_vago),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Resultados del análisis",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.White // para que destaque sobre el fondo
            )

            // Primera fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                fun CardContent(title: String, number: Int) = @Composable {
                    Card(
                        modifier = Modifier.weight(1f).height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0).copy(alpha = 0.9f))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top) {
                                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$number",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                CardContent("Seguidores", result.followers_count)()
                CardContent("Seguidos", result.following_count)()
            }

            // Segunda fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                fun CardContentList(title: String, items: List<String>) = @Composable {
                    Card(
                        modifier = Modifier.weight(1f).height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0).copy(alpha = 0.9f))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top) {
                                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items.forEach { user ->
                                        Text(user, fontSize = 14.sp)
                                    }
                                }
                            }
                            Text(
                                "${items.size}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                CardContentList("No te siguen", result.not_following_back)()
                CardContentList("No los sigues", result.fans)()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.popBackStack("login", inclusive = false)
                    navController.navigate("login") { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Cerrar sesión", fontSize = 22.sp)
            }
        }
    }
}

