package com.beinganie.postman.chat

import android.app.Application
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PostmanRepository(
    private val application: Application,
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val contentResolver = application.contentResolver
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var conversationsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        PostmanUiState(
            screen = Screen.WELCOME,
            isFirebaseConfigured = FirebaseApp.getApps(application).isNotEmpty(),
            isRealtimeActive = false,
            statusMessage = "Choose a username, then start chats by entering another person's username.",
        ),
    )

    val uiState: StateFlow<PostmanUiState> = _uiState.asStateFlow()

    suspend fun login(displayNameInput: String) {
        val normalizedDisplay = displayNameInput.trim().ifBlank { "aniket" }
        val username = normalizeUsername(normalizedDisplay)
        setLoading(true, "Connecting to realtime backend...")

        runCatching {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            val firebaseUser = requireNotNull(auth.currentUser)
            val currentUser = ChatUser(
                id = firebaseUser.uid,
                displayName = normalizedDisplay,
                username = username,
                isCurrentUser = true,
            )
            upsertUserProfile(currentUser)
            observeConversations(currentUser)
            _uiState.update { current ->
                current.copy(
                    currentUser = currentUser,
                    screen = Screen.CHAT_LIST,
                    isFirebaseConfigured = true,
                    isRealtimeActive = true,
                    isLoading = false,
                    statusMessage = "Signed in as @${currentUser.username}. Start a chat by entering another username.",
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    isRealtimeActive = false,
                    statusMessage = "Realtime setup failed: ${error.message ?: "Unknown error"}",
                )
            }
        }
    }

    suspend fun createConversation(peerUsernameInput: String) {
        val currentUser = _uiState.value.currentUser ?: return
        val peerUsername = normalizeUsername(peerUsernameInput)
        if (peerUsername.isBlank()) {
            setStatus("Enter a username to start a chat.")
            return
        }
        if (peerUsername == currentUser.username) {
            setStatus("Use another person's username to start a conversation.")
            return
        }

        setLoading(true, "Opening chat with @$peerUsername...")
        runCatching {
            val peerSnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", peerUsername)
                .limit(1)
                .get()
                .await()

            val peerDocument = peerSnapshot.documents.firstOrNull()
                ?: error("No user found with username @$peerUsername")

            val peerUser = ChatUser(
                id = peerDocument.id,
                displayName = peerDocument.getString("displayName") ?: peerUsername,
                username = peerDocument.getString("username") ?: peerUsername,
            )

            val conversationId = listOf(currentUser.id, peerUser.id).sorted().joinToString("_")
            val conversationData = hashMapOf(
                "participantIds" to listOf(currentUser.id, peerUser.id),
                "participantNames" to mapOf(
                    currentUser.id to currentUser.displayName,
                    peerUser.id to peerUser.displayName,
                ),
                "participantUsernames" to mapOf(
                    currentUser.id to currentUser.username,
                    peerUser.id to peerUser.username,
                ),
                "preview" to "Start chatting",
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedAtMillis" to System.currentTimeMillis(),
            )

            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .set(conversationData)
                .await()

            openConversation(conversationId)
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = "Realtime chat ready with @${peerUser.username}.",
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = error.message ?: "Could not open the conversation.",
                )
            }
        }
    }

    suspend fun openConversation(conversationId: String) {
        _uiState.update { current ->
            current.copy(selectedConversationId = conversationId, screen = Screen.CONVERSATION)
        }
        observeMessages(conversationId)
    }

    fun backToChatList() {
        messagesListener?.remove()
        messagesListener = null
        _uiState.update { current ->
            current.copy(screen = Screen.CHAT_LIST, selectedConversationId = null)
        }
    }

    suspend fun sendMessage(conversationId: String, text: String) {
        val currentUser = _uiState.value.currentUser ?: return
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return

        runCatching {
            val messageId = UUID.randomUUID().toString()
            val sentAtMillis = System.currentTimeMillis()
            val payload = hashMapOf(
                "id" to messageId,
                "senderId" to currentUser.id,
                "senderDisplayName" to currentUser.displayName,
                "senderUsername" to currentUser.username,
                "text" to normalizedText,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to sentAtMillis,
                "deliveryState" to DeliveryState.DELIVERED.name,
                "attachmentType" to null,
                "attachmentFileName" to null,
                "attachmentLocalPath" to null,
                "attachmentRemoteUrl" to null,
            )
            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(payload)
                .await()

            updateConversationPreview(conversationId, normalizedText)
        }.onFailure { error ->
            setStatus("Message send failed: ${error.message ?: "Unknown error"}")
        }
    }

    suspend fun sendAttachment(conversationId: String, type: AttachmentComposerType, uri: Uri?) {
        val currentUser = _uiState.value.currentUser ?: return
        if (uri == null) {
            setStatus("Attachment selection canceled.")
            return
        }

        setLoading(true, "Uploading ${type.name.lowercase()}...")
        runCatching {
            val fileName = resolveFileName(uri, type)
            val storageRef = storage.reference
                .child("chat_media")
                .child(conversationId)
                .child("${UUID.randomUUID()}_$fileName")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                storageRef.putStream(inputStream).await()
            } ?: error("Could not read the selected file.")

            val downloadUrl = storageRef.downloadUrl.await().toString()
            val messageId = UUID.randomUUID().toString()
            val sentAtMillis = System.currentTimeMillis()
            val payload = hashMapOf(
                "id" to messageId,
                "senderId" to currentUser.id,
                "senderDisplayName" to currentUser.displayName,
                "senderUsername" to currentUser.username,
                "text" to attachmentLabel(type, fileName),
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to sentAtMillis,
                "deliveryState" to DeliveryState.DELIVERED.name,
                "attachmentType" to type.name,
                "attachmentFileName" to fileName,
                "attachmentLocalPath" to uri.toString(),
                "attachmentRemoteUrl" to downloadUrl,
            )

            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(payload)
                .await()

            updateConversationPreview(conversationId, attachmentLabel(type, fileName))
            _uiState.update { current ->
                current.copy(isLoading = false, statusMessage = "Attachment shared in realtime.")
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = "Attachment upload failed: ${error.message ?: "Unknown error"}",
                )
            }
        }
    }

    fun clear() {
        conversationsListener?.remove()
        conversationsListener = null
        messagesListener?.remove()
        messagesListener = null
    }

    private suspend fun upsertUserProfile(user: ChatUser) {
        val data = hashMapOf(
            "displayName" to user.displayName,
            "username" to user.username,
            "lastSeenAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(data)
            .await()
    }

    private fun observeConversations(currentUser: ChatUser) {
        conversationsListener?.remove()
        conversationsListener = firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUser.id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    setStatus("Conversation sync failed: ${error.message ?: "Unknown error"}")
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents
                    ?.mapNotNull { document -> documentToConversation(document.id, document.data.orEmpty(), currentUser) }
                    ?.sortedByDescending { it.updatedAtMillis }
                    .orEmpty()

                _uiState.update { current ->
                    val selectedId = current.selectedConversationId
                    val currentMessages = current.conversations.firstOrNull { it.id == selectedId }?.messages.orEmpty()
                    current.copy(
                        conversations = conversations.map { incoming ->
                            if (incoming.id == selectedId && currentMessages.isNotEmpty()) {
                                incoming.copy(messages = currentMessages)
                            } else {
                                incoming
                            }
                        },
                        isRealtimeActive = true,
                    )
                }
            }
    }

    private fun observeMessages(conversationId: String) {
        val currentUser = _uiState.value.currentUser ?: return
        messagesListener?.remove()
        messagesListener = firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("createdAtMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    setStatus("Message sync failed: ${error.message ?: "Unknown error"}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents
                    ?.mapNotNull { document -> documentToMessage(document.data.orEmpty(), currentUser) }
                    .orEmpty()

                _uiState.update { current ->
                    current.copy(
                        conversations = current.conversations.map { conversation ->
                            if (conversation.id == conversationId) {
                                conversation.copy(messages = messages)
                            } else {
                                conversation
                            }
                        },
                    )
                }
            }
    }

    private suspend fun updateConversationPreview(conversationId: String, preview: String) {
        firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .update(
                mapOf(
                    "preview" to preview,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedAtMillis" to System.currentTimeMillis(),
                ),
            )
            .await()
    }

    private fun documentToConversation(
        id: String,
        data: Map<String, Any>,
        currentUser: ChatUser,
    ): ChatConversation? {
        val participantIds = data["participantIds"] as? List<*> ?: return null
        val participantNames = data["participantNames"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val participantUsernames = data["participantUsernames"] as? Map<*, *> ?: emptyMap<Any, Any>()

        val participants = participantIds.mapNotNull { rawId ->
            val userId = rawId as? String ?: return@mapNotNull null
            val username = participantUsernames[userId] as? String ?: userId
            val displayName = participantNames[userId] as? String ?: username
            ChatUser(
                id = userId,
                displayName = displayName,
                username = username,
                isCurrentUser = userId == currentUser.id,
            )
        }

        val otherParticipant = participants.firstOrNull { !it.isCurrentUser }
        val updatedAtMillis = (data["updatedAtMillis"] as? Number)?.toLong() ?: 0L

        return ChatConversation(
            id = id,
            title = otherParticipant?.displayName ?: "New chat",
            counterpartUsername = otherParticipant?.username ?: "",
            preview = data["preview"] as? String ?: "No messages yet",
            participants = participants,
            messages = emptyList(),
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun documentToMessage(
        data: Map<String, Any>,
        currentUser: ChatUser,
    ): ChatMessage? {
        val senderId = data["senderId"] as? String ?: return null
        val senderDisplayName = data["senderDisplayName"] as? String ?: "Unknown"
        val senderUsername = data["senderUsername"] as? String ?: senderDisplayName
        val sentAtMillis = (data["createdAtMillis"] as? Number)?.toLong()
            ?: (data["createdAt"] as? Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis()
        val attachment = attachmentFromDocument(data)

        return ChatMessage(
            id = data["id"] as? String ?: UUID.randomUUID().toString(),
            sender = ChatUser(
                id = senderId,
                displayName = senderDisplayName,
                username = senderUsername,
                isCurrentUser = senderId == currentUser.id,
            ),
            text = data["text"] as? String ?: "",
            sentAt = formatTime(sentAtMillis),
            sentAtMillis = sentAtMillis,
            deliveryState = deliveryStateFromString(data["deliveryState"] as? String),
            attachment = attachment,
        )
    }

    private fun attachmentFromDocument(data: Map<String, Any>): MessageAttachment? {
        val type = data["attachmentType"] as? String ?: return null
        val fileName = data["attachmentFileName"] as? String ?: "file"
        val localPath = data["attachmentLocalPath"] as? String ?: ""
        val remoteUrl = data["attachmentRemoteUrl"] as? String ?: ""
        return when (type) {
            AttachmentComposerType.IMAGE.name -> MessageAttachment.Image(fileName, localPath, remoteUrl)
            AttachmentComposerType.VIDEO.name -> MessageAttachment.Video(fileName, localPath, remoteUrl)
            AttachmentComposerType.DOCUMENT.name -> MessageAttachment.Document(fileName, localPath, remoteUrl)
            else -> null
        }
    }

    private fun deliveryStateFromString(value: String?): DeliveryState = when (value) {
        DeliveryState.READ.name -> DeliveryState.READ
        DeliveryState.SENDING.name -> DeliveryState.SENDING
        else -> DeliveryState.DELIVERED
    }

    private fun resolveFileName(uri: Uri, type: AttachmentComposerType): String {
        val lastSegment = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
        if (!lastSegment.isNullOrBlank()) {
            return lastSegment
        }
        return when (type) {
            AttachmentComposerType.IMAGE -> "image-${System.currentTimeMillis()}.jpg"
            AttachmentComposerType.VIDEO -> "video-${System.currentTimeMillis()}.mp4"
            AttachmentComposerType.DOCUMENT -> "document-${System.currentTimeMillis()}.pdf"
        }
    }

    private fun attachmentLabel(type: AttachmentComposerType, fileName: String): String = when (type) {
        AttachmentComposerType.IMAGE -> "Shared image: $fileName"
        AttachmentComposerType.VIDEO -> "Shared video: $fileName"
        AttachmentComposerType.DOCUMENT -> "Shared document: $fileName"
    }

    private fun normalizeUsername(input: String): String = input
        .trim()
        .lowercase(Locale.getDefault())
        .replace(" ", "")
        .replace(Regex("[^a-z0-9_.]"), "")

    private fun formatTime(timestamp: Long): String = timeFormatter.format(Date(timestamp))

    private fun setStatus(message: String) {
        _uiState.update { current -> current.copy(statusMessage = message) }
    }

    private fun setLoading(isLoading: Boolean, message: String) {
        _uiState.update { current -> current.copy(isLoading = isLoading, statusMessage = message) }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }
}
