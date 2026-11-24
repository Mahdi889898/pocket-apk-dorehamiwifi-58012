package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.games.JsonHelpers.toNestedIntList
import com.example.games.JsonHelpers.toNestedJsonArray
import com.example.model.GameType
import org.json.JSONObject

// ============================================================================
// 5. HOKM 4-PLAYER TEAM CARD GAME (حکم ۴ نفره تیمی اصیل ایرانی)
// Teams: Team 0 = seats 0 & 2, Team 1 = seats 1 & 3
// Phases: "CHOOSE_HOKM" (Hakem sees 5 cards) -> "PLAY" (13 tricks) -> "TRICK_PAUSE"
// ============================================================================
data class HokmState(
    override val playerCount: Int = 4,
    override val currentTurn: Int,
    override val winnerIndex: Int?, // 0 = Team 0 (seats 0&2), 1 = Team 1 (seats 1&3)
    override val statusFa: String,
    val phase: String, // "CHOOSE_HOKM", "PLAY", "TRICK_PAUSE"
    val hakem: Int,
    val hokmSuit: Int?, // 0..3 CardSuit ordinal
    val hands: List<List<Int>>, // 4 hands
    val fullDeckTail: List<Int>, // remaining 32 cards dealt after Hakem picks suit
    val trickCards: List<Int>, // 4 slots: -1 if not played yet, else cardId
    val trickLeadSeat: Int,
    val teamTricks: List<Int>, // [team0Tricks, team1Tricks] in current hand (first to 7 wins hand)
    val teamHandsWon: List<Int>, // [team0Hands, team1Hands] (race to 7 or quick 3 rounds)
    val lastTrickWinner: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.HOKM

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("phase", phase)
        put("hakem", hakem)
        put("hokmSuit", hokmSuit ?: -1)
        put("hands", hands.toNestedJsonArray())
        put("fullDeckTail", fullDeckTail.toJsonArray())
        put("trickCards", trickCards.toJsonArray())
        put("trickLeadSeat", trickLeadSeat)
        put("teamTricks", teamTricks.toJsonArray())
        put("teamHandsWon", teamHandsWon.toJsonArray())
        put("lastTrickWinner", lastTrickWinner ?: -1)
    }.toString()
}

object HokmEngine {
    const val TARGET_HANDS = 3 // Quick exciting 3-hand match (Kot = 2 hands!)

    fun create(hakem: Int = 0, teamHandsWon: List<Int> = listOf(0, 0)): HokmState {
        val deck = CardDeck.fullShuffledDeck()
        val initialHands = List(4) { seat ->
            CardDeck.sortHand(deck.subList(seat * 5, seat * 5 + 5))
        }
        val tail = deck.subList(20, 52)
        return HokmState(
            currentTurn = hakem,
            winnerIndex = null,
            statusFa = "حاکم (بازیکن ${hakem + 1}) با دیدن ۵ کارت اول، خال حکم را انتخاب کند 👑",
            phase = "CHOOSE_HOKM",
            hakem = hakem,
            hokmSuit = null,
            hands = initialHands,
            fullDeckTail = tail,
            trickCards = listOf(-1, -1, -1, -1),
            trickLeadSeat = hakem,
            teamTricks = listOf(0, 0),
            teamHandsWon = teamHandsWon,
            lastTrickWinner = null
        )
    }

