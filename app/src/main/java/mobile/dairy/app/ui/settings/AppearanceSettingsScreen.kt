package mobile.dairy.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.roundToInt
import mobile.dairy.app.ui.components.AccentDot
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.Eyebrow
import mobile.dairy.app.ui.theme.ACCENTS
import mobile.dairy.app.ui.theme.GRADIENTS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()

    val fontSteps = listOf(
        11 to "Extra small",
        12 to "Small",
        13 to "Compact",
        14 to "Standard",
        15 to "Medium",
        16 to "Large",
        18 to "Extra large"
    )
    val currentFontSize = if (p.fontSize == 0) 14 else p.fontSize
    val currentStepIndex = fontSteps.indexOfFirst { it.first == currentFontSize }.takeIf { it >= 0 } ?: 3

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Font size and weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top dynamic font explanation & preview
            Text(
                text = "Apps that support dynamic fonts will adjust the font size and weight to what you have selected. The change varies for different apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Font size stepped slider card
            Eyebrow("Font size")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fontSteps[currentStepIndex].second,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "A",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = currentStepIndex.toFloat(),
                            onValueChange = { stepVal ->
                                val idx = stepVal.roundToInt().coerceIn(0, fontSteps.size - 1)
                                vm.save(mapOf("fontSize" to fontSteps[idx].first))
                            },
                            valueRange = 0f..(fontSteps.size - 1).toFloat(),
                            steps = fontSteps.size - 2,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "A",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Font weight switch card
            Eyebrow("Font weight")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bold text", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = p.boldText,
                        onCheckedChange = { vm.save(mapOf("boldText" to it)) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Theme, Accents & Gradients
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

                Spacer(Modifier.height(20.dp))
                Eyebrow("Accent color")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ACCENTS.keys.forEach { key ->
                        val color = if (isSystemInDarkTheme()) ACCENTS[key]!!.dark else ACCENTS[key]!!.light
                        AccentDot(color, p.accent == key) { vm.save(mapOf("accent" to key)) }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Eyebrow("Card Gradient")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GRADIENTS.keys.forEach { key ->
                        val grad = GRADIENTS[key]!!
                        val colors = if (isSystemInDarkTheme()) grad.dark else grad.light
                        val brush = Brush.linearGradient(colors)
                        val isSelected = p.cardGradient == key
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(brush)
                                .clickable { vm.save(mapOf("cardGradient" to key)) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Restore defaults action
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = {
                        vm.save(
                            mapOf(
                                "fontSize" to 14,
                                "boldText" to false,
                                "theme" to "system",
                                "accent" to "violet",
                                "cardGradient" to "midnight"
                            )
                        )
                    }
                ) {
                    Text("Restore defaults", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
