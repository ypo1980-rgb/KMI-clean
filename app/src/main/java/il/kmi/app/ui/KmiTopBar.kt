@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.app.Activity
import android.content.ContextWrapper
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import il.kmi.app.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.core.view.doOnPreDraw
import shareCurrentScreen
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import il.kmi.app.search.KmiSearchBridge
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
import il.kmi.app.ui.assistant.ui.AiAssistantDialog

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
    topBeltIconRes: Int? = null,
    topBeltIconDescription: String? = null,
    showTopBeltIcon: Boolean = false,
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

    // ✅ תמונת חגורה אוטומטית לפי הכותרת.
    // כך היא תופיע גם במסך נושאים וגם במסך סיכום,
    // גם אם המסך לא העביר topBeltIconRes ידנית.
    val resolvedTopBeltIconRes = remember(title, topBeltIconRes) {
        topBeltIconRes ?: when {
            title.contains("לבנה") || title.contains("White", ignoreCase = true) ->
                R.drawable.belt_white

            title.contains("צהובה") || title.contains("Yellow", ignoreCase = true) ->
                R.drawable.belt_yellow

            title.contains("כתומה") || title.contains("Orange", ignoreCase = true) ->
                R.drawable.belt_orange

            title.contains("ירוקה") || title.contains("Green", ignoreCase = true) ->
                R.drawable.belt_green

            title.contains("כחולה") || title.contains("Blue", ignoreCase = true) ->
                R.drawable.belt_blue

            title.contains("חומה") || title.contains("Brown", ignoreCase = true) ->
                R.drawable.belt_brown

            title.contains("שחורה") || title.contains("Black", ignoreCase = true) ->
                R.drawable.belt_black

            else -> null
        }
    }

    val shouldRenderTopBeltIcon =
        showTopBeltIcon || resolvedTopBeltIconRes != null

    val rootView = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
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

    var historyExpanded by remember { mutableStateOf(false) }

    // 🔵 דיאלוג עוזר חכם (AI)
    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    // ✅ טור פעולות צדדי במקום סרגל אייקונים אופקי פתוח תמיד
    var quickActionsExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ חיפוש גלובאלי מתוך טור האייקונים החדש
    var showGlobalSearch by rememberSaveable { mutableStateOf(false) }
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    var globalSearchInputEnabled by rememberSaveable { mutableStateOf(true) }

    val broadcastTooLong by remember(broadcastText) {
        derivedStateOf { broadcastText.length > MAX_BROADCAST_CHARS }
    }

    val broadcastSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val scope = rememberCoroutineScope()

    fun hideGlobalSearchKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()

        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        rootView.clearFocus()
        imm?.hideSoftInputFromWindow(rootView.windowToken, 0)

        val activityRoot = (ctx as? Activity)?.window?.decorView
        activityRoot?.clearFocus()
        imm?.hideSoftInputFromWindow(activityRoot?.windowToken, 0)

        rootView.postDelayed({
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            imm?.hideSoftInputFromWindow(rootView.windowToken, 0)
            imm?.hideSoftInputFromWindow(activityRoot?.windowToken, 0)
        }, 80)
    }

    fun finishGlobalSearchTyping() {
        hideGlobalSearchKeyboard()

        // כיבוי קצר של TextField מכריח את Samsung Keyboard לסגור את המקלדת.
        globalSearchInputEnabled = false

        scope.launch {
            delay(180)
            globalSearchInputEnabled = true
        }
    }

