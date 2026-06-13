package il.kmi.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.database.KmiDatabaseRepository
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.tasks.await

private data class NetworkCoachInfo(
    val id: String,
    val active: Boolean = true,
    val sortOrder: Int = 999,
    val nameHe: String,
    val nameEn: String,
    val roleHe: String,
    val roleEn: String,
    val rankHe: String,
    val rankEn: String,
    val experienceHe: String,
    val experienceEn: String,
    val trainingHe: String,
    val trainingEn: String,
    val certificationsHe: List<String>,
    val certificationsEn: List<String>,
    val branchesHe: List<String>,
    val branchesEn: List<String>,
    val descriptionHe: String,
    val descriptionEn: String
)

private val hebrewLetters = listOf(
    "הכל",
    "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט",
    "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ",
    "ק", "ר", "ש", "ת"
)

private fun fallbackCoach(
    id: String,
    sortOrder: Int,
    nameHe: String,
    nameEn: String = nameHe,
    roleHe: String = "מאמן ברשת ק.מ.י",
    roleEn: String = "K.A.M.I Network Coach",
    rankHe: String = "דרגה תעודכן בהמשך",
    rankEn: String = "Rank will be updated",
    experienceHe: String = "ותק מקצועי יעודכן בהמשך",
    experienceEn: String = "Professional experience will be updated",
    trainingHe: String = "הכשרות מקצועיות יעודכנו בהמשך",
    trainingEn: String = "Professional training will be updated",
    certificationsHe: List<String> = listOf("הסמכות יעודכנו בהמשך"),
    certificationsEn: List<String> = listOf("Certifications will be updated"),
    branchesHe: List<String> = listOf("רשת ק.מ.י"),
    branchesEn: List<String> = listOf("K.A.M.I Network"),
    descriptionHe: String = "מידע מקצועי על המאמן יעודכן בהמשך דרך Firestore.",
    descriptionEn: String = "Professional coach information will be updated later through Firestore."
): NetworkCoachInfo {
    return NetworkCoachInfo(
        id = id,
        sortOrder = sortOrder,
        nameHe = nameHe,
        nameEn = nameEn,
        roleHe = roleHe,
        roleEn = roleEn,
        rankHe = rankHe,
        rankEn = rankEn,
        experienceHe = experienceHe,
        experienceEn = experienceEn,
        trainingHe = trainingHe,
        trainingEn = trainingEn,
        certificationsHe = certificationsHe,
        certificationsEn = certificationsEn,
        branchesHe = branchesHe,
        branchesEn = branchesEn,
        descriptionHe = descriptionHe,
        descriptionEn = descriptionEn
    )
}

