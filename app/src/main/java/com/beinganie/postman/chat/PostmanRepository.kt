package com.beinganie.postman.chat

import android.app.Application
import android.net.Uri
import com.beinganie.postman.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PostmanRepository(
    private val application: Application,
) {
    private val firebaseApp = FirebaseApp.initializeApp(application) ?: FirebaseApp.getApps(application).firstOrNull()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val contentResolver = application.contentResolver
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var conversationsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        PostmanUiState(
            screen = Screen.WELCOME,
            isFirebaseConfigured = firebaseApp != null,
            isRealtimeActive = false,
            statusMessage = null,
        ),
    )

    val uiState: StateFlow<PostmanUiState> = _uiState.asStateFlow()

    suspend fun login(emailInput: String, passwordInput: String) {
        val email = emailInput.trim()
        val password = passwordInput.trim()
        if (email.isBlank() || password.isBlank()) {
            setStatus("Enter email and password.")
            return
        }

        setLoading(true, "Signing in...")

        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = requireNotNull(auth.currentUser)
            val currentUser = loadCurrentUserProfile(firebaseUser.uid)
            upsertUserProfile(currentUser)
            syncFcmToken(firebaseUser.uid)
            syncCurrentUserPresence(currentUser)
            observeConversations(currentUser)
            _uiState.update { current ->
                current.copy(
                    currentUser = currentUser,
                    screen = Screen.CHAT_LIST,
                    isFirebaseConfigured = true,
                    isRealtimeActive = true,
                    isLoading = false,
                    statusMessage = null,
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

    suspend fun register(
        displayNameInput: String,
        usernameInput: String,
        emailInput: String,
        passwordInput: String,
    ) {
        val displayName = displayNameInput.trim().ifBlank { "User" }
        val username = normalizeUsername(usernameInput)
        val email = emailInput.trim()
        val password = passwordInput.trim()

        when {
            username.isBlank() -> {
                setStatus("Choose a username.")
                return
            }

            email.isBlank() || password.isBlank() -> {
                setStatus("Enter email and password.")
                return
            }

            password.length < 6 -> {
                setStatus("Password must be at least 6 characters.")
                return
            }
        }

        setLoading(true, "Creating account...")
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = requireNotNull(auth.currentUser)
            val existingUser = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
            if (existingUser != null && existingUser.id != firebaseUser.uid) {
                firebaseUser.delete().await()
                error("Username @$username is already taken.")
            }
            val currentUser = ChatUser(
                id = firebaseUser.uid,
                displayName = displayName,
                username = username,
                photoUrl = null,
                isOnline = true,
                isCurrentUser = true,
            )
            upsertUserProfile(currentUser, email = email)
            syncFcmToken(firebaseUser.uid)
            syncCurrentUserPresence(currentUser)
            observeConversations(currentUser)
            _uiState.update { current ->
                current.copy(
                    currentUser = currentUser,
                    screen = Screen.CHAT_LIST,
                    isFirebaseConfigured = true,
                    isRealtimeActive = true,
                    isLoading = false,
                    statusMessage = null,
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    isRealtimeActive = false,
                    statusMessage = error.message ?: "Could not create account.",
                )
            }
        }
    }

    suspend fun resetPassword(emailInput: String) {
        val email = emailInput.trim()
        if (email.isBlank()) {
            setStatus("Enter your email.")
            return
        }

        setLoading(true, "Sending reset email...")
        runCatching {
            auth.sendPasswordResetEmail(email).await()
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = "Reset link sent to $email.",
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = error.message ?: "Could not send reset email.",
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
                photoUrl = peerDocument.getString("photoUrl"),
                isOnline = peerDocument.getBoolean("isOnline") == true,
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
                "participantOnline" to mapOf(
                    currentUser.id to true,
                    peerUser.id to peerUser.isOnline,
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
                    statusMessage = null,
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

    fun openProfile() {
        _uiState.update { current ->
            current.copy(screen = Screen.PROFILE)
        }
    }

    fun backToChatList() {
        messagesListener?.remove()
        messagesListener = null
        _uiState.update { current ->
            current.copy(screen = Screen.CHAT_LIST, selectedConversationId = null)
        }
    }

    fun logout() {
        markCurrentUserOffline()
        conversationsListener?.remove()
        conversationsListener = null
        messagesListener?.remove()
        messagesListener = null
        auth.signOut()
        _uiState.value = PostmanUiState(
            screen = Screen.WELCOME,
            isFirebaseConfigured = firebaseApp != null,
            isRealtimeActive = false,
            statusMessage = "You have been logged out.",
        )
    }

    suspend fun updateProfile(displayNameInput: String, usernameInput: String, photoUri: Uri?) {
        val currentUser = _uiState.value.currentUser ?: return
        val normalizedDisplay = displayNameInput.trim().ifBlank { currentUser.displayName }
        val normalizedUsername = normalizeUsername(usernameInput).ifBlank { currentUser.username }

        setLoading(true, "Saving profile...")
        runCatching {
            val existingUser = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", normalizedUsername)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (existingUser != null && existingUser.id != currentUser.id) {
                error("Username @$normalizedUsername is already taken.")
            }

            val photoUrl = if (photoUri != null) {
                uploadToCloudinary(
                    uri = photoUri,
                    folder = "postman/profile_photos/${currentUser.id}",
                    type = AttachmentComposerType.IMAGE,
                ).secureUrl
            } else {
                currentUser.photoUrl
            }

            val updatedUser = currentUser.copy(
                displayName = normalizedDisplay,
                username = normalizedUsername,
                photoUrl = photoUrl,
                isOnline = currentUser.isOnline,
            )

            upsertUserProfile(updatedUser)
            syncCurrentUserAcrossConversations(updatedUser)

            _uiState.update { current ->
                current.copy(
                    currentUser = updatedUser,
                    conversations = current.conversations.map { conversation ->
                        conversation.copy(
                            participants = conversation.participants.map { participant ->
                                if (participant.id == updatedUser.id) updatedUser else participant
                            },
                        )
                    },
                    screen = Screen.CHAT_LIST,
                    isLoading = false,
                    statusMessage = "Profile saved.",
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = uploadErrorMessage("profile update", error),
                )
            }
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
            val uploadResult = uploadToCloudinary(
                uri = uri,
                folder = "postman/chat_media/$conversationId",
                type = type,
            )
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
                "attachmentLocalPath" to "",
                "attachmentRemoteUrl" to uploadResult.secureUrl,
            )

            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(payload)
                .await()

            updateConversationPreview(conversationId, attachmentLabel(type, fileName))
            _uiState.update { current ->
                current.copy(isLoading = false, statusMessage = null)
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = uploadErrorMessage("${type.name.lowercase()} upload", error),
                )
            }
        }
    }

    suspend fun downloadAttachment(conversationId: String, messageId: String) {
        val currentUser = _uiState.value.currentUser ?: return
        val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
        val message = conversation.messages.firstOrNull { it.id == messageId } ?: return
        val attachment = message.attachment ?: return

        val localFile = localAttachmentFile(messageId, attachment)
        if (localFile.exists()) {
            setStatus("${attachmentFileLabel(attachment)} already saved on this device.")
            return
        }

        setLoading(true, "Downloading ${attachmentFileLabel(attachment).lowercase()}...")
        runCatching {
            downloadRemoteFile(attachmentRemoteUrl(attachment), localFile)

            _uiState.update { current ->
                current.copy(
                    conversations = current.conversations.map { currentConversation ->
                        if (currentConversation.id != conversationId) {
                            currentConversation
                        } else {
                            currentConversation.copy(
                                messages = currentConversation.messages.map { currentMessage ->
                                    if (currentMessage.id == messageId) {
                                        currentMessage.copy(
                                            attachment = currentMessage.attachment?.withLocalPath(localFile.toURI().toString()),
                                        )
                                    } else {
                                        currentMessage
                                    }
                                },
                            )
                        }
                    },
                    isLoading = false,
                    statusMessage = "${attachmentFileLabel(attachment)} saved to your device.",
                )
            }
        }.getOrElse { error ->
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    statusMessage = "Download failed: ${error.message ?: "Unknown error"}",
                )
            }
        }
    }

    fun clear() {
        markCurrentUserOffline()
        conversationsListener?.remove()
        conversationsListener = null
        messagesListener?.remove()
        messagesListener = null
    }

    private suspend fun upsertUserProfile(user: ChatUser, email: String? = auth.currentUser?.email) {
        val data = hashMapOf(
            "displayName" to user.displayName,
            "username" to user.username,
            "photoUrl" to user.photoUrl,
            "isOnline" to user.isOnline,
            "email" to email,
            "lastSeenAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(data)
            .await()
    }

    private suspend fun loadCurrentUserProfile(userId: String): ChatUser {
        val profile = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        if (!profile.exists()) {
            error("Account profile not found.")
        }

        val username = profile.getString("username").orEmpty()
        val displayName = profile.getString("displayName").orEmpty().ifBlank { username.ifBlank { "User" } }
        return ChatUser(
            id = userId,
            displayName = displayName,
            username = username,
            photoUrl = profile.getString("photoUrl"),
            isOnline = true,
            isCurrentUser = true,
        )
    }

    private suspend fun syncFcmToken(userId: String) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    private suspend fun syncCurrentUserAcrossConversations(user: ChatUser) {
        val snapshot = firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", user.id)
            .get()
            .await()

        snapshot.documents.forEach { document ->
            document.reference.update(
                mapOf(
                    "participantNames.${user.id}" to user.displayName,
                    "participantUsernames.${user.id}" to user.username,
                    "participantOnline.${user.id}" to user.isOnline,
                ),
            ).await()
        }
    }

    private suspend fun syncCurrentUserPresence(user: ChatUser) {
        syncCurrentUserAcrossConversations(user)
    }

    private fun markCurrentUserOffline() {
        val currentUser = _uiState.value.currentUser ?: return
        firestore.collection(USERS_COLLECTION)
            .document(currentUser.id)
            .update(
                mapOf(
                    "isOnline" to false,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                ),
            )
        firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUser.id)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { document ->
                    document.reference.update("participantOnline.${currentUser.id}", false)
                }
            }
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
                    ?.mapNotNull { document ->
                        documentToMessage(
                            data = document.data.orEmpty(),
                            currentUser = currentUser,
                        )
                    }
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
        val participantOnline = data["participantOnline"] as? Map<*, *> ?: emptyMap<Any, Any>()

        val participants = participantIds.mapNotNull { rawId ->
            val userId = rawId as? String ?: return@mapNotNull null
            val username = participantUsernames[userId] as? String ?: userId
            val displayName = participantNames[userId] as? String ?: username
            ChatUser(
                id = userId,
                displayName = displayName,
                username = username,
                photoUrl = null,
                isOnline = participantOnline[userId] as? Boolean == true,
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
                photoUrl = null,
                isOnline = false,
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
        val messageId = data["id"] as? String ?: return null
        val remoteUrl = data["attachmentRemoteUrl"] as? String ?: ""
        val localFile = localAttachmentFile(messageId, fileName)
        val localPath = if (localFile.exists()) localFile.toURI().toString() else ""
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

    private suspend fun uploadToCloudinary(
        uri: Uri,
        folder: String,
        type: AttachmentComposerType,
    ): CloudinaryUploadResult = withContext(Dispatchers.IO) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            error("Cloudinary is not configured yet. Add CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET in gradle.properties.")
        }

        val boundary = "Boundary-${UUID.randomUUID()}"
        val fileName = resolveFileName(uri, type)
        val mimeType = contentResolver.getType(uri) ?: fallbackMimeType(type)
        val endpoint = URL("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        connection.outputStream.use { stream ->
            DataOutputStream(stream).use { output ->
                writeFormField(output, boundary, "upload_preset", uploadPreset)
                writeFormField(output, boundary, "folder", folder)
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                output.writeBytes("Content-Type: $mimeType\r\n\r\n")
                contentResolver.openInputStream(uri)?.use { input ->
                    input.copyTo(output)
                } ?: error("Could not read the selected file.")
                output.writeBytes("\r\n")
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }
        }

        val responseCode = connection.responseCode
        val responseText = readHttpResponse(connection, responseCode in 200..299)
        if (responseCode !in 200..299) {
            error("Cloudinary upload failed: $responseText")
        }

        val json = JSONObject(responseText)
        val secureUrl = json.optString("secure_url").takeIf { it.isNotBlank() }
            ?: error("Cloudinary did not return a secure URL.")
        CloudinaryUploadResult(
            secureUrl = secureUrl,
        )
    }

    private fun writeFormField(output: DataOutputStream, boundary: String, name: String, value: String) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.writeBytes("$value\r\n")
    }

    private fun readHttpResponse(connection: HttpURLConnection, success: Boolean): String {
        val stream = if (success) connection.inputStream else connection.errorStream ?: connection.inputStream
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.readText()
        }
    }

    private fun fallbackMimeType(type: AttachmentComposerType): String = when (type) {
        AttachmentComposerType.IMAGE -> "image/jpeg"
        AttachmentComposerType.VIDEO -> "video/mp4"
        AttachmentComposerType.DOCUMENT -> "application/pdf"
    }

    private suspend fun downloadRemoteFile(remoteUrl: String, outputFile: File) = withContext(Dispatchers.IO) {
        if (remoteUrl.isBlank()) error("This attachment no longer has a valid download link.")
        outputFile.parentFile?.mkdirs()
        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
        }
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            error("Remote file download failed with HTTP $responseCode.")
        }
        connection.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun localAttachmentFile(messageId: String, attachment: MessageAttachment): File =
        localAttachmentFile(messageId, attachmentFileName(attachment))

    private fun localAttachmentFile(messageId: String, fileName: String): File {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val baseDir = application.getExternalFilesDir(null) ?: application.filesDir
        val downloadDir = File(baseDir, "downloads").apply { mkdirs() }
        return File(downloadDir, "${messageId}_$safeName")
    }

    private fun attachmentFileName(attachment: MessageAttachment): String = when (attachment) {
        is MessageAttachment.Image -> attachment.fileName
        is MessageAttachment.Video -> attachment.fileName
        is MessageAttachment.Document -> attachment.fileName
    }

    private fun attachmentFileLabel(attachment: MessageAttachment): String = when (attachment) {
        is MessageAttachment.Image -> "Photo"
        is MessageAttachment.Video -> "Video"
        is MessageAttachment.Document -> "Document"
    }

    private fun attachmentRemoteUrl(attachment: MessageAttachment): String = when (attachment) {
        is MessageAttachment.Image -> attachment.remoteUrl
        is MessageAttachment.Video -> attachment.remoteUrl
        is MessageAttachment.Document -> attachment.remoteUrl
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

    private fun uploadErrorMessage(action: String, error: Throwable): String {
        val message = error.message.orEmpty()
        return if ("Cloudinary is not configured yet" in message) {
            message
        } else if ("Cloudinary upload failed" in message) {
            "$action failed: $message"
        } else {
            "${action.replaceFirstChar { it.uppercase() }} failed: ${error.message ?: "Unknown error"}"
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }
}

private data class CloudinaryUploadResult(
    val secureUrl: String,
)

private fun MessageAttachment.withLocalPath(localPath: String): MessageAttachment = when (this) {
    is MessageAttachment.Image -> copy(localPath = localPath)
    is MessageAttachment.Video -> copy(localPath = localPath)
    is MessageAttachment.Document -> copy(localPath = localPath)
}
