package com.example.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.CardDeck
import com.example.games.CardSuit
import com.example.model.AvatarSymbols
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepAmber
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
import com.example.ui.theme.VelvetNavy
import com.example.ui.theme.WalnutDark
import com.example.ui.theme.WalnutWood

/**
 * Interactive 3D Perspective Board Wrapper with extruded wooden tabletop slab thickness.
 */
@Composable
fun Board3DContainer(
    is3D: Boolean,
    modifier: Modifier = Modifier,
    slabTopColor: Color = WalnutWood,
    slabSideColor: Color = Color(0xFF231309),
    innerGlow: Color = RoyalGold,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current.density
    val rotX by animateFloatAsState(
        targetValue = if (is3D) 28f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "boardRotX"
    )
    val scaleVal by animateFloatAsState(
        targetValue = if (is3D) 0.96f else 1f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "boardScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotX
                scaleX = scaleVal
                scaleY = scaleVal
                cameraDistance = 13f * density
            }
    ) {
        // 3D Extruded Wooden Front Thickness Slab (visible underneath when tilted!)
        if (is3D) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 11.dp)
                    .shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(slabTopColor, slabSideColor, Color(0xFF140A04))
                        )
                    )
                    .border(1.dp, DeepAmber.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            )
        }

        // Main Board Surface Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (is3D) 18.dp else 10.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = innerGlow,
                    spotColor = innerGlow
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(slabTopColor, WalnutDark)))
                .border(
                    width = 2.2.dp,
                    brush = Brush.linearGradient(
                        listOf(BrightGold, DeepAmber.copy(alpha = 0.55f), BrightGold)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VelvetNavy)
            ) {
                content()
            }
        }
    }
}

