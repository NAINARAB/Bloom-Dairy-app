package mobile.dairy.app.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Mood
import mobile.dairy.app.data.EntryRepository
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.PersonRef
import mobile.dairy.app.ui.checkin.PeopleFeelingPicker
import androidx.compose.material.icons.filled.DateRange
import mobile.dairy.app.ui.components.BloomDateRangePicker
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.EmptyState
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.MoodEmojiRow
import mobile.dairy.app.ui.components.MoodPicker
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.WrapChips
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val entryRepo: EntryRepository,
    private val prefsRepo: mobile.dairy.app.data.PrefsRepository,
) : ViewModel() {

    val appPrefs = prefsRepo.appPrefs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mobile.dairy.app.domain.AppPrefs())

    val searchQuery = MutableStateFlow("")
    private val dateRange = MutableStateFlow<Pair<String?, String?>>(Dates.addDays(Dates.todayKey(), -7) to Dates.todayKey())

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries = combine(searchQuery, dateRange) { q, range -> Pair(q, range) }
        .flatMapLatest { (q, range) -> 
            if (q.isNotBlank()) {
                entryRepo.entries(null, null)
            } else {
                entryRepo.entries(range.first, range.second)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ratings = entryRepo.ratings(90)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateFilter(start: String?, end: String?) {
        dateRange.value = start to end
    }

    fun updateSearch(q: String) {
        searchQuery.value = q
    }

    fun toggleFavorite(entry: JournalEntry) {
        viewModelScope.launch { runCatching { entryRepo.setFlag(entry.date, "favorite", !entry.favorite) } }
    }
}

/* ------------------------------------------------------------------ */
/* Timeline + search                                                    */
/* ------------------------------------------------------------------ */

@Composable
fun JournalScreen(nav: NavController) {
    // Legacy wrapper
    JournalContent(nav)
}

@Composable
fun JournalContent(nav: NavController, modifier: Modifier = Modifier, vm: JournalViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsState()
    val ratings by vm.ratings.collectAsState()
    val ratingsMap = remember(ratings) { ratings.associateBy { it.date } }
    
    val appPrefsState by vm.appPrefs.collectAsState()
    val journalQuestions = appPrefsState.journalQuestions

    var query by rememberSaveable { mutableStateOf("") }
    var moodFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf<String?>(Dates.addDays(Dates.todayKey(), -7)) }
    var endDate by rememberSaveable { mutableStateOf<String?>(Dates.todayKey()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Quick Range state
    var quickRange by rememberSaveable { mutableStateOf("week") } // all | week | month | custom

    // Field-specific filters
    var filterMistakes by rememberSaveable { mutableStateOf(false) }
    var filterLessons by rememberSaveable { mutableStateOf(false) }
    var filterGratitude by rememberSaveable { mutableStateOf(false) }

    // Search & filters run client-side over the synced window; composite
    // indexes in firestore.indexes.json support server-side variants at scale.
    val filtered = entries.filter { e ->
        if (moodFilter != null && !e.moods.contains(moodFilter)) return@filter false
        if (favoritesOnly && !e.favorite) return@filter false
        
        // Field-specific logic
        if (filterMistakes && e.mistakes.isNullOrBlank()) return@filter false
        if (filterLessons && e.lessons.isNullOrBlank()) return@filter false
        if (filterGratitude && e.gratitude.isEmpty()) return@filter false

        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return@filter true
        val hay = buildList {
            addAll(listOfNotNull(e.note, e.bestPart, e.hardestPart, e.dayReason, e.goodThings, e.mistakes, e.lessons))
            addAll(e.tags); addAll(e.moodCauses); addAll(e.gratitude)
            addAll(e.people.map { it.name })
        }.joinToString(" ").lowercase()
        hay.contains(needle)
    }

    val grouped = filtered.groupBy { Dates.startOfMonth(it.date) }.toSortedMap(compareByDescending { it })

    if (showFilterDialog) {
        mobile.dairy.app.ui.components.GlobalFilterDialog(
            initialStartDate = startDate,
            initialEndDate = endDate,
            initialQuickRange = quickRange,
            onDismiss = { showFilterDialog = false },
            onApply = { range, start, end ->
                quickRange = range
                startDate = start
                endDate = end
                showFilterDialog = false
                
                val effectiveStart = when(range) {
                    "today" -> Dates.todayKey()
                    "week" -> Dates.addDays(Dates.todayKey(), -7)
                    "month" -> Dates.addDays(Dates.todayKey(), -30)
                    "custom" -> start
                    else -> null
                }
                val effectiveEnd = if (range == "custom") end else Dates.todayKey()
                vm.updateFilter(effectiveStart, effectiveEnd)
            }
        ) {
            mobile.dairy.app.ui.components.WrapChips {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Favourites") },
                    leadingIcon = {
                        Icon(
                            if (favoritesOnly) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = filterMistakes,
                    onClick = { filterMistakes = !filterMistakes },
                    label = { Text("Mistakes") }
                )
                FilterChip(
                    selected = filterLessons,
                    onClick = { filterLessons = !filterLessons },
                    label = { Text("Lessons") }
                )
                FilterChip(
                    selected = filterGratitude,
                    onClick = { filterGratitude = !filterGratitude },
                    label = { Text("Gratitude") }
                )
                
                mobile.dairy.app.core.Mood.entries.forEach { m ->
                    FilterChip(
                        selected = moodFilter == m.key,
                        onClick = { moodFilter = if (moodFilter == m.key) null else m.key },
                        label = { Text("${m.emoji} ${m.label}") }
                    )
                }
            }
        }
    }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Journal", style = MaterialTheme.typography.displaySmall)
                androidx.compose.material3.Button(
                    onClick = { nav.navigate(Routes.CHECK_IN) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ New", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { 
                        query = it
                        vm.updateSearch(it)
                    },
                    placeholder = { Text("Search people, tags, notes, memories…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            if (quickRange == "custom" && startDate != null && query.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("$startDate to $endDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    Icons.Default.MenuBook, "Your story starts here",
                    "Your first check-in becomes your first page. It takes about two minutes.",
                )
            }
        }
        grouped.forEach { (month, monthEntries) ->
            item {
                Spacer(Modifier.height(16.dp))
                Eyebrow(Dates.monthLabel(month))
                Spacer(Modifier.height(8.dp))
            }
            items(monthEntries.size) { i ->
                val entry = monthEntries[i]
                val rating = ratingsMap[entry.date]
                JournalCard(
                    entry = entry,
                    rating = rating,
                    questions = journalQuestions,
                    gradientKey = appPrefsState.cardGradient,
                    onOpen = { nav.navigate(Routes.entry(entry.date)) },
                    onToggleFavorite = { vm.toggleFavorite(entry) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun JournalCard(
    entry: JournalEntry,
    rating: mobile.dairy.app.domain.DailyRating?,
    questions: List<mobile.dairy.app.domain.JournalQuestionDef>,
    gradientKey: String?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var ratingsExpanded by remember { mutableStateOf(false) }
    
    val gradientColors = mobile.dairy.app.ui.theme.GRADIENTS[gradientKey]?.let { 
        if (androidx.compose.foundation.isSystemInDarkTheme()) it.dark else it.light 
    } ?: listOf(Color(0xFF533483), Color(0xFF16213E)) // Fallback to Midnight

    mobile.dairy.app.ui.components.GradientCard(
        onClick = onOpen,
        gradientColors = gradientColors
    ) {
        // --- TOP ROW: Date & Favorite/Pin Icons ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.friendly(entry.date), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.important) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Important",
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (entry.favorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(18.dp),
                        tint = if (entry.favorite) Color(0xFFE8B34B) else MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // --- SECOND ROW: HUGE EMOJI & MAIN RATINGS ---
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val primaryMood = entry.moods.firstOrNull()?.let { mobile.dairy.app.core.Mood.fromKey(it) }
            Text(
                text = primaryMood?.emoji ?: "📓",
                fontSize = 56.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            // Render Overall and Productivity statically at the top
            val mainRatings = listOfNotNull(
                rating?.overall?.let { "Overall Day" to it },
                rating?.productivity?.let { "Productivity" to it }
            )
            
            if (mainRatings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    mainRatings.forEach { (label, value) ->
                        mobile.dairy.app.ui.components.CircularRatingIndicator(
                            value = value,
                            label = label.take(12),
                            gradientColors = gradientColors
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- MIDDLE SECTION: TEXT/LIST QUESTIONS ---
        Column(Modifier.fillMaxWidth()) {
            questions.filter { it.type == "text" || it.type == "person" || it.type == "emoji" }.forEach { q ->
                val answerString: String? = when (q.id) {
                    "q_feeling" -> null
                    "q_feeling_reason" -> if (entry.moodCauses.isNotEmpty()) entry.moodCauses.joinToString(", ") else null
                    "q_people" -> if (entry.people.isNotEmpty()) entry.people.joinToString(", ") { it.name } else null
                    "q_best_part" -> entry.bestPart
                    "q_hardest_part" -> entry.hardestPart
                    "q_good_things" -> entry.goodThings
                    "q_mistakes" -> entry.mistakes
                    "q_lessons" -> entry.lessons
                    "q_gratitude" -> if (entry.gratitude.isNotEmpty()) entry.gratitude.joinToString(", ") else null
                    "q_good_decision" -> entry.goodDecision
                    "q_improve_tomorrow" -> entry.improvement
                    "q_anything_else" -> entry.note
                    else -> if (q.isCustom && q.type == "text") entry.customAnswers[q.id] else null
                }
                
                val shouldShow = !answerString.isNullOrBlank() || q.isActive
                
                if (shouldShow && q.id != "q_feeling") {
                    val icon = when (q.id) {
                        "q_people" -> Icons.Default.Group
                        "q_feeling_reason" -> Icons.Default.Psychology
                        "q_best_part", "q_good_things" -> Icons.Default.Favorite
                        "q_hardest_part", "q_mistakes" -> Icons.Default.ErrorOutline
                        "q_lessons" -> Icons.Default.Lightbulb
                        "q_good_decision", "q_improve_tomorrow" -> Icons.Default.CheckCircleOutline
                        "q_anything_else" -> Icons.Default.ChatBubbleOutline
                        else -> Icons.Default.ChatBubbleOutline
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left column
                        Row(Modifier.weight(0.40f), verticalAlignment = Alignment.Top) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 6.dp), tint = gradientColors.first())
                            Text(
                                text = q.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Separator
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        // Right column
                        if (!answerString.isNullOrBlank()) {
                            Text(
                                text = answerString,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.60f),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else {
                            Text("", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.60f))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // --- BOTTOM SECTION: METADATA GRID ---
        val metadataList = mutableListOf<Pair<String, String>>()
        questions.filter { it.type == "date" || it.type == "toggle" || it.type == "dropdown" || it.type == "number" }.forEach { q ->
            val value = entry.customAnswers[q.id]
            if (!value.isNullOrBlank()) {
                metadataList.add(q.title to value)
            }
        }
        
        if (metadataList.isNotEmpty()) {
            metadataList.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Start) {
                    rowItems.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    val missing = 3 - rowItems.size
                    repeat(missing) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // --- TAGS ---
        if (entry.tags.isNotEmpty()) {
            mobile.dairy.app.ui.components.WrapChips {
                entry.tags.forEach { t ->
                    Box(
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("#$t", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // --- EXPANDABLE RATINGS SECTION ---
        val secondaryRatings = mutableListOf<Pair<String, Int>>()
        rating?.energy?.let { secondaryRatings.add("Energy" to it) }
        rating?.happiness?.let { secondaryRatings.add("Happiness" to it) }
        rating?.focus?.let { secondaryRatings.add("Focus" to it) }
        rating?.discipline?.let { secondaryRatings.add("Discipline" to it) }
        rating?.goalEffort?.let { secondaryRatings.add("Goal effort" to it) }
        rating?.stress?.let { secondaryRatings.add("Stress" to it) }
        rating?.sleep?.let { secondaryRatings.add("Sleep" to it) }
        rating?.financialDiscipline?.let { secondaryRatings.add("Finances" to it) }
        
        questions.forEach { q ->
            if (q.type == "slider") {
                val value = entry.customAnswers[q.id]?.toIntOrNull()
                if (value != null) secondaryRatings.add(q.title to value)
            }
        }

        if (secondaryRatings.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            TextButton(
                onClick = { ratingsExpanded = !ratingsExpanded },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (ratingsExpanded) "Hide ratings" else "Show ratings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    if (ratingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = ratingsExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    secondaryRatings.chunked(4).forEach { rowRatings ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowRatings.forEach { (label, value) ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    mobile.dairy.app.ui.components.CircularRatingIndicator(
                                        value = value,
                                        label = label.take(12),
                                        gradientColors = gradientColors
                                    )
                                }
                            }
                            val missing = 4 - rowRatings.size
                            repeat(missing) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Detailed entry editor                                                */
/* ------------------------------------------------------------------ */

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    private val entryRepo: EntryRepository,
    private val prefsRepo: mobile.dairy.app.data.PrefsRepository
) : ViewModel() {

    val appPrefs = prefsRepo.appPrefs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mobile.dairy.app.domain.AppPrefs())

    val loaded = MutableStateFlow<JournalEntry?>(null)
    val loadedRating = MutableStateFlow<mobile.dairy.app.domain.DailyRating?>(null)
    val ready = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val saved = MutableStateFlow(false)

    val recentPeople = entryRepo.entries(30)
        .map { entries -> entries.flatMap { it.people }.distinctBy { it.name }.take(8) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(date: String) {
        if (ready.value) return
        viewModelScope.launch {
            loaded.value = runCatching { entryRepo.entry(date).first() }.getOrNull()
                ?: JournalEntry(id = date, date = date)
            loadedRating.value = runCatching { entryRepo.rating(date).first() }.getOrNull()
                ?: mobile.dairy.app.domain.DailyRating(date = date)
            ready.value = true
        }
    }

    fun save(entry: JournalEntry, rating: mobile.dairy.app.domain.DailyRating) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching {
                entryRepo.upsertEntry(entry)
                entryRepo.upsertRating(rating)
            }
                .onSuccess {
                    busy.value = false
                    saved.value = true
                }
                .onFailure {
                    busy.value = false
                    error.value = "Failed to save to cloud: ${it.message}"
                }
        }
    }

    fun toggleFlag(entry: JournalEntry, field: String) {
        viewModelScope.launch {
            runCatching {
                entryRepo.upsertEntry(
                    if (field == "favorite") entry.copy(favorite = !entry.favorite)
                    else entry.copy(important = !entry.important)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(nav: NavController, date: String, vm: EntryEditorViewModel = hiltViewModel()) {
    LaunchedEffect(date) { vm.load(date) }
    val ready by vm.ready.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val loadedRating by vm.loadedRating.collectAsState()
    val saved by vm.saved.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val recentPeople by vm.recentPeople.collectAsState()
    if (saved) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    if (!ready || loaded == null) {
        mobile.dairy.app.ui.components.GlobalLoadingOverlay(true, "Loading entry...")
        return
    }

    var entry by remember(loaded) { mutableStateOf(loaded!!) }
    var rating by remember(loadedRating) { mutableStateOf(loadedRating ?: mobile.dairy.app.domain.DailyRating(date = date)) }

    val createdDateKey = remember(entry) {
        if (entry.createdAt > 0L) Dates.fromMillis(entry.createdAt) else entry.date
    }
    val daysOld = remember(createdDateKey) {
        runCatching { Dates.daysBetween(createdDateKey, Dates.todayKey()) }.getOrDefault(0L)
    }
    val isEditable = daysOld <= 7

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(Dates.friendly(date), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.save(entry, rating) }, enabled = !busy && isEditable) {
                        if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                        else Icon(Icons.Default.Done, contentDescription = "Save", tint = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            if (!isEditable) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Entries older than 7 days from creation date cannot be edited (Read-Only).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            val appPrefsState by vm.appPrefs.collectAsState()
            val activeQuestions = appPrefsState.journalQuestions.filter { it.isActive }
            
            activeQuestions.forEach { q ->
                EditorStepContent(
                    q = q,
                    entry = entry,
                    rating = rating,
                    recentPeople = recentPeople,
                    isEditable = isEditable,
                    patch = { patch -> entry = patch(entry) },
                    patchRating = { patch -> rating = patch(rating) },
                    error = error
                )
            }

            SectionTitle("Tags")
            WrapChips {
                entry.tags.forEach { t ->
                    FilterChip(
                        selected = true,
                        enabled = isEditable,
                        onClick = { if (isEditable) entry = entry.copy(tags = entry.tags - t) },
                        label = { Text("#$t") }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { vm.save(entry, rating) }, 
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !busy && isEditable
            ) { 
                if (busy) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text(if (isEditable) "Save entry" else "Read-only (7+ days old)", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    mobile.dairy.app.ui.components.GlobalLoadingOverlay(busy, "Saving entry...")
}

private fun JournalEntry.withPersonFeeling(index: Int, feeling: String): JournalEntry =
    copy(people = people.mapIndexed { i, p -> if (i == index) p.copy(feeling = feeling) else p })

@Composable
private fun Editor(placeholder: String, value: String, minLines: Int = 3, numeric: Boolean = false, enabled: Boolean = true, onChange: (String) -> Unit) {
    var text by rememberSaveable(placeholder) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 1500) { text = it; onChange(it) } },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        minLines = minLines,
        singleLine = minLines == 1,
        enabled = enabled,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
    )
}

@Composable
private fun EditorStepContent(
    q: mobile.dairy.app.domain.JournalQuestionDef,
    entry: mobile.dairy.app.domain.JournalEntry,
    rating: mobile.dairy.app.domain.DailyRating,
    recentPeople: List<mobile.dairy.app.domain.PersonRef>,
    isEditable: Boolean,
    patch: ((mobile.dairy.app.domain.JournalEntry) -> mobile.dairy.app.domain.JournalEntry) -> Unit,
    patchRating: ((mobile.dairy.app.domain.DailyRating) -> mobile.dairy.app.domain.DailyRating) -> Unit,
    error: String?
) {
    SectionTitle(q.title)
    if (q.subtitle != null) {
        Text(q.subtitle!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }

    if (q.isCustom) {
        val currentValue = entry.customAnswers[q.id] ?: ""
        when (q.type) {
            "text" -> Editor(q.title, currentValue, minLines = 3, enabled = isEditable) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            "number" -> Editor(q.title, currentValue, minLines = 1, numeric = true, enabled = isEditable) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            "slider" -> mobile.dairy.app.ui.components.RatingScale(q.title, Icons.Default.Star, currentValue.toIntOrNull() ?: 5, enabled = isEditable) { n -> patch { it.copy(customAnswers = it.customAnswers + (q.id to n.toString())) } }
            "toggle" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("No", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(16.dp))
                    androidx.compose.material3.Switch(checked = currentValue == "yes", enabled = isEditable, onCheckedChange = { isChecked -> patch { d -> d.copy(customAnswers = d.customAnswers + (q.id to if (isChecked) "yes" else "no")) } })
                    Spacer(Modifier.width(16.dp))
                    Text("Yes", style = MaterialTheme.typography.titleMedium)
                }
            }
            "dropdown" -> {
                WrapChips {
                    q.options.forEach { opt ->
                        FilterChip(
                            selected = currentValue == opt,
                            enabled = isEditable,
                            onClick = { patch { d -> d.copy(customAnswers = d.customAnswers + (q.id to opt)) } },
                            label = { Text(opt) }
                        )
                    }
                }
            }
            "date" -> DatePickerField(currentValue, "Select date") { v -> if (isEditable) patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
            else -> Editor(q.title, currentValue, minLines = 3, enabled = isEditable) { v -> patch { it.copy(customAnswers = it.customAnswers + (q.id to v)) } }
        }
        Spacer(Modifier.height(24.dp))
        return
    }

    when (q.id) {
        "q_feeling" -> MoodPicker(entry.moods) { key ->
            if (isEditable) patch { d -> d.copy(moods = if (d.moods.contains(key)) d.moods - key else d.moods + key) }
        }
        "q_feeling_reason" -> {
            WrapChips {
                (Constants.CAUSE_SUGGESTIONS + entry.moodCauses).distinct().forEach { c ->
                    FilterChip(
                        selected = entry.moodCauses.contains(c),
                        enabled = isEditable,
                        onClick = { patch { d -> d.copy(moodCauses = if (d.moodCauses.contains(c)) d.moodCauses - c else d.moodCauses + c) } },
                        label = { Text(c) },
                    )
                }
            }
        }
        "q_people" -> {
            PeopleFeelingPicker(entry.people, recentPeople) { newList ->
                if (isEditable) patch { it.copy(people = newList) }
            }
        }
        "q_best_part" -> Editor("A moment worth remembering…", entry.bestPart ?: "", enabled = isEditable) { v -> patch { it.copy(bestPart = v.ifBlank { null }) } }
        "q_hardest_part" -> Editor("What was heavy today…", entry.hardestPart ?: "", enabled = isEditable) { v -> patch { it.copy(hardestPart = v.ifBlank { null }) } }
        "q_good_things" -> Editor("Something you did well or something good that happened...", entry.goodThings ?: "", enabled = isEditable) { v -> patch { it.copy(goodThings = v.ifBlank { null }) } }
        "q_mistakes" -> Editor("It's okay to be honest with yourself...", entry.mistakes ?: "", enabled = isEditable) { v -> patch { it.copy(mistakes = v.ifBlank { null }) } }
        "q_lessons" -> Editor("What's the takeaway from today?", entry.lessons ?: "", enabled = isEditable) { v -> patch { it.copy(lessons = v.ifBlank { null }) } }
        "q_gratitude" -> {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
            }
            WrapChips {
                (Constants.GRATITUDE_SUGGESTIONS + entry.gratitude).distinct().forEach { g ->
                    FilterChip(
                        selected = entry.gratitude.contains(g),
                        enabled = isEditable,
                        onClick = { patch { d -> d.copy(gratitude = if (d.gratitude.contains(g)) d.gratitude - g else d.gratitude + g) } },
                        label = { Text(g) },
                    )
                }
            }
        }
        "q_good_decision" -> {
            WrapChips {
                Constants.DECISION_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = entry.goodDecision == c, enabled = isEditable, onClick = { patch { it.copy(goodDecision = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Editor("Or write your own…", entry.goodDecision ?: "", minLines = 1, enabled = isEditable) { v -> patch { it.copy(goodDecision = v.ifBlank { null }) } }
        }
        "q_improve_tomorrow" -> {
            WrapChips {
                Constants.IMPROVEMENT_SUGGESTIONS.forEach { c ->
                    FilterChip(selected = entry.improvement == c, enabled = isEditable, onClick = { patch { it.copy(improvement = c) } }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Editor("One small thing…", entry.improvement ?: "", minLines = 1, enabled = isEditable) { v -> patch { it.copy(improvement = v.ifBlank { null }) } }
        }
        "q_rating_performance" -> {
            mobile.dairy.app.ui.components.RatingScale("Productivity", Icons.Default.TrendingUp, rating.productivity, enabled = isEditable) { n -> patchRating { it.copy(productivity = n) } }
            mobile.dairy.app.ui.components.RatingScale("Focus", Icons.Default.FilterCenterFocus, rating.focus, enabled = isEditable) { n -> patchRating { it.copy(focus = n) } }
            mobile.dairy.app.ui.components.RatingScale("Discipline", Icons.Default.SelfImprovement, rating.discipline, enabled = isEditable) { n -> patchRating { it.copy(discipline = n) } }
            mobile.dairy.app.ui.components.RatingScale("Goal effort", Icons.Default.FitnessCenter, rating.goalEffort, enabled = isEditable) { n -> patchRating { it.copy(goalEffort = n) } }
            mobile.dairy.app.ui.components.RatingScale("Financial discipline", Icons.Default.Savings, rating.financialDiscipline, enabled = isEditable) { n -> patchRating { it.copy(financialDiscipline = n) } }
        }
        "q_rating_wellbeing" -> {
            mobile.dairy.app.ui.components.RatingScale("Overall day", Icons.Default.WbSunny, rating.overall, enabled = isEditable) { n -> patchRating { it.copy(overall = n) } }
            mobile.dairy.app.ui.components.RatingScale("Energy", Icons.Default.Bolt, rating.energy, enabled = isEditable) { n -> patchRating { it.copy(energy = n) } }
            mobile.dairy.app.ui.components.RatingScale("Happiness", Icons.Default.SentimentSatisfied, rating.happiness, enabled = isEditable) { n -> patchRating { it.copy(happiness = n) } }
            mobile.dairy.app.ui.components.RatingScale("Stress handling", Icons.Default.Spa, rating.stress, enabled = isEditable) { n -> patchRating { it.copy(stress = n) } }
            mobile.dairy.app.ui.components.RatingScale("Sleep quality", Icons.Default.Bedtime, rating.sleep, enabled = isEditable) { n -> patchRating { it.copy(sleep = n) } }
        }
        "q_rating" -> {
            mobile.dairy.app.ui.components.RatingScale("Productivity", Icons.Default.TrendingUp, rating.productivity, enabled = isEditable) { n -> patchRating { it.copy(productivity = n) } }
            mobile.dairy.app.ui.components.RatingScale("Focus", Icons.Default.FilterCenterFocus, rating.focus, enabled = isEditable) { n -> patchRating { it.copy(focus = n) } }
            mobile.dairy.app.ui.components.RatingScale("Discipline", Icons.Default.SelfImprovement, rating.discipline, enabled = isEditable) { n -> patchRating { it.copy(discipline = n) } }
            mobile.dairy.app.ui.components.RatingScale("Goal effort", Icons.Default.FitnessCenter, rating.goalEffort, enabled = isEditable) { n -> patchRating { it.copy(goalEffort = n) } }
            mobile.dairy.app.ui.components.RatingScale("Financial discipline", Icons.Default.Savings, rating.financialDiscipline, enabled = isEditable) { n -> patchRating { it.copy(financialDiscipline = n) } }
            mobile.dairy.app.ui.components.RatingScale("Overall day", Icons.Default.WbSunny, rating.overall, enabled = isEditable) { n -> patchRating { it.copy(overall = n) } }
            mobile.dairy.app.ui.components.RatingScale("Energy", Icons.Default.Bolt, rating.energy, enabled = isEditable) { n -> patchRating { it.copy(energy = n) } }
            mobile.dairy.app.ui.components.RatingScale("Happiness", Icons.Default.SentimentSatisfied, rating.happiness, enabled = isEditable) { n -> patchRating { it.copy(happiness = n) } }
            mobile.dairy.app.ui.components.RatingScale("Stress handling", Icons.Default.Spa, rating.stress, enabled = isEditable) { n -> patchRating { it.copy(stress = n) } }
            mobile.dairy.app.ui.components.RatingScale("Sleep quality", Icons.Default.Bedtime, rating.sleep, enabled = isEditable) { n -> patchRating { it.copy(sleep = n) } }
        }
        "q_anything_else" -> {
            TextField(
                value = entry.note ?: "",
                onValueChange = { v -> if (isEditable) patch { it.copy(note = v.ifBlank { null }) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                enabled = isEditable,
                placeholder = { Text("Dear diary...", style = MaterialTheme.typography.bodyLarge) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
    Spacer(Modifier.height(24.dp))
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
