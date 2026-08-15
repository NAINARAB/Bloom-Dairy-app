package mobile.dairy.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Format
import mobile.dairy.app.data.FinanceRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Finance
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.ui.components.*
import mobile.dairy.app.ui.theme.BloomColors
import javax.inject.Inject

data class MoneyState(
    val prefs: AppPrefs = AppPrefs(),
    val expenses: List<Expense> = emptyList(),
    val savings: List<Saving> = emptyList(),
    val monthlySpent: Double = 0.0,
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val financeRepo: FinanceRepository,
    private val prefsRepo: PrefsRepository,
) : ViewModel() {

    private val dateRange = MutableStateFlow<Pair<String?, String?>>(Dates.addDays(Dates.todayKey(), -7) to Dates.todayKey())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = combine(
        prefsRepo.appPrefs(),
        dateRange.flatMapLatest { (start, end) -> financeRepo.expenses(start, end) },
        dateRange.flatMapLatest { (start, end) -> financeRepo.savings(start, end) },
        financeRepo.expenses(Dates.startOfMonth(Dates.todayKey()), Dates.todayKey())
    ) { p, e, s, mE ->
        MoneyState(p, e, s, mE.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyState())

    fun updateFilter(start: String?, end: String?) {
        dateRange.value = start to end
    }

    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun addExpense(amount: Double, category: String, necessity: String, note: String?) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching {
                financeRepo.addExpense(Expense(date = Dates.todayKey(), amount = amount,
                    category = category, necessity = necessity, note = note))
            }.onFailure {
                error.value = "Failed to save: ${it.message}"
            }
            busy.value = false
        }
    }

    fun addSaving(amount: Double, kind: String, note: String?, category: String) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { 
                financeRepo.addSaving(Saving(date = Dates.todayKey(), amount = amount, kind = kind, note = note, category = category)) 
            }.onFailure {
                error.value = "Failed to save: ${it.message}"
            }
            busy.value = false
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { financeRepo.updateExpense(expense) }
                .onFailure { error.value = "Failed to update: ${it.message}" }
            busy.value = false
        }
    }

    fun updateSaving(saving: Saving) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { financeRepo.updateSaving(saving) }
                .onFailure { error.value = "Failed to update: ${it.message}" }
            busy.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch { runCatching { financeRepo.deleteExpense(id) } }
    }

    fun deleteSaving(id: String) {
        viewModelScope.launch { runCatching { financeRepo.deleteSaving(id) } }
    }
    
    fun updateMonthlyBudget(newBudget: Double) {
        viewModelScope.launch {
            val p = prefsRepo.getAppPrefs()
            prefsRepo.updateAppPrefs(p.copy(monthlyBudget = newBudget))
        }
    }
}

