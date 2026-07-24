package mobile.dairy.app.ui.money

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import mobile.dairy.app.Routes
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.ExpenseCategory
import mobile.dairy.app.core.Format
import mobile.dairy.app.data.FinanceRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Finance
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.ui.components.Bar
import mobile.dairy.app.ui.components.Bars
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.StatCard
import mobile.dairy.app.ui.components.WrapChips
import mobile.dairy.app.ui.dashboard.TabScaffold
import mobile.dairy.app.ui.theme.BloomColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoneyState(
    val prefs: AppPrefs = AppPrefs(),
    val expenses: List<Expense> = emptyList(),
    val savings: List<Saving> = emptyList(),
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val financeRepo: FinanceRepository,
    prefsRepo: PrefsRepository,
) : ViewModel() {

    val state = combine(prefsRepo.appPrefs(), financeRepo.expenses(62), financeRepo.savings(62)) { p, e, s ->
        MoneyState(p, e, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyState())

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

    fun addSaving(amount: Double, kind: String, note: String?) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { 
                financeRepo.addSaving(Saving(date = Dates.todayKey(), amount = amount, kind = kind, note = note)) 
            }.onFailure {
                error.value = "Failed to save: ${it.message}"
            }
            busy.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch { runCatching { financeRepo.deleteExpense(id) } }
    }
}

@Composable
fun MoneyScreen(nav: NavController) {
    mobile.dairy.app.ui.dashboard.MainTabsScreen(nav, Routes.MONEY)
}

@Composable
fun MoneyContent(nav: NavController, modifier: Modifier = Modifier, vm: MoneyViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("expense") } // expense | saved | avoided
    var category by remember { mutableStateOf("other") }

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
        Text("Money", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(20.dp))

        // Quick Stats
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Spent today", Format.money(todayMoney.spent, s.prefs.currency), Modifier.weight(1f))
            StatCard("Kept today", Format.money(todayMoney.saved + todayMoney.avoided, s.prefs.currency), Modifier.weight(1f), good = true)
        }

        SectionTitle("Quick entry")
        BloomCard {
            // Mode Selector
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("expense" to "Spent", "saved" to "Saved", "avoided" to "Avoided").forEach { (m, label) ->
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
                prefix = { Text(s.prefs.currency + " ", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            if (mode == "expense") {
                Spacer(Modifier.height(16.dp))
                Eyebrow("Category")
                Spacer(Modifier.height(8.dp))
                WrapChips {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat.key,
                            onClick = { category = cat.key },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(cat.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
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
                        if (mode == "expense") vm.addExpense(amt, category, "necessary", note.ifBlank { null })
                        else vm.addSaving(amt, mode, note.ifBlank { null })
                        amount = ""
                        note = ""
                        mode = "expense"
                        category = "other"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !busy
            ) {
                if (busy) androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp))
                else Text("Log entry", fontWeight = FontWeight.Bold)
            }
        }

        val recent = s.expenses.take(5)
        if (recent.isNotEmpty()) {
            SectionTitle("Recent expenses")
            recent.forEach { exp ->
                BloomCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ExpenseCategory.fromKey(exp.category).label, style = MaterialTheme.typography.titleMedium)
                            Text(Dates.friendly(exp.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Format.money(exp.amount, s.prefs.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = { vm.deleteExpense(exp.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
