package com.beinganie.postman.chat

data class ChatUser(
    val id: String,
    val displayName: String,
    val username: String,
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val isCurrentUser: Boolean = false,
)

enum class DeliveryState {
    SENDING,
    DELIVERED,
    READ,
}

enum class AttachmentComposerType {
    IMAGE,
    VIDEO,
    DOCUMENT,
}

sealed interface MessageAttachment {
    data class Image(
        val fileName: String,
        val localPath: String,
        val remoteUrl: String,
    ) : MessageAttachment

    data class Video(
        val fileName: String,
        val localPath: String,
        val remoteUrl: String,
    ) : MessageAttachment

    data class Document(
        val fileName: String,
        val localPath: String,
        val remoteUrl: String,
    ) : MessageAttachment
}

data class ChatMessage(
    val id: String,
    val sender: ChatUser,
    val text: String,
    val sentAt: String,
    val sentAtMillis: Long,
    val deliveryState: DeliveryState,
    val attachment: MessageAttachment? = null,
)

data class ChatConversation(
    val id: String,
    val title: String,
    val counterpartUsername: String,
    val preview: String,
    val participants: List<ChatUser>,
    val messages: List<ChatMessage>,
    val updatedAtMillis: Long,
)

enum class Screen {
    WELCOME,
    CHAT_LIST,
    CONVERSATION,
    PROFILE,
}

data class PostmanUiState(
    val currentUser: ChatUser? = null,
    val screen: Screen = Screen.WELCOME,
    val conversations: List<ChatConversation> = emptyList(),
    val selectedConversationId: String? = null,
    val isFirebaseConfigured: Boolean = false,
    val isRealtimeActive: Boolean = false,
    val statusMessage: String? = null,
    val isLoading: Boolean = false,
)
