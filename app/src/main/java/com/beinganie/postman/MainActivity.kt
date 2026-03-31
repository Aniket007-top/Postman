package com.beinganie.postman

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.beinganie.postman.chat.ensureChatNotificationChannel
import com.beinganie.postman.chat.PostmanApp
import com.beinganie.postman.chat.PostmanViewModel
import com.beinganie.postman.ui.theme.PostmanTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PostmanViewModel>()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureChatNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PostmanTheme {
                val state by viewModel.uiState.collectAsState()
                PostmanApp(
                    state = state,
                    onLogin = viewModel::login,
                    onRegister = viewModel::register,
                    onResetPassword = viewModel::resetPassword,
                    onSendFriendRequest = viewModel::sendFriendRequest,
                    onAcceptFriendRequest = viewModel::acceptFriendRequest,
                    onOpenConversation = viewModel::openConversation,
                    onSelectChatListTab = viewModel::selectChatListTab,
                    onOpenProfile = viewModel::openProfile,
                    onBackToChats = viewModel::backToChatList,
                    onLogout = viewModel::logout,
                    onUpdateProfile = viewModel::updateProfile,
                    onSendMessage = viewModel::sendMessage,
                    onSendAttachment = viewModel::sendAttachment,
                    onDownloadAttachment = viewModel::downloadAttachment,
                )
            }
        }
    }
}
