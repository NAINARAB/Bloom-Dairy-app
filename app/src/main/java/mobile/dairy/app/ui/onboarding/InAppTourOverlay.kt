package mobile.dairy.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.launch

private data class TourStep(val tabIndex: Int, val title: String, val text: String)

private val TOUR_STEPS = listOf(
    TourStep(2, "Home Dashboard", "See your daily overview, quick actions, and check-in streaks."),
    TourStep(3, "Journal", "Log your mood and answer custom questions. Entries can be edited for up to 7 days."),
    TourStep(1, "Goals", "Add milestones, track daily tasks, and celebrate your progress."),
    TourStep(0, "Money", "Track daily expenses and savings without guilt."),
    TourStep(4, "Insights", "View beautiful charts and trends about your habits over time.")
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InAppTourOverlay(
    pagerState: PagerState,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val step = TOUR_STEPS[currentStep]
    val isLast = currentStep == TOUR_STEPS.size - 1

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onFinish() }

    // Synchronize pager with tour step
    LaunchedEffect(currentStep) {
        pagerState.animateScrollToPage(step.tabIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 80.dp), // Keep above bottom nav
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step ${currentStep + 1} of ${TOUR_STEPS.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = step.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (!isLast) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onFinish) {
                        Text("Skip Tour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Notifications & Finish")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onFinish) {
                        Text("Finish Without Notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
