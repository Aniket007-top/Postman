package com.beinganie.postman.chat.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beinganie.postman.chat.PostmanUiState
import com.beinganie.postman.chat.components.ConversationHeader
import com.beinganie.postman.chat.components.StatusBanner
import com.beinganie.postman.chat.components.UserAvatar

@Composable
fun ProfileScreen(
    modifier: Modifier,
    state: PostmanUiState,
    onBackToChats: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String, Uri?) -> Unit,
) {
    val currentUser = state.currentUser ?: return
    var displayName by remember(currentUser.displayName) { mutableStateOf(currentUser.displayName) }
    var username by remember(currentUser.username) { mutableStateOf(currentUser.username) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        selectedPhotoUri = uri
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        ConversationHeader(
            title = "Settings",
            participantCount = 1,
            onBack = onBackToChats,
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserAvatar(
                        displayName = displayName,
                        photoModel = selectedPhotoUri ?: currentUser.photoUrl,
                        size = 104.dp,
                        showPresence = true,
                        isOnline = currentUser.isOnline,
                    )
                    Button(onClick = { imagePicker.launch("image/*") }) {
                        Text("Change photo")
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true,
                )

                Text(
                    text = "Update your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { onUpdateProfile(displayName, username, selectedPhotoUri) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text(if (state.isLoading) "Saving..." else "Save profile")
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        state.statusMessage?.let { message ->
            StatusBanner(
                message = message,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}
