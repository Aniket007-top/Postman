package com.beinganie.postman.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.beinganie.postman.chat.screens.ChatListScreen
import com.beinganie.postman.chat.screens.ConversationScreen
import com.beinganie.postman.chat.screens.WelcomeScreen

@Composable
fun PostmanApp(
    state: PostmanUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onSelectChatListTab: (ChatListTab) -> Unit,
    onOpenProfile: () -> Unit,
    onBackToChats: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String, Uri?) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onSendAttachment: (String, AttachmentComposerType, Uri?) -> Unit,
    onDownloadAttachment: (String, String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .padding(innerPadding)
            .safeDrawingPadding()

        when (state.screen) {
            Screen.WELCOME -> WelcomeScreen(
                modifier = screenModifier,
                isFirebaseConfigured = state.isFirebaseConfigured,
                isLoading = state.isLoading,
                statusMessage = state.statusMessage,
                onLogin = onLogin,
                onRegister = onRegister,
                onResetPassword = onResetPassword,
            )

            Screen.CHAT_LIST -> ChatListScreen(
                modifier = screenModifier,
                state = state,
                onSendFriendRequest = onSendFriendRequest,
                onAcceptFriendRequest = onAcceptFriendRequest,
                onOpenConversation = onOpenConversation,
                onSelectChatListTab = onSelectChatListTab,
                onOpenProfile = onOpenProfile,
            )

            Screen.CONVERSATION -> ConversationScreen(
                modifier = screenModifier,
                state = state,
                onBackToChats = onBackToChats,
                onSendMessage = onSendMessage,
                onSendAttachment = onSendAttachment,
                onDownloadAttachment = onDownloadAttachment,
            )

            Screen.PROFILE -> com.beinganie.postman.chat.screens.ProfileScreen(
                modifier = screenModifier,
                state = state,
                onBackToChats = onBackToChats,
                onLogout = onLogout,
                onUpdateProfile = onUpdateProfile,
            )
        }
    }
}
