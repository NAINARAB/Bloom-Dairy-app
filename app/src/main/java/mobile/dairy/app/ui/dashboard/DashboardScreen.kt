package mobile.dairy.app.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Format
import mobile.dairy.app.core.Mood
import mobile.dairy.app.data.AuthRepository
import mobile.dairy.app.data.EntryRepository
import mobile.dairy.app.data.FinanceRepository
import mobile.dairy.app.data.GoalRepository
import mobile.dairy.app.data.InsightRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.DayMoney
import mobile.dairy.app.domain.Finance
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.domain.InsightEngine
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.ScreenTimeDay
import mobile.dairy.app.domain.Streaks
import mobile.dairy.app.services.ScreenTimeService
import mobile.dairy.app.ui.components.Bar
import mobile.dairy.app.ui.components.Bars
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.GoalProgressBar
import mobile.dairy.app.ui.components.InsightCard
import mobile.dairy.app.ui.components.MoodEmojiRow
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.StatCard
import mobile.dairy.app.ui.journal.JournalContent
import mobile.dairy.app.ui.goals.GoalsContent
import mobile.dairy.app.ui.money.MoneyContent
import mobile.dairy.app.ui.insights.InsightsContent
import mobile.dairy.app.ui.theme.BloomColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardState(
    val today: String = Dates.todayKey(),
    val displayName: String = "there",
    val prefs: AppPrefs = AppPrefs(),
    val todayEntry: JournalEntry? = null,
    val entries: List<JournalEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val money: DayMoney = DayMoney(Dates.todayKey(), 0.0, 0.0, 0.0, 0.0, 0.0),
    val screenToday: ScreenTimeDay? = null,
    val checkinStreak: Int = 0,
    val goalStreak: Int = 0,
    val focus: Int? = null,
    val effort: Int? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val entryRepo: EntryRepository,
    private val goalRepo: GoalRepository,
    private val financeRepo: FinanceRepository,
    private val insightRepo: InsightRepository,
    private val prefsRepo: PrefsRepository,
    private val screenTimeService: ScreenTimeService,
) : ViewModel() {

    private val today = Dates.todayKey()

    val state: StateFlow<DashboardState> = combine(
        entryRepo.entries(30),
        entryRepo.ratings(30),
        goalRepo.goals(listOf("active")),
        goalRepo.updateLog(30),
        combine(
            financeRepo.expenses(30), financeRepo.savings(30),
            insightRepo.screenTime(14), insightRepo.unseen(3), prefsRepo.appPrefs(),
        ) { e, s, st, ins, prefs -> Bundle(e, s, st, ins, prefs) },
    ) { entries, ratings, goals, goalLog, b ->
        val rating = ratings.firstOrNull { it.date == today }
        val ctx = InsightContext(
            today = today,
            currency = b.prefs.currency,
            dailyBudget = b.prefs.dailyBudget,
            entries = entries, ratings = ratings, goals = goals, goalUpdates = goalLog,
            expenses = b.expenses, savings = b.savings, screenTime = b.screenTime,
        )
        // On-device motivation engine — keyed by date+type, so re-runs never spam.
        viewModelScope.launch {
            runCatching { insightRepo.saveAll(InsightEngine.generateDailyInsights(ctx)) }
        }
        DashboardState(
            today = today,
            displayName = authRepository.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there",
            prefs = b.prefs,
            todayEntry = entries.firstOrNull { it.date == today },
            entries = entries,
            goals = goals,
            insights = b.insights,
            money = Finance.summarizeDay(today, b.expenses, b.savings),
            screenToday = b.screenTime.firstOrNull { it.date == today },
            checkinStreak = Streaks.current(entries.map { it.date }, today),
            goalStreak = Streaks.current(goalLog.filter { it.minutes > 0 }.map { it.date }, today),
            focus = rating?.focus,
            effort = rating?.goalEffort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private data class Bundle(
        val expenses: List<mobile.dairy.app.domain.Expense>,
        val savings: List<mobile.dairy.app.domain.Saving>,
        val screenTime: List<ScreenTimeDay>,
        val insights: List<Insight>,
        val prefs: AppPrefs,
    )

    fun dismissInsight(id: String) {
        viewModelScope.launch { runCatching { insightRepo.markSeen(id) } }
    }

    fun syncScreenTimeIfEnabled(enabled: Boolean) {
        if (!enabled) return
        viewModelScope.launch { runCatching { screenTimeService.syncToday() } }
    }
}

/* ------------------------------------------------------------------ */
/* Navigation shared data                                               */
/* ------------------------------------------------------------------ */

data class Tab(val route: String, val label: String, val icon: ImageVector)

val TABS = listOf(
    Tab(Routes.MONEY, "Money", Icons.Default.Payments),
    Tab(Routes.GOALS, "Goals", Icons.Default.TrackChanges),
    Tab(Routes.HOME, "Home", Icons.Default.Home),
    Tab(Routes.JOURNAL, "Journal", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.INSIGHTS, "Insights", Icons.Default.AutoAwesome),
)

/* ------------------------------------------------------------------ */
/* Screen                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun DashboardScreen(nav: NavController) {
    // Legacy wrapper, MainActivity now uses DashboardContent directly
    DashboardContent(nav)
}

@Composable
fun DashboardContent(
    nav: NavController,
    modifier: Modifier = Modifier,
    vm: DashboardViewModel = hiltViewModel(),
    onSwitchTab: (String) -> Unit = {}
) {
    val s by vm.state.collectAsState()

    LaunchedEffect(s.prefs.screenTimeEnabled) {
        vm.syncScreenTimeIfEnabled(s.prefs.screenTimeEnabled)
    }

    val dominantMood = s.todayEntry?.moods?.firstOrNull()?.let { Mood.fromKey(it) }
    val tint = dominantMood?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Signature: header softly tinted by today's dominant mood
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.08f), Color.Transparent))),
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) { // Better spacing
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(Dates.friendly(s.today))
                        IconButton(
                            onClick = { nav.navigate(Routes.SETTINGS) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${Dates.greetingForHour(hour)}, ${s.displayName}",
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (s.todayEntry != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoodEmojiRow(s.todayEntry!!.moods, size = 24)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Check-in complete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            "Ready for your daily reflection?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { nav.navigate(Routes.CHECK_IN) },
                            modifier = Modifier.height(54.dp).weight(1.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (s.todayEntry != null) "Review today" else "Start check-in", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onSwitchTab(Routes.MONEY) },
                            modifier = Modifier.height(54.dp).weight(0.8f),
                            shape = RoundedCornerShape(16.dp)
                        ) { 
                            Text("+ Expense", fontWeight = FontWeight.Bold) 
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 24.dp)) { // Consistent padding
                s.insights.forEach { insight ->
                    InsightCard(insight, onDismiss = { vm.dismissInsight(insight.id) })
                }

                if (s.checkinStreak >= 2 || s.goalStreak >= 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (s.checkinStreak >= 2) StreakBadge(s.checkinStreak, "reflection streak")
                        if (s.goalStreak >= 2) StreakBadge(s.goalStreak, "goal streak")
                    }
                }

                SectionTitle("Main goal")
                val mainGoal = s.goals.firstOrNull()
                if (mainGoal != null) {
                    BloomCard(onClick = { nav.navigate(Routes.goal(mainGoal.id)) }) {
                        Text(mainGoal.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        GoalProgressBar(mainGoal.progress)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${mainGoal.progress}% \u00B7 ${if (mainGoal.type == "short") "Short-term" else "Long-term"}" +
                                (mainGoal.deadline?.let { " \u00B7 due $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    BloomCard(onClick = { nav.navigate(Routes.NEW_GOAL) }) {
                        Text(
                            "No active goals yet. Set one small, meaningful goal to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { nav.navigate(Routes.NEW_GOAL) }) { Text("Create a goal") }
                    }
                }

                SectionTitle("Today at a glance")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Focus", "${s.focus ?: "–"}/10", Modifier.weight(1f), icon = Icons.Default.FilterCenterFocus)
                    StatCard("Effort", "${s.effort ?: "–"}/10", Modifier.weight(1f), icon = Icons.Default.Speed)
                    StatCard("Screen", s.screenToday?.let { Format.minutes(it.totalMinutes) } ?: "–", Modifier.weight(1f), icon = Icons.Default.Smartphone)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Spent today", Format.money(s.money.spent, s.prefs.currency), Modifier.weight(1f))
                    StatCard("Saved + avoided", Format.money(s.money.saved + s.money.avoided, s.prefs.currency),
                        Modifier.weight(1f), good = true)
                }

                SectionTitle("Mood this week")
                BloomCard {
                    WeekMoodBars(s.entries, s.today)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
}

@Composable
private fun StreakBadge(days: Int, label: String) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text("$days \u00B7 $label", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}

/** Last 7 days of mood valence — a quick emotional weather report. */
@Composable
fun WeekMoodBars(entries: List<JournalEntry>, today: String) {
    val byDate = entries.associateBy { it.date }
    val bars = (0..6).map { i ->
        val key = Dates.addDays(today, (i - 6).toLong())
        val e = byDate[key]
        val v = if (e != null && e.moods.isNotEmpty()) Mood.valenceOf(e.moods) else null
        Bar(
            label = Dates.friendly(key).take(3),
            value = v?.let { (it + 1) / 2 }, // map -1..1 -> 0..1
            color = v?.let { if (it >= 0) BloomColors.success() else BloomColors.warning() },
        )
    }
    Bars(bars, max = 1.0, height = 64.dp)
}

