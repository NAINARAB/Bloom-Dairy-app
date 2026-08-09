package mobile.dairy.app.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.ui.components.AccentDot
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.theme.ACCENTS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Appearance", style = MaterialTheme.typography.titleMedium) },
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
            BloomCard {
                Eyebrow("Theme")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (m, label) ->
                        val sel = p.theme == m
                        FilterChip(
                            selected = sel,
                            onClick = { vm.save(mapOf("theme" to m)) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Eyebrow("Accent color")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ACCENTS.keys.forEach { key ->
                        val color = if (isSystemInDarkTheme()) ACCENTS[key]!!.dark else ACCENTS[key]!!.light
                        AccentDot(color, p.accent == key) { vm.save(mapOf("accent" to key)) }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Eyebrow("Card Gradient")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    mobile.dairy.app.ui.theme.GRADIENTS.keys.forEach { key ->
                        val grad = mobile.dairy.app.ui.theme.GRADIENTS[key]!!
                        val colors = if (isSystemInDarkTheme()) grad.dark else grad.light
                        val brush = androidx.compose.ui.graphics.Brush.linearGradient(colors)
                        val isSelected = p.cardGradient == key
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(brush)
                                .clickable { vm.save(mapOf("cardGradient" to key)) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
