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
    onLogin: (String) -> Unit,
    onCreateConversation: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onBackToChats: () -> Unit,
    onSendMessage: (String, String) -> Unit,
    onSendAttachment: (String, AttachmentComposerType, Uri?) -> Unit,
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
                onLogin = onLogin,
            )

            Screen.CHAT_LIST -> ChatListScreen(
                modifier = screenModifier,
                state = state,
                onCreateConversation = onCreateConversation,
                onOpenConversation = onOpenConversation,
            )

            Screen.CONVERSATION -> ConversationScreen(
                modifier = screenModifier,
                state = state,
                onBackToChats = onBackToChats,
                onSendMessage = onSendMessage,
                onSendAttachment = onSendAttachment,
            )
        }
    }
}
