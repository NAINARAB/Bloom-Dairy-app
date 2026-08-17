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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.QueryStats
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
        entryRepo.entries(180),
        entryRepo.ratings(180),
        goalRepo.updateLog(180),
        goalRepo.goals(),
        combine(financeRepo.expenses(180), financeRepo.savings(180), insightRepo.screenTime(90), prefsRepo.appPrefs()) {
            e, s, st, p -> listOf(e, s, st, p)
        },
    ) { entries, ratings, goalLog, goalsList, extra ->
        @Suppress("UNCHECKED_CAST")
        InsightContext(
            today = Dates.todayKey(),
            currency = (extra[3] as mobile.dairy.app.domain.AppPrefs).currency,
            dailyBudget = (extra[3] as mobile.dairy.app.domain.AppPrefs).dailyBudget,
            entries = entries,
            ratings = ratings,
            goals = goalsList,
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

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text("Insights", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))

        // 1. Strava-Style Consistency Heatmap
        mobile.dairy.app.ui.insights.components.ConsistencyCalendar(
            ctx = ctx,
            onDayClick = { date ->
                // TODO: Show summary bottom sheet for the day
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        // 2. The App Drawer Style Reports
        SectionTitle("Reports")
        
        // Grid of report icons
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(400.dp),
            userScrollEnabled = false
        ) {
            item {
                AppDrawerItem(
                    icon = Icons.Default.Book,
                    label = "Journal & Mood",
                    onClick = { nav.navigate(Routes.INSIGHTS_JOURNAL) }
                )
            }
            item {
                AppDrawerItem(
                    icon = Icons.Default.CrisisAlert,
                    label = "Goals & Tasks",
                    onClick = { nav.navigate(Routes.INSIGHTS_GOALS) }
                )
            }
            item {
                AppDrawerItem(
                    icon = Icons.Default.QueryStats,
                    label = "Digital Habits",
                    onClick = { nav.navigate(Routes.INSIGHTS_DIGITAL_HABITS) }
                )
            }
            item {
                AppDrawerItem(
                    icon = Icons.Default.AttachMoney,
                    label = "Money & Savings",
                    onClick = { nav.navigate(Routes.INSIGHTS_FINANCE) }
                )
            }
            item {
                AppDrawerItem(
                    icon = Icons.Default.BarChart,
                    label = "Holistic Data",
                    onClick = { nav.navigate(Routes.INSIGHTS_HOLISTIC) }
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AppDrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}