    fun fromJson(json: JSONObject): HokmState {
        val w = json.optInt("winnerIndex", -99)
        val hs = json.optInt("hokmSuit", -1)
        val ltw = json.optInt("lastTrickWinner", -1)
        return HokmState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            phase = json.optString("phase", "PLAY"),
            hakem = json.optInt("hakem", 0),
            hokmSuit = if (hs == -1) null else hs,
            hands = json.optJSONArray("hands")?.toNestedIntList() ?: List(4) { emptyList() },
            fullDeckTail = json.optJSONArray("fullDeckTail")?.toIntList().orEmpty(),
            trickCards = json.optJSONArray("trickCards")?.toIntList() ?: listOf(-1, -1, -1, -1),
            trickLeadSeat = json.optInt("trickLeadSeat", 0),
            teamTricks = json.optJSONArray("teamTricks")?.toIntList() ?: listOf(0, 0),
            teamHandsWon = json.optJSONArray("teamHandsWon")?.toIntList() ?: listOf(0, 0),
            lastTrickWinner = if (ltw == -1) null else ltw
        )
    }

    fun legalCards(state: HokmState, seat: Int): Set<Int> {
        if (state.phase != "PLAY" || seat != state.currentTurn) return emptySet()
        val hand = state.hands[seat]
        val leadCard = state.trickCards[state.trickLeadSeat]
        if (leadCard == -1) return hand.toSet()
        val leadSuit = CardDeck.suitOf(leadCard)
        val sameSuit = hand.filter { CardDeck.suitOf(it) == leadSuit }
        return if (sameSuit.isNotEmpty()) sameSuit.toSet() else hand.toSet()
    }

    fun trickWinner(trickCards: List<Int>, leadSeat: Int, hokmSuitOrd: Int): Int {
        val leadSuit = CardDeck.suitOf(trickCards[leadSeat])
        val hokmSuit = CardSuit.fromOrdinal(hokmSuitOrd)
        var bestSeat = leadSeat
        var bestCard = trickCards[leadSeat]
        for (i in 1..3) {
            val seat = (leadSeat + i) % 4
            val card = trickCards[seat]
            val cSuit = CardDeck.suitOf(card)
            val bSuit = CardDeck.suitOf(bestCard)
            val beats = when {
                cSuit == bSuit -> CardDeck.rankOf(card) > CardDeck.rankOf(bestCard)
                cSuit == hokmSuit && bSuit != hokmSuit -> true
                else -> false
            }
            if (beats) {
                bestSeat = seat
                bestCard = card
            }
        }
        return bestSeat
    }

    fun applyAction(state: HokmState, playerIndex: Int, action: JSONObject): HokmState {
        if (state.winnerIndex != null) return state
        return when (action.optString("op")) {
            "PICK_HOKM" -> {
                if (state.phase != "CHOOSE_HOKM" || playerIndex != state.hakem) return state
                val suitOrd = action.optInt("suit", 0).coerceIn(0, 3)
                val fullHands = List(4) { seat ->
                    val extra = state.fullDeckTail.subList(seat * 8, seat * 8 + 8)
                    CardDeck.sortHand(state.hands[seat] + extra)
                }
                val suit = CardSuit.fromOrdinal(suitOrd)
                state.copy(
                    phase = "PLAY",
                    hokmSuit = suitOrd,
                    hands = fullHands,
                    fullDeckTail = emptyList(),
                    currentTurn = state.hakem,
                    trickLeadSeat = state.hakem,
                    statusFa = "حکم اعلام شد: ${suit.nameFa} ${suit.symbol} — نوبت حاکم برای شروع دست!"
                )
            }
            "PLAY_CARD" -> {
                if (state.phase != "PLAY" || playerIndex != state.currentTurn) return state
                val cardId = action.optInt("card", -1)
                if (cardId !in legalCards(state, playerIndex)) return state

                val updatedHands = state.hands.mapIndexed { idx, h ->
                    if (idx == playerIndex) h.filterNot { it == cardId } else h
                }
                val updatedTrick = state.trickCards.toMutableList()
                updatedTrick[playerIndex] = cardId

                if (updatedTrick.all { it != -1 }) {
                    val winSeat = trickWinner(updatedTrick, state.trickLeadSeat, state.hokmSuit ?: 0)
                    val winTeam = winSeat % 2
                    val nextTricks = state.teamTricks.toMutableList()
                    nextTricks[winTeam] = nextTricks[winTeam] + 1
                    state.copy(
                        hands = updatedHands,
                        trickCards = updatedTrick,
                        teamTricks = nextTricks,
                        lastTrickWinner = winSeat,
                        phase = "TRICK_PAUSE",
                        statusFa = "دست به بازیکن ${winSeat + 1} (تیم ${winTeam + 1}) رسید! ✨"
                    )
                } else {
                    val nextSeat = (playerIndex + 1) % 4
                    state.copy(
                        hands = updatedHands,
                        trickCards = updatedTrick,
                        currentTurn = nextSeat,
                        statusFa = "نوبت بازیکن ${nextSeat + 1}"
                    )
                }
            }
            "CLEAR_TRICK" -> {
                if (state.phase != "TRICK_PAUSE") return state
                val winSeat = state.lastTrickWinner ?: 0
                val t0 = state.teamTricks[0]
                val t1 = state.teamTricks[1]
                if (t0 >= 7 || t1 >= 7) {
                    val roundWinTeam = if (t0 >= 7) 0 else 1
                    val isKot = (t0 == 7 && t1 == 0) || (t1 == 7 && t0 == 0)
                    val bonusPts = if (isKot) 2 else 1
                    val nextHandsWon = state.teamHandsWon.toMutableList()
                    nextHandsWon[roundWinTeam] = nextHandsWon[roundWinTeam] + bonusPts

                    if (nextHandsWon[roundWinTeam] >= TARGET_HANDS) {
                        return state.copy(
                            teamHandsWon = nextHandsWon,
                            trickCards = listOf(-1, -1, -1, -1),
                            phase = "PLAY",
                            winnerIndex = roundWinTeam,
                            statusFa = "تیم ${roundWinTeam + 1} قهرمان بازی حکم شد! 🏆 (${nextHandsWon[0]} - ${nextHandsWon[1]})"
                        )
                    }
                    // Next round: if Hakem's team won, Hakem stays; else rotates to next player
                    val nextHakem = if (roundWinTeam == state.hakem % 2) state.hakem else (state.hakem + 1) % 4
                    val nextRound = create(hakem = nextHakem, teamHandsWon = nextHandsWon)
                    return nextRound.copy(
                        statusFa = (if (isKot) "کت شد! 🔥 ۲ امتیاز برای تیم ${roundWinTeam + 1} — " else "راند به تیم ${roundWinTeam + 1} رسید! — ") + nextRound.statusFa
                    )
                }
                state.copy(
                    phase = "PLAY",
                    trickCards = listOf(-1, -1, -1, -1),
                    currentTurn = winSeat,
                    trickLeadSeat = winSeat,
                    statusFa = "نوبت بازیکن ${winSeat + 1} برای شروع دست جدید"
                )
            }
            else -> state
        }
    }

    fun botAction(state: HokmState): String? {
        if (state.phase == "TRICK_PAUSE") return JSONObject().put("op", "CLEAR_TRICK").toString()
        if (state.phase == "CHOOSE_HOKM") {
            val hand = state.hands[state.hakem]
            val bestSuit = (0..3).maxByOrNull { s ->
                hand.filter { CardDeck.suitOf(it).ordinal == s }.sumOf { CardDeck.rankOf(it) }
            } ?: 0
            return JSONObject().put("op", "PICK_HOKM").put("suit", bestSuit).toString()
        }
        val seat = state.currentTurn
        val legal = legalCards(state, seat).toList()
        if (legal.isEmpty()) return null
        val hokmSuit = CardSuit.fromOrdinal(state.hokmSuit ?: 0)
        val leadCard = state.trickCards[state.trickLeadSeat]
        val chosen = if (leadCard == -1) {
            // Lead strongest non-hokm Ace/King or highest card
            legal.maxByOrNull { c ->
                val r = CardDeck.rankOf(c)
                if (CardDeck.suitOf(c) == hokmSuit) r - 3 else r
            } ?: legal.first()
        } else {
            val leadSuit = CardDeck.suitOf(leadCard)
            val played = state.trickCards.filter { it != -1 }
            val currentHighNonTrump = played.filter { CardDeck.suitOf(it) == leadSuit }.maxOfOrNull { CardDeck.rankOf(it) } ?: 0
            val trumpPlayed = played.any { CardDeck.suitOf(it) == hokmSuit }
            // Try to win cheaply, else dump lowest card
            val canBeatFollow = legal.filter { CardDeck.suitOf(it) == leadSuit && !trumpPlayed && CardDeck.rankOf(it) > currentHighNonTrump }
            val canCut = legal.filter { CardDeck.suitOf(it) == hokmSuit && leadSuit != hokmSuit && !trumpPlayed }
            when {
                canBeatFollow.isNotEmpty() -> canBeatFollow.minBy { CardDeck.rankOf(it) }
                canCut.isNotEmpty() -> canCut.minBy { CardDeck.rankOf(it) }
                else -> legal.minBy { CardDeck.rankOf(it) }
            }
        }
        return JSONObject().put("op", "PLAY_CARD").put("card", chosen).toString()
    }
}

