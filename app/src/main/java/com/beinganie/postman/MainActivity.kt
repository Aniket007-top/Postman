package com.beinganie.postman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.beinganie.postman.chat.PostmanApp
import com.beinganie.postman.chat.PostmanViewModel
import com.beinganie.postman.ui.theme.PostmanTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PostmanViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PostmanTheme {
                val state by viewModel.uiState.collectAsState()
                PostmanApp(
                    state = state,
                    onLogin = viewModel::login,
                    onCreateConversation = viewModel::createConversation,
                    onOpenConversation = viewModel::openConversation,
                    onOpenProfile = viewModel::openProfile,
                    onBackToChats = viewModel::backToChatList,
                    onUpdateProfile = viewModel::updateProfile,
                    onSendMessage = viewModel::sendMessage,
                    onSendAttachment = viewModel::sendAttachment,
                )
            }
        }
    }
}
