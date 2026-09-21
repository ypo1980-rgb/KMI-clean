package il.kmi.app.screens

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.TrainingAttendanceForecast
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingData
import il.kmi.app.training.TrainingOverride
import il.kmi.app.training.TrainingOverrideRepository
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.scaledIconSize
import il.yuval.ui.theme.kmiOnSuccessContainerColor
import il.yuval.ui.theme.kmiSuccessColor
import il.yuval.ui.theme.kmiSuccessContainerColor
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
internal fun rememberHomeCoachAttendanceForecast(
    isCoach: Boolean,
    branch: String,
    group: String,
    trainingDate: LocalDate
): TrainingAttendanceForecast {

    val context =
        LocalContext.current

    val attendanceRepository =
        remember(context) {
            AttendanceRepository.get(
                context.applicationContext as Application
            )
        }

    val attendanceForecastFlow =
        remember(
            isCoach,
            branch,
            group,
            trainingDate
        ) {
            if (
                isCoach &&
                branch.isNotBlank() &&
                group.isNotBlank()
            ) {
                attendanceRepository
                    .attendanceForecastForDay(
                        branch = branch,
                        groupKey = group,
                        date = trainingDate
                    )
            } else {
                null
            }
        }

    return if (attendanceForecastFlow != null) {
        attendanceForecastFlow
            .collectAsState(
                initial =
                    TrainingAttendanceForecast()
            )
            .value
    } else {
        TrainingAttendanceForecast()
    }
}

internal data class HomeCoachOccurrenceItem(
    val training: TrainingData,
    val branch: String,
    val group: String,
    val activeOverride: TrainingOverride?
)

@Composable
internal fun SyncHomeCoachTrainingOccurrences(
    isCoach: Boolean,
    currentUid: String?,
    items: List<HomeCoachOccurrenceItem>
) {
    LaunchedEffect(
        isCoach,
        currentUid,
        items
    ) {
        if (!isCoach) {
            return@LaunchedEffect
        }

        val uid =
            currentUid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            return@LaunchedEffect
        }

        items.forEach { item ->
            TrainingOverrideRepository
                .syncTrainingOccurrence(
                    training = item.training,
                    branch = item.branch,
                    group = item.group,
                    activeOverride = item.activeOverride,
                    onResult = { _, _ ->
                        /*
                         * כשל בסנכרון occurrence
                         * אינו מפיל את מסך הבית.
                         */
                    }
                )
        }
    }
}

@Composable
internal fun HomeCoachAttendanceForecastContent(
    attendanceForecast: TrainingAttendanceForecast,
    isEnglish: Boolean
) {
    Spacer(
        Modifier.height(8.dp)
    )

    HorizontalDivider(
        modifier =
            Modifier.fillMaxWidth(),
        thickness =
            1.dp,
        color =
            MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.18f)
    )

    Spacer(
        Modifier.height(5.dp)
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(6.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        HomeCoachAttendanceForecastCard(
            value =
                attendanceForecast.comingCount,
            title =
                if (isEnglish) {
                    "Coming"
                } else {
                    "מגיעים"
                },
            containerColor =
                kmiSuccessContainerColor()
                    .copy(alpha = 0.96f),
            contentColor =
                kmiOnSuccessContainerColor(),
            borderColor =
                kmiSuccessColor(),
            modifier =
                Modifier.weight(1f)
        )

        HomeCoachAttendanceForecastCard(
            value =
                attendanceForecast.notComingCount,
            title =
                if (isEnglish) {
                    "Not coming"
                } else {
                    "לא מגיעים"
                },
            containerColor =
                MaterialTheme
                    .colorScheme
                    .errorContainer
                    .copy(alpha = 0.96f),
            contentColor =
                MaterialTheme
                    .colorScheme
                    .onErrorContainer,
            borderColor =
                MaterialTheme
                    .colorScheme
                    .error,
            modifier =
                Modifier.weight(1f)
        )

        HomeCoachAttendanceForecastCard(
            value =
                attendanceForecast.noResponseCount,
            title =
                if (isEnglish) {
                    "Pending"
                } else {
                    "טרם סימנו"
                },
            containerColor =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.82f),
            contentColor =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            borderColor =
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(alpha = 0.55f),
            modifier =
                Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeCoachAttendanceForecastCard(
    value: Int,
    title: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier =
            modifier.heightIn(
                min = 64.dp
            ),
        shape =
            RoundedCornerShape(16.dp),
        color =
            containerColor,
        border =
            BorderStroke(
                width = 1.dp,
                color = borderColor
            ),
        tonalElevation =
            0.dp,
        shadowElevation =
            0.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 6.dp,
                        vertical = 6.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text =
                    value.toString(),
                style =
                    KmiTypography
                        .secondary
                        .copy(
                            fontWeight =
                                FontWeight.Black
                        ),
                color =
                    contentColor,
                textAlign =
                    TextAlign.Center,
                maxLines =
                    1
            )

            Spacer(
                Modifier.height(1.dp)
            )

            Text(
                text =
                    title,
                style =
                    KmiTypography
                        .caption
                        .copy(
                            fontWeight =
                                FontWeight.Bold
                        ),
                color =
                    contentColor.copy(
                        alpha = 0.95f
                    ),
                textAlign =
                    TextAlign.Center,
                maxLines =
                    1
            )
        }
    }
}

