package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.games.JsonHelpers.toNestedIntList
import com.example.games.JsonHelpers.toNestedJsonArray
import com.example.model.GameType
import org.json.JSONObject
import kotlin.random.Random

// ============================================================================
// 1. OLD-SCHOOL VINTAGE IRANIAN MENCH (منچ قدیم ۴۰ خانه‌ای)
// Progress per pawn: -1 = corner yard (خانه شروع), 0..39 = 40 outer circles,
// 40..43 = 4 colored home destination holes (خانه‌های مقصد وسط)
// ============================================================================
data class MenchClassicState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val pawns: List<List<Int>>,
    val diceValue: Int,
    val hasRolled: Boolean,
    val rollCount: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.MENCH_CLASSIC

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("pawns", pawns.toNestedJsonArray())
        put("diceValue", diceValue)
        put("hasRolled", hasRolled)
        put("rollCount", rollCount)
    }.toString()
}

object MenchClassicEngine {
    val START_OFFSETS = listOf(0, 10, 20, 30)

    fun create(playerCount: Int): MenchClassicState {
        // Start with 1 pawn on entry hole (0) and 3 in corner yard (-1)
        val pawns = List(playerCount) { listOf(0, -1, -1, -1) }
        return MenchClassicState(
            playerCount = playerCount,
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "منچ قدیم ۴۰ خانه‌ای آغاز شد — تاس بریزید! 🎲",
            pawns = pawns,
            diceValue = 6,
            hasRolled = false,
            rollCount = 0
        )
    }

    fun fromJson(json: JSONObject): MenchClassicState {
        val w = json.optInt("winnerIndex", -99)
        return MenchClassicState(
            playerCount = json.optInt("playerCount", 2),
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            pawns = json.optJSONArray("pawns")?.toNestedIntList() ?: List(2) { listOf(0, -1, -1, -1) },
            diceValue = json.optInt("diceValue", 1),
            hasRolled = json.optBoolean("hasRolled", false),
            rollCount = json.optInt("rollCount", 0)
        )
    }

    fun seatForPlayer(playerIndex: Int, playerCount: Int): Int =
        if (playerCount == 2 && playerIndex == 1) 2 else playerIndex

    fun globalHole(playerIndex: Int, playerCount: Int, progress: Int): Int? {
        if (progress !in 0..39) return null
        val seat = seatForPlayer(playerIndex, playerCount)
        return (START_OFFSETS[seat] + progress) % 40
    }

    fun movablePawns(state: MenchClassicState): List<Int> {
        if (!state.hasRolled || state.winnerIndex != null) return emptyList()
        val myPawns = state.pawns[state.currentTurn]
        val d = state.diceValue
        return myPawns.indices.filter { idx ->
            val cur = myPawns[idx]
            val target = when {
                cur == -1 -> if (d == 6) 0 else -99
                else -> cur + d
            }
            if (target !in 0..43) return@filter false
            // Classic Iranian Mench rule: cannot land on your own pawn!
            val occupiedBySelf = myPawns.indices.any { other -> other != idx && myPawns[other] == target }
            !occupiedBySelf
        }
    }

