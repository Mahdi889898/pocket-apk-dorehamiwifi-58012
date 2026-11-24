package com.example.ui.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.BackgammonEngine
import com.example.games.BackgammonState
import com.example.games.MenchClassicEngine
import com.example.games.MenchClassicState
import com.example.games.PachisiEngine
import com.example.games.PachisiState
import com.example.games.SnakesLaddersEngine
import com.example.games.SnakesLaddersState
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepAmber
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.IvoryTile
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PlayerDarkColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.WalnutDark
import com.example.ui.theme.WalnutWood
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// Classic Vintage Iranian Mench colors (Red قرمز, Blue آبی, Gold/Yellow زرد, Green سبز)
private val VINTAGE_MENCH_COLORS = listOf(
    Color(0xFFD62839), // Red
    Color(0xFF1D6FD6), // Royal Blue
    Color(0xFFE5A910), // Classic Mustard Gold
    Color(0xFF1A936F)  // Vintage Emerald Green
)
private val VINTAGE_MENCH_DARK = listOf(
    Color(0xFF7A0D19),
    Color(0xFF0D3670),
    Color(0xFF8C5F04),
    Color(0xFF0A4D38)
)

// ============================================================================
// 1. OLD-SCHOOL VINTAGE IRANIAN MENCH 3D (منچ قدیم ایرانی ۴۰ خانه‌ای سه‌بعدی)
// Authentic 11x11 cross layout with 40 circular holes + 4x4 corner yards + 4x4 home holes
// ============================================================================
private val MENCH_40_PATH: List<Pair<Int, Int>> = listOf(
    // Seat 0 (Red - Left arm top row going right -> top arm going up)
    4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4,
    3 to 4, 2 to 4, 1 to 4, 0 to 4, 0 to 5,
    // Seat 1 (Blue - Top arm right col going down -> right arm going right)
    0 to 6, 1 to 6, 2 to 6, 3 to 6, 4 to 6,
    4 to 7, 4 to 8, 4 to 9, 4 to 10, 5 to 10,
    // Seat 2 (Yellow - Right arm bottom row going left -> bottom arm going down)
    6 to 10, 6 to 9, 6 to 8, 6 to 7, 6 to 6,
    7 to 6, 8 to 6, 9 to 6, 10 to 6, 10 to 5,
    // Seat 3 (Green - Bottom arm left col going up -> left arm going left)
    10 to 4, 9 to 4, 8 to 4, 7 to 4, 6 to 4,
    6 to 3, 6 to 2, 6 to 1, 6 to 0, 5 to 0
)

private val MENCH_HOME_HOLES: List<List<Pair<Int, Int>>> = listOf(
    listOf(5 to 1, 5 to 2, 5 to 3, 5 to 4), // Red
    listOf(1 to 5, 2 to 5, 3 to 5, 4 to 5), // Blue
    listOf(5 to 9, 5 to 8, 5 to 7, 5 to 6), // Yellow
    listOf(9 to 5, 8 to 5, 7 to 5, 6 to 5)  // Green
)

private val MENCH_CORNER_YARDS: List<List<Pair<Float, Float>>> = listOf(
    listOf(0.8f to 0.8f, 0.8f to 2.15f, 2.15f to 0.8f, 2.15f to 2.15f),       // Red Top-Left
    listOf(0.8f to 8.85f, 0.8f to 10.2f, 2.15f to 8.85f, 2.15f to 10.2f),     // Blue Top-Right
    listOf(8.85f to 8.85f, 8.85f to 10.2f, 10.2f to 8.85f, 10.2f to 10.2f),   // Yellow Bottom-Right
    listOf(8.85f to 0.8f, 8.85f to 2.15f, 10.2f to 0.8f, 10.2f to 2.15f)      // Green Bottom-Left
)

private fun menchPawnGridCoord(playerIdx: Int, playerCount: Int, pawnIdx: Int, progress: Int): Pair<Float, Float> {
    val seat = MenchClassicEngine.seatForPlayer(playerIdx, playerCount)
    return when {
        progress == -1 -> MENCH_CORNER_YARDS[seat][pawnIdx]
        progress in 0..39 -> {
            val g = MenchClassicEngine.globalHole(playerIdx, playerCount, progress) ?: 0
            val (r, c) = MENCH_40_PATH[g]
            (r + 0.5f) to (c + 0.5f)
        }
        else -> {
            val idx = (progress - 40).coerceIn(0, 3)
            val (r, c) = MENCH_HOME_HOLES[seat][idx]
            (r + 0.5f) to (c + 0.5f)
        }
    }
}

