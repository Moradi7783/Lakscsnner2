package com.example.scanner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SniProber {

    private fun getTrustAllSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    // Probes an SNI host over a target IP on port 443
    suspend fun probeSni(
        host: String, 
        targetIp: String = "104.16.123.96", 
        timeoutMs: Int = 1500
    ): Triple<Boolean, Long, String?> {
        return withContext(Dispatchers.IO) {
            var rawSocket: Socket? = null
            var sslSocket: SSLSocket? = null
            try {
                val startTime = System.currentTimeMillis()

                rawSocket = Socket()
                rawSocket.connect(InetSocketAddress(targetIp, 443), timeoutMs)

                val factory = getTrustAllSocketFactory()
                // Use host.trim() for the handshake description
                sslSocket = factory.createSocket(rawSocket, host.trim(), 443, true) as SSLSocket
                sslSocket.soTimeout = timeoutMs

                // Set Server Name Indication (SNI)
                val sslParams = SSLParameters()
                try {
                    sslParams.serverNames = listOf(SNIHostName(host.trim()))
                    sslSocket.sslParameters = sslParams
                } catch (e: Exception) {
                    Log.e("SniProber", "Couldn't set Explicit SNI name: ${e.message}")
                }

                // Execute Handshake
                sslSocket.startHandshake()

                val duration = System.currentTimeMillis() - startTime
                Triple(true, duration, null)
            } catch (e: java.net.SocketTimeoutException) {
                Triple(false, -1L, "تایم‌اوت - خطای لایه TCP (گیت‌وی یا پورت مسدود است) ⏳")
            } catch (e: javax.net.ssl.SSLHandshakeException) {
                Triple(false, -1L, "فیلتر تفکیک بسته (هندشیک TLS مسدود شد - دیوار DPI) ❌")
            } catch (e: java.net.ConnectException) {
                Triple(false, -1L, "عدم پذیرش اتصال (پورت ۴۴۳ باز نیست یا پکت ریست شد) ⚠️")
            } catch (e: java.io.IOException) {
                val msg = e.message ?: ""
                val detail = when {
                    msg.contains("Connection reset", ignoreCase = true) -> "برش فایروال DPI (اتصال به سرور قطع شد - RST) 🛡️"
                    msg.contains("broken pipe", ignoreCase = true) -> "قطع کانال ارتباطی DPI ⛔"
                    msg.contains("Handshake failed", ignoreCase = true) -> "اختلال عمدی فایروال در تبادل کلید TLS 🚫"
                    else -> "محدودیت یا انسداد مسیر لایه انتقال"
                }
                Triple(false, -1L, detail)
            } catch (e: Exception) {
                val errMsg = e.message ?: e.javaClass.simpleName
                Triple(false, -1L, "اختلال یا مانع فنی [$errMsg] 🛑")
            } finally {
                try {
                    sslSocket?.close()
                } catch (e: Exception) {
                    // Suppressed
                }
                try {
                    rawSocket?.close()
                } catch (e: Exception) {
                    // Suppressed
                }
            }
        }
    }
}
