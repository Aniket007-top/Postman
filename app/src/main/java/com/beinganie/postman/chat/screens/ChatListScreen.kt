package com.beinganie.postman.chat.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beinganie.postman.chat.ChatListTab
import com.beinganie.postman.chat.ChatConversation
import com.beinganie.postman.chat.FriendRequest
import com.beinganie.postman.chat.PostmanUiState
import com.beinganie.postman.chat.components.StatusBanner
import com.beinganie.postman.chat.components.UserAvatar

@Composable
fun ChatListScreen(
    modifier: Modifier,
    state: PostmanUiState,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onSelectChatListTab: (ChatListTab) -> Unit,
    onOpenProfile: () -> Unit,
) {
    var peerUsername by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Chats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "@${state.currentUser?.username ?: "guest"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = onOpenProfile,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceBright,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.currentUser?.displayName?.ifBlank { "Profile" } ?: "Profile",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        UserAvatar(
                            displayName = state.currentUser?.displayName ?: "P",
                            photoModel = state.currentUser?.photoUrl,
                            size = 34.dp,
                            showPresence = true,
                            isOnline = true,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = when {
                        state.isLoading -> "Syncing"
                        state.isRealtimeActive -> "Online"
                        state.isFirebaseConfigured -> "Ready"
                        else -> "Offline"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChatListTabs(
                            selectedTab = state.selectedChatListTab,
                            requestCount = state.friendRequests.size,
                            onSelectTab = onSelectChatListTab,
                        )
                        when (state.selectedChatListTab) {
                            ChatListTab.REQUESTS -> FriendRequestsPanel(
                                requests = state.friendRequests,
                                isLoading = state.isLoading,
                                onAcceptFriendRequest = onAcceptFriendRequest,
                            )

                            ChatListTab.NEW_CHAT -> {
                                Text(
                                    text = "New chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Send a friend request.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = peerUsername,
                                    onValueChange = { peerUsername = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text("Username") },
                                    prefix = { Text("@") },
                                )
                                Button(
                                    onClick = {
                                        onSendFriendRequest(peerUsername)
                                        peerUsername = ""
                                    },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Send request")
                                }
                            }
                        }
                    }
                }
            }
            state.statusMessage?.let { message ->
                if (message.shouldShowInChatList()) {
                    item {
                        StatusBanner(
                            message = message,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
            items(state.conversations) { conversation ->
                ChatListItem(conversation = conversation, onClick = { onOpenConversation(conversation.id) })
            }
        }
    }
}

@Composable
private fun ChatListTabs(
    selectedTab: ChatListTab,
    requestCount: Int,
    onSelectTab: (ChatListTab) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChatListTabButton(
                modifier = Modifier.weight(1f),
                title = "Friend requests",
                count = requestCount,
                selected = selectedTab == ChatListTab.REQUESTS,
                onClick = { onSelectTab(ChatListTab.REQUESTS) },
            )
            ChatListTabButton(
                modifier = Modifier.weight(1f),
                title = "New chat",
                count = null,
                selected = selectedTab == ChatListTab.NEW_CHAT,
                onClick = { onSelectTab(ChatListTab.NEW_CHAT) },
            )
        }
    }
}

@Composable
private fun ChatListTabButton(
    modifier: Modifier = Modifier,
    title: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (count != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRequestsPanel(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAcceptFriendRequest: (String) -> Unit,
) {
    if (requests.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No pending requests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Accept a request to start chatting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        requests.forEachIndexed { index, request ->
            FriendRequestRow(
                request = request,
                isLoading = isLoading,
                onAcceptFriendRequest = onAcceptFriendRequest,
            )
            if (index != requests.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun FriendRequestRow(
    request: FriendRequest,
    isLoading: Boolean,
    onAcceptFriendRequest: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            displayName = request.sender.displayName,
            photoModel = request.sender.photoUrl,
            size = 46.dp,
            showPresence = false,
            isOnline = false,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.sender.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "@${request.sender.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = { onAcceptFriendRequest(request.id) },
            enabled = !isLoading,
        ) {
            Text("Accept")
        }
    }
}

private fun String.shouldShowInChatList(): Boolean = listOf(
    "failed",
    "could not",
    "not found",
    "already taken",
    "logged out",
    "request",
    "friends",
).any { keyword -> contains(keyword, ignoreCase = true) }

@Composable
private fun ChatListItem(
    conversation: ChatConversation,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                displayName = conversation.title,
                photoModel = null,
                size = 52.dp,
                showPresence = true,
                isOnline = conversation.participants.firstOrNull { !it.isCurrentUser }?.isOnline == true,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${conversation.counterpartUsername}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    conversation.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${conversation.participants.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
