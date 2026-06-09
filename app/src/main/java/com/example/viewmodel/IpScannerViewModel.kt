package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.scanner.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IpScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ScannerRepository(db.scannerDao())

    val engine = IpScannerEngine(application)
    val countryEngine = IpScannerEngine(application)

    data class CountryInfo(
        val name: String,
        val persianName: String,
        val flag: String,
        val cidrs: List<String>
    )

    val countriesList = listOf(
        CountryInfo("Germany", "آلمان", "🇩🇪", listOf("142.250.186.0/24", "104.16.100.0/24", "172.64.150.0/24")),
        CountryInfo("United States", "ایالات متحده", "🇺🇸", listOf("142.250.72.0/24", "104.16.12.0/24", "172.67.20.0/24")),
        CountryInfo("United Kingdom", "انگلستان", "🇬🇧", listOf("185.199.108.0/24", "104.18.22.0/24", "172.64.120.0/24")),
        CountryInfo("Netherlands", "هلند", "🇳🇱", listOf("140.82.112.0/24", "104.18.30.0/24", "172.64.140.0/24")),
        CountryInfo("France", "فرانسه", "🇫🇷", listOf("104.24.120.0/24", "104.18.0.0/24", "172.64.110.0/24")),
        CountryInfo("Finland", "فنلاند", "🇫🇮", listOf("104.28.10.0/24", "104.18.80.0/24", "172.64.160.0/24")),
        CountryInfo("Canada", "کانادا", "🇨🇦", listOf("104.18.40.0/24", "104.16.90.0/24", "172.64.180.0/24")),
        CountryInfo("Singapore", "سنگاپور", "🇸🇬", listOf("104.18.50.0/24", "104.16.130.0/24", "172.64.190.0/24")),
        CountryInfo("Turkey", "ترکیه", "🇹🇷", listOf("104.18.60.0/24", "104.16.140.0/24", "172.64.130.0/24")),
        CountryInfo("Iran", "ایران", "🇮🇷", listOf("104.18.0.0/24", "104.16.30.0/24"))
    )

    private val _selectedCountry = MutableStateFlow(countriesList[0])
    val selectedCountry = _selectedCountry.asStateFlow()

    fun selectCountry(country: CountryInfo) {
        _selectedCountry.value = country
    }

    fun startCountryScan() {
        val country = _selectedCountry.value
        val cidrs = country.cidrs

        viewModelScope.launch {
            countryEngine.stopScan()
            val ipsToScan = mutableListOf<String>()

            withContext(Dispatchers.Default) {
                for (cidr in cidrs) {
                    if (cidr.isNotBlank()) {
                        ipsToScan.addAll(IpUtils.parseCidr(cidr, maxCount = 60))
                    }
                }
            }

            if (ipsToScan.isEmpty()) {
                countryEngine.addLog("رنج آی‌پی کشوری نامعتبر است!")
                return@launch
            }

            countryEngine.startScan(
                ipsToScan = ipsToScan,
                port = 443,
                providerName = "${country.flag} ${country.persianName}",
                concurrencyLimit = concurrency.value,
                timeoutMs = timeout.value
            ) { completedResults ->
                // Save clean results automatically
                viewModelScope.launch {
                    val entities = completedResults.map {
                        ScannedIp(
                            ip = it.ip,
                            port = it.port,
                            latencyMs = it.latencyMs,
                            downloadSpeedKbps = 0.0,
                            provider = "${country.flag} اسکن کشوری (${country.persianName})",
                            connectionType = connectionDetails.value.first
                        )
                    }
                    repository.insertIps(entities)
                }
            }
        }
    }

    fun stopCountryScan() {
        countryEngine.stopScan()
    }

    // Flow states from database
    val allScannedIps = repository.allScannedIps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteIps = repository.favoriteIps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedSnis = repository.allSnis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Input Config Flow States
    val connectionDetails = MutableStateFlow(engine.getNetworkConnectionDetails())
    val selectedProvider = MutableStateFlow("Cloudflare")
    val customCidr = MutableStateFlow("")
    val concurrency = MutableStateFlow(40)
    val timeout = MutableStateFlow(1200)

    // Speed test result states
    private val _speedResults = MutableStateFlow<Map<String, Double>>(emptyMap())
    val speedResults = _speedResults.asStateFlow()

    private val _isSpeedTesting = MutableStateFlow<String?>(null)
    val isSpeedTesting = _isSpeedTesting.asStateFlow()

    // SNI Scanner UI states
    private val _isSniScanning = MutableStateFlow(false)
    val isSniScanning = _isSniScanning.asStateFlow()

    private val _sniProgress = MutableStateFlow(0f)
    val sniProgress = _sniProgress.asStateFlow()

    private val _sniLogs = MutableStateFlow<List<String>>(emptyList())
    val sniLogs = _sniLogs.asStateFlow()

    private val _scannedSniCount = MutableStateFlow(0)
    val scannedSniCount = _scannedSniCount.asStateFlow()

    private val _totalSniCount = MutableStateFlow(0)
    val totalSniCount = _totalSniCount.asStateFlow()

    init {
        // Pre-populate popular filter-bypass and domestic testing SNIs on cold start
        viewModelScope.launch {
            try {
                val list = repository.allSnis.first()
                val defaults = listOf(
                    SavedSni("speed.cloudflare.com", false, -1L, false, "سرعت‌سنج اختصاصی لایه CDN کلودفلر"),
                    SavedSni("www.cloudflare.com", false, -1L, false, "دامنه اصلی وب‌سایت کلودفلر"),
                    SavedSni("gopro.com", false, -1L, false, "سایت گو‌پرو (عبور دهنده عالی هندشیک TLS)"),
                    SavedSni("twitch.tv", false, -1L, false, "پلتفرم لایو استریم جهانی توییچ"),
                    SavedSni("disney.com", false, -1L, false, "وب‌سایت انیمیشن دیزنی"),
                    SavedSni("spotify.com", false, -1L, false, "پخش موسیقی اسپاتیفای"),
                    SavedSni("images-assets.nasa.gov", false, -1L, false, "بای‌پس عالی با آپلودر تصاویر ناسا"),
                    SavedSni("www.google.com", false, -1L, false, "موتور جستجوی امن گوگل"),
                    SavedSni("www.github.com", false, -1L, false, "سرویس مخازن گیت‌هاب"),
                    SavedSni("coursera.org", false, -1L, false, "پلتفرم آموزشی و وبینار کورسرا"),
                    SavedSni("medium.com", false, -1L, false, "توسعه و وبلاگ مدیوم"),
                    SavedSni("wikipedia.org", false, -1L, false, "دانشنامه بین‌المللی ویکی‌پدیا"),
                    SavedSni("stackoverflow.com", false, -1L, false, "سایت توسعه stackoverflow"),
                    SavedSni("developer.android.com", false, -1L, false, "سایت مستندات برنامه‌نویسی گوگل"),
                    SavedSni("teams.microsoft.com", false, -1L, false, "مایکروسافت تیمز ( whitelisted سازمانی)"),
                    SavedSni("skype.com", false, -1L, false, "سرویس مکالمه تصوری اسکایپ"),
                    SavedSni("zoom.us", false, -1L, false, "سرویس وبینار و پلتفرم زوم"),
                    SavedSni("salesforce.com", false, -1L, false, "سرویس ارتباطی سالزدفورس"),
                    SavedSni("www.office.com", false, -1L, false, "پرتال سرویس آفیس مایکروسافت"),
                    SavedSni("www.webex.com", false, -1L, false, "وبینار و سرویس سیسکو وب‌اکس"),
                    SavedSni("time.ir", false, -1L, false, "ساعت رسمی کشور (سرعت لود فوری داخلی)"),
                    SavedSni("telewebion.com", false, -1L, false, "پخش آنلاین سازمان تلوبیون (رنج داخلی)"),
                    SavedSni("divar.ir", false, -1L, false, "سامانه دیوار (ترافیک سبک شبکه ملی)")
                )
                if (list.size != defaults.size) {
                    repository.clearAllSnis()
                    repository.insertSnis(defaults)
                }
            } catch (e: Exception) {
                // Fail-safe in case of any flow error on init
            }
        }
    }

    fun addSniLog(msg: String) {
        val current = _sniLogs.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg")
        _sniLogs.value = current.take(150)
    }

    fun refreshConnection() {
        connectionDetails.value = engine.getNetworkConnectionDetails()
    }

    // High performance multi-threaded scanner dispatch
    fun startIpScan() {
        val provider = selectedProvider.value
        val cidrs = when (provider) {
            "Cloudflare" -> listOf(
                "104.16.123.0/24",
                "172.64.150.0/24",
                "104.21.30.0/24",
                "104.18.10.0/24",
                "172.67.20.0/24",
                "188.114.96.0/24"
            )
            "Google" -> listOf(
                "172.217.16.0/24",
                "142.250.72.0/24",
                "142.250.190.0/24"
            )
            "GitHub" -> listOf(
                "185.199.108.0/24",
                "140.82.112.0/24"
            )
            "Akamai" -> listOf(
                "23.1.1.0/24",
                "104.23.50.0/24"
            )
            else -> listOf(customCidr.value)
        }

        viewModelScope.launch {
            engine.stopScan()
            val ipsToScan = mutableListOf<String>()

            withContext(Dispatchers.Default) {
                for (cidr in cidrs) {
                    if (cidr.isNotBlank()) {
                        ipsToScan.addAll(IpUtils.parseCidr(cidr, maxCount = 60))
                    }
                }
            }

            if (ipsToScan.isEmpty()) {
                engine.addLog("رنج آی‌پی نامعتبر است! لطفا مقدار درستی وارد کنید.")
                return@launch
            }

            engine.startScan(
                ipsToScan = ipsToScan,
                port = 443,
                providerName = provider,
                concurrencyLimit = concurrency.value,
                timeoutMs = timeout.value
            ) { completedResults ->
                // Save clean results automatically
                viewModelScope.launch {
                    val entities = completedResults.map {
                        ScannedIp(
                            ip = it.ip,
                            port = it.port,
                            latencyMs = it.latencyMs,
                            downloadSpeedKbps = 0.0,
                            provider = it.provider,
                            connectionType = connectionDetails.value.first
                        )
                    }
                    repository.insertIps(entities)
                }
            }
        }
    }

    fun stopIpScan() {
        engine.stopScan()
    }

    fun toggleFavorite(scannedIp: ScannedIp) {
        viewModelScope.launch {
            repository.toggleFavorite(scannedIp.ip, !scannedIp.isFavorite)
        }
    }

    fun deleteScannedIp(scannedIp: ScannedIp) {
        viewModelScope.launch {
            repository.deleteIp(scannedIp.ip)
        }
    }

    fun clearAllScanned() {
        viewModelScope.launch {
            repository.clearAllIps()
        }
    }

    // Direct thread-isolated file socket download speed test
    fun testIpSpeed(scannedIp: ScannedIp) {
        if (_isSpeedTesting.value != null) return
        _isSpeedTesting.value = scannedIp.ip
        viewModelScope.launch {
            val hostHeader = when (scannedIp.provider) {
                "Cloudflare" -> "speed.cloudflare.com"
                "Google" -> "www.google.com"
                "GitHub" -> "github.com"
                "Akamai" -> "www.apple.com"
                else -> "speed.cloudflare.com"
            }
            val speed = engine.runSpeedTest(
                ip = scannedIp.ip,
                port = scannedIp.port,
                hostHeader = hostHeader
            )
            _speedResults.value = _speedResults.value + (scannedIp.ip to speed)

            val updated = scannedIp.copy(downloadSpeedKbps = speed)
            repository.insertIp(updated)

            _isSpeedTesting.value = null
        }
    }

    // SNI test pipeline directly dispatching TLS clients
    fun startSniScan(targetDestIp: String = "104.16.123.96") {
        if (_isSniScanning.value) return
        _isSniScanning.value = true
        _sniProgress.value = 0f
        _scannedSniCount.value = 0
        _sniLogs.value = emptyList()

        val listToTest = savedSnis.value
        _totalSniCount.value = listToTest.size

        addSniLog("شروع تست سلامت پروتکل SNI روی آی‌پی دیتاسنتر: $targetDestIp")
        addSniLog("تعداد کل دامنه‌های در صف بررسی: ${listToTest.size}")

        viewModelScope.launch {
            val results = mutableListOf<SavedSni>()
            for (sni in listToTest) {
                if (!_isSniScanning.value) break
                addSniLog("در حال ارسال بسته هندشیک TLS به دامنه: ${sni.host}")

                val probeResult = SniProber.probeSni(sni.host, targetIp = targetDestIp, timeoutMs = timeout.value)
                val working = probeResult.first
                val latency = probeResult.second
                val errorMsg = probeResult.third

                val updated = sni.copy(
                    isWorking = working,
                    latencyMs = if (working) latency else -1L,
                    lastTestedAt = System.currentTimeMillis()
                )
                results.add(updated)
                repository.insertSni(updated)

                if (working) {
                    addSniLog("سلامت تأیید شد ✅ دامنه ${sni.host} فعال است (تاخیر: ${latency}ms)")
                } else {
                    val detail = if (errorMsg != null) " [$errorMsg]" else ""
                    addSniLog("مسدود بودن دامنه یا اختلال ❌ دامنه ${sni.host} پاسخ نداد$detail")
                }

                _scannedSniCount.value += 1
                if (listToTest.isNotEmpty()) {
                    _sniProgress.value = _scannedSniCount.value.toFloat() / listToTest.size.toFloat()
                }
            }
            _isSniScanning.value = false
            addSniLog("اسکن کل رکوردها پایان یافت.")
        }
    }

    fun stopSniScan() {
        if (_isSniScanning.value) {
            _isSniScanning.value = false
            addSniLog("تست SNI متوقف شد 🚫")
        }
    }

    fun addCustomSni(host: String, note: String) {
        if (host.isBlank()) return
        viewModelScope.launch {
            val element = SavedSni(host.trim(), false, 0, true, note)
            repository.insertSni(element)
            addSniLog("دامنه SNI جدید دستی ثبت شد: $host")
        }
    }

    fun deleteSni(sni: SavedSni) {
        viewModelScope.launch {
            repository.deleteSni(sni.host)
            addSniLog("حذف SNI: ${sni.host}")
        }
    }

    fun clearAllSnis() {
        viewModelScope.launch {
            repository.clearAllSnis()
            addSniLog("تمامی دامنه‌های ذخیره‌شده پاکسازی شدند.")
        }
    }
}
