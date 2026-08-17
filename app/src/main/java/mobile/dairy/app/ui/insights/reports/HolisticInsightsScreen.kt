package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.ScreenTimeDay
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ArrowDropDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolisticInsightsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val allRatings = ctx.ratings
    val allEntries = ctx.entries
    val allScreenTime = ctx.screenTime

    val todayDate = remember { LocalDate.now() }
    var selectedMonthKey by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var selectedMonthLabel by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }

    val monthRatings = remember(allRatings, selectedMonthKey) {
        allRatings.filter { it.date.startsWith(selectedMonthKey) }
    }
    val monthEntries = remember(allEntries, selectedMonthKey) {
        allEntries.filter { it.date.startsWith(selectedMonthKey) }
    }
    val monthScreenTime = remember(allScreenTime, selectedMonthKey) {
        allScreenTime.filter { it.date.startsWith(selectedMonthKey) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Holistic Insights", fontWeight = FontWeight.Bold) },
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
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Chart Info",
                            tint = Color(0xFFA78BFA)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Period",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showMonthPicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedMonthLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                            Text("Understanding Holistic Insights", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Sleep vs Stress: Dots map a day's Sleep Quality (X-axis, 1-10) against its Stress Level (Y-axis, 1-10). If dots group towards the top-left, poor sleep equals high stress!\n" +
                                "• Best Day Formula: Shows your most frequent activities and apps on days rated 9 or 10 overall.",
                                style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 1. Sleep & Stress Correlation
            SectionTitle("1. Sleep vs. Stress Correlation")
            SleepStressScatterPlot(monthRatings)

            Spacer(Modifier.height(24.dp))

            // 2. The "Best Day" Formula
            SectionTitle("2. The \"Best Day\" Formula")
            BestDayFormula(monthRatings, monthEntries, monthScreenTime)

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMonthPicker) {
        mobile.dairy.app.ui.insights.reports.MonthYearPickerDialog(
            initialMonthKey = selectedMonthKey,
            onDateSelected = { key, label ->
                selectedMonthKey = key
                selectedMonthLabel = label
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
fun SleepStressScatterPlot(ratings: List<DailyRating>) {
    val validRatings = ratings.filter { it.sleep != null && it.stress != null }

    BloomCard {
        Text("Sleep quality (X) vs Stress level (Y)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Discover if poor sleep equals higher stress. (1=Low, 10=High)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        if (validRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Not enough data to plot correlation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@BloomCard
        }

        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        val pointColor = Color(0xFF8B5CF6)
        
        val density = LocalDensity.current
        val textPaint = remember(density) {
            android.graphics.Paint().apply {
                color = labelColor
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            val padding = 24.dp.toPx()
            val w = size.width - padding
            val h = size.height - padding
            
            // Draw Axes
            drawLine(axisColor, Offset(padding, 0f), Offset(padding, h), strokeWidth = 2f) // Y Axis (Stress)
            drawLine(axisColor, Offset(padding, h), Offset(size.width, h), strokeWidth = 2f) // X Axis (Sleep)
            
            // Draw Labels
            drawContext.canvas.nativeCanvas.drawText("Stress", padding / 2, 10.dp.toPx(), textPaint)
            drawContext.canvas.nativeCanvas.drawText("Sleep", size.width - 20.dp.toPx(), h + 16.dp.toPx(), textPaint)
            
            val maxVal = 10f
            
            // Draw Grid marks for 1, 5, 10
            listOf(1f, 5f, 10f).forEach { value ->
                val px = padding + ((value / maxVal) * (w - padding))
                val py = h - ((value / maxVal) * h)
                
                // X axis ticks
                drawLine(axisColor, Offset(px, h), Offset(px, h + 4.dp.toPx()), strokeWidth = 2f)
                drawContext.canvas.nativeCanvas.drawText(value.toInt().toString(), px, h + 16.dp.toPx(), textPaint)
                
                // Y axis ticks
                drawLine(axisColor, Offset(padding - 4.dp.toPx(), py), Offset(padding, py), strokeWidth = 2f)
                drawContext.canvas.nativeCanvas.drawText(value.toInt().toString(), padding - 10.dp.toPx(), py + 4.dp.toPx(), textPaint)
            }
            
            // Jitter map to prevent exact overlap
            val pointCounts = mutableMapOf<Pair<Int, Int>, Int>()

            validRatings.forEach { r ->
                val sleep = r.sleep!!
                val stress = r.stress!!
                val key = Pair(sleep, stress)
                val count = pointCounts.getOrDefault(key, 0)
                pointCounts[key] = count + 1
                
                val px = padding + ((sleep / maxVal) * (w - padding))
                val py = h - ((stress / maxVal) * h)
                
                // Add slight jitter for overlapping points
                val jitterX = (count * 4).dp.toPx()
                val jitterY = (count * 4).dp.toPx()

                drawCircle(
                    color = pointColor.copy(alpha = 0.7f),
                    radius = 6.dp.toPx(),
                    center = Offset(px + jitterX, py - jitterY)
                )
            }
        }
    }
}

@Composable
fun BestDayFormula(ratings: List<DailyRating>, entries: List<JournalEntry>, screenTime: List<ScreenTimeDay>) {
    val bestDays = ratings.filter { it.overall == 9 || it.overall == 10 }.map { it.date }

    if (bestDays.isEmpty()) {
        BloomCard {
            Text("No \"perfect\" days (rated 9 or 10) recorded this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val bestEntries = entries.filter { it.date in bestDays }
    val bestScreenTime = screenTime.filter { it.date in bestDays }

    val tagsFreq = mutableMapOf<String, Int>()
    val peopleFreq = mutableMapOf<String, Int>()
    val causesFreq = mutableMapOf<String, Int>()
    val appsFreq = mutableMapOf<String, Int>()

    bestEntries.forEach { entry ->
        entry.tags.forEach { tagsFreq[it] = tagsFreq.getOrDefault(it, 0) + 1 }
        entry.people.forEach { peopleFreq[it.name] = peopleFreq.getOrDefault(it.name, 0) + 1 }
        entry.moodCauses.forEach { causesFreq[it] = causesFreq.getOrDefault(it, 0) + 1 }
    }

    bestScreenTime.forEach { st ->
        st.apps.filter { it.kind == "productive" }.forEach { app ->
            appsFreq[app.name] = appsFreq.getOrDefault(app.name, 0) + 1
        }
    }

    val topTags = tagsFreq.toList().sortedByDescending { it.second }.take(3).map { it.first }
    val topPeople = peopleFreq.toList().sortedByDescending { it.second }.take(3).map { it.first }
    val topCauses = causesFreq.toList().sortedByDescending { it.second }.take(3).map { it.first }
    val topApps = appsFreq.toList().sortedByDescending { it.second }.take(3).map { it.first }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BloomCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFF10B981))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Mood Triggers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val desc = if (topCauses.isNotEmpty()) topCauses.joinToString(", ") else "No specific triggers recorded."
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        BloomCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF3B82F6))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("People You're With", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val desc = if (topPeople.isNotEmpty()) topPeople.joinToString(", ") else "No people recorded."
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        BloomCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFFF59E0B))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Activities / Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val desc = if (topTags.isNotEmpty()) topTags.joinToString(", ") else "No activities recorded."
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        BloomCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF8B5CF6))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Productive Apps Used", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val desc = if (topApps.isNotEmpty()) topApps.joinToString(", ") else "No productive apps recorded."
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