@Composable
fun MenchClassicBoard(
    state: MenchClassicState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val movable = MenchClassicEngine.movablePawns(state)
    val seatColor = VINTAGE_MENCH_COLORS[MenchClassicEngine.seatForPlayer(state.currentTurn, state.playerCount)]
    val pulseTransition = rememberInfiniteTransition(label = "menchPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pawnHalo"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.pawns.map { list -> "${list.count { it in 40..43 }}/4 خانه" }
        )

        Board3DContainer(
            is3D = is3D,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            slabTopColor = WalnutWood,
            slabSideColor = Color(0xFF28150A),
            innerGlow = RoyalGold
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, canAct) {
                        detectTapGestures { tap ->
                            if (!canAct || !state.hasRolled || movable.isEmpty()) return@detectTapGestures
                            val pad = size.width * 0.055f
                            val cell = (min(size.width, size.height) - pad * 2f) / 11f
                            var bestPawn: Int? = null
                            var bestDist = cell * 1.45f
                            for (pIdx in movable) {
                                val (gr, gc) = menchPawnGridCoord(
                                    state.currentTurn,
                                    state.playerCount,
                                    pIdx,
                                    state.pawns[state.currentTurn][pIdx]
                                )
                                val cx = pad + gc * cell
                                val cy = pad + gr * cell
                                val d = hypot(tap.x - cx, tap.y - cy)
                                if (d < bestDist) {
                                    bestDist = d
                                    bestPawn = pIdx
                                }
                            }
                            if (bestPawn != null) {
                                onAction(JSONObject().put("op", "MOVE").put("pawn", bestPawn).toString())
                            }
                        }
                    }
                    .testTag("mench_classic_board_canvas")
            ) {
                val w = size.width
                val h = size.height
                val pad = w * 0.055f
                val cell = (min(w, h) - pad * 2f) / 11f

                // 1. Aged Vintage Iranian Paper / Parchment Background
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDF5E2), Color(0xFFF3E2BC), Color(0xFFE2C792)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.78f
                    )
                )

                // Vintage Double Persian Filigree Border Frame
                drawRoundRect(
                    color = Color(0xFF7B241C).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.018f, h * 0.018f),
                    size = Size(w * 0.964f, h * 0.964f),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 4.5f)
                )
                drawRoundRect(
                    color = Color(0xFF9A6B22).copy(alpha = 0.7f),
                    topLeft = Offset(w * 0.032f, h * 0.032f),
                    size = Size(w * 0.936f, h * 0.936f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 2f)
                )

                // Connecting Vintage Track Line behind the 40 circles
                val trackPath = Path().apply {
                    MENCH_40_PATH.forEachIndexed { idx, (r, c) ->
                        val p = Offset(pad + (c + 0.5f) * cell, pad + (r + 0.5f) * cell)
                        if (idx == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(trackPath, color = Color(0xFF5D4037).copy(alpha = 0.55f), style = Stroke(width = 3.2f))

                // 2. Classic Corner Yards (4 nostalgic framed boxes with 4 circular recesses each)
                val yardBoxes = listOf(
                    Triple(0.15f, 0.15f, 0),
                    Triple(0.15f, 8.20f, 1),
                    Triple(8.20f, 8.20f, 2),
                    Triple(8.20f, 0.15f, 3)
                )
                yardBoxes.forEach { (yr, yc, seat) ->
                    val col = VINTAGE_MENCH_COLORS[seat]
                    val darkCol = VINTAGE_MENCH_DARK[seat]
                    drawRoundRect(
                        color = col.copy(alpha = 0.14f),
                        topLeft = Offset(pad + yc * cell, pad + yr * cell),
                        size = Size(2.65f * cell, 2.65f * cell),
                        cornerRadius = CornerRadius(cell * 0.45f, cell * 0.45f)
                    )
                    drawRoundRect(
                        color = col.copy(alpha = 0.8f),
                        topLeft = Offset(pad + yc * cell, pad + yr * cell),
                        size = Size(2.65f * cell, 2.65f * cell),
                        cornerRadius = CornerRadius(cell * 0.45f, cell * 0.45f),
                        style = Stroke(width = 3f)
                    )
                    MENCH_CORNER_YARDS[seat].forEach { (sr, sc) ->
                        val center = Offset(pad + sc * cell, pad + sr * cell)
                        drawCircle(Color.Black.copy(alpha = 0.16f), cell * 0.42f, center + Offset(1.5f, 2f))
                        drawCircle(col, cell * 0.40f, center)
                        drawCircle(Color.White.copy(alpha = 0.85f), cell * 0.30f, center, style = Stroke(width = 2f))
                        drawCircle(darkCol, cell * 0.40f, center, style = Stroke(width = 2.2f))
                    }
                }

                // 3. Draw 40 Circular Old-School Stepping Holes
                val holeR = cell * 0.39f
                MENCH_40_PATH.forEachIndexed { idx, (r, c) ->
                    val center = Offset(pad + (c + 0.5f) * cell, pad + (r + 0.5f) * cell)
                    val startSeat = MenchClassicEngine.START_OFFSETS.indexOf(idx)
                    // Recessed shadow
                    drawCircle(Color.Black.copy(alpha = 0.18f), holeR, center + Offset(1.5f, 2.2f))
                    if (startSeat >= 0) {
                        val sCol = VINTAGE_MENCH_COLORS[startSeat]
                        drawCircle(sCol, holeR, center)
                        drawCircle(Color(0xFFFFF8E7), holeR * 0.62f, center, style = Stroke(width = 2.2f))
                        drawCircle(VINTAGE_MENCH_DARK[startSeat], holeR, center, style = Stroke(width = 2.5f))
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFFDF7), Color(0xFFEDE0C2)),
                                center = center - Offset(holeR * 0.25f, holeR * 0.25f),
                                radius = holeR * 1.3f
                            ),
                            radius = holeR,
                            center = center
                        )
                        drawCircle(Color(0xFF5D4037), holeR, center, style = Stroke(width = 2.2f))
                    }
                }

                // 4. Draw 4x4 Colored Home Destination Circles (1..4)
                val homePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = cell * 0.34f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    color = android.graphics.Color.WHITE
                }
                MENCH_HOME_HOLES.forEachIndexed { seat, holes ->
                    val col = VINTAGE_MENCH_COLORS[seat]
                    val dark = VINTAGE_MENCH_DARK[seat]
                    holes.forEachIndexed { hIdx, (r, c) ->
                        val center = Offset(pad + (c + 0.5f) * cell, pad + (r + 0.5f) * cell)
                        drawCircle(Color.Black.copy(alpha = 0.18f), holeR * 0.94f, center + Offset(1.5f, 2f))
                        drawCircle(col.copy(alpha = 0.88f), holeR * 0.92f, center)
                        drawCircle(dark, holeR * 0.92f, center, style = Stroke(width = 2.2f))
                        drawContext.canvas.nativeCanvas.drawText(
                            (hIdx + 1).toString(),
                            center.x,
                            center.y + cell * 0.12f,
                            homePaint
                        )
                    }
                }

                // 5. Central Vintage Persian Shamsa Star Medallion
                val mid = Offset(pad + 5.5f * cell, pad + 5.5f * cell)
                drawCircle(Color(0xFF145369), cell * 0.48f, mid)
                drawCircle(RoyalGold, cell * 0.48f, mid, style = Stroke(width = 3f))
                val starPath = Path().apply {
                    for (i in 0 until 16) {
                        val a = i * (2.0 * PI / 16.0)
                        val r = if (i % 2 == 0) cell * 0.38f else cell * 0.18f
                        val px = mid.x + r * cos(a).toFloat()
                        val py = mid.y + r * sin(a).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(starPath, BrightGold)

                // 6. Draw Sculpted 3D Standing Pawns (sorted top-to-bottom by Y for true 3D occlusion!)
                data class PawnRenderItem(
                    val pIdx: Int,
                    val kIdx: Int,
                    val seat: Int,
                    val pos: Offset,
                    val selectable: Boolean
                )
                val renderList = mutableListOf<PawnRenderItem>()
                for (p in 0 until state.playerCount) {
                    val seat = MenchClassicEngine.seatForPlayer(p, state.playerCount)
                    for (k in 0 until 4) {
                        val (gr, gc) = menchPawnGridCoord(p, state.playerCount, k, state.pawns[p][k])
                        renderList.add(
                            PawnRenderItem(
                                pIdx = p,
                                kIdx = k,
                                seat = seat,
                                pos = Offset(pad + gc * cell, pad + gr * cell),
                                selectable = canAct && p == state.currentTurn && k in movable
                            )
                        )
                    }
                }
                renderList.sortedBy { it.pos.y }.forEach { item ->
                    drawSculpted3DPawn(
                        baseCenter = item.pos,
                        radius = cell * 0.36f,
                        mainColor = VINTAGE_MENCH_COLORS[item.seat],
                        darkColor = VINTAGE_MENCH_DARK[item.seat],
                        isSelectedGlow = item.selectable,
                        pulseScale = pulse,
                        is3DMode = is3D
                    )
                }
            }
        }

        // Dice & Quick Pawn Picker
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Animated3DDice(
                value = state.diceValue,
                rollTrigger = state.rollCount,
                enabled = canAct && !state.hasRolled && state.winnerIndex == null,
                accentColor = seatColor,
                onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                StatusBannerCard(text = state.statusFa, accentColor = seatColor)
                if (canAct && state.hasRolled && movable.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        movable.forEach { pIdx ->
                            Button(
                                onClick = { onAction(JSONObject().put("op", "MOVE").put("pawn", pIdx).toString()) },
                                colors = ButtonDefaults.buttonColors(containerColor = seatColor, contentColor = Color.White),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("مهره ${pIdx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. UPGRADED 3D SNAKES & LADDERS (مار و پله سه‌بعدی با مارهای پرجزئیات و نردبان چوبی)
// ============================================================================
private fun snakesCellCenter(num: Int, cell: Float): Offset {
    val idx = (num - 1).coerceIn(0, 99)
    val rowFromBottom = idx / 10
    val colInRow = idx % 10
    val c = if (rowFromBottom % 2 == 0) colInRow else (9 - colInRow)
    val r = 9 - rowFromBottom
    return Offset((c + 0.5f) * cell, (r + 0.5f) * cell)
}

@Composable
fun SnakesLadders3DBoard(
    state: SnakesLaddersState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val activeColor = VINTAGE_MENCH_COLORS[(players.getOrNull(state.currentTurn)?.colorIndex ?: state.currentTurn) % 4]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.positions.map { "خانه $it" }
        )

        Board3DContainer(
            is3D = is3D,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            slabTopColor = Color(0xFF0E5341),
            slabSideColor = Color(0xFF062A20),
            innerGlow = EmeraldFelt
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize().testTag("snakes_ladders_canvas")
            ) {
                val cell = min(size.width, size.height) / 10f
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = cell * 0.29f
                    textAlign = android.graphics.Paint.Align.LEFT
                    isFakeBoldText = true
                }

                // Rich Vintage Illustrated Alternating Tiles (1..100)
                val palette = listOf(
                    Color(0xFFFBF3DF), // Warm Cream
                    Color(0xFFE7D4AC), // Antique Sand
                    Color(0xFFD6EADF), // Soft Mint Parchment
                    Color(0xFFF3D5C0)  // Warm Terracotta Tint
                )
                for (num in 1..100) {
                    val center = snakesCellCenter(num, cell)
                    val tl = Offset(center.x - cell / 2f, center.y - cell / 2f)
                    val r = (tl.y / cell).toInt()
                    val c = (tl.x / cell).toInt()
                    val bg = when (num) {
                        100 -> Color(0xFFF5B841)
                        1 -> Color(0xFF10B981)
                        else -> palette[(r * 2 + c) % palette.size]
                    }
                    drawRect(bg, tl, Size(cell, cell))
                    drawRect(Color(0xFF6B4F2B).copy(alpha = 0.35f), tl, Size(cell, cell), style = Stroke(1.3f))
                    // Subtle top specular bevel
                    drawLine(Color.White.copy(alpha = 0.35f), tl + Offset(2f, 2f), tl + Offset(cell - 2f, 2f), strokeWidth = 1.5f)
                    textPaint.color = if (num == 100 || num == 1) android.graphics.Color.rgb(20, 15, 5)
                    else android.graphics.Color.rgb(66, 45, 22)
                    drawContext.canvas.nativeCanvas.drawText(
                        if (num == 100) "👑100" else num.toString(),
                        tl.x + cell * 0.08f,
                        tl.y + cell * 0.33f,
                        textPaint
                    )
                }

                // High-Graphics 3D Wooden Rope Ladders with Ground Shadows
                SnakesLaddersEngine.LADDERS.forEach { (start, end) ->
                    val p1 = snakesCellCenter(start, cell)
                    val p2 = snakesCellCenter(end, cell)
                    val dx = p2.x - p1.x; val dy = p2.y - p1.y
                    val len = hypot(dx, dy).coerceAtLeast(1f)
                    val nx = -dy / len * (cell * 0.19f)
                    val ny = dx / len * (cell * 0.19f)
                    val shadowOff = Offset(cell * 0.11f, cell * 0.14f)

                    // Ground shadow
                    drawLine(Color.Black.copy(alpha = 0.28f), p1 + Offset(nx, ny) + shadowOff, p2 + Offset(nx, ny) + shadowOff, 7.5f, StrokeCap.Round)
                    drawLine(Color.Black.copy(alpha = 0.28f), p1 - Offset(nx, ny) + shadowOff, p2 - Offset(nx, ny) + shadowOff, 7.5f, StrokeCap.Round)

                    // Rungs first so rails overlap cleanly
                    val rungs = (len / (cell * 0.38f)).toInt().coerceIn(4, 13)
                    for (i in 1 until rungs) {
                        val t = i.toFloat() / rungs
                        val mid = Offset(p1.x + dx * t, p1.y + dy * t)
                        drawLine(Color(0xFF4E2A12), mid + Offset(nx, ny) + Offset(0f, 2f), mid - Offset(nx, ny) + Offset(0f, 2f), 5f, StrokeCap.Round)
                        drawLine(Color(0xFFD99B4E), mid + Offset(nx, ny), mid - Offset(nx, ny), 3.8f, StrokeCap.Round)
                    }
                    // Carved Walnut Wooden Rails + warm highlight
                    drawLine(Color(0xFF4A2511), p1 + Offset(nx, ny), p2 + Offset(nx, ny), 8f, StrokeCap.Round)
                    drawLine(Color(0xFF9C5D29), p1 + Offset(nx, ny), p2 + Offset(nx, ny), 4.2f, StrokeCap.Round)
                    drawLine(Color(0xFF4A2511), p1 - Offset(nx, ny), p2 - Offset(nx, ny), 8f, StrokeCap.Round)
                    drawLine(Color(0xFFB57234), p1 - Offset(nx, ny), p2 - Offset(nx, ny), 4.2f, StrokeCap.Round)
                }

                // High-Graphics Coiled Snakes with Belly Shadows, Scales, Golden Eyes & Forked Tongues
                val snakeStyles = listOf(
                    Triple(Color(0xFFC81D3A), Color(0xFF5F0A1B), Color(0xFFFFD166)),
                    Triple(Color(0xFF7C3AED), Color(0xFF3B0764), Color(0xFF67E8F9)),
                    Triple(Color(0xFF059669), Color(0xFF064E3B), Color(0xFFFDE047)),
                    Triple(Color(0xFFEA580C), Color(0xFF7C2D12), Color(0xFFFEF08A))
                )
                SnakesLaddersEngine.SNAKES.entries.forEachIndexed { sIdx, (head, tail) ->
                    val pHead = snakesCellCenter(head, cell)
                    val pTail = snakesCellCenter(tail, cell)
                    val (bodyCol, darkCol, spotCol) = snakeStyles[sIdx % snakeStyles.size]
                    val bend = cell * 0.95f * if (sIdx % 2 == 0) 1f else -1f
                    val c1 = Offset(pHead.x + (pTail.x - pHead.x) * 0.30f + bend, pHead.y + (pTail.y - pHead.y) * 0.30f)
                    val c2 = Offset(pHead.x + (pTail.x - pHead.x) * 0.70f - bend, pHead.y + (pTail.y - pHead.y) * 0.70f)

                    val snakePath = Path().apply {
                        moveTo(pHead.x, pHead.y)
                        cubicTo(c1.x, c1.y, c2.x, c2.y, pTail.x, pTail.y)
                    }
                    val shadowPath = Path().apply {
                        moveTo(pHead.x + 5f, pHead.y + 7f)
                        cubicTo(c1.x + 5f, c1.y + 7f, c2.x + 5f, c2.y + 7f, pTail.x + 5f, pTail.y + 7f)
                    }
                    // Ground shadow + dark border + glossy body + scale spots along curve
                    drawPath(shadowPath, Color.Black.copy(alpha = 0.28f), style = Stroke(cell * 0.30f, cap = StrokeCap.Round))
                    drawPath(snakePath, darkCol, style = Stroke(cell * 0.28f, cap = StrokeCap.Round))
                    drawPath(snakePath, bodyCol, style = Stroke(cell * 0.20f, cap = StrokeCap.Round))

                    // Sample cubic Bézier to draw diamond scale dots
                    for (step in 2..10) {
                        val t = step / 12f
                        val mt = 1f - t
                        val bx = mt * mt * mt * pHead.x + 3 * mt * mt * t * c1.x + 3 * mt * t * t * c2.x + t * t * t * pTail.x
                        val by = mt * mt * mt * pHead.y + 3 * mt * mt * t * c1.y + 3 * mt * t * t * c2.y + t * t * t * pTail.y
                        drawCircle(spotCol.copy(alpha = 0.85f), radius = cell * 0.055f, center = Offset(bx, by))
                    }

                    // Red forked tongue
                    drawLine(Color(0xFFEF4444), pHead + Offset(0f, -cell * 0.22f), pHead + Offset(0f, -cell * 0.42f), 2.5f)
                    drawLine(Color(0xFFEF4444), pHead + Offset(0f, -cell * 0.38f), pHead + Offset(-cell * 0.09f, -cell * 0.48f), 2f)
                    drawLine(Color(0xFFEF4444), pHead + Offset(0f, -cell * 0.38f), pHead + Offset(cell * 0.09f, -cell * 0.48f), 2f)

                    // Sculpted Viper Head + Glowing Golden Eyes
                    drawOval(darkCol, Offset(pHead.x - cell * 0.28f, pHead.y - cell * 0.24f), Size(cell * 0.56f, cell * 0.46f))
                    drawOval(bodyCol, Offset(pHead.x - cell * 0.24f, pHead.y - cell * 0.20f), Size(cell * 0.48f, cell * 0.38f))
                    drawCircle(Color(0xFFFFE566), cell * 0.075f, pHead + Offset(-cell * 0.09f, -cell * 0.04f))
                    drawCircle(Color(0xFFFFE566), cell * 0.075f, pHead + Offset(cell * 0.09f, -cell * 0.04f))
                    drawCircle(Color.Black, cell * 0.036f, pHead + Offset(-cell * 0.09f, -cell * 0.04f))
                    drawCircle(Color.Black, cell * 0.036f, pHead + Offset(cell * 0.09f, -cell * 0.04f))
                }

                // Draw Sculpted 3D Standing Pawns sorted by Y
                val pawnItems = state.positions.mapIndexed { idx, pos ->
                    val base = snakesCellCenter(pos, cell)
                    val jitter = when (idx) {
                        0 -> Offset(-cell * 0.16f, -cell * 0.05f)
                        1 -> Offset(cell * 0.16f, -cell * 0.05f)
                        2 -> Offset(-cell * 0.16f, cell * 0.16f)
                        else -> Offset(cell * 0.16f, cell * 0.16f)
                    }
                    Triple(idx, base + jitter, (players.getOrNull(idx)?.colorIndex ?: idx) % 4)
                }.sortedBy { it.second.y }

                pawnItems.forEach { (idx, pos, colIdx) ->
                    drawSculpted3DPawn(
                        baseCenter = pos,
                        radius = cell * 0.30f,
                        mainColor = VINTAGE_MENCH_COLORS[colIdx],
                        darkColor = VINTAGE_MENCH_DARK[colIdx],
                        isSelectedGlow = idx == state.currentTurn && state.winnerIndex == null,
                        is3DMode = is3D
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Animated3DDice(
                value = state.diceValue,
                rollTrigger = state.rollCount,
                enabled = canAct && state.winnerIndex == null,
                accentColor = activeColor,
                onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) }
            )
            StatusBannerCard(text = state.statusFa, accentColor = activeColor, modifier = Modifier.weight(1f))
        }
    }
}

// ============================================================================
// 3. ROYAL PACHISI 3D (پچسی سلطنتی ۵۲ خانه‌ای با ۸ خانه امن ستاره‌دار)
// ============================================================================
private val PACHISI_TRACK: List<Pair<Int, Int>> = listOf(
    6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
    5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
    0 to 7, 0 to 8,
    1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
    6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
    7 to 14, 8 to 14,
    8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
    9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
    14 to 7, 14 to 6,
    13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
    8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
    7 to 0, 6 to 0
)
private val PACHISI_HOMES = listOf(
    listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
    listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
    listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
    listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7)
)
private val PACHISI_YARDS = listOf(
    listOf(1.9f to 1.9f, 1.9f to 4.1f, 4.1f to 1.9f, 4.1f to 4.1f),
    listOf(1.9f to 10.9f, 1.9f to 13.1f, 4.1f to 10.9f, 4.1f to 13.1f),
    listOf(10.9f to 10.9f, 10.9f to 13.1f, 13.1f to 10.9f, 13.1f to 13.1f),
    listOf(10.9f to 1.9f, 10.9f to 4.1f, 13.1f to 1.9f, 13.1f to 4.1f)
)

private fun pachisiCoord(playerIdx: Int, playerCount: Int, pawnIdx: Int, prog: Int): Pair<Float, Float> {
    val seat = PachisiEngine.seatForPlayer(playerIdx, playerCount)
    return when {
        prog == -1 -> PACHISI_YARDS[seat][pawnIdx]
        prog in 0..50 -> {
            val g = PachisiEngine.globalTile(playerIdx, playerCount, prog) ?: 0
            val (r, c) = PACHISI_TRACK[g]
            (r + 0.5f) to (c + 0.5f)
        }
        prog in 51..55 -> {
            val (r, c) = PACHISI_HOMES[seat][prog - 51]
            (r + 0.5f) to (c + 0.5f)
        }
        else -> 7.5f to 7.5f
    }
}

@Composable
fun Pachisi3DBoard(
    state: PachisiState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val movable = PachisiEngine.movablePawns(state)
    val seatColor = VINTAGE_MENCH_COLORS[PachisiEngine.seatForPlayer(state.currentTurn, state.playerCount)]
    val pulse by rememberInfiniteTransition(label = "pachisiPulse").animateFloat(
        initialValue = 0.85f, targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "halo"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }
        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.pawns.map { list -> "${list.count { it == 56 }}/4 تاج" }
        )
        Board3DContainer(is3D = is3D, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(state, canAct) {
                    detectTapGestures { tap ->
                        if (!canAct || !state.hasRolled || movable.isEmpty()) return@detectTapGestures
                        val cell = min(size.width, size.height) / 15f
                        var best: Int? = null; var dist = cell * 1.5f
                        for (k in movable) {
                            val (gr, gc) = pachisiCoord(state.currentTurn, state.playerCount, k, state.pawns[state.currentTurn][k])
                            val d = hypot(tap.x - gc * cell, tap.y - gr * cell)
                            if (d < dist) { dist = d; best = k }
                        }
                        if (best != null) onAction(JSONObject().put("op", "MOVE").put("pawn", best).toString())
                    }
                }
            ) {
                val cell = min(size.width, size.height) / 15f
                drawRect(Color(0xFFF6EBD2))
                listOf(Triple(0, 0, 0), Triple(0, 9, 1), Triple(9, 9, 2), Triple(9, 0, 3)).forEach { (r, c, s) ->
                    val col = VINTAGE_MENCH_COLORS[s]
                    drawRect(col, Offset(c * cell, r * cell), Size(6 * cell, 6 * cell))
                    drawRoundRect(Color(0xFF17273A), Offset((c + 0.7f) * cell, (r + 0.7f) * cell), Size(4.6f * cell, 4.6f * cell), CornerRadius(cell * 0.5f, cell * 0.5f))
                    PACHISI_YARDS[s].forEach { (yr, yc) ->
                        drawCircle(col.copy(alpha = 0.25f), cell * 0.58f, Offset(yc * cell, yr * cell))
                        drawCircle(col, cell * 0.58f, Offset(yc * cell, yr * cell), style = Stroke(2.5f))
                    }
                }
                PACHISI_TRACK.forEachIndexed { idx, (r, c) ->
                    val startSeat = PachisiEngine.START_OFFSETS.indexOf(idx)
                    val safe = idx in PachisiEngine.SAFE_TILES
                    drawRect(if (startSeat >= 0) VINTAGE_MENCH_COLORS[startSeat].copy(alpha = 0.85f) else if (safe) Color(0xFFE7D3A3) else IvoryTile, Offset(c * cell, r * cell), Size(cell, cell))
                    drawRect(Color(0xFF8C7355), Offset(c * cell, r * cell), Size(cell, cell), style = Stroke(1.2f))
                    if (safe) {
                        drawCircle(if (startSeat >= 0) Color.White else RoyalGold, cell * 0.24f, Offset((c + 0.5f) * cell, (r + 0.5f) * cell))
                    }
                }
                PACHISI_HOMES.forEachIndexed { s, lane ->
                    lane.forEach { (r, c) ->
                        drawRect(VINTAGE_MENCH_COLORS[s].copy(alpha = 0.82f), Offset(c * cell, r * cell), Size(cell, cell))
                        drawRect(Color.White.copy(alpha = 0.5f), Offset(c * cell, r * cell), Size(cell, cell), style = Stroke(1.2f))
                    }
                }
                // Center Crown
                drawRect(RoyalGold, Offset(6 * cell, 6 * cell), Size(3 * cell, 3 * cell))
                drawCircle(Color(0xFF102030), cell * 0.9f, Offset(7.5f * cell, 7.5f * cell))
                drawCircle(BrightGold, cell * 0.6f, Offset(7.5f * cell, 7.5f * cell))

                // Sculpted 3D Pawns
                val items = buildList {
                    for (p in 0 until state.playerCount) {
                        val seat = PachisiEngine.seatForPlayer(p, state.playerCount)
                        for (k in 0 until 4) {
                            val (gr, gc) = pachisiCoord(p, state.playerCount, k, state.pawns[p][k])
                            add(Triple(seat, Offset(gc * cell, gr * cell), canAct && p == state.currentTurn && k in movable))
                        }
                    }
                }.sortedBy { it.second.y }
                items.forEach { (seat, pos, sel) ->
                    drawSculpted3DPawn(pos, cell * 0.35f, VINTAGE_MENCH_COLORS[seat], VINTAGE_MENCH_DARK[seat], sel, pulse, is3D)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Animated3DDice(state.diceValue, state.rollCount, canAct && !state.hasRolled && state.winnerIndex == null, seatColor, onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) })
            StatusBannerCard(state.statusFa, seatColor, Modifier.weight(1f))
        }
    }
}

