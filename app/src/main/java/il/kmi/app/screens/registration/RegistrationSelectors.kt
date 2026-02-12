@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package il.kmi.app.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * בורר אזור/סניף/קבוצה עבור מתאמן.
 * - לא מנהל state פנימי משמעותי, אלא עובד מול ה־state של ההורה.
 * - ההורה ממשיך להחזיק את selectedRegion/Branch/Group ואת דגלי השגיאות.
 */
@Composable
fun RegionBranchGroupPicker(
    selectedRegion: String,
    selectedBranch: String,
    selectedGroup: String,
    onRegionChange: (String) -> Unit,
    onBranchChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    regionError: Boolean,
    branchError: Boolean,
    groupError: Boolean,
    branchesByRegion: Map<String, List<String>>,
    groupsByBranch: Map<String, List<String>>,
    fieldWidth: Float,
    fieldHeight: Dp
) {
    // מצבים לפתיחת התפריטים
    var regionExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var groupExpanded  by remember { mutableStateOf(false)  }

    val regions = remember { branchesByRegion.keys.toList() }
    val branchesForRegion = branchesByRegion[selectedRegion].orEmpty()
    val groupsForBranch   = groupsByBranch[selectedBranch].orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(fieldWidth),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // === אזור ===
        ExposedDropdownMenuBox(
            expanded = regionExpanded,
            onExpandedChange = { regionExpanded = !regionExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRegion,
                onValueChange = { /* readOnly */ },
                readOnly = true,
                label = { Text("בחר/י אזור", color = Color.Black) },
                isError = regionError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = fieldHeight)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                placeholder = { Text("אזור") }
            )
            ExposedDropdownMenu(
                expanded = regionExpanded,
                onDismissRequest = { regionExpanded = false }
            ) {
                regions.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            regionExpanded = false
                            if (region != selectedRegion) {
                                onRegionChange(region)   // מאפס סניף/קבוצה למעלה אצלך
                            }
                        }
                    )
                }
            }
        }
        if (regionError) Text("יש לבחור אזור", color = MaterialTheme.colorScheme.error)

        // === סניף ===
        ExposedDropdownMenuBox(
            expanded = branchExpanded,
            onExpandedChange = {
                // לא לאפשר פתיחה כשאין אזור
                if (selectedRegion.isNotBlank()) branchExpanded = !branchExpanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedBranch,
                onValueChange = { /* readOnly */ },
                readOnly = true,
                label = { Text("בחר/י סניף", color = Color.Black) },
                isError = branchError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = fieldHeight)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                placeholder = { Text(if (selectedRegion.isBlank()) "בחר/י אזור קודם" else "סניף") }
            )
            ExposedDropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false }
            ) {
                branchesForRegion.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch) },
                        onClick = {
                            branchExpanded = false
                            if (branch != selectedBranch) {
                                onBranchChange(branch)   // מאפס קבוצה אצלך
                            }
                        }
                    )
                }
            }
        }
        if (branchError) Text("יש לבחור סניף", color = MaterialTheme.colorScheme.error)

        // === קבוצה (כמו קומבו) ===
        ExposedDropdownMenuBox(
            expanded = groupExpanded,
            onExpandedChange = {
                if (selectedBranch.isNotBlank()) groupExpanded = !groupExpanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGroup,
                onValueChange = { /* readOnly */ },
                readOnly = true,
                label = { Text("בחר/י קבוצה", color = Color.Black) },
                isError = groupError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = fieldHeight)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                placeholder = { Text(if (selectedBranch.isBlank()) "בחר/י סניף קודם" else "קבוצה") }
            )
            ExposedDropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false }
            ) {
                groupsForBranch.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            groupExpanded = false
                            if (group != selectedGroup) onGroupChange(group)
                        }
                    )
                }
            }
        }
        if (groupError) Text("יש לבחור קבוצה", color = MaterialTheme.colorScheme.error)
    }
}





