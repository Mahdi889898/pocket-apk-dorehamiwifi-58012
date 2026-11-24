package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.QuickPersianReactions
import com.example.model.VoiceChatState
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PersianTurquoise
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RubyCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy

@Composable
fun VoiceWaveformBars(
    level: Float,
    active: Boolean,
    barColor: Color = BrightGold,
    modifier: Modifier = Modifier
) {
    val multipliers = listOf(0.45f, 0.75f, 1.0f, 0.85f, 0.6f, 0.9f, 0.5f)
    Row(
        modifier = modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        multipliers.forEach { mult ->
            val targetH = if (active) (0.22f + level * mult * 0.78f).coerceIn(0.22f, 1f) else 0.18f
            val animH by animateFloatAsState(targetValue = targetH, animationSpec = tween(90), label = "bar")
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animH)
                    .clip(CircleShape)
                    .background(if (active) barColor else TextMuted.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun InGameCommsBar(
    voiceState: VoiceChatState,
    unreadChatCount: Int,
    lastMessagePreview: String?,
    onRequestMicPermission: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleOpenMic: () -> Unit,
    onToggleSpeakerMute: () -> Unit,
    onOpenChatSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(
                width = 1.2.dp,
                color = if (voiceState.isMicActive) BrightGold else RoyalGold.copy(alpha = 0.3f),
                shape = RoundedCornerShape(22.dp)
            ),
        color = VelvetNavy.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Push-To-Talk Hold Pill
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (voiceState.isMicActive) Brush.horizontalGradient(listOf(EmeraldFelt, PersianTurquoise))
                        else Brush.horizontalGradient(listOf(ElevatedSlate, Color(0xFF1E3249)))
                    )
                    .border(
                        width = 1.dp,
                        color = if (voiceState.isMicActive) Color.White else RoyalGold.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .pointerInput(voiceState.hasRecordPermission) {
                        detectTapGestures(
                            onPress = {
                                if (!voiceState.hasRecordPermission) {
                                    onRequestMicPermission()
                                    return@detectTapGestures
                                }
                                onPushToTalkChange(true)
                                tryAwaitRelease()
                                onPushToTalkChange(false)
                            }
                        )
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("ingame_ptt_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (voiceState.isMicActive) Icons.Rounded.Mic else Icons.Rounded.MicNone,
                        contentDescription = "نگه دارید برای صحبت",
                        tint = if (voiceState.isMicActive) MidnightBg else BrightGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (voiceState.isMicActive) "در حال پخش صدا..." else "نگه دار و حرف بزن",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (voiceState.isMicActive) MidnightBg else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Open Mic Quick Toggle
            IconButton(
                onClick = {
                    if (!voiceState.hasRecordPermission) onRequestMicPermission()
                    else onToggleOpenMic()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (voiceState.isOpenMic) RoyalGold.copy(alpha = 0.22f) else ElevatedSlate)
                    .testTag("ingame_open_mic_btn")
            ) {
                Icon(
                    imageVector = if (voiceState.isOpenMic) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    contentDescription = "میکروفون باز",
                    tint = if (voiceState.isOpenMic) BrightGold else TextSecondary
                )
            }

            // Speaker Mute Toggle
            IconButton(
                onClick = onToggleSpeakerMute,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (voiceState.isSpeakerMuted) RubyCrimson.copy(alpha = 0.22f) else ElevatedSlate)
                    .testTag("ingame_speaker_btn")
            ) {
                Icon(
                    imageVector = if (voiceState.isSpeakerMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "صدا",
                    tint = if (voiceState.isSpeakerMuted) RubyCrimson else NeonCyan
                )
            }

            // Live waveform or speaker indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenChatSheet),
                contentAlignment = Alignment.Center
            ) {
                if (voiceState.isMicActive) {
                    VoiceWaveformBars(level = voiceState.localMicLevel, active = true)
                } else if (voiceState.activeSpeakerName != null) {
                    Text(
                        text = "🔊 ${voiceState.activeSpeakerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldFelt,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!lastMessagePreview.isNullOrBlank()) {
                    Text(
                        text = lastMessagePreview,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "چت اتاق",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Chat Sheet Trigger with Unread Badge
            IconButton(
                onClick = onOpenChatSheet,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RoyalGold.copy(alpha = 0.16f))
                    .testTag("open_chat_sheet_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadChatCount > 0) {
                            Badge(containerColor = RubyCrimson, contentColor = Color.White) {
                                Text(unreadChatCount.coerceAtMost(99).toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = "چت متنی اتاق",
                        tint = BrightGold
                    )
                }
            }
        }
    }
}

@Composable
fun RoomChatAndVoicePanel(
    messages: List<ChatMessage>,
    localPlayerId: String,
    voiceState: VoiceChatState,
    onRequestMicPermission: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleOpenMic: () -> Unit,
    onToggleSpeakerMute: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    chatListMaxHeightDp: Int = 240
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Voice Studio Control Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            color = ElevatedSlate,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = BrightGold)
                        Column {
                            Text("چت صوتی زنده وای‌فای", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = when {
                                    !voiceState.hasRecordPermission -> "برای صحبت دسترسی میکروفون را فعال کنید"
                                    voiceState.isMicActive -> "میکروفون فعال و در حال ارسال صدا"
                                    voiceState.activeSpeakerName != null -> "${voiceState.activeSpeakerName} در حال صحبت است..."
                                    else -> "آماده مکالمه بدون تاخیر روی وای‌فای"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (voiceState.isMicActive) EmeraldFelt else TextSecondary
                            )
                        }
                    }
                    VoiceWaveformBars(level = voiceState.localMicLevel, active = voiceState.isMicActive)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hold to talk button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (voiceState.isPushToTalkPressed) EmeraldFelt
                                else RoyalGold.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (voiceState.isPushToTalkPressed) Color.White else RoyalGold,
                                RoundedCornerShape(14.dp)
                            )
                            .pointerInput(voiceState.hasRecordPermission) {
                                detectTapGestures(
                                    onPress = {
                                        if (!voiceState.hasRecordPermission) {
                                            onRequestMicPermission()
                                            return@detectTapGestures
                                        }
                                        onPushToTalkChange(true)
                                        tryAwaitRelease()
                                        onPushToTalkChange(false)
                                    }
                                )
                            }
                            .testTag("sheet_ptt_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (voiceState.isPushToTalkPressed) "رها کنید تا قطع شود 🎙️" else "نگه دارید و صحبت کنید 🎙️",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (voiceState.isPushToTalkPressed) MidnightBg else BrightGold
                        )
                    }

                    // Hands-Free Open Mic Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("باز", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Switch(
                            checked = voiceState.isOpenMic,
                            onCheckedChange = {
                                if (!voiceState.hasRecordPermission) onRequestMicPermission()
                                else onToggleOpenMic()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MidnightBg,
                                checkedTrackColor = EmeraldFelt
                            )
                        )
                    }

                    // Speaker Mute
                    IconButton(
                        onClick = onToggleSpeakerMute,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (voiceState.isSpeakerMuted) RubyCrimson.copy(alpha = 0.25f) else MidnightBg)
                    ) {
                        Icon(
                            imageVector = if (voiceState.isSpeakerMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "تغییر وضعیت بلندگو",
                            tint = if (voiceState.isSpeakerMuted) RubyCrimson else NeonCyan
                        )
                    }
                }
            }
        }

        // Quick Persian Reactions Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(QuickPersianReactions) { reaction ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSendReaction(reaction) },
                    color = ElevatedSlate,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = reaction,
                        style = MaterialTheme.typography.labelMedium,
                        color = BrightGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Chat Feed
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = chatListMaxHeightDp.dp),
            color = MidnightBg.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "هنوز پیامی ارسال نشده؛ به دوستانتان سلام کنید! 👋",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        if (msg.isSystem) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeonCyan.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            val isMine = msg.senderId == localPlayerId
                            val senderCol = PlayerColors[msg.senderColorIndex % PlayerColors.size]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMine) Arrangement.Start else Arrangement.End
                            ) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isMine) RoyalGold.copy(alpha = 0.2f) else ElevatedSlate)
                                        .border(
                                            1.dp,
                                            if (isMine) RoyalGold.copy(alpha = 0.4f) else senderCol.copy(alpha = 0.35f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    if (!isMine) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = senderCol,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message Composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("پیامی بنویسید...", color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSendMessage(draft)
                        draft = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RoyalGold)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "ارسال پیام",
                    tint = MidnightBg
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomChatModalSheet(
    messages: List<ChatMessage>,
    localPlayerId: String,
    voiceState: VoiceChatState,
    onDismiss: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleOpenMic: () -> Unit,
    onToggleSpeakerMute: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelvetNavy,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "چت متنی و صوتی اتاق",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrightGold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            RoomChatAndVoicePanel(
                messages = messages,
                localPlayerId = localPlayerId,
                voiceState = voiceState,
                onRequestMicPermission = onRequestMicPermission,
                onPushToTalkChange = onPushToTalkChange,
                onToggleOpenMic = onToggleOpenMic,
                onToggleSpeakerMute = onToggleSpeakerMute,
                onSendMessage = onSendMessage,
                onSendReaction = onSendReaction,
                chatListMaxHeightDp = 280
            )
        }
    }
}
