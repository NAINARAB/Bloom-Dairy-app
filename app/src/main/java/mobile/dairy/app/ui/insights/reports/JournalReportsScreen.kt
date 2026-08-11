package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalReportsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val allEntries = ctx.entries
    val allRatings = ctx.ratings

    // Default to current month
    val todayDate = remember { LocalDate.now() }
    var selectedMonthKey by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var selectedMonthLabel by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }

    // Filter data for the selected month
    val monthEntries = remember(allEntries, selectedMonthKey) {
        allEntries.filter { it.date.startsWith(selectedMonthKey) }
    }
    val monthRatings = remember(allRatings, selectedMonthKey) {
        allRatings.filter { it.date.startsWith(selectedMonthKey) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal & Mood Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Purple Info Button (Toggles Understanding the Charts Card)
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Month Filter Picker Button
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

            Spacer(Modifier.height(12.dp))

            // "Understanding the Charts" Card (Collapsible)
            if (showInfoCard) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Understanding the Charts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• X-Axis (Horizontal): Represents Dates / Time (left = older, right = recent).\n" +
                                        "• Y-Axis (Vertical): Represents Rating Level (1 = Low/Poor, 10 = High/Excellent).",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 1. Mood Distribution & Triggers
            SectionTitle("1. Mood Distribution & Triggers")
            BloomCard {
                Text(
                    "Mood Frequency ($selectedMonthLabel)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                val allMoods = monthEntries.flatMap { it.moods }
                val moodCounts = allMoods.groupingBy { it }.eachCount()

                if (allMoods.isEmpty()) {
                    Text(
                        "No mood entries recorded for $selectedMonthLabel. Log daily check-ins to see your mood breakdown!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    // Multi-color Donut Ring Chart with Right Side Legend
                    MoodDonutChartWithLegend(moodCounts = moodCounts, totalMoods = allMoods.size)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))

                // Top Mood Triggers (Multi-Color Chips like Image)
                Text(
                    "Top Mood Triggers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                val triggers = monthEntries.flatMap { it.moodCauses }.groupingBy { it }.eachCount()
                if (triggers.isEmpty()) {
                    Text(
                        "Add mood causes when journaling to discover what influences your daily feelings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val triggerChipStyles = listOf(
                        Triple(Color(0xFF8B5CF6), Icons.Default.Work, "Work"),
                        Triple(Color(0xFF06B6D4), Icons.Default.Group, "Friends"),
                        Triple(Color(0xFF3B82F6), Icons.Default.WaterDrop, "Sad"),
                        Triple(Color(0xFFEC4899), Icons.Default.Favorite, "Family"),
                        Triple(Color(0xFF10B981), Icons.Default.Star, "Other")
                    )

                    val topTriggers = triggers.entries.sortedByDescending { it.value }.take(5)
                    
                    // Display multi-colored trigger pills using FlowRow for perfect wrapping
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            topTriggers.forEachIndexed { index, (cause, count) ->
                                val (badgeColor, badgeIcon, _) = triggerChipStyles[index % triggerChipStyles.size]

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badgeColor.copy(alpha = 0.25f))
                                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            badgeIcon,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "$cause ($count)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "Summary of logged mood triggers (${topTriggers.take(2).joinToString { it.key }}).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Ratings Trend Over Time
            SectionTitle("2. Ratings Trend Over Time")
            BloomCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Overall Rating vs Energy Score (Interactive)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Y-Axis: Rating (1-10) | X-Axis: Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    // Multi-Color Legend Header
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(12.dp, 3.dp).background(Color(0xFF8B5CF6)))
                            Text("Overall Rating", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(12.dp, 3.dp).background(Color(0xFF06B6D4)))
                            Text("Energy Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val sortedRatings = remember(monthRatings) { monthRatings.sortedBy { it.date } }
                    val chartData = if (sortedRatings.isNotEmpty()) {
                        sortedRatings.takeLast(10).map { rating ->
                            val dayStr = rating.date.substringAfterLast("-").toInt().toString()
                            val o = (rating.overall ?: 5).toFloat()
                            val e = (rating.energy ?: 5).toFloat()
                            Triple(dayStr, o, e)
                        }
                    } else {
                        listOf(
                            Triple("1", 6f, 4f),
                            Triple("5", 6.5f, 5.2f),
                            Triple("10", 5f, 4f),
                            Triple("15", 8f, 6.8f),
                            Triple("20", 9f, 5f),
                            Triple("25", 7f, 6.5f),
                            Triple("30", 8.5f, 8f)
                        )
                    }

                    CustomRatingsLineChart(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Social Impact & People
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("3. Social Impact & People")
            }

            BloomCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Top Interacted People",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Crown",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                val peopleList = monthEntries.flatMap { it.people }
                val personCounts = peopleList.groupBy { it.name }

                if (personCounts.isEmpty()) {
                    Text(
                        "Tag family, friends, or colleagues in your check-ins for $selectedMonthLabel to reveal your social impact breakdown!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    personCounts.entries.sortedByDescending { it.value.size }.take(5).forEachIndexed { idx, (name, refs) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            1.5.dp,
                                            if (idx == 0) Color(0xFFF59E0B) else Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Check-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "${refs.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "#${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
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
// Helper Composables for Visual Perfection (Matching User's Image)
// -------------------------------------------------------------------------

@Composable
fun MonthYearPickerDialog(
    initialMonthKey: String,
    onDateSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initialMonthKey.substring(0, 4).toInt()) }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year-- }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("$year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { year++ }) { Icon(Icons.Default.ChevronRight, null) }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until 3) {
                                val i = row * 3 + col
                                val monthNum = String.format(Locale.US, "%02d", i + 1)
                                val key = "$year-$monthNum"
                                val isSelected = key == initialMonthKey
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { 
                                            onDateSelected(key, "${months[i]} $year") 
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        months[i],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}


@Composable
private fun MoodDonutChartWithLegend(
    moodCounts: Map<String, Int>,
    totalMoods: Int
) {
    val moodPalette = mapOf(
        "happy" to Color(0xFF8B5CF6),    
        "neutral" to Color(0xFF06B6D4),  
        "sad" to Color(0xFF3B82F6),      
        "angry" to Color(0xFFEF4444),    
        "excited" to Color(0xFFEC4899),  
        "calm" to Color(0xFF10B981),     
        "tired" to Color(0xFFF59E0B)     
    )
    val fallbackColors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF10B981))
    val sortedMoods = moodCounts.entries.sortedByDescending { it.value }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                sortedMoods.forEachIndexed { idx, (mood, count) ->
                    val sweepAngle = (count.toFloat() / totalMoods.coerceAtLeast(1)) * 360f
                    val color = moodPalette[mood.lowercase(Locale.US)] ?: fallbackColors[idx % fallbackColors.size]
                    
                    // Don't draw a gap if there's only 1 item filling 100%
                    val gap = if (sortedMoods.size > 1) 4f else 0f

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = (sweepAngle - gap).coerceAtLeast(1f),
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx())
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sortedMoods.take(4).forEachIndexed { idx, (mood, count) ->
                val color = moodPalette[mood.lowercase(Locale.US)] ?: fallbackColors[idx % fallbackColors.size]
                val percent = if (totalMoods > 0) (count * 100 / totalMoods) else 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Text(
                        text = mood.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "| $count days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun CustomRatingsLineChart(
    data: List<Triple<String, Float, Float>>, // Triple(DayLabel, Overall, Energy)
    modifier: Modifier = Modifier
) {
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val axisLabelColorArgb = axisLabelColor.toArgb()
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = axisLabelColorArgb
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    // Using a Row to properly layout Y-axis labels and the Canvas side by side
    // This perfectly aligns the labels with the grid lines.
    Row(modifier = modifier) {
        
        val xAxisPaddingDp = 24.dp
        
        // Y-Axis Integer Labels Column (0, 2, 4, 6, 8, 10)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .padding(bottom = xAxisPaddingDp), // Matches xAxisPadding in Canvas to align the bottom '0'
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf(10, 8, 6, 4, 2, 0).forEach { tick ->
                Text(
                    text = "$tick",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = axisLabelColor
                )
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val yAxisPadding = 10f // Small padding so dots don't clip left edge
            val rightPadding = 10f // Small padding so dots don't clip right edge
            val xAxisPadding = xAxisPaddingDp.toPx()

            val chartWidth = width - yAxisPadding - rightPadding
            val chartHeight = height - xAxisPadding

            // 1. Horizontal Grid Lines (aligned with our external labels)
            val yTicks = listOf(0, 2, 4, 6, 8, 10)
            yTicks.forEach { tick ->
                val yPos = chartHeight - (tick.toFloat() / 10f) * chartHeight
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, yPos),
                    end = Offset(width, yPos),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Helper to convert data index & value to Canvas coordinates
            fun getPointOffset(index: Int, totalPoints: Int, value: Float): Offset {
                val count = (totalPoints - 1).coerceAtLeast(1)
                val x = yAxisPadding + (index.toFloat() / count) * chartWidth
                val y = chartHeight - (value.coerceIn(0f, 10f) / 10f) * chartHeight
                return Offset(x, y)
            }

            // 2. Draw Overall Rating Path (Glowing Solid Purple Line)
            if (data.isNotEmpty()) {
                val purplePath = Path()
                val purpleGradientPath = Path()

                val firstPoint = getPointOffset(0, data.size, data[0].second)
                purplePath.moveTo(firstPoint.x, firstPoint.y)
                purpleGradientPath.moveTo(firstPoint.x, chartHeight)
                purpleGradientPath.lineTo(firstPoint.x, firstPoint.y)

                for (i in 1 until data.size) {
                    val p1 = getPointOffset(i - 1, data.size, data[i - 1].second)
                    val p2 = getPointOffset(i, data.size, data[i].second)
                    val cx = (p1.x + p2.x) / 2f
                    
                    purplePath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    purpleGradientPath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }

                val lastPoint = getPointOffset(data.size - 1, data.size, data.last().second)
                purpleGradientPath.lineTo(lastPoint.x, chartHeight)
                purpleGradientPath.close()

                // Gradient Fill under Purple Line
                drawPath(
                    path = purpleGradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Stroke Purple Line
                drawPath(
                    path = purplePath,
                    color = Color(0xFF8B5CF6),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 3. Draw Energy Score Path (Dashed Cyan Line)
            if (data.isNotEmpty()) {
                val cyanPath = Path()
                val firstPoint = getPointOffset(0, data.size, data[0].third)
                cyanPath.moveTo(firstPoint.x, firstPoint.y)

                for (i in 1 until data.size) {
                    val p1 = getPointOffset(i - 1, data.size, data[i - 1].third)
                    val p2 = getPointOffset(i, data.size, data[i].third)
                    val cx = (p1.x + p2.x) / 2f
                    cyanPath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }

                drawPath(
                    path = cyanPath,
                    color = Color(0xFF06B6D4),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // 4. Draw Data Point Dots & X-Axis Labels
            drawIntoCanvas { canvas ->
                data.forEachIndexed { i, triple ->
                    val overallP = getPointOffset(i, data.size, triple.second)
                    
                    // Draw point dot
                    drawCircle(Color(0xFF8B5CF6), radius = 4.dp.toPx(), center = overallP)
                    drawCircle(Color.White, radius = 2.dp.toPx(), center = overallP)
                    
                    // Draw X-Axis day label perfectly aligned under the point
                    canvas.nativeCanvas.drawText(
                        triple.first,
                        overallP.x,
                        height - 8f, // Draw text near the bottom edge
                        textPaint
                    )
                }
            }
        }
    }
}
