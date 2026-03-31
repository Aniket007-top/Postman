package com.beinganie.postman.chat.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beinganie.postman.chat.components.StatusBanner

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun WelcomeScreen(
    modifier: Modifier,
    isFirebaseConfigured: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onResetPassword: (String) -> Unit,
) {
    var authMode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Postman", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (authMode == AuthMode.SIGN_IN) "Welcome back." else "Create account.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        AuthModeSwitcher(
            authMode = authMode,
            onChange = { authMode = it },
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (authMode == AuthMode.SIGN_UP) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                label = { Text("Name") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                label = { Text("Username") },
                prefix = { Text("@") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            label = { Text("Email") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            label = { Text("Password") },
            supportingText = {
                Text(if (authMode == AuthMode.SIGN_UP) "At least 6 characters." else "Enter your password.")
            },
            singleLine = true,
        )
        if (authMode == AuthMode.SIGN_IN) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = { onResetPassword(email) },
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Text(
                    text = "Forgot password?",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (authMode == AuthMode.SIGN_IN) {
                    onLogin(email, password)
                } else {
                    onRegister(displayName, username, email, password)
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = isFirebaseConfigured && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (authMode == AuthMode.SIGN_IN) "Sign in" else "Sign up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FirebaseModeNotice(isFirebaseConfigured = isFirebaseConfigured)
        statusMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            StatusBanner(message = message)
        }
    }
}

@Composable
private fun AuthModeSwitcher(
    authMode: AuthMode,
    onChange: (AuthMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(AuthMode.SIGN_IN to "Sign in", AuthMode.SIGN_UP to "Sign up").forEach { (mode, label) ->
            Surface(
                onClick = { onChange(mode) },
                shape = RoundedCornerShape(999.dp),
                color = if (authMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceBright,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (authMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun FirebaseModeNotice(isFirebaseConfigured: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isFirebaseConfigured) "Secure login" else "Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFirebaseConfigured) {
                    "Email account required."
                } else {
                    "Setup needed."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