@Composable
internal fun BoxScope.HomeCoachTrainingEditButton(
    isEnglish: Boolean,
    onManageTraining: () -> Unit
) {
    val haptic =
        rememberHapticsGlobal()

    val clickSound =
        rememberClickSound()

    /*
     * האייקון ממוקם בפינה השמאלית הפיזית
     * גם בעברית וגם באנגלית.
     *
     * ההזזה גורמת לכך שחלק מהעיגול נמצא
     * בתוך הכרטיס וחלקו מחוץ לכרטיס.
     */
    val editButtonAlignment =
        if (isEnglish) {
            Alignment.TopStart
        } else {
            Alignment.TopEnd
        }

    Surface(
        modifier =
            Modifier
                .align(
                    editButtonAlignment
                )
                .absoluteOffset(
                    x = (-10).dp,
                    y = (-10).dp
                )
                .size(
                    scaledIconSize(46.dp)
                )
                .zIndex(3f),
        shape =
            CircleShape,
        color =
            MaterialTheme
                .colorScheme
                .primaryContainer,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.55f)
            ),
        tonalElevation =
            0.dp,
        shadowElevation =
            0.dp
    ) {
        IconButton(
            onClick = {
                clickSound()
                haptic(true)
                onManageTraining()
            },
            modifier =
                Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector =
                    Icons.Filled.EditNote,
                contentDescription =
                    if (isEnglish) {
                        "Change or cancel training"
                    } else {
                        "שינוי או ביטול אימון"
                    },
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier =
                    Modifier.size(
                        scaledIconSize(23.dp)
                    )
            )
        }
    }
}

internal data class HomeCoachAssignmentsState(
    val coachName: String,
    val branchType: String,
    val branchGroupPairs: List<Pair<String, String>>
) {
    val isAbroadBranch: Boolean
        get() =
            branchType == "abroad"
}

