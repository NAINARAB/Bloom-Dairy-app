package mobile.dairy.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mobile.dairy.app.core.Constants
import mobile.dairy.app.services.LocalPrefs
import mobile.dairy.app.services.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val localPrefs: LocalPrefs,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    fun finish() {
        viewModelScope.launch {
            runCatching {
                reminderScheduler.rescheduleAll()
                localPrefs.setOnboarded()
            }
        }
    }
}

private data class Slide(val icon: ImageVector, val title: String, val body: String)

private val SLIDES = listOf(
    Slide(Icons.AutoMirrored.Filled.MenuBook, "A diary that helps you grow",
        "Two quiet minutes a day: your mood, your wins, your lessons. Bloom turns them into gentle, judgement-free insights."),
    Slide(Icons.Default.FilterCenterFocus, "Goals with honest progress",
        "Track short and long-term goals with milestones, minutes, and effort — and get encouragement exactly when a day was hard."),
    Slide(Icons.Default.AccountBalanceWallet, "Money awareness, not guilt",
        "Log what you spent, saved, and deliberately avoided. Watch your \u201Cavoided spending\u201D grow into real savings."),
    Slide(Icons.Default.Lock, "Private by design",
        "Your entries live in your own account, protected by security rules and an optional biometric lock. Notifications can hide all content."),
)

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val pager = rememberPagerState { SLIDES.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == SLIDES.size - 1

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> vm.finish() } // proceed either way — reminders simply stay silent if denied

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val slide = SLIDES[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    slide.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(22.dp))
                Text(slide.title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (page == SLIDES.size - 1) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        Constants.WELLBEING_DISCLAIMER,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(SLIDES.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pager.currentPage) 10.dp else 7.dp)
                        .background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Button(
                onClick = {
                    if (!isLast) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.finish()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isLast) "Enable gentle reminders & start" else "Next") }
            if (isLast) {
                TextButton(onClick = vm::finish, modifier = Modifier.fillMaxWidth()) {
                    Text("Start without notifications")
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
