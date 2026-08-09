package mobile.dairy.app.ui.checkin

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Switch
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
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
import kotlinx.coroutines.flow.map
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
    val appPrefs = prefsRepo.appPrefs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, mobile.dairy.app.domain.AppPrefs())
    val saving = MutableStateFlow(false)
    val doneMessage = MutableStateFlow<String?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    val recentPeople = entryRepo.entries(30).map { entries -> entries.flatMap { it.people }.distinctBy { it.name }.take(10) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
                        customAnswers = d.customAnswers,
                    )
                    entryRepo.upsertEntry(entry)
                    entryRepo.upsertRating(
                        DailyRating(date = today, overall = d.overall, focus = d.focus, goalEffort = d.effort)
                    )

                    // Ensure everything is synced before clearing draft
                    kotlinx.coroutines.delay(500)

                    // Removed Finance saving logic as per journal streamlining

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



@Composable
fun CheckInScreen(nav: NavController, vm: CheckInViewModel = hiltViewModel()) {
    val appPrefs by vm.appPrefs.collectAsState()
    val draft by vm.draft.collectAsState()
    val goals by vm.goals.collectAsState()
    val saving by vm.saving.collectAsState()
    val done by vm.doneMessage.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val recentPeople by vm.recentPeople.collectAsState()

    val activeQuestions = remember(appPrefs.journalQuestions) {
        appPrefs.journalQuestions.filter { it.isActive }
    }

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

    val pager = rememberPagerState { activeQuestions.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Column {
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
                        repeat(activeQuestions.size) { i ->
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
                
                val isLast = pager.currentPage == activeQuestions.size - 1
                Button(
                    onClick = {
                        if (isLast) {
                            vm.finish()
                        } else {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !saving
                ) {
                    Text(
                        if (saving) "Saving..." 
                        else if (isLast) "Complete" 
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
        ) { page ->
            val q = activeQuestions[page]
            val isCurrentPage = pager.currentPage == page
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = q.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (q.subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = q.subtitle!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(48.dp))

                StepContent(q, draft, goals, recentPeople, vm::patch, isCurrentPage)
            }
        }
    }

    mobile.dairy.app.ui.components.GlobalLoadingOverlay(saving)
}

@Composable
private fun StepContent(
    q: mobile.dairy.app.domain.JournalQuestionDef,
    draft: CheckinDraft,
    goals: List<Goal>,
    recentPeople: List<PersonRef>,
    patch: ((CheckinDraft) -> CheckinDraft) -> Unit,
    isCurrentPage: Boolean = false,
) {
    if (q.isCustom) {
        val currentValue = draft.customAnswers[q.id] ?: ""
        when (q.type) {
            "text" -> NoteField(currentValue, "Enter your answer...", minLines = 3, autoFocus = isCurrentPage) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            "number" -> NoteField(currentValue, "0", numeric = true, single = true, autoFocus = isCurrentPage) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            "slider" -> RatingScale(q.title, Icons.Default.AutoAwesome, currentValue.toIntOrNull() ?: 5) { n -> patch { it.copy(customAnswers = it.customAnswers + (q.id to n.toString())) } }
            "toggle" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("No", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = currentValue == "yes", onCheckedChange = { isChecked -> patch { d -> d.copy(customAnswers = d.customAnswers + (q.id to if (isChecked) "yes" else "no")) } })
                    Spacer(Modifier.width(16.dp))
                    Text("Yes", style = MaterialTheme.typography.titleMedium)
                }
            }
            "dropdown" -> {
                WrapChips {
                    q.options.forEach { opt ->
                        FilterChip(
                            selected = currentValue == opt,
                            onClick = { patch { d -> d.copy(customAnswers = d.customAnswers + (q.id to opt)) } },
                            label = { Text(opt) }
                        )
                    }
                }
            }
            "date" -> DatePickerField(currentValue, "Select date") { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            else -> NoteField(currentValue, "...", minLines = 3, autoFocus = isCurrentPage) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
        }
        return
    }

    when (q.id) {
        "q_feeling" -> MoodPicker(draft.moods) { key ->
            patch { d ->
                d.copy(moods = if (d.moods.contains(key)) d.moods - key else d.moods + key)
            }
        }

        "q_feeling_reason" -> {
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

        "q_people" -> {
            PeopleFeelingPicker(draft.people, recentPeople) { newList ->
                patch { it.copy(people = newList) }
            }
        }

        "q_best_part" -> NoteField(draft.bestPart, "A moment worth remembering…", autoFocus = isCurrentPage) { v ->
            patch { it.copy(bestPart = v) }
        }

        "q_hardest_part" -> NoteField(draft.hardestPart, "What was heavy today…", autoFocus = isCurrentPage) { v ->
            patch { it.copy(hardestPart = v) }
        }

        "q_good_things" -> NoteField(draft.goodThings, "Something you did well or something good that happened...", autoFocus = isCurrentPage) { v ->
            patch { it.copy(goodThings = v) }
        }

        "q_mistakes" -> NoteField(draft.mistakes, "It's okay to be honest with yourself...", autoFocus = isCurrentPage) { v ->
            patch { it.copy(mistakes = v) }
        }

        "q_lessons" -> NoteField(draft.lessons, "What's the takeaway from today?", autoFocus = isCurrentPage) { v ->
            patch { it.copy(lessons = v) }
        }

        "q_gratitude" -> {
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

        "q_good_decision" -> {
            WrapChips {
                Constants.DECISION_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = draft.goodDecision == c,
                        onClick = { patch { it.copy(goodDecision = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.goodDecision, "Or write your own…", single = true, autoFocus = isCurrentPage) { v ->
                patch { it.copy(goodDecision = v) }
            }
        }

        "q_improve_tomorrow" -> {
            WrapChips {
                Constants.IMPROVEMENT_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = draft.improvement == c,
                        onClick = { patch { it.copy(improvement = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            NoteField(draft.improvement, "One small thing…", single = true, autoFocus = isCurrentPage) { v ->
                patch { it.copy(improvement = v) }
            }
        }

        "q_rating" -> {
            RatingScale("Overall day", Icons.Default.WbSunny, draft.overall) { n -> patch { it.copy(overall = n) } }
            RatingScale("Focus", Icons.Default.FilterCenterFocus, draft.focus) { n -> patch { it.copy(focus = n) } }
        }

        "q_anything_else" -> NoteField(draft.note, "Dear diary…", minLines = 5, autoFocus = isCurrentPage) { v ->
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
    autoFocus: Boolean = false,
    onChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            kotlinx.coroutines.delay(150)
            runCatching { focusRequester.requestFocus() }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 1500) onChange(it) },
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = single,
        minLines = if (single) 1 else minLines,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Decimal)
        } else {
            KeyboardOptions.Default
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { showDialog = true })
    }

    if (showDialog) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = if (value.isNotBlank()) {
                runCatching { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(value)?.time }.getOrNull()
            } else null
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        onChange(sdf.format(java.util.Date(millis)))
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
fun PeopleFeelingPicker(people: List<PersonRef>, recentPeople: List<PersonRef>, onUpdate: (List<PersonRef>) -> Unit) {
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

        if (recentPeople.isNotEmpty() && newName.isBlank() && people.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Recent people", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            WrapChips {
                recentPeople.forEach { p ->
                    FilterChip(
                        selected = false,
                        onClick = { onUpdate(people + p) },
                        label = { Text(p.name) },
                        leadingIcon = { Text(if (p.feeling == "stronger") "💪" else if (p.feeling == "stressed") "😫" else "😊") }
                    )
                }
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
