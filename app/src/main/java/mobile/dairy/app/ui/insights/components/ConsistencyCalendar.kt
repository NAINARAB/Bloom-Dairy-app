package mobile.dairy.app.ui.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile.dairy.app.core.Dates
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle

@Composable
fun ConsistencyCalendar(
    ctx: InsightContext,
    daysToDisplay: Int = 90,
    onDayClick: (String) -> Unit
) {
    val today = ctx.today
    val startDate = Dates.addDays(today, -(daysToDisplay - 1).toLong())
    
    // Group activities by date
    val entryDates = ctx.entries.map { it.date }.toSet()
    val goalDates = ctx.goalUpdates.map { it.date }.toSet()
    val expenseDates = ctx.expenses.map { it.date }.toSet()
    
    val days = (0 until daysToDisplay).map { i ->
        Dates.addDays(startDate, i.toLong())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Consistency Heatmap")
        BloomCard {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(180.dp)
            ) {
                items(days.size) { index ->
                    val date = days[index]
                    val hasEntry = entryDates.contains(date)
                    val hasGoal = goalDates.contains(date)
                    val hasExpense = expenseDates.contains(date)
                    
                    val hasActivity = hasEntry || hasGoal || hasExpense

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (hasActivity) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onDayClick(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasEntry) {
                            Icon(
                                Icons.Default.Book, 
                                contentDescription = "Journal", 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        } else if (hasGoal) {
                            Icon(
                                Icons.Default.CrisisAlert, 
                                contentDescription = "Goal", 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        } else if (hasExpense) {
                            Icon(
                                Icons.Default.AttachMoney, 
                                contentDescription = "Expense", 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last $daysToDisplay days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(Icons.Default.Book, "Journal")
                    LegendItem(Icons.Default.CrisisAlert, "Goals")
                    LegendItem(Icons.Default.AttachMoney, "Money")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
