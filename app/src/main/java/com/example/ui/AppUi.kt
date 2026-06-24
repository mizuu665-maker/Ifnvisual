package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Order
import com.example.data.Product
import com.example.data.User
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// =======================================================
// MODERN GLASSMORPHIC COMPOSABLES & MODIFIERS
// =======================================================

@Composable
fun Modifier.glassmorphicCard(
    cornerRadius: Dp = 20.dp,
    shadowElevation: Dp = 8.dp
): Modifier {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) DarkSurface.copy(alpha = 0.65f) else LightSurface.copy(alpha = 0.85f)
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight

    return this
        .shadow(shadowElevation, RoundedCornerShape(cornerRadius), clip = false)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(bgColor, bgColor.copy(alpha = 0.4f))
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(borderColor, borderColor.copy(alpha = 0.1f))
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
fun GradientTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .testTag(testTag)
            .height(52.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(IfnBlue, IfnCyan)),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.getDp())
        )
    }
}

// Inline responsive helper to avoid standard dynamic density issues
@Composable
fun Int.getDp(): Dp = this.dp

// =======================================================
// MAIN UI CONTROLLER
// =======================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppUiContainer(viewModel: AppViewModel) {
    var currentScreen by remember { mutableStateOf("splash") }
    val screenStack = remember { mutableStateListOf<String>() }

    fun navigateTo(screen: String) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.size - 1)
        }
    }

    BackHandler(enabled = currentScreen != "splash" && currentScreen != "main") {
        navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // SIMULATED PUSH NOTIFICATION OVERLAY STATE
    var activePushNotification by remember { mutableStateOf<Pair<String, String>?>(null) }
    var notificationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.pushNotificationFlow.collectLatest { notif ->
            activePushNotification = notif
            notificationVisible = true
            delay(4000)
            notificationVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) DarkBg else LightBg)
    ) {
        // SCREEN ROUTING
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) with fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "splash" -> SplashScreen(onSplashFinished = {
                    currentScreen = "login"
                })
                "login" -> LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        currentScreen = "main"
                        screenStack.clear()
                    },
                    onNavigateToRegister = { navigateTo("register") },
                    onNavigateToForgot = { navigateTo("forgot_password") }
                )
                "register" -> RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        currentScreen = "main"
                        screenStack.clear()
                    },
                    onNavigateBack = { navigateBack() }
                )
                "forgot_password" -> ForgotPasswordScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
                "main" -> MainAppDashboard(
                    viewModel = viewModel,
                    onLogout = {
                        viewModel.logout()
                        currentScreen = "login"
                        screenStack.clear()
                    }
                )
            }
        }

        // SIMULATED PUSH NOTIFICATION SLIDE-DOWN BANNER
        AnimatedVisibility(
            visible = notificationVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .zIndex(999f)
        ) {
            activePushNotification?.let { (title, msg) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) DarkSurface.copy(alpha = 0.95f) else Color.White
                    ),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(IfnBlue, IfnCyan)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(listOf(IfnBlue, IfnCyan)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Push Alert",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF475569),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { notificationVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// SPLASH SCREEN
// =======================================================

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1200),
        label = "LogoAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF020617))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(
                        scaleX = scaleAnim,
                        scaleY = scaleAnim,
                        alpha = alphaAnim
                    )
            ) {
                // Load App Logo
                Image(
                    painter = painterResource(id = R.drawable.img_ifn_logo_1782342207075),
                    contentDescription = "IFN Visual Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "IFN_VISUAL",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                color = Color.White,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Creating Visuals With Soul",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = IfnCyan,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            )

            Spacer(modifier = Modifier.height(60.dp))

            CircularProgressIndicator(
                color = IfnCyan,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(alpha = alphaAnim)
            )
        }
    }
}

