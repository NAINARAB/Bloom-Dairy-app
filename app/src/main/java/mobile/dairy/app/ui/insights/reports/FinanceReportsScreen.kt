package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ArrowDropDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceReportsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val allExpenses = ctx.expenses
    val allSavings = ctx.savings

    // Default to current month
    val todayDate = remember { LocalDate.now() }
    var selectedMonthKey by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var selectedMonthLabel by remember { mutableStateOf(todayDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }

    // Filter data for the selected month
    val monthExpenses = remember(allExpenses, selectedMonthKey) {
        allExpenses.filter { it.date.startsWith(selectedMonthKey) }
    }
    val monthSavings = remember(allSavings, selectedMonthKey) {
        allSavings.filter { it.date.startsWith(selectedMonthKey) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money & Savings Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoCard = !showInfoCard },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Chart Info",
                            tint = Color(0xFFA78BFA)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Period",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showMonthPicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedMonthLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            
            if (showInfoCard) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Understanding the Finance Charts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Expense Breakdown: Shows your top spending categories for the month.\n" +
                                "• Impulse vs Necessary: Tracks how much spending was planned vs impulsive.\n" +
                                "• Cash Flow: Compares your savings/income against your expenses across 5 potential weeks in the month.",
                                style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 1. Expense Breakdown by Category
            SectionTitle("1. Expense Breakdown by Category")
            ExpenseCategoryBreakdown(monthExpenses)

            Spacer(Modifier.height(24.dp))

            // 2. Impulse vs. Necessary Spending
            SectionTitle("2. Impulse vs. Necessary Spending")
            ImpulseVsNecessaryCard(monthExpenses)

            Spacer(Modifier.height(24.dp))

            // 3. Cash Flow (Savings vs. Expenses)
            SectionTitle("3. Cash Flow (Weekly)")
            CashFlowChart(monthExpenses, monthSavings, selectedMonthKey)

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMonthPicker) {
        mobile.dairy.app.ui.insights.reports.MonthYearPickerDialog(
            initialMonthKey = selectedMonthKey,
            onDateSelected = { key, label ->
                selectedMonthKey = key
                selectedMonthLabel = label
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
fun ExpenseCategoryBreakdown(expenses: List<Expense>) {
    if (expenses.isEmpty()) {
        BloomCard {
            Text("No expenses logged for this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val categoryTotals = expenses
        .groupBy { it.category.lowercase().replaceFirstChar { c -> c.uppercase() } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(6) // Top 6

    val totalSum = categoryTotals.sumOf { it.second }
    val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF64748B))

    BloomCard {
        Column {
            categoryTotals.forEachIndexed { index, (cat, total) ->
                val percent = if (totalSum > 0) (total / totalSum).toFloat() else 0f
                val color = colors[index % colors.size]

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Circle indicator
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "%.2f", total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        // Bar
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(color.copy(alpha = 0.2f), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(percent).fillMaxHeight().background(color, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImpulseVsNecessaryCard(expenses: List<Expense>) {
    val necessary = expenses.filter { it.necessity == "necessary" }.sumOf { it.amount }
    val impulse = expenses.filter { it.necessity == "unnecessary" }.sumOf { it.amount }
    val total = necessary + impulse

    val necPercent = if (total > 0) (necessary / total).toFloat() else 0f
    val impPercent = if (total > 0) (impulse / total).toFloat() else 0f

    BloomCard {
        if (total == 0.0) {
            Text("No spending data available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column {
                Text("Spending Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))) {
                    if (necPercent > 0) {
                        Box(modifier = Modifier.weight(necPercent).fillMaxHeight().background(Color(0xFF10B981)))
                    }
                    if (impPercent > 0) {
                        Box(modifier = Modifier.weight(impPercent).fillMaxHeight().background(Color(0xFFEF4444)))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Planned/Necessary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "%.2f", necessary), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Impulsive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "%.2f", impulse), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowChart(expenses: List<Expense>, savings: List<Saving>, monthKey: String) {
    // Dynamically calculate weeks based on total days in month
    val totalDays = try {
        java.time.YearMonth.parse(monthKey).lengthOfMonth()
    } catch (e: Exception) {
        31
    }
    val totalWeeks = ((totalDays - 1) / 7) + 1 // Will be 4 for 28 days, 5 for 29-31 days
    
    val weekExpenses = DoubleArray(totalWeeks) { 0.0 }
    val weekSavings = DoubleArray(totalWeeks) { 0.0 }
    
    expenses.forEach { exp ->
        try {
            val date = LocalDate.parse(exp.date)
            val weekIdx = ((date.dayOfMonth - 1) / 7).coerceIn(0, totalWeeks - 1)
            weekExpenses[weekIdx] += exp.amount
        } catch (e: Exception) {}
    }
    
    savings.forEach { sav ->
        try {
            val date = LocalDate.parse(sav.date)
            val weekIdx = ((date.dayOfMonth - 1) / 7).coerceIn(0, totalWeeks - 1)
            weekSavings[weekIdx] += sav.amount
        } catch (e: Exception) {}
    }

    val maxAmount = (weekExpenses.maxOrNull() ?: 0.0).coerceAtLeast(weekSavings.maxOrNull() ?: 0.0).coerceAtLeast(10.0).toFloat()
    
    BloomCard {
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val density = LocalDensity.current
        val textPaint = remember(density) {
            android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF3B82F6), RoundedCornerShape(2.dp)))
                Text("Income/Savings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFF43F5E), RoundedCornerShape(2.dp)))
                Text("Expenses", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(24.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val width = size.width
            val height = size.height - 24.dp.toPx()
            
            val barWidth = 14.dp.toPx()
            val spacing = 4.dp.toPx()
            val groupWidth = (barWidth * 2) + spacing
            
            for (i in 0 until totalWeeks) {
                val cx = (width / totalWeeks) * i + (width / (totalWeeks * 2))
                val savH = ((weekSavings[i] / maxAmount) * height).toFloat()
                val expH = ((weekExpenses[i] / maxAmount) * height).toFloat()
                
                // Saving Bar (Blue)
                if (savH > 0) {
                    drawRoundRect(
                        color = Color(0xFF3B82F6),
                        topLeft = Offset(cx - groupWidth/2, height - savH),
                        size = Size(barWidth, savH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                
                // Expense Bar (Red)
                if (expH > 0) {
                    drawRoundRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(cx - groupWidth/2 + barWidth + spacing, height - expH),
                        size = Size(barWidth, expH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                
                // Week Label
                drawContext.canvas.nativeCanvas.drawText("Week ${i+1}", cx, size.height - 4.dp.toPx(), textPaint)
            }
        }
    }
}
