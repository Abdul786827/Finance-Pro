package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthResult
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BiometricSecurityManager
import com.example.util.SecurityUtils
import androidx.fragment.app.FragmentActivity

enum class AuthMode {
    LOGIN,
    SIGNUP,
    VERIFY_EMAIL,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: FinanceViewModel,
    initialMode: AuthMode = AuthMode.LOGIN,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var currentMode by remember { mutableStateOf(initialMode) }

    // Signup form state
    var signupFullName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupMobile by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }
    var signupConfirmVisible by remember { mutableStateOf(false) }
    var signupAgreeTerms by remember { mutableStateOf(false) }
    var signupErrorMessage by remember { mutableStateOf<String?>(null) }

    // Login form state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var loginRememberMe by remember { mutableStateOf(true) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    // Verification state
    var pendingVerificationEmail by remember { mutableStateOf("") }
    var pendingVerificationToken by remember { mutableStateOf("") }
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }

    // Forgot password state
    var forgotEmail by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotConfirmPassword by remember { mutableStateOf("") }
    var forgotPasswordVisible by remember { mutableStateOf(false) }
    var forgotStatusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldPrimaryLight.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Header
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = EmeraldPrimaryLight,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Finance Pro Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Selector Pill (Sign In vs Create Account)
            if (currentMode == AuthMode.LOGIN || currentMode == AuthMode.SIGNUP) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (currentMode == AuthMode.LOGIN)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                                .clickable {
                                    currentMode = AuthMode.LOGIN
                                    loginErrorMessage = null
                                }
                                .testTag("auth_tab_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMode == AuthMode.LOGIN) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (currentMode == AuthMode.SIGNUP)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                                .clickable {
                                    currentMode = AuthMode.SIGNUP
                                    signupErrorMessage = null
                                }
                                .testTag("auth_tab_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMode == AuthMode.SIGNUP) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Animated Screen Content
            AnimatedContent(
                targetState = currentMode,
                label = "auth_screen_transition"
            ) { mode ->
                when (mode) {
                    AuthMode.SIGNUP -> {
                        // SIGNUP FORM IN WORKING POSITION
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_form"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Create Your Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start managing accounts and daily statements from zero with complete privacy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                signupErrorMessage?.let { errorMsg ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = errorMsg,
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Full Name
                                OutlinedTextField(
                                    value = signupFullName,
                                    onValueChange = {
                                        signupFullName = it
                                        signupErrorMessage = null
                                    },
                                    label = { Text("Full Name *") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_fullname_input")
                                )

                                // Email
                                OutlinedTextField(
                                    value = signupEmail,
                                    onValueChange = {
                                        signupEmail = it
                                        signupErrorMessage = null
                                    },
                                    label = { Text("Email Address *") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_email_input")
                                )

                                // Mobile Number (Optional)
                                OutlinedTextField(
                                    value = signupMobile,
                                    onValueChange = { signupMobile = it },
                                    label = { Text("Mobile Number (Optional)") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    placeholder = { Text("+92 300 1234567") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_mobile_input")
                                )

                                // Password
                                OutlinedTextField(
                                    value = signupPassword,
                                    onValueChange = {
                                        signupPassword = it
                                        signupErrorMessage = null
                                    },
                                    label = { Text("Password * (Min 6 chars)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                            Icon(
                                                imageVector = if (signupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (signupPasswordVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_password_input")
                                )

                                // Confirm Password
                                OutlinedTextField(
                                    value = signupConfirmPassword,
                                    onValueChange = {
                                        signupConfirmPassword = it
                                        signupErrorMessage = null
                                    },
                                    label = { Text("Confirm Password *") },
                                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { signupConfirmVisible = !signupConfirmVisible }) {
                                            Icon(
                                                imageVector = if (signupConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (signupConfirmVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (signupConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_confirm_password_input")
                                )

                                // Agree to Terms Checkbox
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { signupAgreeTerms = !signupAgreeTerms },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = signupAgreeTerms,
                                        onCheckedChange = { signupAgreeTerms = it },
                                        modifier = Modifier.testTag("signup_terms_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "I agree to Terms & Conditions and Statement Policy",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Submit Button
                                Button(
                                    onClick = {
                                        // Validate fields
                                        when {
                                            signupFullName.isBlank() -> {
                                                signupErrorMessage = "Please enter your full name."
                                            }
                                            signupEmail.isBlank() || !signupEmail.contains("@") -> {
                                                signupErrorMessage = "Please enter a valid email address."
                                            }
                                            signupPassword.length < 6 -> {
                                                signupErrorMessage = "Password must be at least 6 characters long."
                                            }
                                            signupPassword != signupConfirmPassword -> {
                                                signupErrorMessage = "Passwords do not match."
                                            }
                                            !signupAgreeTerms -> {
                                                signupErrorMessage = "Please agree to the Terms & Conditions."
                                            }
                                            else -> {
                                                signupErrorMessage = null
                                                viewModel.register(
                                                    fullName = signupFullName.trim(),
                                                    email = signupEmail.trim(),
                                                    mobile = signupMobile.trim(),
                                                    rawPassword = signupPassword.trim()
                                                ) { token ->
                                                    pendingVerificationEmail = signupEmail.trim()
                                                    pendingVerificationToken = token
                                                    verificationStatusMessage = null
                                                    currentMode = AuthMode.VERIFY_EMAIL
                                                    Toast.makeText(context, "Account created! Please verify your email.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("signup_submit_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                // Switch to Login link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Already have an account? ", style = MaterialTheme.typography.bodySmall)
                                    TextButton(
                                        onClick = { currentMode = AuthMode.LOGIN },
                                        modifier = Modifier.testTag("switch_to_login_button")
                                    ) {
                                        Text("Sign In", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    AuthMode.LOGIN -> {
                        // LOGIN FORM
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_form"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Welcome Back",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sign in to access your statement management ledger.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                loginErrorMessage?.let { errorMsg ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("login_error_banner")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = errorMsg,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (errorMsg.contains("Incorrect password", ignoreCase = true)) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                TextButton(
                                                    onClick = {
                                                        forgotEmail = loginEmail.trim()
                                                        currentMode = AuthMode.FORGOT_PASSWORD
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(28.dp).testTag("inline_reset_password_button")
                                                ) {
                                                    Text(
                                                        text = "Forgot password? Tap here to reset it.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Email
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = {
                                        loginEmail = it
                                        loginErrorMessage = null
                                    },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_email_input")
                                )

                                // Password
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        loginErrorMessage = null
                                    },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (loginPasswordVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                // Remember Me & Forgot Password
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { loginRememberMe = !loginRememberMe }
                                    ) {
                                        Checkbox(
                                            checked = loginRememberMe,
                                            onCheckedChange = { loginRememberMe = it }
                                        )
                                        Text("Remember Me", style = MaterialTheme.typography.bodySmall)
                                    }

                                    TextButton(
                                        onClick = {
                                            forgotEmail = loginEmail.trim()
                                            currentMode = AuthMode.FORGOT_PASSWORD
                                        },
                                        modifier = Modifier.testTag("forgot_password_button")
                                    ) {
                                        Text("Forgot Password?", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                // Sign In Button
                                Button(
                                    onClick = {
                                        val cleanEmail = loginEmail.trim()
                                        val cleanPassword = loginPassword.trim()
                                        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                                            loginErrorMessage = "Please enter both email and password."
                                            return@Button
                                        }

                                        viewModel.login(cleanEmail, cleanPassword) { result ->
                                            when (result) {
                                                is AuthResult.Success -> {
                                                    Toast.makeText(context, "Welcome ${result.profile.fullName.ifBlank { "User" }}!", Toast.LENGTH_SHORT).show()
                                                    onAuthSuccess()
                                                }
                                                is AuthResult.EmailNotVerified -> {
                                                    pendingVerificationEmail = result.email
                                                    pendingVerificationToken = result.token
                                                    verificationStatusMessage = "Email verification is required before signing in."
                                                    currentMode = AuthMode.VERIFY_EMAIL
                                                }
                                                is AuthResult.Error -> {
                                                    loginErrorMessage = result.message
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("login_submit_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                // Biometric / Fingerprint Option (Only shown when enabled by user AND hardware/fingerprint is enrolled)
                                val canUseBiometric = remember(context, userProfile) {
                                    (userProfile?.isBiometricEnabled == true) && BiometricSecurityManager.canAuthenticate(context)
                                }
                                if (canUseBiometric) {
                                    OutlinedButton(
                                        onClick = {
                                            val activity = context as? FragmentActivity
                                            if (activity != null) {
                                                BiometricSecurityManager.authenticate(
                                                    activity = activity,
                                                    title = "Unlock Finance Pro",
                                                    subtitle = "Touch the fingerprint sensor to sign in",
                                                    onSucceeded = {
                                                        viewModel.unlockWithBiometric {
                                                            onAuthSuccess()
                                                        }
                                                    },
                                                    onError = { err ->
                                                        loginErrorMessage = err
                                                    }
                                                )
                                            } else {
                                                loginErrorMessage = "Biometric authentication not supported in this context."
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("biometric_login_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Fingerprint Login", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Switch to Sign Up link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Don't have an account? ", style = MaterialTheme.typography.bodySmall)
                                    TextButton(
                                        onClick = { currentMode = AuthMode.SIGNUP },
                                        modifier = Modifier.testTag("switch_to_signup_button")
                                    ) {
                                        Text("Create Account", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    AuthMode.VERIFY_EMAIL -> {
                        // EMAIL VERIFICATION SYSTEM
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verify_email_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    color = FinanceIncomeBg
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MarkEmailRead,
                                            contentDescription = null,
                                            tint = FinanceIncomeGreen,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Verify Your Email Address",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "A verification code has been generated for:\n$pendingVerificationEmail",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                // Simulated Email Box
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Verification Dispatch (30 min expiration)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Security Token: $pendingVerificationToken",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Please tap 'Verify My Email' below to activate your account and start with a fresh statement.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // One-click Verify Button
                                Button(
                                    onClick = {
                                        viewModel.verifyEmail(pendingVerificationToken) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Email Verified Successfully! Logging in...", Toast.LENGTH_SHORT).show()
                                                // Sign in user
                                                viewModel.login(pendingVerificationEmail, signupPassword.ifBlank { "123456" }) {
                                                    onAuthSuccess()
                                                }
                                            } else {
                                                Toast.makeText(context, "Verification failed or token expired.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("verify_email_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FinanceIncomeGreen)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify My Email & Continue", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.resendVerification { newToken ->
                                                pendingVerificationToken = newToken
                                                Toast.makeText(context, "New verification token sent!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("resend_verification_button")
                                    ) {
                                        Text("Resend Email")
                                    }

                                    TextButton(onClick = { currentMode = AuthMode.LOGIN }) {
                                        Text("Back to Sign In")
                                    }
                                }
                            }
                        }
                    }

                    AuthMode.FORGOT_PASSWORD -> {
                        // FORGOT PASSWORD FORM
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_form"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Reset Your Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter your registered email address to create a new password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                forgotStatusMessage?.let { msg ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = msg,
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = forgotEmail,
                                    onValueChange = { forgotEmail = it },
                                    label = { Text("Registered Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = forgotNewPassword,
                                    onValueChange = { forgotNewPassword = it },
                                    label = { Text("New Password (Min 6 chars)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = if (forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { forgotPasswordVisible = !forgotPasswordVisible }) {
                                            Icon(
                                                imageVector = if (forgotPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = forgotConfirmPassword,
                                    onValueChange = { forgotConfirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                    visualTransformation = if (forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (forgotEmail.isBlank()) {
                                            forgotStatusMessage = "Please enter your email."
                                            return@Button
                                        }
                                        if (forgotNewPassword.length < 6) {
                                            forgotStatusMessage = "Password must be at least 6 characters."
                                            return@Button
                                        }
                                        if (forgotNewPassword != forgotConfirmPassword) {
                                            forgotStatusMessage = "Passwords do not match."
                                            return@Button
                                        }

                                        viewModel.resetPassword(forgotEmail, forgotNewPassword) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Password updated successfully! Please sign in.", Toast.LENGTH_SHORT).show()
                                                loginEmail = forgotEmail.trim()
                                                loginPassword = forgotNewPassword.trim()
                                                loginErrorMessage = null
                                                currentMode = AuthMode.LOGIN
                                            } else {
                                                forgotStatusMessage = "Email address not found. Please check your email."
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("reset_password_submit_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset Password", fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { currentMode = AuthMode.LOGIN },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Cancel & Return to Sign In")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