    fun applyAction(state: MenchClassicState, playerIndex: Int, action: JSONObject): MenchClassicState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "ROLL" -> {
                if (state.hasRolled) return state
                val rolled = Random.nextInt(1, 7)
                val nextS = state.copy(diceValue = rolled, hasRolled = true, rollCount = state.rollCount + 1)
                val valid = movablePawns(nextS)
                when {
                    valid.isEmpty() -> {
                        val nextTurn = if (rolled == 6) state.currentTurn else (state.currentTurn + 1) % state.playerCount
                        nextS.copy(
                            currentTurn = nextTurn,
                            hasRolled = false,
                            statusFa = "تاس $rolled آمد، حرکتی مجاز نیست — نوبت بازیکن ${nextTurn + 1}"
                        )
                    }
                    valid.size == 1 -> movePawn(nextS, valid.first())
                    else -> nextS.copy(statusFa = "عدد $rolled آمد! یکی از مهره‌های درخشان را انتخاب کنید ✨")
                }
            }
            "MOVE" -> {
                if (!state.hasRolled) return state
                val pawnIdx = action.optInt("pawn", -1)
                if (pawnIdx !in movablePawns(state)) return state
                movePawn(state, pawnIdx)
            }
            else -> state
        }
    }

    private fun movePawn(state: MenchClassicState, pawnIdx: Int): MenchClassicState {
        val turn = state.currentTurn
        val d = state.diceValue
        val updated = state.pawns.map { it.toMutableList() }
        val cur = updated[turn][pawnIdx]
        val target = if (cur == -1) 0 else cur + d
        updated[turn][pawnIdx] = target

        var captured = false
        val landedHole = globalHole(turn, state.playerCount, target)
        if (landedHole != null) {
            for (opp in 0 until state.playerCount) {
                if (opp == turn) continue
                for (k in 0 until 4) {
                    if (globalHole(opp, state.playerCount, updated[opp][k]) == landedHole) {
                        updated[opp][k] = -1
                        captured = true
                    }
                }
            }
        }

        val allHome = updated[turn].all { it in 40..43 }
        if (allHome) {
            return state.copy(
                pawns = updated,
                hasRolled = false,
                winnerIndex = turn,
                statusFa = "بازیکن ${turn + 1} هر ۴ مهره را به خانه رساند و قهرمان منچ شد! 🏆"
            )
        }

        val enteredHome = cur < 40 && target >= 40
        val bonus = d == 6 || captured
        val nextTurn = if (bonus) turn else (turn + 1) % state.playerCount
        val msg = when {
            captured -> "مهره حریف زده شد! 💥 جایزه تاس دوباره برای بازیکن ${turn + 1}"
            enteredHome && d == 6 -> "مهره به خانه نهایی رفت و ۶ آمد! 👑 دوباره تاس بریزید"
            enteredHome -> "یک مهره با موفقیت وارد خانه نهایی شد! 🏠 نوبت بازیکن ${nextTurn + 1}"
            d == 6 -> "۶ آمد! 🎉 دوباره تاس بریزید"
            else -> "نوبت بازیکن ${nextTurn + 1}"
        }
        return state.copy(
            pawns = updated,
            currentTurn = nextTurn,
            hasRolled = false,
            statusFa = msg
        )
    }

    fun botAction(state: MenchClassicState): String {
        if (!state.hasRolled) return JSONObject().put("op", "ROLL").toString()
        val valid = movablePawns(state)
        if (valid.isEmpty()) return JSONObject().put("op", "ROLL").toString()
        val turn = state.currentTurn
        val d = state.diceValue
        val best = valid.maxByOrNull { idx ->
            val cur = state.pawns[turn][idx]
            val target = if (cur == -1) 0 else cur + d
            val gHole = globalHole(turn, state.playerCount, target)
            var score = target
            if (cur == -1) score += 45
            if (target in 40..43) score += 120
            if (gHole != null) {
                val hitsOpp = (0 until state.playerCount).any { opp ->
                    opp != turn && state.pawns[opp].any { globalHole(opp, state.playerCount, it) == gHole }
                }
                if (hitsOpp) score += 260
            }
            score
        } ?: valid.first()
        return JSONObject().put("op", "MOVE").put("pawn", best).toString()
    }
}

// ============================================================================
// 2. SNAKES & LADDERS 3D (مار و پله سه‌بعدی ۱۰۰ خانه‌ای)
// ============================================================================
data class SnakesLaddersState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val positions: List<Int>,
    val diceValue: Int,
    val rollCount: Int,
    val lastJumpFrom: Int?,
    val lastJumpTo: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.SNAKES_LADDERS

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("positions", positions.toJsonArray())
        put("diceValue", diceValue)
        put("rollCount", rollCount)
        put("lastJumpFrom", lastJumpFrom ?: -1)
        put("lastJumpTo", lastJumpTo ?: -1)
    }.toString()
}

