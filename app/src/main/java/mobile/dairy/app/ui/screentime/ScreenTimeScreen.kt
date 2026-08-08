package mobile.dairy.app.ui.screentime

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Cyclone
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Format
import mobile.dairy.app.data.InsightRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.services.ScreenTimeService
import mobile.dairy.app.ui.components.Bar
import mobile.dairy.app.ui.components.Bars
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.EmptyState
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.StatCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeService: ScreenTimeService,
    private val prefsRepo: PrefsRepository,
    insightRepo: InsightRepository,
) : ViewModel() {

    val days = insightRepo.screenTime(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val prefs = prefsRepo.appPrefs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mobile.dairy.app.domain.AppPrefs())

    val hasPermission = MutableStateFlow(screenTimeService.hasPermission())
    val syncing = MutableStateFlow(false)

    fun refreshPermission() {
        hasPermission.value = screenTimeService.hasPermission()
    }

    fun openSettings() = screenTimeService.openUsageAccessSettings()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { prefsRepo.save(mapOf("screenTimeEnabled" to enabled)) }
            if (enabled) sync()
        }
    }

    fun sync() {
        viewModelScope.launch {
            syncing.value = true
            runCatching { screenTimeService.syncToday() }
            refreshPermission()
            syncing.value = false
        }
    }
}

private val KIND_ICON = mapOf(
    "productive" to Icons.Default.Eco,
    "neutral" to Icons.Default.Circle,
    "distracting" to Icons.Default.Cyclone
)
private val KIND_COLOR = mapOf(
    "productive" to Color(0xFF5FB4A2),
    "neutral" to Color(0xFF8A8FA3),
    "distracting" to Color(0xFFD45B50)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeScreen(nav: NavController, vm: ScreenTimeViewModel = hiltViewModel()) {
    val days by vm.days.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val hasPermission by vm.hasPermission.collectAsState()
    val syncing by vm.syncing.collectAsState()

    LaunchedEffect(Unit) { vm.refreshPermission() }

    val today = Dates.todayKey()
    val byDate = days.associateBy { it.date }
    val todayRec = byDate[today]
    val weekBars = (0..6).map { i ->
        val key = Dates.addDays(today, (i - 6).toLong())
        Bar(Dates.friendly(key).take(3), byDate[key]?.totalMinutes)
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Screen Time", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::sync, enabled = !syncing && hasPermission) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            
            BloomCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Track usage", style = MaterialTheme.typography.titleMedium)
                        Text("On-device app minutes", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = prefs.screenTimeEnabled, onCheckedChange = { vm.setEnabled(it) })
                }
            }

            if (!prefs.screenTimeEnabled) {
                EmptyState(Icons.Default.Smartphone, "Tracking is off", "Enable tracking to see your digital habits.")
                return@Column
            }

            if (!hasPermission) {
                EmptyState(Icons.Default.Lock, "Permission required", "Bloom needs 'Usage Access' to read app stats.")
                Button(onClick = vm::openSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open System Settings")
                }
                return@Column
            }

            SectionTitle("Weekly trend")
            BloomCard {
                Bars(weekBars, height = 100.dp)
            }

            if (todayRec != null) {
                SectionTitle("Today's stats")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Total", Format.minutes(todayRec.totalMinutes), Modifier.weight(1f))
                    StatCard("Unlocks", todayRec.unlocks?.toString() ?: "–", Modifier.weight(1f))
                }

                SectionTitle("Top apps")
                todayRec.apps.sortedByDescending { it.minutes }.take(5).forEach { app ->
                    BloomCard(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    KIND_ICON[app.kind] ?: Icons.Default.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = KIND_COLOR[app.kind] ?: MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                                    Text(app.kind.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(Format.minutes(app.minutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                EmptyState(Icons.Default.HourglassEmpty, "No data yet", "Tap sync or wait a few moments.")
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}
