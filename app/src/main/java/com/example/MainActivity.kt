package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.ScannedIp
import com.example.database.SavedSni
import com.example.viewmodel.IpScannerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // High Density Premium Light-Slate theme Colors
            val highDensityColorScheme = lightColorScheme(
                primary = Color(0xFF005AC1),
                onPrimary = Color.White,
                secondary = Color(0xFFD3E4FF),
                onSecondary = Color(0xFF001D35),
                background = Color(0xFFFDFBFF),
                onBackground = Color(0xFF1B1B1F),
                surface = Color.White,
                onSurface = Color(0xFF1B1B1F),
                surfaceVariant = Color(0xFFF3F4F9),
                onSurfaceVariant = Color(0xFF44474E),
                outline = Color(0xFFE1E2EC)
            )

            MaterialTheme(
                colorScheme = highDensityColorScheme,
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: IpScannerViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collecting states from ViewModel
    val activeTab = remember { mutableIntStateOf(0) }
    val isScanning by viewModel.engine.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.engine.scanProgress.collectAsStateWithLifecycle()
    val scannedCount by viewModel.engine.scannedCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.engine.totalCount.collectAsStateWithLifecycle()
    val liveResults by viewModel.engine.liveResults.collectAsStateWithLifecycle()
    val logs by viewModel.engine.logs.collectAsStateWithLifecycle()
    
    val allScannedIps by viewModel.allScannedIps.collectAsStateWithLifecycle()
    val favoriteIps by viewModel.favoriteIps.collectAsStateWithLifecycle()
    val connectionDetails by viewModel.connectionDetails.collectAsStateWithLifecycle()
    
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val customCidr by viewModel.customCidr.collectAsStateWithLifecycle()
    val concurrency by viewModel.concurrency.collectAsStateWithLifecycle()
    val timeout by viewModel.timeout.collectAsStateWithLifecycle()
    
    val speedResults by viewModel.speedResults.collectAsStateWithLifecycle()
    val isSpeedTesting by viewModel.isSpeedTesting.collectAsStateWithLifecycle()

    // Periodically refresh network details
    LaunchedEffect(Unit) {
        viewModel.refreshConnection()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFBFF))
                    .statusBarsPadding()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = Color(0xFFE1E2EC),
                            start = Offset(0f, size.height - strokeWidth/2),
                            end = Offset(size.width, size.height - strokeWidth/2),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Brand Logo container and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF005AC1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "L2",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "لک اسکنر ۲",
                                color = Color(0xFF1B1B1F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Engine v4.2 • High Stealth",
                                color = Color(0xFF44474E),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Right side: Pulsing indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = if (isScanning) Color(0xFF005AC1) else Color(0xFFEF4444),
                                        radius = size.minDimension / 2 * pulseScale
                                    )
                                }
                        )
                        Text(
                            text = if (isScanning) "اسکن فعال" else "آماده اسکن",
                            color = if (isScanning) Color(0xFF005AC1) else Color(0xFF44474E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Network Status Indicator card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.refreshConnection() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "بروزرسانی شبکه",
                                tint = Color(0xFF005AC1)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = connectionDetails.first,
                                color = Color(0xFF1B1B1F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "وضعیت وب",
                                tint = Color(0xFF005AC1),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "شبکه فعلی شما:",
                                color = Color(0xFF44474E),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3F4F9),
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFFE1E2EC),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
            ) {
                NavigationBarItem(
                    selected = activeTab.intValue == 1,
                    onClick = { activeTab.intValue = 1 },
                    icon = { Icon(Icons.Default.Star, "برگزیده‌ها & خروجی") },
                    label = { Text("آی‌پی گزیده", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF005AC1),
                        selectedTextColor = Color(0xFF005AC1),
                        unselectedIconColor = Color(0xFF44474E),
                        unselectedTextColor = Color(0xFF44474E),
                        indicatorColor = Color(0xFFD3E4FF)
                    )
                )

                NavigationBarItem(
                    selected = activeTab.intValue == 0,
                    onClick = { activeTab.intValue = 0 },
                    icon = { Icon(Icons.Default.Home, "اسکنر دیتاسنترها") },
                    label = { Text("اسکنر آی‌پی", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF005AC1),
                        selectedTextColor = Color(0xFF005AC1),
                        unselectedIconColor = Color(0xFF44474E),
                        unselectedTextColor = Color(0xFF44474E),
                        indicatorColor = Color(0xFFD3E4FF)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFBFF))
                .padding(innerPadding)
        ) {
            when (activeTab.intValue) {
                0 -> IpScannerViewTab(
                    viewModel = viewModel,
                    isScanning = isScanning,
                    scanProgress = scanProgress,
                    scannedCount = scannedCount,
                    totalCount = totalCount,
                    liveResults = liveResults,
                    logs = logs,
                    selectedProvider = selectedProvider,
                    customCidr = customCidr,
                    concurrency = concurrency,
                    timeout = timeout,
                    speedResults = speedResults,
                    isSpeedTesting = isSpeedTesting,
                    context = context
                )
                1 -> SavedResultsViewTab(
                    viewModel = viewModel,
                    allSavedIps = allScannedIps,
                    favoriteIps = favoriteIps,
                    speedResults = speedResults,
                    isSpeedTesting = isSpeedTesting,
                    context = context
                )
            }
        }
    }
}