@Composable
internal fun rememberHomeCoachAssignmentsState(
    currentUid: String?,
    userSp: SharedPreferences
): HomeCoachAssignmentsState {

    val context =
        LocalContext.current

    var groupsRefreshTick by remember {
        mutableIntStateOf(0)
    }

    var branchesRefreshTick by remember {
        mutableIntStateOf(0)
    }

    var coachName by remember(userSp) {
        mutableStateOf(
            userSp
                .getString(
                    "coach_name",
                    ""
                )
                .orEmpty()
        )
    }

    DisposableEffect(userSp) {
        val listener =
            SharedPreferences
                .OnSharedPreferenceChangeListener { _,
                                                    key ->

                    when (key) {
                        "groups_json",
                        "selected_groups",
                        "groups",
                        "age_groups",
                        "age_group",
                        "group" -> {
                            groupsRefreshTick++
                        }

                        "branches_json",
                        "selected_branches",
                        "branches",
                        "branch",
                        "branch2",
                        "branch3",
                        "branch_type",
                        "coach_branch_assignments_json" -> {
                            branchesRefreshTick++
                        }

                        "coach_name" -> {
                            coachName =
                                userSp
                                    .getString(
                                        "coach_name",
                                        ""
                                    )
                                    .orEmpty()
                        }
                    }
                }

        userSp.registerOnSharedPreferenceChangeListener(
            listener
        )

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(
                listener
            )
        }
    }

    LaunchedEffect(
        currentUid,
        userSp
    ) {
        val uid =
            currentUid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            return@LaunchedEffect
        }

        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                fun listFromFirestore(
                    listKey: String,
                    csvKey: String,
                    fallbackKey: String
                ): List<String> {
                    val storedList =
                        (document.get(listKey) as? List<*>)
                            ?.mapNotNull { value ->
                                value
                                    ?.toString()
                                    ?.trim()
                            }
                            ?.filter { value ->
                                value.isNotBlank()
                            }
                            ?.distinct()
                            .orEmpty()

                    if (storedList.isNotEmpty()) {
                        return@listFromFirestore storedList
                    }

                    val storedText =
                        document
                            .getString(csvKey)
                            ?.takeIf { value ->
                                value.isNotBlank()
                            }
                            ?: document
                                .getString(fallbackKey)
                                .orEmpty()

                    return@listFromFirestore storedText
                        .split(
                            ',',
                            ';',
                            '|',
                            '\n'
                        )
                        .map { value ->
                            value.trim()
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }
                        .distinct()
                }

                val remoteBranches =
                    listFromFirestore(
                        listKey = "branches",
                        csvKey = "branchesCsv",
                        fallbackKey = "branch"
                    )

                val remoteGroups =
                    listFromFirestore(
                        listKey = "groups",
                        csvKey = "groupsCsv",
                        fallbackKey = "primaryGroup"
                    )

                if (
                    remoteBranches.isEmpty() &&
                    remoteGroups.isEmpty()
                ) {
                    return@addOnSuccessListener
                }

                val remoteActiveBranch =
                    document
                        .getString("activeBranch")
                        ?.takeIf { branch ->
                            branch.isNotBlank() &&
                                    branch in remoteBranches
                        }
                        ?: remoteBranches
                            .firstOrNull()
                            .orEmpty()

                val remoteActiveGroup =
                    document
                        .getString("activeGroup")
                        ?.takeIf { group ->
                            group.isNotBlank() &&
                                    group in remoteGroups
                        }
                        ?: remoteGroups
                            .firstOrNull()
                            .orEmpty()

                val branchesCsv =
                    remoteBranches.joinToString(", ")

                val groupsCsv =
                    remoteGroups.joinToString(", ")

                val branchesJson =
                    JSONArray(
                        remoteBranches
                    ).toString()

                val groupsJson =
                    JSONArray(
                        remoteGroups
                    ).toString()

                userSp.edit {
                    remove("branches")
                    remove("selected_branches")
                    remove("groups")
                    remove("selected_groups")

                    putString(
                        "branch",
                        branchesCsv
                    )
                    putString(
                        "branches",
                        branchesCsv
                    )
                    putString(
                        "branches_json",
                        branchesJson
                    )
                    putString(
                        "selected_branches",
                        branchesCsv
                    )
                    putString(
                        "active_branch",
                        remoteActiveBranch
                    )

                    putString(
                        "age_groups",
                        groupsCsv
                    )
                    putString(
                        "groups",
                        groupsCsv
                    )
                    putString(
                        "groups_json",
                        groupsJson
                    )
                    putString(
                        "selected_groups",
                        groupsCsv
                    )
                    putString(
                        "age_group",
                        remoteGroups
                            .firstOrNull()
                            .orEmpty()
                    )
                    putString(
                        "group",
                        remoteGroups
                            .firstOrNull()
                            .orEmpty()
                    )
                    putString(
                        "active_group",
                        remoteActiveGroup
                    )
                }

                branchesRefreshTick++
                groupsRefreshTick++
            }
            .addOnFailureListener {
                /*
                 * כשל זמני בטעינת הפרופיל אינו
                 * מוחק את השיוכים המקומיים.
                 */
            }
    }

    val branchType =
        remember(
            userSp,
            branchesRefreshTick
        ) {
            userSp
                .getString(
                    "branch_type",
                    "israel"
                )
                .orEmpty()
                .ifBlank {
                    "israel"
                }
        }

    val branchAssignments =
        remember(
            userSp,
            branchesRefreshTick,
            groupsRefreshTick
        ) {
            CoachBranchAssignmentsCodec.decode(
                userSp.getString(
                    "coach_branch_assignments_json",
                    ""
                )
            )
        }

    val branchGroupPairs =
        remember(
            branchAssignments,
            context
        ) {
            branchAssignments
                .flatMap { assignment ->
                    assignment.groups.map { groupName ->
                        assignment.branch.trim() to
                                groupName.trim()
                    }
                }
                .filter { (branchName, groupName) ->

                    branchName.isNotBlank() &&
                            groupName.isNotBlank()
                }
                .filter { (branchName, groupName) ->

                    val databaseGroups =
                        KmiDatabaseProvider
                            .branchByName(
                                context,
                                branchName
                            )
                            ?.trainingDays
                            ?.map { day ->
                                TrainingCatalog
                                    .normalizeGroupName(
                                        day.groupHe
                                    )
                                    .ifBlank {
                                        day.groupHe.trim()
                                    }
                            }
                            ?.filter { group ->
                                group.isNotBlank()
                            }
                            ?.distinct()
                            .orEmpty()

                    val catalogGroups =
                        TrainingCatalog
                            .groupsForBranch(
                                branch = branchName,
                                isEnglish = false
                            )
                            .map { group ->
                                TrainingCatalog
                                    .normalizeGroupName(
                                        group
                                    )
                                    .ifBlank {
                                        group.trim()
                                    }
                            }
                            .filter { group ->
                                group.isNotBlank()
                            }
                            .distinct()

                    val validGroups =
                        databaseGroups
                            .takeIf { groups ->
                                groups.isNotEmpty()
                            }
                            ?: catalogGroups

                    val wantedGroup =
                        TrainingCatalog
                            .normalizeGroupName(
                                groupName
                            )
                            .ifBlank {
                                groupName.trim()
                            }

                    validGroups.any { validGroup ->
                        validGroup.equals(
                            wantedGroup,
                            ignoreCase = true
                        )
                    }
                }
                .distinct()
        }

    return HomeCoachAssignmentsState(
        coachName = coachName,
        branchType = branchType,
        branchGroupPairs = branchGroupPairs
    )
}

