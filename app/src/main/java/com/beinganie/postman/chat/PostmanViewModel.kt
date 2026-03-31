package com.beinganie.postman.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostmanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PostmanRepository(application)
    val uiState: StateFlow<PostmanUiState> = repository.uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            repository.login(email, password)
        }
    }

    fun register(displayName: String, username: String, email: String, password: String) {
        viewModelScope.launch {
            repository.register(displayName, username, email, password)
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            repository.resetPassword(email)
        }
    }

    fun createConversation(peerUsername: String) {
        viewModelScope.launch {
            repository.createConversation(peerUsername)
        }
    }

    fun openConversation(conversationId: String) {
        viewModelScope.launch {
            repository.openConversation(conversationId)
        }
    }

    fun openProfile() {
        repository.openProfile()
    }

    fun backToChatList() {
        repository.backToChatList()
    }

    fun logout() {
        repository.logout()
    }

    fun updateProfile(displayName: String, username: String, photoUri: Uri?) {
        viewModelScope.launch {
            repository.updateProfile(displayName, username, photoUri)
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, text)
        }
    }

    fun sendAttachment(conversationId: String, type: AttachmentComposerType, uri: Uri? = null) {
        viewModelScope.launch {
            repository.sendAttachment(conversationId, type, uri)
        }
    }

    fun downloadAttachment(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.downloadAttachment(conversationId, messageId)
        }
    }

    override fun onCleared() {
        repository.clear()
        super.onCleared()
    }
}
