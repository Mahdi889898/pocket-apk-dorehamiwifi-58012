package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.games.JsonHelpers.toNestedIntList
import com.example.games.JsonHelpers.toNestedJsonArray
import com.example.model.GameType
import org.json.JSONObject

// ============================================================================
// 7. CHAHAR BARG / PASOOR 11 (چهاربرگ اصیل ایرانی)
// ============================================================================
data class ChaharBargState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val deck: List<Int>,
    val hands: List<List<Int>>, // 2 players
    val tableCards: List<Int>,
    val collected: List<List<Int>>, // captured cards per player
    val surCounts: List<Int>, // Sur (+5 pts each) per player
    val lastCapturer: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.CHAHAR_BARG

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("deck", deck.toJsonArray())
        put("hands", hands.toNestedJsonArray())
        put("tableCards", tableCards.toJsonArray())
        put("collected", collected.toNestedJsonArray())
        put("surCounts", surCounts.toJsonArray())
        put("lastCapturer", lastCapturer)
    }.toString()
}

object ChaharBargEngine {
    fun create(): ChaharBargState {
        val shuffled = CardDeck.fullShuffledDeck().toMutableList()
        // Ensure initial 4 table cards contain no Jack (traditional Iranian Pasoor rule)
        val table = mutableListOf<Int>()
        val iter = shuffled.iterator()
        while (table.size < 4 && iter.hasNext()) {
            val c = iter.next()
            if (CardDeck.rankOf(c) != 11) {
                table.add(c)
                iter.remove()
            }
        }
        val h0 = shuffled.take(4)
        val h1 = shuffled.drop(4).take(4)
        val remDeck = shuffled.drop(8)
        return ChaharBargState(
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "چهاربرگ آغاز شد! جمع ۱۱ بسازید یا با سرباز زمین را جمع کنید 🃏",
            deck = remDeck,
            hands = listOf(h0, h1),
            tableCards = table,
            collected = listOf(emptyList(), emptyList()),
            surCounts = listOf(0, 0),
            lastCapturer = 0
        )
    }

