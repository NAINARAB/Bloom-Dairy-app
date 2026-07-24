package mobile.dairy.app.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.data.EntryRepository
import mobile.dairy.app.data.FinanceRepository
import mobile.dairy.app.data.GoalRepository
import mobile.dairy.app.data.InsightRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.domain.PersonRef
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.domain.InsightEngine
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.domain.Streaks
import mobile.dairy.app.services.CheckinDraft
import mobile.dairy.app.services.LocalPrefs
import mobile.dairy.app.services.Notifier
import mobile.dairy.app.ui.components.MoodPicker
import mobile.dairy.app.ui.components.RatingScale
import mobile.dairy.app.ui.components.WrapChips
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
    private val entryRepo: EntryRepository,
    private val goalRepo: GoalRepository,
    private val financeRepo: FinanceRepository,
    private val insightRepo: InsightRepository,
    private val prefsRepo: PrefsRepository,
    private val localPrefs: LocalPrefs,
) : ViewModel() {

    private val today = Dates.todayKey()

    val draft = MutableStateFlow(CheckinDraft(date = today))
    val goals = goalRepo.goals(listOf("active"))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val saving = MutableStateFlow(false)
    val doneMessage = MutableStateFlow<String?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    init {
        // Draft recovery: restore only if it belongs to today.
        viewModelScope.launch {
            localPrefs.draft.first()?.let { saved ->
                if (saved.date == today) draft.value = saved
            }
        }
    }

    /** Every edit auto-saves — a killed app never loses the check-in. */
    fun patch(transform: (CheckinDraft) -> CheckinDraft) {
        draft.value = transform(draft.value)
        viewModelScope.launch { runCatching { localPrefs.saveDraft(draft.value) } }
    }

    fun finish() {
        if (saving.value) return
        saving.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeout(30000L) { // Increased timeout to 30s for robustness
                    val d = draft.value
                    val recentEntries = entryRepo.entries(30).first()
                    val prefs = prefsRepo.appPrefs().first()
                    val activeGoals = goals.value

                    val entry = JournalEntry(
                        id = today, date = today,
                        moods = d.moods, moodCauses = d.moodCauses,
                        bestPart = d.bestPart.ifBlank { null },
                        hardestPart = d.hardestPart.ifBlank { null },
                        goodThings = d.goodThings.ifBlank { null },
                        mistakes = d.mistakes.ifBlank { null },
                        lessons = d.lessons.ifBlank { null },
                        gratitude = d.gratitude,
                        people = d.people,
                        goodDecision = d.goodDecision.ifBlank { null },
                        improvement = d.improvement.ifBlank { null },
                        note = d.note.ifBlank { null },
                        tags = d.moodCauses,
                        favorite = recentEntries.firstOrNull { it.date == today }?.favorite ?: false,
                        important = recentEntries.firstOrNull { it.date == today }?.important ?: false,
                        createdAt = recentEntries.firstOrNull { it.date == today }?.createdAt ?: 0,
                    )
                    entryRepo.upsertEntry(entry)
                    entryRepo.upsertRating(
                        DailyRating(date = today, overall = d.overall, focus = d.focus, goalEffort = d.effort)
                    )

                    // Ensure everything is synced before clearing draft
                    kotlinx.coroutines.delay(500)

                    d.spent.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                        financeRepo.addExpense(Expense(date = today, amount = it, category = "other",
                            necessity = "necessary", note = "Daily check-in total"))
                    }
                    d.saved.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                        financeRepo.addSaving(Saving(date = today, amount = it, kind = "saved"))
                    }
                    d.avoided.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                        financeRepo.addSaving(Saving(date = today, amount = it, kind = "avoided"))
                    }

                    // Supportive completion message from the on-device engine.
                    val ctx = InsightContext(
                        today = today, currency = prefs.currency,
                        entries = recentEntries.filter { it.date != today } + entry,
                        ratings = listOf(DailyRating(date = today, overall = d.overall, focus = d.focus, goalEffort = d.effort)),
                        goals = activeGoals,
                    )
                    val message = InsightEngine.checkinCompleteMessage(ctx)
                    insightRepo.saveAll(listOf(Insight(
                        id = "$today-checkin-complete", date = today, type = "checkin-complete",
                        message = message, emoji = "\u2728", icon = "AutoAwesome", priority = 100,
                        createdAt = System.currentTimeMillis(),
                    )))

                    val streak = Streaks.current(recentEntries.map { it.date } + today, today)
                    if (Streaks.isMilestone(streak, Constants.STREAK_CELEBRATION_DAYS)) {
                        val privateMode = localPrefs.reminderSettingsNow().privateMode
                        Notifier.post(appContext, 9_100 + streak, "$streak-day streak \uD83D\uDD25",
                            "You have checked in $streak days in a row. Consistency like this changes things.",
                            privateMode, Notifier.CHANNEL_CELEBRATIONS)
                    }

                    localPrefs.clearDraft()
                    draft.value = CheckinDraft(date = today)
                    doneMessage.value = message
                }
            } catch (e: Exception) {
                // If it's a timeout or network error, Firestore might still have it in local queue.
                // We show a message but allow them to proceed if it was just a slow sync.
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    errorMessage.value = "Sync taking longer than expected. Your check-in is saved locally."
                } else {
                    errorMessage.value = "An error occurred, but your data is saved on this device."
                }
                // Allow "Done" screen to show even if sync is pending
                doneMessage.value = "Checked in! Your data will sync when you are back online."
            } finally {
                saving.value = false
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Screen                                                               */
/* ------------------------------------------------------------------ */

private val STEP_TITLES = listOf(
    "How are you feeling?" to "Pick everything that fits — days are rarely one thing.",
    "What made you feel this way?" to "Situations, habits — tap or add your own.",
    "Who influenced your day?" to "People who made you feel stronger, happy, or even stressed.",
    "Best part of your day?" to null,
    "And the most difficult part?" to "Naming it is often half the weight.",
    "Good things today?" to "Successes, kindnesses, or just things that went well.",
    "Any mistakes or bad choices?" to "Honesty with yourself is the first step to growth.",
    "Lessons learned?" to "What did today teach you?",
    "What are you grateful for?" to "Small things count.",
    "One good decision you made?" to null,
    "What would you improve tomorrow?" to "Small and realistic beats grand and forgotten.",
    "Rate your day" to null,
    "Money today" to "Rough numbers are fine — awareness is the goal.",
    "Anything else for today?" to "A free note, a memory, a thought for future-you.",
)

@Composable
fun CheckInScreen(nav: NavController, vm: CheckInViewModel = hiltViewModel()) {
    val draft by vm.draft.collectAsState()
    val goals by vm.goals.collectAsState()
    val saving by vm.saving.collectAsState()
    val done by vm.doneMessage.collectAsState()

    if (done != null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text("Done for today!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(done!!, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = { nav.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Home", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val steps = STEP_TITLES
    val pager = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    
                    // Progress Indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(steps.size) { i ->
                            Box(
                                Modifier
                                    .size(width = if (i == pager.currentPage) 24.dp else 8.dp, height = 8.dp)
                                    .background(
                                        if (i <= pager.currentPage) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    TextButton(onClick = { nav.popBackStack() }) {
                        Text("Save later", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pager.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Back") }
                }
                
                Button(
                    onClick = {
                        if (pager.currentPage < steps.size - 1) {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        } else {
                            vm.finish()
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !saving
                ) {
                    Text(
                        if (saving) "Saving..." 
                        else if (pager.currentPage == steps.size - 1) "Complete" 
                        else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pager,
            modifier = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = false,
            // Removed contentPadding to prevent adjacent pages from being visible
            // Padding is now handled inside each step's Column
        ) { page ->
            val (title, hint) = steps[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp) // Added padding here instead
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                if (hint != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(32.dp))
                StepContent(title, draft, goals, vm::patch)
            }
        }
    }

    if (saving) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            BloomCard(modifier = Modifier.width(280.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Syncing with Bloom...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    title: String,
    draft: CheckinDraft,
    goals: List<Goal>,
    patch: ((CheckinDraft) -> CheckinDraft) -> Unit,
) {
    when (title) {
        "How are you feeling?" -> MoodPicker(draft.moods) { key ->
            patch { d ->
                d.copy(moods = if (d.moods.contains(key)) d.moods - key else d.moods + key)
            }
        }

        "What made you feel this way?" -> {
            WrapChips {
                (Constants.CAUSE_SUGGESTIONS + draft.moodCauses).distinct().forEach { c ->
                    FilterChip(
                        selected = draft.moodCauses.contains(c),
                        onClick = {
                            patch { d ->
                                d.copy(moodCauses = if (d.moodCauses.contains(c)) d.moodCauses - c else d.moodCauses + c)
                            }
                        },
                        label = { Text(c) },
                    )
                }
            }
        }

        "Who influenced your day?" -> {
            PeopleFeelingPicker(draft.people) { newList ->
                patch { it.copy(people = newList) }
            }
        }

        "Best part of your day?" -> NoteField(draft.bestPart, "A moment worth remembering…") { v ->
            patch { it.copy(bestPart = v) }
        }

        "And the most difficult part?" -> NoteField(draft.hardestPart, "What was heavy today…") { v ->
            patch { it.copy(hardestPart = v) }
        }

        "Good things today?" -> NoteField(draft.goodThings, "Something you did well or something good that happened...") { v ->
            patch { it.copy(goodThings = v) }
        }

        "Any mistakes or bad choices?" -> NoteField(draft.mistakes, "It's okay to be honest with yourself...") { v ->
            patch { it.copy(mistakes = v) }
        }

        "Lessons learned?" -> NoteField(draft.lessons, "What's the takeaway from today?") { v ->
            patch { it.copy(lessons = v) }
        }

        "What are you grateful for?" -> {
            WrapChips {
                (Constants.GRATITUDE_SUGGESTIONS + draft.gratitude).distinct().forEach { g ->
                    FilterChip(
                        selected = draft.gratitude.contains(g),
                        onClick = {
                            patch { d ->
                                d.copy(gratitude = if (d.gratitude.contains(g)) d.gratitude - g else d.gratitude + g)
                            }
                        },
                        label = { Text(g) },
                    )
                }
            }
        }

        "One good decision you made?" -> {
            WrapChips {
                Constants.DECISION_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = draft.goodDecision == c,
                        onClick = { patch { it.copy(goodDecision = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.goodDecision, "Or write your own…", single = true) { v ->
                patch { it.copy(goodDecision = v) }
            }
        }

        "What would you improve tomorrow?" -> {
            WrapChips {
                Constants.IMPROVEMENT_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = draft.improvement == c,
                        onClick = { patch { it.copy(improvement = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.improvement, "One small thing…", single = true) { v ->
                patch { it.copy(improvement = v) }
            }
        }

        "Rate your day" -> {
            RatingScale("Overall day", Icons.Default.WbSunny, draft.overall) { n -> patch { it.copy(overall = n) } }
            RatingScale("Focus", Icons.Default.FilterCenterFocus, draft.focus) { n -> patch { it.copy(focus = n) } }
            RatingScale("Effort on goals", Icons.Default.Speed, draft.effort) { n -> patch { it.copy(effort = n) } }
        }

        "Money today" -> {
            NoteField(draft.spent, "Spent", single = true, numeric = true) { v -> patch { it.copy(spent = v) } }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.saved, "Saved", single = true, numeric = true) { v -> patch { it.copy(saved = v) } }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.avoided, "Unnecessary spending you avoided", single = true, numeric = true) { v ->
                patch { it.copy(avoided = v) }
            }
        }

        "Anything else for today?" -> NoteField(draft.note, "Dear diary…", minLines = 5) { v ->
            patch { it.copy(note = v) }
        }
    }
}

@Composable
private fun NoteField(
    value: String,
    placeholder: String,
    single: Boolean = false,
    numeric: Boolean = false,
    minLines: Int = 3,
    autoFocus: Boolean = true,
    onChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            kotlinx.coroutines.delay(150)
            runCatching { focusRequester.requestFocus() }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = single,
        minLines = if (single) 1 else minLines,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
    )
}

@Composable
fun PeopleFeelingPicker(people: List<PersonRef>, onUpdate: (List<PersonRef>) -> Unit) {
    var newName by remember { mutableStateOf("") }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("Add someone...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onUpdate(people + PersonRef(name = newName.trim(), feeling = "happy"))
                        newName = ""
                    }
                },
                enabled = newName.isNotBlank()
            ) {
                Text("Add")
            }
        }

        if (people.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
        }

        people.forEachIndexed { index, person ->
            BloomCard(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(person.name, style = MaterialTheme.typography.titleMedium)
                        Text("How did they make you feel?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { 
                        onUpdate(people.toMutableList().apply { removeAt(index) }) 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                WrapChips {
                    val feelings = listOf("happy", "stronger", "motivated", "calm", "sad", "angry", "stressed")
                    feelings.forEach { f ->
                        FilterChip(
                            selected = person.feeling == f,
                            onClick = {
                                onUpdate(people.mapIndexed { i, p -> if (i == index) p.copy(feeling = f) else p })
                            },
                            label = { Text(f.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }
    }
}
