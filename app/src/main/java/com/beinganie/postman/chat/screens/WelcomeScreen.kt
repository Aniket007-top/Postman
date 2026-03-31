package com.beinganie.postman.chat.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beinganie.postman.chat.components.StatusBanner

@Composable
fun WelcomeScreen(
    modifier: Modifier,
    isFirebaseConfigured: Boolean,
    isLoading: Boolean,
    onLogin: (String) -> Unit,
) {
    var displayName by remember { mutableStateOf("Aniket") }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Postman", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "This build uses Firebase for realtime private chats. Choose a username that other people can search for.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            label = { Text("Username") },
            prefix = { Text("@") },
            supportingText = { Text("Use letters and numbers only for the most reliable lookup.") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onLogin(displayName.trim()) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = isFirebaseConfigured && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Continue")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FirebaseModeNotice(isFirebaseConfigured = isFirebaseConfigured)
        Spacer(modifier = Modifier.height(12.dp))
        StatusBanner(
            message = if (isFirebaseConfigured) {
                "Realtime backend detected. Messages and attachments can sync across devices."
            } else {
                "Firebase config is missing, so cross-device sync will not work."
            },
        )
    }
}

@Composable
private fun FirebaseModeNotice(isFirebaseConfigured: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isFirebaseConfigured) "Realtime mode" else "Backend missing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFirebaseConfigured) {
                    "User profiles, conversation metadata, live messages, and media uploads are routed through Firebase Auth, Firestore, and Storage."
                } else {
                    "Add the Firebase app config to enable user discovery, per-user chatboxes, live messaging, and media uploads."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
