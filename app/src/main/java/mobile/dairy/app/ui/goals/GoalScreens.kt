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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardOptionKey
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
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
    val startDate = MutableStateFlow<String?>(null)
    val endDate = MutableStateFlow<String?>(null)

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

    var showDateFilterDialog by remember { mutableStateOf(false) }

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

    if (showDateFilterDialog) {
        BloomTwoDatePickerDialog(
            initialStartDate = startFilter,
            initialEndDate = endFilter,
            onDismiss = { showDateFilterDialog = false },
            onRangeSelected = { s, e ->
                vm.startDate.value = s
                vm.endDate.value = e
                showDateFilterDialog = false
            }
        )
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
            Button(
                onClick = { nav.navigate(Routes.NEW_GOAL) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ New", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("active" to "Active", "completed" to "Done", "paused" to "Paused").forEach { (f, label) ->
                val sel = filter == f
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { vm.filter.value = f },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Separate Date Range Filter Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDateFilterDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (startFilter != null && endFilter != null) "$startFilter to $endFilter"
                    else "Filter by Date",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (startFilter != null && endFilter != null) {
                TextButton(onClick = {
                    vm.startDate.value = null
                    vm.endDate.value = null
                }) {
                    Text("Clear Filter", style = MaterialTheme.typography.labelSmall)
                }
            }
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
                    Row(verticalAlignment = Alignment.Top) {
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
                            Text(
                                "${goal.priority.replaceFirstChar { it.uppercase() }} Priority" +
                                    (goal.deadline?.let { " \u00B7 due $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showAddDailyTaskDialog by remember { mutableStateOf(false) }

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

    if (showAddMilestoneDialog) {
        var milestoneTitleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Add Milestone", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = milestoneTitleInput,
                    onValueChange = { milestoneTitleInput = it },
                    placeholder = { Text("e.g. Complete chapter 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (milestoneTitleInput.isNotBlank()) {
                            milestones.add(Milestone(id = newId(), title = milestoneTitleInput.trim()))
                            showAddMilestoneDialog = false
                        }
                    },
                    enabled = milestoneTitleInput.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddDailyTaskDialog) {
        var taskTitleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDailyTaskDialog = false },
            title = { Text("Add Daily Task", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = taskTitleInput,
                    onValueChange = { taskTitleInput = it },
                    placeholder = { Text("e.g. Read 20 mins daily") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitleInput.isNotBlank()) {
                            dailyTasks.add(GoalTask(id = newId(), title = taskTitleInput.trim()))
                            showAddDailyTaskDialog = false
                        }
                    },
                    enabled = taskTitleInput.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDailyTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .imePadding()
            .statusBarsPadding(),
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
                .padding(24.dp)
        ) {
            Text("Goal Title", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Read 12 books this year") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text("Description (Optional)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Why is this goal important to you?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            SectionTitle("Timeframe & Category")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("short" to "Short-term", "long" to "Long-term").forEach { (t, label) ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                }
            }

            SectionTitle("Priority Level")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low" to "Low", "medium" to "Medium", "high" to "High").forEach { (p, label) ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(label) })
                }
            }

            SectionTitle("Deadline")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeadlinePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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

            // Milestones Header with + Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Milestones", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddMilestoneDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Milestone", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (milestones.isEmpty()) {
                Text("No milestones added yet. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                milestones.forEachIndexed { i, m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${m.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { milestones.removeAt(i) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Daily Tasks Header with + Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Tasks", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddDailyTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Daily Task", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (dailyTasks.isEmpty()) {
                Text("No daily tasks added yet. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dailyTasks.forEachIndexed { i, t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓ ${t.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { dailyTasks.removeAt(i) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
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
                else Text("Create goal", fontWeight = FontWeight.Bold)
            }
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

    LaunchedEffect(id) { vm.load(id) }

    if (goal == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val g = goal!!

    if (showUpdateDialog) {
        GoalUpdateDialog(
            goal = g,
            onDismiss = { showUpdateDialog = false },
            onSave = { update ->
                vm.logUpdate(g, update)
                showUpdateDialog = false
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
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) {
                    Text("Cancel")
                }
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
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDailyTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Goal Details", style = MaterialTheme.typography.titleMedium) },
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
            
            Spacer(Modifier.height(28.dp))

            // Update Progress CTA
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
            SectionTitle("Overall Progress")
            BloomCard {
                GoalProgressBar(g.progress)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${g.progress}% completed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Status: ${g.status.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Milestones Section with + Icon Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Milestones", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddMilestoneDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Milestone", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (g.milestones.isEmpty()) {
                Text(
                    "No milestones added yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                g.milestones.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.toggleMilestone(g, m.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (m.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (m.done) BloomColors.success() else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            m.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Tasks Section with + Icon Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Tasks", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddDailyTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Daily Task", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (g.dailyTasks.isEmpty()) {
                Text(
                    "No daily tasks added yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                g.dailyTasks.forEach { t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.toggleDailyTask(g, t.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (t.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (t.done) BloomColors.success() else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            t.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionTitle("Recent Updates & Reflection Log")
            if (updates.isNotEmpty()) {
                updates.forEach { u ->
                    BloomCard(modifier = Modifier.padding(bottom = 12.dp)) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(Dates.friendly(u.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${u.progress}% progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            
                            if (u.minutes > 0 || u.effort != null || u.focus != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (u.minutes > 0) Text("⏱️ ${u.minutes} mins", style = MaterialTheme.typography.bodySmall)
                                    u.effort?.let { Text("💪 Effort: $it/10", style = MaterialTheme.typography.bodySmall) }
                                    u.focus?.let { Text("🎯 Focus: $it/10", style = MaterialTheme.typography.bodySmall) }
                                }
                            }

                            u.achievement?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(6.dp))
                                Text("✨ Achievement: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            u.obstacles?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("🚧 Obstacles: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            u.lesson?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("💡 Action / Lesson: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
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
    onDismiss: () -> Unit,
    onSave: (GoalUpdate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var progress by remember { mutableFloatStateOf(goal.progress.toFloat()) }
    var status by remember { mutableStateOf(goal.status) }
    var minutesText by remember { mutableStateOf("") }
    var effort by remember { mutableStateOf<Int?>(5) }
    var focus by remember { mutableStateOf<Int?>(5) }
    var achievements by remember { mutableStateOf("") }
    var obstacles by remember { mutableStateOf("") }
    var lesson by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Update Goal Progress", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(goal.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            // Progress Slider
            Text("Progress: ${progress.toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Slider(
                value = progress,
                onValueChange = { progress = it },
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Status Option")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("active" to "Active", "paused" to "Paused", "completed" to "Completed").forEach { (st, label) ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(label) })
                }
            }

            SectionTitle("Time Spent Today (Minutes)")
            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it },
                placeholder = { Text("e.g. 45") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            SectionTitle("Daily Ratings (1 to 10)")
            RatingScale("Effort Spent", Icons.Default.Speed, effort) { effort = it }
            RatingScale("Focus Level", Icons.Default.FilterCenterFocus, focus) { focus = it }

            SectionTitle("Conversational Check-in")
            Text("What progress or achievements did you make today?", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = achievements,
                onValueChange = { achievements = it },
                placeholder = { Text("Completed chapter 3, solved main problem...") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))
            Text("What stopped you from focusing today? (Obstacles)", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = obstacles,
                onValueChange = { obstacles = it },
                placeholder = { Text("Distractions, unexpected calls...") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))
            Text("What is one small action you can take tomorrow?", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = lesson,
                onValueChange = { lesson = it },
                placeholder = { Text("Start 10 minutes earlier, turn off notifications...") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    val update = GoalUpdate(
                        date = Dates.todayKey(),
                        progress = progress.toInt(),
                        status = status,
                        minutes = minutesText.toIntOrNull() ?: 0,
                        effort = effort,
                        focus = focus,
                        achievement = achievements.ifBlank { null },
                        obstacles = obstacles.ifBlank { null },
                        lesson = lesson.ifBlank { null },
                    )
                    onSave(update)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Goal Update", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
