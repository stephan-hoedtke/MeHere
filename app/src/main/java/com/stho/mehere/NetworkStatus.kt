package com.stho.mehere

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.NetworkInterface


sealed class NetworkStatus {
    object AVAILABLE : NetworkStatus()
    object LOST: NetworkStatus()

    override fun toString(): String {
        return when (this) {
            AVAILABLE -> "Available"
            LOST -> "Lost"
        }
    }
}

class NetworkInfo(val networkName: String, val status: NetworkStatus, val capabilities: String? = null) {
}


class NetworkStatusInfo {
    private val map: HashMap<String, NetworkInfo> = HashMap()

    fun setUnavailable() {
        map.clear()
    }

    fun setLost(network: Network) {
        val networkName = network.toString()
        map[networkName] = NetworkInfo(networkName, NetworkStatus.LOST)
    }

    fun setAvailable(connectivityManager: ConnectivityManager, network: Network) {
        val networkName = network.toString()
        val capabilities = getNetworkCapabilitiesAsString(connectivityManager, network)
        map[networkName] = NetworkInfo(networkName, NetworkStatus.AVAILABLE, capabilities)
    }

    private val isOnline: Boolean
        get() {
            for (networkInfo: NetworkInfo in map.values) {
                if (networkInfo.status == NetworkStatus.AVAILABLE) {
                    return true
                }
            }
            return false
        }

    override fun toString(): String {
        val sb = StringBuilder()

        if (isOnline) {
            sb.append("ONLINE: ")
        } else {
            sb.append("OFFLINE")
        }

        if (map.keys.size > 0) {
            var first: Boolean = true
            for (networkInfo: NetworkInfo in map.values) {
                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }
                sb.append(networkInfo.networkName)
                sb.append(": ")
                sb.append(networkInfo.status)

                if (networkInfo.capabilities != null) {
                    sb.append(" ")
                    sb.append(networkInfo.capabilities)
                }
            }
        }

        return sb.trim().toString()
    }

    val iconId: Int
        get() = if (isOnline) R.drawable.online else R.drawable.offline

    companion object {
        private fun getNetworkCapabilitiesAsString(connectivityManager: ConnectivityManager, network: Network): String? {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps != null) {
                val sb = java.lang.StringBuilder()
                sb.append(caps.linkDownstreamBandwidthKbps)
                sb.append("/")
                sb.append(caps.linkUpstreamBandwidthKbps)
                sb.append(" ")

                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                    sb.append("INTERNET")

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH))
                    sb.append("BLUETOOTH")

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    sb.append("CELLULAR")

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                    sb.append("ETHERNET")

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    sb.append("WIFI ")

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                    sb.append("VPN")

                return sb.toString()
            } else {
                return null
            }
        }
    }
}

