package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AvatarSymbols
import com.example.model.GameCategory
import com.example.model.GameType
import com.example.model.RoomMode
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PersianTurquoise
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy
import com.example.viewmodel.AppUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AppUiState,
    onHostRoomWithGame: (GameType) -> Unit,
    onOpenWifiHub: () -> Unit,
    onStartLocalGame: (GameType, RoomMode, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(GameCategory.ALL) }
    var chosenGameForSheet by remember { mutableStateOf<GameType?>(null) }

    val filteredGames = remember(selectedCategory) {
        if (selectedCategory == GameCategory.ALL) GameType.entries
        else GameType.entries.filter { it.category == selectedCategory }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 164.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Greeting & Profile Chip
        item(span = { GridItemSpan(maxLineSpan) }) {
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
                            .background(Brush.radialGradient(listOf(RoyalGold, Color(0xFF925F0A))))
                            .border(1.5.dp, BrightGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AvatarSymbols[uiState.profile.avatarIndex % AvatarSymbols.size],
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(
                            text = "سلام، ${uiState.profile.name}!",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "منچ قدیم سه‌بعدی، مارپله، حکم و بازی‌های اصیل",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PersianTurquoise.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(1.dp, PersianTurquoise.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenWifiHub)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = "وای‌فای", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${uiState.discoveredRooms.size} اتاق",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // High-Graphics Hero Lounge Banner
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(BrightGold, PersianTurquoise)),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_lounge),
                    contentDescription = "سالن بازی دورهمی وای‌فای",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    MidnightBg.copy(alpha = 0.65f),
                                    MidnightBg.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = RoyalGold.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = MidnightBg, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "چت صوتی و متنی زنده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MidnightBg,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Surface(
                            color = MidnightBg.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "IP: ${uiState.localIp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "دورهمی وای‌فای سلطنتی",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrightGold
                        )
                        Text(
                            text = "بدون اینترنت، با یک وای‌فای یا هات‌اسپات کنار هم بازی کنید و حرف بزنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onHostRoomWithGame(GameType.MENCH_CLASSIC) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_host_wifi_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Router, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ساخت اتاق وای‌فای", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenWifiHub,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_join_wifi_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = VelvetNavy.copy(alpha = 0.85f),
                                    contentColor = NeonCyan
                                )
                            ) {
                                Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("اتصال به اتاق", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Row
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GameCategory.entries) { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.titleFa,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalGold,
                            selectedLabelColor = MidnightBg,
                            containerColor = ElevatedSlate,
                            labelColor = TextSecondary
                        ),
                        border = null
                    )
                }
            }
        }

        // 12 High-Graphics Game Cards Grid
        items(filteredGames, key = { it.id }) { game ->
            GameCatalogCard(
                game = game,
                onClick = { chosenGameForSheet = game }
            )
        }
    }

    // Play Mode Selector Bottom Sheet
    chosenGameForSheet?.let { game ->
        var chosenCount by remember(game) { mutableIntStateOf(game.minPlayers) }
        ModalBottomSheet(
            onDismissRequest = { chosenGameForSheet = null },
            containerColor = VelvetNavy,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(game.accentStart, game.accentEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(game.icon, contentDescription = null, tint = MidnightBg, modifier = Modifier.size(30.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(game.titleFa, style = MaterialTheme.typography.headlineSmall, color = BrightGold)
                        Text(game.subtitleFa, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                Surface(
                    color = ElevatedSlate,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = game.rulesFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (game.maxPlayers > game.minPlayers) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعداد بازیکنان:", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (cnt in game.minPlayers..game.maxPlayers) {
                                FilterChip(
                                    selected = chosenCount == cnt,
                                    onClick = { chosenCount = cnt },
                                    label = { Text("$cnt نفر") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalGold,
                                        selectedLabelColor = MidnightBg
                                    )
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        chosenGameForSheet = null
                        onHostRoomWithGame(game)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sheet_host_wifi_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ساخت اتاق وای‌فای (چندنفره + چت صوتی)", fontWeight = FontWeight.ExtraBold)
                }

                Button(
                    onClick = {
                        chosenGameForSheet = null
                        onStartLocalGame(game, RoomMode.SOLO_VS_BOT, chosenCount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sheet_play_bot_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PersianTurquoise, contentColor = MidnightBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بازی سریع با هوش مصنوعی ($chosenCount نفره)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        chosenGameForSheet = null
                        onStartLocalGame(game, RoomMode.LOCAL_PASS_AND_PLAY, chosenCount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sheet_play_local_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Group, contentDescription = null, tint = BrightGold)
                    Spacer(Modifier.width(8.dp))
                    Text("بازی دورهمی روی همین گوشی ($chosenCount نفر)", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameCatalogCard(
    game: GameType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, game.accentStart.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("game_card_${game.id}"),
        color = ElevatedSlate,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Illustrated Visual Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                Image(
                    painter = painterResource(id = game.bannerRes),
                    contentDescription = game.titleFa,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    game.accentStart.copy(alpha = 0.55f),
                                    MidnightBg.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                // Player count pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = MidnightBg.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (game.minPlayers == game.maxPlayers) "${game.minPlayers} نفره"
                        else "${game.minPlayers} تا ${game.maxPlayers} نفر",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                // Floating Game Icon Medallion
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(game.accentStart, game.accentEnd)))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(game.icon, contentDescription = null, tint = MidnightBg, modifier = Modifier.size(24.dp))
                }
            }

            // Title & Play Cue
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = game.titleFa,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = game.subtitleFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وای‌فای • آفلاین",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldFelt
                    )
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "شروع",
                        tint = BrightGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