object SnakesLaddersEngine {
    val LADDERS = mapOf(
        4 to 25,
        13 to 46,
        33 to 49,
        42 to 63,
        50 to 69,
        62 to 81,
        74 to 92
    )
    val SNAKES = mapOf(
        27 to 7,
        40 to 3,
        43 to 18,
        54 to 31,
        66 to 45,
        76 to 58,
        89 to 53,
        98 to 41
    )

    fun create(playerCount: Int): SnakesLaddersState = SnakesLaddersState(
        playerCount = playerCount,
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "مار و پله سه‌بعدی آغاز شد! نوبت بازیکن ۱ 🎲",
        positions = List(playerCount) { 1 },
        diceValue = 1,
        rollCount = 0,
        lastJumpFrom = null,
        lastJumpTo = null
    )

    fun fromJson(json: JSONObject): SnakesLaddersState {
        val w = json.optInt("winnerIndex", -99)
        val jf = json.optInt("lastJumpFrom", -1)
        val jt = json.optInt("lastJumpTo", -1)
        return SnakesLaddersState(
            playerCount = json.optInt("playerCount", 2),
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            positions = json.optJSONArray("positions")?.toIntList() ?: listOf(1, 1),
            diceValue = json.optInt("diceValue", 1),
            rollCount = json.optInt("rollCount", 0),
            lastJumpFrom = if (jf == -1) null else jf,
            lastJumpTo = if (jt == -1) null else jt
        )
    }

    fun applyAction(state: SnakesLaddersState, playerIndex: Int, action: JSONObject): SnakesLaddersState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        if (action.optString("op") != "ROLL") return state

        val rolled = Random.nextInt(1, 7)
        val updated = state.positions.toMutableList()
        var nextPos = updated[playerIndex] + rolled
        if (nextPos > 100) nextPos = 100 - (nextPos - 100)

        var jf: Int? = null
        var jt: Int? = null
        var msg = "بازیکن ${playerIndex + 1} عدد $rolled آورد و به خانه $nextPos رفت"

        LADDERS[nextPos]?.let { dest ->
            jf = nextPos; jt = dest; nextPos = dest
            msg = "نردبان چوبی! 🪜 صعود از خانه $jf به $dest"
        } ?: SNAKES[nextPos]?.let { dest ->
            jf = nextPos; jt = dest; nextPos = dest
            msg = "نیش مار! 🐍 سقوط از خانه $jf به $dest"
        }

        updated[playerIndex] = nextPos
        if (nextPos == 100) {
            return state.copy(
                positions = updated,
                diceValue = rolled,
                rollCount = state.rollCount + 1,
                winnerIndex = playerIndex,
                lastJumpFrom = jf,
                lastJumpTo = jt,
                statusFa = "بازیکن ${playerIndex + 1} به قله ۱۰۰ رسید و قهرمان شد! 🏆"
            )
        }
        val nextTurn = if (rolled == 6) playerIndex else (playerIndex + 1) % state.playerCount
        return state.copy(
            positions = updated,
            currentTurn = nextTurn,
            diceValue = rolled,
            rollCount = state.rollCount + 1,
            lastJumpFrom = jf,
            lastJumpTo = jt,
            statusFa = msg + if (rolled == 6) " — جایزه ۶: دوباره تاس بریزید!" else ""
        )
    }

    fun botAction(state: SnakesLaddersState): String = JSONObject().put("op", "ROLL").toString()
}

// ============================================================================
// 3. ROYAL PACHISI (پچسی سلطنتی ۵۲ خانه‌ای با ۸ خانه امن ستاره‌دار)
// ============================================================================
data class PachisiState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val pawns: List<List<Int>>, // -1 yard, 0..50 track, 51..55 home stretch, 56 crown
    val diceValue: Int,
    val hasRolled: Boolean,
    val rollCount: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.PACHISI_ROYAL

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("pawns", pawns.toNestedJsonArray())
        put("diceValue", diceValue)
        put("hasRolled", hasRolled)
        put("rollCount", rollCount)
    }.toString()
}

