package mobile.dairy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.rememberDatePickerState
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhonelinkOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Mood
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.ui.theme.BloomColors

private val INSIGHT_ICONS = mapOf(
    "Eco" to Icons.Default.Eco,
    "SentimentSatisfied" to Icons.Default.SentimentSatisfied,
    "Extension" to Icons.Default.Extension,
    "AutoAwesome" to Icons.Default.AutoAwesome,
    "Whatshot" to Icons.Default.Whatshot,
    "EmojiEvents" to Icons.Default.EmojiEvents,
    "MilitaryTech" to Icons.Default.MilitaryTech,
    "Explore" to Icons.Default.Explore,
    "Security" to Icons.Default.Security,
    "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
    "FilterCenterFocus" to Icons.Default.FilterCenterFocus,
    "PhonelinkOff" to Icons.Default.PhonelinkOff,
    "Lightbulb" to Icons.Default.Lightbulb,
    "Link" to Icons.Default.Link
)

/* ------------------------------------------------------------------ */
/* Global Loading Overlay                                               */
/* ------------------------------------------------------------------ */

@Composable
fun GlobalLoadingOverlay(isLoading: Boolean) {
    if (!isLoading) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "page_flip")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "page_flip_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {}, // Consume taps
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(60.dp, 80.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))) {
                // Left page static
                Box(Modifier.fillMaxHeight().width(30.dp).align(Alignment.CenterStart).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant))
                // Right page static
                Box(Modifier.fillMaxHeight().width(30.dp).align(Alignment.CenterEnd).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant))
                // Flipping page
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(30.dp)
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            rotationY = -rotation
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 8f * density
                        }
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Syncing...", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/* ------------------------------------------------------------------ */
/* Cards + labels                                                      */
/* ------------------------------------------------------------------ */