// ============================================================================
// 6. SHELEM (شلم ۴ نفره تیمی امتیازی - مزایده ۱۰۰ تا ۱۶۵)
// Points per hand: 13 tricks x 5 = 65 + four Aces (40) + four Tens (40) + four Fives (20) = 165 pts
// ============================================================================
data class ShelemState(
    override val playerCount: Int = 4,
    override val currentTurn: Int,
    override val winnerIndex: Int?, // 0 = Team 0 (0&2), 1 = Team 1 (1&3)
    override val statusFa: String,
    val phase: String, // "BID", "CHOOSE_HOKM", "PLAY", "TRICK_PAUSE"
    val highestBid: Int,
    val highestBidder: Int,
    val passedMask: List<Int>, // 4 flags (0/1)
    val hokmSuit: Int?,
    val hands: List<List<Int>>,
    val trickCards: List<Int>,
    val trickLeadSeat: Int,
    val teamPoints: List<Int>, // points collected in current hand
    val lastTrickWinner: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.SHELEM

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("phase", phase)
        put("highestBid", highestBid)
        put("highestBidder", highestBidder)
        put("passedMask", passedMask.toJsonArray())
        put("hokmSuit", hokmSuit ?: -1)
        put("hands", hands.toNestedJsonArray())
        put("trickCards", trickCards.toJsonArray())
        put("trickLeadSeat", trickLeadSeat)
        put("teamPoints", teamPoints.toJsonArray())
        put("lastTrickWinner", lastTrickWinner ?: -1)
    }.toString()
}