// היה כאן mutableStateListOf(...) ללא remember → עוטפים ב-remember(spUser)
    val recentMessages = remember(spUser) {
        mutableStateListOf<RecentBroadcast>().apply {
            addAll(spUser.getRecentBroadcasts())
        }
    }

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

    val shouldShowRolePillBelowTitle =
        showRoleBadge && userRole?.isNotBlank() == true

    // ✅ מצב המשתמש מוצג עכשיו כתג קטן בפינה,
    // לכן לא צריך לתת לו גובה מרכזי גדול בכותרת.
    val topBarHeight = if (shouldShowRolePillBelowTitle) 68.dp else 64.dp

    // ✅ טור האייקונים נפתח כ-overlay מעל המסך,
    // לכן ה-TopBar עצמו נשאר בגובה הכותרת בלבד.
    val quickActionsWidth = 58.dp

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

            // ✅ הכותרת כבר לא מוצגת דרך ה-title slot של TopAppBar,
            // כדי שלא תוסט בגלל אייקון התפריט / actions.
            title = {
                Spacer(modifier = Modifier.fillMaxSize())
            },

            actions = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

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

        // ✅ כותרת Overlay ממורכזת לפי כל רוחב הסרגל,
        // ללא תלות באייקון התפריט או באייקונים בצדדים.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(
                    start = 58.dp,
                    end = 58.dp,
                    bottom = if (shouldShowRolePillBelowTitle) 4.dp else 0.dp
                )
                .zIndex(12f),
            contentAlignment = Alignment.Center
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
                textAlign = TextAlign.Center,
                color = Color(0xFF111827),
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity = 30.dp
                    )
            )
        }

        if (shouldRenderTopBeltIcon && resolvedTopBeltIconRes != null) {
            Box(
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .padding(
                        start = 6.dp,
                        bottom = if (shouldShowRolePillBelowTitle) 22.dp else 9.dp
                    )
                    .zIndex(30f)
            ) {
                Image(
                    painter = painterResource(id = resolvedTopBeltIconRes),
                    contentDescription = topBeltIconDescription ?: title,
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            rotationZ = -18f
                        }
                )
            }
        }

        if (shouldShowRolePillBelowTitle) {
            Box(
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .padding(start = 8.dp, bottom = 3.dp)
                    .zIndex(25f)
            ) {
                RoleInlinePill(
                    isCoach = isCoachForPill,
                    isEnglish = isEnglish
                )
            }
        }

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
                    x = with(density) { (8).dp.roundToPx() },
                    y = with(density) { (topBarHeight - 5.dp).roundToPx() }
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
                    x = with(density) { (-2).dp.roundToPx() },
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
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        shadowElevation = 18.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.98f),
                                            Color(0xFFF8F7FF),
                                            Color.White.copy(alpha = 0.98f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE7DDFB),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            KmiLightTheme {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                VerticalQuickActionItem(
                                    icon = Icons.Filled.Search,
                                    label = if (isEnglish) "Search" else "חיפוש",
                                    tint = Color(0xFF10B981),
                                    background = Color(0x1A10B981),
                                    enabled = !lockSearch,
                                    onClick = {
                                        quickActionsExpanded = false
                                        focusManager.clearFocus(force = true)

                                        // אם מסך חיצוני רוצה להגיב לחיפוש — נשאיר לו אפשרות.
                                        onSearch?.invoke()

                                        // החיפוש הגלובאלי האמיתי נפתח כאן.
                                        globalSearchQuery = ""
                                        showGlobalSearch = true
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

    // === דיאלוג עוזר חכם (AI) ===
    if (showAiDialog) {
        AiAssistantDialog(
            onDismiss = {
                showAiDialog = false
            },
            onOpenDrawer = {
                showAiDialog = false
                DrawerBridge.open()
            }
        )
    }

    // === חיפוש גלובאלי מתפריט האייקונים הצדדי ===
    if (showGlobalSearch) {
        ModalBottomSheet(
            onDismissRequest = {
                hideGlobalSearchKeyboard()
                showGlobalSearch = false
                globalSearchQuery = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111827).copy(alpha = 0.42f))
                )
            }
        ) {
            val searchTitle = if (isEnglish) "Global Search" else "חיפוש גלובאלי"
            val searchLabel = if (isEnglish) {
                "Search exercise"
            } else {
                "חפש תרגיל"
            }

            val searchHint = if (isEnglish) {
                "Type a word to search all exercises."
            } else {
                "הקלד מילה כדי לחפש בכל התרגילים."
            }

            val noResultsText = if (isEnglish) {
                "No results found"
            } else {
                "לא נמצאו תוצאות"
            }

            val searchLayoutDirection =
                if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

            val searchTextAlign =
                if (isEnglish) TextAlign.Left else TextAlign.Right

            val searchHorizontalAlignment =
                if (isEnglish) Alignment.Start else Alignment.End

            fun normalizedSearchVariants(query: String): List<String> {
                val q = query
                    .replace("\u200F", "")
                    .replace("\u200E", "")
                    .replace("\u00A0", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                if (q.isBlank()) return emptyList()

                val variants = linkedSetOf<String>()

                fun add(value: String) {
                    val clean = value
                        .replace("\u200F", "")
                        .replace("\u200E", "")
                        .replace("\u00A0", " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    if (clean.length >= 2) {
                        variants += clean
                    }
                }

                add(q)

                // אם המשתמש הקליד בטעות "ה נגד..." או "הגנה נגד..."
                add(q.removePrefix("ה ").trim())
                add(q.removePrefix("ה").trim())

                add(q.replace("הגנה נגד", "נגד").trim())
                add(q.replace("הגנה", "").trim())
                add(q.replace("ה ", "").trim())

                // חיפוש מדויק להגנות נגד בעיטה לצד.
                // חשוב: לא מוסיפים כאן "בעיטה לצד" לבד,
                // כי זה מחזיר תרגילי בעיטה רגילים במקום תרגילי הגנה.
                val isSideKickDefenseSearch =
                    q.contains("הגנה נגד בעיטה לצד") ||
                            q.contains("הגנה נגד בעיטות לצד") ||
                            q.contains("נגד בעיטה לצד") ||
                            q.contains("נגד בעיטות לצד") ||
                            q.contains("הגנה חיצונית באמת") ||
                            q.contains("הגנה חיצונית באמה") ||
                            q.contains("באמת ימין") ||
                            q.contains("באמת שמאל") ||
                            q.contains("באמה ימין") ||
                            q.contains("באמה שמאל")

                if (isSideKickDefenseSearch) {
                    add("הגנה חיצונית באמת ימין נגד בעיטה לצד")
                    add("הגנה חיצונית באמת שמאל נגד בעיטה לצד")
                    add("הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית")
                    add("הגנות נגד בעיטות נגד בעיטות לצד")
                    add("הגנות נגד בעיטות")
                    add("נגד בעיטה לצד")
                    add("נגד בעיטות לצד")
                }

                return variants.toList()
            }

            fun normalizeForGlobalSearch(value: String): String {
                return value
                    .replace("\u200F", "")
                    .replace("\u200E", "")
                    .replace("\u00A0", " ")
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("־", "-")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }

            fun beltLabelForSearch(beltName: String): String {
                return when (beltName.uppercase()) {
                    "YELLOW" -> if (isEnglish) "Yellow belt" else "חגורה צהובה"
                    "ORANGE" -> if (isEnglish) "Orange belt" else "חגורה כתומה"
                    "GREEN" -> if (isEnglish) "Green belt" else "חגורה ירוקה"
                    "BLUE" -> if (isEnglish) "Blue belt" else "חגורה כחולה"
                    "BROWN" -> if (isEnglish) "Brown belt" else "חגורה חומה"
                    "BLACK" -> if (isEnglish) "Black belt" else "חגורה שחורה"
                    else -> beltName
                }
            }

            fun searchSubtitleFromResolvedKey(rawKey: String, fallbackBeltName: String): String {
                val resolved = il.kmi.app.domain.ContentRepo.resolveItemKey(rawKey)

                return if (resolved != null) {
                    val beltLabel = beltLabelForSearch(resolved.belt.name)

                    if (resolved.topicTitle.isNotBlank()) {
                        "$beltLabel • ${resolved.topicTitle}"
                    } else {
                        beltLabel
                    }
                } else {
                    beltLabelForSearch(fallbackBeltName)
                }
            }

            fun matchesGlobalSearchTitle(
                title: String,
                variants: List<String>
            ): Boolean {
                val normalizedTitle = normalizeForGlobalSearch(title)

                return variants.any { variant ->
                    val normalizedVariant = normalizeForGlobalSearch(variant)

                    if (normalizedVariant.length < 2) {
                        false
                    } else {
                        normalizedTitle.contains(normalizedVariant, ignoreCase = true) ||
                                normalizedVariant
                                    .split(" ")
                                    .filter { it.length >= 2 }
                                    .all { word ->
                                        normalizedTitle.contains(word, ignoreCase = true)
                                    }
                    }
                }
            }

            fun directExplanationSideKickDefenseResults(query: String): List<UiSearchResult> {
                val normalizedQuery = normalizeForGlobalSearch(query)

                val shouldUseDirectSideKickDefense =
                    normalizedQuery.contains("הגנה נגד בעיטה לצד") ||
                            normalizedQuery.contains("הגנה נגד בעיטות לצד") ||
                            normalizedQuery.contains("נגד בעיטה לצד") ||
                            normalizedQuery.contains("נגד בעיטות לצד")

                if (!shouldUseDirectSideKickDefense) {
                    return emptyList()
                }

                val greenBelt = il.kmi.shared.domain.Belt.GREEN

                val directTitles = listOf(
                    "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                    "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
                    "הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית"
                )

                return directTitles.mapNotNull { title ->
                    val explanation = il.kmi.app.domain.Explanations
                        .get(
                            belt = greenBelt,
                            item = title,
                            exerciseId = null
                        )
                        .trim()

                    val isRealExplanation =
                        explanation.isNotBlank() &&
                                !explanation.startsWith("הסבר מפורט על:")

                    if (isRealExplanation) {
                        UiSearchResult(
                            id = "green::הגנות::$title",
                            title = title,
                            subtitle = if (isEnglish) {
                                "Green belt • Defenses"
                            } else {
                                "חגורה ירוקה • הגנות"
                            }
                        )
                    } else {
                        null
                    }
                }
            }

            val explanationSearchRows = remember {
                il.kmi.app.domain.Explanations.auditKnownExerciseExplanations()
            }

            val results = remember(globalSearchQuery, explanationSearchRows, isEnglish) {
                val query = globalSearchQuery.trim()

                if (query.length < 2) {
                    emptyList()
                } else {
                    val variants = normalizedSearchVariants(query)

                    val directExplanationResults =
                        directExplanationSideKickDefenseResults(query)

                    val explanationResults = explanationSearchRows
                        .filter { row ->
                            matchesGlobalSearchTitle(
                                title = row.title,
                                variants = variants
                            )
                        }
                        .map { row ->
                            UiSearchResult(
                                id = row.exerciseId,
                                title = row.title,
                                subtitle = searchSubtitleFromResolvedKey(
                                    rawKey = row.exerciseId,
                                    fallbackBeltName = row.belt.name
                                )
                            )
                        }

                    val bridgeResults = variants
                        .flatMap { variant ->
                            runCatching {
                                KmiSearchBridge.searchExercises(variant)
                            }.getOrElse {
                                emptyList()
                            }
                        }
                        .map { hit ->
                            val rawKey = hit.id ?: hit.title

                            UiSearchResult(
                                id = rawKey,
                                title = hit.title,
                                subtitle = hit.subtitle
                                    ?: searchSubtitleFromResolvedKey(
                                        rawKey = rawKey,
                                        fallbackBeltName = ""
                                    )
                            )
                        }

                    (directExplanationResults + explanationResults + bridgeResults)
                        .distinctBy { hit ->
                            normalizeForGlobalSearch(hit.title)
                        }
                }
            }

            val sheetDirection = if (isEnglish) {
                LayoutDirection.Ltr
            } else {
                LayoutDirection.Rtl
            }

            CompositionLocalProvider(LocalLayoutDirection provides sheetDirection) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8F5FB),
                                    Color(0xFFEDE7F6),
                                    Color(0xFFF9F6FC)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = searchHorizontalAlignment
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = Color.Transparent,
                            shadowElevation = 10.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF0F1B35),
                                                Color(0xFF243B6B),
                                                Color(0xFF5B4BB7)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 18.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        hideGlobalSearchKeyboard()
                                        showGlobalSearch = false
                                        globalSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .align(AbsoluteAlignment.TopLeft)
                                        .offset(x = (-6).dp, y = (-6).dp)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = if (isEnglish) "Close search" else "סגור חיפוש",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = searchHorizontalAlignment
                                ) {
                                    Text(
                                        text = searchTitle,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = searchTextAlign,
                                        fontSize = 24.sp,
                                        lineHeight = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )

                                    Text(
                                        text = searchHint,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = searchTextAlign,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            tonalElevation = 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFE9DDF7)
                            )
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides searchLayoutDirection
                            ) {
                                OutlinedTextField(
                                    value = globalSearchQuery,
                                    onValueChange = { newValue ->
                                        val cleanValue = newValue
                                            .replace("\n", " ")
                                            .replace("\r", " ")
                                            .replace(Regex("\\s+"), " ")

                                        globalSearchQuery = cleanValue

                                        if (cleanValue != newValue) {
                                            finishGlobalSearchTyping()
                                        }
                                    },
                                    enabled = globalSearchInputEnabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 62.dp, max = 92.dp)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    singleLine = false,
                                    minLines = 1,
                                    maxLines = 2,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            finishGlobalSearchTyping()
                                        },
                                        onSearch = {
                                            finishGlobalSearchTyping()
                                        },
                                        onGo = {
                                            finishGlobalSearchTyping()
                                        },
                                        onSend = {
                                            finishGlobalSearchTyping()
                                        }
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = searchTextAlign,
                                        color = Color(0xFF111827)
                                    ),
                                    label = {
                                        Text(
                                            text = searchLabel,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = searchTextAlign,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = searchLabel,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = searchTextAlign,
                                            fontSize = 11.5.sp,
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = searchLabel,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(21.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (globalSearchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    globalSearchQuery = ""
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = if (isEnglish) "Clear" else "נקה",
                                                    tint = Color(0xFF6D4ED8),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFFDFBFF),
                                        unfocusedContainerColor = Color(0xFFFDFBFF),
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFFE5DDF2),
                                        focusedLabelColor = Color(0xFF6D4ED8),
                                        unfocusedLabelColor = Color(0xFF64748B),
                                        cursorColor = Color(0xFF6D4ED8)
                                    )
                                )
                            }
                        }

                        when {
                            globalSearchQuery.trim().length < 2 -> {
                                Text(
                                    text = searchHint,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    textAlign = searchTextAlign,
                                    color = Color(0xFF111827),
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            results.isEmpty() -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFF7ED),
                                    shadowElevation = 3.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text = "$noResultsText: ${globalSearchQuery.trim()}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        textAlign = searchTextAlign,
                                        color = Color(0xFF9A3412),
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = results,
                                        key = { hit -> hit.id ?: hit.title }
                                    ) { hit ->
                                        val rawKey = (hit.id ?: hit.title).trim()

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val resolved =
                                                        il.kmi.app.domain.ContentRepo.resolveItemKey(rawKey)

                                                    val stableKey = if (resolved != null) {
                                                        // שולחים מפתח פשוט ויציב להסבר:
                                                        // belt::topic::item
                                                        // לא מכניסים כאן subTopic, כי חלק מהמסכים מפרשים רק 3 חלקים.
                                                        listOf(
                                                            resolved.belt.id,
                                                            resolved.topicTitle,
                                                            resolved.itemTitle
                                                        ).joinToString("::")
                                                    } else {
                                                        rawKey
                                                    }

                                                    keyboardController?.hide()
                                                    focusManager.clearFocus(force = true)

                                                    showGlobalSearch = false
                                                    globalSearchQuery = ""
                                                    onPickSearchResult?.invoke(stableKey)
                                                        ?: onOpenExercise?.invoke(stableKey)
                                                },
                                            shape = RoundedCornerShape(22.dp),
                                            color = Color.White,
                                            tonalElevation = 0.dp,
                                            shadowElevation = 6.dp,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color(0xFFECE4F6)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                                horizontalAlignment = searchHorizontalAlignment
                                            ) {
                                                Text(
                                                    text = hit.title,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = searchTextAlign,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    lineHeight = 20.sp,
                                                    color = Color(0xFF111827)
                                                )

                                                if (!hit.subtitle.isNullOrBlank()) {
                                                    Text(
                                                        text = hit.subtitle!!,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = searchTextAlign,
                                                        fontSize = 13.sp,
                                                        lineHeight = 17.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
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
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.40f,
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

    Surface(
        modifier = Modifier
            .size(width = 40.dp, height = 28.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        onClick = {
            pressed = true
            onToggle()
        },
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 18.dp,
            bottomEnd = 18.dp
        ),
        color = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "סגור סרגל אייקונים" else "פתח סרגל אייקונים",
                tint = Color(0xFF4B478F),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    }
            )
        }
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(
                    elevation = if (enabled) 2.dp else 0.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(background.copy(alpha = background.alpha * itemAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint.copy(alpha = itemAlpha),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(1.dp))

        Text(
            text = label,
            color = Color(0xFF111827).copy(alpha = itemAlpha),
            fontSize = 8.5.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
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

/** תג מצב קטן ושקט בפינה השמאלית התחתונה של הכותרת */
@Composable
private fun RoleInlinePill(
    isCoach: Boolean,
    isEnglish: Boolean
) {
    val bg = if (isCoach) {
        Color(0xFF2A1F52)
    } else {
        Color(0xFF1E2947)
    }

    val accent = if (isCoach) {
        Color(0xFFD8B4FE)
    } else {
        Color(0xFFBFDBFE)
    }

    val label = when {
        isEnglish && isCoach -> "Coach"
        isEnglish && !isCoach -> "Trainee"
        !isEnglish && isCoach -> "מאמן"
        else -> "מתאמן"
    }

    Surface(
        color = bg.copy(alpha = 0.94f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.30f)
        )
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 8.5.sp,
            lineHeight = 9.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp)
        )
    }
}