package mobile.dairy.app.ui.goals

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardOptionKey
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Dates
import mobile.dairy.app.data.GoalRepository
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.GoalTask
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.Milestone
import mobile.dairy.app.data.newId
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.BloomTwoDatePickerDialog
import mobile.dairy.app.ui.components.EmptyState
import mobile.dairy.app.ui.components.GoalProgressBar
import mobile.dairy.app.ui.components.RatingScale
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.theme.BloomColors
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepo: GoalRepository,
) : ViewModel() {

    val filter = MutableStateFlow("active")
    val startDate = MutableStateFlow<String?>(Dates.addDays(Dates.todayKey(), -7))
    val endDate = MutableStateFlow<String?>(Dates.todayKey())

    val goals = goalRepo.goals(listOf("active", "completed", "paused", "archived"))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val busy = MutableStateFlow(false)

    fun create(goal: Goal, onDone: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { goalRepo.createGoal(goal) }
            busy.value = false
            onDone()
        }
    }
}

private val PRIORITY_ICON = mapOf(
    "high" to Icons.Default.KeyboardDoubleArrowUp,
    "medium" to Icons.Default.KeyboardOptionKey,
    "low" to Icons.Default.Circle
)
private val PRIORITY_COLOR = mapOf(
    "high" to Color(0xFFD45B50),
    "medium" to Color(0xFFE8B34B),
    "low" to Color(0xFF5FB4A2)
)

