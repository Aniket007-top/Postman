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

    fun login(displayName: String) {
        viewModelScope.launch {
            repository.login(displayName)
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

    fun backToChatList() {
        repository.backToChatList()
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

    override fun onCleared() {
        repository.clear()
        super.onCleared()
    }
}
