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
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    const val TOUR = "tour"
    const val INFO = "info"
    const val HOME = "home"
    const val JOURNAL = "journal"
    const val GOALS = "goals"
    const val MONEY = "money"
    const val INSIGHTS = "insights"
    const val INSIGHTS_JOURNAL = "insights/journal"
    const val INSIGHTS_GOALS = "insights/goals"
    const val INSIGHTS_DIGITAL_HABITS = "insights/digital-habits"
    const val INSIGHTS_FINANCE = "insights/finance"
    const val INSIGHTS_HOLISTIC = "insights/holistic"
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
    val activeLocalUserId by vm.activeLocalUserId.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val hasSeenTour by vm.hasSeenTour.collectAsState()
    val onlineMode by vm.onlineMode.collectAsState()
    val locked by vm.locked.collectAsState()
    val globalLoading by vm.globalLoading.collectAsState()
    val initialLoading by vm.initialLoading.collectAsState()

    BloomTheme(
        themeMode = prefs.theme,
        accentKey = prefs.accent,
        fontSize = prefs.fontSize,
        boldText = prefs.boldText
    ) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                when {
                    !onboarded -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { OnboardingScreen() }
                    locked && prefs.lockEnabled -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { LockScreen(onUnlocked = vm::unlock) }
                    (onlineMode && user == null) || (!onlineMode && activeLocalUserId == null) -> Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { AuthScreen() }
                    else -> {
                        Box(androidx.compose.ui.Modifier.safeDrawingPadding()) { 
                            BloomNavHost(vm) 
                        }
                    }
                }
                
                mobile.dairy.app.ui.components.GlobalLoadingOverlay(
                    isLoading = globalLoading || initialLoading,
                    message = if (initialLoading) "Starting Bloom..." else "Syncing..."
                )
            }
        }
    }
}

@Composable
fun BloomNavHost(vm: RootViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") { MainScreen(nav, vm) }
        composable(Routes.CHECK_IN) { CheckInScreen(nav) }
        composable(Routes.NEW_GOAL) { NewGoalScreen(nav) }
        composable(
            Routes.GOAL_DETAIL,
            arguments = listOf(navArgument("id") { defaultValue = "" }),
        ) { back ->
            GoalDetailScreen(nav, back.arguments?.getString("id") ?: "")
        }
        composable(Routes.ENTRY, arguments = listOf(navArgument("date") { defaultValue = "" })) { back ->
            EntryEditorScreen(nav, back.arguments?.getString("date") ?: "")
        }
        composable(Routes.SCREEN_TIME) { ScreenTimeScreen(nav) }
        composable(Routes.TOUR) { 
            androidx.compose.runtime.LaunchedEffect(Unit) {
                vm.resetTour()
                nav.popBackStack()
            }
        }
        composable(Routes.INFO) { mobile.dairy.app.ui.info.DoYouKnowScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.SETTINGS_APPEARANCE) { mobile.dairy.app.ui.settings.AppearanceSettingsScreen(nav) }
        composable(Routes.SETTINGS_CATEGORIES) { mobile.dairy.app.ui.settings.CategorySettingsScreen(nav) }
        composable(Routes.SETTINGS_JOURNAL) { mobile.dairy.app.ui.settings.JournalSettingsScreen(nav) }
        composable(Routes.SETTINGS_PRIVACY) { mobile.dairy.app.ui.settings.PrivacySettingsScreen(nav) }
        composable(Routes.INSIGHTS_JOURNAL) { mobile.dairy.app.ui.insights.reports.JournalReportsScreen(nav) }
        composable(Routes.INSIGHTS_GOALS) { mobile.dairy.app.ui.insights.reports.GoalReportsScreen(nav) }
        composable(Routes.INSIGHTS_DIGITAL_HABITS) { mobile.dairy.app.ui.insights.reports.DigitalHabitsScreen(nav) }
        composable(Routes.INSIGHTS_FINANCE) { mobile.dairy.app.ui.insights.reports.FinanceReportsScreen(nav) }
        composable(Routes.INSIGHTS_HOLISTIC) { mobile.dairy.app.ui.insights.reports.HolisticInsightsScreen(nav) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(rootNav: androidx.navigation.NavController, vm: RootViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val hasSeenTour by vm.hasSeenTour.collectAsState()
    val homePageIndex = remember { TABS.indexOfFirst { it.route == Routes.HOME }.takeIf { it >= 0 } ?: 0 }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = homePageIndex,
        pageCount = { TABS.size }
    )
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val exitQuestions = remember {
        try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val array = jsonObject.optJSONArray("exit_questions")
            if (array != null && array.length() > 0) {
                List(array.length()) { i -> array.getString(i) }
            } else {
                listOf("Are you going to continue your goal progress today?")
            }
        } catch (e: Exception) {
            listOf("Are you going to continue your goal progress today?")
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var currentExitQuestion by remember { mutableStateOf("") }

    BackHandler {
        if (pagerState.currentPage != homePageIndex) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(homePageIndex)
            }
        } else {
            currentExitQuestion = exitQuestions.randomOrNull() ?: "Are you going to continue your goal progress today?"
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("🌱 Pause Your Progress?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    currentExitQuestion,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text("Exit App", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Stay & Grow", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

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
        
        if (!hasSeenTour) {
            mobile.dairy.app.ui.onboarding.InAppTourOverlay(
                pagerState = pagerState,
                onFinish = { vm.finishTour() }
            )
        }
    }
}
