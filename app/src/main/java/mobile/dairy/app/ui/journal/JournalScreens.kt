package mobile.dairy.app.ui.journal

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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
) : ViewModel() {

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

    var query by rememberSaveable { mutableStateOf("") }
    var moodFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf<String?>(null) }
    var endDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Quick Range state
    var quickRange by rememberSaveable { mutableStateOf("all") } // all | week | month | custom

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
                EntryCard(
                    entry = entry,
                    rating = rating,
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
private fun EntryCard(
    entry: JournalEntry,
    rating: mobile.dairy.app.domain.DailyRating?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val snippet = entry.bestPart ?: entry.note ?: entry.dayReason ?: entry.hardestPart ?: ""
    BloomCard(
        onClick = onOpen
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.friendly(entry.date), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
        Spacer(Modifier.height(8.dp))
        MoodEmojiRow(entry.moods)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        if (snippet.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(snippet, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 3)
        }
        
        if (rating != null) {
            Spacer(Modifier.height(12.dp))
            WrapChips {
                rating.overall?.let { 
                    Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { 
                        Text("Overall: $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) 
                    } 
                }
                rating.happiness?.let { 
                    Box(Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { 
                        Text("Happiness: $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) 
                    } 
                }
                rating.energy?.let { 
                    Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { 
                        Text("Energy: $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    } 
                }
                rating.sleep?.let { 
                    Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { 
                        Text("Sleep: $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    } 
                }
            }
        }

        if (entry.gratitude.isNotEmpty() || entry.people.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (entry.people.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(entry.people.joinToString(", ") { it.name }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                if (entry.gratitude.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(entry.gratitude.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }

        if (entry.tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            WrapChips {
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
    }
}

/* ------------------------------------------------------------------ */
/* Detailed entry editor                                                */
/* ------------------------------------------------------------------ */

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    private val entryRepo: EntryRepository,
) : ViewModel() {

    val loaded = MutableStateFlow<JournalEntry?>(null)
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
            ready.value = true
        }
    }

    fun save(entry: JournalEntry) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { entryRepo.upsertEntry(entry) }
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
    val saved by vm.saved.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val recentPeople by vm.recentPeople.collectAsState()
    if (saved) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    if (!ready || loaded == null) return

    var entry by remember { mutableStateOf(loaded!!) }

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
                    IconButton(onClick = { vm.save(entry) }, enabled = !busy) {
                        if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                        else Icon(Icons.Default.Done, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
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
            
            Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            MoodPicker(entry.moods) { key ->
                entry = entry.copy(moods = if (entry.moods.contains(key)) entry.moods - key else entry.moods + key)
            }

            Spacer(Modifier.height(32.dp))
            Text("The day, in your words", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            
            TextField(
                value = entry.note ?: "",
                onValueChange = { entry = entry.copy(note = it.ifBlank { null }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp),
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

            SectionTitle("Moments")
            Editor("Best part of your day", entry.bestPart ?: "") { entry = entry.copy(bestPart = it.ifBlank { null }) }
            Editor("Most difficult part", entry.hardestPart ?: "") { entry = entry.copy(hardestPart = it.ifBlank { null }) }

            SectionTitle("Actions & Lessons")
            Editor("Good things you did", entry.goodThings ?: "") { entry = entry.copy(goodThings = it.ifBlank { null }) }
            Editor("Mistakes or bad choices", entry.mistakes ?: "") { entry = entry.copy(mistakes = it.ifBlank { null }) }
            Editor("Lessons learned", entry.lessons ?: "") { entry = entry.copy(lessons = it.ifBlank { null }) }

            SectionTitle("Decisions")
            Editor("One good decision", entry.goodDecision ?: "") { entry = entry.copy(goodDecision = it.ifBlank { null }) }
            Editor("Improvement for tomorrow", entry.improvement ?: "") { entry = entry.copy(improvement = it.ifBlank { null }) }

            SectionTitle("People")
            PeopleFeelingPicker(entry.people, recentPeople) { entry = entry.copy(people = it) }

            SectionTitle("Gratitude")
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
            }
            WrapChips {
                (Constants.GRATITUDE_SUGGESTIONS + entry.gratitude).distinct().forEach { g ->
                    FilterChip(
                        selected = entry.gratitude.contains(g),
                        onClick = {
                            entry = entry.copy(
                                gratitude = if (entry.gratitude.contains(g)) entry.gratitude - g else entry.gratitude + g,
                            )
                        },
                        label = { Text(g) },
                    )
                }
            }

            SectionTitle("Tags")
            WrapChips {
                entry.tags.forEach { t ->
                    FilterChip(selected = true, onClick = { entry = entry.copy(tags = entry.tags - t) },
                        label = { Text("#$t") })
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { vm.save(entry) }, 
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !busy
            ) { 
                if (busy) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Save entry", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

private fun JournalEntry.withPersonFeeling(index: Int, feeling: String): JournalEntry =
    copy(people = people.mapIndexed { i, p -> if (i == index) p.copy(feeling = feeling) else p })

@Composable
private fun Editor(placeholder: String, value: String, minLines: Int = 3, onChange: (String) -> Unit) {
    var text by rememberSaveable(placeholder) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        minLines = minLines,
    )
}
