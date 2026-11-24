package com.example.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.CardDeck
import com.example.games.CardSuit
import com.example.games.ChaharBargEngine
import com.example.games.ChaharBargState
import com.example.games.HaftKhabisEngine
import com.example.games.HaftKhabisState
import com.example.games.HokmEngine
import com.example.games.HokmState
import com.example.games.ShelemEngine
import com.example.games.ShelemState
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepEmeraldFelt
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PersianTurquoise
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RubyCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy
import com.example.ui.theme.WalnutWood
import org.json.JSONObject

@Composable
private fun VelvetTableArena(
    is3D: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Board3DContainer(
        is3D = is3D,
        modifier = modifier,
        slabTopColor = WalnutWood,
        slabSideColor = Color(0xFF211108),
        innerGlow = EmeraldFelt
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0E6B4F), DeepEmeraldFelt, Color(0xFF05241B))
                    )
                )
                .border(1.5.dp, BrightGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            content()
        }
    }
}

// ============================================================================
// 5. HOKM 4-PLAYER TABLE UI (میز حکم ۴ نفره تیمی سه‌بعدی)
// ============================================================================
@Composable
fun HokmTableUi(
    state: HokmState,
    players: List<PlayerInfo>,
    viewerSeat: Int,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val seat = viewerSeat.coerceIn(0, 3)
    val partnerSeat = (seat + 2) % 4
    val rightSeat = (seat + 1) % 4
    val leftSeat = (seat + 3) % 4
    val legal = HokmEngine.legalCards(state, seat)
    val myTeam = seat % 2
    val oppTeam = 1 - myTeam
    val hokmSuitObj = state.hokmSuit?.let { CardSuit.fromOrdinal(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Scoreboard & 3D Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ElevatedSlate,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(1.dp, RoyalGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ما: ${state.teamTricks[myTeam]}/7", style = MaterialTheme.typography.labelMedium, color = EmeraldFelt, fontWeight = FontWeight.ExtraBold)
                    Text("حریف: ${state.teamTricks[oppTeam]}/7", style = MaterialTheme.typography.labelMedium, color = RubyCrimson, fontWeight = FontWeight.ExtraBold)
                    Text("راند: ${state.teamHandsWon[myTeam]}-${state.teamHandsWon[oppTeam]}", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                }
            }
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        // 3D Velvet Card Table
        VelvetTableArena(
            is3D = is3D,
            modifier = Modifier.fillMaxWidth().height(272.dp)
        ) {
            // Hokm Medallion Badge Top-End
            if (hokmSuitObj != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .border(1.2.dp, BrightGold, RoundedCornerShape(12.dp)),
                    color = MidnightBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "حکم: ${hokmSuitObj.nameFa} ${hokmSuitObj.symbol}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hokmSuitObj.isRed) Color(0xFFFF8DA1) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Partner (Top)
            SeatTag(
                name = players.getOrNull(partnerSeat)?.name ?: "یار",
                isHakem = state.hakem == partnerSeat,
                isTurn = state.currentTurn == partnerSeat,
                cardCount = state.hands[partnerSeat].size,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Left & Right Opponents
            SeatTag(
                name = players.getOrNull(leftSeat)?.name ?: "حریف ۱",
                isHakem = state.hakem == leftSeat,
                isTurn = state.currentTurn == leftSeat,
                cardCount = state.hands[leftSeat].size,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            SeatTag(
                name = players.getOrNull(rightSeat)?.name ?: "حریف ۲",
                isHakem = state.hakem == rightSeat,
                isTurn = state.currentTurn == rightSeat,
                cardCount = state.hands[rightSeat].size,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // Center 4-Card Trick Display
            Box(
                modifier = Modifier.align(Alignment.Center).size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.trickCards[partnerSeat] != -1) {
                    PlayingCardView(state.trickCards[partnerSeat], 48.dp, 70.dp, modifier = Modifier.align(Alignment.TopCenter))
                }
                if (state.trickCards[leftSeat] != -1) {
                    PlayingCardView(state.trickCards[leftSeat], 48.dp, 70.dp, modifier = Modifier.align(Alignment.CenterStart))
                }
                if (state.trickCards[rightSeat] != -1) {
                    PlayingCardView(state.trickCards[rightSeat], 48.dp, 70.dp, modifier = Modifier.align(Alignment.CenterEnd))
                }
                if (state.trickCards[seat] != -1) {
                    PlayingCardView(state.trickCards[seat], 50.dp, 74.dp, isHighlighted = true, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }

            // Bottom Self Hakem / Seat label
            SeatTag(
                name = players.getOrNull(seat)?.name ?: "شما",
                isHakem = state.hakem == seat,
                isTurn = state.currentTurn == seat,
                cardCount = state.hands[seat].size,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Hakem Suit Selection Bar
        if (state.phase == "CHOOSE_HOKM" && canAct && state.hakem == seat) {
            SuitPickerBar(
                titleFa = "شما حاکم هستید! 👑 خال حکم این دست را انتخاب کنید:",
                onPickSuit = { s -> onAction(JSONObject().put("op", "PICK_HOKM").put("suit", s).toString()) }
            )
        }

        // Player's Interactive Card Hand
        val myHand = state.hands.getOrElse(seat) { emptyList() }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
        ) {
            items(myHand, key = { it }) { cardId ->
                val playable = canAct && cardId in legal
                PlayingCardView(
                    cardId = cardId,
                    width = 60.dp,
                    height = 88.dp,
                    isPlayable = playable || state.phase == "CHOOSE_HOKM",
                    isHighlighted = playable,
                    onClick = if (playable) {
                        { onAction(JSONObject().put("op", "PLAY_CARD").put("card", cardId).toString()) }
                    } else null
                )
            }
        }

        StatusBannerCard(state.statusFa, RoyalGold)
    }
}

@Composable
private fun SeatTag(
    name: String,
    isHakem: Boolean,
    isTurn: Boolean,
    cardCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(
            width = if (isTurn) 1.8.dp else 1.dp,
            color = if (isTurn) BrightGold else Color.White.copy(alpha = 0.25f),
            shape = RoundedCornerShape(10.dp)
        ),
        color = MidnightBg.copy(alpha = 0.82f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isHakem) Text("👑", fontSize = 11.sp)
            Text(name, style = MaterialTheme.typography.labelSmall, color = if (isTurn) BrightGold else TextPrimary, maxLines = 1)
            Text("($cardCount)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
        }
    }
}

// ============================================================================
// 6. SHELEM TABLE UI (میز شلم ۴ نفره امتیازی با مزایده ۱۰۰ تا ۱۶۵)
// ============================================================================
@Composable
fun ShelemTableUi(
    state: ShelemState,
    players: List<PlayerInfo>,
    viewerSeat: Int,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val seat = viewerSeat.coerceIn(0, 3)
    val partnerSeat = (seat + 2) % 4
    val rightSeat = (seat + 1) % 4
    val leftSeat = (seat + 3) % 4
    val legal = ShelemEngine.legalCards(state, seat)
    val myTeam = seat % 2
    val oppTeam = 1 - myTeam
    val hokmSuitObj = state.hokmSuit?.let { CardSuit.fromOrdinal(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ElevatedSlate,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(1.dp, RoyalGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ما: ${state.teamPoints[myTeam]}", style = MaterialTheme.typography.labelMedium, color = EmeraldFelt, fontWeight = FontWeight.ExtraBold)
                    Text("حریف: ${state.teamPoints[oppTeam]}", style = MaterialTheme.typography.labelMedium, color = RubyCrimson, fontWeight = FontWeight.ExtraBold)
                    Text("تعهد: ${state.highestBid.coerceAtLeast(100)}", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                }
            }
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        VelvetTableArena(is3D = is3D, modifier = Modifier.fillMaxWidth().height(258.dp)) {
            if (hokmSuitObj != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).border(1.2.dp, BrightGold, RoundedCornerShape(12.dp)),
                    color = MidnightBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "حکم: ${hokmSuitObj.nameFa} ${hokmSuitObj.symbol}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hokmSuitObj.isRed) Color(0xFFFF8DA1) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
            SeatTag(players.getOrNull(partnerSeat)?.name ?: "یار", state.highestBidder == partnerSeat, state.currentTurn == partnerSeat, state.hands[partnerSeat].size, Modifier.align(Alignment.TopCenter))
            SeatTag(players.getOrNull(leftSeat)?.name ?: "حریف ۱", state.highestBidder == leftSeat, state.currentTurn == leftSeat, state.hands[leftSeat].size, Modifier.align(Alignment.CenterStart))
            SeatTag(players.getOrNull(rightSeat)?.name ?: "حریف ۲", state.highestBidder == rightSeat, state.currentTurn == rightSeat, state.hands[rightSeat].size, Modifier.align(Alignment.CenterEnd))

            Box(modifier = Modifier.align(Alignment.Center).size(164.dp), contentAlignment = Alignment.Center) {
                if (state.trickCards[partnerSeat] != -1) PlayingCardView(state.trickCards[partnerSeat], 46.dp, 68.dp, modifier = Modifier.align(Alignment.TopCenter))
                if (state.trickCards[leftSeat] != -1) PlayingCardView(state.trickCards[leftSeat], 46.dp, 68.dp, modifier = Modifier.align(Alignment.CenterStart))
                if (state.trickCards[rightSeat] != -1) PlayingCardView(state.trickCards[rightSeat], 46.dp, 68.dp, modifier = Modifier.align(Alignment.CenterEnd))
                if (state.trickCards[seat] != -1) PlayingCardView(state.trickCards[seat], 48.dp, 72.dp, isHighlighted = true, modifier = Modifier.align(Alignment.BottomCenter))
            }

            SeatTag(players.getOrNull(seat)?.name ?: "شما", state.highestBidder == seat, state.currentTurn == seat, state.hands[seat].size, Modifier.align(Alignment.BottomCenter))
        }

        // Bidding Panel
        if (state.phase == "BID" && canAct && state.currentTurn == seat) {
            val minNext = (state.highestBid + 5).coerceAtLeast(100)
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.5.dp, BrightGold, RoundedCornerShape(16.dp)),
                color = VelvetNavy,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نوبت مزایده شما! امتیاز تعهدی بخوانید یا پاس دهید:", style = MaterialTheme.typography.labelLarge, color = BrightGold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(minNext, minNext + 5, minNext + 10).filter { it <= 165 }.forEach { b ->
                            Button(
                                onClick = { onAction(JSONObject().put("op", "BID").put("bid", b).toString()) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("$b", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Button(
                            onClick = { onAction(JSONObject().put("op", "BID").put("bid", 0).toString()) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RubyCrimson, contentColor = Color.White),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("پاس", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.phase == "CHOOSE_HOKM" && canAct && state.highestBidder == seat) {
            SuitPickerBar(
                titleFa = "شما برنده مزایده (${state.highestBid}) شدید! خال حکم را انتخاب کنید:",
                onPickSuit = { s -> onAction(JSONObject().put("op", "PICK_HOKM").put("suit", s).toString()) }
            )
        }

        val myHand = state.hands.getOrElse(seat) { emptyList() }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
        ) {
            items(myHand, key = { it }) { cardId ->
                val playable = canAct && cardId in legal
                PlayingCardView(
                    cardId = cardId,
                    width = 58.dp,
                    height = 86.dp,
                    isPlayable = playable || state.phase != "PLAY",
                    isHighlighted = playable,
                    onClick = if (playable) {
                        { onAction(JSONObject().put("op", "PLAY_CARD").put("card", cardId).toString()) }
                    } else null
                )
            }
        }

        StatusBannerCard(state.statusFa, Color(0xFFA855F7))
    }
}

// ============================================================================
// 7. CHAHAR BARG / PASOOR 11 UI (چهاربرگ / پاسور ۱۱ اصیل ایرانی)
// ============================================================================
@Composable
fun ChaharBargTableUi(
    state: ChaharBargState,
    players: List<PlayerInfo>,
    viewerSeat: Int,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val seat = viewerSeat.coerceIn(0, 1)
    val s0 = ChaharBargEngine.computeScore(state.collected[0], state.surCounts[0])
    val s1 = ChaharBargEngine.computeScore(state.collected[1], state.surCounts[1])

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("بانک کارت‌ها: ${state.deck.size} برگ", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("$s0 امتیاز (${state.surCounts[0]} سور)", "$s1 امتیاز (${state.surCounts[1]} سور)")
        )

        VelvetTableArena(is3D = is3D, modifier = Modifier.fillMaxWidth().height(225.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("کارت‌های روی زمین (جمع ۱۱ یا سرباز جاروکن)", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                if (state.tableCards.isEmpty()) {
                    Text("زمین خالی است! ✨", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        items(state.tableCards, key = { it }) { cardId ->
                            PlayingCardView(cardId = cardId, width = 58.dp, height = 86.dp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("کارت‌های جمع‌شده شما: ${state.collected[seat].size}", style = MaterialTheme.typography.labelSmall, color = EmeraldFelt)
                    Text("خاج شما: ${state.collected[seat].count { CardDeck.suitOf(it) == CardSuit.CLUBS }}", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                }
            }
        }

        // Your 4-Card Hand
        val myHand = state.hands.getOrElse(seat) { emptyList() }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            myHand.forEach { cardId ->
                val willCapture = ChaharBargEngine.findBestCapture(cardId, state.tableCards).isNotEmpty()
                PlayingCardView(
                    cardId = cardId,
                    width = 68.dp,
                    height = 98.dp,
                    isPlayable = canAct && state.winnerIndex == null,
                    isHighlighted = canAct && willCapture,
                    onClick = if (canAct && state.winnerIndex == null) {
                        { onAction(JSONObject().put("card", cardId).toString()) }
                    } else null
                )
            }
        }

        StatusBannerCard(state.statusFa, PersianTurquoise)
    }
}

// ============================================================================
// 8. HAFT KHABIS UI (هفت خبیث ایرانی ۲ تا ۴ نفره)
// ============================================================================
@Composable
fun HaftKhabisTableUi(
    state: HaftKhabisState,
    players: List<PlayerInfo>,
    viewerSeat: Int,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val seat = viewerSeat.coerceIn(0, state.playerCount - 1)
    val playable = HaftKhabisEngine.playableCards(state, seat)
    val activeSuitObj = CardSuit.fromOrdinal(state.activeSuit)
    val topCard = state.discardPile.lastOrNull() ?: 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.direction == 1) "جهت بازی: ساعت‌گرد ↻" else "جهت بازی: پادساعت‌گرد ↺",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.hands.map { "${it.size} کارت" }
        )

        VelvetTableArena(is3D = is3D, modifier = Modifier.fillMaxWidth().height(215.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MidnightBg.copy(alpha = 0.85f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "خال فعال: ${activeSuitObj.nameFa} ${activeSuitObj.symbol}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (activeSuitObj.isRed) Color(0xFFFF8FA3) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (state.pendingDraw > 0) {
                        Surface(color = RubyCrimson, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "جریمه +${state.pendingDraw} کارت! 🔥",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    // Draw Deck Pile
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CardBackView(
                            width = 66.dp,
                            height = 96.dp,
                            countBadge = state.deck.size,
                            onClick = if (canAct && !state.awaitingSuitPick && state.winnerIndex == null) {
                                { onAction(JSONObject().put("op", "DRAW").toString()) }
                            } else null
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (state.pendingDraw > 0) "کشیدن ${state.pendingDraw} کارت" else "کشیدن کارت",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrightGold
                        )
                    }

                    // Active Top Discard Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayingCardView(cardId = topCard, width = 68.dp, height = 98.dp, isHighlighted = true)
                        Spacer(Modifier.height(4.dp))
                        Text("کارت روی زمین", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }

                Text("۷: +۲ کارت | ۸: جایزه | ۱۰: برگشت | تک: پرش | سرباز: تغییر خال", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
            }
        }

        if (state.awaitingSuitPick && canAct && state.currentTurn == seat) {
            SuitPickerBar(
                titleFa = "سرباز بازی کردید! خال بعدی را انتخاب کنید:",
                onPickSuit = { s -> onAction(JSONObject().put("op", "PICK_SUIT").put("suit", s).toString()) }
            )
        }

        val myHand = state.hands.getOrElse(seat) { emptyList() }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
        ) {
            items(myHand, key = { it }) { cardId ->
                val isOk = canAct && cardId in playable
                PlayingCardView(
                    cardId = cardId,
                    width = 60.dp,
                    height = 88.dp,
                    isPlayable = isOk,
                    isHighlighted = isOk,
                    onClick = if (isOk) {
                        { onAction(JSONObject().put("op", "PLAY_CARD").put("card", cardId).toString()) }
                    } else null
                )
            }
        }

        StatusBannerCard(state.statusFa, RubyCrimson)
    }
}