object PachisiEngine {
    val START_OFFSETS = listOf(0, 13, 26, 39)
    val SAFE_TILES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun create(playerCount: Int): PachisiState = PachisiState(
        playerCount = playerCount,
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "پچسی ستاره‌دار شروع شد! خانه‌های ستاره‌دار امن هستند ⭐",
        pawns = List(playerCount) { listOf(0, -1, -1, -1) },
        diceValue = 6,
        hasRolled = false,
        rollCount = 0
    )

    fun fromJson(json: JSONObject): PachisiState {
        val w = json.optInt("winnerIndex", -99)
        return PachisiState(
            playerCount = json.optInt("playerCount", 2),
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            pawns = json.optJSONArray("pawns")?.toNestedIntList() ?: List(2) { listOf(0, -1, -1, -1) },
            diceValue = json.optInt("diceValue", 1),
            hasRolled = json.optBoolean("hasRolled", false),
            rollCount = json.optInt("rollCount", 0)
        )
    }

    fun seatForPlayer(playerIndex: Int, playerCount: Int): Int =
        if (playerCount == 2 && playerIndex == 1) 2 else playerIndex

    fun globalTile(playerIndex: Int, playerCount: Int, progress: Int): Int? {
        if (progress !in 0..50) return null
        return (START_OFFSETS[seatForPlayer(playerIndex, playerCount)] + progress) % 52
    }

    fun movablePawns(state: PachisiState): List<Int> {
        if (!state.hasRolled || state.winnerIndex != null) return emptyList()
        val my = state.pawns[state.currentTurn]
        val d = state.diceValue
        return my.indices.filter { idx ->
            val p = my[idx]
            when {
                p == -1 -> d == 6
                p == 56 -> false
                else -> p + d <= 56
            }
        }
    }

    fun applyAction(state: PachisiState, playerIndex: Int, action: JSONObject): PachisiState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "ROLL" -> {
                if (state.hasRolled) return state
                val rolled = Random.nextInt(1, 7)
                val nextS = state.copy(diceValue = rolled, hasRolled = true, rollCount = state.rollCount + 1)
                val valid = movablePawns(nextS)
                when {
                    valid.isEmpty() -> {
                        val next = (state.currentTurn + 1) % state.playerCount
                        nextS.copy(currentTurn = next, hasRolled = false, statusFa = "تاس $rolled آمد — نوبت بازیکن ${next + 1}")
                    }
                    valid.size == 1 -> movePawn(nextS, valid.first())
                    else -> nextS.copy(statusFa = "عدد $rolled آمد! مهره دلخواه را انتخاب کنید ✨")
                }
            }
            "MOVE" -> {
                if (!state.hasRolled) return state
                val idx = action.optInt("pawn", -1)
                if (idx !in movablePawns(state)) return state
                movePawn(state, idx)
            }
            else -> state
        }
    }

    private fun movePawn(state: PachisiState, pawnIdx: Int): PachisiState {
        val turn = state.currentTurn
        val d = state.diceValue
        val updated = state.pawns.map { it.toMutableList() }
        val cur = updated[turn][pawnIdx]
        val target = if (cur == -1) 0 else cur + d
        updated[turn][pawnIdx] = target

        var captured = false
        val landed = globalTile(turn, state.playerCount, target)
        if (landed != null && landed !in SAFE_TILES) {
            for (opp in 0 until state.playerCount) {
                if (opp == turn) continue
                for (k in 0 until 4) {
                    if (globalTile(opp, state.playerCount, updated[opp][k]) == landed) {
                        updated[opp][k] = -1
                        captured = true
                    }
                }
            }
        }
        val reached = target == 56
        if (updated[turn].all { it == 56 }) {
            return state.copy(pawns = updated, hasRolled = false, winnerIndex = turn, statusFa = "بازیکن ${turn + 1} قهرمان پچسی سلطنتی شد! 🏆")
        }
        val bonus = d == 6 || captured || reached
        val next = if (bonus) turn else (turn + 1) % state.playerCount
        return state.copy(
            pawns = updated,
            currentTurn = next,
            hasRolled = false,
            statusFa = when {
                captured -> "مهره حریف زده شد! 💥 جایزه تاس دوباره"
                reached -> "مهره به تاج وسط رسید! 👑 جایزه تاس دوباره"
                d == 6 -> "۶ آمد! 🎉 دوباره تاس بریزید"
                else -> "نوبت بازیکن ${next + 1}"
            }
        )
    }

    fun botAction(state: PachisiState): String {
        if (!state.hasRolled) return JSONObject().put("op", "ROLL").toString()
        val valid = movablePawns(state)
        if (valid.isEmpty()) return JSONObject().put("op", "ROLL").toString()
        val best = valid.maxByOrNull { idx ->
            val cur = state.pawns[state.currentTurn][idx]
            val target = if (cur == -1) 0 else cur + state.diceValue
            var sc = target
            if (cur == -1) sc += 45
            if (target == 56) sc += 200
            val g = globalTile(state.currentTurn, state.playerCount, target)
            if (g != null && g in SAFE_TILES) sc += 30
            sc
        } ?: valid.first()
        return JSONObject().put("op", "MOVE").put("pawn", best).toString()
    }
}