object ShelemEngine {
    fun create(): ShelemState {
        val deck = CardDeck.fullShuffledDeck()
        val hands = List(4) { seat ->
            CardDeck.sortHand(deck.subList(seat * 13, seat * 13 + 13))
        }
        return ShelemState(
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "مرحله مزایده شلم! از ۱۰۰ تا ۱۶۵ امتیاز بخوانید یا پاس دهید 🎯",
            phase = "BID",
            highestBid = 95,
            highestBidder = 0,
            passedMask = listOf(0, 0, 0, 0),
            hokmSuit = null,
            hands = hands,
            trickCards = listOf(-1, -1, -1, -1),
            trickLeadSeat = 0,
            teamPoints = listOf(0, 0),
            lastTrickWinner = null
        )
    }

    fun fromJson(json: JSONObject): ShelemState {
        val w = json.optInt("winnerIndex", -99)
        val hs = json.optInt("hokmSuit", -1)
        val ltw = json.optInt("lastTrickWinner", -1)
        return ShelemState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            phase = json.optString("phase", "BID"),
            highestBid = json.optInt("highestBid", 100),
            highestBidder = json.optInt("highestBidder", 0),
            passedMask = json.optJSONArray("passedMask")?.toIntList() ?: listOf(0, 0, 0, 0),
            hokmSuit = if (hs == -1) null else hs,
            hands = json.optJSONArray("hands")?.toNestedIntList() ?: List(4) { emptyList() },
            trickCards = json.optJSONArray("trickCards")?.toIntList() ?: listOf(-1, -1, -1, -1),
            trickLeadSeat = json.optInt("trickLeadSeat", 0),
            teamPoints = json.optJSONArray("teamPoints")?.toIntList() ?: listOf(0, 0),
            lastTrickWinner = if (ltw == -1) null else ltw
        )
    }

    fun cardPoints(cardId: Int): Int = when (CardDeck.rankOf(cardId)) {
        14 -> 10
        10 -> 10
        5 -> 5
        else -> 0
    }

    fun legalCards(state: ShelemState, seat: Int): Set<Int> {
        if (state.phase != "PLAY" || seat != state.currentTurn) return emptySet()
        val hand = state.hands[seat]
        val leadCard = state.trickCards[state.trickLeadSeat]
        if (leadCard == -1) return hand.toSet()
        val leadSuit = CardDeck.suitOf(leadCard)
        val sameSuit = hand.filter { CardDeck.suitOf(it) == leadSuit }
        return if (sameSuit.isNotEmpty()) sameSuit.toSet() else hand.toSet()
    }

    fun applyAction(state: ShelemState, playerIndex: Int, action: JSONObject): ShelemState {
        if (state.winnerIndex != null) return state
        return when (action.optString("op")) {
            "BID" -> {
                if (state.phase != "BID" || playerIndex != state.currentTurn) return state
                val bidVal = action.optInt("bid", 0) // 0 = PASS
                val passed = state.passedMask.toMutableList()
                var highBid = state.highestBid
                var highBidder = state.highestBidder

                if (bidVal == 0) {
                    passed[playerIndex] = 1
                } else {
                    if (bidVal <= highBid || bidVal > 165) return state
                    highBid = bidVal
                    highBidder = playerIndex
                }

                val activeBidders = (0..3).filter { passed[it] == 0 }
                if (activeBidders.size <= 1 || highBid >= 165) {
                    val finalBid = highBid.coerceAtLeast(100)
                    val winnerBidder = if (activeBidders.size == 1) activeBidders.first() else highBidder
                    return state.copy(
                        phase = "CHOOSE_HOKM",
                        highestBid = finalBid,
                        highestBidder = winnerBidder,
                        passedMask = passed,
                        currentTurn = winnerBidder,
                        statusFa = "بازیکن ${winnerBidder + 1} با امتیاز $finalBid حاکم شلم شد! خال حکم را انتخاب کنید 👑"
                    )
                }
                var next = (playerIndex + 1) % 4
                while (passed[next] == 1) next = (next + 1) % 4
                state.copy(
                    highestBid = highBid,
                    highestBidder = highBidder,
                    passedMask = passed,
                    currentTurn = next,
                    statusFa = if (bidVal == 0) "بازیکن ${playerIndex + 1} پاس داد — نوبت بازیکن ${next + 1}"
                    else "بازیکن ${playerIndex + 1} امتیاز $bidVal خواند! نوبت بازیکن ${next + 1}"
                )
            }
            "PICK_HOKM" -> {
                if (state.phase != "CHOOSE_HOKM" || playerIndex != state.highestBidder) return state
                val suitOrd = action.optInt("suit", 0).coerceIn(0, 3)
                val suit = CardSuit.fromOrdinal(suitOrd)
                state.copy(
                    phase = "PLAY",
                    hokmSuit = suitOrd,
                    currentTurn = state.highestBidder,
                    trickLeadSeat = state.highestBidder,
                    statusFa = "حکم شلم: ${suit.nameFa} ${suit.symbol} (هدف: ${state.highestBid} امتیاز) — نوبت حاکم!"
                )
            }
            "PLAY_CARD" -> {
                if (state.phase != "PLAY" || playerIndex != state.currentTurn) return state
                val cardId = action.optInt("card", -1)
                if (cardId !in legalCards(state, playerIndex)) return state

                val updatedHands = state.hands.mapIndexed { idx, h ->
                    if (idx == playerIndex) h.filterNot { it == cardId } else h
                }
                val updatedTrick = state.trickCards.toMutableList()
                updatedTrick[playerIndex] = cardId

                if (updatedTrick.all { it != -1 }) {
                    val winSeat = HokmEngine.trickWinner(updatedTrick, state.trickLeadSeat, state.hokmSuit ?: 0)
                    val winTeam = winSeat % 2
                    val trickPts = 5 + updatedTrick.sumOf { cardPoints(it) }
                    val nextPts = state.teamPoints.toMutableList()
                    nextPts[winTeam] = nextPts[winTeam] + trickPts
                    state.copy(
                        hands = updatedHands,
                        trickCards = updatedTrick,
                        teamPoints = nextPts,
                        lastTrickWinner = winSeat,
                        phase = "TRICK_PAUSE",
                        statusFa = "بازیکن ${winSeat + 1} این دست (+$trickPts امتیاز) را برد! ✨"
                    )
                } else {
                    val next = (playerIndex + 1) % 4
                    state.copy(
                        hands = updatedHands,
                        trickCards = updatedTrick,
                        currentTurn = next,
                        statusFa = "نوبت بازیکن ${next + 1}"
                    )
                }
            }
            "CLEAR_TRICK" -> {
                if (state.phase != "TRICK_PAUSE") return state
                val winSeat = state.lastTrickWinner ?: 0
                if (state.hands.all { it.isEmpty() }) {
                    val hakemTeam = state.highestBidder % 2
                    val hakemScored = state.teamPoints[hakemTeam]
                    val matchWinner = if (hakemScored >= state.highestBid) hakemTeam else (1 - hakemTeam)
                    val msg = if (hakemScored >= state.highestBid) {
                        "تیم حاکم (تیم ${hakemTeam + 1}) با $hakemScored امتیاز به تعهد ${state.highestBid} رسید و برنده شلم شد! 🏆"
                    } else {
                        "تیم حاکم به تعهد ${state.highestBid} نرسید ($hakemScored امتیاز) — تیم ${matchWinner + 1} برنده شد! 🏆"
                    }
                    return state.copy(
                        trickCards = listOf(-1, -1, -1, -1),
                        phase = "PLAY",
                        winnerIndex = matchWinner,
                        statusFa = msg
                    )
                }
                state.copy(
                    phase = "PLAY",
                    trickCards = listOf(-1, -1, -1, -1),
                    currentTurn = winSeat,
                    trickLeadSeat = winSeat,
                    statusFa = "نوبت بازیکن ${winSeat + 1} برای دست بعدی"
                )
            }
            else -> state
        }
    }

    fun botAction(state: ShelemState): String? {
        if (state.phase == "TRICK_PAUSE") return JSONObject().put("op", "CLEAR_TRICK").toString()
        val seat = state.currentTurn
        if (state.phase == "BID") {
            val hand = state.hands[seat]
            val strength = hand.count { CardDeck.rankOf(it) >= 12 } * 12 +
                (0..3).maxOf { s -> hand.count { CardDeck.suitOf(it).ordinal == s } } * 10
            val maxWilling = (95 + strength).coerceAtMost(140)
            val nextBid = (state.highestBid + 5).coerceAtLeast(100)
            return if (nextBid <= maxWilling && state.highestBidder != seat) {
                JSONObject().put("op", "BID").put("bid", nextBid).toString()
            } else {
                JSONObject().put("op", "BID").put("bid", 0).toString()
            }
        }
        if (state.phase == "CHOOSE_HOKM") {
            val hand = state.hands[seat]
            val bestSuit = (0..3).maxByOrNull { s ->
                hand.filter { CardDeck.suitOf(it).ordinal == s }.sumOf { CardDeck.rankOf(it) }
            } ?: 0
            return JSONObject().put("op", "PICK_HOKM").put("suit", bestSuit).toString()
        }
        val legal = legalCards(state, seat).toList()
        if (legal.isEmpty()) return null
        val chosen = legal.maxByOrNull { CardDeck.rankOf(it) + cardPoints(it) } ?: legal.first()
        return JSONObject().put("op", "PLAY_CARD").put("card", chosen).toString()
    }
}
