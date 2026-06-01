@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import il.kmi.app.R
import kotlinx.coroutines.launch
import androidx.core.view.doOnPreDraw
import shareCurrentScreen
import androidx.compose.runtime.saveable.rememberSaveable
import il.kmi.app.ui.assistant.ui.AiAssistantDialog
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density

//===============================================================================

// ====== Colors & Theme ======
private val White        = Color(0xFFFFFFFF)
private val Ink950       = Color(0xFF0B1020)
private val Ink900       = Color(0xFF0F172A)
private val Ink800       = Color(0xFF172036)
private val Ink700       = Color(0xFF24304D)
private val Ink600       = Color(0xFF475569)
private val DividerCol   = Color(0x33FFFFFF)
private val AccentBlue   = Color(0xFF3B82F6)
private val AccentGreen  = Color(0xFF16A34A)

/** ערכת צבעים */
@Composable
fun KmiLightTheme(
    useGreenAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    val accent = if (useGreenAccent) AccentGreen else AccentBlue
    val scheme = lightColorScheme(
        primary = accent,
        onPrimary = White,
        surface = White,
        onSurface = Ink900,
        outline = DividerCol,
        secondary = Ink600,
        onSecondary = White,
        background = White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerCol)
    )
}

data class UiSearchResult(
    val id: String,
    val title: String,
    val subtitle: String?
)

// --- חתימה (כולל חדשים) ---
@Composable
fun KmiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onToggleLanguage: (() -> Unit)? = null,
    currentLang: String = "he",
    showMenu: Boolean = true,
    showFontQuick: Boolean = true,
    showRoleStatus: Boolean = true,
    showSettings: Boolean = true,
    showBottomActions: Boolean = true,
    showModePill: Boolean = true,
    modePillIsCoach: Boolean? = null,
    onShare: (() -> Unit)? = null,
    onOpenExercise: ((String) -> Unit)? = null,   // ← חדש (אופציונלי)
    onShareWhatsApp: (() -> Unit)? = null,
    onTts: (() -> Unit)? = null,
    onFont: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    whatsAppIconRes: Int? = null,
    accessibilityIconRes: Int? = null,
    onOpenDrawer: (() -> Unit)? = null,
    extraActions: @Composable RowScope.() -> Unit = {},
    onPickSearchResult: ((String) -> Unit)? = null,
    showLogoInBar: Boolean = false,
    logoRes: Int? = R.drawable.kami_logo,
    logoSize: Dp = 72.dp,
    centerTitle: Boolean = true,
    showRoleBadge: Boolean = true,
    lockSearch: Boolean = false,
    lockHome: Boolean = false,
    homeDisabledToast: String? = null,
    showCoachBroadcastFab: Boolean = true,
    requireRegistrationForCoachBroadcast: Boolean = true,
    onOpenCoachBroadcast: (() -> Unit)? = null,
    onSendCoachBroadcast: ((String) -> Unit)? = null,
    forceInternalCoachBroadcast: Boolean = true,
    useCloseIcon: Boolean = false,
    alignTitleEnd: Boolean = false,
    showTopHome: Boolean = true,
    showTopSearch: Boolean = true,

    // ברירת מחדל: לא מציגים אייקון שיתוף בכותרת העליונה.
    // השיתוף עדיין קיים בסרגל האייקונים התחתון.
    showTopShare: Boolean = false,

    isInsideAssistant: Boolean = false,
    onOpenAi: (() -> Unit)? = null
) {
    // 🔴 כאן היה רינדור מוקדם של CenterAlignedTopAppBar/TopAppBar – הורדנו אותו
    // כדי שלא תהיה כותרת כפולה. משלב זה והלאה נשאר הכול כמו אצלך.

    val ctx = LocalContext.current
    val density = LocalDensity.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val currentLangResolved = languageManager.getCurrentLanguage().code
    val isEnglish = currentLangResolved == "en"
    val rootView = LocalView.current
    var hideBottomForShare by remember { mutableStateOf(false) }

    fun runKmiShare() {
        if (onShare != null) {
            onShare()
        } else {
            hideBottomForShare = true
            val root = (ctx as? Activity)?.window?.decorView?.rootView ?: rootView

            root.post {
                root.doOnPreDraw {
                    root.post {
                        shareCurrentScreen(context = ctx, rootView = root)
                        hideBottomForShare = false
                    }
                }
            }
        }
    }

    // מצב משתמש
    val spUser = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    // SharedPreferences ברירת־מחדל
    val defaultPrefsName = remember { ctx.packageName + "_preferences" }
    val spDefault = remember { ctx.getSharedPreferences(defaultPrefsName, Context.MODE_PRIVATE) }

    // תפקיד המשתמש (coach/trainee) – כולל פולבאק ל־default prefs
    var userRole by remember {
        mutableStateOf(spUser.getString("user_role", null) ?: spDefault.getString("user_role", null))
    }

    // בדיקת הרשמה – כולל לוגין (username+password)
    fun SharedPreferences.isRegFlag(): Boolean =
        getBoolean("is_registered", false) ||
                getBoolean("verified_registration", false) ||
                !getString("user_id", null).isNullOrBlank() ||
                (!getString("username", null).isNullOrBlank() && !getString("password", null).isNullOrBlank())

    fun computeIsRegistered(): Boolean =
        spUser.isRegFlag() || spDefault.isRegFlag()

    var isRegistered by remember { mutableStateOf(computeIsRegistered()) }

    // האזנה לשינויים בשני ה-Prefs
    DisposableEffect(spUser, spDefault) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "user_role") {
                userRole = spUser.getString("user_role", null) ?: spDefault.getString("user_role", null)
            }
            if (key == null || key in setOf("is_registered","verified_registration","user_id","username","password")) {
                isRegistered = computeIsRegistered()
            }
        }
        spUser.registerOnSharedPreferenceChangeListener(listener)
        spDefault.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            spUser.unregisterOnSharedPreferenceChangeListener(listener)
            spDefault.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

