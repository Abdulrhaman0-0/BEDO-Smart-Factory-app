package com.bedo.smartfactory

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class Telemetry(
    val temperature: Double?,
    val humidity: Double?,
    val stale: Boolean,
    val ageSeconds: Long?,
) {
    companion object {
        fun decode(body: String): Telemetry {
            val json = JSONObject(body)
            return Telemetry(
                json.optDouble("temp").takeIf { it.isFinite() },
                json.optDouble("hum").takeIf { it.isFinite() && it > 0.0 },
                json.getBoolean("stale"),
                json.optLong("age_s", -1).takeIf { it >= 0 },
            )
        }
    }
}

internal class ModuleSource(private val connectivity: ConnectivityManager) {
    private val endpoint = URL("http://192.168.4.1/data")

    @Suppress("DEPRECATION")
    fun read(): Telemetry {
        val wifi = connectivity.allNetworks.firstOrNull {
            connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: throw IOException("Wi-Fi unavailable")
        val request = wifi.openConnection(endpoint) as HttpURLConnection
        try {
            request.connectTimeout = 3000
            request.readTimeout = 3000
            request.instanceFollowRedirects = false
            request.useCaches = false
            if (request.responseCode != 200) throw IOException("Unexpected HTTP response")
            return request.inputStream.bufferedReader().use { Telemetry.decode(it.readText()) }
        } finally {
            request.disconnect()
        }
    }
}