    fun fromJson(json: JSONObject): ChaharBargState {
        val w = json.optInt("winnerIndex", -99)
        return ChaharBargState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            deck = json.optJSONArray("deck")?.toIntList().orEmpty(),
            hands = json.optJSONArray("hands")?.toNestedIntList() ?: listOf(emptyList(), emptyList()),
            tableCards = json.optJSONArray("tableCards")?.toIntList().orEmpty(),
            collected = json.optJSONArray("collected")?.toNestedIntList() ?: listOf(emptyList(), emptyList()),
            surCounts = json.optJSONArray("surCounts")?.toIntList() ?: listOf(0, 0),
            lastCapturer = json.optInt("lastCapturer", 0)
        )
    }

    fun findBestCapture(cardId: Int, table: List<Int>): List<Int> {
        val rank = CardDeck.rankOf(cardId)
        // Jack sweeps all non-King, non-Queen cards
        if (rank == 11) {
            return table.filter { val r = CardDeck.rankOf(it); r != 12 && r != 13 }
        }
        // King matches King, Queen matches Queen
        if (rank == 12 || rank == 13) {
            val match = table.firstOrNull { CardDeck.rankOf(it) == rank }
            return if (match != null) listOf(match) else emptyList()
        }
        // Numeric card (Ace=1..10): find best subset summing to (11 - val)
        val need = 11 - CardDeck.pasoorValue(cardId)
        val numericTable = table.filter { CardDeck.pasoorValue(it) > 0 }
        var bestSubset: List<Int> = emptyList()
        val n = numericTable.size.coerceAtMost(12)
        val limit = 1 shl n
        for (mask in 1 until limit) {
            var sum = 0
            val subset = mutableListOf<Int>()
            for (i in 0 until n) {
                if ((mask and (1 shl i)) != 0) {
                    val c = numericTable[i]
                    sum += CardDeck.pasoorValue(c)
                    subset.add(c)
                }
            }
            if (sum == need && scoreSubset(subset) > scoreSubset(bestSubset)) {
                bestSubset = subset
            }
        }
        return bestSubset
    }

    private fun scoreSubset(cards: List<Int>): Int = cards.sumOf { c ->
        var w = 1
        if (CardDeck.suitOf(c) == CardSuit.CLUBS) w += 3
        if (CardDeck.rankOf(c) == 14) w += 4
        if (CardDeck.suitOf(c) == CardSuit.DIAMONDS && CardDeck.rankOf(c) == 10) w += 12
        if (CardDeck.suitOf(c) == CardSuit.CLUBS && CardDeck.rankOf(c) == 2) w += 8
        w
    }

    fun computeScore(collected: List<Int>, surs: Int): Int {
        var pts = surs * 5
        val clubs = collected.count { CardDeck.suitOf(it) == CardSuit.CLUBS }
        if (clubs >= 7) pts += 7
        if (collected.any { CardDeck.suitOf(it) == CardSuit.DIAMONDS && CardDeck.rankOf(it) == 10 }) pts += 3
        if (collected.any { CardDeck.suitOf(it) == CardSuit.CLUBS && CardDeck.rankOf(it) == 2 }) pts += 2
        pts += collected.count { CardDeck.rankOf(it) == 14 } // Aces
        pts += collected.count { CardDeck.rankOf(it) == 11 } // Jacks
        return pts
    }

    fun applyAction(state: ChaharBargState, playerIndex: Int, action: JSONObject): ChaharBargState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val cardId = action.optInt("card", -1)
        if (cardId !in state.hands[playerIndex]) return state

        val newHands = state.hands.mapIndexed { idx, h ->
            if (idx == playerIndex) h.filterNot { it == cardId } else h
        }.toMutableList()
        val captured = findBestCapture(cardId, state.tableCards)
        var newTable = state.tableCards.toMutableList()
        val newCollected = state.collected.map { it.toMutableList() }
        val newSurs = state.surCounts.toMutableList()
        var lastCap = state.lastCapturer
        var eventMsg: String

        if (captured.isNotEmpty()) {
            newTable.removeAll(captured.toSet())
            newCollected[playerIndex].add(cardId)
            newCollected[playerIndex].addAll(captured)
            lastCap = playerIndex
            val isSur = newTable.isEmpty() && CardDeck.rankOf(cardId) != 11 &&
                !(state.deck.isEmpty() && newHands.all { it.isEmpty() })
            if (isSur) {
                newSurs[playerIndex] = newSurs[playerIndex] + 1
                eventMsg = "سور!! 🔥 +۵ امتیاز ویژه برای بازیکن ${playerIndex + 1}!"
            } else {
                eventMsg = "بازیکن ${playerIndex + 1} تعداد ${captured.size + 1} کارت جمع کرد ✨"
            }
        } else {
            newTable.add(cardId)
            eventMsg = "بازیکن ${playerIndex + 1} کارت ${CardDeck.rankLabelFa(cardId)} ${CardDeck.suitOf(cardId).symbol} را روی زمین گذاشت"
        }

        var remDeck = state.deck
        // Redeal 4 cards each when both hands are empty
        if (newHands[0].isEmpty() && newHands[1].isEmpty()) {
            if (remDeck.size >= 8) {
                newHands[0] = remDeck.take(4)
                newHands[1] = remDeck.drop(4).take(4)
                remDeck = remDeck.drop(8)
                eventMsg += " — ۴ کارت جدید پخش شد 🃏"
            } else {
                // End of round: remaining table cards go to last capturer
                if (newTable.isNotEmpty()) {
                    newCollected[lastCap].addAll(newTable)
                    newTable = mutableListOf()
                }
                val s0 = computeScore(newCollected[0], newSurs[0])
                val s1 = computeScore(newCollected[1], newSurs[1])
                val win = when {
                    s0 > s1 -> 0
                    s1 > s0 -> 1
                    else -> -1
                }
                return state.copy(
                    deck = emptyList(),
                    hands = newHands,
                    tableCards = emptyList(),
                    collected = newCollected,
                    surCounts = newSurs,
                    lastCapturer = lastCap,
                    winnerIndex = win,
                    statusFa = if (win == -1) "پایان چهاربرگ: مساوی ($s0 - $s1) 🤝"
                    else "پایان چهاربرگ! برنده: بازیکن ${win + 1} با امتیاز $s0 در برابر $s1 🏆"
                )
            }
        }

        val nextTurn = 1 - playerIndex
        return state.copy(
            deck = remDeck,
            hands = newHands,
            tableCards = newTable,
            collected = newCollected,
            surCounts = newSurs,
            lastCapturer = lastCap,
            currentTurn = nextTurn,
            statusFa = eventMsg
        )
    }

    fun botAction(state: ChaharBargState): String? {
        val hand = state.hands[state.currentTurn]
        if (hand.isEmpty()) return null
        val bestCard = hand.maxByOrNull { c ->
            val cap = findBestCapture(c, state.tableCards)
            if (cap.isNotEmpty()) {
                val surBonus = if (cap.size == state.tableCards.size && CardDeck.rankOf(c) != 11) 50 else 0
                20 + scoreSubset(cap) + surBonus
            } else {
                // Prefer dropping non-valuable cards
                if (CardDeck.rankOf(c) == 11) -20 else 5 - scoreSubset(listOf(c))
            }
        } ?: hand.first()
        return JSONObject().put("card", bestCard).toString()
    }
}

