package mobile.dairy.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Mood
import mobile.dairy.app.data.EntryRepository
import mobile.dairy.app.data.FinanceRepository
import mobile.dairy.app.data.GoalRepository
import mobile.dairy.app.data.InsightRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.domain.InsightEngine
import mobile.dairy.app.ui.components.Bar
import mobile.dairy.app.ui.components.Bars
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.BloomDateRangePicker
import mobile.dairy.app.ui.components.EmptyState
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.StatCard
import mobile.dairy.app.ui.components.WrapChips
import mobile.dairy.app.ui.theme.BloomColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    entryRepo: EntryRepository,
    goalRepo: GoalRepository,
    financeRepo: FinanceRepository,
    insightRepo: InsightRepository,
    prefsRepo: PrefsRepository,
) : ViewModel() {

    val ctx = combine(
        entryRepo.entries(90),
        entryRepo.ratings(90),
        goalRepo.updateLog(90),
        combine(financeRepo.expenses(62), financeRepo.savings(62), insightRepo.screenTime(30), prefsRepo.appPrefs()) {
            e, s, st, p -> listOf(e, s, st, p)
        },
    ) { entries, ratings, goalLog, extra ->
        @Suppress("UNCHECKED_CAST")
        InsightContext(
            today = Dates.todayKey(),
            currency = (extra[3] as mobile.dairy.app.domain.AppPrefs).currency,
            dailyBudget = (extra[3] as mobile.dairy.app.domain.AppPrefs).dailyBudget,
            entries = entries,
            ratings = ratings,
            goalUpdates = goalLog,
            expenses = extra[0] as List<mobile.dairy.app.domain.Expense>,
            savings = extra[1] as List<mobile.dairy.app.domain.Saving>,
            screenTime = extra[2] as List<mobile.dairy.app.domain.ScreenTimeDay>,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightContext(today = Dates.todayKey()))
}

@Composable
fun InsightsScreen(nav: NavController) {
    // Legacy wrapper
    InsightsContent(nav)
}

@Composable
fun InsightsContent(nav: NavController, modifier: Modifier = Modifier, vm: InsightsViewModel = hiltViewModel()) {
    val ctx by vm.ctx.collectAsState()

    var range by remember { mutableStateOf("30d") } // 7d | 30d | custom
    var showDatePicker by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf<String?>(null) }
    var customEnd by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        mobile.dairy.app.ui.components.BloomTwoDatePickerDialog(
            initialStartDate = customStart,
            initialEndDate = customEnd,
            onDismiss = { showDatePicker = false },
            onRangeSelected = { start, end ->
                customStart = start
                customEnd = end
                showDatePicker = false
            }
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        val today = ctx.today
        val entryByDate = ctx.entries.associateBy { it.date }

        Spacer(Modifier.height(28.dp))
        Text("Insights", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))

        // Range Selector
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("7d" to "7 days", "30d" to "30 days", "custom" to "Custom").forEach { (r, label) ->
                val sel = range == r
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { 
                            range = r
                            if (r == "custom") showDatePicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val rangeEnd = if (range == "custom") (customEnd ?: today) else today
        val rangeStart = when (range) {
            "7d" -> Dates.addDays(today, -6)
            "30d" -> Dates.addDays(today, -29)
            else -> customStart ?: Dates.addDays(today, -29)
        }

        if (range == "custom" && customStart != null) {
            Spacer(Modifier.height(8.dp))
            Text("$customStart to $customEnd", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))

        // Personal Motivation Assistant Daily Encouragements
        val dailyInsights = remember(ctx) { InsightEngine.generateDailyInsights(ctx) }
        if (dailyInsights.isNotEmpty()) {
            SectionTitle("Personal Motivation Assistant")
            dailyInsights.forEach { insight ->
                mobile.dairy.app.ui.components.InsightCard(insight = insight)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (ctx.entries.isEmpty()) {
            EmptyState(
                Icons.Default.BarChart, "Waiting for data",
                "Bloom needs a few check-ins to start seeing patterns in your mood, money, and goals."
            )
        } else {
            // Weekly Reflection Summary
            SectionTitle("Weekly Reflection & Growth")
            val summary = remember(ctx, rangeStart, rangeEnd) {
                InsightEngine.generateWeeklySummary(ctx, rangeStart, rangeEnd)
            }
            BloomCard {
                Text(summary.message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Eyebrow("Check-ins")
                        Text("${summary.checkins} logged", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Eyebrow("Goal Focus")
                        Text("${summary.goalMinutes} mins", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Eyebrow("Avg Rating")
                        Text(summary.avgOverall?.let { "%.1f/10".format(it) } ?: "–", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Mood Trend
            SectionTitle("Mood trend")
            BloomCard {
                val daysCount = (Dates.daysBetween(rangeStart, rangeEnd) + 1).toInt()
                val bars = (0 until daysCount).map { i ->
                    val d = Dates.addDays(rangeStart, i.toLong())
                    val e = entryByDate[d]
                    val v = e?.moods?.let { Mood.valenceOf(it) }
                    Bar(
                        label = if (daysCount <= 14) Dates.friendly(d).take(3) else "",
                        value = v?.let { (it + 1) / 2 },
                        color = v?.let { if (it >= 0) BloomColors.success() else BloomColors.warning() }
                    )
                }
                Bars(bars, max = 1.0, height = 120.dp)
                if (range == "custom") {
                    Spacer(Modifier.height(8.dp))
                    Text("$rangeStart to $rangeEnd", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Productivity & Focus
            val filteredRatings = ctx.ratings.filter { it.date in rangeStart..rangeEnd }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("Avg Focus")
                    val avgFocus = filteredRatings.mapNotNull { it.focus }.takeIf { it.isNotEmpty() }?.average()
                    StatCard("", "${avgFocus?.let { "%.1f".format(it) } ?: "–"}/10", Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f)) {
                    SectionTitle("Goal Effort")
                    val avgEffort = filteredRatings.mapNotNull { it.goalEffort }.takeIf { it.isNotEmpty() }?.average()
                    StatCard("", "${avgEffort?.let { "%.1f".format(it) } ?: "–"}/10", Modifier.fillMaxWidth(), good = true)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Influencers
            val filteredEntries = ctx.entries.filter { it.date in rangeStart..rangeEnd }
            val (pos, neg) = InsightEngine.moodInfluencers(filteredEntries)
            if (pos.isNotEmpty() || neg.isNotEmpty()) {
                SectionTitle("What affects your mood")
                BloomCard {
                    if (pos.isNotEmpty()) {
                        Eyebrow("Boosts mood")
                        Spacer(Modifier.height(8.dp))
                        WrapChips {
                            pos.take(5).forEach { inf ->
                                FilterChip(selected = true, onClick = {}, label = { Text(inf.name) })
                            }
                        }
                    }
                    if (neg.isNotEmpty()) {
                        if (pos.isNotEmpty()) Spacer(Modifier.height(16.dp))
                        Eyebrow("Brings mood down")
                        Spacer(Modifier.height(8.dp))
                        WrapChips {
                            neg.take(5).forEach { inf ->
                                FilterChip(selected = false, onClick = {}, label = { Text(inf.name) },
                                    colors = FilterChipDefaults.filterChipColors(labelColor = MaterialTheme.colorScheme.error))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
            Text(
                "Disclaimer: " + Constants.WELLBEING_DISCLAIMER,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
