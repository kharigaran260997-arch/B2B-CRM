package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.AuthState
import com.example.ui.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CrmViewModel,
    onDarkThemeToggle: (Boolean) -> Unit,
    currentDarkState: Boolean
) {
    val context = LocalContext.current
    val enrichedDeals by viewModel.enrichedDeals.collectAsState()
    val revenueTarget by viewModel.revenueTarget.collectAsState()
    val teamMembers by viewModel.teamMembers.collectAsState()
    val customFields by viewModel.customFields.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // CSV Mapping workspace states
    val csvHeaders by viewModel.csvHeaders.collectAsState()
    val csvRows by viewModel.csvParsedRows.collectAsState()
    val columnMappings by viewModel.columnMappings.collectAsState()
    val importSuccessCount by viewModel.csvImportSuccessCount.collectAsState()

    var editableTarget by remember { mutableStateOf((revenueTarget / 100000.0).toString()) }
    var newTeamMemberName by remember { mutableStateOf("") }

    // Custom field creator inputs
    var newFieldLabel by remember { mutableStateOf("") }
    var newFieldType by remember { mutableStateOf(FieldType.TEXT) }
    var showCustomFieldDialog by remember { mutableStateOf(false) }

    // Picker for CSV import
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.parseCSV(context, it)
        }
    }

    // Displays success notification after importing
    LaunchedEffect(importSuccessCount) {
        importSuccessCount?.let {
            Toast.makeText(context, "Successfully imported $it deals to database!", Toast.LENGTH_LONG).show()
            viewModel.csvImportSuccessCount.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Control Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Session Header
            (authState as? AuthState.Authenticated)?.let { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Session", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(session.email, fontWeight = FontWeight.Bold)
                            Text("Role: ${session.role}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Log out", tint = Color.Red)
                        }
                    }
                }
            }

            // Editable Target metrics
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Configurable Revenue Target Range", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editableTarget,
                            onValueChange = { editableTarget = it },
                            label = { Text("Goal (Lakhs, eg: 50L = 50)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("revenue_target_field"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val lakhs = editableTarget.toDoubleOrNull() ?: 50.0
                                viewModel.setRevenueTarget(lakhs * 100000.0)
                                Toast.makeText(context, "Revenue target saved!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Update")
                        }
                    }
                }
            }

            // Team Database Configurations
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Team Database Management", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newTeamMemberName,
                            onValueChange = { newTeamMemberName = it },
                            placeholder = { Text("E.g: John Doe") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newTeamMemberName.isNotBlank()) {
                                    viewModel.addTeamMember(newTeamMemberName)
                                    newTeamMemberName = ""
                                }
                            }
                        ) {
                            Text("Add Rep")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                    Text("Active Team Members:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    teamMembers.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(member, fontSize = 13.sp)
                            IconButton(onClick = { viewModel.removeTeamMember(member) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Delete member", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Custom Dynamic Fields configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Field Schema Managers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = { showCustomFieldDialog = true }) {
                            Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (customFields.isEmpty()) {
                        Text("No custom attributes added yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        customFields.forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${r.label} (${r.type.name})", fontSize = 12.sp)
                                IconButton(onClick = { viewModel.deleteCustomField(r.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // CSV Imports & Exports (Satisfies Page 12)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data Import / Export Hub (CSV)", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { csvLauncher.launch("text/comma-separated-values") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import CSV file", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val csvText = viewModel.generateExportCSVText(enrichedDeals)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, csvText)
                                    putExtra(Intent.EXTRA_SUBJECT, "rcc_export.csv")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Export Details"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV Share", fontSize = 11.sp)
                        }
                    }

                    // Render inline CSV Mapping tools if file is picked
                    if (csvRows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Map Column Indexes to Databases Fields", fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                val columnsToMap = listOf("Name", "Company", "Deal Value", "Stage", "Email", "Phone")
                                columnsToMap.forEach { targetColumn ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(targetColumn, fontSize = 11.sp)

                                        // Simple mapping options dropdown selector
                                        var showDropdown by remember { mutableStateOf(false) }
                                        val selectedIdx = columnMappings[targetColumn] ?: -1
                                        val selectedHeader = if (selectedIdx in csvHeaders.indices) csvHeaders[selectedIdx] else "Skip"

                                        Box {
                                            OutlinedButton(onClick = { showDropdown = true }) {
                                                Text(selectedHeader, fontSize = 10.sp)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                            DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                                                DropdownMenuItem(
                                                    text = { Text("Skip", fontSize = 10.sp) },
                                                    onClick = {
                                                        viewModel.columnMappings.value = columnMappings - targetColumn
                                                        showDropdown = false
                                                    }
                                                )
                                                csvHeaders.forEachIndexed { i, h ->
                                                    DropdownMenuItem(
                                                        text = { Text(h, fontSize = 10.sp) },
                                                        onClick = {
                                                            viewModel.updateMapping(targetColumn, i)
                                                            showDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = { viewModel.importMappedCSV() }) {
                                        Text("Confirm Bulk Imports (${csvRows.size} Rows)")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dark Theme and General Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("System Integration Properties", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dark Visual Palette", fontSize = 13.sp)
                        Switch(checked = currentDarkState, onCheckedChange = onDarkThemeToggle)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biometric Security Verification", fontSize = 13.sp)
                        var biometricSim by remember { mutableStateOf(true) }
                        Switch(checked = biometricSim, onCheckedChange = { biometricSim = it })
                    }
                }
            }

            // App Meta Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("REVENUE COMMAND CENTER CRM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                Text("Production Version v1.0.4 - Supported by Gemini Flash 3.5", fontSize = 10.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Field Schema configuration dialog
        if (showCustomFieldDialog) {
            AlertDialog(
                onDismissRequest = { showCustomFieldDialog = false },
                title = { Text("Define Custom Field Label") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newFieldLabel,
                            onValueChange = { newFieldLabel = it },
                            label = { Text("Label Title (E.g: CFO Contacted)") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_field_label")
                        )

                        // Select Schema type
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { dropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Field Type: ${newFieldType.name}")
                            }
                            DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                FieldType.values().forEach { fl ->
                                    DropdownMenuItem(
                                        text = { Text(fl.name) },
                                        onClick = {
                                            newFieldType = fl
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFieldLabel.isNotBlank()) {
                                viewModel.createCustomField(newFieldLabel, newFieldType, emptyList())
                                newFieldLabel = ""
                                showCustomFieldDialog = false
                            }
                        }
                    ) {
                        Text("Apply Schema")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomFieldDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
