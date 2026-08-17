package mobile.dairy.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mobile.dairy.app.core.UsageStatsHelper
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.StatCard
import mobile.dairy.app.ui.components.WrapChips
import mobile.dairy.app.ui.theme.BloomColors
import java.util.Calendar
import javax.inject.Inject

data class DashboardState(
    val loading: Boolean = true,
    val today: String = Dates.todayKey(),
    val displayName: String = "there",
    val prefs: AppPrefs = AppPrefs(),
    val todayEntry: JournalEntry? = null,
    val entries: List<JournalEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val money: DayMoney = DayMoney(Dates.todayKey(), 0.0, 0.0, 0.0, 0.0, 0.0),
    val monthlySpent: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val screenToday: ScreenTimeDay? = null,
    val checkinStreak: Int = 0,
    val goalStreak: Int = 0,
    val focus: Int? = null,
    val effort: Int? = null,
    val energy: Int? = null,
    val goalMinutesToday: Int = 0,
    val ratings: List<mobile.dairy.app.domain.DailyRating> = emptyList(),
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
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val quotesMap: Map<String, List<String>> by lazy {
        val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
        val jsonObject = org.json.JSONObject(jsonString)
        val map = mutableMapOf<String, List<String>>()
        jsonObject.keys().forEach { key ->
            val array = jsonObject.getJSONArray(key)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            map[key] = list
        }
        map
    }

    private val today = Dates.todayKey()
    private val generatedInsightIds = mutableSetOf<String>()

    val state: StateFlow<DashboardState> = combine(
        entryRepo.entries(30),
        entryRepo.ratings(30L),
        goalRepo.goals(listOf("active")),
        goalRepo.updateLog(30),
        combine(
            financeRepo.expenses(30), financeRepo.savings(30),
            insightRepo.screenTime(14L), insightRepo.unseen(3L), prefsRepo.appPrefs(),
        ) { e, s, st, ins, prefs -> Bundle(e, s, st, ins, prefs) },
    ) { entries, ratings, goals, goalLog, b ->
        val rating = ratings.firstOrNull { it.date == today }
        val monthPrefix = today.take(7)
        val mSpent = b.expenses.filter { it.date.startsWith(monthPrefix) }.sumOf { it.amount }
        val mBudget = b.prefs.monthlyBudget
        val gMins = goalLog.filter { it.date == today }.sumOf { it.minutes }

        val ctx = InsightContext(
            today = today,
            currency = b.prefs.currency,
            dailyBudget = b.prefs.monthlyBudget / 30.0, // derive daily from monthly budget setting
            entries = entries, ratings = ratings, goals = goals, goalUpdates = goalLog,
            expenses = b.expenses, savings = b.savings, screenTime = b.screenTime,
            quotes = quotesMap
        )
        // On-device motivation engine — keyed by date+type, so re-runs never spam.
        val newInsights = InsightEngine.generateDailyInsights(ctx).filter { it.id !in generatedInsightIds }
        if (newInsights.isNotEmpty()) {
            viewModelScope.launch {
                runCatching { 
                    insightRepo.saveAll(newInsights) 
                    generatedInsightIds.addAll(newInsights.map { it.id })
                }
            }
        }
        DashboardState(
            loading = false,
            today = today,
            displayName = authRepository.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there",
            prefs = b.prefs,
            todayEntry = entries.firstOrNull { it.date == today },
            entries = entries,
            goals = goals,
            insights = b.insights,
            money = Finance.summarizeDay(today, b.expenses, b.savings),
            monthlySpent = mSpent,
            monthlyBudget = mBudget,
            screenToday = b.screenTime.firstOrNull { it.date == today },
            checkinStreak = Streaks.current(entries.map { it.date }, today),
            goalStreak = Streaks.current(goalLog.filter { it.minutes > 0 }.map { it.date }, today),
            focus = rating?.focus,
            effort = rating?.goalEffort,
            energy = rating?.energy,
            goalMinutesToday = gMins,
            ratings = ratings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private data class Bundle(
        val expenses: List<mobile.dairy.app.domain.Expense>,
        val savings: List<mobile.dairy.app.domain.Saving>,
        val screenTime: List<ScreenTimeDay>,
        val insights: List<Insight>,
        val prefs: AppPrefs,
    )

    fun dismissInsight(insight: Insight) {
        viewModelScope.launch { 
            runCatching { 
                insightRepo.markSeen(insight.id)
                // Increment index so they see a new quote next time
                val currentPrefs = prefsRepo.getAppPrefs()
                val newIndices = currentPrefs.quoteIndices.toMutableMap()
                val cat = insight.type
                newIndices[cat] = (newIndices[cat] ?: 0) + 1
                prefsRepo.updateAppPrefs(currentPrefs.copy(quoteIndices = newIndices))
            } 
        }
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
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context)) }
    var liveScreenTimeToday by remember { mutableStateOf<mobile.dairy.app.domain.ScreenTimeDay?>(null) }

    LaunchedEffect(hasUsagePermission) {
        if (hasUsagePermission) {
            val usage = withContext(Dispatchers.IO) {
                UsageStatsHelper.getLast7DaysUsage(context)
            }
            liveScreenTimeToday = usage.firstOrNull { it.date == mobile.dairy.app.core.Dates.todayKey() }
        }
    }

    LaunchedEffect(s.prefs.screenTimeEnabled) {
        vm.syncScreenTimeIfEnabled(s.prefs.screenTimeEnabled)
    }

    val dominantMood = s.todayEntry?.moods?.firstOrNull()?.let { mobile.dairy.app.core.Mood.fromKey(it) }
    val tint = dominantMood?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Signature: header softly tinted by today's dominant mood
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.08f), Color.Transparent))),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                // Header Top Row (Greeting & Settings)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            "${Dates.greetingForHour(hour)}, ${s.displayName}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(Modifier.height(2.dp))
                        Eyebrow(Dates.friendly(s.today).uppercase())
                    }
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

                Spacer(Modifier.height(16.dp))

                // Quick Action Chips Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { QuickActionCard("Review Entry", "See your details", Icons.AutoMirrored.Filled.MenuBook, onClick = { nav.navigate(Routes.CHECK_IN) }, modifier = Modifier.width(180.dp)) }
                    item { QuickActionCard("Add Expense", "Track spending", Icons.Default.Payments, onClick = { onSwitchTab(Routes.MONEY) }, modifier = Modifier.width(180.dp)) }
                    item { QuickActionCard("Add Goal", "Set new goal", Icons.Default.TrackChanges, onClick = { nav.navigate(Routes.NEW_GOAL) }, modifier = Modifier.width(180.dp)) }
                }

                Spacer(Modifier.height(16.dp))

                // Dynamic Check-In Card
                if (s.todayEntry != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981).copy(alpha = 0.18f),
                                        Color(0xFF059669).copy(alpha = 0.08f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Check-in complete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dominantMoodEmoji = s.todayEntry?.moods?.firstOrNull()?.let { Mood.fromKey(it) }
                                Text(dominantMoodEmoji?.emoji ?: "😊", fontSize = 48.sp)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Feeling ${s.todayEntry?.moods?.firstOrNull() ?: "happy"}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Thank you for checking in today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    WrapChips {
                                        s.todayEntry?.moodCauses?.forEach { tag ->
                                            Box(Modifier.background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                Text(tag, style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { nav.navigate(Routes.CHECK_IN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Review Entry  →", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    BloomCard(onClick = { nav.navigate(Routes.CHECK_IN) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✍️", fontSize = 32.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Daily Reflection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Takes about 2 minutes to reflect on today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { nav.navigate(Routes.CHECK_IN) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start Today's Check-in", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            // Insights Section
            if (s.insights.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("✨ Insights")
                    // Text(
                    //     "See all", 
                    //     style = MaterialTheme.typography.labelMedium, 
                    //     color = MaterialTheme.colorScheme.primary,
                    //     modifier = Modifier.clickable { nav.navigate(Routes.INSIGHTS) }
                    // )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.insights) { insight ->
                        InsightCard(insight, onDismiss = { vm.dismissInsight(insight) }, modifier = Modifier.width(280.dp))
                    }
                }
            }

            // Streak Badges Row
            if (s.checkinStreak >= 1 || s.goalStreak >= 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    if (s.checkinStreak >= 1) StreakBadge(s.checkinStreak, "reflection streak")
                    if (s.goalStreak >= 1) StreakBadge(s.goalStreak, "goal streak")
                }
            }

            // Main Goal Section
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Goals")
                // Text(
                //     "View all", 
                //     style = MaterialTheme.typography.labelMedium, 
                //     color = MaterialTheme.colorScheme.primary,
                //     modifier = Modifier.clickable { nav.navigate(Routes.GOALS) }
                // )
            }
            val mainGoal = s.goals.firstOrNull()
            if (mainGoal != null) {
                BloomCard(onClick = { nav.navigate(Routes.goal(mainGoal.id)) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f), CircleShape).padding(8.dp)) {
                                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(mainGoal.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        if (mainGoal.deadline != null) {
                            Box(
                                Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Due ${mainGoal.deadline}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${mainGoal.progress}% \u00B7 ${if (mainGoal.type == "short") "Short-term" else "Long-term"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${mainGoal.progress}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    GoalProgressBar(mainGoal.progress)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("${s.goalMinutesToday} mins logged today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { nav.navigate(Routes.goal(mainGoal.id)) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f))
                        ) {
                            Text("Log Time", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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

            // Today at a Glance (4 Circular Stat Pills Grid)
            SectionTitle("Today at a glance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CircularStatPill(
                    label = "Focus",
                    value = "${s.focus ?: "–"}/10",
                    icon = Icons.Default.FilterCenterFocus,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                CircularStatPill(
                    label = "Effort",
                    value = "${s.effort ?: "–"}/10",
                    icon = Icons.Default.Speed,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                CircularStatPill(
                    label = "Energy",
                    value = "${s.energy ?: "–"}/10",
                    icon = Icons.Default.Bolt,
                    color = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
                val screenValue = (liveScreenTimeToday ?: s.screenToday)?.let {
                    val totalMins = it.totalMinutes.toInt()
                    if (totalMins < 60) {
                        "$totalMins mins"
                    } else {
                        val hours = totalMins / 60
                        val mins = totalMins % 60
                        String.format("%02d:%02d", hours, mins)
                    }
                } ?: "–"
                CircularStatPill(
                    label = "Screen",
                    value = screenValue,
                    icon = Icons.Default.Smartphone,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { nav.navigate(Routes.INSIGHTS_DIGITAL_HABITS) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Money Section
            SectionTitle("Money")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Spent today",
                    Format.money(s.money.spent, s.prefs.currency),
                    Modifier.weight(1f),
                    good = false,
                    emphasize = true
                )
                StatCard(
                    "Earned + saved",
                    Format.money(s.money.earned + s.money.avoided, s.prefs.currency),
                    Modifier.weight(1f),
                    good = true
                )
            }
            Spacer(Modifier.height(10.dp))
            BloomCard {
                Text("MONTHLY BUDGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Spent ${Format.money(s.monthlySpent, s.prefs.currency)} of ${Format.money(s.monthlyBudget, s.prefs.currency)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${((s.monthlySpent / s.monthlyBudget.coerceAtLeast(1.0)) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoalProgressBar(((s.monthlySpent / s.monthlyBudget.coerceAtLeast(1.0)) * 100).toInt().coerceIn(0, 100))
            }

            // Weekly Mood Trend Section
            SectionTitle("Weekly Mood Trend")
            BloomCard {
                WeekMoodLineChart(s.ratings, s.today)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    mobile.dairy.app.ui.components.GlobalLoadingOverlay(s.loading, "Loading your dashboard...")
}

@Composable
fun CircularStatPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onClick != null) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 2.dp).size(14.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
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
        Text(
            "$days \u00B7 $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WeekMoodLineChart(ratings: List<mobile.dairy.app.domain.DailyRating>, todayKey: String) {
    val weekKeys = (6 downTo 0).map { Dates.addDays(todayKey, -it.toLong()) }
    val data = weekKeys.map { date ->
        val r = ratings.firstOrNull { it.date == date }
        r?.overall?.toDouble() ?: 0.0
    }
    
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val maxValence = 10.0
            val minValence = 0.0
            val range = 10.0
            val w = size.width
            val h = size.height
            val stepX = if (data.size > 1) w / (data.size - 1) else w
            
            val path = androidx.compose.ui.graphics.Path()
            val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
            
            data.forEachIndexed { i, valence ->
                val normalized = (valence - minValence) / range
                val y = h - (normalized * h).toFloat()
                val x = i * stepX
                points.add(androidx.compose.ui.geometry.Offset(x, y))
                if (i == 0) path.moveTo(x, y)
                else {
                    val prev = points[i - 1]
                    val controlX = (prev.x + x) / 2
                    path.cubicTo(controlX, prev.y, controlX, y, x, y)
                }
            }
            
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.4f), Color.Transparent)
                )
            )
            
            // Draw horizontal grid lines for 2, 4, 6, 8, 10
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 28f
                isAntiAlias = true
            }
            listOf(2.0, 4.0, 6.0, 8.0, 10.0).forEach { value ->
                val normalized = (value - minValence) / range
                val y = h - (normalized * h).toFloat()
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(value.toInt().toString(), 0f, y - 8f, textPaint)
            }

            drawPath(
                path = path,
                color = Color(0xFF8B5CF6),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            points.forEach { pt ->
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pt)
                drawCircle(color = Color(0xFF8B5CF6), radius = 4.dp.toPx(), center = pt)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekKeys.forEachIndexed { i, date ->
                val label = if (i == 6) "Today" else date.takeLast(2)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BloomCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f), CircleShape).padding(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
