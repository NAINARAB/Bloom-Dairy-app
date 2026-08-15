package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import mobile.dairy.app.core.UsageStatsHelper
import mobile.dairy.app.ui.insights.InsightsViewModel
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalHabitsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val allRatings = ctx.ratings

    val todayDate = remember { LocalDate.now() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsagePermission by remember { mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context)) }
    var selectedScreenTimeDayIndex by remember { mutableIntStateOf(6) }
    var showInfoCard by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = UsageStatsHelper.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var liveScreenTime by remember { mutableStateOf<List<mobile.dairy.app.domain.ScreenTimeDay>>(emptyList()) }
    LaunchedEffect(hasUsagePermission) {
        if (hasUsagePermission) {
            liveScreenTime = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                UsageStatsHelper.getLast7DaysUsage(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Habits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoCard = !showInfoCard },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFA78BFA))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            if (showInfoCard) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Productivity Impact", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• See exactly how your app usage impacts your daily productivity rating.\n" +
                                        "• We don't save this data—it's generated on-the-fly directly from your device.",
                                style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle("Screen Time & Productivity Impact")
            BloomCard {
                if (!hasUsagePermission) {
                    // Permission Gate UI
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QueryStats, contentDescription = "Stats", tint = Color(0xFFA78BFA), modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Unlock Digital Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "See exactly how your app usage impacts your daily productivity rating. We don't save this data—it's generated on-the-fly directly from your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { context.startActivity(UsageStatsHelper.getUsageSettingsIntent()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Grant Access in Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val last7Days = liveScreenTime
                    
                    if (last7Days.isEmpty()) {
                        Text("Loading screen time data...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } else {
                        val activeIndex = selectedScreenTimeDayIndex.coerceIn(0, last7Days.size - 1)
                        val selectedDay = last7Days[activeIndex]

                        // Chart Data
                        val chartData = mutableListOf<Triple<String, Pair<Float, Float>, Float>>()
                        for (i in 0..6) {
                            val d = todayDate.minusDays((6 - i).toLong())
                            val dKey = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val dayOfWeek = d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                            
                            val found = liveScreenTime.find { it.date == dKey }
                            val productiveMins = found?.apps?.filter { it.kind == "productive" }?.sumOf { it.minutes }?.toFloat() ?: 0f
                            val distractingMins = found?.apps?.filter { it.kind == "distracting" }?.sumOf { it.minutes }?.toFloat() ?: 0f
                            
                            val rating = allRatings.find { it.date == dKey }?.productivity?.toFloat() ?: 0f
                            
                            chartData.add(Triple(dayOfWeek, Pair(productiveMins, distractingMins), rating))
                        }

                        // Header Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(12.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
                                Text("Distracting", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(12.dp).background(Color(0xFF10B981), RoundedCornerShape(2.dp)))
                                Text("Productive", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(12.dp, 3.dp).background(Color(0xFF06B6D4)))
                                Text("Prod. Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Combo Chart with Tap Selection & Active Day Highlight
                        DigitalHabitsComboChart(
                            data = chartData,
                            selectedIndex = activeIndex,
                            onDaySelected = { selectedScreenTimeDayIndex = it },
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )

                        Spacer(Modifier.height(24.dp))
                        
                        // AI Insight Generation
                        val mostDistracting = chartData.maxByOrNull { it.second.second }
                        var insightText = "Your digital habits are looking balanced this week!"
                        
                        if (mostDistracting != null && mostDistracting.second.second > 60f) {
                            val distHours = (mostDistracting.second.second / 60).toInt()
                            val distMins = (mostDistracting.second.second % 60).toInt()
                            insightText = "Your most distracting day was ${mostDistracting.first} (${distHours}h ${distMins}m). Notice how spikes in red bars often push your cyan Productivity score down!"
                        } else if (chartData.all { it.second.first > it.second.second }) {
                            insightText = "Excellent! You spent more time on productive apps than distracting ones every single day this week."
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Insights, contentDescription = "Insight", tint = Color(0xFFA78BFA), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(insightText, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE0E7FF), lineHeight = 18.sp)
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        
                        // Interactive Date Switcher Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (activeIndex > 0) selectedScreenTimeDayIndex = activeIndex - 1 },
                                enabled = activeIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft, 
                                    contentDescription = "Previous Day",
                                    tint = if (activeIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }

                            val parsedDate = try { LocalDate.parse(selectedDay.date) } catch (e: Exception) { todayDate }
                            val isToday = parsedDate == todayDate
                            val displayDate = if (isToday) "Today, ${parsedDate.format(DateTimeFormatter.ofPattern("MMM d"))}" else parsedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

                            Text(
                                text = displayDate, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = { if (activeIndex < last7Days.size - 1) selectedScreenTimeDayIndex = activeIndex + 1 },
                                enabled = activeIndex < last7Days.size - 1
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight, 
                                    contentDescription = "Next Day",
                                    tint = if (activeIndex < last7Days.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))

                        // Categorized List for Selected Day
                        val productiveApps = selectedDay.apps.filter { it.kind == "productive" }
                        val distractingApps = selectedDay.apps.filter { it.kind == "distracting" }
                        val neutralApps = selectedDay.apps.filter { it.kind == "neutral" }

                        @Composable
                        fun AppCategoryList(title: String, color: Color, apps: List<mobile.dairy.app.domain.AppUsage>) {
                            if (apps.isEmpty()) return
                            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                apps.take(5).forEach { app -> // Show top 5 per category
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        AppIcon(
                                            packageName = app.packageName,
                                            fallbackChar = app.name,
                                            fallbackColor = color,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            val appHours = app.minutes.toInt() / 60
                                            val appMins = app.minutes.toInt() % 60
                                            val timeStr = if (appHours > 0) "$appHours hr, $appMins min" else if (appMins > 0) "$appMins minutes" else "Less than 1 minute"
                                            Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        AppCategoryList("Distracting Habits", Color(0xFFEF4444), distractingApps)
                        AppCategoryList("Productive Toolkit", Color(0xFF10B981), productiveApps)
                        AppCategoryList("Other Apps", Color(0xFF94A3B8), neutralApps)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AppIcon(packageName: String?, fallbackChar: String, fallbackColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        if (packageName != null) {
            try {
                context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.clip(RoundedCornerShape(10.dp)))
    } else {
        Box(modifier = modifier.background(fallbackColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(fallbackChar.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = fallbackColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DigitalHabitsComboChart(
    data: List<Triple<String, Pair<Float, Float>, Float>>,
    selectedIndex: Int = -1,
    onDaySelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Row(modifier = modifier) {
        val xAxisPaddingDp = 24.dp
        
        val rawMaxMins = (data.maxOfOrNull { it.second.first + it.second.second } ?: 60f).coerceAtLeast(30f)
        val maxTotalMins: Float = when {
            rawMaxMins <= 60f -> 60f
            rawMaxMins <= 120f -> 120f
            rawMaxMins <= 240f -> 240f
            rawMaxMins <= 360f -> 360f
            rawMaxMins <= 480f -> 480f
            rawMaxMins <= 720f -> 720f
            rawMaxMins <= 960f -> 960f
            rawMaxMins <= 1200f -> 1200f
            else -> kotlin.math.ceil(rawMaxMins / 240f) * 240f
        }

        val ticks = listOf(maxTotalMins, maxTotalMins * 0.75f, maxTotalMins * 0.5f, maxTotalMins * 0.25f, 0f)

        fun formatTick(mins: Float): String {
            if (mins == 0f) return "0"
            val totalHours = mins / 60f
            return if (totalHours >= 1f && totalHours % 1f == 0f) {
                "${totalHours.toInt()}h"
            } else if (totalHours >= 1f) {
                String.format(java.util.Locale.US, "%.1fh", totalHours)
            } else {
                "${mins.toInt()}m"
            }
        }

        Column(
            modifier = Modifier.fillMaxHeight().width(36.dp).padding(bottom = xAxisPaddingDp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            ticks.forEach { tick ->
                Text(formatTick(tick), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = axisLabelColor)
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val yAxisPadding = 15f
                        val rightPadding = 15f
                        val chartWidth = width - yAxisPadding - rightPadding
                        val count = (data.size - 1).coerceAtLeast(1)
                        
                        var minDiff = Float.MAX_VALUE
                        var closestIndex = 0
                        for (i in data.indices) {
                            val barX = yAxisPadding + (i.toFloat() / count) * chartWidth
                            val diff = kotlin.math.abs(offset.x - barX)
                            if (diff < minDiff) {
                                minDiff = diff
                                closestIndex = i
                            }
                        }
                        onDaySelected(closestIndex)
                    }
                }
        ) {
            val width = size.width
            val chartHeight = size.height - xAxisPaddingDp.toPx()
            val yAxisPadding = 15f
            val rightPadding = 15f
            val chartWidth = width - yAxisPadding - rightPadding

            ticks.forEachIndexed { idx, _ ->
                val yPos = (idx.toFloat() / (ticks.size - 1)) * chartHeight
                drawLine(gridLineColor, Offset(0f, yPos), Offset(width, yPos), 1.dp.toPx())
            }

            fun getXPos(index: Int): Float {
                val count = (data.size - 1).coerceAtLeast(1)
                return yAxisPadding + (index.toFloat() / count) * chartWidth
            }

            val barWidth = 16.dp.toPx()
            val radius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            
            data.forEachIndexed { i, triple ->
                val x = getXPos(i)
                val prodHeight = (triple.second.first / maxTotalMins) * chartHeight
                val distHeight = (triple.second.second / maxTotalMins) * chartHeight
                
                val distTopY = chartHeight - prodHeight - distHeight
                
                if (i == selectedIndex) {
                    drawRoundRect(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        topLeft = Offset(x - barWidth * 0.9f, 0f),
                        size = Size(barWidth * 1.8f, chartHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }

                if (distHeight > 0) {
                    drawRoundRect(
                        color = Color(0xFFEF4444).copy(alpha = 0.85f),
                        topLeft = Offset(x - barWidth / 2f, distTopY),
                        size = Size(barWidth, distHeight + (if (prodHeight > 0) 4.dp.toPx() else 0f)),
                        cornerRadius = radius
                    )
                }
                
                if (prodHeight > 0) {
                    drawRoundRect(
                        color = Color(0xFF10B981).copy(alpha = 0.85f),
                        topLeft = Offset(x - barWidth / 2f, chartHeight - prodHeight),
                        size = Size(barWidth, prodHeight),
                        cornerRadius = radius
                    )
                }
            }

            if (data.isNotEmpty()) {
                val prodPath = Path()
                data.forEachIndexed { i, triple ->
                    val x = getXPos(i)
                    val rating = triple.third
                    val y = chartHeight - (rating / 10f) * chartHeight
                    
                    if (i == 0) {
                        if (rating > 0) prodPath.moveTo(x, y)
                    } else {
                        val prevRating = data[i - 1].third
                        if (rating > 0 && prevRating > 0) {
                            val prevX = getXPos(i - 1)
                            val prevY = chartHeight - (prevRating / 10f) * chartHeight
                            val cx = (prevX + x) / 2f
                            prodPath.cubicTo(cx, prevY, cx, y, x, y)
                        } else if (rating > 0) {
                            prodPath.moveTo(x, y)
                        }
                    }
                }
                drawPath(prodPath, Color(0xFF06B6D4), style = Stroke(2.dp.toPx()))
                
                drawIntoCanvas { canvas ->
                    data.forEachIndexed { i, triple ->
                        val x = getXPos(i)
                        
                        val rating = triple.third
                        if (rating > 0) {
                            val y = chartHeight - (rating / 10f) * chartHeight
                            drawCircle(Color(0xFF06B6D4), 4.dp.toPx(), Offset(x, y))
                            drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                        }
                        
                        canvas.nativeCanvas.drawText(triple.first, x, size.height - 8f, textPaint)
                    }
                }
            }
        }
    }
}