// ============================================================================
// 4. PERSIAN BACKGAMMON (تخته نرد ایرانی سه‌بعدی)
// Points 0..23:
//   Player 0 (positive counts): moves 0 -> 23, bears off > 23 (home = 18..23)
//   Player 1 (negative counts): moves 23 -> 0, bears off < 0 (home = 0..5)
// ============================================================================
data class BackgammonMove(val from: Int, val to: Int, val dieUsed: Int) // from = -1 means bar; to = 24 or -2 means bear-off

data class BackgammonState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val points: List<Int>, // 24 points: >0 P0 checkers, <0 P1 checkers
    val barP0: Int,
    val barP1: Int,
    val offP0: Int,
    val offP1: Int,
    val dice: List<Int>, // 2 rolled dice values for display
    val remainingMoves: List<Int>, // remaining die values to spend
    val selectedFrom: Int?, // null, -1 (bar), or 0..23
    val rollCount: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.BACKGAMMON

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("points", points.toJsonArray())
        put("barP0", barP0)
        put("barP1", barP1)
        put("offP0", offP0)
        put("offP1", offP1)
        put("dice", dice.toJsonArray())
        put("remainingMoves", remainingMoves.toJsonArray())
        put("selectedFrom", selectedFrom ?: -99)
        put("rollCount", rollCount)
    }.toString()
}

object BackgammonEngine {
    fun create(): BackgammonState {
        val pts = MutableList(24) { 0 }
        // Standard Backgammon setup
        pts[0] = 2; pts[11] = 5; pts[16] = 3; pts[18] = 5
        pts[23] = -2; pts[12] = -5; pts[7] = -3; pts[5] = -5
        return BackgammonState(
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "تخته نرد ایرانی آغاز شد! برای شروع تاس بریزید 🎲",
            points = pts,
            barP0 = 0,
            barP1 = 0,
            offP0 = 0,
            offP1 = 0,
            dice = listOf(6, 5),
            remainingMoves = emptyList(),
            selectedFrom = null,
            rollCount = 0
        )
    }