// ---------------- TAB 1: IP SCANNER SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpScannerViewTab(
    viewModel: IpScannerViewModel,
    isScanning: Boolean,
    scanProgress: Float,
    scannedCount: Int,
    totalCount: Int,
    liveResults: List<com.example.scanner.ScanResult>,
    logs: List<String>,
    selectedProvider: String,
    customCidr: String,
    concurrency: Int,
    timeout: Int,
    speedResults: Map<String, Double>,
    isSpeedTesting: String?,
    context: Context
) {
    val providers = listOf("Cloudflare", "Google", "GitHub", "Akamai", "رنج دستی")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Density Dashboard Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat Card 1
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("آی‌پی‌های زنده", color = Color(0xFF44474E), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${liveResults.size}",
                        color = Color(0xFF005AC1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stat Card 2
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("دیتاسنتر فعلی", color = Color(0xFF44474E), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        selectedProvider,
                        color = Color(0xFF1B1B1F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Stat Card 3
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("سرعت اسکن", color = Color(0xFF44474E), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isScanning) "Ultra Fast" else "Idle",
                        color = if (isScanning) Color(0xFF005AC1) else Color(0xFF44474E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Step 1: Provider selection
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مرجع دیتاسنتر هدف آی‌پی:",
                        color = Color(0xFF44474E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "۱. مرجع اسکن",
                        color = Color(0xFF005AC1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Horizontal Flow Row-like provider capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.reversed().forEach { provider ->
                        val isSelected = provider == selectedProvider
                        Button(
                            onClick = { viewModel.selectedProvider.value = provider },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF005AC1) else Color(0xFFF3F4F9),
                                contentColor = if (isSelected) Color.White else Color(0xFF1B1B1F)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = provider, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // If "رنج دستی" is selected, draw text field for custom CIDR
                if (selectedProvider == "رنج دستی") {
                    OutlinedTextField(
                        value = customCidr,
                        onValueChange = { viewModel.customCidr.value = it },
                        label = { Text("وارد کردن رنج آی‌پی (مثال: 104.16.0.0/24)", fontSize = 11.sp) },
                        placeholder = { Text("104.16.0.0/24") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color(0xFFE1E2EC),
                            focusedLabelColor = Color(0xFF005AC1),
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF44474E),
                            focusedContainerColor = Color(0xFFF3F4F9),
                            unfocusedContainerColor = Color(0xFFF3F4F9)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_cidr_input")
                    )
                }
            }
        }

        // Step 2: Concurrency & Port settings
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "کاهش بارگذاری سوکت با تغییر مقادیر زیر:",
                        color = Color(0xFF44474E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "۲. پیکربندی پیشرفته هسته اسکنر",
                        color = Color(0xFF005AC1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Threads (Concurrency slider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "$concurrency نخ همزمان", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(text = "Concurrency Limit:", color = Color(0xFF44474E), fontSize = 11.sp)
                }
                Slider(
                    value = concurrency.toFloat(),
                    onValueChange = { viewModel.concurrency.value = it.toInt() },
                    valueRange = 10f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF005AC1),
                        activeTrackColor = Color(0xFF005AC1),
                        inactiveTrackColor = Color(0xFFE1E2EC)
                    ),
                    modifier = Modifier.testTag("concurrency_slider")
                )

                // Timeout slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "$timeout میلی‌ثانیه", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(text = "Socket Timeout Config:", color = Color(0xFF44474E), fontSize = 11.sp)
                }
                Slider(
                    value = timeout.toFloat(),
                    onValueChange = { viewModel.timeout.value = it.toInt() },
                    valueRange = 500f..3000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF005AC1),
                        activeTrackColor = Color(0xFF005AC1),
                        inactiveTrackColor = Color(0xFFE1E2EC)
                    ),
                    modifier = Modifier.testTag("timeout_slider")
                )
            }
        }

        // Controls Area (Big Launch Button + Progress display)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isScanning) {
                // Circular Glowing Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF005AC1),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFFE1E2EC)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(scanProgress * 100).toInt()}%",
                            color = Color(0xFF1B1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "$scannedCount/$totalCount",
                            color = Color(0xFF44474E),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.stopIpScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("stop_scan_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "توقف", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("توقف فرآیند اسکن زنده", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = { viewModel.startIpScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_scan_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "شروع", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شروع اسکن آی‌پی‌های سالم", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }

        // Real-time Console Log (Retro terminal look in high contrast white-slate background)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "گزارش زنده فعالیت هسته اسکنر:",
                    color = Color(0xFF44474E),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "در انتظار شروع عملیات ... گزارش ترافیک اینجا نمایش داده می‌شود.",
                                color = Color(0xFF8E9099),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                    } else {
                        items(logs) { logMsg ->
                            Text(
                                text = logMsg,
                                color = if (logMsg.contains("✅") || logMsg.contains("🟢")) Color(0xFF005AC1) else Color(0xFF1B1B1F),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }

        // Section: Real-time scan result list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${liveResults.size} آی‌پی فعال",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xFF005AC1), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = "نتایج زنده اسکن دیتاسنتر:",
                color = Color(0xFF1B1B1F),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (liveResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "بدون نتیجه",
                        tint = Color(0xFF8E9099),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هیچ آی‌پی فعالی روی بستر فیلترینگ فعلی یافت نشد.\nبرای شروع روی دکمه اسکن بزنید.",
                        color = Color(0xFF44474E),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Live results mapping
            liveResults.forEach { result ->
                IpResultItemCard(
                    scannedIp = ScannedIp(
                        ip = result.ip,
                        port = result.port,
                        latencyMs = result.latencyMs,
                        downloadSpeedKbps = speedResults[result.ip] ?: 0.0,
                        provider = result.provider,
                        connectionType = "زنده"
                    ),
                    isTestingSpeed = isSpeedTesting == result.ip,
                    onSpeedTest = { viewModel.testIpSpeed(it) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CleanIP", result.ip))
                        Toast.makeText(context, "آی‌پی کپی شد: ${result.ip}", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {} // live scan results won't delete immediately
                )
            }
        }
    }
}

// ---------------- TAB 2: SNI SCANNER TAB ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniScannerViewTab(
    viewModel: IpScannerViewModel,
    isSniScanning: Boolean,
    sniProgress: Float,
    scannedSniCount: Int,
    totalSniCount: Int,
    sniLogs: List<String>,
    savedSnis: List<SavedSni>,
    timeout: Int,
    context: Context
) {
    var gatewayIp by remember { mutableStateOf("104.16.123.96") }
    var selectedCategory by remember { mutableStateOf("همه") }
    val scrollState = rememberScrollState()

    fun getSniCategory(host: String): String {
        return when {
            host.contains("cloudflare") || host.contains("gopro") || host.contains("twitch") || host.contains("disney") || host.contains("spotify") || host.contains("nasa.gov") -> "شبکه‌های توزیع محتوا (CDN)"
            host.contains("github") || host.contains("coursera") || host.contains("medium") || host.contains("wikipedia") || host.contains("stackoverflow") || host.contains("android.com") -> "آموزشی و توسعه‌دهندگان"
            host.contains("teams") || host.contains("skype") || host.contains("zoom") || host.contains("salesforce") || host.contains("office") || host.contains("webex") -> "سرویس‌های سازمانی و تجاری"
            host.contains("time.ir") || host.contains("telewebion") || host.contains("divar") -> "سایت‌ها و ترافیک داخلی"
            else -> "سایر دامنه‌ها"
        }
    }

    val filteredSnis = remember(savedSnis, selectedCategory) {
        if (selectedCategory == "همه") {
            savedSnis
        } else {
            savedSnis.filter { sni ->
                val cat = getSniCategory(sni.host)
                when (selectedCategory) {
                    "مهم‌ترین CDNs" -> cat == "شبکه‌های توزیع محتوا (CDN)"
                    "آموزشی و داکس" -> cat == "آموزشی و توسعه‌دهندگان"
                    "وبینار و سازمانی" -> cat == "سرویس‌های سازمانی و تجاری"
                    "ترافیک داخلی" -> cat == "سایت‌ها و ترافیک داخلی"
                    else -> true
                }
            }
        }
    }

    // 🌟 Intelligent Recommendation Card logic
    val bestSni = remember(savedSnis) {
        savedSnis
            .filter { it.isWorking && it.latencyMs > 0 && !getSniCategory(it.host).contains("داخلی") }
            .minByOrNull { it.latencyMs }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step 1: Network Insight Header Explanation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ارزیابی سلامت و معرفی دامنه‌های پاک (Clean SNIs)",
                    color = Color(0xFF005AC1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "فناوری تفکیک عمیق بسته‌ها (DPI) در سیستم مسدودسازی اینترنت کشور، برخی نام‌های دامنه را فیلتر می‌کند. با استفاده از این ابزار هوشمند، دامنه‌هایی که عاری از دیوار رمزنگاری TLS هستند را کشف کرده و در برنامه‌های v2rayNG ،Nekobox و ... کپی کنید.",
                    color = Color(0xFF44474E),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                HorizontalDivider(color = Color(0xFFE1E2EC), thickness = 0.5.dp)

                OutlinedTextField(
                    value = gatewayIp,
                    onValueChange = { gatewayIp = it },
                    label = { Text("آی‌پی دروازه آزمایشی (پیش‌فرض: کلودفلر تمیز)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF005AC1),
                        unfocusedBorderColor = Color(0xFFE1E2EC),
                        focusedLabelColor = Color(0xFF005AC1),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF44474E),
                        focusedContainerColor = Color(0xFFF3F4F9),
                        unfocusedContainerColor = Color(0xFFF3F4F9)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 🌟 Intelligent Recommendation Card UI
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (bestSni != null) Color(0xFFEBF3FF) else Color(0xFFFAFAFD)
            ),
            border = BorderStroke(1.dp, if (bestSni != null) Color(0xFF8AB4F8) else Color(0xFFE1E2EC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "پیشنهاد هوشمند دامنه پاک (SNI)",
                        color = Color(0xFF005AC1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Recommended",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (bestSni != null) {
                    Text(
                        text = "بر اساس اسکن اخیر روی شبکه اینترنت شما، دامنه زیر دارای کمترین تاخیر پاسخگویی هندشیک و فاقد بلاکینگ DPI در اپراتور شماست:",
                        color = Color(0xFF1B1B1F),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD3E4FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("clean_sni", bestSni.host)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "دامنه ${bestSni.host} کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, 
                                contentDescription = "Copy",
                                tint = Color(0xFF005AC1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = bestSni.host,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF005AC1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تاخیر لایو: ${bestSni.latencyMs} میلی‌ثانیه",
                                color = Color(0xFF44474E),
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Text(
                        text = "💡 راهنما: این دامنه را در بخش SNI کانفیگ‌های مستقیم خود قرار دهید تا اینترنت شما دور زنندهٔ فیلترینگ شود.",
                        color = Color(0xFF44474E),
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                } else {
                    Text(
                        text = "برای دریافت پیشنهاد هوشمندِ مبتنی بر پینگ و سلامت لایو، دکمه «شروع ارزیابی هوشمند» زیر را کلیک کنید تا متناسب با نوع اپراتور اینترنت شما (همراه‌اول، ایرانسل یا خانگی) بهترین SNIs استخراج گردند.",
                        color = Color(0xFF44474E),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // TRIGGER CONTROL BUTTONS
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSniScanning) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                    CircularProgressIndicator(
                        progress = { sniProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF005AC1),
                        strokeWidth = 5.dp,
                        trackColor = Color(0xFFE1E2EC)
                    )
                    Text(
                        text = "$scannedSniCount/$totalSniCount",
                        color = Color(0xFF1B1B1F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { viewModel.stopSniScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("قطع سنجش فعال دامنه‌ها", color = Color.White)
                }
            } else {
                Button(
                    onClick = { viewModel.startSniScan(gatewayIp) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "آزمایش دامنه‌ها", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شروع تست و ارزیابی هوشمند", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Interactive Live Logs Terminal
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "ریز تراکنش‌های دیواره آتش بر روی بسته‌های TLS:",
                    color = Color(0xFF44474E),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (sniLogs.isEmpty()) {
                        item {
                            Text(
                                text = "هیچ لاگی ثبت نشده است. دکمه ارزیابی را فشار دهید.",
                                color = Color(0xFF8E9099),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                    } else {
                        items(sniLogs) { itemLog ->
                            Text(
                                text = itemLog,
                                color = if (itemLog.contains("✅") || itemLog.contains("تأیید")) Color(0xFF005AC1) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }

        // Classified Categories Filter Title
        Text(
            text = "دسته‌بندی دامنه‌ها بر اساس کاربری و عملکرد:",
            color = Color(0xFF1B1B1F),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf("همه", "مهم‌ترین CDNs", "آموزشی و داکس", "وبینار و سازمانی", "ترافیک داخلی")
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color(0xFF005AC1) else Color(0xFFF3F4F9),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF005AC1) else Color(0xFFE1E2EC),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else Color(0xFF44474E),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Domain Items List
        filteredSnis.forEach { sni ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Copy button on left side
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("sni", sni.host)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "کپی شد: ${sni.host}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "کپی آدرس",
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Content on right side
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sni.lastTestedAt > 0) {
                                if (sni.isWorking) {
                                    Text(
                                        text = "${sni.latencyMs}ms سالم [بدون بلاک]",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF005AC1), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "مسدودشده با DPI",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "تست نشده",
                                    color = Color(0xFF8E9099),
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sni.host,
                                color = Color(0xFF1B1B1F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End
                            )
                        }
                        if (sni.note.isNotBlank()) {
                            Text(
                                text = sni.note,
                                color = Color(0xFF44474E),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 3: SAVED RESULTS VIEW SCREEN ----------------
@Composable
fun SavedResultsViewTab(
    viewModel: IpScannerViewModel,
    allSavedIps: List<ScannedIp>,
    favoriteIps: List<ScannedIp>,
    speedResults: Map<String, Double>,
    isSpeedTesting: String?,
    context: Context
) {
    val scrollState = rememberScrollState()
    val listToDisplay = if (favoriteIps.isNotEmpty()) favoriteIps else allSavedIps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مدیریت خروجی آی‌پی‌های سالم",
                    color = Color(0xFF005AC1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "تمامی آی‌پی‌های سالمی که اسکن شده‌اند در جدول زیر ذخیره می‌شوند. می‌توانید به راحتی همه را یکجا کپی کنید و در برنامه‌های v2rayNG، Nekobox، Shadowrocket یا Streisand برای فرار از فیلترینگ استفاده نمایید.",
                    color = Color(0xFF44474E),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear database history
                    Button(
                        onClick = {
                            viewModel.clearAllScanned()
                            Toast.makeText(context, "کل ترافیک تاریخچه حذف شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFDFBFF),
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف کل")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف تاریخچه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy all unblocked IPs to dashboard
                    Button(
                        onClick = {
                            if (listToDisplay.isEmpty()) {
                                Toast.makeText(context, "لیست خالی است", Toast.LENGTH_SHORT).show()
                            } else {
                                val textToCopy = listToDisplay.joinToString(separator = "\n") { it.ip }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AllCleanIPs", textToCopy))
                                Toast.makeText(context, "${listToDisplay.size} آی‌پی سالم یکجا کپی شد", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "کپی همگانی")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی همگانی آی‌پی‌ها", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // IP result list display
        Text(
            text = if (favoriteIps.isNotEmpty()) "آی‌پی‌های نشان‌شده (برگزیده):" else "تمامی آی‌پی‌های در تاریخچه محلی:",
            color = Color(0xFF1B1B1F),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        if (listToDisplay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هنوز هیچ آی‌پی پاک تایید شده‌ای ذخیره نشده است.\nدر زبانه اول دکمه ضربان اسکنر را فشار دهید.",
                    color = Color(0xFF8E9099),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            listToDisplay.forEach { scannedIp ->
                IpResultItemCard(
                    scannedIp = scannedIp,
                    isTestingSpeed = isSpeedTesting == scannedIp.ip,
                    onSpeedTest = { viewModel.testIpSpeed(it) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CleanIP", scannedIp.ip))
                        Toast.makeText(context, "آی‌پی کپی شد: ${scannedIp.ip}", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { viewModel.deleteScannedIp(scannedIp) },
                    onToggleFavorite = { viewModel.toggleFavorite(scannedIp) }
                )
            }
        }
    }
}

// ---------------- REUSABLE COMPOSE COMPONENT: IP CARD ----------------
@Composable
fun IpResultItemCard(
    scannedIp: ScannedIp,
    isTestingSpeed: Boolean,
    onSpeedTest: (ScannedIp) -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: IP and latency indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left elements: Copy and Delete and Favorite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = { onToggleFavorite() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "نشان کردن",
                                tint = if (scannedIp.isFavorite) Color(0xFFF59E0B) else Color(0xFF8E9099),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onCopy() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "کپی آدرس",
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (scannedIp.connectionType != "زنده") {
                        IconButton(
                            onClick = { onDelete() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف رکورد",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                        )
                    }
                }
                }

                // Right elements: Host IP info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Network Latency Badge
                    val latencyVal = scannedIp.latencyMs
                    val badgeColor = when {
                        latencyVal < 80 -> Color(0xFF10B981) // Neon Green
                        latencyVal < 185 -> Color(0xFFF59E0B) // Golden Amber
                        else -> Color(0xFFEF4444) // Hot Rose Red
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${latencyVal}ms",
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = scannedIp.ip,
                        color = Color(0xFF1B1B1F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("ip_text_${scannedIp.ip}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE1E2EC), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Speed Test controls & Results
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive real speed test outcomes
                if (isTestingSpeed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color(0xFF005AC1),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اندازه‌گیری فعال...",
                            color = Color(0xFF8E9099),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.clickable { onSpeedTest(scannedIp) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "دکمه سرعت",
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تست و بررسی سرعت واقعی",
                            color = Color(0xFF005AC1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Speed indicator label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (scannedIp.downloadSpeedKbps > 0) {
                            val speedValue = scannedIp.downloadSpeedKbps
                            if (speedValue > 1000) {
                                String.format("%.2f Mbps", speedValue / 1024.0)
                            } else {
                                String.format("%.0f Kbps", speedValue)
                            }
                        } else {
                            "تست نشده 🛑"
                        },
                        color = if (scannedIp.downloadSpeedKbps > 0) Color(0xFF005AC1) else Color(0xFF8E9099),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "سرعت تک‌اتصال:",
                        color = Color(0xFF44474E),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
