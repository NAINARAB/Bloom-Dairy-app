package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import mobile.dairy.app.core.UsageStatsHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalReportsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val allGoalUpdates = ctx.goalUpdates
    val allRatings = ctx.ratings
    val allScreenTime = ctx.screenTime
    val rawGoals = ctx.goals

    val todayDate = remember { LocalDate.now() }
    var selectedMonthKey by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var selectedMonthLabel by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }

    val monthUpdates = remember(allGoalUpdates, selectedMonthKey) {
        allGoalUpdates.filter { it.date.startsWith(selectedMonthKey) }
    }
    
    val allGoals = remember(rawGoals, monthUpdates, selectedMonthKey) {
        rawGoals.filter { goal ->
            val createdDate = java.time.Instant.ofEpochMilli(goal.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val createdMonthKey = createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            
            if (createdMonthKey > selectedMonthKey) {
                false
            } else {
                val hasLogData = monthUpdates.any { it.goalId == goal.id }
                goal.status == "active" || hasLogData
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals & Productivity", fontWeight = FontWeight.Bold) },
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
            Spacer(Modifier.height(8.dp))

            // Top Header: Month Picker & Summary Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Report Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showMonthPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedMonthLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Summary Card
                val monthCompletedGoals = allGoals.count { it.status == "completed" || it.progress >= 100 }
                val monthTotalGoals = allGoals.size
                val monthTotalMins = monthUpdates.sumOf { it.minutes }
                val monthTotalHrs = if (monthTotalMins > 0) String.format(Locale.US, "%.1f", monthTotalMins / 60f) else "0"
                val monthAvgEffort = monthUpdates.mapNotNull { it.effort }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
                val monthAvgEffortStr = if (monthAvgEffort > 0) String.format(Locale.US, "%.1f", monthAvgEffort) else "0"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Summary", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                            Text("Goals Completed: $monthCompletedGoals/$monthTotalGoals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF8B5CF6), CircleShape))
                            Text("Time Logged: $monthTotalHrs hrs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                            Text("Avg Effort: $monthAvgEffortStr/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
                            Text("Productivity Metrics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Track your goal effort versus time spent.\n" +
                                        "• Monitor overall task completion.\n" +
                                        "• See how screen time affects your productivity rating.",
                                style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 1. Goal Consistency & Effort
            SectionTitle("1. Goal Consistency & Effort")
            BloomCard {
                Text("Time Invested vs Effort ($selectedMonthLabel)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF10B981), RoundedCornerShape(2.dp)))
                        Text("Minutes Logged", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp, 3.dp).background(Color(0xFFF59E0B)))
                        Text("Effort Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Aggregate minutes and avg effort per day
                val dailyEffortStats = monthUpdates.groupBy { it.date }.mapValues { entry ->
                    val totalMins = entry.value.sumOf { it.minutes }
                    val avgEffort = entry.value.mapNotNull { it.effort }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                    totalMins to avgEffort
                }.entries.sortedBy { it.key }.takeLast(10) // Show last 10 active days

                if (dailyEffortStats.isEmpty()) {
                    Text("No goal updates logged for this month.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    GoalEffortComboChart(
                        data = dailyEffortStats.map { (date, stats) ->
                            val dayLabel = date.substringAfterLast("-").toInt().toString()
                            Triple(dayLabel, stats.first.toFloat(), stats.second)
                        },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // 2. Goal Execution & Milestone Progress
            SectionTitle("2. Goal Execution & Milestone Progress")
            BloomCard {
                Text("Overall Execution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                val totalGoalsCount = allGoals.size
                val completedGoalsCount = allGoals.count { it.status == "completed" || it.progress >= 100 }
                val avgGoalProgress = if (allGoals.isNotEmpty()) allGoals.map { it.progress }.average().toInt() else 0

                val totalMilestones = allGoals.sumOf { it.milestones.size }
                val completedMilestones = allGoals.sumOf { it.milestones.count { m -> m.done } }
                
                val totalRoutines = allGoals.sumOf { it.dailyTasks.size }

                if (totalGoalsCount == 0) {
                    Text("No goals found. Create a goal in the Goals tab to track your execution progress!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Donut Chart showing Avg Progress & Milestone Rate
                        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                            TaskCompletionDonut(
                                taskPercent = avgGoalProgress / 100f,
                                milestonePercent = if (totalMilestones > 0) completedMilestones.toFloat() / totalMilestones else 0f,
                                modifier = Modifier.size(100.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$avgGoalProgress%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Execution", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))

                        val monthTotalMins = monthUpdates.sumOf { it.minutes }
                        val monthTotalHrs = if (monthTotalMins > 0) String.format(Locale.US, "%.1f", monthTotalMins / 60f) else "0"
                        val monthAvgEffort = monthUpdates.mapNotNull { it.effort }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
                        val monthAvgEffortStr = if (monthAvgEffort > 0) String.format(Locale.US, "%.1f", monthAvgEffort) else "0"

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🏆 Goals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text("$completedGoalsCount/$totalGoalsCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🎯 Milestones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text("$completedMilestones/$totalMilestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⏳ Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${monthTotalHrs}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🔥 Avg Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text(monthAvgEffortStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text("Active Goals Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        allGoals.forEach { goal ->
                            var expanded by remember { mutableStateOf(false) }
                            val gPercent = (goal.progress.coerceIn(0, 100)) / 100f
                            val gDoneMilestones = goal.milestones.count { it.done }
                            val gTotalMilestones = goal.milestones.size
                            val gRoutinesCount = goal.dailyTasks.size
                            
                            Card(
                                onClick = { expanded = !expanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Text("${goal.progress}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (goal.progress >= 100) Color(0xFF10B981) else Color(0xFF8B5CF6))
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape)) {
                                        Box(modifier = Modifier.fillMaxWidth(gPercent).fillMaxHeight().background(if (goal.progress >= 100) Color(0xFF10B981) else Color(0xFF8B5CF6), CircleShape))
                                    }
                                    
                                    if (expanded) {
                                        Spacer(Modifier.height(4.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("Milestones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$gDoneMilestones/$gTotalMilestones completed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Daily Routines", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$gRoutinesCount active", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))



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

// -------------------------------------------------------------------------
// Helper Composables for Goal Charts
// -------------------------------------------------------------------------

/**
 * Combo Chart: Vertical bars for Minutes, overlaid Line for Effort Score (1-10)
 * Data = List<Triple<DayLabel, TotalMinutes, AvgEffort>>
 */
@Composable
private fun GoalEffortComboChart(
    data: List<Triple<String, Float, Float>>,
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
        
        // Y-Axis for Minutes (Max calculation)
        val rawMaxMins = (data.maxOfOrNull { it.second } ?: 60f).coerceAtLeast(30f)
        val maxMins: Float = when {
            rawMaxMins <= 60f -> 60f
            rawMaxMins <= 120f -> 120f
            rawMaxMins <= 240f -> 240f
            rawMaxMins <= 360f -> 360f
            rawMaxMins <= 480f -> 480f
            rawMaxMins <= 720f -> 720f
            rawMaxMins <= 960f -> 960f
            rawMaxMins <= 1200f -> 1200f
            else -> ((rawMaxMins / 60f).toInt() + 1) * 60f
        }
        val ticks = listOf(maxMins, maxMins * 0.75f, maxMins * 0.5f, maxMins * 0.25f, 0f)

        fun formatTick(mins: Float): String {
            if (mins == 0f) return "0"
            val h = (mins / 60).toInt()
            val m = (mins % 60).toInt()
            return if (m == 0 && h > 0) "${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m"
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
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val chartHeight = size.height - xAxisPaddingDp.toPx()
            val yAxisPadding = 15f
            val rightPadding = 15f
            val chartWidth = width - yAxisPadding - rightPadding

            // Grid Lines
            ticks.forEachIndexed { idx, _ ->
                val yPos = (idx.toFloat() / (ticks.size - 1)) * chartHeight
                drawLine(gridLineColor, Offset(0f, yPos), Offset(width, yPos), 1.dp.toPx())
            }

            fun getXPos(index: Int): Float {
                val count = (data.size - 1).coerceAtLeast(1)
                return yAxisPadding + (index.toFloat() / count) * chartWidth
            }

            // Draw Bars (Minutes)
            val barWidth = 16.dp.toPx()
            data.forEachIndexed { i, triple ->
                val x = getXPos(i)
                val barHeight = (triple.second / maxMins) * chartHeight
                val topY = chartHeight - barHeight
                
                drawRoundRect(
                    color = Color(0xFF10B981).copy(alpha = 0.8f),
                    topLeft = Offset(x - barWidth / 2f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }

            // Draw Line (Effort 0-10)
            if (data.isNotEmpty()) {
                val effortPath = Path()
                data.forEachIndexed { i, triple ->
                    val x = getXPos(i)
                    val y = chartHeight - (triple.third / 10f) * chartHeight
                    if (i == 0) effortPath.moveTo(x, y)
                    else {
                        val prevX = getXPos(i - 1)
                        val prevY = chartHeight - (data[i - 1].third / 10f) * chartHeight
                        val cx = (prevX + x) / 2f
                        effortPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }
                drawPath(effortPath, Color(0xFFF59E0B), style = Stroke(3.dp.toPx()))
                
                // Dots & Labels
                drawIntoCanvas { canvas ->
                    data.forEachIndexed { i, triple ->
                        val x = getXPos(i)
                        val y = chartHeight - (triple.third / 10f) * chartHeight
                        drawCircle(Color(0xFFF59E0B), 4.dp.toPx(), Offset(x, y))
                        drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                        canvas.nativeCanvas.drawText(triple.first, x, size.height - 8f, textPaint)
                    }
                }
            }
        }
    }
}




/**
 * Dual Ring Donut Chart for Tasks and Milestones
 */
@Composable
private fun TaskCompletionDonut(
    taskPercent: Float,      // 0f to 1f
    milestonePercent: Float, // 0f to 1f
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val spacing = 8.dp.toPx()
        
        // Inner Ring (Tasks - Blue)
        val innerSize = size.width - strokeWidth * 2 - spacing * 2
        drawArc(
            color = Color(0xFF3B82F6).copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokeWidth + spacing, strokeWidth + spacing),
            size = Size(innerSize, innerSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF3B82F6),
            startAngle = -90f,
            sweepAngle = 360f * taskPercent,
            useCenter = false,
            topLeft = Offset(strokeWidth + spacing, strokeWidth + spacing),
            size = Size(innerSize, innerSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        
        // Outer Ring (Milestones - Purple)
        val outerSize = size.width
        drawArc(
            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(outerSize, outerSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF8B5CF6),
            startAngle = -90f,
            sweepAngle = 360f * milestonePercent,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(outerSize, outerSize),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}