@Composable
fun View3DToggleChip(
    is3D: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.2.dp,
                color = if (is3D) BrightGold else NeonCyan.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .testTag("toggle_3d_view_chip"),
        color = if (is3D) RoyalGold.copy(alpha = 0.22f) else ElevatedSlate,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ViewInAr,
                contentDescription = "تغییر نمای سه‌بعدی",
                tint = if (is3D) BrightGold else NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (is3D) "نمای سه‌بعدی 3D فعال" else "نمای تخت 2D (تغییر به 3D)",
                style = MaterialTheme.typography.labelMedium,
                color = if (is3D) BrightGold else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Draws a sculpted 3D standing classical wooden/lacquered pawn with pedestal base,
 * tapered conical torso, collar ring, spherical head, specular highlights, and cast shadow.
 */
fun DrawScope.drawSculpted3DPawn(
    baseCenter: Offset,
    radius: Float,
    mainColor: Color,
    darkColor: Color,
    isSelectedGlow: Boolean = false,
    pulseScale: Float = 1f,
    is3DMode: Boolean = true
) {
    val liftY = if (isSelectedGlow) radius * 0.45f * (pulseScale - 0.85f) else 0f
    val anchor = baseCenter - Offset(0f, liftY)
    val pawnHeight = if (is3DMode) radius * 2.15f else radius * 1.45f
    val baseW = radius * 1.05f
    val baseH = radius * 0.58f

    // Ground selection pulse halo
    if (isSelectedGlow) {
        drawOval(
            color = BrightGold.copy(alpha = 0.45f),
            topLeft = Offset(baseCenter.x - baseW * 1.45f * pulseScale, baseCenter.y - baseH * 1.35f * pulseScale),
            size = Size(baseW * 2.9f * pulseScale, baseH * 2.7f * pulseScale)
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(baseCenter.x - baseW * 1.25f, baseCenter.y - baseH * 1.15f),
            size = Size(baseW * 2.5f, baseH * 2.3f),
            style = Stroke(width = 2.6f)
        )
    }

    // Cast ground shadow on board
    drawOval(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = Offset(baseCenter.x - baseW * 0.85f + radius * 0.35f, baseCenter.y - baseH * 0.55f + radius * 0.22f),
        size = Size(baseW * 2.0f, baseH * 1.4f)
    )

    // Pedestal base disc (dark bottom rim + glossy top)
    drawOval(
        color = darkColor,
        topLeft = Offset(anchor.x - baseW, anchor.y - baseH * 0.45f),
        size = Size(baseW * 2f, baseH * 1.2f)
    )
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), mainColor, darkColor),
            startX = anchor.x - baseW,
            endX = anchor.x + baseW
        ),
        topLeft = Offset(anchor.x - baseW * 0.94f, anchor.y - baseH * 0.62f),
        size = Size(baseW * 1.88f, baseH * 1.05f)
    )
    // Golden filigree base ring
    drawOval(
        color = BrightGold.copy(alpha = 0.72f),
        topLeft = Offset(anchor.x - baseW * 0.95f, anchor.y - baseH * 0.62f),
        size = Size(baseW * 1.9f, baseH * 1.05f),
        style = Stroke(width = 1.4f)
    )

    // Tapered hourglass / conical sculpted torso
    val neckY = anchor.y - pawnHeight * 0.68f
    val torsoPath = Path().apply {
        moveTo(anchor.x - baseW * 0.72f, anchor.y - baseH * 0.1f)
        cubicTo(
            anchor.x - baseW * 0.55f, anchor.y - pawnHeight * 0.32f,
            anchor.x - baseW * 0.34f, neckY + radius * 0.12f,
            anchor.x - baseW * 0.30f, neckY
        )
        lineTo(anchor.x + baseW * 0.30f, neckY)
        cubicTo(
            anchor.x + baseW * 0.34f, neckY + radius * 0.12f,
            anchor.x + baseW * 0.55f, anchor.y - pawnHeight * 0.32f,
            anchor.x + baseW * 0.72f, anchor.y - baseH * 0.1f
        )
        close()
    }
    drawPath(
        path = torsoPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                mainColor,
                darkColor,
                Color.Black.copy(alpha = 0.45f)
            ),
            startX = anchor.x - baseW * 0.75f,
            endX = anchor.x + baseW * 0.75f
        )
    )
    drawPath(torsoPath, Color.Black.copy(alpha = 0.25f), style = Stroke(width = 1.1f))

    // Collar Ring under head
    drawOval(
        color = BrightGold.copy(alpha = 0.9f),
        topLeft = Offset(anchor.x - baseW * 0.44f, neckY - radius * 0.15f),
        size = Size(baseW * 0.88f, radius * 0.32f)
    )

    // Glossy Spherical Pawn Head
    val headR = radius * 0.56f
    val headCenter = Offset(anchor.x, neckY - headR * 0.68f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), mainColor, darkColor),
            center = headCenter - Offset(headR * 0.34f, headR * 0.36f),
            radius = headR * 1.45f
        ),
        radius = headR,
        center = headCenter
    )
    // Specular highlight dot on sphere
    drawCircle(
        color = Color.White.copy(alpha = 0.82f),
        radius = headR * 0.22f,
        center = headCenter - Offset(headR * 0.32f, headR * 0.32f)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = headR,
        center = headCenter,
        style = Stroke(width = 1.2f)
    )
}

/**
 * High-graphics Persian/Classic Playing Card UI
 */