internal fun openHomeCoachTrainingManagement(
    training: TrainingData,
    occurrenceKey: String,
    branch: String,
    group: String,
    activeOverride: TrainingOverride?,
    coachName: String,
    fallbackName: String,
    isEnglish: Boolean
) {
    val timeFormatter =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        )

    val dateFormatter =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

    val displayedStartTime =
        if (
            activeOverride
                ?.hasChangedTime == true
        ) {
            timeFormatter.format(
                Date(
                    activeOverride
                        .effectiveStartMillis
                )
            )
        } else {
            training.start
        }

    val displayedEndTime =
        if (
            activeOverride
                ?.hasChangedTime == true
        ) {
            timeFormatter.format(
                Date(
                    activeOverride
                        .effectiveEndMillis
                )
            )
        } else {
            training.end
        }

    val changedByName =
        coachName
            .trim()
            .ifBlank {
                fallbackName.trim()
            }
            .ifBlank {
                FirebaseAuth
                    .getInstance()
                    .currentUser
                    ?.displayName
                    ?.trim()
                    .orEmpty()
            }
            .ifBlank {
                if (isEnglish) {
                    "Coach"
                } else {
                    "מאמן"
                }
            }

    TrainingManagementNavigationStore.open(
        TrainingManagementRequest(
            uiData =
                TrainingManagementUiData(
                    occurrenceKey =
                        occurrenceKey,
                    place =
                        training.place,
                    branch =
                        branch,
                    group =
                        group,
                    dateText =
                        dateFormatter.format(
                            training.cal.time
                        ),
                    startTime =
                        displayedStartTime,
                    endTime =
                        displayedEndTime
                ),
            training =
                training,
            branch =
                branch,
            group =
                group,
            changedByName =
                changedByName
        )
    )
}