// ============================================================================
// 4. PERSIAN BACKGAMMON 3D (تخته نرد ایرانی چوب گردو سه‌بعدی)
// ============================================================================
@Composable
fun Backgammon3DBoard(
    state: BackgammonState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    var is3D by remember { mutableStateOf(true) }
    val legal = BackgammonEngine.legalMoves(state)
    val destinationsForSelected = state.selectedFrom?.let { sel ->
        legal.filter { it.from == sel }.map { it.to }.toSet()
    } ?: emptySet()
    val movableSources = legal.map { it.from }.toSet()
    val activeColor = if (state.currentTurn == 0) BrightGold else Color(0xFFF43F5E)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            View3DToggleChip(is3D = is3D, onToggle = { is3D = !is3D })
        }

        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("خارج شده: ${state.offP0}/15", "خارج شده: ${state.offP1}/15")
        )

        Board3DContainer(
            is3D = is3D,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.18f),
            slabTopColor = WalnutWood,
            slabSideColor = Color(0xFF221107),
            innerGlow = RoyalGold
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(state, canAct) {
                    detectTapGestures { tap ->
                        if (!canAct || state.remainingMoves.isEmpty() || state.winnerIndex != null) return@detectTapGestures
                        val w = size.width; val h = size.height
                        val barW = w * 0.08f
                        val colW = (w - barW) / 12f
                        val isTop = tap.y < h / 2f

                        // Check Bar click
                        val barLeft = 6f * colW
                        if (tap.x in barLeft..(barLeft + barW)) {
                            onAction(JSONObject().put("op", "TAP_POINT").put("pt", -1).toString())
                            return@detectTapGestures
                        }

                        // Map x to visual column 0..11
                        val visualCol = if (tap.x < barLeft) {
                            (tap.x / colW).toInt().coerceIn(0, 5)
                        } else {
                            (6 + ((tap.x - barLeft - barW) / colW).toInt()).coerceIn(6, 11)
                        }
                        // Top row: left-to-right = points 12..23; Bottom row: left-to-right = points 11..0
                        val pt = if (isTop) 12 + visualCol else 11 - visualCol
                        onAction(JSONObject().put("op", "TAP_POINT").put("pt", pt).toString())
                    }
                }
            ) {
                val w = size.width; val h = size.height
                val barW = w * 0.08f
                val colW = (w - barW) / 12f
                val barX = 6f * colW

                // Carved Walnut Inner Surface
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5A3720), Color(0xFF3E2414), Color(0xFF5A3720))))
                // Center Hinged Bar
                drawRect(Brush.horizontalGradient(listOf(WalnutDark, Color(0xFF4A2B18), WalnutDark)), Offset(barX, 0f), Size(barW, h))
                drawRect(RoyalGold.copy(alpha = 0.6f), Offset(barX, 0f), Size(barW, h), style = Stroke(2f))

                fun colLeftX(visualCol: Int): Float = if (visualCol < 6) visualCol * colW else barX + barW + (visualCol - 6) * colW

                // Draw 24 Inlaid Khatam Triangles + Checkers
                for (pt in 0 until 24) {
                    val isTop = pt >= 12
                    val vCol = if (isTop) pt - 12 else 11 - pt
                    val x0 = colLeftX(vCol)
                    val cx = x0 + colW / 2f
                    val triH = h * 0.42f
                    val triPath = Path().apply {
                        if (isTop) {
                            moveTo(x0 + 2f, 0f); lineTo(x0 + colW - 2f, 0f); lineTo(cx, triH); close()
                        } else {
                            moveTo(x0 + 2f, h); lineTo(x0 + colW - 2f, h); lineTo(cx, h - triH); close()
                        }
                    }
                    val isDest = pt in destinationsForSelected
                    val isSel = state.selectedFrom == pt
                    val triCol = when {
                        isDest -> EmeraldFelt.copy(alpha = 0.78f)
                        isSel -> RoyalGold.copy(alpha = 0.78f)
                        pt % 2 == 0 -> Color(0xFF8E2B22) // Mahogany Ruby
                        else -> Color(0xFFE7D3AE)        // Inlaid Ivory
                    }
                    drawPath(triPath, triCol)
                    drawPath(triPath, DeepAmber.copy(alpha = 0.45f), style = Stroke(1.3f))

                    // Draw stacked checkers on this point
                    val count = state.points[pt]
                    val absCnt = abs(count)
                    if (absCnt > 0) {
                        val isP0 = count > 0
                        val cLight = if (isP0) Color(0xFFFFFDF5) else Color(0xFFF43F5E)
                        val cDark = if (isP0) Color(0xFFCBB692) else Color(0xFF7F1D1D)
                        val r = min(colW * 0.44f, h * 0.068f)
                        val shown = absCnt.coerceAtMost(5)
                        for (i in 0 until shown) {
                            val cy = if (isTop) r * 1.15f + i * (r * 1.65f) else h - r * 1.15f - i * (r * 1.65f)
                            drawCircle(Color.Black.copy(alpha = 0.35f), r, Offset(cx + 2f, cy + 3f))
                            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), cLight, cDark), Offset(cx - r * 0.3f, cy - r * 0.3f), r * 1.4f), r, Offset(cx, cy))
                            val outline = if (isSel && i == shown - 1) BrightGold else if (canAct && pt in movableSources && i == shown - 1) Color.White else Color.Black.copy(alpha = 0.3f)
                            drawCircle(outline, r, Offset(cx, cy), style = Stroke(if (isSel && i == shown - 1) 3.5f else 1.5f))
                        }
                        if (absCnt > 5) {
                            val p = android.graphics.Paint().apply {
                                isAntiAlias = true; textSize = r * 1.05f; isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                                color = if (isP0) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            }
                            val cy = if (isTop) r * 1.15f + 4 * (r * 1.65f) else h - r * 1.15f - 4 * (r * 1.65f)
                            drawContext.canvas.nativeCanvas.drawText(absCnt.toString(), cx, cy + r * 0.35f, p)
                        }
                    }
                }

                // Draw Bar Checkers if any hit!
                val barCx = barX + barW / 2f
                val barR = barW * 0.36f
                if (state.barP0 > 0) {
                    drawCircle(Color(0xFFFFFDF5), barR, Offset(barCx, h * 0.38f))
                    drawCircle(BrightGold, barR, Offset(barCx, h * 0.38f), style = Stroke(2.5f))
                }
                if (state.barP1 > 0) {
                    drawCircle(Color(0xFFF43F5E), barR, Offset(barCx, h * 0.62f))
                    drawCircle(BrightGold, barR, Offset(barCx, h * 0.62f), style = Stroke(2.5f))
                }
            }
        }

        // Bear-off button if selected checker can legally bear off
        val bearOffTarget = if (state.currentTurn == 0) 24 else -2
        if (canAct && bearOffTarget in destinationsForSelected) {
            Button(
                onClick = { onAction(JSONObject().put("op", "TAP_POINT").put("pt", bearOffTarget).toString()) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFelt, contentColor = MidnightBg),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("خارج کردن مهره انتخاب شده از صفحه ✨", fontWeight = FontWeight.ExtraBold)
            }
        }

        // Dual 3D Dice + Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val canRoll = canAct && state.remainingMoves.isEmpty() && state.winnerIndex == null
            Animated3DDice(state.dice.getOrElse(0) { 6 }, state.rollCount, canRoll, activeColor, 58.dp, onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) })
            Animated3DDice(state.dice.getOrElse(1) { 5 }, state.rollCount, canRoll, activeColor, 58.dp, onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) })
            StatusBannerCard(state.statusFa, activeColor, Modifier.weight(1f))
        }
    }
}
