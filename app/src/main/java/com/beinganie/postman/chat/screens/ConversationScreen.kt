package com.beinganie.postman.chat.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beinganie.postman.chat.AttachmentComposerType
import com.beinganie.postman.chat.ChatConversation
import com.beinganie.postman.chat.ChatMessage
import com.beinganie.postman.chat.ChatUser
import com.beinganie.postman.chat.DeliveryState
import com.beinganie.postman.chat.PostmanUiState
import com.beinganie.postman.chat.Screen
import com.beinganie.postman.chat.components.Composer
import com.beinganie.postman.chat.components.ConversationHeader
import com.beinganie.postman.chat.components.MessageBubble
import com.beinganie.postman.chat.components.StatusBanner
import com.beinganie.postman.ui.theme.PostmanTheme

@Composable
fun ConversationScreen(
    modifier: Modifier,
    state: PostmanUiState,
    onBackToChats: () -> Unit,
    onSendMessage: (String, String) -> Unit,
    onSendAttachment: (String, AttachmentComposerType, Uri?) -> Unit,
    onDownloadAttachment: (String, String) -> Unit,
) {
    val conversation = state.conversations.firstOrNull { it.id == state.selectedConversationId } ?: return
    var draft by remember { mutableStateOf("") }
    var currentAttachmentType by remember { mutableStateOf<AttachmentComposerType?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        currentAttachmentType?.let { type ->
            if (uri != null) {
                onSendAttachment(conversation.id, type, uri)
            }
        }
        currentAttachmentType = null
    }

    Column(modifier = modifier) {
        ConversationHeader(
            title = conversation.title,
            participantCount = conversation.participants.size,
            isOtherUserOnline = conversation.participants.firstOrNull { !it.isCurrentUser }?.isOnline == true,
            onBack = onBackToChats,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.statusMessage?.let { message ->
                if (message.shouldShowInConversation()) {
                    item {
                        StatusBanner(
                            message = message,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
            items(conversation.messages) { message ->
                MessageBubble(
                    message = message,
                    onAttachmentClick = {
                        onDownloadAttachment(conversation.id, message.id)
                    },
                )
            }
        }
        Composer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val content = draft.trim()
                if (content.isNotEmpty()) {
                    onSendMessage(conversation.id, content)
                    draft = ""
                }
            },
            onSendAttachment = { type ->
                currentAttachmentType = type
                val mimeType = when (type) {
                    AttachmentComposerType.IMAGE -> "image/*"
                    AttachmentComposerType.VIDEO -> "video/*"
                    AttachmentComposerType.DOCUMENT -> "application/pdf"
                }
                pickerLauncher.launch(mimeType)
            },
        )
    }
}

private fun String.shouldShowInConversation(): Boolean = listOf(
    "failed",
    "canceled",
    "could not",
    "already taken",
).any { keyword -> contains(keyword, ignoreCase = true) }

@Preview(showBackground = true)
@Composable
private fun ConversationScreenPreview() {
    val me = ChatUser(id = "me", displayName = "Aniket", username = "aniket", isCurrentUser = true)
    val friend = ChatUser(id = "ravi", displayName = "Ravi", username = "ravi")
    val conversation = ChatConversation(
        id = "demo",
        title = "Ravi",
        counterpartUsername = "ravi",
        preview = "See you at 6",
        participants = listOf(me, friend),
        messages = listOf(
            ChatMessage(
                id = "m1",
                sender = friend,
                text = "Send the document too.",
                sentAt = "09:10",
                sentAtMillis = 1L,
                deliveryState = DeliveryState.READ,
            ),
            ChatMessage(
                id = "m2",
                sender = me,
                text = "Uploading it now.",
                sentAt = "09:12",
                sentAtMillis = 2L,
                deliveryState = DeliveryState.DELIVERED,
            ),
        ),
        updatedAtMillis = 2L,
    )

    PostmanTheme {
        ConversationScreen(
            modifier = Modifier,
            state = PostmanUiState(
                screen = Screen.CONVERSATION,
                currentUser = me,
                conversations = listOf(conversation),
                selectedConversationId = "demo",
                statusMessage = "Preview mode",
            ),
            onBackToChats = {},
            onSendMessage = { _, _ -> },
            onSendAttachment = { _, _, _ -> },
            onDownloadAttachment = { _, _ -> },
        )
    }
}