// --- State שמשפיע על קומפוזיציה ---
    var showBroadcastSheet by remember { mutableStateOf(false) }
    var broadcastText by remember { mutableStateOf("") }
    val MAX_BROADCAST_CHARS = 280
    val focusManager = LocalFocusManager.current

// היה כאן mutableStateListOf(...) ללא remember → עוטפים ב-remember(spUser)
    val recentMessages = remember(spUser) {
        mutableStateListOf<RecentBroadcast>().apply {
            addAll(spUser.getRecentBroadcasts())
        }
    }

    var historyExpanded by remember { mutableStateOf(false) }

// 🔵 דיאלוג עוזר חכם (AI)
    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    // ✅ טור פעולות צדדי במקום סרגל אייקונים אופקי פתוח תמיד
    var quickActionsExpanded by rememberSaveable { mutableStateOf(false) }

    // היה:
    // val broadcastTooLong by derivedStateOf { broadcastText.length > MAX_BROADCAST_CHARS }
    // צריך לזכור את ה-state:
    val broadcastTooLong by remember(broadcastText) {
        derivedStateOf { broadcastText.length > MAX_BROADCAST_CHARS }
    }

    // remember מודאל – חובה ליצור עם remember
    val broadcastSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(showBroadcastSheet) {
        if (!showBroadcastSheet) focusManager.clearFocus(force = true)
    }


    // עזר לזיהוי מאמן
    fun isCoach(role: String?): Boolean = role?.equals("coach", ignoreCase = true) == true
    val effectiveIsRegistered = isRegistered
    val userIsCoach = isCoach(userRole)
    val isCoachForPill = modePillIsCoach ?: userIsCoach

    // הגדרות זמינות תמיד
    val showSettingsAllowed = showSettings

    // האם לאפשר פתיחה
    val canBroadcast = userIsCoach &&
            (!requireRegistrationForCoachBroadcast || isRegistered)

    /** טריגר לפתיחת "שידור מאמן". */
    val triggerCoachBroadcast: () -> Unit =
        remember(onOpenCoachBroadcast, forceInternalCoachBroadcast, canBroadcast) {
            {
                if (!canBroadcast) {
                    android.widget.Toast
                        .makeText(
                            ctx,
                            if (isEnglish) "Broadcast is available to coaches only" else "שידור זמין רק למאמנים",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                    return@remember
                }
                if (forceInternalCoachBroadcast || onOpenCoachBroadcast == null) {
                    showBroadcastSheet = true
                } else {
                    onOpenCoachBroadcast()
                }
            }
        }

    val topBarHeight = 56.dp

    // ✅ טור האייקונים נפתח כ-overlay מעל המסך,
    // לכן ה-TopBar עצמו נשאר בגובה הכותרת בלבד.
    val quickActionsWidth = 66.dp

    // Back בטוח
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val activity: Activity? = remember(ctx) { ctx.safeFindActivity() }
    fun performBackSafe() {
        when {
            onBack != null -> onBack.invoke()
            backDispatcher != null -> backDispatcher.onBackPressed()
            activity != null && !activity.isFinishing -> {
                if (activity is ComponentActivity && !activity.isTaskRoot) {
                    activity.finishAfterTransition()
                } else if (!activity.isTaskRoot) {
                    activity.finish()
                } else {
                    activity.moveTaskToBack(false)
                }
            }
        }
    }

    val providedDrawerForQuickActions = LocalAppDrawerState.current

    val isDrawerOpenForQuickActions =
        providedDrawerForQuickActions?.currentValue == DrawerValue.Open ||
                providedDrawerForQuickActions?.targetValue == DrawerValue.Open

    val showQuickActions =
        showBottomActions &&
                !hideBottomForShare &&
                !isDrawerOpenForQuickActions

    // --- מעטפת עליונה ---
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topBarHeight)
    ) {
        Spacer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(0.dp)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topBarHeight),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            color = Color.White
        ) {}

        CenterAlignedTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .zIndex(10f),
            windowInsets = WindowInsets(0),

            navigationIcon = {
                val providedDrawer = providedDrawerForQuickActions
                val scopeOpen = rememberCoroutineScope()
                val openDrawerClick: () -> Unit = {
                    quickActionsExpanded = false

                    when {
                        onOpenDrawer != null -> onOpenDrawer()
                        providedDrawer != null -> scopeOpen.launch { providedDrawer.open() }
                        else -> DrawerBridge.open()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showMenu) {
                        Box(
                            modifier = Modifier.offset(x = (-6).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PremiumMenuImageIcon(
                                contentDescription = if (isEnglish) "Menu" else "תפריט",
                                onClick = { openDrawerClick() }
                            )
                        }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }

                    if (onBack != null) {
                        PremiumActionIcon(
                            icon = if (useCloseIcon) {
                                Icons.Filled.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            tint = Color(0xFF2563EB),
                            background = Color(0x1A2563EB),
                            contentDescription = if (useCloseIcon) "סגור" else "חזור",
                            onClick = { performBackSafe() }
                        )
                    }
                }
            },

            title = {
                val titleAlignment = if (centerTitle) Alignment.Center else Alignment.CenterEnd
                val titleTextAlign = if (centerTitle) TextAlign.Center else TextAlign.End

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = titleAlignment
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = titleTextAlign,

                        // ✅ צבע קבוע וברור לכותרות המסכים גם במצב כהה
                        // ה-TopBar עצמו לבן, לכן לא משתמשים כאן ב-onSurface של ה-Theme.
                        color = Color(0xFF111827),

                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                velocity = 30.dp
                            )
                    )
                }
            },

            actions = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showRoleBadge && userRole?.isNotBlank() == true) {
                            Box(
                                modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                RoleSquareBadge(isCoach = isCoachForPill)
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // ⬅️ בית + חיפוש + שפה
                        if (showTopHome && onHome != null) {
                            val homeTint = if (lockHome) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                            } else {
                                Color(0xFF2563EB)
                            }

                            val homeBg = if (lockHome) {
                                Color(0x14000000)
                            } else {
                                Color(0x1A2563EB)
                            }

                            PremiumActionIcon(
                                icon = Icons.Filled.Home,
                                tint = homeTint,
                                background = homeBg,
                                contentDescription = "בית",
                                onClick = {
                                    if (lockHome) {
                                        android.widget.Toast
                                            .makeText(
                                                ctx,
                                                homeDisabledToast ?: "אתה כבר במסך הבית 🙂",
                                                android.widget.Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    } else {
                                        onHome()
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        // 🔎 חיפוש — רק אם לא נעול וגם הותר בכותרת
                        if (showTopSearch && !lockSearch && onSearch != null) {
                            PremiumActionIcon(
                                icon = Icons.Filled.Search,
                                tint = Color(0xFF10B981),
                                background = Color(0x1A10B981),
                                contentDescription = "חיפוש",
                                onClick = onSearch
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        // 📤 שתף — אייקון שיתוף עליון, ניתן להסתרה במסכים מסוימים
                        if (showTopShare) {
                            PremiumActionIcon(
                                icon = Icons.Filled.Share,
                                tint = Color(0xFF4F46E5),
                                background = Color(0x1A4F46E5),
                                contentDescription = "שתף",
                                onClick = { runKmiShare() }
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        // אקשנס נוספים מהמסכים
                        extraActions()
                    }
                }
            },

            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White,
                scrolledContainerColor = Color.White,
                titleContentColor = Ink900,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Divider(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarHeight - 1.dp)
                .fillMaxWidth()
                .height(1.dp),
            color = Color(0x14000000)
        )

        val ttsHandler: () -> Unit = onTts ?: { /* no-op */ }
        val fontHandler: () -> Unit = onFont ?: { /* no-op */ }

        // (כמו אצלך ממשיך...)
        val isHomeLockedHere = lockHome && onHome != null
        val homeEnabledForBar = onHome != null && !isHomeLockedHere
        val homeToastForBar = if (isHomeLockedHere) {
            homeDisabledToast ?: "😶 אתה כבר במסך הבית"
        } else {
            null
        }

        if (showQuickActions) {
            Popup(
                alignment = AbsoluteAlignment.TopRight,
                offset = IntOffset(
                    x = with(density) { (-4).dp.roundToPx() },
                    y = with(density) { (topBarHeight - 1.dp).roundToPx() }
                ),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                IconsRailAttachedHandle(
                    expanded = quickActionsExpanded,
                    onToggle = {
                        quickActionsExpanded = !quickActionsExpanded
                    }
                )
            }
        }

        if (showQuickActions && quickActionsExpanded) {
            Popup(
                alignment = AbsoluteAlignment.TopRight,
                offset = IntOffset(
                    x = with(density) { (-10).dp.roundToPx() },
                    y = with(density) { (topBarHeight + 36.dp).roundToPx() }
                ),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = false
                )
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn() +
                            androidx.compose.animation.slideInHorizontally { it / 3 },
                    exit = androidx.compose.animation.fadeOut() +
                            androidx.compose.animation.slideOutHorizontally { it / 3 }
                ) {
                    Surface(
                        modifier = Modifier.width(quickActionsWidth),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 12.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(1.dp, Color(0x14000000))
                    ) {
                        KmiLightTheme {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Search,
                                    label = if (isEnglish) "Search" else "חיפוש",
                                    tint = Color(0xFF10B981),
                                    background = Color(0x1A10B981),
                                    enabled = !lockSearch && onSearch != null,
                                    onClick = {
                                        quickActionsExpanded = false
                                        onSearch?.invoke()
                                    }
                                )

                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Home,
                                    label = if (isEnglish) "Home" else "בית",
                                    tint = if (lockHome) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    } else {
                                        Color(0xFF2563EB)
                                    },
                                    background = if (lockHome) {
                                        Color(0x14000000)
                                    } else {
                                        Color(0x1A2563EB)
                                    },
                                    enabled = onHome != null,
                                    onClick = {
                                        if (lockHome) {
                                            android.widget.Toast
                                                .makeText(
                                                    ctx,
                                                    homeDisabledToast ?: "אתה כבר במסך הבית 🙂",
                                                    android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        } else {
                                            quickActionsExpanded = false
                                            focusManager.clearFocus(force = true)
                                            onHome?.invoke()
                                        }
                                    }
                                )

                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Settings,
                                    label = if (isEnglish) "Settings" else "הגדרות",
                                    tint = Color(0xFFF59E0B),
                                    background = Color(0x1AF59E0B),
                                    enabled = showSettingsAllowed,
                                    onClick = {
                                        quickActionsExpanded = false
                                        focusManager.clearFocus(force = true)
                                        DrawerBridge.openSettings()
                                    }
                                )

                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Lightbulb,
                                    label = if (isEnglish) "AI" else "עוזר",
                                    tint = Color(0xFF8B5CF6),
                                    background = Color(0x1A8B5CF6),
                                    enabled = !isInsideAssistant,
                                    onClick = {
                                        if (!isInsideAssistant) {
                                            quickActionsExpanded = false
                                            showAiDialog = true
                                        }
                                    }
                                )

                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Share,
                                    label = if (isEnglish) "Share" else "שתף",
                                    tint = Color(0xFFEC4899),
                                    background = Color(0x1AEC4899),
                                    enabled = true,
                                    onClick = {
                                        quickActionsExpanded = false
                                        runKmiShare()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // לוגו אופציונלי
        if (showLogoInBar && logoRes != null) {
            val logoSz = logoSize.coerceIn(40.dp, 72.dp)
            val baseOffset = topBarHeight

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = baseOffset - 6.dp)
                    .size(logoSz)
                    .zIndex(50f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(4.dp, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "KMI Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showQuickActions && quickActionsExpanded) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(0.dp)
            )
        }
    }

    // === דיאלוג עוזר חכם (AI) — בלוק יחיד ותקין ===
    if (showAiDialog) {
        AiAssistantDialog(
            onDismiss = { showAiDialog = false },
            onOpenDrawer = {
                DrawerBridge.open()
            }
        )
    }

    // === יריעת שידור מאמן ===
    if (showBroadcastSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBroadcastSheet = false },
            sheetState = broadcastSheetState
        ) {
            val canSend = broadcastText.isNotBlank() && broadcastText.length <= MAX_BROADCAST_CHARS

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // כותרת + כפתור היסטוריה
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isEnglish) "Coach Broadcast" else "שידור מאמן",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // כפתור היסטוריה
                    Box {
                        IconButton(onClick = { historyExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = if (isEnglish) "Previous messages" else "הודעות קודמות"
                            )
                        }
                        DropdownMenu(
                            expanded = historyExpanded,
                            onDismissRequest = { historyExpanded = false }
                        ) {
                            if (recentMessages.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (isEnglish) "No saved messages" else "אין הודעות שמורות") },
                                    enabled = false,
                                    onClick = {}
                                )
                            } else {
                                recentMessages.forEach { rb ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(rb.message, maxLines = 2)
                                                val tsStr = formatRecentTs(rb.ts)
                                                if (tsStr.isNotEmpty()) {
                                                    Text(
                                                        tsStr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            broadcastText = rb.message
                                            historyExpanded = false
                                        }
                                    )
                                }
                                Divider()
                                DropdownMenuItem(
                                    text = { Text(if (isEnglish) "Clear history" else "נקה היסטוריה") },
                                    onClick = {
                                        spUser.edit().remove(PREF_RECENTS_KEY).apply()
                                        recentMessages.clear()
                                        historyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // שדה הודעה + מונה תווים
            OutlinedTextField(
                value = broadcastText,
                onValueChange = { broadcastText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text(if (isEnglish) "Message content" else "תוכן ההודעה") },
                isError = broadcastText.length > MAX_BROADCAST_CHARS,
                supportingText = {
                    val count = "${broadcastText.length}/$MAX_BROADCAST_CHARS"
                    Text(
                        text = count,
                        color = if (broadcastText.length > MAX_BROADCAST_CHARS)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // כפתורים
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        spUser.pushRecentBroadcast(broadcastText.trim())
                        recentMessages.clear()
                        recentMessages.addAll(spUser.getRecentBroadcasts())

                        scope.launch {
                            runCatching { broadcastSheetState.hide() }
                            showBroadcastSheet = false
                            broadcastText = ""
                        }
                    },
                    enabled = canSend
                ) { Text(if (isEnglish) "Send" else "שלח") }

                TextButton(onClick = {
                    scope.launch {
                        runCatching { broadcastSheetState.hide() }
                        showBroadcastSheet = false
                    }
                }) { Text(if (isEnglish) "Cancel" else "ביטול") }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PremiumActionIcon(
    icon: ImageVector,
    tint: Color,
    background: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.76f else 1f,
        animationSpec = spring(
            dampingRatio = 0.28f,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "iconBounce"
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE6E6EE))
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF4B478F),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun PremiumMenuImageIcon(
    contentDescription: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = Spring.StiffnessLow
        ),
        label = "premiumMenuImageIcon"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF6D4ED8),
                            Color(0xFF3B1F82)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumTextActionIcon(
    text: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.76f else 1f,
        animationSpec = spring(
            dampingRatio = 0.28f,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "textIconBounce"
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE6E6EE))
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF4B478F),
            fontSize = 19.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

private class IconsRailAttachedTabShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)

            // צד ימין מחובר לכותרת ויורד בעדינות
            lineTo(w, h * 0.56f)

            // קימור פרימיום אל החוד התחתון
            cubicTo(
                w, h * 0.72f,
                w * 0.76f, h * 0.86f,
                w * 0.50f, h
            )

            cubicTo(
                w * 0.24f, h * 0.86f,
                0f, h * 0.72f,
                0f, h * 0.56f
            )

            lineTo(0f, 0f)
            close()
        }

        return Outline.Generic(path)
    }
}

