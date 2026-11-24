package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.games.ActiveGameState
import com.example.games.GameEngineDispatcher
import com.example.games.HokmState
import com.example.games.ShelemState
import com.example.model.AppScreen
import com.example.model.ChatMessage
import com.example.model.DiscoveredRoom
import com.example.model.GameType
import com.example.model.PlayerInfo
import com.example.model.RoomMode
import com.example.model.UserProfile
import com.example.model.VoiceChatState
import com.example.network.NetworkEvent
import com.example.network.WifiMultiplayerManager
import com.example.network.WifiVoiceChatEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

data class AppUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val profile: UserProfile,
    val localIp: String = "127.0.0.1",
    val discoveredRooms: List<DiscoveredRoom> = emptyList(),
    val roomMode: RoomMode = RoomMode.SOLO_VS_BOT,
    val roomName: String = "اتاق دورهمی",
    val hostIpConnected: String = "",
    val selectedGame: GameType = GameType.MENCH_CLASSIC,
    val players: List<PlayerInfo> = emptyList(),
    val activeGameState: ActiveGameState? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val unreadChatCount: Int = 0,
    val isChatSheetOpen: Boolean = false,
    val voiceState: VoiceChatState = VoiceChatState(),
    val connectionMessage: String? = null,
    val isConnecting: Boolean = false
) {
    val localPlayerIndex: Int
        get() = players.indexOfFirst { it.id == profile.id }.coerceAtLeast(0)

    val isHostOrLocal: Boolean
        get() = roomMode != RoomMode.WIFI_CLIENT

    val isWifiSession: Boolean
        get() = roomMode == RoomMode.WIFI_HOST || roomMode == RoomMode.WIFI_CLIENT
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("dorehami_wifi_prefs", Context.MODE_PRIVATE)
    private val wifiManager = WifiMultiplayerManager(application)
    private val voiceEngine = WifiVoiceChatEngine()

    private val _uiState = MutableStateFlow(
        AppUiState(
            profile = loadProfile(),
            localIp = wifiManager.refreshLocalIp()
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var botTurnJob: Job? = null
    private var specialTimerJob: Job? = null
    private var winRecordedForRound: Boolean = false

    init {
        wifiManager.startScanningRooms()

        viewModelScope.launch {
            wifiManager.discoveredRooms.collect { rooms ->
                _uiState.update { it.copy(discoveredRooms = rooms) }
            }
        }
        viewModelScope.launch {
            wifiManager.localIp.collect { ip ->
                _uiState.update { it.copy(localIp = ip) }
            }
        }
        viewModelScope.launch {
            voiceEngine.localMicLevel.collect { level ->
                _uiState.update { it.copy(voiceState = it.voiceState.copy(localMicLevel = level)) }
            }
        }
        viewModelScope.launch {
            voiceEngine.remoteSpeakerId.collect { speakerId ->
                val name = _uiState.value.players.firstOrNull { it.id == speakerId }?.name
                _uiState.update { state ->
                    state.copy(
                        voiceState = state.voiceState.copy(activeSpeakerName = name),
                        players = state.players.map { p ->
                            if (p.id == state.profile.id) p
                            else p.copy(isSpeaking = (speakerId != null && p.id == speakerId))
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            wifiManager.events.collect { event ->
                handleNetworkEvent(event)
            }
        }
    }

    private fun loadProfile(): UserProfile {
        val savedId = prefs.getString("player_id", null) ?: UUID.randomUUID().toString().take(8).also {
            prefs.edit().putString("player_id", it).apply()
        }
        val savedName = prefs.getString("player_name", null) ?: "بازیکن ${(10..99).random()}"
        return UserProfile(
            id = savedId,
            name = savedName,
            avatarIndex = prefs.getInt("avatar_idx", 0),
            favoriteColorIndex = prefs.getInt("color_idx", 0),
            totalWins = prefs.getInt("total_wins", 0),
            gamesPlayed = prefs.getInt("games_played", 0)
        )
    }

    fun updateProfile(name: String, avatarIndex: Int, favoriteColorIndex: Int) {
        val cleanName = name.trim().ifBlank { "بازیکن" }.take(18)
        prefs.edit()
            .putString("player_name", cleanName)
            .putInt("avatar_idx", avatarIndex)
            .putInt("color_idx", favoriteColorIndex)
            .apply()
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    name = cleanName,
                    avatarIndex = avatarIndex,
                    favoriteColorIndex = favoriteColorIndex
                )
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.WifiHub) {
            wifiManager.refreshLocalIp()
            wifiManager.startScanningRooms()
        }
        _uiState.update { it.copy(currentScreen = screen, connectionMessage = null) }
    }

    fun clearConnectionMessage() {
        _uiState.update { it.copy(connectionMessage = null) }
    }

    fun setChatSheetOpen(open: Boolean) {
        _uiState.update {
            it.copy(
                isChatSheetOpen = open,
                unreadChatCount = if (open) 0 else it.unreadChatCount
            )
        }
    }

    // -------------------------------------------------------------------------
    // Room Hosting / Joining / Local Launch
    // -------------------------------------------------------------------------
    fun hostWifiRoom(roomName: String, initialGame: GameType) {
        val profile = _uiState.value.profile
        val cleanRoomName = roomName.trim().ifBlank { "اتاق ${profile.name}" }
        val hostPlayer = PlayerInfo(
            id = profile.id,
            name = profile.name,
            avatarIndex = profile.avatarIndex,
            colorIndex = 0,
            isHost = true,
            isBot = false,
            isReady = true,
            ipAddress = wifiManager.refreshLocalIp()
        )
        val result = wifiManager.startHostingRoom(cleanRoomName, hostPlayer, initialGame)
        if (result.isSuccess) {
            val ip = result.getOrNull().orEmpty()
            voiceEngine.startSession(playerId = profile.id, asHost = true)
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.RoomLobby,
                    roomMode = RoomMode.WIFI_HOST,
                    roomName = cleanRoomName,
                    hostIpConnected = ip,
                    localIp = ip,
                    selectedGame = initialGame,
                    players = listOf(hostPlayer),
                    activeGameState = null,
                    chatMessages = listOf(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderId = "system",
                            senderName = "اتاق وای‌فای",
                            senderColorIndex = 0,
                            text = "اتاق روی آی‌پی $ip ساخته شد! دوستانتان روی همین وای‌فای می‌توانند متصل شوند.",
                            isSystem = true
                        )
                    ),
                    unreadChatCount = 0,
                    connectionMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(connectionMessage = "خطا در راه‌اندازی سرور وای‌فای. اتصال وای‌فای یا هات‌اسپات را بررسی کنید.")
            }
        }
    }

    fun joinWifiRoom(hostIp: String, port: Int = WifiMultiplayerManager.GAME_TCP_PORT) {
        val trimmedIp = hostIp.trim()
        if (trimmedIp.isBlank()) {
            _uiState.update { it.copy(connectionMessage = "لطفاً آدرس آی‌پی میزبان را وارد کنید.") }
            return
        }
        val profile = _uiState.value.profile
        val localPlayer = PlayerInfo(
            id = profile.id,
            name = profile.name,
            avatarIndex = profile.avatarIndex,
            colorIndex = profile.favoriteColorIndex,
            isHost = false
        )
        _uiState.update { it.copy(isConnecting = true, connectionMessage = null) }
        wifiManager.joinRoom(
            hostIp = trimmedIp,
            port = port,
            localPlayer = localPlayer,
            onConnected = {
                voiceEngine.startSession(playerId = profile.id, asHost = false, hostIp = trimmedIp)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        currentScreen = AppScreen.RoomLobby,
                        roomMode = RoomMode.WIFI_CLIENT,
                        hostIpConnected = trimmedIp,
                        chatMessages = listOf(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                senderId = "system",
                                senderName = "اتاق وای‌فای",
                                senderColorIndex = 1,
                                text = "اتصال به میزبان $trimmedIp برقرار شد! چت متنی و صوتی فعال است.",
                                isSystem = true
                            )
                        ),
                        unreadChatCount = 0
                    )
                }
            },
            onError = { err ->
                _uiState.update { it.copy(isConnecting = false, connectionMessage = err) }
            }
        )
    }

    fun startQuickLocalGame(gameType: GameType, mode: RoomMode, playerCount: Int) {
        leaveCurrentRoom(navigateHome = false)
        val clampedCount = playerCount.coerceIn(gameType.minPlayers, gameType.maxPlayers)
        val profile = _uiState.value.profile
        val botNames = listOf("هوشمند آریا", "هوشمند سارا", "هوشمند کیان")
        val humanNames = listOf("بازیکن ۲", "بازیکن ۳", "بازیکن ۴")

        val players = buildList {
            add(
                PlayerInfo(
                    id = profile.id,
                    name = profile.name,
                    avatarIndex = profile.avatarIndex,
                    colorIndex = 0,
                    isHost = true,
                    isBot = false
                )
            )
            for (i in 1 until clampedCount) {
                val isBot = mode == RoomMode.SOLO_VS_BOT
                add(
                    PlayerInfo(
                        id = if (isBot) "bot_$i" else "local_$i",
                        name = if (isBot) botNames[(i - 1) % botNames.size] else humanNames[(i - 1) % humanNames.size],
                        avatarIndex = (profile.avatarIndex + i) % 8,
                        colorIndex = i % 4,
                        isHost = false,
                        isBot = isBot
                    )
                )
            }
        }

        val initialState = GameEngineDispatcher.createInitialState(gameType, players.size)
        winRecordedForRound = false
        recordGamePlayed()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.ActiveGame,
                roomMode = mode,
                roomName = if (mode == RoomMode.SOLO_VS_BOT) "رقابت با هوش مصنوعی" else "بازی دورهمی روی یک گوشی",
                selectedGame = gameType,
                players = players,
                activeGameState = initialState,
                chatMessages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderId = "system",
                        senderName = "سالن بازی",
                        senderColorIndex = 0,
                        text = "بازی ${gameType.titleFa} شروع شد! خوش بگذره 🎲",
                        isSystem = true
                    )
                ),
                unreadChatCount = 0
            )
        }
        scheduleSpecialTimersAndBots(initialState)
    }

    fun selectGameInLobby(gameType: GameType) {
        val state = _uiState.value
        if (!state.isHostOrLocal) return
        _uiState.update { it.copy(selectedGame = gameType) }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.updateHostedRoomMetadata(gameType, state.players, isPlaying = false)
        }
    }

    fun addBotToLobby() {
        val state = _uiState.value
        if (!state.isHostOrLocal) return
        val maxP = state.selectedGame.maxPlayers
        if (state.players.size >= maxP) return
        val usedColors = state.players.map { it.colorIndex }.toSet()
        val color = (0..3).firstOrNull { it !in usedColors } ?: (state.players.size % 4)
        val botNum = state.players.count { it.isBot } + 1
        val bot = PlayerInfo(
            id = "bot_${UUID.randomUUID().toString().take(4)}",
            name = "ربات هوشمند $botNum",
            avatarIndex = (botNum + 2) % 8,
            colorIndex = color,
            isHost = false,
            isBot = true
        )
        val updated = state.players + bot
        _uiState.update { it.copy(players = updated) }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.updateHostedRoomMetadata(state.selectedGame, updated, isPlaying = false)
        }
    }

    fun removeBotFromLobby(botId: String) {
        val state = _uiState.value
        if (!state.isHostOrLocal) return
        val updated = state.players.filterNot { it.id == botId && it.isBot }
        _uiState.update { it.copy(players = updated) }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.updateHostedRoomMetadata(state.selectedGame, updated, isPlaying = false)
        }
    }

    fun launchGameFromLobby() {
        val state = _uiState.value
        if (!state.isHostOrLocal) return
        val game = state.selectedGame
        val roster = state.players.take(game.maxPlayers).toMutableList()
        // Auto-fill remaining seats up to game.minPlayers (e.g. 4 for Hokm/Shelem) with smart bots
        var botNum = roster.count { it.isBot } + 1
        while (roster.size < game.minPlayers) {
            val usedColors = roster.map { it.colorIndex }.toSet()
            val col = (0..3).firstOrNull { it !in usedColors } ?: (roster.size % 4)
            roster.add(
                PlayerInfo(
                    id = "bot_auto_$botNum",
                    name = "ربات هوشمند $botNum",
                    avatarIndex = (botNum + 2) % 8,
                    colorIndex = col,
                    isHost = false,
                    isBot = true
                )
            )
            botNum++
        }
        val initial = GameEngineDispatcher.createInitialState(game, roster.size)
        winRecordedForRound = false
        recordGamePlayed()
        _uiState.update {
            it.copy(
                players = roster,
                activeGameState = initial,
                currentScreen = AppScreen.ActiveGame
            )
        }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.updateHostedRoomMetadata(game, roster, isPlaying = true)
            wifiManager.broadcastStartGame(game, initial.toJson())
        }
        scheduleSpecialTimersAndBots(initial)
    }

    fun restartActiveGame() {
        val state = _uiState.value
        if (!state.isHostOrLocal) return
        val initial = GameEngineDispatcher.createInitialState(state.selectedGame, state.players.size)
        winRecordedForRound = false
        recordGamePlayed()
        _uiState.update { it.copy(activeGameState = initial) }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.broadcastGameState(state.selectedGame, initial.toJson())
        }
        scheduleSpecialTimersAndBots(initial)
    }

    fun returnToLobbyOrHome() {
        botTurnJob?.cancel()
        specialTimerJob?.cancel()
        val state = _uiState.value
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.broadcastReturnToLobby()
            _uiState.update { it.copy(currentScreen = AppScreen.RoomLobby, activeGameState = null) }
        } else if (state.roomMode == RoomMode.WIFI_CLIENT) {
            _uiState.update { it.copy(currentScreen = AppScreen.RoomLobby) }
        } else {
            _uiState.update { it.copy(currentScreen = AppScreen.Home, activeGameState = null) }
        }
    }

    fun leaveCurrentRoom(navigateHome: Boolean = true) {
        botTurnJob?.cancel()
        specialTimerJob?.cancel()
        voiceEngine.stopSession()
        wifiManager.closeSession()
        _uiState.update {
            it.copy(
                currentScreen = if (navigateHome) AppScreen.Home else it.currentScreen,
                players = emptyList(),
                activeGameState = null,
                isChatSheetOpen = false,
                voiceState = it.voiceState.copy(
                    isMicActive = false,
                    isOpenMic = false,
                    isPushToTalkPressed = false,
                    localMicLevel = 0f,
                    activeSpeakerName = null
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // Game Actions & Bot / Timer Orchestration
    // -------------------------------------------------------------------------
    fun performGameAction(actionJson: String, explicitPlayerIndex: Int? = null) {
        val state = _uiState.value
        val game = state.activeGameState ?: return

        if (state.roomMode == RoomMode.WIFI_CLIENT) {
            wifiManager.sendClientGameAction(state.profile.id, actionJson)
            return
        }

        val actorIndex = explicitPlayerIndex ?: when (state.roomMode) {
            RoomMode.LOCAL_PASS_AND_PLAY -> game.currentTurn
            RoomMode.SOLO_VS_BOT, RoomMode.WIFI_HOST -> state.localPlayerIndex
            RoomMode.WIFI_CLIENT -> state.localPlayerIndex
        }
        applyAuthoritativeAction(actorIndex, actionJson)
    }

    private fun applyAuthoritativeAction(playerIndex: Int, actionJson: String) {
        val state = _uiState.value
        val current = state.activeGameState ?: return
        val next = GameEngineDispatcher.applyAction(current, playerIndex, actionJson)
        if (next == current) return

        _uiState.update { it.copy(activeGameState = next) }
        if (state.roomMode == RoomMode.WIFI_HOST) {
            wifiManager.broadcastGameState(next.gameType, next.toJson())
        }
        if (next.winnerIndex != null && !winRecordedForRound) {
            winRecordedForRound = true
            val isTeamGame = next is HokmState || next is ShelemState
            val won = if (isTeamGame) next.winnerIndex == (state.localPlayerIndex % 2) else next.winnerIndex == state.localPlayerIndex
            if (won) recordWin()
        }
        scheduleSpecialTimersAndBots(next)
    }

    private fun scheduleSpecialTimersAndBots(gameState: ActiveGameState) {
        botTurnJob?.cancel()
        specialTimerJob?.cancel()

        val ui = _uiState.value
        if (!ui.isHostOrLocal || gameState.winnerIndex != null) return

        // Auto-clear completed 4-card trick after brief visual pause in Hokm & Shelem
        if ((gameState is HokmState && gameState.phase == "TRICK_PAUSE") ||
            (gameState is ShelemState && gameState.phase == "TRICK_PAUSE")
        ) {
            specialTimerJob = viewModelScope.launch {
                delay(1150)
                applyAuthoritativeAction(0, JSONObject().put("op", "CLEAR_TRICK").toString())
            }
            return
        }

        // Standard turn-based AI bot execution
        val turnIdx = gameState.currentTurn
        val activePlayer = ui.players.getOrNull(turnIdx)
        if (activePlayer?.isBot == true) {
            botTurnJob = viewModelScope.launch {
                delay(Random.nextLong(620L, 1080L))
                val latest = _uiState.value.activeGameState ?: return@launch
                if (latest.winnerIndex != null || latest.currentTurn != turnIdx) return@launch
                val botMove = GameEngineDispatcher.computeBotAction(latest) ?: return@launch
                applyAuthoritativeAction(turnIdx, botMove)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chat & Real-Time Voice Chat
    // -------------------------------------------------------------------------
    fun sendChatMessage(text: String, isReaction: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val me = state.players.firstOrNull { it.id == state.profile.id }
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = state.profile.id,
            senderName = state.profile.name,
            senderColorIndex = me?.colorIndex ?: state.profile.favoriteColorIndex,
            text = clean,
            isReaction = isReaction
        )
        if (state.isWifiSession) {
            wifiManager.sendChatMessage(msg)
        } else {
            appendChatMessage(msg)
        }
    }

    private fun appendChatMessage(msg: ChatMessage) {
        _uiState.update { state ->
            val nextList = (state.chatMessages + msg).takeLast(80)
            val unread = if (state.isChatSheetOpen || msg.senderId == state.profile.id) {
                state.unreadChatCount
            } else {
                state.unreadChatCount + 1
            }
            state.copy(chatMessages = nextList, unreadChatCount = unread)
        }
    }

    fun updateRecordPermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                voiceState = it.voiceState.copy(
                    hasRecordPermission = granted,
                    errorMessage = if (granted) null else "دسترسی میکروفون برای چت صوتی تایید نشد."
                )
            )
        }
    }

    fun setPushToTalk(pressed: Boolean) {
        val state = _uiState.value
        if (!state.voiceState.hasRecordPermission) return
        val active = pressed || state.voiceState.isOpenMic
        if (!voiceEngine.isRunning.value && active) {
            voiceEngine.startSession(
                playerId = state.profile.id,
                asHost = state.roomMode != RoomMode.WIFI_CLIENT,
                hostIp = state.hostIpConnected.ifBlank { null }
            )
        }
        val ok = voiceEngine.setTransmitting(active)
        if (state.isWifiSession) {
            wifiManager.sendVoiceStatus(state.profile.id, active && ok)
        }
        _uiState.update { s ->
            s.copy(
                voiceState = s.voiceState.copy(
                    isPushToTalkPressed = pressed,
                    isMicActive = active && ok,
                    errorMessage = if (!ok && active) "میکروفون در دسترس نیست." else null
                ),
                players = s.players.map { p ->
                    if (p.id == s.profile.id) p.copy(isSpeaking = active && ok) else p
                }
            )
        }
    }

    fun toggleOpenMic() {
        val state = _uiState.value
        if (!state.voiceState.hasRecordPermission) return
        val nextOpen = !state.voiceState.isOpenMic
        if (!voiceEngine.isRunning.value && nextOpen) {
            voiceEngine.startSession(
                playerId = state.profile.id,
                asHost = state.roomMode != RoomMode.WIFI_CLIENT,
                hostIp = state.hostIpConnected.ifBlank { null }
            )
        }
        val ok = voiceEngine.setTransmitting(nextOpen)
        if (state.isWifiSession) {
            wifiManager.sendVoiceStatus(state.profile.id, nextOpen && ok)
        }
        _uiState.update { s ->
            s.copy(
                voiceState = s.voiceState.copy(
                    isOpenMic = nextOpen && ok,
                    isMicActive = nextOpen && ok,
                    errorMessage = if (!ok && nextOpen) "میکروفون دستگاه آماده نیست." else null
                ),
                players = s.players.map { p ->
                    if (p.id == s.profile.id) p.copy(isSpeaking = nextOpen && ok) else p
                }
            )
        }
    }

    fun toggleSpeakerMute() {
        val nextMuted = !_uiState.value.voiceState.isSpeakerMuted
        voiceEngine.setSpeakerMuted(nextMuted)
        _uiState.update { it.copy(voiceState = it.voiceState.copy(isSpeakerMuted = nextMuted)) }
    }

    // -------------------------------------------------------------------------
    // Network Event Handling
    // -------------------------------------------------------------------------
    private fun handleNetworkEvent(event: NetworkEvent) {
        when (event) {
            is NetworkEvent.RoomUpdated -> {
                if (_uiState.value.roomMode == RoomMode.WIFI_HOST) {
                    voiceEngine.updatePeers(wifiManager.getConnectedPeerIps())
                }
                _uiState.update {
                    it.copy(
                        roomName = event.roomName,
                        selectedGame = event.gameType,
                        players = event.players
                    )
                }
            }

            is NetworkEvent.ChatReceived -> {
                appendChatMessage(event.message)
            }

            is NetworkEvent.GameStarted -> {
                val parsed = GameEngineDispatcher.deserialize(event.gameType, event.initialStateJson)
                winRecordedForRound = false
                recordGamePlayed()
                _uiState.update {
                    it.copy(
                        selectedGame = event.gameType,
                        activeGameState = parsed,
                        currentScreen = AppScreen.ActiveGame
                    )
                }
            }

            is NetworkEvent.GameStateSynced -> {
                val parsed = GameEngineDispatcher.deserialize(event.gameType, event.stateJson) ?: return
                val state = _uiState.value
                if (parsed.winnerIndex != null && !winRecordedForRound) {
                    winRecordedForRound = true
                    if (parsed.winnerIndex == state.localPlayerIndex) recordWin()
                }
                _uiState.update { it.copy(activeGameState = parsed) }
            }

            is NetworkEvent.ClientActionReceived -> {
                if (_uiState.value.roomMode != RoomMode.WIFI_HOST) return
                val idx = _uiState.value.players.indexOfFirst { it.id == event.senderId }
                if (idx >= 0) {
                    applyAuthoritativeAction(idx, event.actionJson)
                }
            }

            is NetworkEvent.ReturnedToLobby -> {
                _uiState.update { it.copy(currentScreen = AppScreen.RoomLobby, activeGameState = null) }
            }

            is NetworkEvent.VoiceStatusChanged -> {
                _uiState.update { state ->
                    state.copy(
                        players = state.players.map { p ->
                            if (p.id == event.playerId) p.copy(isSpeaking = event.isSpeaking) else p
                        }
                    )
                }
            }

            is NetworkEvent.Disconnected -> {
                voiceEngine.stopSession()
                _uiState.update {
                    it.copy(
                        currentScreen = AppScreen.WifiHub,
                        players = emptyList(),
                        activeGameState = null,
                        connectionMessage = event.reasonFa
                    )
                }
            }
        }
    }

    private fun recordWin() {
        val nextWins = _uiState.value.profile.totalWins + 1
        prefs.edit().putInt("total_wins", nextWins).apply()
        _uiState.update { it.copy(profile = it.profile.copy(totalWins = nextWins)) }
    }

    private fun recordGamePlayed() {
        val nextPlayed = _uiState.value.profile.gamesPlayed + 1
        prefs.edit().putInt("games_played", nextPlayed).apply()
        _uiState.update { it.copy(profile = it.profile.copy(gamesPlayed = nextPlayed)) }
    }

    override fun onCleared() {
        super.onCleared()
        botTurnJob?.cancel()
        specialTimerJob?.cancel()
        voiceEngine.stopSession()
        wifiManager.closeSession()
        wifiManager.stopScanningRooms()
    }
}
