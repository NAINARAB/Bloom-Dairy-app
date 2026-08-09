package mobile.dairy.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.domain.JournalQuestionDef
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.SectionTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalSettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val allQuestions = p.journalQuestions
    val activeQuestions = allQuestions.filter { it.isActive }
    val inactiveQuestions = allQuestions.filter { !it.isActive }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Journal Template", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Question")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    "Customize your daily check-in. You can have up to 12 active questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item { SectionTitle("Active Questions (${activeQuestions.size}/12)") }

            items(activeQuestions.size) { index ->
                val q = activeQuestions[index]
                QuestionItem(
                    q = q,
                    isFirst = index == 0,
                    isLast = index == activeQuestions.size - 1,
                    onMoveUp = {
                        if (index > 0) {
                            val newList = allQuestions.toMutableList()
                            val i1 = newList.indexOfFirst { it.id == q.id }
                            val i2 = newList.indexOfFirst { it.id == activeQuestions[index - 1].id }
                            if (i1 != -1 && i2 != -1) {
                                val temp = newList[i1]
                                newList[i1] = newList[i2]
                                newList[i2] = temp
                                vm.save(mapOf("journalQuestions" to newList))
                            }
                        }
                    },
                    onMoveDown = {
                        if (index < activeQuestions.size - 1) {
                            val newList = allQuestions.toMutableList()
                            val i1 = newList.indexOfFirst { it.id == q.id }
                            val i2 = newList.indexOfFirst { it.id == activeQuestions[index + 1].id }
                            if (i1 != -1 && i2 != -1) {
                                val temp = newList[i1]
                                newList[i1] = newList[i2]
                                newList[i2] = temp
                                vm.save(mapOf("journalQuestions" to newList))
                            }
                        }
                    },
                    onToggle = {
                        val newList = allQuestions.map { if (it.id == q.id) it.copy(isActive = false) else it }
                        vm.save(mapOf("journalQuestions" to newList))
                    },
                    onDelete = if (q.isCustom) {
                        {
                            val newList = allQuestions.filter { it.id != q.id }
                            vm.save(mapOf("journalQuestions" to newList))
                        }
                    } else null
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                Spacer(Modifier.height(24.dp))
                SectionTitle("Inactive Questions (${inactiveQuestions.size}/8)")
            }

            items(inactiveQuestions.size) { index ->
                val q = inactiveQuestions[index]
                QuestionItem(
                    q = q,
                    isFirst = true,
                    isLast = true,
                    onMoveUp = {},
                    onMoveDown = {},
                    onToggle = {
                        if (activeQuestions.size < 12) {
                            val newList = allQuestions.map { if (it.id == q.id) it.copy(isActive = true) else it }
                            vm.save(mapOf("journalQuestions" to newList))
                        }
                    },
                    onDelete = if (q.isCustom) {
                        {
                            val newList = allQuestions.filter { it.id != q.id }
                            vm.save(mapOf("journalQuestions" to newList))
                        }
                    } else null
                )
                Spacer(Modifier.height(8.dp))
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        if (showAddDialog) {
            AddQuestionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, type, options ->
                    val customCount = allQuestions.count { it.isCustom }
                    if (customCount < 20 && activeQuestions.size < 12) {
                        val newQ = JournalQuestionDef(
                            id = "custom_${UUID.randomUUID().toString().take(8)}",
                            title = title,
                            type = type,
                            isCustom = true,
                            isActive = true,
                            isMandatory = false,
                            options = options
                        )
                        vm.save(mapOf("journalQuestions" to allQuestions + newQ))
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun QuestionItem(
    q: JournalQuestionDef,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    BloomCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (q.isActive) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (isFirst) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (isLast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(q.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val typeLabel = when (q.type) {
                    "text" -> "Text Field"
                    "dropdown" -> "Drop Down"
                    "number" -> "Number Input"
                    "slider" -> "1-10 Rating"
                    "toggle" -> "Yes/No Toggle"
                    "date" -> "Date Picker"
                    else -> "Built-in"
                }
                Text("Type: $typeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (q.isMandatory) {
                    Text("Mandatory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!q.isMandatory) {
                Switch(checked = q.isActive, onCheckedChange = { onToggle() })
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionDialog(onDismiss: () -> Unit, onAdd: (title: String, type: String, options: List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("text") }
    var expanded by remember { mutableStateOf(false) }
    var optionsText by remember { mutableStateOf("") }

    val types = listOf(
        "text" to "Text Field",
        "dropdown" to "Drop Down",
        "number" to "Number Input",
        "slider" to "1-10 Rating",
        "toggle" to "Yes/No Toggle",
        "date" to "Date Picker"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Question") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("Question Title (Max 50 chars)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = types.find { it.first == type }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Input Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    type = key
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (type == "dropdown") {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = { optionsText = it },
                        label = { Text("Options (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opts = if (type == "dropdown") optionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                    onAdd(title, type, opts)
                },
                enabled = title.isNotBlank() && (type != "dropdown" || optionsText.isNotBlank())
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