@Composable
fun BloomCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp) // Softer, more premium rounded corners
    val base = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    
    Card(
        modifier = base,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        // Subtle elevation for premium depth
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(Modifier.padding(20.dp), content = content) // Increased padding
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

/* ------------------------------------------------------------------ */
/* MoodPicker — one-tap multi-select grid                               */
/* ------------------------------------------------------------------ */

@Composable
fun MoodPicker(selected: List<String>, onToggle: (String) -> Unit) {
    val moods = Mood.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        moods.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { mood ->
                    val isSel = selected.contains(mood.key)
                    val color = Color(mood.colorHex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.5.dp,
                                if (isSel) color else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp),
                            )
                            .background(
                                if (isSel) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp),
                            )
                            .clickable { onToggle(mood.key) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(
                            mood.emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            mood.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSel) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun MoodEmojiRow(moods: List<String>, size: Int = 18) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        moods.forEach { key ->
            Mood.fromKey(key)?.let { m ->
                Text(
                    m.emoji,
                    fontSize = size.sp
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* RatingScale — tappable 1..10                                         */
/* ------------------------------------------------------------------ */

@Composable
fun RatingScale(label: String, icon: ImageVector, value: Int?, onChange: (Int) -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${value ?: 5}/10",
                style = MaterialTheme.typography.bodyMedium,
                color = if (value != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        
        var width by remember { mutableFloatStateOf(1f) }
        val steps = 10
        val currentValue = value ?: 5
        
        fun updateFromX(x: Float) {
            val fraction = (x / width).coerceIn(0f, 1f)
            val n = (fraction * steps).roundToInt().coerceIn(1, steps)
            if (n != currentValue) onChange(n)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onGloballyPositioned { width = it.size.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> updateFromX(offset.x) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateFromX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            )
            
            // Filled track
            val fraction = if (currentValue == 0) 0f else (currentValue.toFloat() / steps)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(18.dp)
                    .background(
                        MaterialTheme.colorScheme.primary, 
                        RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = if (currentValue == 10) 4.dp else 0.dp, bottomEnd = if (currentValue == 10) 4.dp else 0.dp)
                    )
            )
            
            // Dots
            Row(
                Modifier.fillMaxWidth().height(18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..steps) {
                    Box(Modifier.size(3.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape))
                }
            }
            
            // Thumb
            if (currentValue > 0) {
                Box(
                    Modifier.fillMaxWidth(fraction).height(32.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Progress + chips + insight card                                      */
/* ------------------------------------------------------------------ */

@Composable
fun GoalProgressBar(progress: Int, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(
        progress = { (progress.coerceIn(0, 100)) / 100f },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun ChipRow(
    options: List<String>,
    selected: (String) -> Boolean,
    onClick: (String) -> Unit,
    labelOf: (String) -> String = { it },
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { opt ->
            FilterChip(selected = selected(opt), onClick = { onClick(opt) }, label = { Text(labelOf(opt)) })
        }
    }
}

@Composable
fun WrapChips(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
fun InsightCard(insight: Insight, onDismiss: (() -> Unit)? = null) {
    BloomCard(container = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            val vector = INSIGHT_ICONS[insight.icon]
            if (vector != null) {
                Icon(vector, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
                Text(insight.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(insight.message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        if (onDismiss != null) {
            Text(
                "Got it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp).clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, good: Boolean = false, emphasize: Boolean = false) {
    BloomCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(label)
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = when {
                good -> BloomColors.success()
                emphasize -> BloomColors.warning()
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/* ------------------------------------------------------------------ */
/* Bars — zero-dependency bar chart                                     */
/* ------------------------------------------------------------------ */

data class Bar(val label: String, val value: Double?, val color: Color? = null)

/** null values render a faint stub so gaps stay honest instead of reading as 0. */
@Composable
fun Bars(bars: List<Bar>, max: Double? = null, height: Dp = 80.dp) {
    val m = max ?: bars.mapNotNull { it.value }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Column {
        Row(
            Modifier.fillMaxWidth().height(height),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            bars.forEach { bar ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val frac = if (bar.value == null) 0.05f
                    else (bar.value / m).toFloat().coerceIn(0.05f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth(0.62f)
                            .height(height * frac)
                            .background(
                                bar.color ?: if (bar.value == null) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalFilterDialog(
    initialStartDate: String? = null,
    initialEndDate: String? = null,
    initialQuickRange: String = "week", // "today", "week", "month", "custom"
    onDismiss: () -> Unit,
    onApply: (quickRange: String, start: String, end: String) -> Unit,
    content: @Composable () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { Dates.todayKey() }
    
    var quickRange by remember { mutableStateOf(initialQuickRange) }
    var startDate by remember { mutableStateOf(initialStartDate ?: today) }
    var endDate by remember { mutableStateOf(initialEndDate ?: today) }
    
    var pickingFor by remember { mutableStateOf<String?>(null) } // "start" | "end" | null

    val isValid = remember(startDate, endDate) {
        startDate <= endDate
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            content()
            
            Spacer(Modifier.height(16.dp))
            SectionTitle("Date Range")
            Spacer(Modifier.height(8.dp))
            
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("today" to "Today", "week" to "7D", "month" to "30D", "custom" to "Custom").forEach { (key, label) ->
                    val sel = quickRange == key
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(
                                if (sel) MaterialTheme.colorScheme.surface else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { quickRange = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (quickRange == "custom") {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Date Input Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pickingFor = "start" },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(Dates.friendly(startDate), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    
                    // End Date Input Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pickingFor = "end" },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(Dates.friendly(endDate), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                if (!isValid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ Start date cannot be after End date.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (quickRange != "custom" || isValid) {
                        onApply(quickRange, startDate, endDate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = quickRange != "custom" || isValid
            ) {
                Text("Apply Filter", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingFor != null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                val targetKey = if (pickingFor == "start") startDate else endDate
                java.time.LocalDate.parse(targetKey).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
            }.getOrNull()
        )

        DatePickerDialog(
            onDismissRequest = { pickingFor = null },
            confirmButton = {
                Button(onClick = {
                    val selMs = dateState.selectedDateMillis
                    if (selMs != null) {
                        val key = Dates.fromMillis(selMs)
                        if (pickingFor == "start") {
                            startDate = key
                        } else {
                            endDate = key
                        }
                    }
                    pickingFor = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingFor = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomTwoDatePickerDialog(
    initialStartDate: String? = null,
    initialEndDate: String? = null,
    onDismiss: () -> Unit,
    onRangeSelected: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { Dates.todayKey() }
    
    var startDate by remember { mutableStateOf(initialStartDate ?: today) }
    var endDate by remember { mutableStateOf(initialEndDate ?: today) }
    
    var pickingFor by remember { mutableStateOf<String?>(null) } // "start" | "end" | null

    val isValid = remember(startDate, endDate) {
        startDate <= endDate
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Date Range", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f).clickable { pickingFor = "start" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(Dates.friendly(startDate), style = MaterialTheme.typography.titleSmall)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f).clickable { pickingFor = "end" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(Dates.friendly(endDate), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            if (!isValid) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ Start date cannot be after End date.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onRangeSelected(startDate, endDate) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isValid
            ) {
                Text("Confirm Range", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingFor != null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                val targetKey = if (pickingFor == "start") startDate else endDate
                java.time.LocalDate.parse(targetKey).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
            }.getOrNull()
        )

        DatePickerDialog(
            onDismissRequest = { pickingFor = null },
            confirmButton = {
                Button(onClick = {
                    val selMs = dateState.selectedDateMillis
                    if (selMs != null) {
                        val key = Dates.fromMillis(selMs)
                        if (pickingFor == "start") startDate = key else endDate = key
                    }
                    pickingFor = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingFor = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomDateRangePicker(
    onDismiss: () -> Unit,
    onRangeSelected: (String, String) -> Unit
) {
    BloomTwoDatePickerDialog(onDismiss = onDismiss, onRangeSelected = onRangeSelected)
}

/* ------------------------------------------------------------------ */
/* Accent dot (settings)                                                */
/* ------------------------------------------------------------------ */

@Composable
fun AccentDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .background(color, CircleShape)
            .border(
                if (selected) 3.dp else 0.dp,
                MaterialTheme.colorScheme.onBackground,
                CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
