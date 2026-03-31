package com.beinganie.postman.chat.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beinganie.postman.ui.theme.Mint
import com.beinganie.postman.ui.theme.Slate
import com.beinganie.postman.chat.AttachmentComposerType
import com.beinganie.postman.chat.ChatMessage
import com.beinganie.postman.chat.DeliveryState
import com.beinganie.postman.chat.MessageAttachment
import coil.compose.AsyncImage

@Composable
fun UserAvatar(
    displayName: String,
    photoModel: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    showPresence: Boolean = false,
    isOnline: Boolean = false,
) {
    val hasPhoto = when (photoModel) {
        is String -> photoModel.isNotBlank()
        else -> photoModel != null
    }

    Box(modifier = modifier.size(size)) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            if (hasPhoto) {
                AsyncImage(
                    model = photoModel,
                    contentDescription = "$displayName profile photo",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.size(size),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (showPresence) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp),
                shape = CircleShape,
                color = if (isOnline) Mint else Slate,
                tonalElevation = 2.dp,
            ) {}
        }
    }
}

@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun ConversationHeader(
    title: String,
    participantCount: Int,
    isOtherUserOnline: Boolean = false,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Back")
            }
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = if (isOtherUserOnline) Mint else Slate,
                    ) {}
                }
                Text(
                    text = if (isOtherUserOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                text = "$participantCount users",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onAttachmentClick: (() -> Unit)? = null,
) {
    val isMine = message.sender.isCurrentUser
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMine) 20.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 20.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isMine) {
                    Text(
                        text = "${message.sender.displayName} @${message.sender.username}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                message.attachment?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    AttachmentCard(
                        attachment = it,
                        isMine = isMine,
                        onClick = onAttachmentClick,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${message.sentAt}  ${deliveryLabel(message.deliveryState)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun AttachmentCard(
    attachment: MessageAttachment,
    isMine: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val title = when (attachment) {
        is MessageAttachment.Image -> "Photo"
        is MessageAttachment.Video -> "Video"
        is MessageAttachment.Document -> "Document"
    }
    val fileName = when (attachment) {
        is MessageAttachment.Image -> attachment.fileName
        is MessageAttachment.Video -> attachment.fileName
        is MessageAttachment.Document -> attachment.fileName
    }
    val previewModel = when (attachment) {
        is MessageAttachment.Image -> attachment.localPath.ifBlank { attachment.remoteUrl }
        else -> null
    }

    val supportingLabel = when {
        attachmentLocalPath(attachment).isNotBlank() -> "Saved on device"
        onClick != null -> "Tap to download"
        else -> null
    }

    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (attachment is MessageAttachment.Image && !previewModel.isNullOrBlank()) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = fileName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            supportingLabel?.let { label ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendAttachment: (AttachmentComposerType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AttachmentComposerType.entries.forEach { type ->
                AssistChip(
                    onClick = { onSendAttachment(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                label = { Text("Type a message") },
            )
            Button(
                onClick = onSend,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Send")
            }
        }
    }
}

private fun deliveryLabel(state: DeliveryState): String = when (state) {
    DeliveryState.SENDING -> "Sending"
    DeliveryState.DELIVERED -> "Delivered"
    DeliveryState.READ -> "Read"
}

private fun attachmentLocalPath(attachment: MessageAttachment): String = when (attachment) {
    is MessageAttachment.Image -> attachment.localPath
    is MessageAttachment.Video -> attachment.localPath
    is MessageAttachment.Document -> attachment.localPath
}