    fun fromJson(json: JSONObject): BackgammonState {
        val w = json.optInt("winnerIndex", -99)
        val sel = json.optInt("selectedFrom", -99)
        return BackgammonState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            points = json.optJSONArray("points")?.toIntList() ?: create().points,
            barP0 = json.optInt("barP0", 0),
            barP1 = json.optInt("barP1", 0),
            offP0 = json.optInt("offP0", 0),
            offP1 = json.optInt("offP1", 0),
            dice = json.optJSONArray("dice")?.toIntList() ?: listOf(1, 1),
            remainingMoves = json.optJSONArray("remainingMoves")?.toIntList().orEmpty(),
            selectedFrom = if (sel == -99) null else sel,
            rollCount = json.optInt("rollCount", 0)
        )
    }

    private fun canBearOff(state: BackgammonState, player: Int): Boolean {
        return if (player == 0) {
            state.barP0 == 0 && (0..17).all { state.points[it] <= 0 }
        } else {
            state.barP1 == 0 && (6..23).all { state.points[it] >= 0 }
        }
    }

    fun legalMoves(state: BackgammonState): List<BackgammonMove> {
        if (state.remainingMoves.isEmpty() || state.winnerIndex != null) return emptyList()
        val p = state.currentTurn
        val distinctDice = state.remainingMoves.distinct()
        val moves = mutableListOf<BackgammonMove>()

        val myBar = if (p == 0) state.barP0 else state.barP1
        if (myBar > 0) {
            for (d in distinctDice) {
                val target = if (p == 0) d - 1 else 24 - d
                val destVal = state.points[target]
                val open = if (p == 0) destVal >= -1 else destVal <= 1
                if (open) moves.add(BackgammonMove(-1, target, d))
            }
            return moves
        }

        val bearOffAllowed = canBearOff(state, p)
        for (i in 0 until 24) {
            val hasMine = if (p == 0) state.points[i] > 0 else state.points[i] < 0
            if (!hasMine) continue
            for (d in distinctDice) {
                val target = if (p == 0) i + d else i - d
                if (target in 0..23) {
                    val destVal = state.points[target]
                    val open = if (p == 0) destVal >= -1 else destVal <= 1
                    if (open) moves.add(BackgammonMove(i, target, d))
                } else if (bearOffAllowed) {
                    val exactOrFurthest = if (p == 0) {
                        target == 24 || (18 until i).all { state.points[it] <= 0 }
                    } else {
                        target == -1 || (i + 1..5).all { state.points[it] >= 0 }
                    }
                    if (exactOrFurthest) {
                        moves.add(BackgammonMove(i, if (p == 0) 24 else -2, d))
                    }
                }
            }
        }
        return moves
    }

    fun applyAction(state: BackgammonState, playerIndex: Int, action: JSONObject): BackgammonState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "ROLL" -> {
                if (state.remainingMoves.isNotEmpty()) return state
                val d1 = Random.nextInt(1, 7)
                val d2 = Random.nextInt(1, 7)
                val rem = if (d1 == d2) listOf(d1, d1, d1, d1) else listOf(d1, d2)
                val rolledState = state.copy(
                    dice = listOf(d1, d2),
                    remainingMoves = rem,
                    selectedFrom = null,
                    rollCount = state.rollCount + 1
                )
                val legal = legalMoves(rolledState)
                if (legal.isEmpty()) {
                    val next = 1 - state.currentTurn
                    rolledState.copy(
                        currentTurn = next,
                        remainingMoves = emptyList(),
                        statusFa = "تاس ($d1 و $d2) حرکتی ندارد — نوبت بازیکن ${next + 1}"
                    )
                } else {
                    val doubleTxt = if (d1 == d2) "جفت $d1! ۴ حرکت 🎉" else "تاس $d1 و $d2"
                    val autoSel = if ((state.currentTurn == 0 && state.barP0 > 0) || (state.currentTurn == 1 && state.barP1 > 0)) -1 else null
                    rolledState.copy(
                        selectedFrom = autoSel,
                        statusFa = "$doubleTxt — مهره خود را انتخاب و حرکت دهید"
                    )
                }
            }
            "TAP_POINT" -> {
                if (state.remainingMoves.isEmpty()) return state
                val pt = action.optInt("pt", -99) // -1 bar, 0..23 point, 24/-2 bear off
                val legal = legalMoves(state)
                val sel = state.selectedFrom
                if (sel != null) {
                    val chosen = legal.firstOrNull { it.from == sel && it.to == pt }
                    if (chosen != null) return executeMove(state, chosen)
                }
                // Select source if it has legal moves
                val fromMoves = legal.filter { it.from == pt }
                if (fromMoves.size == 1 && sel == pt) {
                    return executeMove(state, fromMoves.first())
                }
                if (fromMoves.isNotEmpty()) {
                    return state.copy(selectedFrom = pt)
                }
                state.copy(selectedFrom = null)
            }
            "MOVE" -> {
                val from = action.optInt("from")
                val to = action.optInt("to")
                val chosen = legalMoves(state).firstOrNull { it.from == from && it.to == to } ?: return state
                executeMove(state, chosen)
            }
            else -> state
        }
    }

    private fun executeMove(state: BackgammonState, move: BackgammonMove): BackgammonState {
        val p = state.currentTurn
        val pts = state.points.toMutableList()
        var bar0 = state.barP0
        var bar1 = state.barP1
        var off0 = state.offP0
        var off1 = state.offP1

        if (move.from == -1) {
            if (p == 0) bar0-- else bar1--
        } else {
            pts[move.from] = if (p == 0) pts[move.from] - 1 else pts[move.from] + 1
        }

        var hit = false
        if (move.to == 24 || move.to == -2) {
            if (p == 0) off0++ else off1++
        } else {
            if (p == 0 && pts[move.to] == -1) {
                pts[move.to] = 1
                bar1++
                hit = true
            } else if (p == 1 && pts[move.to] == 1) {
                pts[move.to] = -1
                bar0++
                hit = true
            } else {
                pts[move.to] = if (p == 0) pts[move.to] + 1 else pts[move.to] - 1
            }
        }

        if (off0 >= 15 || off1 >= 15) {
            return state.copy(
                points = pts, barP0 = bar0, barP1 = bar1, offP0 = off0, offP1 = off1,
                remainingMoves = emptyList(), selectedFrom = null,
                winnerIndex = p,
                statusFa = "بازیکن ${p + 1} همه ۱۵ مهره را خارج کرد و قهرمان تخته نرد شد! 🏆"
            )
        }

        val rem = state.remainingMoves.toMutableList()
        rem.remove(move.dieUsed)
        val after = state.copy(
            points = pts, barP0 = bar0, barP1 = bar1, offP0 = off0, offP1 = off1,
            remainingMoves = rem, selectedFrom = null
        )
        val nextLegal = legalMoves(after)
        return if (rem.isEmpty() || nextLegal.isEmpty()) {
            val next = 1 - p
            after.copy(
                currentTurn = next,
                remainingMoves = emptyList(),
                statusFa = if (hit) "مهره حریف زده شد! 💥 نوبت بازیکن ${next + 1}" else "نوبت بازیکن ${next + 1} — تاس بریزید 🎲"
            )
        } else {
            after.copy(
                statusFa = if (hit) "مهره حریف زده شد! 💥 حرکت بعدی (${rem.joinToString(" و ")})"
                else "حرکت باقی‌مانده: ${rem.joinToString(" و ")}"
            )
        }
    }

    fun botAction(state: BackgammonState): String? {
        if (state.remainingMoves.isEmpty()) return JSONObject().put("op", "ROLL").toString()
        val legal = legalMoves(state)
        if (legal.isEmpty()) return null
        val p = state.currentTurn
        val best = legal.maxByOrNull { m ->
            var score = Random.nextInt(8)
            if (m.to == 24 || m.to == -2) score += 120
            else {
                val dest = state.points[m.to]
                if ((p == 0 && dest == -1) || (p == 1 && dest == 1)) score += 90 // Hit blot!
                if ((p == 0 && dest >= 1) || (p == 1 && dest <= -1)) score += 35 // Make safe point!
                score += if (p == 0) m.to else (23 - m.to)
            }
            score
        } ?: legal.first()
        return JSONObject().put("op", "MOVE").put("from", best.from).put("to", best.to).toString()
    }
}
