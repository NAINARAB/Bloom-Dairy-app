package mobile.dairy.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mobile.dairy.app.ui.dashboard.DashboardContent
import mobile.dairy.app.ui.dashboard.TABS
import mobile.dairy.app.ui.auth.AuthScreen
import mobile.dairy.app.ui.checkin.CheckInScreen
import mobile.dairy.app.ui.dashboard.DashboardScreen
import mobile.dairy.app.ui.goals.GoalDetailScreen
import mobile.dairy.app.ui.goals.GoalsScreen
import mobile.dairy.app.ui.goals.NewGoalScreen
import mobile.dairy.app.ui.insights.InsightsScreen
import mobile.dairy.app.ui.journal.EntryEditorScreen
import mobile.dairy.app.ui.journal.JournalScreen
import mobile.dairy.app.ui.lock.LockScreen
import mobile.dairy.app.ui.money.MoneyScreen
import mobile.dairy.app.ui.onboarding.OnboardingScreen
import mobile.dairy.app.ui.screentime.ScreenTimeScreen
import mobile.dairy.app.ui.settings.SettingsScreen
import mobile.dairy.app.ui.theme.BloomTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // FragmentActivity: required by BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BloomRoot() }
    }
}

/* ------------------------------------------------------------------ */
/* Navigation                                                           */
/* ------------------------------------------------------------------ */

object Routes {
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val JOURNAL = "journal"
    const val GOALS = "goals"
    const val MONEY = "money"
    const val INSIGHTS = "insights"
    const val INSIGHTS_JOURNAL = "insights/journal"
    const val SCREEN_TIME = "screen-time"
    const val CHECK_IN = "check-in"
    const val NEW_GOAL = "goal/new"
    const val GOAL_DETAIL = "goal/{id}"
    const val ENTRY = "entry/{date}"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_CATEGORIES = "settings/categories"
    const val SETTINGS_JOURNAL = "settings/journal"
    const val SETTINGS_PRIVACY = "settings/privacy"

    fun goal(id: String) = "goal/$id"
    fun entry(date: String) = "entry/$date"
}

@Composable
fun BloomRoot(vm: RootViewModel = hiltViewModel()) {
    val user by vm.user.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val locked by vm.locked.collectAsState()

    BloomTheme(themeMode = prefs.theme, accentKey = prefs.accent) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            when {
                user == null -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { AuthScreen() }
                locked && prefs.lockEnabled -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { LockScreen(onUnlocked = vm::unlock) }
                !onboarded -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { OnboardingScreen() }
                else -> {
                    Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { 
                        BloomNavHost() 
                        
                        val globalLoading by vm.globalLoading.collectAsState()
                        mobile.dairy.app.ui.components.GlobalLoadingOverlay(globalLoading)
                    }
                }
            }
        }
    }
}

@Composable
fun BloomNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") { MainScreen(nav) }
        composable(Routes.CHECK_IN) { CheckInScreen(nav) }
        composable(Routes.NEW_GOAL) { NewGoalScreen(nav) }
        composable(
            Routes.GOAL_DETAIL,
            arguments = listOf(navArgument("id") { defaultValue = "" }),
        ) { back ->
            GoalDetailScreen(nav, back.arguments?.getString("id") ?: "")
        }
        composable(
            Routes.ENTRY,
            arguments = listOf(navArgument("date") { defaultValue = "" }),
        ) { back ->
            EntryEditorScreen(nav, back.arguments?.getString("date") ?: "")
        }
        composable(Routes.SCREEN_TIME) { ScreenTimeScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.SETTINGS_APPEARANCE) { mobile.dairy.app.ui.settings.AppearanceSettingsScreen(nav) }
        composable(Routes.SETTINGS_CATEGORIES) { mobile.dairy.app.ui.settings.CategorySettingsScreen(nav) }
        composable(Routes.SETTINGS_JOURNAL) { mobile.dairy.app.ui.settings.JournalSettingsScreen(nav) }
        composable(Routes.SETTINGS_PRIVACY) { mobile.dairy.app.ui.settings.PrivacySettingsScreen(nav) }
        composable(Routes.INSIGHTS_JOURNAL) { mobile.dairy.app.ui.insights.reports.JournalReportsScreen(nav) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(rootNav: androidx.navigation.NavController) {
    val initialPage = remember { TABS.indexOfFirst { it.route == Routes.HOME }.takeIf { it >= 0 } ?: 0 }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { TABS.size }
    )
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                TABS.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { 
                            coroutineScope.launch { 
                                pagerState.animateScrollToPage(index) 
                            } 
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) { page ->
            val modifier = Modifier.fillMaxSize()
            when (TABS[page].route) {
                Routes.HOME -> DashboardContent(rootNav, modifier) { tabRoute -> 
                    val targetIndex = TABS.indexOfFirst { it.route == tabRoute }
                    if (targetIndex >= 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) }
                    }
                }
                Routes.JOURNAL -> mobile.dairy.app.ui.journal.JournalContent(rootNav, modifier)
                Routes.GOALS -> mobile.dairy.app.ui.goals.GoalsContent(rootNav, modifier)
                Routes.MONEY -> mobile.dairy.app.ui.money.MoneyContent(rootNav, modifier)
                Routes.INSIGHTS -> mobile.dairy.app.ui.insights.InsightsContent(rootNav, modifier)
            }
        }
    }
}
