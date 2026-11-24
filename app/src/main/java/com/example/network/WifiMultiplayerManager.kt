package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.ChatMessage
import com.example.model.DiscoveredRoom
import com.example.model.GameType
import com.example.model.PlayerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface NetworkEvent {
    data class RoomUpdated(
        val roomName: String,
        val gameType: GameType,
        val players: List<PlayerInfo>,
        val isPlaying: Boolean
    ) : NetworkEvent

    data class ChatReceived(val message: ChatMessage) : NetworkEvent
    data class GameStateSynced(val gameType: GameType, val stateJson: String) : NetworkEvent
    data class ClientActionReceived(val senderId: String, val actionJson: String) : NetworkEvent
    data class GameStarted(val gameType: GameType, val initialStateJson: String) : NetworkEvent
    data object ReturnedToLobby : NetworkEvent
    data class VoiceStatusChanged(val playerId: String, val isSpeaking: Boolean) : NetworkEvent
    data class Disconnected(val reasonFa: String) : NetworkEvent
}

/**
 * Real LAN Wi-Fi Multiplayer networking layer:
 * - UDP port 48911: Automatic Room Discovery beacons on local Wi-Fi/Hotspot
 * - TCP port 48912: Reliable room roster, text chat, voice status, and real-time game state sync
 */
