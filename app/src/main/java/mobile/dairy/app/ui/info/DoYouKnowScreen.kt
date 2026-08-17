package mobile.dairy.app.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class FeatureInfo(
    val title: String,
    val description: String,
    val steps: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoYouKnowScreen(navController: NavController) {
    val features = listOf(
        FeatureInfo(
            title = "Custom Journals",
            description = "Create journals with your own questions or sections.",
            steps = "1. Go to Settings.\n2. Tap 'Journal Template'.\n3. Tap '+' for new questions or sections.",
            icon = Icons.Default.MenuBook
        ),
        FeatureInfo(
            title = "Track Goals",
            description = "Add milestones and daily tasks to hit goals.",
            steps = "1. Go to Goals tab.\n2. Create a Goal.\n3. Add milestones and log daily tasks.",
            icon = Icons.Default.TrackChanges
        ),
        FeatureInfo(
            title = "Money Manager",
            description = "Track daily expenses, earnings, and savings.",
            steps = "1. Go to Money tab.\n2. Tap + to log expense, earning, or saving.\n3. Review your budget.",
            icon = Icons.Default.Payments
        ),
        FeatureInfo(
            title = "Cloud Sync",
            description = "Sync your data safely using Google or Email.",
            steps = "1. Go to Settings.\n2. Scroll down to Data & Privacy.\n3. Tap 'Sync with Cloud' or 'Switch to Online Mode'.",
            icon = Icons.Default.CloudSync
        ),
        FeatureInfo(
            title = "Multiple Accounts",
            description = "Use multiple accounts offline seamlessly.",
            steps = "1. Go to Settings.\n2. Scroll down to Data & Privacy.\n3. Tap 'Sign out' to switch or add a new offline account.",
            icon = Icons.Default.AccountCircle
        ),
        FeatureInfo(
            title = "7-Day Edits",
            description = "Edit journals for up to 7 days.",
            steps = "1. Open a past journal entry.\n2. Tap the text to edit.\n3. Note: Locked after 7 days.",
            icon = Icons.Default.EditCalendar
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Do you know?", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            features.forEach { feature ->
                FeatureCard(feature)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FeatureCard(feature: FeatureInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = feature.title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!expanded) {
                        Text(
                            text = "Tap for details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "How to use:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.steps,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