private val fallbackNetworkCoaches = listOf(
    fallbackCoach(
        id = "avi_avisidon",
        sortOrder = 1,
        nameHe = "אבי אביסידון",
        nameEn = "Avi Avisidon",
        roleHe = "ראש השיטה",
        roleEn = "Head of the Method",
        rankHe = "חגורה שחורה דאן 10",
        rankEn = "Black Belt Dan 10",
        experienceHe = "מעל 40 שנות ותק",
        experienceEn = "Over 40 years of experience",
        trainingHe = "הכשרת מדריכים, הדרכת מאמנים ופיתוח מקצועי",
        trainingEn = "Instructor training, coach mentoring and professional development",
        certificationsHe = listOf("ראש השיטה", "הכשרת מדריכים", "פיתוח מקצועי"),
        certificationsEn = listOf("Head of the Method", "Instructor Training", "Professional Development"),
        descriptionHe = "ראש שיטת ק.מ.י ומוביל מקצועי ברשת.",
        descriptionEn = "Head of the K.A.M.I method and professional leader of the network."
    ),
    fallbackCoach(
        id = "itzik_biton",
        sortOrder = 2,
        nameHe = "איציק ביטון",
        nameEn = "Itzik Biton",
        roleHe = "מאמן בכיר",
        roleEn = "Senior Coach",
        rankHe = "חגורה שחורה",
        rankEn = "Black Belt",
        experienceHe = "אימון והדרכת מתאמנים ברשת",
        experienceEn = "Training and coaching students across the network",
        trainingHe = "הדרכת קבוצות, הכנה למבחנים וליווי מתאמנים",
        trainingEn = "Group coaching, exam preparation and trainee development",
        certificationsHe = listOf("מאמן בכיר", "הכשרת מתאמנים", "ליווי מקצועי"),
        certificationsEn = listOf("Senior Coach", "Trainee Development", "Professional Mentoring")
    ),
    fallbackCoach(
        id = "eli_levshtein",
        sortOrder = 3,
        nameHe = "אלי לבשטיין",
        nameEn = "Eli Levshtein"
    ),
    fallbackCoach(
        id = "akiva_nalkin",
        sortOrder = 4,
        nameHe = "עקיבא נלקין",
        nameEn = "Akiva Nalkin"
    ),
    fallbackCoach(
        id = "peri_gonen",
        sortOrder = 5,
        nameHe = "פרי גונן",
        nameEn = "Peri Gonen"
    ),
    fallbackCoach(
        id = "shlomi_avisidon",
        sortOrder = 6,
        nameHe = "שלומי אבסידון",
        nameEn = "Shlomi Avisidon"
    ),
    fallbackCoach(
        id = "rafi_nachum",
        sortOrder = 7,
        nameHe = "רפי נחום",
        nameEn = "Rafi Nachum"
    ),
    fallbackCoach(
        id = "barak_shapira",
        sortOrder = 8,
        nameHe = "ברק שפירא",
        nameEn = "Barak Shapira"
    ),
    fallbackCoach(
        id = "zohar_bayit",
        sortOrder = 9,
        nameHe = "זהר בית",
        nameEn = "Zohar Bayit"
    ),
    fallbackCoach(
        id = "maor_hakak",
        sortOrder = 10,
        nameHe = "מאור חקאק",
        nameEn = "Maor Hakak"
    ),
    fallbackCoach(
        id = "ibrahim_tokgoz",
        sortOrder = 11,
        nameHe = "איברהים טוקגוז",
        nameEn = "Ibrahim Tokgoz"
    ),
    fallbackCoach(
        id = "roi_shachar",
        sortOrder = 12,
        nameHe = "רועי שחר",
        nameEn = "Roi Shachar"
    ),
    fallbackCoach(
        id = "ofer_golan",
        sortOrder = 13,
        nameHe = "עופר גולן",
        nameEn = "Ofer Golan"
    ),
    fallbackCoach(
        id = "zigmund_leitner",
        sortOrder = 14,
        nameHe = "זיגמונד לייטנר",
        nameEn = "Zigmund Leitner"
    ),
    fallbackCoach(
        id = "yonatan_melsa",
        sortOrder = 15,
        nameHe = "יונתן מלסה",
        nameEn = "Yonatan Melsa"
    ),
    fallbackCoach(
        id = "gal_hajaj",
        sortOrder = 16,
        nameHe = "גל חג'ג",
        nameEn = "Gal Hajaj"
    ),
    fallbackCoach(
        id = "adam_holtzman",
        sortOrder = 17,
        nameHe = "אדם הולצמן",
        nameEn = "Adam Holtzman"
    )
)

private fun KmiDatabaseRepository.KmiNetworkCoach.toNetworkCoachInfo(): NetworkCoachInfo {
    return NetworkCoachInfo(
        id = id,
        active = active,
        sortOrder = sortOrder,
        nameHe = nameHe,
        nameEn = nameEn,
        roleHe = roleHe,
        roleEn = roleEn,
        rankHe = rankHe,
        rankEn = rankEn,
        experienceHe = experienceHe,
        experienceEn = experienceEn,
        trainingHe = trainingHe,
        trainingEn = trainingEn,
        certificationsHe = certificationsHe.ifEmpty { listOf("הסמכות יעודכנו בהמשך") },
        certificationsEn = certificationsEn.ifEmpty { listOf("Certifications will be updated") },
        branchesHe = branchesHe.ifEmpty { listOf("רשת ק.מ.י") },
        branchesEn = branchesEn.ifEmpty { listOf("K.A.M.I Network") },
        descriptionHe = descriptionHe,
        descriptionEn = descriptionEn
    )
}

