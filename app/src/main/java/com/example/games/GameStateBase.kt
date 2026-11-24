package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.model.GameType
import org.json.JSONArray
import org.json.JSONObject

sealed interface ActiveGameState {
    val gameType: GameType
    val playerCount: Int
    val currentTurn: Int
    val winnerIndex: Int? // null = ongoing, -1 = draw, 0..3 = winning player or team lead
    val statusFa: String

    fun toJson(): String
}

/**
 * Standard 52-card encoding:
 * cardId = suit.ordinal * 13 + (rank - 2)
 * rank: 2..10, 11 = Jack (سرباز), 12 = Queen (بی‌بی), 13 = King (شاه), 14 = Ace (تک)
 */
enum class CardSuit(val symbol: String, val nameFa: String, val isRed: Boolean) {
    SPADES("♠", "پیک", false),
    HEARTS("♥", "دل", true),
    CLUBS("♣", "گشنیز", false),
    DIAMONDS("♦", "خشت", true);

    companion object {
        fun fromOrdinal(ord: Int): CardSuit = entries.getOrElse(ord) { SPADES }
    }
}

object CardDeck {
    fun suitOf(cardId: Int): CardSuit = CardSuit.fromOrdinal((cardId / 13).coerceIn(0, 3))
    fun rankOf(cardId: Int): Int = (cardId % 13) + 2 // 2..14 (14 = Ace)

    fun rankLabel(cardId: Int): String = when (rankOf(cardId)) {
        14 -> "A"
        13 -> "K"
        12 -> "Q"
        11 -> "J"
        else -> rankOf(cardId).toString()
    }

    fun rankLabelFa(cardId: Int): String = when (rankOf(cardId)) {
        14 -> "تک"
        13 -> "شاه"
        12 -> "بی‌بی"
        11 -> "سرباز"
        else -> rankOf(cardId).toString()
    }

    fun pasoorValue(cardId: Int): Int = when (val r = rankOf(cardId)) {
        14 -> 1
        in 2..10 -> r
        else -> 0 // J, Q, K are picture cards
    }

    fun fullShuffledDeck(): List<Int> = (0 until 52).shuffled()

    fun sortHand(hand: List<Int>): List<Int> = hand.sortedWith(
        compareBy<Int> { suitOf(it).ordinal }.thenByDescending { rankOf(it) }
    )
}

object JsonHelpers {
    fun List<Int>.toJsonArray(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it) } }
    fun JSONArray.toIntList(): List<Int> = List(length()) { optInt(it) }
    fun List<List<Int>>.toNestedJsonArray(): JSONArray = JSONArray().also { outer ->
        forEach { inner -> outer.put(inner.toJsonArray()) }
    }
    fun JSONArray.toNestedIntList(): List<List<Int>> = List(length()) { i ->
        optJSONArray(i)?.toIntList().orEmpty()
    }
}

object GameEngineDispatcher {
    fun createInitialState(gameType: GameType, playerCount: Int): ActiveGameState {
        val clamped = playerCount.coerceIn(gameType.minPlayers, gameType.maxPlayers)
        return when (gameType) {
            GameType.MENCH_CLASSIC -> MenchClassicEngine.create(clamped)
            GameType.SNAKES_LADDERS -> SnakesLaddersEngine.create(clamped)
            GameType.PACHISI_ROYAL -> PachisiEngine.create(clamped)
            GameType.BACKGAMMON -> BackgammonEngine.create()
            GameType.HOKM -> HokmEngine.create()
            GameType.SHELEM -> ShelemEngine.create()
            GameType.CHAHAR_BARG -> ChaharBargEngine.create()
            GameType.HAFT_KHABIS -> HaftKhabisEngine.create(clamped)
        }
    }

    fun deserialize(gameType: GameType, jsonStr: String): ActiveGameState? {
        return try {
            val json = JSONObject(jsonStr)
            when (gameType) {
                GameType.MENCH_CLASSIC -> MenchClassicEngine.fromJson(json)
                GameType.SNAKES_LADDERS -> SnakesLaddersEngine.fromJson(json)
                GameType.PACHISI_ROYAL -> PachisiEngine.fromJson(json)
                GameType.BACKGAMMON -> BackgammonEngine.fromJson(json)
                GameType.HOKM -> HokmEngine.fromJson(json)
                GameType.SHELEM -> ShelemEngine.fromJson(json)
                GameType.CHAHAR_BARG -> ChaharBargEngine.fromJson(json)
                GameType.HAFT_KHABIS -> HaftKhabisEngine.fromJson(json)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun applyAction(state: ActiveGameState, playerIndex: Int, actionJson: String): ActiveGameState {
        val action = try {
            JSONObject(actionJson)
        } catch (_: Exception) {
            return state
        }
        return when (state) {
            is MenchClassicState -> MenchClassicEngine.applyAction(state, playerIndex, action)
            is SnakesLaddersState -> SnakesLaddersEngine.applyAction(state, playerIndex, action)
            is PachisiState -> PachisiEngine.applyAction(state, playerIndex, action)
            is BackgammonState -> BackgammonEngine.applyAction(state, playerIndex, action)
            is HokmState -> HokmEngine.applyAction(state, playerIndex, action)
            is ShelemState -> ShelemEngine.applyAction(state, playerIndex, action)
            is ChaharBargState -> ChaharBargEngine.applyAction(state, playerIndex, action)
            is HaftKhabisState -> HaftKhabisEngine.applyAction(state, playerIndex, action)
        }
    }

    fun computeBotAction(state: ActiveGameState): String? {
        if (state.winnerIndex != null) return null
        return when (state) {
            is MenchClassicState -> MenchClassicEngine.botAction(state)
            is SnakesLaddersState -> SnakesLaddersEngine.botAction(state)
            is PachisiState -> PachisiEngine.botAction(state)
            is BackgammonState -> BackgammonEngine.botAction(state)
            is HokmState -> HokmEngine.botAction(state)
            is ShelemState -> ShelemEngine.botAction(state)
            is ChaharBargState -> ChaharBargEngine.botAction(state)
            is HaftKhabisState -> HaftKhabisEngine.botAction(state)
        }
    }
}
