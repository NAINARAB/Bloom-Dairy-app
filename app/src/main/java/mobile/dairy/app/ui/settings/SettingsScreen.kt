package mobile.dairy.app.ui.settings

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.NotificationCategory
import mobile.dairy.app.data.AuthRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.services.LocalPrefs
import mobile.dairy.app.services.ReminderScheduler
import mobile.dairy.app.services.ReminderSettings
import mobile.dairy.app.ui.components.AccentDot
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.components.WrapChips
import mobile.dairy.app.ui.lock.canUseBiometrics
import mobile.dairy.app.ui.theme.ACCENTS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PrefsRepository,
    private val authRepo: AuthRepository,
    private val localPrefs: LocalPrefs,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val prefs = prefsRepo.appPrefs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPrefs())
    val reminders = localPrefs.reminderSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    val email: String? get() = authRepo.currentUser?.email
    val name: String? get() = authRepo.currentUser?.displayName

    fun save(patch: Map<String, Any?>) {
        viewModelScope.launch { runCatching { prefsRepo.save(patch) } }
    }

    /** Notification settings live in DataStore too, so AlarmManager receivers work offline. */
    fun saveReminders(transform: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch {
            runCatching {
                val next = transform(localPrefs.reminderSettingsNow())
                localPrefs.saveReminderSettings(next)
                reminderScheduler.rescheduleAll()
                // Mirror private mode into Firestore prefs (used by Cloud Functions pushes).
                prefsRepo.save(mapOf("privateNotifications" to next.privateMode))
            }
        }
    }

    fun exportData(onUrl: (String) -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { authRepo.requestDataExport() }
                .onSuccess(onUrl)
                .onFailure { message.value = "Export failed: ${it.message}" }
            busy.value = false
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            busy.value = true
            runCatching { authRepo.deleteAccount() }
                .onFailure { message.value = "Deletion failed: ${it.message}" }
            busy.value = false
        }
    }

    fun signOut() = authRepo.signOut()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()
    val r by vm.reminders.collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Profile
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {
                Box(Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Text(vm.name?.take(1) ?: "?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(vm.name ?: "Bloom user", style = MaterialTheme.typography.titleMedium)
                    Text(vm.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))
            BloomCard {
                SettingsMenuRow(
                    title = "Appearance",
                    subtitle = "Theme, fonts, and accent colors",
                    onClick = { nav.navigate(mobile.dairy.app.Routes.SETTINGS_APPEARANCE) }
                )
                Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuRow(
                    title = "Journal Template",
                    subtitle = "Customize your daily check-in questions",
                    onClick = { nav.navigate(mobile.dairy.app.Routes.SETTINGS_JOURNAL) }
                )
                Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuRow(
                    title = "Transaction Categories",
                    subtitle = "Manage tags for spending and earning",
                    onClick = { nav.navigate(mobile.dairy.app.Routes.SETTINGS_CATEGORIES) }
                )
                Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuRow(
                    title = "Notifications",
                    subtitle = "Manage your daily reminders",
                    onClick = { nav.navigate(mobile.dairy.app.Routes.SETTINGS_PRIVACY) }
                )
            }

            Spacer(Modifier.height(32.dp))
            SectionTitle("Data & Privacy")
            BloomCard {
                SettingSwitch(
                    "Private mode", "Notifications only show \"Your daily reflection is ready.\"",
                    r.privateMode, { on -> vm.saveReminders { it.copy(privateMode = on) } }
                )
                
                SettingSwitch(
                    "Biometric lock", "Face or fingerprint unlock required to open Bloom",
                    p.lockEnabled, { on -> if (canUseBiometrics(context) || !on) vm.save(mapOf("lockEnabled" to on)) }
                )

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        vm.exportData { url ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "My Bloom data export: $url")
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Bloom data"))
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (busy) "Preparing export..." else "Export my data (JSON)")
                }
                
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.height(48.dp))
            Text(Constants.WELLBEING_DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsMenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