@Composable
fun PlayingCardView(
    cardId: Int,
    width: Dp = 62.dp,
    height: Dp = 92.dp,
    isPlayable: Boolean = true,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val suit = CardDeck.suitOf(cardId)
    val rankText = CardDeck.rankLabel(cardId)
    val suitColor = if (suit.isRed) Color(0xFFD91E36) else Color(0xFF151C28)
    val liftY by animateFloatAsState(
        targetValue = if (isHighlighted) -10f else 0f,
        animationSpec = tween(180),
        label = "cardLift"
    )

    Box(
        modifier = modifier
            .offset(y = liftY.dp)
            .width(width)
            .height(height)
            .shadow(
                elevation = if (isHighlighted) 12.dp else 6.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = if (isHighlighted) BrightGold else Color.Black,
                spotColor = if (isHighlighted) BrightGold else Color.Black
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isPlayable) listOf(Color(0xFFFFFEFA), Color(0xFFF3EBDC))
                    else listOf(Color(0xFFCBD2DB), Color(0xFFA8B3C1))
                )
            )
            .border(
                width = if (isHighlighted) 2.2.dp else 1.dp,
                color = if (isHighlighted) BrightGold else Color(0xFF8C7A5B).copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (onClick != null && isPlayable) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(4.dp)
    ) {
        // Subtle inner golden decorative border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.7.dp, DeepAmber.copy(alpha = 0.28f), RoundedCornerShape(7.dp))
                .padding(3.dp)
        ) {
            // Top-left index
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = rankText,
                    color = suitColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (width >= 58.dp) 14.sp else 11.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = suit.symbol,
                    color = suitColor,
                    fontSize = if (width >= 58.dp) 12.sp else 10.sp,
                    lineHeight = 12.sp
                )
            }

            // Center artwork emblem
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val r = CardDeck.rankOf(cardId)
                if (r in 11..13) {
                    Text(
                        text = when (r) {
                            13 -> "👑"
                            12 -> "👸"
                            else -> "🛡️"
                        },
                        fontSize = if (width >= 58.dp) 18.sp else 14.sp
                    )
                }
                Text(
                    text = suit.symbol,
                    color = suitColor,
                    fontSize = if (width >= 58.dp) (if (r in 11..13) 16.sp else 26.sp) else 18.sp
                )
            }

            // Bottom-right index
            Text(
                text = rankText,
                color = suitColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = if (width >= 58.dp) 11.sp else 9.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun CardBackView(
    width: Dp = 44.dp,
    height: Dp = 64.dp,
    countBadge: Int? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(6.dp, RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0E4B50), Color(0xFF0A2438))))
            .border(1.5.dp, BrightGold.copy(alpha = 0.85f), RoundedCornerShape(9.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, PersianTurquoise.copy(alpha = 0.65f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (countBadge != null) {
                Text(
                    text = countBadge.toString(),
                    color = BrightGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            } else {
                Text("❖", color = BrightGold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SuitPickerBar(
    titleFa: String,
    onPickSuit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.8.dp, BrightGold, RoundedCornerShape(18.dp)),
        color = VelvetNavy.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(titleFa, style = MaterialTheme.typography.titleSmall, color = BrightGold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardSuit.entries.forEach { suit ->
                    Button(
                        onClick = { onPickSuit(suit.ordinal) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pick_suit_${suit.ordinal}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (suit.isRed) Color(0xFF5A1424) else ElevatedSlate,
                            contentColor = if (suit.isRed) Color(0xFFFF9EB0) else Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(suit.symbol, fontSize = 18.sp)
                            Text(suit.nameFa, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScoreRibbon(
    players: List<PlayerInfo>,
    currentTurn: Int,
    scores: List<String>? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        players.forEachIndexed { idx, player ->
            val color = PlayerColors[player.colorIndex % PlayerColors.size]
            val isTurn = idx == currentTurn
            val infinite = rememberInfiniteTransition(label = "turnGlow")
            val pulse by infinite.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
                label = "pulse"
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = if (isTurn) 2.dp else 1.dp,
                        color = if (isTurn) color.copy(alpha = pulse) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                color = if (isTurn) color.copy(alpha = 0.18f) else ElevatedSlate.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(color, color.copy(alpha = 0.45f))))
                            .border(
                                width = if (player.isSpeaking) 2.dp else 1.dp,
                                color = if (player.isSpeaking) BrightGold else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(AvatarSymbols[player.avatarIndex % AvatarSymbols.size], fontSize = 13.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (player.isSpeaking) {
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Rounded.GraphicEq, contentDescription = "در حال صحبت", tint = BrightGold, modifier = Modifier.size(13.dp))
                            }
                        }
                        if (scores != null && idx < scores.size) {
                            Text(
                                text = scores[idx],
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else if (isTurn) {
                            Text("نوبت", style = MaterialTheme.typography.labelSmall, color = BrightGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Animated3DDice(
    value: Int,
    rollTrigger: Int,
    enabled: Boolean,
    accentColor: Color,
    size: Dp = 66.dp,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(rollTrigger) {
        if (rollTrigger > 0) {
            scale.animateTo(1.24f, tween(110, easing = FastOutSlowInEasing))
            rotation.animateTo(rotation.value + 360f, tween(300, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale.value)
            .rotate(rotation.value % 360f)
            .shadow(
                elevation = if (enabled) 16.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor,
                spotColor = accentColor
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onRoll)
            .testTag("dice_roll_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = if (enabled) listOf(Color(0xFFFFFDF7), Color(0xFFE7D3AF))
                    else listOf(Color(0xFFD1D5DB), Color(0xFF9CA3AF)),
                    start = Offset.Zero,
                    end = Offset(w, h)
                ),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(BrightGold, accentColor)),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
                style = Stroke(width = w * 0.065f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(w * 0.1f, h * 0.08f),
                size = Size(w * 0.8f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
            )
            val pipR = w * 0.095f
            val pipColor = if (value == 1 || value == 6) Color(0xFFDC2626) else Color(0xFF1E293B)
            val c1 = 0.27f; val c2 = 0.50f; val c3 = 0.73f
            val pips = when (value.coerceIn(1, 6)) {
                1 -> listOf(c2 to c2)
                2 -> listOf(c1 to c1, c3 to c3)
                3 -> listOf(c1 to c1, c2 to c2, c3 to c3)
                4 -> listOf(c1 to c1, c3 to c1, c1 to c3, c3 to c3)
                5 -> listOf(c1 to c1, c3 to c1, c2 to c2, c1 to c3, c3 to c3)
                else -> listOf(c1 to c1, c3 to c1, c1 to c2, c3 to c2, c1 to c3, c3 to c3)
            }
            pips.forEach { (px, py) ->
                drawCircle(Color.Black.copy(alpha = 0.18f), pipR, Offset(w * px + 1.5f, h * py + 2f))
                drawCircle(pipColor, pipR, Offset(w * px, h * py))
            }
        }
    }
}

@Composable
fun StatusBannerCard(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(14.dp)),
        color = ElevatedSlate.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun WinnerCelebrationBanner(
    winnerIndex: Int,
    players: List<PlayerInfo>,
    isTeamGame: Boolean = false,
    canRestart: Boolean,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val winnerLabel = when {
        winnerIndex == -1 -> "بازی مساوی شد!"
        isTeamGame -> if (winnerIndex == 0) "تیم اول (${players.getOrNull(0)?.name} و ${players.getOrNull(2)?.name})"
        else "تیم دوم (${players.getOrNull(1)?.name} و ${players.getOrNull(3)?.name})"
        else -> players.getOrNull(winnerIndex)?.name ?: "بازیکن ${winnerIndex + 1}"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(BrightGold, RoyalGold, BrightGold)),
                shape = RoundedCornerShape(22.dp)
            ),
        color = VelvetNavy.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = "جام قهرمانی", tint = BrightGold, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (winnerIndex == -1) "تساوی نفس‌گیر!" else "قهرمان: $winnerLabel",
                style = MaterialTheme.typography.headlineSmall,
                color = BrightGold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canRestart) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_again_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg)
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("بازی مجدد", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("back_to_lobby_button")
                ) {
                    Text("بازگشت", color = TextPrimary)
                }
            }
        }
    }
}