@Composable
fun GoalsScreen(nav: NavController) {
    // Legacy wrapper
    GoalsContent(nav)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsContent(nav: NavController, modifier: Modifier = Modifier, vm: GoalsViewModel = hiltViewModel()) {
    val allGoals by vm.goals.collectAsState()
    val filter by vm.filter.collectAsState()
    val startFilter by vm.startDate.collectAsState()
    val endFilter by vm.endDate.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var quickRange by remember { mutableStateOf("week") }

    val filtered = allGoals.filter { g ->
        if (g.status != filter) return@filter false
        val s = startFilter
        val e = endFilter
        if (s != null && e != null) {
            val createdKey = Dates.fromMillis(g.createdAt)
            if (createdKey < s || createdKey > e) return@filter false
        }
        true
    }

    if (showFilterDialog) {
        mobile.dairy.app.ui.components.GlobalFilterDialog(
            initialStartDate = startFilter,
            initialEndDate = endFilter,
            initialQuickRange = quickRange,
            onDismiss = { showFilterDialog = false },
            onApply = { range, start, end ->
                quickRange = range
                showFilterDialog = false
                val effectiveStart = when(range) {
                    "today" -> Dates.todayKey()
                    "week" -> Dates.addDays(Dates.todayKey(), -7)
                    "month" -> Dates.addDays(Dates.todayKey(), -30)
                    "custom" -> start
                    else -> null
                }
                val effectiveEnd = if (range == "custom") end else Dates.todayKey()
                vm.startDate.value = effectiveStart
                vm.endDate.value = effectiveEnd
            }
        ) {
            mobile.dairy.app.ui.components.WrapChips {
                androidx.compose.material3.FilterChip(
                    selected = filter == "active",
                    onClick = { vm.filter.value = "active" },
                    label = { Text("Active") }
                )
                androidx.compose.material3.FilterChip(
                    selected = filter == "completed",
                    onClick = { vm.filter.value = "completed" },
                    label = { Text("Done") }
                )
                androidx.compose.material3.FilterChip(
                    selected = filter == "paused",
                    onClick = { vm.filter.value = "paused" },
                    label = { Text("Paused") }
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Goals", style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { nav.navigate(Routes.NEW_GOAL) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ New", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        if (quickRange == "custom" && startFilter != null) {
            Text("$startFilter to $endFilter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Default.FilterCenterFocus, 
                if (filter == "active") "No active goals" else "No goals here",
                "A good goal is small enough to start this week and meaningful enough to keep going."
            )
        } else {
            filtered.forEach { goal ->
                BloomCard(
                    modifier = Modifier.padding(bottom = 16.dp),
                    onClick = { nav.navigate(Routes.goal(goal.id)) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                            Icon(
                                PRIORITY_ICON[goal.priority] ?: Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).padding(top = 2.dp),
                                tint = PRIORITY_COLOR[goal.priority] ?: MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    Box(
                                        Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (goal.type == "short") "Short-Term" else "Long-Term",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(4.dp))
                                val deadlineText = goal.deadline?.let { deadlineKey ->
                                    try {
                                        val diff = Dates.daysBetween(Dates.todayKey(), deadlineKey)
                                        when {
                                            diff > 0 -> " \u00B7 $diff days left"
                                            diff == 0L -> " \u00B7 due today"
                                            else -> " \u00B7 ${-diff} days overdue"
                                        }
                                    } catch (e: Exception) {
                                        " \u00B7 due $deadlineKey"
                                    }
                                } ?: ""
                                val isOverdue = goal.deadline?.let { Dates.daysBetween(Dates.todayKey(), it) } ?: 0L < 0L
                                Text(
                                    "${goal.priority.replaceFirstChar { it.uppercase() }} Priority" + deadlineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (goal.milestones.isNotEmpty() || goal.dailyTasks.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${goal.milestones.count { it.done }}/${goal.milestones.size} milestones · ${goal.dailyTasks.count { it.done }}/${goal.dailyTasks.size} daily tasks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.height(16.dp))
                                GoalProgressBar(goal.progress, color = if (goal.status == "completed") BloomColors.success() else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("${goal.progress}% complete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGoalScreen(nav: NavController, vm: GoalsViewModel = hiltViewModel()) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("short") }
    var priority by remember { mutableStateOf("medium") }
    var deadline by remember { mutableStateOf<String?>(null) }
    
    var showDeadlinePicker by remember { mutableStateOf(false) }

    var newMilestoneTitle by remember { mutableStateOf("") }
    var newTaskTitle by remember { mutableStateOf("") }

    val milestones = remember { mutableStateListOf<Milestone>() }
    val dailyTasks = remember { mutableStateListOf<GoalTask>() }

    val busy by vm.busy.collectAsState()

    if (showDeadlinePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                Button(onClick = {
                    val ms = dateState.selectedDateMillis
                    if (ms != null) {
                        deadline = Dates.fromMillis(ms)
                    }
                    showDeadlinePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Goal", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            BloomCard {
                Text("Goal Overview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Goal Title (e.g. Read 12 books)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            BloomCard {
                Text("Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                
                Text("Timeframe", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("short" to "Short-term", "long" to "Long-term").forEach { (t, label) ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Priority Level", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Low", "medium" to "Medium", "high" to "High").forEach { (p, label) ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(label) })
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Deadline", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeadlinePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            deadline?.let { "Due on $it" } ?: "Set a deadline (Optional)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BloomCard {
                Text("Milestones", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("Break your goal down into smaller, achievable steps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    placeholder = { Text("Add a milestone...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newMilestoneTitle.isNotBlank()) {
                                    milestones.add(Milestone(id = newId(), title = newMilestoneTitle.trim()))
                                    newMilestoneTitle = ""
                                }
                            },
                            enabled = newMilestoneTitle.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                )

                if (milestones.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    mobile.dairy.app.ui.components.WrapChips {
                        milestones.forEachIndexed { i, m ->
                            Box(
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .clickable { milestones.removeAt(i) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(m.title, style = MaterialTheme.typography.labelMedium)
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BloomCard {
                Text("Daily Tasks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("What will you do every day to reach this goal?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    placeholder = { Text("Add a daily task...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newTaskTitle.isNotBlank()) {
                                    dailyTasks.add(GoalTask(id = newId(), title = newTaskTitle.trim()))
                                    newTaskTitle = ""
                                }
                            },
                            enabled = newTaskTitle.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                )

                if (dailyTasks.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dailyTasks.forEachIndexed { i, t ->
                            Row(
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { dailyTasks.removeAt(i) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        vm.create(
                            Goal(
                                title = title,
                                description = description.ifBlank { null },
                                type = type,
                                priority = priority,
                                deadline = deadline,
                                milestones = milestones.toList(),
                                dailyTasks = dailyTasks.toList()
                            )
                        ) {
                            nav.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !busy && title.isNotBlank()
            ) {
                if (busy) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Save Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepo: GoalRepository,
) : ViewModel() {
    val goalId = MutableStateFlow<String?>(null)
    val goal = MutableStateFlow<Goal?>(null)
    val updates = MutableStateFlow<List<GoalUpdate>>(emptyList())
    val busy = MutableStateFlow(false)

    fun load(id: String) {
        goalId.value = id
        viewModelScope.launch {
            goalRepo.goal(id).collect { goal.value = it }
        }
        viewModelScope.launch {
            goalRepo.updatesFor(id).collect { updates.value = it }
        }
    }

    fun logUpdate(g: Goal, u: GoalUpdate) {
        viewModelScope.launch {
            busy.value = true
            goalRepo.addUpdate(g, u)
            busy.value = false
        }
    }

    fun editUpdate(g: Goal, u: GoalUpdate) {
        viewModelScope.launch {
            busy.value = true
            goalRepo.editUpdate(g, u)
            busy.value = false
        }
    }

    fun deleteUpdate(g: Goal, u: GoalUpdate) {
        viewModelScope.launch {
            busy.value = true
            goalRepo.deleteUpdate(g, u)
            busy.value = false
        }
    }

    fun toggleMilestone(g: Goal, milestoneId: String) {
        val updated = g.milestones.map { m ->
            if (m.id == milestoneId) m.copy(done = !m.done) else m
        }
        viewModelScope.launch {
            goalRepo.updateMilestones(g.id, updated)
        }
    }

    fun addMilestone(g: Goal, title: String) {
        val updated = g.milestones + Milestone(id = newId(), title = title)
        viewModelScope.launch {
            goalRepo.updateMilestones(g.id, updated)
        }
    }

    fun toggleDailyTask(g: Goal, taskId: String) {
        val updated = g.dailyTasks.map { t ->
            if (t.id == taskId) t.copy(done = !t.done) else t
        }
        viewModelScope.launch {
            goalRepo.updateDailyTasks(g.id, updated)
        }
    }

    fun addDailyTask(g: Goal, title: String) {
        val updated = g.dailyTasks + GoalTask(id = newId(), title = title)
        viewModelScope.launch {
            goalRepo.updateDailyTasks(g.id, updated)
        }
    }

    fun editMilestone(g: Goal, milestoneId: String, newTitle: String) {
        val updated = g.milestones.map { m ->
            if (m.id == milestoneId) m.copy(title = newTitle) else m
        }
        viewModelScope.launch {
            goalRepo.updateMilestones(g.id, updated)
        }
    }

    fun removeMilestone(g: Goal, milestoneId: String) {
        val updated = g.milestones.filter { it.id != milestoneId }
        viewModelScope.launch {
            goalRepo.updateMilestones(g.id, updated)
        }
    }

    fun editDailyTask(g: Goal, taskId: String, newTitle: String) {
        val updated = g.dailyTasks.map { t ->
            if (t.id == taskId) t.copy(title = newTitle) else t
        }
        viewModelScope.launch {
            goalRepo.updateDailyTasks(g.id, updated)
        }
    }

    fun removeDailyTask(g: Goal, taskId: String) {
        val updated = g.dailyTasks.filter { it.id != taskId }
        viewModelScope.launch {
            goalRepo.updateDailyTasks(g.id, updated)
        }
    }

    fun updateGoal(g: Goal, newTitle: String, newStatus: String, newProgress: Int) {
        viewModelScope.launch {
            busy.value = true
            goalRepo.patchGoal(g.id, mapOf(
                "title" to newTitle,
                "status" to newStatus,
                "progress" to newProgress
            ))
            busy.value = false
        }
    }

    fun deleteGoal(g: Goal, onDeleted: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            goalRepo.deleteGoal(g.id)
            busy.value = false
            onDeleted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(nav: NavController, id: String, vm: GoalDetailViewModel = hiltViewModel()) {
    val goal by vm.goal.collectAsState()
    val updates by vm.updates.collectAsState()
    val busy by vm.busy.collectAsState()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showAddDailyTaskDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    var showManageMilestones by remember { mutableStateOf(false) }
    var showManageDailyTasks by remember { mutableStateOf(false) }

    var updateToEdit by remember { mutableStateOf<GoalUpdate?>(null) }

    var editingMilestone by remember { mutableStateOf<Milestone?>(null) }
    var editingDailyTask by remember { mutableStateOf<GoalTask?>(null) }

    var quickRange by remember { mutableStateOf("week") }
    var startFilter by remember { mutableStateOf<String?>(Dates.addDays(Dates.todayKey(), -7)) }
    var endFilter by remember { mutableStateOf<String?>(Dates.todayKey()) }

    LaunchedEffect(id) { vm.load(id) }

    if (goal == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val g = goal!!

    val filteredUpdates = updates.filter { u ->
        if (startFilter != null && endFilter != null) {
            val d = u.date
            if (d < startFilter!! || d > endFilter!!) return@filter false
        }
        true
    }

    if (showFilterDialog) {
        mobile.dairy.app.ui.components.GlobalFilterDialog(
            initialStartDate = startFilter,
            initialEndDate = endFilter,
            initialQuickRange = quickRange,
            onDismiss = { showFilterDialog = false },
            onApply = { range, start, end ->
                quickRange = range
                showFilterDialog = false
                val effectiveStart = when(range) {
                    "today" -> Dates.todayKey()
                    "week" -> Dates.addDays(Dates.todayKey(), -7)
                    "month" -> Dates.addDays(Dates.todayKey(), -30)
                    "custom" -> start
                    else -> null
                }
                val effectiveEnd = if (range == "custom") end else Dates.todayKey()
                startFilter = effectiveStart
                endFilter = effectiveEnd
            }
        )
    }

    if (showEditDialog) {
        GoalEditDialog(
            goal = g,
            onDismiss = { showEditDialog = false },
            onSave = { title, status, progress ->
                vm.updateGoal(g, title, status, progress)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to permanently delete this goal?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteGoal(g) { nav.popBackStack() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showUpdateDialog) {
        GoalUpdateDialog(
            goal = g,
            initialUpdate = null,
            onDismiss = { showUpdateDialog = false },
            onSave = { update ->
                vm.logUpdate(g, update)
                showUpdateDialog = false
            }
        )
    }

    if (updateToEdit != null) {
        GoalUpdateDialog(
            goal = g,
            initialUpdate = updateToEdit,
            onDismiss = { updateToEdit = null },
            onSave = { updated ->
                vm.editUpdate(g, updated)
                updateToEdit = null
            },
            onDelete = { update ->
                vm.deleteUpdate(g, update)
                updateToEdit = null
            }
        )
    }

    if (showAddMilestoneDialog) {
        var milestoneTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Add Milestone", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = milestoneTitle,
                    onValueChange = { milestoneTitle = it },
                    placeholder = { Text("e.g. Read first 5 chapters") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (milestoneTitle.isNotBlank()) {
                            vm.addMilestone(g, milestoneTitle.trim())
                            showAddMilestoneDialog = false
                        }
                    },
                    enabled = milestoneTitle.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddDailyTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDailyTaskDialog = false },
            title = { Text("Add Daily Task", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    placeholder = { Text("e.g. Read 15 minutes daily") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            vm.addDailyTask(g, taskTitle.trim())
                            showAddDailyTaskDialog = false
                        }
                    },
                    enabled = taskTitle.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDailyTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editingMilestone != null) {
        var editTitle by remember { mutableStateOf(editingMilestone!!.title) }
        AlertDialog(
            onDismissRequest = { editingMilestone = null },
            title = { Text("Edit Milestone", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editTitle.isNotBlank()) {
                        vm.editMilestone(g, editingMilestone!!.id, editTitle.trim())
                        editingMilestone = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { 
                        vm.removeMilestone(g, editingMilestone!!.id)
                        editingMilestone = null 
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editingMilestone = null }) { Text("Cancel") }
                }
            }
        )
    }

    if (editingDailyTask != null) {
        var editTitle by remember { mutableStateOf(editingDailyTask!!.title) }
        AlertDialog(
            onDismissRequest = { editingDailyTask = null },
            title = { Text("Edit Daily Task", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editTitle.isNotBlank()) {
                        vm.editDailyTask(g, editingDailyTask!!.id, editTitle.trim())
                        editingDailyTask = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { 
                        vm.removeDailyTask(g, editingDailyTask!!.id)
                        editingDailyTask = null 
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editingDailyTask = null }) { Text("Cancel") }
                }
            }
        )
    }

    if (showManageMilestones) {
        ModalBottomSheet(onDismissRequest = { showManageMilestones = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Manage Milestones", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddMilestoneDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (g.milestones.isEmpty()) {
                    Text("No milestones added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    g.milestones.forEach { m ->
                        BloomCard(modifier = Modifier.padding(vertical = 4.dp).clickable { editingMilestone = m }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.toggleMilestone(g, m.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (m.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (m.done) BloomColors.success() else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(m.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManageDailyTasks) {
        ModalBottomSheet(onDismissRequest = { showManageDailyTasks = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Manage Daily Tasks", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddDailyTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (g.dailyTasks.isEmpty()) {
                    Text("No daily tasks added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    g.dailyTasks.forEach { t ->
                        BloomCard(modifier = Modifier.padding(vertical = 4.dp).clickable { editingDailyTask = t }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Circle, contentDescription = null, modifier = Modifier.size(8.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Text(t.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                val compCount = updates.count { u ->
                                    u.achievement?.startsWith("Completed tasks:") == true &&
                                    u.achievement!!.removePrefix("Completed tasks:").trim().split(", ").map { it.trim() }.contains(t.title)
                                }
                                if (compCount > 0) {
                                    Text("$compCount done", style = MaterialTheme.typography.labelSmall, color = BloomColors.success(), modifier = Modifier.padding(end = 8.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Goal Details", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.headlineSmall)
                    g.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (g.type == "short") "Short-Term" else "Long-Term",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    PRIORITY_ICON[g.priority] ?: Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PRIORITY_COLOR[g.priority] ?: MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${g.priority.replaceFirstChar { it.uppercase() }} priority",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                g.deadline?.let {
                    Text(" · Due $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BloomCard(modifier = Modifier.weight(1f).clickable { showManageMilestones = true }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            val completedMilestones = g.milestones.count { it.done }
                            Text("Milestones", style = MaterialTheme.typography.titleSmall)
                            Text("$completedMilestones/${g.milestones.size} completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Manage", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                BloomCard(modifier = Modifier.weight(1f).clickable { showManageDailyTasks = true }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Daily Tasks", style = MaterialTheme.typography.titleSmall)
                            Text("${g.dailyTasks.size} tasks to track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Manage", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Overall Progress")
            var localProgress by remember(g.progress) { mutableFloatStateOf(g.progress.toFloat()) }
            BloomCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${localProgress.toInt()}% completed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Status: ${g.status.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = localProgress,
                    onValueChange = { localProgress = it },
                    onValueChangeFinished = {
                        vm.updateGoal(g, g.title, g.status, localProgress.toInt())
                    },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tip: Drag the slider to quickly update your progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showUpdateDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log Progress / Update Goal", fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Recent Updates")
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter Updates")
                }
            }
            
            
            if (quickRange == "custom" && startFilter != null) {
                Text("$startFilter to $endFilter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }

            if (filteredUpdates.isNotEmpty()) {
                filteredUpdates.forEach { u ->
                    BloomCard(modifier = Modifier.padding(bottom = 12.dp).clickable { updateToEdit = u }) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    val dateText = if (u.createdAt > 0L) Dates.formatDateTime(u.createdAt) else Dates.friendly(u.date)
                                    Text(dateText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("${u.progress}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Edit Log", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            if (u.minutes > 0 || u.effort != null || u.focus != null) {
                                mobile.dairy.app.ui.components.WrapChips {
                                    if (u.minutes > 0) {
                                        Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("⏱️ ${u.minutes}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    u.effort?.let { 
                                        Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("💪 $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    u.focus?.let { 
                                        Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("🎯 $it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            val ach = u.achievement
                            if (g.dailyTasks.isNotEmpty()) {
                                val completedNames = if (ach != null && ach.startsWith("Completed tasks:")) {
                                    ach.removePrefix("Completed tasks:").trim().split(", ").map { it.trim() }
                                } else emptyList()
                                
                                g.dailyTasks.forEach { t ->
                                    val done = completedNames.contains(t.title)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            if (done) "✓" else "✗", 
                                            color = if (done) BloomColors.success() else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Text(
                                            t.title, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else if (!ach.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("✨", modifier = Modifier.padding(end = 4.dp))
                                    Text(ach, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            u.obstacles?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("🚧", modifier = Modifier.padding(end = 4.dp))
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            u.lesson?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("💡", modifier = Modifier.padding(end = 4.dp))
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyState(Icons.Default.TrendingUp, "No updates logged yet", "Use the 'Log Progress' button to record your time, focus, and progress.")
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalUpdateDialog(
    goal: Goal,
    initialUpdate: GoalUpdate? = null,
    onDismiss: () -> Unit,
    onSave: (GoalUpdate) -> Unit,
    onDelete: ((GoalUpdate) -> Unit)? = null
) {
    var step by remember { mutableStateOf(1) }
    
    // Step 1: Time
    var minutesText by remember { mutableStateOf(initialUpdate?.minutes?.toString()?.takeIf { it != "0" } ?: "") }
    
    // Step 2: Ratings
    var effort by remember { mutableStateOf<Int?>(initialUpdate?.effort ?: 5) }
    var focus by remember { mutableStateOf<Int?>(initialUpdate?.focus ?: 5) }
    
    // Step 3: Reflection
    val initialTaskIds = remember(initialUpdate) {
        if (initialUpdate != null && initialUpdate.achievement?.startsWith("Completed tasks:") == true) {
            val names = initialUpdate.achievement!!.removePrefix("Completed tasks:").trim().split(", ").map { it.trim() }
            goal.dailyTasks.filter { names.contains(it.title) }.map { it.id }
        } else emptyList()
    }
    val completedTaskIds = remember { mutableStateListOf<String>().apply { addAll(initialTaskIds) } }
    var notes by remember { mutableStateOf(initialUpdate?.obstacles ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (initialUpdate != null) "Edit Log Entry" else when (step) {
                        1 -> "Time Spent"
                        2 -> "Daily Ratings"
                        else -> "Reflection Log"
                    }, 
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Step $step of 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))

            when (step) {
                1 -> {
                    Text("How much time did you spend on this goal today?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("Minutes (e.g. 45)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                2 -> {
                    RatingScale("Effort Spent", Icons.Default.Speed, effort) { effort = it }
                    Spacer(Modifier.height(16.dp))
                    RatingScale("Focus Level", Icons.Default.FilterCenterFocus, focus) { focus = it }
                }
                3 -> {
                    if (goal.dailyTasks.isNotEmpty()) {
                        Text("Daily Tasks", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Check off what you did today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        
                        goal.dailyTasks.forEach { t ->
                            val isChecked = completedTaskIds.contains(t.id)
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    if (isChecked) completedTaskIds.remove(t.id) else completedTaskIds.add(t.id)
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) completedTaskIds.add(t.id) else completedTaskIds.remove(t.id)
                                    }
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(t.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Text("Notes / Obstacles (Optional)", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Any thoughts or blockers?") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 1) {
                    OutlinedButton(onClick = { step-- }, shape = RoundedCornerShape(12.dp)) {
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                
                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            val completedTaskNames = goal.dailyTasks
                                .filter { completedTaskIds.contains(it.id) }
                                .map { it.title }
                            
                            val achievementText = if (completedTaskNames.isNotEmpty()) {
                                "Completed tasks: " + completedTaskNames.joinToString(", ")
                            } else {
                                null
                            }

                            val update = (initialUpdate ?: GoalUpdate()).copy(
                                date = initialUpdate?.date ?: Dates.todayKey(),
                                progress = initialUpdate?.progress ?: goal.progress,
                                status = initialUpdate?.status ?: goal.status,
                                minutes = minutesText.toIntOrNull() ?: 0,
                                effort = effort,
                                focus = focus,
                                achievement = achievementText,
                                obstacles = notes.ifBlank { null },
                                lesson = initialUpdate?.lesson,
                                createdAt = initialUpdate?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(update)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (step < 3) "Next" else "Save Log", fontWeight = FontWeight.Bold)
                }
            }
            
            if (initialUpdate != null && onDelete != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onDelete(initialUpdate) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Log Entry", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (title: String, status: String, progress: Int) -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var status by remember { mutableStateOf(goal.status) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Goal", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Goal Title")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Status")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("active" to "Active", "paused" to "Paused", "completed" to "Completed").forEach { (st, label) ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSave(title, status, goal.progress) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
