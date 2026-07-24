package mobile.dairy.app

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mobile.dairy.app.data.AuthRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.services.LocalPrefs
import mobile.dairy.app.services.Notifier
import mobile.dairy.app.services.ReminderScheduler
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
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BloomApp : Application() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
    }
}

/* ------------------------------------------------------------------ */
/* Root state: session, prefs, lock                                     */
/* ------------------------------------------------------------------ */

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    prefsRepository: PrefsRepository,
    localPrefs: LocalPrefs,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val user = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    @OptIn(ExperimentalCoroutinesApi::class)
    val prefs: StateFlow<AppPrefs> = user.flatMapLatest { u ->
        if (u == null) flowOf(AppPrefs()) else prefsRepository.appPrefs()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())

    val onboarded = localPrefs.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val locked = MutableStateFlow(false)

    init {
        // Lock the diary whenever the app leaves the foreground (if enabled).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (prefs.value.lockEnabled) locked.value = true
            }
        })
        viewModelScope.launch {
            // Ensure reminders exist on first launch.
            reminderScheduler.rescheduleAll()
        }
    }

    fun unlock() { locked.value = false }
    fun signOut() = authRepository.signOut()
}

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
    const val CHECK_IN = "check-in"
    const val NEW_GOAL = "goal/new"
    const val GOAL_DETAIL = "goal/{id}"
    const val ENTRY = "entry/{date}"
    const val SCREEN_TIME = "screen-time"
    const val SETTINGS = "settings"

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
                else -> BloomNavHost()
            }
        }
    }
}

@Composable
fun BloomNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { DashboardScreen(nav) }
        composable(Routes.JOURNAL) { JournalScreen(nav) }
        composable(Routes.GOALS) { GoalsScreen(nav) }
        composable(Routes.MONEY) { MoneyScreen(nav) }
        composable(Routes.INSIGHTS) { InsightsScreen(nav) }
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
    }
}
