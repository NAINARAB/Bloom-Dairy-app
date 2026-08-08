package mobile.dairy.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.NotificationCategory
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import mobile.dairy.app.ui.lock.canUseBiometrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()
    val r by vm.reminders.collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current
    var showTimePickerFor by remember { mutableStateOf<NotificationCategory?>(null) }

    if (showTimePickerFor != null) {
        val cat = showTimePickerFor!!
        val currentMinute = r.timeMinutes[cat.name] ?: (r.times[cat.name]?.times(60)) ?: (cat.defaultHour?.times(60)) ?: (20 * 60)
        val timeState = rememberTimePickerState(initialHour = currentMinute / 60, initialMinute = currentMinute % 60)
        AlertDialog(
            onDismissRequest = { showTimePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveReminders { it.copy(timeMinutes = it.timeMinutes + (cat.name to (timeState.hour * 60 + timeState.minute))) }
                    showTimePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerFor = null }) { Text("Cancel") }
            },
            title = { Text("Set time for ${cat.label}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timeState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notifications", style = MaterialTheme.typography.titleMedium) },
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
                .padding(24.dp)
        ) {
            SectionTitle("Notifications")
            BloomCard {
                SettingSwitch(
                    "All reminders", "Master switch for daily nudges",
                    r.enabled, { on -> vm.saveReminders { it.copy(enabled = on) } }
                )
                
                if (r.enabled) {
                    Divider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text("Daily Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    NotificationCategory.entries.forEach { cat ->
                        val isEnabled = !r.disabledCategories.contains(cat.name)
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cat.label, style = MaterialTheme.typography.bodyLarge)
                                Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val currentMinute = r.timeMinutes[cat.name] ?: (r.times[cat.name]?.times(60)) ?: (cat.defaultHour?.times(60)) ?: (20 * 60)
                                Text(
                                    String.format("%02d:%02d", currentMinute / 60, currentMinute % 60),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, 
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .clickable(enabled = isEnabled) { 
                                            showTimePickerFor = cat
                                        }
                                )
                                Spacer(Modifier.width(16.dp))
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        vm.saveReminders { 
                                            val newDisabled = if (checked) it.disabledCategories - cat.name else it.disabledCategories + cat.name
                                            it.copy(disabledCategories = newDisabled)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

