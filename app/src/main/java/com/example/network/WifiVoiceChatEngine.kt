package com.example.network

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Low-latency LAN Wi-Fi Voice Chat engine using UDP PCM 16-bit mono @ 16kHz.
 * Host relays received voice frames to all connected peers; clients stream to Host IP.
 */
class WifiVoiceChatEngine {

    companion object {
        const val VOICE_UDP_PORT = 48913
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SAMPLES = 320 // 20ms @ 16kHz -> 640 bytes PCM
        private const val FRAME_BYTES = FRAME_SAMPLES * 2
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var captureJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    private var isHostMode: Boolean = false
    private var hostAddress: InetAddress? = null
    private var localPlayerId: String = ""
    private val peerAddresses = CopyOnWriteArraySet<InetAddress>()

    @Volatile
    private var transmitting: Boolean = false

    @Volatile
    private var speakerMuted: Boolean = false

    private val _localMicLevel = MutableStateFlow(0f)
    val localMicLevel: StateFlow<Float> = _localMicLevel.asStateFlow()

    private val _remoteSpeakerId = MutableStateFlow<String?>(null)
    val remoteSpeakerId: StateFlow<String?> = _remoteSpeakerId.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun startSession(
        playerId: String,
        asHost: Boolean,
        hostIp: String? = null
    ) {
        stopSession()
        localPlayerId = playerId
        isHostMode = asHost
        hostAddress = try {
            if (!asHost && !hostIp.isNullOrBlank()) InetAddress.getByName(hostIp) else null
        } catch (_: Exception) {
            null
        }
        peerAddresses.clear()

        try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(VOICE_UDP_PORT))
                soTimeout = 0
            }
            udpSocket = socket
            _isRunning.value = true
            startReceiver(socket)
        } catch (_: Exception) {
            // Port might already be bound on loopback/single-device test; fallback ephemeral socket
            try {
                val fallback = DatagramSocket()
                udpSocket = fallback
                _isRunning.value = true
                startReceiver(fallback)
            } catch (_: Exception) {
                _isRunning.value = false
            }
        }
    }

    fun updatePeers(ipList: List<String>) {
        peerAddresses.clear()
        ipList.forEach { ip ->
            if (ip.isNotBlank() && ip != "127.0.0.1") {
                try {
                    peerAddresses.add(InetAddress.getByName(ip))
                } catch (_: Exception) {
                }
            }
        }
    }

    fun setSpeakerMuted(muted: Boolean) {
        speakerMuted = muted
    }

    @SuppressLint("MissingPermission")
    fun setTransmitting(active: Boolean): Boolean {
        if (transmitting == active) return true
        transmitting = active
        if (active) {
            return startMicCapture()
        } else {
            stopMicCapture()
            _localMicLevel.value = 0f
            return true
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicCapture(): Boolean {
        stopMicCapture()
        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_BYTES * 4)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                transmitting = false
                return false
            }
            audioRecord = recorder
            recorder.startRecording()

            val idBytes = localPlayerId.toByteArray(Charsets.UTF_8)
            val idLen = min(idBytes.size, 64)

            captureJob = scope.launch {
                val pcmBuffer = ByteArray(FRAME_BYTES)
                val packetBuffer = ByteArray(1 + idLen + FRAME_BYTES)
                packetBuffer[0] = idLen.toByte()
                System.arraycopy(idBytes, 0, packetBuffer, 1, idLen)

                while (isActive && transmitting) {
                    val read = try {
                        recorder.read(pcmBuffer, 0, FRAME_BYTES)
                    } catch (_: Exception) {
                        -1
                    }
                    if (read > 0) {
                        // Compute RMS volume for live UI visualizer
                        val level = computeRmsNormalized(pcmBuffer, read)
                        _localMicLevel.value = level

                        val socket = udpSocket
                        if (socket != null && !socket.isClosed) {
                            System.arraycopy(pcmBuffer, 0, packetBuffer, 1 + idLen, read)
                            val totalLen = 1 + idLen + read
                            if (isHostMode) {
                                for (peer in peerAddresses) {
                                    try {
                                        socket.send(
                                            DatagramPacket(packetBuffer, totalLen, peer, VOICE_UDP_PORT)
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            } else {
                                val target = hostAddress
                                if (target != null) {
                                    try {
                                        socket.send(
                                            DatagramPacket(packetBuffer, totalLen, target, VOICE_UDP_PORT)
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    }
                }
                _localMicLevel.value = 0f
            }
            true
        } catch (_: Exception) {
            transmitting = false
            _localMicLevel.value = 0f
            false
        }
    }

    private fun stopMicCapture() {
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    private fun startReceiver(socket: DatagramSocket) {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_BYTES * 4)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                null
            }
            audioTrack = track
            try {
                track?.play()
            } catch (_: Exception) {
            }

            val recvBuf = ByteArray(1024)
            var lastSpeakerClearMs = 0L

            while (isActive && !socket.isClosed) {
                try {
                    val packet = DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(packet)
                    val len = packet.length
                    if (len < 3) continue
                    val idLen = recvBuf[0].toInt() and 0xFF
                    if (len <= 1 + idLen) continue

                    val senderId = String(recvBuf, 1, idLen, Charsets.UTF_8)
                    val audioOffset = 1 + idLen
                    val audioLen = len - audioOffset

                    // Host relays audio to all other peers
                    if (isHostMode) {
                        val senderAddr = packet.address
                        if (senderAddr != null) {
                            peerAddresses.add(senderAddr)
                            for (peer in peerAddresses) {
                                if (peer != senderAddr) {
                                    try {
                                        socket.send(
                                            DatagramPacket(recvBuf, len, peer, VOICE_UDP_PORT)
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    }

                    if (senderId != localPlayerId) {
                        _remoteSpeakerId.value = senderId
                        lastSpeakerClearMs = System.currentTimeMillis()
                        if (!speakerMuted && track != null && audioLen > 0) {
                            track.write(recvBuf, audioOffset, audioLen)
                        }
                    }

                    if (System.currentTimeMillis() - lastSpeakerClearMs > 600L) {
                        _remoteSpeakerId.value = null
                    }
                } catch (_: Exception) {
                    if (socket.isClosed) break
                }
            }
        }
    }

    private fun computeRmsNormalized(pcmBytes: ByteArray, length: Int): Float {
        var sumSquares = 0.0
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f
        var i = 0
        while (i + 1 < length) {
            val sample = ((pcmBytes[i + 1].toInt() shl 8) or (pcmBytes[i].toInt() and 0xFF)).toShort().toDouble()
            sumSquares += sample * sample
            i += 2
        }
        val rms = sqrt(sumSquares / sampleCount)
        return (rms / 6500.0).toFloat().coerceIn(0f, 1f)
    }

    fun stopSession() {
        transmitting = false
        stopMicCapture()
        receiveJob?.cancel()
        receiveJob = null
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        peerAddresses.clear()
        _localMicLevel.value = 0f
        _remoteSpeakerId.value = null
        _isRunning.value = false
        scope.coroutineContext.cancelChildren()
    }
}
