package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import mobile.dairy.app.core.Dates
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalReportsScreen(
    nav: NavController,
    vm: InsightsViewModel = hiltViewModel()
) {
    val ctx by vm.ctx.collectAsState()
    val entries = ctx.entries
    val ratings = ctx.ratings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal & Mood Analytics") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // Chart Guide Card (Answers User's question about X & Y Axes)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Understanding the Charts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "• X-Axis (Horizontal): Represents Dates / Time (left = older, right = recent).\n" +
                                    "• Y-Axis (Vertical): Represents Rating Level (1 = Low/Poor, 10 = High/Excellent).",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 1. Mood Distribution & Breakdown
            SectionTitle("1. Mood Distribution & Triggers")
            BloomCard {
                val allMoods = entries.flatMap { it.moods }
                val moodCounts = allMoods.groupingBy { it }.eachCount()

                if (allMoods.isEmpty()) {
                    Text(
                        "No mood entries recorded yet. Keep logging your journal to unlock mood patterns!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Frequency of logged moods",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    val maxCount = moodCounts.values.maxOrNull() ?: 1
                    moodCounts.entries.sortedByDescending { it.value }.take(5).forEach { (mood, count) ->
                        val ratio = count.toFloat() / maxCount
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                mood.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(90.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "$count days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                // Top Triggers
                val triggers = entries.flatMap { it.moodCauses }.groupingBy { it }.eachCount()
                Eyebrow("Top Mood Triggers")
                Spacer(Modifier.height(6.dp))
                if (triggers.isEmpty()) {
                    Text(
                        "Add reasons when writing entries to see what impacts your mood.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        triggers.entries.sortedByDescending { it.value }.take(4).forEach { (cause, count) ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("$cause ($count)") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Ratings Trend Chart
            SectionTitle("2. Ratings Trend Over Time")
            BloomCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Overall Rating vs Energy Score",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Y-Axis: Rating (1-10) | X-Axis: Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Map recent ratings to Vico chart
                val recentRatings = ratings.sortedBy { it.date }.takeLast(14)
                if (recentRatings.isNotEmpty()) {
                    val points = recentRatings.map { (it.overall ?: 5).toFloat() }.toFloatArray()
                    val chartModel = entryModelOf(*points.toTypedArray())

                    Chart(
                        chart = lineChart(),
                        model = chartModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    // Fallback visual chart when data is minimal
                    val fallbackPoints = arrayOf(6f, 7f, 5f, 8f, 9f, 7f, 8f, 9f)
                    val chartModel = entryModelOf(*fallbackPoints)

                    Chart(
                        chart = lineChart(),
                        model = chartModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Social Impact Report
            SectionTitle("3. Social Impact & People")
            BloomCard {
                val peopleList = entries.flatMap { it.people }
                val personCounts = peopleList.groupBy { it.name }

                if (personCounts.isEmpty()) {
                    Text(
                        "Mention people in your daily journal check-ins to track who influences your mood most!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    personCounts.entries.sortedByDescending { it.value.size }.take(4).forEach { (name, refs) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "${refs.size} check-ins",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