// ============================================================================
// 8. HAFT KHABIS (هفت خبیث ایرانی - ۲ تا ۴ نفر)
// 7 = +2 draw stack, 8 = play again, 10 = reverse, Ace(14) = skip, Jack(11) = wild change suit
// ============================================================================
data class HaftKhabisState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val deck: List<Int>,
    val discardPile: List<Int>,
    val hands: List<List<Int>>,
    val activeSuit: Int, // 0..3
    val direction: Int, // +1 or -1
    val pendingDraw: Int, // stacked +2s from 7s
    val awaitingSuitPick: Boolean
) : ActiveGameState {
    override val gameType: GameType = GameType.HAFT_KHABIS

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("deck", deck.toJsonArray())
        put("discardPile", discardPile.toJsonArray())
        put("hands", hands.toNestedJsonArray())
        put("activeSuit", activeSuit)
        put("direction", direction)
        put("pendingDraw", pendingDraw)
        put("awaitingSuitPick", awaitingSuitPick)
    }.toString()
}

object HaftKhabisEngine {
    fun create(playerCount: Int): HaftKhabisState {
        val deck = CardDeck.fullShuffledDeck().toMutableList()
        val hands = List(playerCount) {
            val h = deck.take(7)
            repeat(7) { deck.removeAt(0) }
            CardDeck.sortHand(h)
        }
        val firstDiscard = deck.removeAt(0)
        return HaftKhabisState(
            playerCount = playerCount,
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "هفت خبیث آغاز شد! کارت هم‌خال یا هم‌عدد بازی کنید 🃏",
            deck = deck,
            discardPile = listOf(firstDiscard),
            hands = hands,
            activeSuit = CardDeck.suitOf(firstDiscard).ordinal,
            direction = 1,
            pendingDraw = 0,
            awaitingSuitPick = false
        )
    }

    fun fromJson(json: JSONObject): HaftKhabisState {
        val w = json.optInt("winnerIndex", -99)
        val pc = json.optInt("playerCount", 2)
        return HaftKhabisState(
            playerCount = pc,
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            deck = json.optJSONArray("deck")?.toIntList().orEmpty(),
            discardPile = json.optJSONArray("discardPile")?.toIntList() ?: listOf(0),
            hands = json.optJSONArray("hands")?.toNestedIntList() ?: List(pc) { emptyList() },
            activeSuit = json.optInt("activeSuit", 0),
            direction = json.optInt("direction", 1),
            pendingDraw = json.optInt("pendingDraw", 0),
            awaitingSuitPick = json.optBoolean("awaitingSuitPick", false)
        )
    }

    fun playableCards(state: HaftKhabisState, seat: Int): Set<Int> {
        if (seat != state.currentTurn || state.awaitingSuitPick || state.winnerIndex != null) return emptySet()
        val hand = state.hands[seat]
        val top = state.discardPile.lastOrNull() ?: return hand.toSet()
        val topRank = CardDeck.rankOf(top)
        if (state.pendingDraw > 0) {
            return hand.filter { CardDeck.rankOf(it) == 7 }.toSet()
        }
        return hand.filter { c ->
            CardDeck.rankOf(c) == 11 ||
                CardDeck.rankOf(c) == topRank ||
                CardDeck.suitOf(c).ordinal == state.activeSuit
        }.toSet()
    }

    private fun advance(cur: Int, dir: Int, count: Int, steps: Int = 1): Int =
        ((cur + dir * steps) % count + count) % count

    private fun drawCards(deck: MutableList<Int>, discard: MutableList<Int>, n: Int): List<Int> {
        val drawn = mutableListOf<Int>()
        repeat(n) {
            if (deck.isEmpty() && discard.size > 1) {
                val top = discard.removeAt(discard.lastIndex)
                deck.addAll(discard.shuffled())
                discard.clear()
                discard.add(top)
            }
            if (deck.isNotEmpty()) drawn.add(deck.removeAt(0))
        }
        return drawn
    }

