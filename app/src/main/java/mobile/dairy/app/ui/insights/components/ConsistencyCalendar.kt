package mobile.dairy.app.ui.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mobile.dairy.app.core.Dates
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import java.time.LocalDate

@Composable
fun ConsistencyCalendar(
    ctx: InsightContext,
    weeksToDisplay: Int = 20, // ~140 days (4.5 months)
    onDayClick: ((String) -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val todayKey = ctx.today
    val todayDate = try { Dates.parse(todayKey) } catch (e: Exception) { LocalDate.now() }

    // Start on Monday of the earliest week
    val currentDowValue = todayDate.dayOfWeek.value // 1 = Mon, 7 = Sun
    val endOfWeek = todayDate.plusDays((7 - currentDowValue).toLong())
    val startOfWeek = endOfWeek.minusDays(6)
    val earliestDate = startOfWeek.minusWeeks((weeksToDisplay - 1).toLong())

    // Activity lookup maps
    val entryByDate = ctx.entries.associateBy { it.date }
    val goalUpdatesByDate = ctx.goalUpdates.groupBy { it.date }
    val expensesByDate = ctx.expenses.groupBy { it.date }
    val savingsByDate = ctx.savings.groupBy { it.date }
    val ratingByDate = ctx.ratings.associateBy { it.date }

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Consistency Heatmap")
            Text(
                "Scroll for history →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BloomCard {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Day of Week Labels Column (Mon, Wed, Fri)
                Column(
                    modifier = Modifier
                        .padding(top = 22.dp, end = 6.dp)
                        .width(28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val daysOfWeek = listOf("Mon", "", "Wed", "", "Fri", "", "")
                    daysOfWeek.forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Horizontal Scrollable Weeks Matrix
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    Column {
                        // Month Headers Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(18.dp)
                        ) {
                            var prevMonth = ""
                            for (w in 0 until weeksToDisplay) {
                                val weekStartDate = earliestDate.plusWeeks(w.toLong())
                                val monthName = weekStartDate.month.name.take(3).lowercase()
                                    .replaceFirstChar { it.uppercase() }

                                val showMonth = monthName != prevMonth
                                if (showMonth) prevMonth = monthName

                                Box(
                                    modifier = Modifier.width(22.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (showMonth) {
                                        Text(
                                            text = monthName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 7-Row x N-Week Grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (dayIndex in 0 until 7) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (weekIndex in 0 until weeksToDisplay) {
                                        val dateObj = earliestDate.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
                                        val dateKey = Dates.key(dateObj)
                                        val isFuture = dateObj.isAfter(todayDate)

                                        val entry = entryByDate[dateKey]
                                        val goals = goalUpdatesByDate[dateKey].orEmpty()
                                        val expenses = expensesByDate[dateKey].orEmpty()
                                        val savings = savingsByDate[dateKey].orEmpty()

                                        val hasJournal = entry != null
                                        val hasGoals = goals.isNotEmpty()
                                        val hasMoney = expenses.isNotEmpty() || savings.isNotEmpty()

                                        val activityCount = (if (hasJournal) 1 else 0) +
                                                (if (hasGoals) 1 else 0) +
                                                (if (hasMoney) 1 else 0)

                                        HeatmapCell(
                                            dateKey = dateKey,
                                            isToday = dateKey == todayKey,
                                            isFuture = isFuture,
                                            hasJournal = hasJournal,
                                            hasGoals = hasGoals,
                                            hasMoney = hasMoney,
                                            activityCount = activityCount,
                                            onClick = {
                                                if (!isFuture) {
                                                    selectedDate = dateKey
                                                    onDayClick?.invoke(dateKey)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Click the square",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendBadge(Color(0xFF3B82F6), Icons.Default.Book, "Journal")
                    LegendBadge(Color(0xFF10B981), Icons.Default.CrisisAlert, "Goals")
                    LegendBadge(Color(0xFFA855F7), Icons.Default.AttachMoney, "Money")
                }
            }
        }
    }

    // Day Details Dialog
    selectedDate?.let { date ->
        val entry = entryByDate[date]
        val goals = goalUpdatesByDate[date].orEmpty()
        val expenses = expensesByDate[date].orEmpty()
        val savings = savingsByDate[date].orEmpty()
        val rating = ratingByDate[date]

        DaySummaryDialog(
            dateKey = date,
            entry = entry,
            goalsCount = goals.size,
            goalMinutes = goals.sumOf { it.minutes },
            expenseTotal = expenses.sumOf { it.amount },
            savingTotal = savings.sumOf { it.amount },
            rating = rating?.overall,
            currency = ctx.currency,
            onDismiss = { selectedDate = null }
        )
    }
}

@Composable
private fun HeatmapCell(
    dateKey: String,
    isToday: Boolean,
    isFuture: Boolean,
    hasJournal: Boolean,
    hasGoals: Boolean,
    hasMoney: Boolean,
    activityCount: Int,
    onClick: () -> Unit
) {
    val baseBg = when {
        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        activityCount == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        activityCount >= 2 -> MaterialTheme.colorScheme.primary
        hasJournal -> Color(0xFF3B82F6) // Blue
        hasGoals -> Color(0xFF10B981)   // Emerald Green
        hasMoney -> Color(0xFFA855F7)   // Purple/Gold
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val boxModifier = Modifier
        .size(22.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(baseBg)
        .then(
            if (isToday) Modifier.background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                RoundedCornerShape(4.dp)
            ) else Modifier
        )
        .clickable(enabled = !isFuture, onClick = onClick)

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!isFuture && activityCount > 0) {
            when {
                activityCount >= 2 -> {
                    // Multi-activity indicator badge
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                hasJournal -> {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Journal",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                hasGoals -> {
                    Icon(
                        Icons.Default.CrisisAlert,
                        contentDescription = "Goals",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                hasMoney -> {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = "Money",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendBadge(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DaySummaryDialog(
    dateKey: String,
    entry: mobile.dairy.app.domain.JournalEntry?,
    goalsCount: Int,
    goalMinutes: Int,
    expenseTotal: Double,
    savingTotal: Double,
    rating: Int?,
    currency: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Dates.friendly(dateKey),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateKey,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (entry == null && goalsCount == 0 && expenseTotal == 0.0 && savingTotal == 0.0 && rating == null) {
                    Text(
                        "No activity logged for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    rating?.let { r ->
                        ActivityRow(
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFEAB308),
                            title = "Day Rating",
                            subtitle = "$r / 10 score"
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    if (entry != null) {
                        ActivityRow(
                            icon = Icons.Default.Book,
                            iconTint = Color(0xFF3B82F6),
                            title = "Journal Entry",
                            subtitle = if (entry.moods.isNotEmpty()) "Moods: ${entry.moods.joinToString()}" else "Entry logged"
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    if (goalsCount > 0) {
                        ActivityRow(
                            icon = Icons.Default.CrisisAlert,
                            iconTint = Color(0xFF10B981),
                            title = "Goal Progress",
                            subtitle = "$goalsCount goal updates ($goalMinutes mins spent)"
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    if (expenseTotal > 0.0 || savingTotal > 0.0) {
                        ActivityRow(
                            icon = Icons.Default.AttachMoney,
                            iconTint = Color(0xFFA855F7),
                            title = "Financial Activity",
                            subtitle = "Expenses: $currency ${expenseTotal.toInt()} | Savings: $currency ${savingTotal.toInt()}"
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
