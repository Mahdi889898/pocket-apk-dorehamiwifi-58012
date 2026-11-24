package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvatarSymbols
import com.example.model.DiscoveredRoom
import com.example.model.GameType
import com.example.model.RoomMode
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
import com.example.viewmodel.AppUiState

@Composable
fun WifiHubScreen(
    uiState: AppUiState,
    onHostRoom: (String, GameType) -> Unit,
    onJoinRoom: (String, Int) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHostDialog by remember { mutableStateOf(false) }
    var showDirectIpDialog by remember { mutableStateOf(false) }
    var roomNameInput by remember { mutableStateOf("اتاق ${uiState.profile.name}") }
    var selectedInitGame by remember { mutableStateOf(GameType.MENCH_CLASSIC) }
    var directIpInput by remember { mutableStateOf("192.168.1.") }

    val radarAnim = rememberInfiniteTransition(label = "radar")
    val radarScale by radarAnim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "radarScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "اتاق‌های چندنفره وای‌فای",
                style = MaterialTheme.typography.headlineMedium,
                color = BrightGold
            )
            Text(
                text = "با اتصال به یک مودم وای‌فای یا هات‌اسپات گوشی، بدون مصرف اینترنت بازی و چت صوتی کنید",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Local Device IP & Actions Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, RoyalGold.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                color = ElevatedSlate,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PersianTurquoise.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = NeonCyan)
                            }
                            Column {
                                Text("آدرس شبکه وای‌فای شما", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    text = uiState.localIp,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showHostDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("wifi_create_room_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Router, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ساخت اتاق جدید", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showDirectIpDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("wifi_direct_ip_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Lan, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اتصال با آی‌پی", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Error or Connecting Status
        if (uiState.isConnecting) {
            item {
                Surface(color = VelvetNavy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = RoyalGold, strokeWidth = 2.5.dp)
                        Text("در حال اتصال به اتاق وای‌فای...", color = TextPrimary)
                    }
                }
            }
        }
        uiState.connectionMessage?.let { msg ->
            item {
                Surface(
                    color = RubyCrimson.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RubyCrimson.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismissError) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = TextPrimary)
                        }
                    }
                }
            }
        }

        // Active Discovered Rooms Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(radarScale)
                        .clip(CircleShape)
                        .background(EmeraldFelt)
                )
                Text(
                    text = "اتاق‌های پیدا شده روی این وای‌فای (${uiState.discoveredRooms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        if (uiState.discoveredRooms.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelvetNavy,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = NeonCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "در حال جستجوی خودکار اتاق‌ها روی شبکه وای‌فای...",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "اگر دو گوشی روی یک وای‌فای یا هات‌اسپات هستند، یک نفر «ساخت اتاق جدید» را بزند تا بلافاصله اینجا ظاهر شود یا با دکمه «اتصال با آی‌پی» وارد شوید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.discoveredRooms, key = { it.roomId }) { room ->
                DiscoveredRoomCard(room = room, onJoin = { onJoinRoom(room.hostIp, room.tcpPort) })
            }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            containerColor = VelvetNavy,
            title = { Text("ساخت اتاق بازی وای‌فای", color = BrightGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        label = { Text("نام اتاق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("بازی اولیه اتاق:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GameType.entries) { g ->
                            val sel = g == selectedInitGame
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedInitGame = g },
                                color = if (sel) RoyalGold else ElevatedSlate
                            ) {
                                Text(
                                    text = g.titleFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) MidnightBg else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHostDialog = false
                        onHostRoom(roomNameInput, selectedInitGame)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg)
                ) {
                    Text("ساخت اتاق", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostDialog = false }) { Text("انصراف", color = TextSecondary) }
            }
        )
    }

    if (showDirectIpDialog) {
        AlertDialog(
            onDismissRequest = { showDirectIpDialog = false },
            containerColor = VelvetNavy,
            title = { Text("اتصال مستقیم با آدرس IP", color = BrightGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "آدرس IP نمایش داده شده در گوشی میزبان را وارد کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = directIpInput,
                        onValueChange = { directIpInput = it },
                        label = { Text("مثال: 192.168.1.5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDirectIpDialog = false
                        onJoinRoom(directIpInput, 48912)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PersianTurquoise, contentColor = MidnightBg)
                ) {
                    Text("اتصال به اتاق", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectIpDialog = false }) { Text("انصراف", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun DiscoveredRoomCard(
    room: DiscoveredRoom,
    onJoin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, room.gameType.accentStart.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        color = ElevatedSlate,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(room.gameType.accentStart, room.gameType.accentEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(room.gameType.icon, contentDescription = null, tint = MidnightBg)
                }
                Column {
                    Text(room.roomName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${room.gameType.titleFa} • میزبان: ${room.hostName}", style = MaterialTheme.typography.bodySmall, color = BrightGold)
                    Text("IP: ${room.hostIp} • ${room.playerCount}/${room.maxPlayers} نفر", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = PersianTurquoise, contentColor = MidnightBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text("ورود", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RoomLobbyScreen(
    uiState: AppUiState,
    onSelectGame: (GameType) -> Unit,
    onAddBot: () -> Unit,
    onRemoveBot: (String) -> Unit,
    onLaunchGame: () -> Unit,
    onLeaveRoom: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleOpenMic: () -> Unit,
    onToggleSpeakerMute: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHost = uiState.roomMode == RoomMode.WIFI_HOST

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Room Header
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, RoyalGold.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                color = ElevatedSlate,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.roomName, style = MaterialTheme.typography.titleLarge, color = BrightGold)
                        Text(
                            text = "${uiState.roomMode.titleFa} • IP: ${uiState.hostIpConnected.ifBlank { uiState.localIp }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = onLeaveRoom,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RubyCrimson.copy(alpha = 0.2f))
                            .testTag("leave_room_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "خروج از اتاق", tint = RubyCrimson)
                    }
                }
            }
        }

        // Players Roster
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بازیکنان حاضر در اتاق (${uiState.players.size}/${uiState.selectedGame.maxPlayers})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (isHost && uiState.players.size < uiState.selectedGame.maxPlayers) {
                        TextButton(onClick = onAddBot) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("افزودن ربات", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                uiState.players.forEach { p ->
                    val col = PlayerColors[p.colorIndex % 4]
                    Surface(
                        color = VelvetNavy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (p.isSpeaking) 2.dp else 1.dp,
                                color = if (p.isSpeaking) BrightGold else col.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(col.copy(alpha = 0.25f))
                                        .border(1.5.dp, col, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(AvatarSymbols[p.avatarIndex % AvatarSymbols.size], fontSize = 18.sp)
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(p.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        if (p.isHost) {
                                            Text("میزبان 👑", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                                        }
                                        if (p.isBot) {
                                            Icon(Icons.Rounded.SmartToy, contentDescription = "ربات", tint = NeonCyan, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    if (p.isSpeaking) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = EmeraldFelt, modifier = Modifier.size(14.dp))
                                            Text("در حال صحبت...", style = MaterialTheme.typography.labelSmall, color = EmeraldFelt)
                                        }
                                    }
                                }
                            }
                            if (isHost && p.isBot) {
                                IconButton(onClick = { onRemoveBot(p.id) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "حذف ربات", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Game Selector Carousel (All 12 Games)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isHost) "انتخاب بازی از میان ۸ بازی اصیل:" else "بازی انتخاب شده توسط میزبان:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameType.entries) { g ->
                        val selected = g == uiState.selectedGame
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) BrightGold else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = isHost) { onSelectGame(g) },
                            color = if (selected) g.accentStart.copy(alpha = 0.24f) else ElevatedSlate,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(g.icon, contentDescription = null, tint = if (selected) BrightGold else g.accentStart, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(g.titleFa, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                    Text("${g.minPlayers} تا ${g.maxPlayers} نفر", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Launch Game CTA
        item {
            if (isHost) {
                Button(
                    onClick = onLaunchGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("lobby_start_game_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("شروع بازی ${uiState.selectedGame.titleFa}!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Surface(color = ElevatedSlate, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "منتظر شروع بازی توسط میزبان... تا شروع بازی می‌توانید در زیر چت صوتی و متنی کنید! 🎙️",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Embedded Room Voice & Text Chat
        item {
            RoomChatAndVoicePanel(
                messages = uiState.chatMessages,
                localPlayerId = uiState.profile.id,
                voiceState = uiState.voiceState,
                onRequestMicPermission = onRequestMicPermission,
                onPushToTalkChange = onPushToTalkChange,
                onToggleOpenMic = onToggleOpenMic,
                onToggleSpeakerMute = onToggleSpeakerMute,
                onSendMessage = onSendMessage,
                onSendReaction = onSendReaction,
                chatListMaxHeightDp = 220
            )
        }
    }
}
