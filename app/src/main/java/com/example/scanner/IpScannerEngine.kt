package com.example.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.measureTimeMillis

data class ScanResult(
    val ip: String,
    val port: Int,
    val latencyMs: Long,
    val isLive: Boolean,
    val provider: String,
    val timestamp: Long = System.currentTimeMillis()
)

class IpScannerEngine(private val context: Context) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount = _scannedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    private val _liveResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val liveResults = _liveResults.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private var scanJob: Job? = null

    fun addLog(msg: String) {
        val current = _logs.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg")
        _logs.value = current.take(150) // keep last 150 entries
    }

    // Identifies local network and suggests timeouts & concurrency limit based on bandwidth capability
    fun getNetworkConnectionDetails(): Pair<String, Int> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return Pair("بدون اتصال / نامشخص (Offline/Unknown)", 1200)
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return Pair("نامشخص", 1200)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Pair("واي‌فاي (WiFi)", 800)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Pair("اينترنت همراه (Mobile Data)", 1500)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Pair("اترنت (Ethernet)", 600)
            else -> Pair("نامشخص (Unknown Conn)", 1200)
        }
    }

    // Start scanner with optimized concurrency limits
    fun startScan(
        ipsToScan: List<String>,
        port: Int = 443,
        providerName: String,
        concurrencyLimit: Int = 60,
        timeoutMs: Int = 1000,
        onComplete: (List<ScanResult>) -> Unit
    ) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scannedCount.value = 0
        _totalCount.value = ipsToScan.size
        _scanProgress.value = 0f
        _liveResults.value = emptyList()
        _logs.value = emptyList()

        addLog("شروع اسکن هوشمند آی‌پی دیتاسنتر $providerName")
        addLog("تعداد فرآیندهای همزمان: $concurrencyLimit | حداکثر انتظار: ${timeoutMs}ms")
        addLog("در حال بررسی ${ipsToScan.size} آی‌پی منتخب...")

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            supervisorScope {
                val semaphore = kotlinx.coroutines.sync.Semaphore(concurrencyLimit)
                val results = mutableListOf<ScanResult>()
                val jobs = mutableListOf<Job>()

                for (ip in ipsToScan) {
                    if (!isActive) break
                    semaphore.acquire()

                    val job = launch {
                        try {
                            if (!isActive) return@launch
                            // Use strict timeout wrapper in case low-level socket connect hangs
                            val latency = withTimeoutOrNull(timeoutMs.toLong() + 200) {
                                testIpTcp(ip, port, timeoutMs)
                            } ?: -1L

                            if (latency >= 0) {
                                val result = ScanResult(
                                    ip = ip,
                                    port = port,
                                    latencyMs = latency,
                                    isLive = true,
                                    provider = providerName
                                )
                                synchronized(results) {
                                    results.add(result)
                                    val updated = _liveResults.value.toMutableList()
                                    updated.add(result)
                                    _liveResults.value = updated.sortedBy { it.latencyMs }
                                }
                                addLog("آی‌پی سالم یافت شد 🟢 $ip | پینگ: ${latency}ms")
                            }
                        } catch (e: Exception) {
                            // Suppressed
                        } finally {
                            semaphore.release()
                            synchronized(this@IpScannerEngine) {
                                _scannedCount.value += 1
                                if (ipsToScan.isNotEmpty()) {
                                    _scanProgress.value = _scannedCount.value.toFloat() / ipsToScan.size.toFloat()
                                }
                            }
                        }
                    }
                    jobs.add(job)
                }

                jobs.joinAll()
            }
            _isScanning.value = false
            addLog("اسکن کل رنج‌ها پایان یافت! ${_liveResults.value.size} آی‌پی زنده یافت شد.")
            onComplete(_liveResults.value)
        }
    }

    fun stopScan() {
        if (_isScanning.value) {
            scanJob?.cancel()
            _isScanning.value = false
            addLog("اسکن توسط کاربر متوقف شد 🚫")
        }
    }

    // Accurate low-level TCP Socket scanner
    private suspend fun testIpTcp(ip: String, port: Int, timeoutMs: Int): Long {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                var latency = -1L
                val time = measureTimeMillis {
                    socket = Socket()
                    socket?.connect(InetSocketAddress(ip, port), timeoutMs)
                }
                latency = time
                latency
            } catch (e: Exception) {
                -1L
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    // Suppressed
                }
            }
        }
    }

    // Advanced raw socket TLS/HTTP bandwidth measurement (speed test)
    suspend fun runSpeedTest(ip: String, port: Int = 443, hostHeader: String = "speed.cloudflare.com"): Double {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var finalSocket: Socket? = null
            try {
                socket = java.net.Socket()
                socket.connect(InetSocketAddress(ip, port), 3000)

                // For SSL/HTTPS ports, wrap inside a standard SSL connection
                // but use custom handshake with appropriate SNI host to bypass ISP DPI filters
                finalSocket = if (port == 443) {
                    val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val ssl = factory.createSocket(socket, ip, port, true) as SSLSocket
                    ssl.soTimeout = 3000
                    try {
                        // Set manual SNI using Reflection or SSLParameters for older systems if necessary,
                        // or rely on default handshaking
                        val sslParams = ssl.sslParameters
                        sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(hostHeader))
                        ssl.sslParameters = sslParams
                    } catch (e: Exception) {
                        // Failover to standard wrapper
                    }
                    ssl.startHandshake()
                    ssl
                } else {
                    socket.soTimeout = 3000
                    socket
                }

                val writer = finalSocket.getOutputStream().bufferedWriter()
                val reader = finalSocket.getInputStream()

                // Request a 150KB chunk file test to measure speed accurately without downloading massive datasets,
                // or fall back to / (index) for other non-Cloudflare hosts
                val path = if (hostHeader == "speed.cloudflare.com") "/__down?bytes=150000" else "/"
                val request = "GET $path HTTP/1.1\r\n" +
                        "Host: $hostHeader\r\n" +
                        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\r\n" +
                        "Accept: */*\r\n" +
                        "Connection: close\r\n\r\n"

                val downloadStartTime = System.currentTimeMillis()
                writer.write(request)
                writer.flush()

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytes = 0L
                val maxDurationMs = 1500L // 1.5 seconds max download time window

                while (System.currentTimeMillis() - downloadStartTime < maxDurationMs) {
                    bytesRead = try {
                        reader.read(buffer)
                    } catch (e: Exception) {
                        break
                    }
                    if (bytesRead == -1) break
                    totalBytes += bytesRead
                    // Stop early as 300KB is enough data to measure speed accurately
                    if (totalBytes > 300_000) {
                        break
                    }
                }

                val duration = System.currentTimeMillis() - downloadStartTime
                if (duration >= 50 && totalBytes >= 2048) {
                    val speedKbps = (totalBytes * 8.0 / 1024.0) / (duration / 1000.0)
                    speedKbps // Kilobits per second
                } else {
                    0.0
                }
            } catch (e: Exception) {
                0.0
            } finally {
                try {
                    finalSocket?.close()
                } catch (e: Exception) { /* Suppressed */ }
                try {
                    socket?.close()
                } catch (e: Exception) { /* Suppressed */ }
            }
        }
    }
}