@Composable
fun MoneyScreen(nav: NavController) {
    MoneyContent(nav)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyContent(nav: NavController, modifier: Modifier = Modifier, vm: MoneyViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var editExpense by remember { mutableStateOf<Expense?>(null) }
    var editSaving by remember { mutableStateOf<Saving?>(null) }
    var deleteConfirmExpense by remember { mutableStateOf<Expense?>(null) }
    var deleteConfirmSaving by remember { mutableStateOf<Saving?>(null) }
    
    // Filters
    var quickRange by remember { mutableStateOf("month") }
    var startDate by remember { mutableStateOf<String?>(Dates.startOfMonth(Dates.todayKey())) }
    var endDate by remember { mutableStateOf<String?>(Dates.todayKey()) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf<String?>(null) } // "expense", "saving"
    var sortAmount by remember { mutableStateOf<String?>(null) } // "highest", "lowest"

    // Filter logic (Note: date range filtering is now done at the database level)
    val filteredExpenses = s.expenses.filter { e ->
        if (filterType == "saving") return@filter false
        if (filterCategory != null && e.category != filterCategory) return@filter false
        true
    }
    
    val filteredSavings = s.savings.filter { sv ->
        if (filterType == "expense") return@filter false
        true
    }
    
    // Merge and sort
    val combined = (filteredExpenses.map { it as Any } + filteredSavings.map { it as Any }).sortedByDescending { 
        when (it) {
            is Expense -> it.date
            is Saving -> it.date
            else -> ""
        }
    }
    
    val sortedCombined = when (sortAmount) {
        "highest" -> combined.sortedByDescending { if (it is Expense) it.amount else (it as Saving).amount }
        "lowest" -> combined.sortedBy { if (it is Expense) it.amount else (it as Saving).amount }
        else -> combined
    }

    if (showFilterDialog) {
        GlobalFilterDialog(
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
            WrapChips {
                FilterChip(selected = filterType == "expense", onClick = { filterType = if (filterType == "expense") null else "expense" }, label = { Text("Expenses") })
                FilterChip(selected = filterType == "saving", onClick = { filterType = if (filterType == "saving") null else "saving" }, label = { Text("Earned/Avoided") })
            }

            if (filterType == "expense") {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.prefs.transactionCategories.size) { i ->
                        val cat = s.prefs.transactionCategories[i]
                        FilterChip(selected = filterCategory == cat.key, onClick = { filterCategory = if (filterCategory == cat.key) null else cat.key }, label = { Text(cat.label) })
                    }
                }
            }
            
            WrapChips {
                FilterChip(selected = sortAmount == "highest", onClick = { sortAmount = if (sortAmount == "highest") null else "highest" }, label = { Text("Highest Amount") })
                FilterChip(selected = sortAmount == "lowest", onClick = { sortAmount = if (sortAmount == "lowest") null else "lowest" }, label = { Text("Lowest Amount") })
            }
        }
    }

    if (showAddSheet || editExpense != null || editSaving != null) {
        AddTransactionSheet(
            prefs = s.prefs, 
            vm = vm, 
            initialExpense = editExpense,
            initialSaving = editSaving,
            onDismiss = { 
                showAddSheet = false
                editExpense = null
                editSaving = null
            },
            onDelete = {
                if (editExpense != null) {
                    deleteConfirmExpense = editExpense
                } else if (editSaving != null) {
                    deleteConfirmSaving = editSaving
                }
                showAddSheet = false
                editExpense = null
                editSaving = null
            }
        )
    }

    if (deleteConfirmExpense != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmExpense = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExpense(deleteConfirmExpense!!.id)
                    deleteConfirmExpense = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmExpense = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteConfirmSaving != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmSaving = null },
            title = { Text("Delete Saving") },
            text = { Text("Are you sure you want to delete this saving?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSaving(deleteConfirmSaving!!.id)
                    deleteConfirmSaving = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmSaving = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        val today = Dates.todayKey()
        val todayMoney = Finance.summarizeDay(today, s.expenses, s.savings)

        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Money", style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ New", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        if (quickRange == "custom" && startDate != null) {
            Text("$startDate to $endDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        var showBudgetDialog by remember { mutableStateOf(false) }

        BloomCard(modifier = Modifier.clickable { showBudgetDialog = true }) {
            Text("MONTHLY BUDGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Spent ${Format.money(s.monthlySpent, s.prefs.currency)} of ${Format.money(s.prefs.monthlyBudget, s.prefs.currency)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "${((s.monthlySpent / s.prefs.monthlyBudget.coerceAtLeast(1.0)) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            GoalProgressBar(((s.monthlySpent / s.prefs.monthlyBudget.coerceAtLeast(1.0)) * 100).toInt().coerceIn(0, 100))
        }

        Spacer(Modifier.height(16.dp))
        
        if (showBudgetDialog) {
            var budgetInput by remember { mutableStateOf(s.prefs.monthlyBudget.toString().removeSuffix(".0")) }
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Update Monthly Budget") },
                text = {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() || c == '.' } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text(s.prefs.currency + " ") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        budgetInput.toDoubleOrNull()?.let { vm.updateMonthlyBudget(it) }
                        showBudgetDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Quick Stats
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Spent today", Format.money(todayMoney.spent, s.prefs.currency), Modifier.weight(1f))
            StatCard("Kept today", Format.money(todayMoney.earned + todayMoney.avoided, s.prefs.currency), Modifier.weight(1f), good = true)
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Transactions")

        if (sortedCombined.isEmpty()) {
            EmptyState(Icons.Default.Delete, "No transactions", "You haven't recorded any expenses or savings for this period.")
        } else {
            sortedCombined.forEach { item ->
                when (item) {
                    is Expense -> {
                        BloomCard(modifier = Modifier.padding(bottom = 8.dp).clickable { editExpense = item }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.prefs.transactionCategories.find { it.key == item.category }?.label ?: "Other", style = MaterialTheme.typography.titleMedium)
                                    Text(Dates.friendly(item.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(Format.money(item.amount, s.prefs.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(16.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    is Saving -> {
                        val isAvoided = item.kind == "avoided"
                        BloomCard(
                            modifier = Modifier.padding(bottom = 8.dp).clickable { editSaving = item },
                            container = Color.Transparent
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (isAvoided) "Avoided Expense" else "Saved", style = MaterialTheme.typography.titleMedium, color = mobile.dairy.app.ui.theme.BloomColors.success())
                                    Text(Dates.friendly(item.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("+ " + Format.money(item.amount, s.prefs.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = mobile.dairy.app.ui.theme.BloomColors.success())
                                Spacer(Modifier.width(16.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
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
fun AddTransactionSheet(
    prefs: AppPrefs,
    vm: MoneyViewModel,
    initialExpense: Expense? = null,
    initialSaving: Saving? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var amount by remember { mutableStateOf(initialExpense?.amount?.toString()?.removeSuffix(".0") ?: initialSaving?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var note by remember { mutableStateOf(initialExpense?.note ?: initialSaving?.note ?: "") }
    var mode by remember { mutableStateOf(if (initialSaving != null) initialSaving.kind else "expense") } // expense | earned | avoided
    var category by remember { mutableStateOf(initialExpense?.category ?: initialSaving?.category ?: "other") }

    val isEditing = initialExpense != null || initialSaving != null

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
            Text(if (isEditing) "Edit Transaction" else "Add Transaction", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            // Mode Selector
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("expense" to "Spent", "avoided" to "Avoided", "earned" to "Earned").forEach { (m, label) ->
                    val sel = mode == m
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { mode = m },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            TextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00", style = MaterialTheme.typography.headlineSmall) },
                prefix = { Text(prefs.currency + " ", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(Modifier.height(16.dp))
            Eyebrow("Category")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(prefs.transactionCategories.size) { i ->
                    val cat = prefs.transactionCategories[i]
                    FilterChip(
                        selected = category == cat.key,
                        onClick = { category = cat.key },
                        label = { Text("${cat.emoji} ${cat.label}") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        if (mode == "expense") {
                            if (initialExpense != null) {
                                vm.updateExpense(initialExpense.copy(amount = amt, category = category, note = note.ifBlank { null }))
                            } else {
                                vm.addExpense(amt, category, "necessary", note.ifBlank { null })
                            }
                        } else {
                            if (initialSaving != null) {
                                vm.updateSaving(initialSaving.copy(amount = amt, category = category, kind = mode, note = note.ifBlank { null }))
                            } else {
                                vm.addSaving(amt, mode, note.ifBlank { null }, category)
                            }
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !busy && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                if (busy) androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp))
                else Text(if (isEditing) "Update entry" else "Log entry", fontWeight = FontWeight.Bold)
            }
            
            if (isEditing && onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    mobile.dairy.app.ui.components.GlobalLoadingOverlay(busy, "Saving transaction...")
}