@Composable
private fun IconsRailAttachedHandle(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.38f,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconsRailAttachedHandlePress"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.48f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconsRailAttachedHandleArrow"
    )

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 36.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(IconsRailAttachedTabShape())
            .background(Color.White)
            .clickable {
                pressed = true
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "סגור סרגל אייקונים" else "פתח סרגל אייקונים",
            tint = Color(0xFF6D4ED8),
            modifier = Modifier
                .size(21.dp)
                .offset(y = 4.dp)
                .graphicsLayer {
                    rotationZ = arrowRotation
                }
        )
    }
}

@Composable
private fun VerticalQuickActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val itemAlpha = if (enabled) 1f else 0.38f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(background.copy(alpha = background.alpha * itemAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint.copy(alpha = itemAlpha),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = label,
            color = Color(0xFF1F2937).copy(alpha = itemAlpha),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ====================== עזרים ====================== */

private fun shareAppDefault(ctx: Context, text: String = "הורידו את KAMI – ק.מ.י") {
    val isEnglish = AppLanguageManager(ctx).getCurrentLanguage().code == "en"
    val shareTitle = if (isEnglish) "Share with" else "שתף באמצעות"

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, shareTitle)
    if (ctx !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(chooser)
}

@Composable
fun ModeBadgeSmall(isCoach: Boolean) {
    val ctx = LocalContext.current
    val isEnglish = remember(ctx) {
        AppLanguageManager(ctx).getCurrentLanguage().code == "en"
    }

    val label = if (isCoach) {
        if (isEnglish) "Coach" else "מאמן"
    } else {
        if (isEnglish) "Trainee" else "מתאמן"
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Helper: מאתר Activity מה־Context
private fun Context.safeFindActivity(maxDepth: Int = 12): Activity? {
    var depth = 0
    var cur: Context? = this
    while (depth < maxDepth && cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
        depth++
    }
    return null
}

// ===== Recent Broadcasts (message + timestamp) =====
private const val PREF_RECENTS_KEY = "recent_coach_broadcasts_v2"
private data class RecentBroadcast(val message: String, val ts: Long)

private fun SharedPreferences.getRecentBroadcasts(): List<RecentBroadcast> {
    val json = getString(PREF_RECENTS_KEY, "[]") ?: "[]"
    return runCatching {
        val arr = org.json.JSONArray(json)
        val out = arrayListOf<RecentBroadcast>()
        for (i in 0 until arr.length()) {
            when (val any = arr.get(i)) {
                is org.json.JSONObject -> {
                    val m = any.optString("m").trim()
                    val t = any.optLong("t", 0L)
                    if (m.isNotBlank()) out += RecentBroadcast(m, t)
                }
                is String -> { // תאימות לאחור
                    val m = any.trim()
                    if (m.isNotBlank()) out += RecentBroadcast(m, 0L)
                }
            }
        }
        out
    }.getOrElse { emptyList() }
}

private fun <T> MutableList<T>.safeRemoveLast(): T? =
    if (isNotEmpty()) removeAt(lastIndex) else null

private fun SharedPreferences.pushRecentBroadcast(message: String, limit: Int = 10) {
    val trimmed = message.trim()
    if (trimmed.isBlank()) return
    val existing = getRecentBroadcasts().toMutableList()
    existing.removeAll { it.message == trimmed }
    existing.add(0, RecentBroadcast(trimmed, System.currentTimeMillis()))
    while (existing.size > limit) {
        existing.removeAt(existing.lastIndex)
    }


    val arr = org.json.JSONArray()
    existing.forEach { rb ->
        val o = org.json.JSONObject()
            .put("m", rb.message)
            .put("t", rb.ts)
        arr.put(o)
    }
    edit().putString(PREF_RECENTS_KEY, arr.toString()).apply()
}

private fun formatRecentTs(ts: Long): String {
    if (ts <= 0L) return ""
    val df = java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT
    )
    return df.format(java.util.Date(ts))
}

// המשפט של עמידת המוצא שאנחנו רוצים להדגיש
private const val STANCE_SENTENCE =
    "עמידת מוצא - רגל שמאל קדימה, כפות רגליים מקבילות ברוחב האגן, ברכיים כפופות מעט, ידיים למעלה במצב הגנה"

private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val stanceSentence = STANCE_SENTENCE

    // אם המשפט לא נמצא – מחזירים טקסט רגיל
    val idx = source.indexOf(stanceSentence)
    if (idx == -1) return AnnotatedString(source)

    val before = source.substring(0, idx)
    val after  = source.substring(idx + stanceSentence.length)

    val builder = AnnotatedString.Builder()

    // לפני עמידת מוצא
    builder.append(before)

    // מוסיפים את "עמידת מוצא..." עם הדגשה וצבע
    val stanceStart = builder.length
    builder.append(stanceSentence)
    val stanceEnd = builder.length

    builder.addStyle(
        style = SpanStyle(
            fontWeight = FontWeight.Bold,
            color = stanceColor
        ),
        start = stanceStart,
        end = stanceEnd
    )

    // שאר ההסבר
    builder.append(after)

    return builder.toAnnotatedString()
}

@Composable
fun ExplanationWithStanceHighlight(
    explanation: String,
    modifier: Modifier = Modifier
) {
    // הצבע של "עמידת מוצא..." – ניקח מה־Theme פעם אחת
    val stanceColor = MaterialTheme.colorScheme.primary

    // בונים AnnotatedString פעם אחת לזוג (הסבר, צבע)
    val annotated = remember(explanation, stanceColor) {
        buildExplanationWithStanceHighlight(
            source = explanation,
            stanceColor = stanceColor
        )
    }

    Text(
        text = annotated,
        modifier = modifier,
        style  = MaterialTheme.typography.bodyLarge,
        color  = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * מדגיש את המשפט שבו מופיע "עמידת מוצא" בצבע שונה ובמודגש.
 * שאר הטקסט נשאר כרגיל.
 */
@Composable
private fun buildExplanationWithStanceHighlight(
    source: String
): AnnotatedString {
    val stanceSentence = "עמידת מוצא: עמידת הלוחם"

    val idx = source.indexOf(stanceSentence)
    if (idx == -1) return AnnotatedString(source)

    val before = source.substring(0, idx)
    val after = source.substring(idx + stanceSentence.length)

    val builder = AnnotatedString.Builder()

    // מה שלפני עמידת המוצא
    builder.append(before)

    // המשפט של עמידת המוצא – מודגש וצבעוני
    val stanceStart = builder.length
    builder.append(stanceSentence)
    val stanceEnd = builder.length

    builder.addStyle(
        style = SpanStyle(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary   // צבע שונה למשפט עמידת המוצא
        ),
        start = stanceStart,
        end = stanceEnd
    )

    // שאר ההסבר
    builder.append(after)

    return builder.toAnnotatedString()
}

/** תג מצב ריבועי (מאמן/מתאמן) */
@Composable
private fun RoleSquareBadge(isCoach: Boolean) {
    val bg  = if (isCoach) Color(0xFF2A1F52) else Color(0xFF1E2947)
    val txt = Color.White
    val line2 = if (isCoach) "מאמן" else "מתאמן"

    Surface(
        color = bg,
        contentColor = txt,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Text(
            text = "מצב\n$line2",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