private fun DocumentSnapshot.toNetworkCoachInfo(): NetworkCoachInfo? {
    fun s(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            getString(key)?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    fun b(vararg keys: String): Boolean {
        return keys.firstNotNullOfOrNull { key ->
            getBoolean(key)
        } ?: true
    }

    fun i(vararg keys: String): Int {
        return keys.firstNotNullOfOrNull { key ->
            getLong(key)?.toInt()
        } ?: 999
    }

    fun list(vararg keys: String): List<String> {
        val raw = keys.firstNotNullOfOrNull { key ->
            get(key)
        }

        return when (raw) {
            is List<*> -> raw
                .mapNotNull { it?.toString()?.trim() }
                .filter { it.isNotBlank() }

            is String -> raw
                .split(",", ";", "|", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            else -> emptyList()
        }
    }

    val nameHe = s("nameHe", "fullNameHe", "name", "fullName", "displayName")
    val nameEn = s("nameEn", "fullNameEn", "englishName").ifBlank { nameHe }

    if (nameHe.isBlank() && nameEn.isBlank()) return null

    return NetworkCoachInfo(
        id = id,
        active = b("active", "isActive"),
        sortOrder = i("sortOrder", "order"),
        nameHe = nameHe.ifBlank { nameEn },
        nameEn = nameEn.ifBlank { nameHe },
        roleHe = s("roleHe", "titleHe", "role").ifBlank { "מאמן" },
        roleEn = s("roleEn", "titleEn").ifBlank { "Coach" },
        rankHe = s("rankHe", "beltHe", "rank").ifBlank { "—" },
        rankEn = s("rankEn", "beltEn").ifBlank { "—" },
        experienceHe = s("experienceHe", "seniorityHe", "experience").ifBlank { "—" },
        experienceEn = s("experienceEn", "seniorityEn").ifBlank { "—" },
        trainingHe = s("trainingHe", "educationHe", "training").ifBlank { "—" },
        trainingEn = s("trainingEn", "educationEn").ifBlank { "—" },
        certificationsHe = list("certificationsHe", "certifications", "certsHe"),
        certificationsEn = list("certificationsEn", "certsEn"),
        branchesHe = list("branchesHe", "branches", "branchHe", "branch"),
        branchesEn = list("branchesEn", "branchEn"),
        descriptionHe = s("descriptionHe", "bioHe", "description", "bio").ifBlank { "—" },
        descriptionEn = s("descriptionEn", "bioEn").ifBlank { "—" }
    )
}

private suspend fun loadNetworkCoachesFromFirestore(
    context: android.content.Context
): List<NetworkCoachInfo> {
    val localCoaches = runCatching {
        KmiDatabaseProvider.networkCoaches(context)
            .map { it.toNetworkCoachInfo() }
    }.getOrDefault(emptyList())

    val docs = Firebase.firestore
        .collection("network_coaches")
        .get()
        .await()
        .documents

    val serverCoaches = docs
        .mapNotNull { it.toNetworkCoachInfo() }
        .filter { it.active }

    // סדר המיזוג:
    // 1. fallback פנימי בקוד
    // 2. network_coaches.json
    // 3. Firestore
    // כל שכבה מחליפה לפי id.
    val mergedById = linkedMapOf<String, NetworkCoachInfo>()

    fallbackNetworkCoaches.forEach { coach ->
        mergedById[coach.id] = coach
    }

    localCoaches.forEach { coach ->
        mergedById[coach.id] = coach
    }

    serverCoaches.forEach { coach ->
        mergedById[coach.id] = coach
    }

    return mergedById.values
        .filter { it.active }
        .sortedWith(
            compareBy<NetworkCoachInfo> { it.sortOrder }
                .thenBy { it.nameHe }
        )
}

private fun firstHebrewLetter(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return ""

    val first = clean.first().toString()
    return if (first in hebrewLetters) first else ""
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutNetworkCoachesScreen(
    isEnglish: Boolean,
    onClose: () -> Unit,
    onHome: () -> Unit = {},
    onOpenExercise: ((String) -> Unit)? = null
) {
    var coaches by remember { mutableStateOf<List<NetworkCoachInfo>>(fallbackNetworkCoaches) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var selectedLetter by remember { mutableStateOf("הכל") }
    var selectedLetterExpanded by remember { mutableStateOf(false) }
    var selectedCoachId by remember { mutableStateOf<String?>(null) }

    val ctx = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(ctx) {
        isLoading = true
        loadError = null

        runCatching {
            loadNetworkCoachesFromFirestore(ctx)
        }.onSuccess { loaded ->
            coaches = loaded.ifEmpty { fallbackNetworkCoaches }
        }.onFailure { error ->
            val localOnly = runCatching {
                KmiDatabaseProvider.networkCoaches(ctx)
                    .map { it.toNetworkCoachInfo() }
            }.getOrDefault(emptyList())

            coaches = localOnly.ifEmpty { fallbackNetworkCoaches }
            loadError = error.message
        }

        isLoading = false
    }

    val filteredCoaches = remember(coaches, selectedLetter) {
        if (selectedLetter == "הכל") {
            coaches
        } else {
            coaches.filter { coach ->
                firstHebrewLetter(coach.nameHe) == selectedLetter
            }
        }
    }

    val selectedCoach = remember(filteredCoaches, selectedCoachId) {
        filteredCoaches.firstOrNull { it.id == selectedCoachId }
            ?: filteredCoaches.firstOrNull()
    }

    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    val fieldContainerColor = Color(0xFFDCE3F1)
    val fieldTextColor = Color(0xFF13213F)
    val fieldLabelColor = Color(0xFF52627A)
    val fieldBorderColor = Color(0xFFB8C4D9)

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr("אודות המאמנים ברשת", "About Network Coaches"),
                centerTitle = true,
                showMenu = true,
                onBack = null,

                // ✅ מפעיל את אייקון הבית בסרגל האייקונים הצדדי
                onHome = onHome,

                // ✅ מאפשר פתיחת תרגיל מתוך החיפוש הגלובלי אם הניווט מועבר מבחוץ
                onOpenExercise = onOpenExercise,

                showBottomActions = true,
                showRoleBadge = true,
                showModePill = true,

                // ✅ החיפוש בסרגל הצדדי פעיל
                lockSearch = false,

                // ✅ הבית פעיל במסך מאמנים ברשת
                lockHome = false,

                // ✅ לא מציגים בית/חיפוש בכותרת העליונה עצמה
                // הם נשארים רק בסרגל האייקונים הצדדי
                showTopHome = false,
                showTopSearch = false,
                showTopShare = false
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF4FF),
                            Color(0xFFB7DDF7),
                            Color(0xFF1F78B4),
                            Color(0xFF062B4A)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = horizontal
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = horizontal
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 4 }
                    ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF314875)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = horizontal
                        ) {
                            Text(
                                text = tr(
                                    "בחרו אות, ואז בחרו מאמן כדי לראות דרגה, הכשרה, ותק, הסמכות ומידע מקצועי.",
                                    "Choose a letter, then select a coach to view rank, training, experience, certifications and professional details."
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            loadError?.let {
                                Text(
                                    text = tr(
                                        "לא הצלחנו לטעון מהשרת כרגע, מוצגים נתוני ברירת מחדל.",
                                        "Could not load from the server right now. Showing default data."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD6A5),
                                    textAlign = textAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = tr("סינון לפי אות", "Filter by Hebrew letter"),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = selectedLetterExpanded,
                                onExpandedChange = {
                                    selectedLetterExpanded = !selectedLetterExpanded
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = if (selectedLetter == "הכל") {
                                        tr("הכל", "All")
                                    } else {
                                        selectedLetter
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = {
                                        Text(
                                            text = tr("בחרו אות", "Choose letter"),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = selectedLetterExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        color = fieldTextColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = textAlign
                                    ),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedTextColor = fieldTextColor,
                                        unfocusedTextColor = fieldTextColor,
                                        focusedBorderColor = fieldBorderColor,
                                        unfocusedBorderColor = fieldBorderColor,
                                        focusedContainerColor = fieldContainerColor,
                                        unfocusedContainerColor = fieldContainerColor,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White,
                                        focusedTrailingIconColor = fieldLabelColor,
                                        unfocusedTrailingIconColor = fieldLabelColor,
                                        cursorColor = fieldTextColor
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = selectedLetterExpanded,
                                    onDismissRequest = {
                                        selectedLetterExpanded = false
                                    },
                                    containerColor = Color(0xFF314875)
                                ) {
                                    hebrewLetters.forEach { letter ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (letter == "הכל") tr("הכל", "All") else letter,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = textAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            onClick = {
                                                selectedLetter = letter
                                                selectedCoachId = null
                                                selectedLetterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.16f))

                            Text(
                                text = tr("בחירת מאמן", "Choose coach"),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (filteredCoaches.isEmpty()) {
                                Text(
                                    text = tr(
                                        "אין מאמנים שמתחילים באות $selectedLetter",
                                        "No coaches found for this letter"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f),
                                    textAlign = textAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredCoaches.forEach { coach ->
                                        val selected = coach.id == selectedCoach?.id

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedCoachId = coach.id },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) Color(0xFF12B981) else fieldContainerColor,
                                            border = BorderStroke(
                                                width = if (selected) 1.6.dp else 1.dp,
                                                color = if (selected) Color(0xFFFFD166) else fieldBorderColor
                                            ),
                                            shadowElevation = if (selected) 8.dp else 0.dp,
                                            tonalElevation = if (selected) 4.dp else 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = if (selected) 16.dp else 14.dp,
                                                        vertical = if (selected) 13.dp else 12.dp
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (selected) {
                                                        Color.White.copy(alpha = 0.20f)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    border = if (selected) {
                                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.38f))
                                                    } else {
                                                        null
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (selected) Color(0xFF071A2E) else fieldLabelColor,
                                                        modifier = Modifier
                                                            .padding(if (selected) 5.dp else 0.dp)
                                                            .size(if (selected) 18.dp else 18.dp)
                                                    )
                                                }

                                                Spacer(Modifier.size(10.dp))

                                                Text(
                                                    text = if (isEnglish) coach.nameEn else coach.nameHe,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (selected) Color.White else fieldTextColor,
                                                    textAlign = textAlign,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectedCoach?.let { coach ->
                    CoachDetailsCard(
                        isEnglish = isEnglish,
                        coach = coach
                    )
                }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CoachDetailsCard(
    isEnglish: Boolean,
    coach: NetworkCoachInfo
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A3D66)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = horizontal
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = if (isEnglish) coach.nameEn else coach.nameHe,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isEnglish) coach.roleEn else coach.roleHe,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.size(12.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier
                            .padding(12.dp)
                            .size(30.dp)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.14f))

            CoachInfoRow(
                icon = Icons.Default.WorkspacePremium,
                label = tr("דרגה", "Rank"),
                value = if (isEnglish) coach.rankEn else coach.rankHe,
                isEnglish = isEnglish
            )

            CoachInfoRow(
                icon = Icons.Default.AccessTime,
                label = tr("ותק", "Experience"),
                value = if (isEnglish) coach.experienceEn else coach.experienceHe,
                isEnglish = isEnglish
            )

            CoachInfoRow(
                icon = Icons.Default.School,
                label = tr("הכשרה", "Training"),
                value = if (isEnglish) coach.trainingEn else coach.trainingHe,
                isEnglish = isEnglish
            )

            CoachInfoRow(
                icon = Icons.Default.Verified,
                label = tr("הסמכות", "Certifications"),
                value = if (isEnglish) {
                    coach.certificationsEn.ifEmpty { coach.certificationsHe }.joinToString(" • ").ifBlank { "—" }
                } else {
                    coach.certificationsHe.joinToString(" • ").ifBlank { "—" }
                },
                isEnglish = isEnglish
            )

            CoachInfoRow(
                icon = Icons.Default.Person,
                label = tr("סניפים / פעילות", "Branches / Activity"),
                value = if (isEnglish) {
                    coach.branchesEn.ifEmpty { coach.branchesHe }.joinToString(" • ").ifBlank { "—" }
                } else {
                    coach.branchesHe.joinToString(" • ").ifBlank { "—" }
                },
                isEnglish = isEnglish
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFDCE3F1),
                border = BorderStroke(1.dp, Color(0xFFB8C4D9))
            ) {
                Text(
                    text = if (isEnglish) coach.descriptionEn else coach.descriptionHe,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF13213F),
                    textAlign = textAlign,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun CoachInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isEnglish: Boolean
) {
    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFDCE3F1),
        border = BorderStroke(1.dp, Color(0xFFB8C4D9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF52627A),
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.size(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = horizontal
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF52627A),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF13213F),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}