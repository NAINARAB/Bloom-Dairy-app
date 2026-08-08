package mobile.dairy.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import mobile.dairy.app.domain.CategoryDef
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.ui.components.BloomCard
import mobile.dairy.app.ui.components.WrapChips
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()
    val focusManager = LocalFocusManager.current
    val defaultCategoryKeys = remember { AppPrefs().transactionCategories.map { it.key } }

    var showAddDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<CategoryDef?>(null) }

    if (showAddDialog) {
        var newLabel by remember { mutableStateOf("") }
        var newEmoji by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEmoji,
                        onValueChange = { if (it.length <= 2) newEmoji = it },
                        label = { Text("Emoji") },
                        placeholder = { Text("💰") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newLabel.isNotBlank()) {
                        val newCat = CategoryDef(
                            key = newLabel.lowercase().replace(" ", "-"),
                            label = newLabel,
                            emoji = newEmoji.ifBlank { "💠" }
                        )
                        vm.save(mapOf("transactionCategories" to p.transactionCategories + newCat))
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editCategory != null) {
        val cat = editCategory!!
        val isDefault = defaultCategoryKeys.contains(cat.key)
        var editLabel by remember { mutableStateOf(cat.label) }
        var editEmoji by remember { mutableStateOf(cat.emoji) }
        
        AlertDialog(
            onDismissRequest = { editCategory = null },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmoji,
                        onValueChange = { if (it.length <= 2) editEmoji = it },
                        label = { Text("Emoji") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editLabel.isNotBlank()) {
                        val newCats = p.transactionCategories.map { 
                            if (it.key == cat.key) it.copy(label = editLabel, emoji = editEmoji.ifBlank { "💠" }) else it 
                        }
                        vm.save(mapOf("transactionCategories" to newCats))
                        editCategory = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (!isDefault) {
                        TextButton(onClick = { 
                            val newCats = p.transactionCategories.filter { it.key != cat.key }
                            vm.save(mapOf("transactionCategories" to newCats))
                            editCategory = null
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { editCategory = null }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Category")
            }
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
                Text("Manage the categories used for spending and earning", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                WrapChips {
                    p.transactionCategories.forEach { cat ->
                        FilterChip(
                            selected = false,
                            onClick = { editCategory = cat },
                            label = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