    fun applyAction(state: HaftKhabisState, playerIndex: Int, action: JSONObject): HaftKhabisState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "PICK_SUIT" -> {
                if (!state.awaitingSuitPick) return state
                val suitOrd = action.optInt("suit", 0).coerceIn(0, 3)
                val next = advance(playerIndex, state.direction, state.playerCount)
                val s = CardSuit.fromOrdinal(suitOrd)
                state.copy(
                    activeSuit = suitOrd,
                    awaitingSuitPick = false,
                    currentTurn = next,
                    statusFa = "سرباز خال را به ${s.nameFa} ${s.symbol} تغییر داد! نوبت بازیکن ${next + 1}"
                )
            }
            "DRAW" -> {
                if (state.awaitingSuitPick) return state
                val deck = state.deck.toMutableList()
                val discard = state.discardPile.toMutableList()
                val countToDraw = if (state.pendingDraw > 0) state.pendingDraw else 1
                val drawn = drawCards(deck, discard, countToDraw)
                val newHands = state.hands.mapIndexed { idx, h ->
                    if (idx == playerIndex) CardDeck.sortHand(h + drawn) else h
                }
                val next = advance(playerIndex, state.direction, state.playerCount)
                state.copy(
                    deck = deck,
                    discardPile = discard,
                    hands = newHands,
                    pendingDraw = 0,
                    currentTurn = next,
                    statusFa = "بازیکن ${playerIndex + 1} تعداد ${drawn.size} کارت کشید — نوبت بازیکن ${next + 1}"
                )
            }
            "PLAY_CARD" -> {
                val cardId = action.optInt("card", -1)
                if (cardId !in playableCards(state, playerIndex)) return state
                val newHands = state.hands.mapIndexed { idx, h ->
                    if (idx == playerIndex) h.filterNot { it == cardId } else h
                }
                val newDiscard = state.discardPile + cardId
                if (newHands[playerIndex].isEmpty()) {
                    return state.copy(
                        hands = newHands,
                        discardPile = newDiscard,
                        winnerIndex = playerIndex,
                        statusFa = "دست بازیکن ${playerIndex + 1} خالی شد و برنده هفت خبیث شد! 🏆"
                    )
                }

                val rank = CardDeck.rankOf(cardId)
                val suitOrd = CardDeck.suitOf(cardId).ordinal
                when (rank) {
                    11 -> state.copy(
                        hands = newHands,
                        discardPile = newDiscard,
                        awaitingSuitPick = true,
                        statusFa = "سرباز بازی شد! خال جدید را انتخاب کنید 🃏"
                    )
                    7 -> {
                        val next = advance(playerIndex, state.direction, state.playerCount)
                        val nextDraw = state.pendingDraw + 2
                        state.copy(
                            hands = newHands,
                            discardPile = newDiscard,
                            activeSuit = suitOrd,
                            pendingDraw = nextDraw,
                            currentTurn = next,
                            statusFa = "هفت خبیث! 🔥 بازیکن ${next + 1} باید $nextDraw کارت بکشد یا ۷ بگذارد!"
                        )
                    }
                    8 -> state.copy(
                        hands = newHands,
                        discardPile = newDiscard,
                        activeSuit = suitOrd,
                        currentTurn = playerIndex,
                        statusFa = "کارت ۸! ✨ جایزه یک نوبت اضافه برای بازیکن ${playerIndex + 1}"
                    )
                    10 -> {
                        val newDir = -state.direction
                        val next = if (state.playerCount == 2) playerIndex
                        else advance(playerIndex, newDir, state.playerCount)
                        state.copy(
                            hands = newHands,
                            discardPile = newDiscard,
                            activeSuit = suitOrd,
                            direction = newDir,
                            currentTurn = next,
                            statusFa = "کارت ۱۰! 🔄 جهت بازی برعکس شد — نوبت بازیکن ${next + 1}"
                        )
                    }
                    14 -> {
                        val next = advance(playerIndex, state.direction, state.playerCount, steps = 2)
                        state.copy(
                            hands = newHands,
                            discardPile = newDiscard,
                            activeSuit = suitOrd,
                            currentTurn = next,
                            statusFa = "تک! ⚡ نوبت نفر بعدی سوخت — نوبت بازیکن ${next + 1}"
                        )
                    }
                    else -> {
                        val next = advance(playerIndex, state.direction, state.playerCount)
                        state.copy(
                            hands = newHands,
                            discardPile = newDiscard,
                            activeSuit = suitOrd,
                            currentTurn = next,
                            statusFa = "نوبت بازیکن ${next + 1}"
                        )
                    }
                }
            }
            else -> state
        }
    }

    fun botAction(state: HaftKhabisState): String {
        val seat = state.currentTurn
        if (state.awaitingSuitPick) {
            val bestSuit = (0..3).maxByOrNull { s ->
                state.hands[seat].count { CardDeck.suitOf(it).ordinal == s }
            } ?: 0
            return JSONObject().put("op", "PICK_SUIT").put("suit", bestSuit).toString()
        }
        val playable = playableCards(state, seat).toList()
        if (playable.isEmpty()) return JSONObject().put("op", "DRAW").toString()
        // Save Jack (11) for last, prefer action cards (7, 8, 14)
        val chosen = playable.maxByOrNull { c ->
            when (CardDeck.rankOf(c)) {
                7 -> 100
                8 -> 90
                14 -> 80
                11 -> 10
                else -> 50
            }
        } ?: playable.first()
        return JSONObject().put("op", "PLAY_CARD").put("card", chosen).toString()
    }
}