class WifiMultiplayerManager(
    private val context: Context
) {
    companion object {
        const val DISCOVERY_UDP_PORT = 48911
        const val GAME_TCP_PORT = 48912
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var multicastLock: WifiManager.MulticastLock? = null

    private var discoveryListenJob: Job? = null
    private var discoveryBroadcastJob: Job? = null
    private var discoverySocket: DatagramSocket? = null

    private var serverSocket: ServerSocket? = null
    private var serverAcceptJob: Job? = null
    private val clientWriters = ConcurrentHashMap<String, ClientConnection>()

    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private var clientReadJob: Job? = null

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private val _localIp = MutableStateFlow(WifiNetworkUtils.getLocalWifiIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _events = MutableSharedFlow<NetworkEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NetworkEvent> = _events.asSharedFlow()

    @Volatile
    private var hostedRoomId: String = ""

    @Volatile
    private var hostedRoomName: String = ""

    @Volatile
    private var hostedGameType: GameType = GameType.MENCH_CLASSIC

    @Volatile
    private var hostedPlayers: List<PlayerInfo> = emptyList()

    @Volatile
    private var hostedIsPlaying: Boolean = false

    @Volatile
    var isHost: Boolean = false
        private set

    private data class ClientConnection(
        val playerId: String,
        val socket: Socket,
        val writer: BufferedWriter,
        val ip: String
    )

    fun refreshLocalIp(): String {
        val ip = WifiNetworkUtils.getLocalWifiIpAddress()
        _localIp.value = ip
        return ip
    }

    fun startScanningRooms() {
        if (discoveryListenJob?.isActive == true) return
        refreshLocalIp()
        multicastLock = WifiNetworkUtils.acquireMulticastLock(context)

        discoveryListenJob = scope.launch {
            val roomMap = ConcurrentHashMap<String, DiscoveredRoom>()
            val socket = try {
                DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_UDP_PORT))
                    soTimeout = 1200
                }
            } catch (_: Exception) {
                null
            }
            discoverySocket = socket
            val buf = ByteArray(2048)

            while (isActive) {
                if (socket != null && !socket.isClosed) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val raw = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(raw)
                        if (json.optString("type") == "ROOM_BEACON") {
                            val roomId = json.optString("roomId")
                            if (roomId.isNotBlank() && roomId != hostedRoomId) {
                                val senderIp = packet.address?.hostAddress ?: json.optString("hostIp")
                                val room = DiscoveredRoom(
                                    roomId = roomId,
                                    roomName = json.optString("roomName", "اتاق بازی"),
                                    hostName = json.optString("hostName", "میزبان"),
                                    hostIp = senderIp,
                                    tcpPort = json.optInt("tcpPort", GAME_TCP_PORT),
                                    voicePort = json.optInt("voicePort", WifiVoiceChatEngine.VOICE_UDP_PORT),
                                    gameType = GameType.fromId(json.optString("gameId", GameType.MENCH_CLASSIC.id)),
                                    playerCount = json.optInt("playerCount", 1),
                                    maxPlayers = json.optInt("maxPlayers", 4),
                                    isPlaying = json.optBoolean("isPlaying", false),
                                    lastSeenMs = System.currentTimeMillis()
                                )
                                roomMap[roomId] = room
                            }
                        }
                    } catch (_: Exception) {
                        // Timeout or parse ignore
                    }
                } else {
                    delay(1200)
                }

                // Prune rooms older than 6 seconds
                val now = System.currentTimeMillis()
                val active = roomMap.values.filter { now - it.lastSeenMs < 6500L }
                    .sortedByDescending { it.lastSeenMs }
                roomMap.keys.retainAll(active.map { it.roomId }.toSet())
                _discoveredRooms.value = active
            }
        }
    }

    fun stopScanningRooms() {
        discoveryListenJob?.cancel()
        discoveryListenJob = null
        try {
            discoverySocket?.close()
        } catch (_: Exception) {
        }
        discoverySocket = null
        WifiNetworkUtils.releaseMulticastLock(multicastLock)
        multicastLock = null
    }

    fun startHostingRoom(
        roomName: String,
        hostPlayer: PlayerInfo,
        initialGame: GameType
    ): Result<String> {
        closeSession()
        val ip = refreshLocalIp()
        return try {
            val server = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(GAME_TCP_PORT))
            }
            serverSocket = server
            isHost = true
            hostedRoomId = UUID.randomUUID().toString().take(8)
            hostedRoomName = roomName
            hostedGameType = initialGame
            hostedPlayers = listOf(hostPlayer.copy(isHost = true, colorIndex = 0, ipAddress = ip))
            hostedIsPlaying = false

            startAcceptLoop(server)
            startBeaconBroadcast()
            Result.success(ip)
        } catch (e: Exception) {
            closeSession()
            Result.failure(e)
        }
    }

    fun updateHostedRoomMetadata(
        gameType: GameType,
        players: List<PlayerInfo>,
        isPlaying: Boolean
    ) {
        hostedGameType = gameType
        hostedPlayers = players
        hostedIsPlaying = isPlaying
        if (isHost) {
            broadcastRoomState()
        }
    }

    fun getConnectedPeerIps(): List<String> {
        return clientWriters.values.map { it.ip }.distinct()
    }

    private fun startBeaconBroadcast() {
        discoveryBroadcastJob?.cancel()
        discoveryBroadcastJob = scope.launch {
            val udp = try {
                DatagramSocket().apply { broadcast = true }
            } catch (_: Exception) {
                return@launch
            }
            try {
                while (isActive && isHost) {
                    val hostName = hostedPlayers.firstOrNull { it.isHost }?.name ?: "میزبان"
                    val beacon = JSONObject().apply {
                        put("type", "ROOM_BEACON")
                        put("roomId", hostedRoomId)
                        put("roomName", hostedRoomName)
                        put("hostName", hostName)
                        put("hostIp", refreshLocalIp())
                        put("tcpPort", GAME_TCP_PORT)
                        put("voicePort", WifiVoiceChatEngine.VOICE_UDP_PORT)
                        put("gameId", hostedGameType.id)
                        put("playerCount", hostedPlayers.size)
                        put("maxPlayers", hostedGameType.maxPlayers)
                        put("isPlaying", hostedIsPlaying)
                    }.toString().toByteArray(Charsets.UTF_8)

                    for (bcast in WifiNetworkUtils.getBroadcastAddresses()) {
                        try {
                            udp.send(DatagramPacket(beacon, beacon.size, bcast, DISCOVERY_UDP_PORT))
                        } catch (_: Exception) {
                        }
                    }
                    delay(1200)
                }
            } finally {
                try {
                    udp.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startAcceptLoop(server: ServerSocket) {
        serverAcceptJob?.cancel()
        serverAcceptJob = scope.launch {
            while (isActive && !server.isClosed) {
                val socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                handleIncomingClient(socket)
            }
        }
    }

    private fun handleIncomingClient(socket: Socket) {
        scope.launch {
            var joinedPlayerId: String? = null
            try {
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                val clientIp = socket.inetAddress?.hostAddress.orEmpty()

                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val json = try {
                        JSONObject(line)
                    } catch (_: Exception) {
                        continue
                    }
                    when (json.optString("type")) {
                        "JOIN" -> {
                            val pid = json.optString("playerId")
                            val pname = json.optString("playerName", "بازیکن")
                            val avatar = json.optInt("avatarIndex", 1)
                            joinedPlayerId = pid

                            val usedColors = hostedPlayers.map { it.colorIndex }.toSet()
                            val nextColor = (0..3).firstOrNull { it !in usedColors } ?: (hostedPlayers.size % 4)
                            val newPlayer = PlayerInfo(
                                id = pid,
                                name = pname,
                                avatarIndex = avatar,
                                colorIndex = nextColor,
                                isHost = false,
                                isBot = false,
                                isReady = true,
                                ipAddress = clientIp
                            )
                            clientWriters[pid] = ClientConnection(pid, socket, writer, clientIp)
                            hostedPlayers = hostedPlayers.filterNot { it.id == pid } + newPlayer
                            broadcastRoomState()
                            _events.tryEmit(
                                NetworkEvent.RoomUpdated(
                                    hostedRoomName,
                                    hostedGameType,
                                    hostedPlayers,
                                    hostedIsPlaying
                                )
                            )
                            val sysMsg = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                senderId = "system",
                                senderName = "اتاق",
                                senderColorIndex = nextColor,
                                text = "$pname به اتاق وای‌فای پیوست 🎉",
                                isSystem = true
                            )
                            sendChatMessage(sysMsg)
                        }

                        "CHAT" -> {
                            val msg = parseChat(json)
                            broadcastJson(json, excludePlayerId = null)
                            _events.tryEmit(NetworkEvent.ChatReceived(msg))
                        }

                        "GAME_ACTION" -> {
                            val senderId = json.optString("senderId")
                            val payload = json.optString("payload")
                            _events.tryEmit(NetworkEvent.ClientActionReceived(senderId, payload))
                        }

                        "VOICE_STATUS" -> {
                            val pid = json.optString("playerId")
                            val speaking = json.optBoolean("isSpeaking", false)
                            broadcastJson(json, excludePlayerId = pid)
                            _events.tryEmit(NetworkEvent.VoiceStatusChanged(pid, speaking))
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                joinedPlayerId?.let { pid ->
                    val leavingName = hostedPlayers.firstOrNull { it.id == pid }?.name
                    clientWriters.remove(pid)
                    hostedPlayers = hostedPlayers.filterNot { it.id == pid }
                    broadcastRoomState()
                    _events.tryEmit(
                        NetworkEvent.RoomUpdated(
                            hostedRoomName,
                            hostedGameType,
                            hostedPlayers,
                            hostedIsPlaying
                        )
                    )
                    if (leavingName != null) {
                        sendChatMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                senderId = "system",
                                senderName = "اتاق",
                                senderColorIndex = 0,
                                text = "$leavingName از اتاق خارج شد.",
                                isSystem = true
                            )
                        )
                    }
                }
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun joinRoom(
        hostIp: String,
        port: Int = GAME_TCP_PORT,
        localPlayer: PlayerInfo,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        closeSession()
        isHost = false
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostIp.trim(), port), 4500)
                socket.tcpNoDelay = true
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                clientSocket = socket
                clientWriter = writer

                val joinMsg = JSONObject().apply {
                    put("type", "JOIN")
                    put("playerId", localPlayer.id)
                    put("playerName", localPlayer.name)
                    put("avatarIndex", localPlayer.avatarIndex)
                }
                sendRawToHost(joinMsg)
                onConnected()

                clientReadJob = scope.launch {
                    try {
                        while (isActive && !socket.isClosed) {
                            val line = reader.readLine() ?: break
                            val json = try {
                                JSONObject(line)
                            } catch (_: Exception) {
                                continue
                            }
                            when (json.optString("type")) {
                                "ROOM_STATE" -> {
                                    val rName = json.optString("roomName", "اتاق وای‌فای")
                                    val gType = GameType.fromId(json.optString("gameId", GameType.MENCH_CLASSIC.id))
                                    val playing = json.optBoolean("isPlaying", false)
                                    val players = parsePlayers(json.optJSONArray("players"))
                                    _events.tryEmit(NetworkEvent.RoomUpdated(rName, gType, players, playing))
                                }

                                "CHAT" -> {
                                    _events.tryEmit(NetworkEvent.ChatReceived(parseChat(json)))
                                }

                                "START_GAME" -> {
                                    val gType = GameType.fromId(json.optString("gameId"))
                                    val state = json.optString("stateJson")
                                    _events.tryEmit(NetworkEvent.GameStarted(gType, state))
                                }

                                "GAME_STATE" -> {
                                    val gType = GameType.fromId(json.optString("gameId"))
                                    val state = json.optString("stateJson")
                                    _events.tryEmit(NetworkEvent.GameStateSynced(gType, state))
                                }

                                "RETURN_LOBBY" -> {
                                    _events.tryEmit(NetworkEvent.ReturnedToLobby)
                                }

                                "VOICE_STATUS" -> {
                                    _events.tryEmit(
                                        NetworkEvent.VoiceStatusChanged(
                                            json.optString("playerId"),
                                            json.optBoolean("isSpeaking", false)
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        _events.tryEmit(NetworkEvent.Disconnected("ارتباط با میزبان وای‌فای قطع شد."))
                    }
                }
            } catch (_: Exception) {
                onError("اتصال به $hostIp برقرار نشد. مطمئن شوید هر دو گوشی به یک وای‌فای یا هات‌اسپات متصل هستند.")
            }
        }
    }

    fun sendChatMessage(message: ChatMessage) {
        val json = JSONObject().apply {
            put("type", "CHAT")
            put("id", message.id)
            put("senderId", message.senderId)
            put("senderName", message.senderName)
            put("senderColorIndex", message.senderColorIndex)
            put("text", message.text)
            put("timestampMs", message.timestampMs)
            put("isSystem", message.isSystem)
            put("isReaction", message.isReaction)
        }
        if (isHost) {
            broadcastJson(json)
            _events.tryEmit(NetworkEvent.ChatReceived(message))
        } else {
            sendRawToHost(json)
        }
    }

    fun sendVoiceStatus(playerId: String, isSpeaking: Boolean) {
        val json = JSONObject().apply {
            put("type", "VOICE_STATUS")
            put("playerId", playerId)
            put("isSpeaking", isSpeaking)
        }
        if (isHost) {
            broadcastJson(json)
        } else {
            sendRawToHost(json)
        }
    }

    fun broadcastStartGame(gameType: GameType, initialStateJson: String) {
        if (!isHost) return
        hostedGameType = gameType
        hostedIsPlaying = true
        val json = JSONObject().apply {
            put("type", "START_GAME")
            put("gameId", gameType.id)
            put("stateJson", initialStateJson)
        }
        broadcastJson(json)
    }

    fun broadcastGameState(gameType: GameType, stateJson: String) {
        if (!isHost) return
        val json = JSONObject().apply {
            put("type", "GAME_STATE")
            put("gameId", gameType.id)
            put("stateJson", stateJson)
        }
        broadcastJson(json)
    }

    fun broadcastReturnToLobby() {
        if (!isHost) return
        hostedIsPlaying = false
        broadcastJson(JSONObject().put("type", "RETURN_LOBBY"))
        broadcastRoomState()
    }

    fun sendClientGameAction(senderId: String, payloadJson: String) {
        val json = JSONObject().apply {
            put("type", "GAME_ACTION")
            put("senderId", senderId)
            put("payload", payloadJson)
        }
        sendRawToHost(json)
    }

    private fun broadcastRoomState() {
        val arr = JSONArray()
        hostedPlayers.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("avatarIndex", p.avatarIndex)
                    put("colorIndex", p.colorIndex)
                    put("isHost", p.isHost)
                    put("isBot", p.isBot)
                    put("isReady", p.isReady)
                    put("ipAddress", p.ipAddress)
                }
            )
        }
        val msg = JSONObject().apply {
            put("type", "ROOM_STATE")
            put("roomName", hostedRoomName)
            put("gameId", hostedGameType.id)
            put("isPlaying", hostedIsPlaying)
            put("players", arr)
        }
        broadcastJson(msg)
    }

    private fun broadcastJson(json: JSONObject, excludePlayerId: String? = null) {
        val line = json.toString()
        scope.launch {
            clientWriters.values.forEach { conn ->
                if (conn.playerId != excludePlayerId) {
                    try {
                        synchronized(conn.writer) {
                            conn.writer.write(line)
                            conn.writer.newLine()
                            conn.writer.flush()
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun sendRawToHost(json: JSONObject) {
        val line = json.toString()
        val writer = clientWriter ?: return
        scope.launch {
            try {
                synchronized(writer) {
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun parsePlayers(arr: JSONArray?): List<PlayerInfo> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PlayerInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                PlayerInfo(
                    id = o.optString("id"),
                    name = o.optString("name", "بازیکن"),
                    avatarIndex = o.optInt("avatarIndex", 0),
                    colorIndex = o.optInt("colorIndex", i % 4),
                    isHost = o.optBoolean("isHost", false),
                    isBot = o.optBoolean("isBot", false),
                    isReady = o.optBoolean("isReady", true),
                    ipAddress = o.optString("ipAddress", "")
                )
            )
        }
        return list
    }

    private fun parseChat(json: JSONObject): ChatMessage {
        return ChatMessage(
            id = json.optString("id", UUID.randomUUID().toString()),
            senderId = json.optString("senderId"),
            senderName = json.optString("senderName", "بازیکن"),
            senderColorIndex = json.optInt("senderColorIndex", 0),
            text = json.optString("text"),
            timestampMs = json.optLong("timestampMs", System.currentTimeMillis()),
            isSystem = json.optBoolean("isSystem", false),
            isReaction = json.optBoolean("isReaction", false)
        )
    }

    fun closeSession() {
        isHost = false
        hostedRoomId = ""
        discoveryBroadcastJob?.cancel()
        discoveryBroadcastJob = null
        serverAcceptJob?.cancel()
        serverAcceptJob = null
        clientReadJob?.cancel()
        clientReadJob = null

        clientWriters.values.forEach { conn ->
            try {
                conn.socket.close()
            } catch (_: Exception) {
            }
        }
        clientWriters.clear()

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null

        try {
            clientSocket?.close()
        } catch (_: Exception) {
        }
        clientSocket = null
        clientWriter = null
    }
}
