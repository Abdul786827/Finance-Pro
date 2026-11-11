package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.FinanceIncomeBg
import com.example.ui.theme.FinanceIncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ImageUtils
import com.example.util.SecurityUtils

import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricSecurityManager

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val profileBitmap = remember(userProfile?.profileImageBase64) {
        userProfile?.profileImageBase64?.let { ImageUtils.base64ToBitmap(it) }
    }

    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profileBitmap != null) {
                                    Image(
                                        bitmap = profileBitmap.asImageBitmap(),
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile?.fullName ?: "Finance Pro User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (userProfile?.isEmailVerified == true) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = FinanceIncomeBg
                                    ) {
                                        Text(
                                            text = "VERIFIED",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FinanceIncomeGreen
                                        )
                                    }
                                }
                            }
                            Text(
                                text = userProfile?.email ?: "user@financepro.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userProfile?.mobile ?: "+92 300 1234567",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            }
        }

        // Security & Authentication
        item {
            Text(
                text = "Security & Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text("Password Protection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Change your login & encryption password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("change_password_button")
                        ) {
                            Text("Update", maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    val hasBiometricHardware = remember(context) {
                        BiometricSecurityManager.canAuthenticate(context)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fingerprint / Biometric Login", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (hasBiometricHardware) "Allow fingerprint sensor authentication on login screen" else "No enrolled fingerprint or hardware sensor detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = (userProfile?.isBiometricEnabled ?: false) && hasBiometricHardware,
                            enabled = hasBiometricHardware,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        BiometricSecurityManager.authenticate(
                                            activity = activity,
                                            title = "Confirm Fingerprint",
                                            subtitle = "Touch sensor to enable fingerprint login",
                                            onSucceeded = {
                                                viewModel.setBiometricEnabled(true)
                                                Toast.makeText(context, "Fingerprint Login enabled", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "FragmentActivity required for BiometricPrompt", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.setBiometricEnabled(false)
                                    Toast.makeText(context, "Fingerprint Login disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("biometric_settings_switch")
                        )
                    }
                }
            }
        }

        // Backup, Restore & Demo Reset
        item {
            Text(
                text = "Data Management & Backups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Save & Persistent Storage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("SQLite relational database active with atomic transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FinanceIncomeGreen)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset All Data to Zero", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Clears all transactions and resets all 7 accounts (HBL, UBL, Sadapay, Easypaisa, JazzCash, Mashreq, Wallet) to 0.00 balance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("reset_zero_button")
                        ) {
                            Text("Reset to 0")
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Session", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Sign out of your active account session and return to Login/Signup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Out")
                        }
                    }
                }
            }
        }

        // App Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Finance Pro v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Statement Management System based on Statement for app.xlsx.\nTimezone: Asia/Karachi (PKT, UTC+5).\nAccounts: HBL, UBL, Sadapay, Easypaisa, JazzCash, Mashreq, Wallet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Edit Profile Dialog with Avatar Photo & Camera Picker
    if (showEditProfileDialog) {
        userProfile?.let { prof ->
            var name by rememberSaveable { mutableStateOf(prof.fullName) }
            var email by rememberSaveable { mutableStateOf(prof.email) }
            var mobile by rememberSaveable { mutableStateOf(prof.mobile) }
            var currentImageBase64 by rememberSaveable { mutableStateOf(prof.profileImageBase64) }
            var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
            var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }

            val previewBitmap = remember(currentImageBase64) {
                currentImageBase64.takeIf { it.isNotBlank() }?.let { ImageUtils.base64ToBitmap(it) }
            }

            // Photo Gallery Picker (Android Photo Picker - PickVisualMedia)
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                viewModel.setExternalPickerActive(false)
                if (uri != null) {
                    val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 512)
                    if (base64 != null) {
                        currentImageBase64 = base64
                    } else {
                        Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Camera TakePicture contract with FileProvider URI
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success: Boolean ->
                viewModel.setExternalPickerActive(false)
                if (success && pendingCameraUri != null) {
                    val uri = Uri.parse(pendingCameraUri)
                    val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 512)
                    if (base64 != null) {
                        currentImageBase64 = base64
                    } else {
                        Toast.makeText(context, "Could not load captured photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Camera Permission Request
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    val tempUri = ImageUtils.createTempPictureUri(context)
                    if (tempUri != null) {
                        pendingCameraUri = tempUri.toString()
                        viewModel.setExternalPickerActive(true)
                        cameraLauncher.launch(tempUri)
                    } else {
                        Toast.makeText(context, "Could not initialize camera storage", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Camera permission is required to take a profile photo", Toast.LENGTH_SHORT).show()
                }
            }

            AlertDialog(
                onDismissRequest = {
                    showEditProfileDialog = false
                    showPhotoSourceSheet = false
                },
                title = { Text("Update User Profile") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Tappable Avatar with Camera/Edit Badge
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(90.dp)
                                .clickable {
                                    showPhotoSourceSheet = true
                                }
                                .testTag("profile_avatar_picker")
                        ) {
                            Surface(
                                modifier = Modifier.size(86.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (previewBitmap != null) {
                                        Image(
                                            bitmap = previewBitmap.asImageBitmap(),
                                            contentDescription = "Profile Picture Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            // Camera badge icon
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Photo Action Buttons (Choose / Camera / Remove)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setExternalPickerActive(true)
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }

                            if (currentImageBase64.isNotBlank()) {
                                TextButton(
                                    onClick = { currentImageBase64 = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Remove", fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                fullName = name.trim(),
                                email = SecurityUtils.normalizeEmail(email),
                                mobile = mobile.trim(),
                                profileImageBase64 = currentImageBase64
                            )
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
                }
            )

            // Choose Source Dialog if Avatar was tapped
            if (showPhotoSourceSheet) {
                AlertDialog(
                    onDismissRequest = { showPhotoSourceSheet = false },
                    title = { Text("Select Photo Source") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ListItem(
                                headlineContent = { Text("Choose from Gallery") },
                                leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldPrimaryLight) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPhotoSourceSheet = false
                                        viewModel.setExternalPickerActive(true)
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            )
                            ListItem(
                                headlineContent = { Text("Take New Photo with Camera") },
                                leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = EmeraldPrimaryLight) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPhotoSourceSheet = false
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                            )
                            if (currentImageBase64.isNotBlank()) {
                                ListItem(
                                    headlineContent = { Text("Remove Current Photo", color = MaterialTheme.colorScheme.error) },
                                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentImageBase64 = ""
                                            showPhotoSourceSheet = false
                                        }
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showPhotoSourceSheet = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }

    // Change Password Dialog with verification and SHA-256 hashing
    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var changePasswordError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    changePasswordError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = {
                            oldPassword = it
                            changePasswordError = null
                        },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            changePasswordError = null
                        },
                        label = { Text("New Password (Min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            changePasswordError = null
                        },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanOld = oldPassword.trim()
                        val cleanNew = newPassword.trim()
                        val cleanConfirm = confirmPassword.trim()

                        if (cleanOld.isEmpty()) {
                            changePasswordError = "Please enter your current password."
                            return@Button
                        }
                        if (cleanNew.length < 6) {
                            changePasswordError = "New password must be at least 6 characters."
                            return@Button
                        }
                        if (cleanNew != cleanConfirm) {
                            changePasswordError = "New passwords do not match."
                            return@Button
                        }

                        viewModel.changePassword(cleanOld, cleanNew) { success, message ->
                            if (success) {
                                showChangePasswordDialog = false
                                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                changePasswordError = message
                            }
                        }
                    }
                ) {
                    Text("Change Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            }
        )
    }


    // Reset All Data to Zero Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Data to Zero?") },
            text = {
                Text("This will wipe all existing transactions and reset all accounts (HBL, UBL, Sadapay, Easypaisa, JazzCash, Mashreq, Wallet) to 0.00 so you can input all your amounts from scratch.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllDataToZero()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "All data reset to zero", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset to 0")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
