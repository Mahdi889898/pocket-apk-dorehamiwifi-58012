package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.BackgammonState
import com.example.games.ChaharBargState
import com.example.games.HaftKhabisState
import com.example.games.HokmState
import com.example.games.MenchClassicState
import com.example.games.PachisiState
import com.example.games.ShelemState
import com.example.games.SnakesLaddersState
import com.example.model.AvatarSymbols
import com.example.model.RoomMode
import com.example.ui.games.Backgammon3DBoard
import com.example.ui.games.ChaharBargTableUi
import com.example.ui.games.HaftKhabisTableUi
import com.example.ui.games.HokmTableUi
import com.example.ui.games.MenchClassicBoard
import com.example.ui.games.Pachisi3DBoard
import com.example.ui.games.ShelemTableUi
import com.example.ui.games.SnakesLadders3DBoard
import com.example.ui.games.WinnerCelebrationBanner
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlayerColorNamesFa
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy
import com.example.viewmodel.AppUiState

@Composable
fun ActiveGameScreen(
    uiState: AppUiState,
    onGameAction: (String, Int?) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onOpenChatSheet: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleOpenMic: () -> Unit,
    onToggleSpeakerMute: () -> Unit
) {
    val gameState = uiState.activeGameState ?: return
    val gameType = gameState.gameType
    var showRulesDialog by remember { mutableStateOf(false) }

    val isPassAndPlay = uiState.roomMode == RoomMode.LOCAL_PASS_AND_PLAY
    val currentPlayerObj = uiState.players.getOrNull(gameState.currentTurn)
    val canAct = when (uiState.roomMode) {
        RoomMode.LOCAL_PASS_AND_PLAY -> currentPlayerObj?.isBot != true
        RoomMode.SOLO_VS_BOT -> gameState.currentTurn == uiState.localPlayerIndex
        RoomMode.WIFI_HOST, RoomMode.WIFI_CLIENT -> gameState.currentTurn == uiState.localPlayerIndex
    }
    val viewerSeat = if (isPassAndPlay) gameState.currentTurn else uiState.localPlayerIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
            .statusBarsPadding()
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("game_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                }
                Column {
                    Text(gameType.titleFa, style = MaterialTheme.typography.titleLarge, color = BrightGold)
                    Text(uiState.roomMode.titleFa, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Icon(Icons.Rounded.HelpOutline, contentDescription = "راهنمای بازی", tint = TextSecondary)
                }
                if (uiState.isHostOrLocal) {
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier.testTag("game_restart_button")
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = "شروع مجدد", tint = BrightGold)
                    }
                }
            }
        }

        // Scrollable Game Arena
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                is MenchClassicState -> MenchClassicBoard(
                    state = gameState,
                    players = uiState.players,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is SnakesLaddersState -> SnakesLadders3DBoard(
                    state = gameState,
                    players = uiState.players,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is PachisiState -> Pachisi3DBoard(
                    state = gameState,
                    players = uiState.players,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is BackgammonState -> Backgammon3DBoard(
                    state = gameState,
                    players = uiState.players,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is HokmState -> HokmTableUi(
                    state = gameState,
                    players = uiState.players,
                    viewerSeat = viewerSeat,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is ShelemState -> ShelemTableUi(
                    state = gameState,
                    players = uiState.players,
                    viewerSeat = viewerSeat,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is ChaharBargState -> ChaharBargTableUi(
                    state = gameState,
                    players = uiState.players,
                    viewerSeat = viewerSeat,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
                is HaftKhabisState -> HaftKhabisTableUi(
                    state = gameState,
                    players = uiState.players,
                    viewerSeat = viewerSeat,
                    canAct = canAct,
                    onAction = { onGameAction(it, null) }
                )
            }

            gameState.winnerIndex?.let { winnerIdx ->
                WinnerCelebrationBanner(
                    winnerIndex = winnerIdx,
                    players = uiState.players,
                    isTeamGame = gameState is HokmState || gameState is ShelemState,
                    canRestart = uiState.isHostOrLocal,
                    onRestart = onRestart,
                    onBack = onBack
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // Anchored In-Game Voice & Chat Bar
        InGameCommsBar(
            voiceState = uiState.voiceState,
            unreadChatCount = uiState.unreadChatCount,
            lastMessagePreview = uiState.chatMessages.lastOrNull()?.let { "${it.senderName}: ${it.text}" },
            onRequestMicPermission = onRequestMicPermission,
            onPushToTalkChange = onPushToTalkChange,
            onToggleOpenMic = onToggleOpenMic,
            onToggleSpeakerMute = onToggleSpeakerMute,
            onOpenChatSheet = onOpenChatSheet
        )
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            containerColor = VelvetNavy,
            title = { Text("قوانین ${gameType.titleFa}", color = BrightGold) },
            text = { Text(gameType.rulesFa, color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("متوجه شدم", color = BrightGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProfileScreen(
    uiState: AppUiState,
    onSaveProfile: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember(uiState.profile.name) { mutableStateOf(uiState.profile.name) }
    var selectedAvatar by remember(uiState.profile.avatarIndex) { mutableIntStateOf(uiState.profile.avatarIndex) }
    var selectedColor by remember(uiState.profile.favoriteColorIndex) { mutableIntStateOf(uiState.profile.favoriteColorIndex) }
    var savedBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("پروفایل بازیکن", style = MaterialTheme.typography.headlineMedium, color = BrightGold)
            Text("نام و نشان خود را برای اتاق‌های چندنفره وای‌فای تنظیم کنید", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "پیروزی‌ها",
                    value = uiState.profile.totalWins.toString(),
                    icon = Icons.Rounded.EmojiEvents,
                    accent = BrightGold,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "بازی‌های انجام شده",
                    value = uiState.profile.gamesPlayed.toString(),
                    icon = Icons.Rounded.SportsEsports,
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                color = ElevatedSlate,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            savedBanner = false
                        },
                        label = { Text("نام نمایشی در اتاق بازی") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Text("انتخاب آواتار:", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AvatarSymbols.forEachIndexed { idx, sym ->
                            val isSel = idx == selectedAvatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) RoyalGold.copy(alpha = 0.3f) else MidnightBg)
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) BrightGold else Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedAvatar = idx
                                        savedBanner = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sym, fontSize = 18.sp)
                            }
                        }
                    }

                    Text("رنگ مهره دلخواه:", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayerColors.forEachIndexed { idx, col ->
                            val isSel = idx == selectedColor
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) Color.White else col.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedColor = idx
                                        savedBanner = false
                                    },
                                color = col.copy(alpha = if (isSel) 0.35f else 0.14f)
                            ) {
                                Text(
                                    text = PlayerColorNamesFa[idx],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSaveProfile(nameInput, selectedAvatar, selectedColor)
                            savedBanner = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (savedBanner) "تغییرات ذخیره شد ✓" else "ذخیره پروفایل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetNavy,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = EmeraldFelt)
                        Text("راهنمای بازی چندنفره روی وای‌فای", style = MaterialTheme.typography.titleMedium, color = EmeraldFelt)
                    }
                    Text(
                        text = "۱. همه گوشی‌ها را به یک وای‌فای خانگی متصل کنید (یا یک نفر هات‌اسپات گوشی خود را روشن کند و بقیه به آن وصل شوند).\n" +
                            "۲. یک بازیکن در تب «اتاق وای‌فای» گزینه «ساخت اتاق جدید» را می‌زند.\n" +
                            "۳. سایر بازیکنان اتاق را به‌صورت خودکار می‌بینند یا آدرس IP میزبان را وارد می‌کنند.\n" +
                            "۴. در بازی‌های ۴ نفره مثل حکم و شلم، صندلی‌های خالی به‌طور خودکار با ربات هوشمند پر می‌شوند و در طول بازی چت صوتی و متنی فعال است!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        color = ElevatedSlate,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
