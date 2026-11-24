package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object WifiNetworkUtils {

    fun getLocalWifiIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            // Prefer wlan / ap / softap interfaces first
            val preferred = interfaces.filter { iface ->
                iface.isUp && !iface.isLoopback && (
                    iface.name.startsWith("wlan", ignoreCase = true) ||
                        iface.name.startsWith("ap", ignoreCase = true) ||
                        iface.name.startsWith("swlan", ignoreCase = true)
                    )
            }
            val ordered = preferred + interfaces.filter { it.isUp && !it.isLoopback && it !in preferred }
            for (iface in ordered) {
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        return addr.hostAddress ?: continue
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    fun getBroadcastAddresses(): List<InetAddress> {
        val result = linkedSetOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (ifaceAddr in iface.interfaceAddresses) {
                    val broadcast = ifaceAddr.broadcast
                    if (broadcast is Inet4Address) {
                        result.add(broadcast)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback below
        }
        try {
            result.add(InetAddress.getByName("255.255.255.255"))
        } catch (_: Exception) {
        }
        return result.toList()
    }

    fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.createMulticastLock("DorehamiWifiMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {
        try {
            if (lock != null && lock.isHeld) {
                lock.release()
            }
        } catch (_: Exception) {
        }
    }
}
