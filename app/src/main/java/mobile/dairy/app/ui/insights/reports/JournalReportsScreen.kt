package mobile.dairy.app.ui.insights.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.insights.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalReportsScreen(nav: NavController, vm: InsightsViewModel = hiltViewModel()) {
    val ctx by vm.ctx.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal & Mood Reports") },
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // 1. Mood Distribution
            SectionTitle("Mood Distribution & Triggers")
            BloomCard {
                Text("Placeholder: Pie chart of moods will go here.", style = MaterialTheme.typography.bodyMedium)
                // Note: Vico does not have a built-in Pie chart in the free version (it's often a custom canvas or bar chart). 
                // We'll implement a simple horizontal bar chart or custom pie chart later if needed.
                Spacer(Modifier.height(16.dp))
                Text("Top Triggers", style = MaterialTheme.typography.labelLarge)
                Text("• Work\n• Sleep\n• Family", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))

            // 2. Ratings Trend
            SectionTitle("Ratings Trend")
            BloomCard {
                // Prepare mock or real data for Vico Line Chart
                // In a real scenario, we'd map ctx.ratings to entryModelOf()
                val mockData = entryModelOf(4, 6, 8, 7, 9, 6, 8)
                
                Chart(
                    chart = lineChart(),
                    model = mockData,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 3. Social Impact
            SectionTitle("Social Impact")
            BloomCard {
                Text("Who you spend time with most and how they affect your mood.", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