// =======================================================
// LOGIN SCREEN
// =======================================================

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) DarkBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // HEADER BRAND
            Card(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ifn_logo_1782342207075),
                    contentDescription = "Logo Header",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "IFN_VISUAL",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = "Creating Visuals With Soul",
                fontSize = 12.sp,
                color = IfnCyan,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // LOGIN FORM
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Masuk Akun",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Kelola dan pesan visual kustom berkarakter di sini.",
                        fontSize = 12.sp,
                        color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        placeholder = { Text("nama@email.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        leadingIcon = { Icon(Icons.Default.Email, "Email Icon") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IfnBlue,
                            unfocusedBorderColor = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IfnBlue,
                            unfocusedBorderColor = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = onNavigateToForgot) {
                            Text("Lupa Password?", color = IfnCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = IfnBlue,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(28.dp)
                        )
                    } else {
                        GradientTextButton(
                            text = "Login",
                            onClick = {
                                isSubmitting = true
                                viewModel.login(email, password,
                                    onSuccess = {
                                        isSubmitting = false
                                        onLoginSuccess()
                                    },
                                    onError = { err ->
                                        isSubmitting = false
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "login_button"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GOOGLE LOGIN BUTTON SIMULATOR
            OutlinedButton(
                onClick = {
                    viewModel.loginWithGoogle(
                        onSuccess = onLoginSuccess,
                        onError = { err -> errorMessage = err }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google Icon",
                    tint = IfnCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Masuk Dengan Google",
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEED QUICK LOGIN CHEATS FOR DEVELOPER TESTING CONVENIENCE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0x1F00C8FF) else Color(0xFFF1F5F9)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Demo Token (Klik Cepat):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = IfnBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InputChip(
                            selected = false,
                            onClick = {
                                email = "customer@ifn.com"
                                password = "customer123"
                            },
                            label = { Text("Tester Customer", fontSize = 10.sp) }
                        )
                        InputChip(
                            selected = false,
                            onClick = {
                                email = "admin@ifn.com"
                                password = "admin123"
                            },
                            label = { Text("Tester Admin", fontSize = 10.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Belum punya akun? ",
                    color = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray,
                    fontSize = 14.sp
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text("Register di sini", color = IfnBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// =======================================================
// REGISTER SCREEN
// =======================================================

@Composable
fun RegisterScreen(
    viewModel: AppViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var wa by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CUSTOMER") } // "CUSTOMER" or "ADMIN"
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) DarkBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
            }

            Text(
                text = "Daftar Akun",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "Bergabunglah dengan IFN_VISUAL untuk memesan desain berkualitas tinggi secara realtime.",
                fontSize = 13.sp,
                color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Nama Lengkap") },
                        placeholder = { Text("Contoh: Budi Santoso") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, "Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IfnBlue),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        placeholder = { Text("nama@email.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, "Email") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IfnBlue),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wa,
                        onValueChange = { wa = it; errorMessage = null },
                        label = { Text("Nomor WhatsApp") },
                        placeholder = { Text("Contoh: 081234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Call, "WhatsApp") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IfnBlue),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Daftar Sebagai:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = role == "CUSTOMER",
                                onClick = { role = "CUSTOMER" }
                            )
                            Text("Pelanggan (Customer)", fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = role == "ADMIN",
                                onClick = { role = "ADMIN" }
                            )
                            Text("Admin", fontSize = 13.sp)
                        }
                    }

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = IfnBlue,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(28.dp)
                        )
                    } else {
                        GradientTextButton(
                            text = "Register",
                            onClick = {
                                isSubmitting = true
                                viewModel.register(email, name, wa, role,
                                    onSuccess = {
                                        isSubmitting = false
                                        onRegisterSuccess()
                                    },
                                    onError = { err ->
                                        isSubmitting = false
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// =======================================================
// FORGOT PASSWORD SCREEN
// =======================================================

@Composable
fun ForgotPasswordScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var successSent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) DarkBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
            }

            Text(
                text = "Lupa Password",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "Masukkan alamat email akun Anda. Kami akan mengirimkan tautan pemulihan sandi secara instan.",
                fontSize = 13.sp,
                color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (successSent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Tautan Atur Ulang Sandi Terkirim!\nSilakan periksa folder kotak masuk/spam email Anda: $email",
                                color = Color(0xFF10B981),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Terdaftar") },
                            placeholder = { Text("nama@email.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, "Email") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IfnBlue),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        GradientTextButton(
                            text = "Kirim Link Atur Ulang",
                            onClick = {
                                viewModel.forgotPassword(email,
                                    onSuccess = { successSent = true },
                                    onError = {}
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// =======================================================
// MAIN DASHBOARD (HOLDS BOTTOM NAVIGATION & INNER TABS)
// =======================================================

@Composable
fun MainAppDashboard(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (isSystemInDarkTheme()) DarkSurface else LightSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val isDark = isSystemInDarkTheme()
                val activeColor = IfnBlue
                val inactiveColor = if (isDark) Color.Gray else Color.DarkGray

                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "products",
                    onClick = { selectedTab = "products" },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, "Products") },
                    label = { Text("Produk", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "order",
                    onClick = { selectedTab = "order" },
                    icon = { Icon(Icons.Default.ShoppingCart, "Order") },
                    label = { Text("Pesan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "progress",
                    onClick = { selectedTab = "progress" },
                    icon = { Icon(Icons.Default.Refresh, "Progress") },
                    label = { Text("Progres", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "payment",
                    onClick = { selectedTab = "payment" },
                    icon = { Icon(Icons.Default.AccountBalance, "Payment") },
                    label = { Text("Bayar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor.copy(alpha = 0.12f),
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                "home" -> HomeTabScreen(viewModel, onNavigateToTab = { selectedTab = it })
                "products" -> ProductsTabScreen(viewModel)
                "order" -> OrderFormTabScreen(viewModel, onOrderSubmitted = { selectedTab = "progress" })
                "progress" -> ProgressTrackingTabScreen(viewModel)
                "payment" -> PaymentTabScreen(viewModel)
                "profile" -> ProfileTabScreen(viewModel, onLogout = onLogout)
            }
        }
    }
}

// =======================================================
// TAB 1: HOME TAB
// =======================================================

@Composable
fun HomeTabScreen(
    viewModel: AppViewModel,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val user = viewModel.currentUser
    val isAdmin = user?.profileRole == "ADMIN"

    // Stat State Flow values
    val totalOrders by viewModel.totalOrdersCount.collectAsStateWithLifecycle()
    val activeOrders by viewModel.activeOrdersCount.collectAsStateWithLifecycle()
    val completedOrders by viewModel.completedOrdersCount.collectAsStateWithLifecycle()
    val monthlyIncomeVal by viewModel.monthlyIncome.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.getDp()),
        verticalArrangement = Arrangement.spacedBy(16.getDp())
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // BRAND BANNER WIDESCREEN
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ifn_banner_1782342220963),
                        contentDescription = "Visual Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Glassy Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "IFN_VISUAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Creating Visuals With Soul",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = IfnCyan
                        )
                    }
                }
            }
        }

        // HELLO WELCOME USER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Brush.linearGradient(listOf(IfnBlue, IfnCyan)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.fullName?.take(1) ?: "U").uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Halo, ${user?.fullName ?: "Tamu"}! 👋",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isAdmin) "Role: Administrator Agency" else "Pesan desain impian Anda dengan mudah.",
                            fontSize = 12.sp,
                            color = IfnCyan
                        )
                    }
                }
            }
        }

        // STATS SUMMARY (DASHBOARD)
        item {
            Text(
                text = if (isAdmin) "Statistik Bisnis (Admin)" else "Ikhtisar Akun Anda",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Orders
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .glassmorphicCard(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Total Pesanan", fontSize = 11.sp, color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray)
                            Text("$totalOrders", fontSize = 24.sp, fontWeight = FontWeight.Black, color = IfnBlue)
                        }
                    }

                    // Processed Orders
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .glassmorphicCard(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Sedang Diproses", fontSize = 11.sp, color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray)
                            Text("$activeOrders", fontSize = 24.sp, fontWeight = FontWeight.Black, color = IfnCyan)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Completed Orders
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .glassmorphicCard(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Selesai", fontSize = 11.sp, color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray)
                            Text("$completedOrders", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        }
                    }

                    // Revenue (Admin only displays real, customer displays total cost they made)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .glassmorphicCard(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isAdmin) "Pendapatan Bulanan" else "Total Belanja",
                                fontSize = 11.sp,
                                color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                            )
                            Text(
                                text = viewModel.formatRupiah(monthlyIncomeVal),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // MENU QUICK ACTIONS NAVIGATION
        item {
            Text(
                text = "Pintasan Menu",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeActionRow(
                    title1 = "Katalog Produk",
                    subtitle1 = "Kategori Lengkap",
                    icon1 = Icons.AutoMirrored.Filled.List,
                    color1 = IfnBlue,
                    onClick1 = { onNavigateToTab("products") },
                    title2 = "Form Pemesanan",
                    subtitle2 = "Buat Order Desain",
                    icon2 = Icons.Default.ShoppingCart,
                    color2 = IfnCyan,
                    onClick2 = { onNavigateToTab("order") }
                )

                HomeActionRow(
                    title1 = "Progres Tracking",
                    subtitle1 = "Pantau Realtime",
                    icon1 = Icons.Default.Refresh,
                    color1 = Color(0xFFF59E0B),
                    onClick1 = { onNavigateToTab("progress") },
                    title2 = "Metode Pembayaran",
                    subtitle2 = "BRI, QRIS, E-Wallet",
                    icon2 = Icons.Default.AccountBalance,
                    color2 = Color(0xFF10B981),
                    onClick2 = { onNavigateToTab("payment") }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun HomeActionRow(
    title1: String, subtitle1: String, icon1: androidx.compose.ui.graphics.vector.ImageVector, color1: Color, onClick1: () -> Unit,
    title2: String, subtitle2: String, icon2: androidx.compose.ui.graphics.vector.ImageVector, color2: Color, onClick2: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick1() }
                .glassmorphicCard(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color1.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon1, contentDescription = title1, tint = color1)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(title1, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A))
                Text(subtitle1, fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick2() }
                .glassmorphicCard(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color2.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon2, contentDescription = title2, tint = color2)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(title2, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A))
                Text(subtitle2, fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
            }
        }
    }
}

// =======================================================
// TAB 2: PRODUCTS CATALOG (AND PRODUCT MANAGEMENT)
// =======================================================

@Composable
fun ProductsTabScreen(viewModel: AppViewModel) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val user = viewModel.currentUser
    val isAdmin = user?.profileRole == "ADMIN"

    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Desain Grafis", "Motion Graphic", "Video Editing", "Branding")

    // Modals & Sheets
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = if (selectedCategory == "Semua") {
        products
    } else {
        products.filter { it.category == selectedCategory }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Katalog Jasa Visual",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Karya visual kustom yang berjiwa dan bertenaga.",
                        fontSize = 12.sp,
                        color = IfnCyan
                    )
                }

                if (isAdmin) {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(listOf(IfnBlue, IfnCyan)),
                                RoundedCornerShape(12.dp)
                            )
                            .size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, "Tambah Produk", tint = Color.White)
                    }
                }
            }

            // Categories horizontal filter list
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Card(
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) IfnBlue else if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) IfnBlue.copy(alpha = 0.15f) else Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) IfnBlue else if (isSystemInDarkTheme()) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada produk di kategori ini.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { prod ->
                        ProductItemCard(
                            product = prod,
                            isAdmin = isAdmin,
                            onDetailClick = { selectedProductForDetail = prod },
                            onDeleteClick = { viewModel.deleteProduct(prod) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // DIALOG ADD PRODUCT (ADMIN ONLY)
        if (showAddDialog) {
            ProductAddEditDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, cat, price, desc, duration ->
                    viewModel.addProduct(name, cat, price, desc, duration)
                    showAddDialog = false
                }
            )
        }

        // DIALOG PRODUCT DETAIL
        selectedProductForDetail?.let { prod ->
            ProductDetailDialog(
                product = prod,
                onDismiss = { selectedProductForDetail = null }
            )
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    isAdmin: Boolean,
    onDetailClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() }
            .glassmorphicCard(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing category style
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(IfnBlue, IfnCyan)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (product.category) {
                        "Desain Grafis" -> Icons.Default.Brush
                        "Motion Graphic" -> Icons.Default.PlayArrow
                        "Video Editing" -> Icons.Default.Movie
                        else -> Icons.Default.Star
                    },
                    contentDescription = "Visual Style",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.category,
                    fontSize = 11.sp,
                    color = IfnCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.getDp()))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mulai dari: ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Rp ${String.format("%,d", product.price).replace(',', '.')}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            Row {
                if (isAdmin) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Detail",
                    tint = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun ProductDetailDialog(product: Product, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicCard(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Layanan",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.linearGradient(listOf(IfnBlue, IfnCyan)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Visual Service icon",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )

                Text(
                    text = product.category,
                    fontSize = 12.sp,
                    color = IfnCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = product.description,
                    fontSize = 13.sp,
                    color = if (isSystemInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF475569),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ESTIMASI DURASI", fontSize = 10.sp, color = Color.Gray)
                        Text(product.duration, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("HARGA JASA", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "Rp ${String.format("%,d", product.price).replace(',', '.')}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = IfnBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductAddEditDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Desain Grafis") }
    val categories = listOf("Desain Grafis", "Motion Graphic", "Video Editing", "Branding")
    var priceStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicCard(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambah Produk Jasa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Kategori Layanan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val selected = cat == category
                        InputChip(
                            selected = selected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Harga (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Durasi Kerja (Contoh: 2-3 Hari)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Layanan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientTextButton(
                        text = "Simpan",
                        onClick = {
                            val price = priceStr.toLongOrNull() ?: 0L
                            if (name.isNotEmpty() && price > 0 && duration.isNotEmpty()) {
                                onSave(name, category, price, description, duration)
                            }
                        }
                    )
                }
            }
        }
    }
}

// =======================================================
// TAB 3: ORDER FORM (PEMESANAN)
// =======================================================

@Composable
fun OrderFormTabScreen(
    viewModel: AppViewModel,
    onOrderSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val user = viewModel.currentUser

    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var whatsapp by remember { mutableStateOf(user?.whatsappNumber ?: "") }
    var detailRequirements by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Desain Grafis") }
    var referenceUrl by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Desain Grafis", "Motion Graphic", "Video Editing", "Branding")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.getDp())
    ) {
        Text(
            text = "Form Pemesanan Desain",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
        )
        Text(
            text = "Isi rincian visual impian Anda di bawah.",
            fontSize = 12.sp,
            color = IfnCyan,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicCard(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Client Info Section
                Text(
                    text = "KONTAK PELANGGAN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = IfnBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp (Aktif)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Service Spec Section
                Text(
                    text = "SPESIFIKASI VISUAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = IfnBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text("Jenis / Kategori Layanan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        InputChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = detailRequirements,
                    onValueChange = { detailRequirements = it },
                    label = { Text("Detail / Konsep Pesanan") },
                    placeholder = { Text("Contoh: Poster bertema cyber-punk warna neon cyan, logo naga di pojok, tulisan Launching 2026.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = referenceUrl,
                    onValueChange = { referenceUrl = it },
                    label = { Text("Link / Unggah Referensi (Google Drive/Pinterest)") },
                    placeholder = { Text("https://pin.it/xxxxxx") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deadlineDate,
                    onValueChange = { deadlineDate = it },
                    label = { Text("Deadline Penyelesaian (Contoh: 30 Juni 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                GradientTextButton(
                    text = "Submit Pemesanan",
                    onClick = {
                        if (name.isNotEmpty() && whatsapp.isNotEmpty() && detailRequirements.isNotEmpty() && deadlineDate.isNotEmpty()) {
                            viewModel.createOrder(
                                customerName = name,
                                customerWhatsapp = whatsapp,
                                productType = selectedCategory,
                                detailPesanan = detailRequirements,
                                referenceUri = referenceUrl.ifEmpty { null },
                                deadline = deadlineDate,
                                catatan = notes.ifEmpty { null },
                                onSuccess = { orderNum ->
                                    Toast.makeText(context, "Pemesanan berhasil dibuat: $orderNum", Toast.LENGTH_LONG).show()
                                    onOrderSubmitted()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Mohon lengkapi seluruh kolom wajib!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// =======================================================
// TAB 4: ORDER PROGRESS TRACKING (PROGRES PESANAN)
// =======================================================

@Composable
fun ProgressTrackingTabScreen(viewModel: AppViewModel) {
    val orders by if (viewModel.currentUser?.profileRole == "ADMIN") {
        viewModel.allOrders.collectAsStateWithLifecycle()
    } else {
        viewModel.getCustomerOrders().collectAsStateWithLifecycle(emptyList())
    }

    var selectedOrderForProgress by remember { mutableStateOf<Order?>(null) }
    var showAdminUpdateDialog by remember { mutableStateOf(false) }

    val isAdmin = viewModel.currentUser?.profileRole == "ADMIN"

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (isAdmin) "Kelola Progres Semua Pesanan" else "Pantau Progres Desain",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )
        Text(
            text = if (isAdmin) "Perbarui tahapan progres pengerjaan klien secara realtime." else "Lacak pesanan visual Anda dari draft hingga final.",
            fontSize = 12.sp,
            color = IfnCyan,
            modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        )

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "No orders",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada pesanan terdaftar.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { ord ->
                    OrderProgressCard(
                        order = ord,
                        onTrackClick = { selectedOrderForProgress = ord },
                        onAdminUpdateClick = {
                            selectedOrderForProgress = ord
                            showAdminUpdateDialog = true
                        },
                        isAdmin = isAdmin
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // TRACKING HISTORY DIALOG modal
    selectedOrderForProgress?.let { ord ->
        if (showAdminUpdateDialog && isAdmin) {
            AdminProgressUpdateDialog(
                order = ord,
                onDismiss = {
                    selectedOrderForProgress = null
                    showAdminUpdateDialog = false
                },
                onSave = { status, percentage, estimate, reason ->
                    viewModel.updateOrderStatus(ord.id, status, percentage, estimate, reason)
                    selectedOrderForProgress = null
                    showAdminUpdateDialog = false
                }
            )
        } else if (!showAdminUpdateDialog) {
            ProgressDetailTrackDialog(
                viewModel = viewModel,
                order = ord,
                onDismiss = { selectedOrderForProgress = null }
            )
        }
    }
}

@Composable
fun OrderProgressCard(
    order: Order,
    onTrackClick: () -> Unit,
    onAdminUpdateClick: () -> Unit,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = IfnCyan
                    )
                    Text(
                        text = "Produk: ${order.productType}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                }

                // Status Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (order.status) {
                            "Selesai" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "Pesanan Diterima" -> IfnBlue.copy(alpha = 0.15f)
                            "Revisi" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> IfnCyan.copy(alpha = 0.15f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status,
                        color = when (order.status) {
                            "Selesai" -> Color(0xFF10B981)
                            "Pesanan Diterima" -> IfnBlue
                            "Revisi" -> Color(0xFFF59E0B)
                            else -> IfnCyan
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated / Static progress line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progres: ${order.progressPercentage}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "Deadline: ${order.deadline}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Slider Bar
            LinearProgressIndicator(
                progress = { order.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Brush.horizontalGradient(listOf(IfnBlue, IfnCyan)).let { IfnCyan }, // Standard linear indicator takes simple color
                trackColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isAdmin) {
                    OutlinedButton(
                        onClick = onAdminUpdateClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Update (Admin)", fontSize = 12.sp)
                    }
                }

                GradientTextButton(
                    text = "Lacak Progres",
                    onClick = onTrackClick,
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

@Composable
fun ProgressDetailTrackDialog(
    viewModel: AppViewModel,
    order: Order,
    onDismiss: () -> Unit
) {
    val trackingLogs by viewModel.getTrackingByOrder(order.id).collectAsStateWithLifecycle(emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .glassmorphicCard(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Realtime Tracking",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Text(
                    text = order.orderNumber,
                    fontSize = 13.sp,
                    color = IfnCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Current Visual Status Progress circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(IfnBlue.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, IfnBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${order.progressPercentage}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = IfnBlue
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = order.status,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Estimasi Selesai: ${order.estimasiSelesai}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = "RIWAYAT UPDATE PROGRESS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = IfnBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (trackingLogs.isEmpty()) {
                        item {
                            Text(
                                "Belum ada riwayat pengerjaan.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(trackingLogs) { log ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(IfnCyan, CircleShape)
                                        .align(Alignment.Top)
                                        .padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.status,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = IfnCyan
                                        )
                                        Text(
                                            text = SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault()).format(Date(log.timestamp)),
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = log.description,
                                        fontSize = 11.sp,
                                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProgressUpdateDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String) -> Unit
) {
    var status by remember { mutableStateOf(order.status) }
    var percentageStr by remember { mutableStateOf(order.progressPercentage.toString()) }
    var estimate by remember { mutableStateOf(order.estimasiSelesai) }
    var updateLogText by remember { mutableStateOf("") }

    val statusOptions = listOf("Pesanan Diterima", "Sedang Dikerjakan", "Revisi", "Finalisasi", "Selesai")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicCard(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Update Status Pesanan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )
                Text(order.orderNumber, fontSize = 12.sp, color = IfnCyan)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Status Tahapan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    statusOptions.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = status == opt, onClick = { status = opt })
                            Text(opt, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = percentageStr,
                    onValueChange = { percentageStr = it },
                    label = { Text("Progres (%) - 0 sampai 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = estimate,
                    onValueChange = { estimate = it },
                    label = { Text("Estimasi Selesai") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = updateLogText,
                    onValueChange = { updateLogText = it },
                    label = { Text("Catatan Detail Log Update") },
                    placeholder = { Text("Contoh: Draft layout awal poster telah selesai, bersiap rendering.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientTextButton(
                        text = "Simpan",
                        onClick = {
                            val pct = percentageStr.toIntOrNull() ?: order.progressPercentage
                            onSave(status, pct, estimate, updateLogText)
                        }
                    )
                }
            }
        }
    }
}

// =======================================================
// TAB 5: PAYMENTS (PEMBAYARAN & PAYMENT VERIFICATION)
// =======================================================

@Composable
fun PaymentTabScreen(viewModel: AppViewModel) {
    val user = viewModel.currentUser
    val isAdmin = user?.profileRole == "ADMIN"

    val orders by if (isAdmin) {
        viewModel.allOrders.collectAsStateWithLifecycle()
    } else {
        viewModel.getCustomerOrders().collectAsStateWithLifecycle(emptyList())
    }

    val payments by viewModel.allPayments.collectAsStateWithLifecycle()

    var showQrisFullModal by remember { mutableStateOf(false) }
    var selectedOrderForPayment by remember { mutableStateOf<Order?>(null) }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (isAdmin) "Verifikasi Pembayaran" else "Menu Pembayaran Jasa",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
            Text(
                text = if (isAdmin) "Verifikasi bukti transfer dari pelanggan di bawah ini." else "Pilih orderan Anda yang aktif dan lakukan transfer.",
                fontSize = 12.sp,
                color = IfnCyan,
                modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            )

            if (!isAdmin) {
                // CUSTOMER PAYMENT CARDS (GUIDANCE + PAY DETAILS)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PAYMENT GUIDE
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicCard(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "REKENING RESMI IFN_VISUAL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = IfnBlue
                                )
                                Spacer(modifier = Modifier.height(8.getDp()))

                                // BANK BRI
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Transfer Bank BRI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("No. Rek: 1234-5678-9012-345", fontSize = 12.sp)
                                        Text("A/N: IFN_VISUAL AGENCY", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    OutlinedButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("No. Rek BRI", "123456789012345")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "No. Rekening BRI berhasil disalin!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Salin", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(10.dp))

                                // E-WALLET DANA & GOPAY
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("E-Wallet (DANA, GoPay)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("No. Akun: 0812-3456-7890", fontSize = 12.sp)
                                    }
                                    GradientTextButton(
                                        text = "QRIS Full",
                                        onClick = { showQrisFullModal = true },
                                        modifier = Modifier.height(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Lakukan Pembayaran Untuk Orderan Anda:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Outstanding order list to pay
                    val unpaidOrders = orders.filter { it.status == "Pesanan Diterima" }
                    if (unpaidOrders.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tidak ada orderan tertunda yang butuh pembayaran.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(unpaidOrders) { ord ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(ord.orderNumber, fontWeight = FontWeight.Black, fontSize = 13.sp, color = IfnCyan)
                                        Text("Layanan: ${ord.productType}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    GradientTextButton(
                                        text = "Bayar / Konfirmasi",
                                        onClick = { selectedOrderForPayment = ord },
                                        modifier = Modifier.height(40.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Status Pembayaran Terkirim:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Logged payments list
                    if (payments.isEmpty()) {
                        item {
                            Text("Belum ada bukti pembayaran terkirim.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(payments) { pay ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Order: ${pay.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Via: ${pay.paymentMethod} | Jml: ${viewModel.formatRupiah(pay.amount)}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = when (pay.status) {
                                                "Pembayaran Diterima" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                "Pembayaran Ditolak" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = pay.status,
                                            color = when (pay.status) {
                                                "Pembayaran Diterima" -> Color(0xFF10B981)
                                                "Pembayaran Ditolak" -> MaterialTheme.colorScheme.error
                                                else -> Color(0xFFF59E0B)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ADMIN PAYMENT LIST TO VERIFY APPROVED / REJECTED
                val pendingPayments = payments.filter { it.status == "Menunggu Verifikasi" }
                if (pendingPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Semua pembayaran terkirim telah diverifikasi sukses.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingPayments) { pay ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(pay.orderNumber, fontWeight = FontWeight.Black, fontSize = 14.sp, color = IfnCyan)
                                            Text("Via: ${pay.paymentMethod}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text(
                                            text = viewModel.formatRupiah(pay.amount),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = IfnBlue
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Bukti Transfer Pelanggan (Link/Path):", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = pay.proofUri ?: "Tidak dilampirkan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { viewModel.verifyPayment(pay.id, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Tolak (Reject)", fontSize = 12.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = { viewModel.verifyPayment(pay.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Verifikasi Sukses", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // QRIS MODAL DIALOG
        if (showQrisFullModal) {
            QrisFullModal(onDismiss = { showQrisFullModal = false })
        }

        // SUBMIT PAYMENT PROOF DIALOG
        selectedOrderForPayment?.let { ord ->
            SubmitPaymentProofDialog(
                order = ord,
                onDismiss = { selectedOrderForPayment = null },
                onSubmit = { method, amount, proofUri ->
                    viewModel.submitPayment(ord.id, ord.orderNumber, method, amount, proofUri)
                    selectedOrderForPayment = null
                }
            )
        }
    }
}

@Composable
fun QrisFullModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicCard(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SCAN QRIS IFN_VISUAL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.getDp()))

                // Fake QR code displaying modern graphics
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Modern Vector representation of a QR matrix
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(16) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(16) {
                                    val isFilled = (0..2).random() != 0
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (isFilled) Color.Black else Color.White,
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("QRIS Resmi - Berlaku untuk DANA, GoPay, OVO, LinkAja", fontSize = 11.sp, color = Color.Gray)
                Text("IFN_VISUAL CREATIVE AGENCY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IfnCyan)
            }
        }
    }
}

@Composable
fun SubmitPaymentProofDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmit: (String, Long, String) -> Unit
) {
    var method by remember { mutableStateOf("BRI") }
    var amountStr by remember { mutableStateOf("") }
    var proofLink by remember { mutableStateOf("") }

    val methods = listOf("BRI", "DANA", "GoPay")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicCard(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Konfirmasi Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                )
                Text(order.orderNumber, fontSize = 12.sp, color = IfnCyan)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Metode Pembayaran Transfer:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { met ->
                        val isSel = met == method
                        InputChip(
                            selected = isSel,
                            onClick = { method = met },
                            label = { Text(met) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Jumlah Pembayaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = proofLink,
                    onValueChange = { proofLink = it },
                    label = { Text("Link Bukti Transfer (G-Drive / Screenshot)") },
                    placeholder = { Text("Contoh: https://link_bukti_anda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientTextButton(
                        text = "Kirim",
                        onClick = {
                            val amt = amountStr.toLongOrNull() ?: 0L
                            if (amt > 0 && proofLink.isNotEmpty()) {
                                onSubmit(method, amt, proofLink)
                            }
                        }
                    )
                }
            }
        }
    }
}

// =======================================================
// TAB 6: PROFILE & COMPANY PROFILE SECTION
// =======================================================

@Composable
fun ProfileTabScreen(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val user = viewModel.currentUser

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.getDp()),
        verticalArrangement = Arrangement.spacedBy(16.getDp())
    ) {
        // IFN VISUAL HEADER COMPANY PROFILE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ifn_logo_1782342207075),
                            contentDescription = "Profile Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "IFN_VISUAL",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Creating Visuals With Soul",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = IfnCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Layanan Desain Grafis, Motion Graphic, Branding, dan Video Editing dengan visual berkarakter orisinal kuat.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = if (isSystemInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // SERVICES LIST
        item {
            Text(
                text = "Layanan Utama Kami",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Desain Grafis", "Motion Graphic", "Branding", "Video Editing").forEach { service ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E2D4A) else Color(0xFFF1F5F9)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = service,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = IfnBlue
                            )
                        }
                    }
                }
            }
        }

        // AGENCY CONTACT SECTION
        item {
            Text(
                text = "Hubungi IFN_VISUAL",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // WhatsApp
                    ContactRow(
                        icon = Icons.Default.Call,
                        label = "WhatsApp Resmi Agency",
                        detail = "0812-3456-7890",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/6281234567890")
                            }
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Instagram
                    ContactRow(
                        icon = Icons.Default.Share,
                        label = "Instagram Portfolio",
                        detail = "@ifn_visual",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://instagram.com/ifn_visual")
                            }
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Email
                    ContactRow(
                        icon = Icons.Default.Email,
                        label = "Email Marketing & Inquiry",
                        detail = "hello@ifnvisual.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:hello@ifnvisual.com")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // USER ACCOUNT PROFILE & LOGOUT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = user?.fullName ?: "Akun Guest",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = user?.email ?: "guest@ifn.com",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log Out", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(IfnBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = IfnBlue)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(detail, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A))
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Contact action",
            tint = Color.Gray
        )
    }
}
