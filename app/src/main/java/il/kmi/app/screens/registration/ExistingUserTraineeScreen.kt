@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import il.kmi.app.FcmTokenManager
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.prefs.KmiPrefs
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

//======================================================================

private object SecureLoginPasswordStore {

    private const val ANDROID_KEYSTORE =
        "AndroidKeyStore"

    private const val KEY_ALIAS =
        "kmi_login_password_key"

    private const val PREFS_NAME =
        "kmi_secure_login"

    private const val PREF_CIPHER =
        "password_cipher"

    private const val PREF_IV =
        "password_iv"

    private fun getOrCreateKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()

        keyGenerator.init(spec)

        return keyGenerator.generateKey()
    }

    fun save(
        context: Context,
        password: String
    ) {
        if (password.isBlank()) {
            clear(context)
            return
        }

        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encrypted =
            cipher.doFinal(
                password.toByteArray(
                    Charsets.UTF_8
                )
            )

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit {
            putString(
                PREF_CIPHER,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            )

            putString(
                PREF_IV,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP
                )
            )
        }
    }

    fun load(
        context: Context
    ): String {
        return runCatching {
            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val cipherText =
                prefs.getString(
                    PREF_CIPHER,
                    null
                ) ?: return ""

            val ivText =
                prefs.getString(
                    PREF_IV,
                    null
                ) ?: return ""

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    128,
                    Base64.decode(
                        ivText,
                        Base64.NO_WRAP
                    )
                )
            )

            val decrypted =
                cipher.doFinal(
                    Base64.decode(
                        cipherText,
                        Base64.NO_WRAP
                    )
                )

            String(
                decrypted,
                Charsets.UTF_8
            )

        }.getOrDefault("")
    }

    fun clear(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit {
            clear()
        }
    }
}

private object KmiDeviceIdentity {

    private const val PREFS_NAME =
        "kmi_device_identity"

    private const val KEY_DEVICE_ID =
        "device_id"

    fun getOrCreate(
        context: Context
    ): String {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val existing =
            prefs.getString(
                KEY_DEVICE_ID,
                null
            )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (existing != null) {
            return existing
        }

        val newDeviceId =
            UUID.randomUUID()
                .toString()

        prefs.edit(commit = true) {
            putString(
                KEY_DEVICE_ID,
                newDeviceId
            )
        }

        return newDeviceId
    }
}

private enum class DeviceBindingResult {
    ALLOWED,
    TRANSFER_REQUIRED
}

private data class DeviceBindingCheck(
    val result: DeviceBindingResult,
    val activeDeviceId: String
)

private suspend fun checkCurrentDevice(
    context: Context,
    uid: String
): DeviceBindingCheck {

    if (uid.isBlank()) {
        throw IllegalArgumentException("Missing user UID")
    }

    val deviceId =
        KmiDeviceIdentity.getOrCreate(
            context
        )

    val userRef =
        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(uid)

    if (
        FirebaseAuth.getInstance().currentUser?.uid != uid
    ) {
        throw IllegalStateException(
            "Authenticated user does not match"
        )
    }

    val snapshot =
        userRef.get(Source.SERVER).await()

    if (!snapshot.exists()) {
        throw IllegalStateException(
            "User profile does not exist"
        )
    }

    val activeDeviceId =
        snapshot.getString("activeDeviceId")
            .orEmpty()
            .trim()

    if (activeDeviceId == deviceId) {
        return DeviceBindingCheck(
            result = DeviceBindingResult.ALLOWED,
            activeDeviceId = deviceId
        )
    }

    if (activeDeviceId.isNotBlank()) {
        return DeviceBindingCheck(
            result = DeviceBindingResult.TRANSFER_REQUIRED,
            activeDeviceId = activeDeviceId
        )
    }

    /*
     * שיוך ראשוני בלבד:
     * אין מכשיר פעיל ולכן מבקשים
     * מהשרת לבצע את השיוך האטומי.
     */
    val result =
        FirebaseFunctions
            .getInstance("europe-west1")
            .getHttpsCallable("transferKmiActiveDevice")
            .call(
                mapOf(
                    "deviceId" to deviceId,
                    "expectedActiveDeviceId" to ""
                )
            )
            .await()

    val accepted =
        (result.data as? Map<*, *>)
            ?.get("accepted") == true

    if (!accepted) {
        throw IllegalStateException(
            "Initial device binding not confirmed"
        )
    }

    return DeviceBindingCheck(
        result = DeviceBindingResult.ALLOWED,
        activeDeviceId = deviceId
    )
}

internal suspend fun isCurrentDeviceStillActive(
    context: Context,
    uid: String
): Boolean {

    if (uid.isBlank()) {
        return false
    }

    val currentDeviceId =
        KmiDeviceIdentity.getOrCreate(
            context
        )

    if (
        FirebaseAuth.getInstance().currentUser?.uid != uid
    ) {
        return false
    }

    val snapshot =
        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(uid)
            .get(Source.SERVER)
            .await()

    val activeDeviceId =
        snapshot
            .getString("activeDeviceId")
            .orEmpty()
            .trim()

    /*
     * אם עדיין אין activeDeviceId בשרת,
     * לא חוסמים משתמש קיים.
     *
     * מצב זה חשוב גם למשתמשים קיימים
     * שנרשמו לפני הוספת מנגנון המכשירים.
     */
    if (activeDeviceId.isBlank()) {
        return true
    }

    return activeDeviceId == currentDeviceId
}

private suspend fun transferCurrentDevice(
    context: Context,
    uid: String,
    expectedActiveDeviceId: String
) {
    require(uid.isNotBlank())
    require(expectedActiveDeviceId.isNotBlank())

    val user =
        FirebaseAuth.getInstance().currentUser

    if (
        user == null ||
        user.isAnonymous ||
        user.uid != uid
    ) {
        throw IllegalStateException(
            "Authenticated user changed"
        )
    }

    val deviceId =
        KmiDeviceIdentity.getOrCreate(context)

    val result =
        FirebaseFunctions
            .getInstance("europe-west1")
            .getHttpsCallable("transferKmiActiveDevice")
            .call(
                mapOf(
                    "deviceId" to deviceId,
                    "expectedActiveDeviceId" to
                            expectedActiveDeviceId
                )
            )
            .await()

    val accepted =
        (result.data as? Map<*, *>)
            ?.get("accepted") == true

    if (!accepted) {
        throw IllegalStateException(
            "Device transfer not confirmed"
        )
    }
}

@Composable
private fun ExistingUserLockedTopBar(
    title: String,
    onBack: () -> Unit
) {
    KmiTopBar(
        title = title,
        onBack = onBack,
        showTopHome = false,
        showTopSearch = false,
        showTopShare = false,
        showBottomActions = true,
        lockHome = true,
        lockSearch = true,
        lockAllActions = true,
        centerTitle = true
    )
}

@Composable
fun ExistingUserTraineeScreen(
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs
) {
    var showRecoveryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler {
        if (showRecoveryDialog) {
            showRecoveryDialog = false
        } else {
            onBack()
        }
    }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val appCtx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    // כשהמקלדת פתוחה, מסתירים את הקרדיט הקבוע בתחתית
    // כדי שלא יעלה מעל המקלדת ויכסה את שדות ההתחברות.
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    val langManager = remember { AppLanguageManager(appCtx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    // 🔊+📳 טעינת ההעדפות ממסך ההגדרות (kmi_settings)
    val settingsSp = remember {
        appCtx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }
    val clickEnabled by remember {
        mutableStateOf(
            settingsSp.getBoolean(
                "click_sounds",
                settingsSp.getBoolean("tap_sound", false)
            )
        )
    }
    val hapticEnabled by remember {
        mutableStateOf(
            settingsSp.getBoolean(
                "haptics_on",
                settingsSp.getBoolean("short_haptic", false)
            )
        )
    }

    fun playStrongFeedback() {
        if (clickEnabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        if (hapticEnabled) {
            // רטט חזק
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

// מצב נבחר: מתאמן / מאמן.
// אם קיימת בחירה אחרונה, מציגים אותה מיד.
// הרשאת מאמן עדיין נבדקת מול authorizedCoaches/{uid}.
    var isCoach by rememberSaveable {
        mutableStateOf(
            sp.getString(
                "last_active_app_role",
                "trainee"
            )
                ?.equals(
                    "coach",
                    ignoreCase = true
                ) == true
        )
    }

    // שדות
    var username by rememberSaveable {
        mutableStateOf(sp.getString("remember_username", sp.getString("username", "") ?: "") ?: "")
    }

    var password by rememberSaveable {
        mutableStateOf(
            if (
                sp.getBoolean(
                    "remember_me_login",
                    false
                )
            ) {
                SecureLoginPasswordStore.load(
                    appCtx
                )
            } else {
                ""
            }
        )
    }

    var rememberMe by rememberSaveable { mutableStateOf(sp.getBoolean("remember_me_login", false)) }
    var loginError by remember { mutableStateOf(false) }
    var loginDebugText by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // אם rememberMe דלוק – טוענים קרדנציאלס מראש
    LaunchedEffect(Unit) {
        if (rememberMe) {
            username =
                sp.getString(
                    "remember_username",
                    ""
                ) ?: ""

            password =
                SecureLoginPasswordStore.load(
                    appCtx
                )
        }
    }

    // BringIntoView לשדות
    val usernameBring = remember { BringIntoViewRequester() }
    val passwordBring = remember { BringIntoViewRequester() }

    val fieldWidth = 0.88f
    val fieldHeight = 52.dp

    // —— ניווט חד־פעמי לאחר התחברות מוצלחת ——
    var loginSucceeded by rememberSaveable {
        mutableStateOf(false)
    }

    var navigated by rememberSaveable {
        mutableStateOf(false)
    }

    var showDeviceTransferDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var pendingTransferUid by rememberSaveable {
        mutableStateOf("")
    }

    var pendingTransferPreviousDeviceId by rememberSaveable {
        mutableStateOf("")
    }

    var deviceTransferInProgress by rememberSaveable {
        mutableStateOf(false)
    }

    var deviceTransferError by rememberSaveable {
        mutableStateOf(false)
    }

    fun cancelDeviceTransfer() {
        if (deviceTransferInProgress) return

        deviceTransferInProgress = true

        scope.launch {
            val tokenRemoved =
                withTimeoutOrNull(5.seconds) {
                    FcmTokenManager
                        .removeCurrentDeviceTokenBeforeSignOut()
                } == true

            if (!tokenRemoved) {
                Log.w(
                    "KMI_FCM_LOGOUT",
                    "FCM token cleanup not confirmed before device transfer cancellation"
                )
            }

            FirebaseAuth.getInstance().signOut()

            sp.edit {
                putBoolean("is_logged_in", false)
            }

            appCtx.getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            ).edit {
                putBoolean("is_logged_in", false)
            }

            pendingTransferUid = ""
            pendingTransferPreviousDeviceId = ""
            deviceTransferError = false
            deviceTransferInProgress = false
            showDeviceTransferDialog = false
        }
    }

    LaunchedEffect(loginSucceeded) {
        if (loginSucceeded && !navigated) {
            navigated = true

            // רענון FCM רק לאחר התחברות ואישור שיוך המכשיר.
            FcmTokenManager.refreshTokenAfterAuthorizedLogin()

            onLoginComplete()
        }
    }

    if (showRecoveryDialog) {
        AccountRecoveryScreen(
            onBack = {
                showRecoveryDialog = false
            }
        )
        return
    }

    if (showDeviceTransferDialog) {

        AlertDialog(
            onDismissRequest = {
                cancelDeviceTransfer()
            },
            title = {
                Text(
                    text = tr(
                        "החשבון פעיל במכשיר אחר",
                        "Account active on another device"
                    ),
                    style =
                        KmiTypography
                            .sectionTitle
                            .copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            )
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = tr(
                            "ניתן להשתמש בחשבון במכשיר אחד בלבד. האם להעביר את החשבון למכשיר הזה?",
                            "This account can be used on one device only. Transfer the account to this device?"
                        ),
                        style = KmiTypography.body
                    )

                    if (deviceTransferError) {
                        Text(
                            text = tr(
                                "לא הצלחנו להעביר את החשבון. בדוק את החיבור לאינטרנט ונסה שוב.",
                                "We couldn't transfer the account. Check your internet connection and try again."
                            ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            style =
                                KmiTypography
                                    .caption
                                    .copy(
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                        )
                    }
                }
            },
            confirmButton = {

                Button(
                    enabled =
                        !deviceTransferInProgress,
                    onClick = {

                        if (
                            pendingTransferUid
                                .isBlank()
                        ) {
                            return@Button
                        }

                        deviceTransferInProgress =
                            true

                        deviceTransferError =
                            false

                        scope.launch {

                            runCatching {
                                transferCurrentDevice(
                                    context = appCtx,
                                    uid = pendingTransferUid,
                                    expectedActiveDeviceId =
                                        pendingTransferPreviousDeviceId
                                )
                            }.onSuccess {

                                sp.edit {
                                    putBoolean(
                                        "is_logged_in",
                                        true
                                    )
                                }

                                appCtx.getSharedPreferences(
                                    "kmi_user",
                                    Context.MODE_PRIVATE
                                ).edit {
                                    putBoolean(
                                        "is_logged_in",
                                        true
                                    )
                                }

                                deviceTransferInProgress =
                                    false

                                showDeviceTransferDialog =
                                    false

                                pendingTransferUid =
                                    ""

                                pendingTransferPreviousDeviceId =
                                    ""

                                loginSucceeded =
                                    true

                            }.onFailure {

                                deviceTransferInProgress =
                                    false

                                deviceTransferError =
                                    true
                            }
                        }
                    }
                ) {

                    Text(
                        text =
                            if (
                                deviceTransferInProgress
                            ) {
                                tr(
                                    "מעביר...",
                                    "Transferring..."
                                )
                            } else {
                                tr(
                                    "העבר למכשיר הזה",
                                    "Transfer to this device"
                                )
                            },
                        style =
                            KmiTypography
                                .action
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold
                                )
                    )
                }
            },
            dismissButton = {

                TextButton(
                    enabled =
                        !deviceTransferInProgress,
                    onClick = {
                        cancelDeviceTransfer()
                    }
                ) {
                    Text(
                        text = tr(
                            "ביטול",
                            "Cancel"
                        ),
                        style =
                            KmiTypography.action.copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                    )
                }
            },
            shape =
                RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            ExistingUserLockedTopBar(
                title = tr("התחברות", "Login"),
                onBack = onBack
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                val selectedIndex = if (isCoach) 1 else 0

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = kmiSectionHeaderBrush()
                            )
                    ) {

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(1.dp)
                                .height(32.dp)
                                .background(
                                    Color.White.copy(alpha = 0.55f)
                                )
                        )

                        TabRow(
                            selectedTabIndex = selectedIndex,
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            divider = {},
                            indicator = { positions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(
                                            positions[selectedIndex]
                                        )
                                        .padding(
                                            horizontal = 26.dp
                                        ),
                                    height = 3.dp,
                                    color = Color.White
                                )
                            }
                        ) {

                            Tab(
                                selected = !isCoach,
                                onClick = {
                                    if (isCoach) {
                                        playStrongFeedback()

                                        isCoach = false
                                        loginError = false
                                    }
                                },
                                text = {
                                    Text(
                                        text = tr(
                                            "מתאמן",
                                            "Trainee"
                                        ),
                                        style =
                                            KmiTypography.action.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                        color =
                                            if (!isCoach) {
                                                Color.White
                                            } else {
                                                Color.White.copy(
                                                    alpha = 0.78f
                                                )
                                            },
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )
                                }
                            )

                            Tab(
                                selected = isCoach,
                                onClick = {
                                    if (!isCoach) {
                                        playStrongFeedback()
                                        isCoach = true
                                    }
                                },
                                text = {
                                    Text(
                                        text = tr(
                                            "מאמן",
                                            "Coach"
                                        ),
                                        style =
                                            KmiTypography.action.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                        color =
                                            if (isCoach) {
                                                Color.White
                                            } else {
                                                Color.White.copy(
                                                    alpha = 0.78f
                                                )
                                            },
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .imePadding()
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Horizontal
                            )
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    // מצב מאמן:
// אין יותר שדה קוד מאמן במסך.
// האימות מתבצע לפי Firebase UID מול authorizedCoaches/{uid}.
                    if (isCoach) {
                        Text(
                            text = tr(
                                "מצב מאמן יאומת מול השרת בעת ההתחברות",
                                "Coach mode will be verified by the server during login"
                            ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                                    .copy(alpha = 0.92f),
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(fieldWidth)
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = {
                            Text(
                                text = tr("שם משתמש", "Username"),
                                style = KmiTypography.caption,
                                color = Color.Black
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .defaultMinSize(minHeight = fieldHeight)
                            .bringIntoViewRequester(usernameBring)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        usernameBring.bringIntoView()
                                    }
                                }
                            },
                        singleLine = true,
                        textStyle = KmiTypography.body,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surface,
                            focusedTextColor =
                                MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor =
                                MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor =
                                MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            errorBorderColor =
                                MaterialTheme.colorScheme.error
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                text = tr("סיסמה", "Password"),
                                style = KmiTypography.caption,
                                color = Color.Black
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .defaultMinSize(minHeight = fieldHeight)
                            .bringIntoViewRequester(passwordBring)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        passwordBring.bringIntoView()
                                    }
                                }
                            },
                        singleLine = true,
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = KmiTypography.body,
                        trailingIcon = {
                            val icon =
                                if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                }

                            val desc =
                                if (passwordVisible) {
                                    tr(
                                        "הסתר סיסמה",
                                        "Hide password"
                                    )
                                } else {
                                    tr(
                                        "הצג סיסמה",
                                        "Show password"
                                    )
                                }

                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                },
                                modifier = Modifier.size(
                                    KmiIconSize.medium
                                )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = desc,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(
                                        KmiIconSize.small
                                    )
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surface,
                            focusedTextColor =
                                MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor =
                                MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor =
                                MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            errorBorderColor =
                                MaterialTheme.colorScheme.error
                        )
                    )

                    // שמירה לכניסה הבאה
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .heightIn(min = 48.dp)
                            .background(
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.92f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor =
                                    MaterialTheme.colorScheme.primary,
                                uncheckedColor =
                                    MaterialTheme.colorScheme.outline,
                                checkmarkColor =
                                    MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        Spacer(
                            Modifier.width(2.dp)
                        )

                        Text(
                            text = tr(
                                "שמירה לכניסה הבאה",
                                "Remember me"
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            style =
                                KmiTypography.body.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Right
                                }
                        )
                    }

                    if (loginError) {
                        Text(
                            text = tr(
                                "פרטי ההתחברות שגויים",
                                "Invalid login details"
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(fieldWidth)
                        )
                    }

                    Button(
                        onClick = {
                            playStrongFeedback()
                            loginDebugText = null

                            scope.launch {

                                if (
                                    username.isBlank() ||
                                    password.isBlank()
                                ) {
                                    loginError = true
                                    return@launch
                                }

                                val loginEmail =
                                    if (
                                        Patterns.EMAIL_ADDRESS
                                            .matcher(
                                                username.trim()
                                            )
                                            .matches()
                                    ) {
                                        username.trim()

                                    } else {

                                        runCatching {

                                            val result =
                                                FirebaseFunctions
                                                    .getInstance()
                                                    .getHttpsCallable(
                                                        "resolveUsernameLoginEmail"
                                                    )
                                                    .call(
                                                        mapOf(
                                                            "username" to
                                                                    username.trim()
                                                        )
                                                    )
                                                    .await()

                                            val payload =
                                                result.data as? Map<*, *>
                                                    ?: return@runCatching ""

                                            val found =
                                                payload["found"] as? Boolean
                                                    ?: false

                                            val resolvedEmail =
                                                payload["loginEmail"]
                                                    ?.toString()
                                                    ?.trim()
                                                    .orEmpty()

                                            if (
                                                found &&
                                                Patterns.EMAIL_ADDRESS
                                                    .matcher(
                                                        resolvedEmail
                                                    )
                                                    .matches()
                                            ) {
                                                resolvedEmail
                                            } else {
                                                ""
                                            }

                                        }.getOrDefault("")
                                    }

                                if (loginEmail.isBlank()) {
                                    loginError = true
                                    return@launch
                                }

                                val firebaseUser =
                                    runCatching {
                                        FirebaseAuth.getInstance()
                                            .signInWithEmailAndPassword(
                                                loginEmail,
                                                password
                                            )
                                            .await()
                                            .user
                                    }.getOrNull()

                                if (firebaseUser == null) {
                                    loginError = true
                                    return@launch
                                }

                                val resolvedLoginUid =
                                    firebaseUser.uid

                                val deviceBindingResult =
                                    try {
                                        checkCurrentDevice(
                                            context = appCtx,
                                            uid = resolvedLoginUid
                                        )
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Log.w(
                                            "KMI_DEVICE_AUTH",
                                            "Device authorization check failed",
                                            e
                                        )

                                        sp.edit {
                                            putBoolean(
                                                "is_logged_in",
                                                false
                                            )
                                        }

                                        appCtx.getSharedPreferences(
                                            "kmi_user",
                                            Context.MODE_PRIVATE
                                        ).edit {
                                            putBoolean(
                                                "is_logged_in",
                                                false
                                            )
                                        }

                                        FirebaseAuth
                                            .getInstance()
                                            .signOut()

                                        Toast.makeText(
                                            appCtx,
                                            tr(
                                                "לא ניתן לאמת את המכשיר כרגע. בדוק את החיבור לאינטרנט ונסה שוב.",
                                                "Unable to verify this device. Check your internet connection and try again."
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@launch
                                    }

                                var resolvedCoachRole: String
                                var resolvedCoachActive: Boolean
                                var resolvedCoachName = ""

                                var resolvedCanOpenCoachDrawer = false
                                var resolvedCanViewTrainees = false
                                var resolvedCanManageTrainees = false
                                var resolvedCanManageAttendance = false
                                var resolvedCanManageInternalExams = false
                                var resolvedCanViewPaymentReports = false
                                var resolvedCanManagePayments = false
                                var resolvedCanSendBroadcasts = false

                                val coachOk =
                                    if (isCoach) {
                                        if (resolvedLoginUid.isBlank()) {
                                            false
                                        } else {
                                            val coachDoc =
                                                runCatching {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("authorizedCoaches")
                                                        .document(resolvedLoginUid)
                                                        .get()
                                                        .await()
                                                }.getOrNull()

                                            if (coachDoc?.exists() != true) {
                                                false
                                            } else {
                                                resolvedCoachActive =
                                                    coachDoc.getBoolean("active") == true

                                                resolvedCoachRole =
                                                    coachDoc.getString("role")
                                                        .orEmpty()

                                                resolvedCoachName =
                                                    coachDoc.getString("fullName")
                                                        .orEmpty()

                                                resolvedCanOpenCoachDrawer =
                                                    coachDoc.getBoolean("canOpenCoachDrawer") == true

                                                resolvedCanViewTrainees =
                                                    coachDoc.getBoolean("canViewTrainees") == true

                                                resolvedCanManageTrainees =
                                                    coachDoc.getBoolean("canManageTrainees") == true

                                                resolvedCanManageAttendance =
                                                    coachDoc.getBoolean("canManageAttendance") == true

                                                resolvedCanManageInternalExams =
                                                    coachDoc.getBoolean("canManageInternalExams") == true ||
                                                            coachDoc.getBoolean("canManageExams") == true

                                                resolvedCanViewPaymentReports =
                                                    coachDoc.getBoolean("canViewPaymentReports") == true

                                                resolvedCanManagePayments =
                                                    coachDoc.getBoolean("canManagePayments") == true

                                                resolvedCanSendBroadcasts =
                                                    coachDoc.getBoolean("canSendBroadcasts") == true

                                                resolvedCoachActive &&
                                                        resolvedCoachRole.equals(
                                                            "coach",
                                                            ignoreCase = true
                                                        )
                                            }
                                        }
                                    } else {
                                        resolvedLoginUid.isNotBlank()
                                    }

                                if (!coachOk) {
                                    loginError = true

                                    // אין להשאיר סשן Firebase פעיל
                                    // לאחר שניסיון כניסה כמאמן נדחה.
                                    sp.edit {
                                        putBoolean("is_logged_in", false)
                                    }

                                    appCtx.getSharedPreferences(
                                        "kmi_user",
                                        Context.MODE_PRIVATE
                                    ).edit {
                                        putBoolean("is_logged_in", false)
                                    }

                                    FirebaseAuth.getInstance().signOut()

                                    // לא משנים הרשאות מאמן שמורות.
                                    return@launch
                                }

                                loginError = false

                                val userPrefs =
                                    appCtx.getSharedPreferences(
                                        "kmi_user",
                                        Context.MODE_PRIVATE
                                    )

                                if (rememberMe) {
                                    sp.edit {
                                        putBoolean(
                                            "remember_me_login",
                                            true
                                        )
                                        putString(
                                            "remember_username",
                                            username.trim()
                                        )
                                        remove("remember_password")
                                    }

                                    userPrefs.edit {
                                        putBoolean(
                                            "remember_me_login",
                                            true
                                        )
                                        putString(
                                            "remember_username",
                                            username.trim()
                                        )
                                        remove("remember_password")
                                    }

                                    SecureLoginPasswordStore.save(
                                        context = appCtx,
                                        password = password
                                    )

                                } else {
                                    sp.edit {
                                        putBoolean(
                                            "remember_me_login",
                                            false
                                        )
                                        remove("remember_username")
                                        remove("remember_password")
                                    }

                                    userPrefs.edit {
                                        putBoolean(
                                            "remember_me_login",
                                            false
                                        )
                                        remove("remember_username")
                                        remove("remember_password")
                                    }

                                    SecureLoginPasswordStore.clear(
                                        appCtx
                                    )
                                }

                                val role = if (isCoach) "coach" else "trainee"

                                sp.edit {
                                    putString(
                                        "uid",
                                        resolvedLoginUid
                                    )
                                    putString(
                                        "profile_completed_uid",
                                        resolvedLoginUid
                                    )
                                    putString(
                                        "user_role",
                                        role
                                    )
                                    putString(
                                        "last_active_app_role",
                                        role
                                    )
                                    remove("coach_code")
                                    putString(
                                        "coach_name",
                                        if (role == "coach") {
                                            resolvedCoachName
                                        } else {
                                            ""
                                        }
                                    )
                                    putBoolean(
                                        "coach_authorized",
                                        role == "coach"
                                    )
                                    putBoolean(
                                        "can_open_coach_drawer",
                                        role == "coach" &&
                                                resolvedCanOpenCoachDrawer
                                    )
                                    putBoolean(
                                        "can_view_trainees",
                                        role == "coach" &&
                                                resolvedCanViewTrainees
                                    )
                                    putBoolean(
                                        "can_manage_trainees",
                                        role == "coach" &&
                                                resolvedCanManageTrainees
                                    )
                                    putBoolean(
                                        "can_manage_attendance",
                                        role == "coach" &&
                                                resolvedCanManageAttendance
                                    )
                                    putBoolean(
                                        "can_manage_internal_exams",
                                        role == "coach" &&
                                                resolvedCanManageInternalExams
                                    )
                                    putBoolean(
                                        "can_view_payment_reports",
                                        role == "coach" &&
                                                resolvedCanViewPaymentReports
                                    )
                                    putBoolean(
                                        "can_manage_payments",
                                        role == "coach" &&
                                                resolvedCanManagePayments
                                    )
                                    putBoolean(
                                        "can_send_broadcasts",
                                        role == "coach" &&
                                                resolvedCanSendBroadcasts
                                    )
                                    putBoolean(
                                        "is_logged_in",
                                        deviceBindingResult.result ==
                                                DeviceBindingResult.ALLOWED
                                    )
                                }

                                appCtx.getSharedPreferences(
                                    "kmi_user",
                                    Context.MODE_PRIVATE
                                ).edit {
                                    putString(
                                        "uid",
                                        resolvedLoginUid
                                    )
                                    putString(
                                        "profile_completed_uid",
                                        resolvedLoginUid
                                    )
                                    putString(
                                        "user_role",
                                        role
                                    )
                                    putString(
                                        "last_active_app_role",
                                        role
                                    )
                                    remove("coach_code")
                                    putString(
                                        "coach_name",
                                        if (role == "coach") {
                                            resolvedCoachName
                                        } else {
                                            ""
                                        }
                                    )
                                    putBoolean(
                                        "coach_authorized",
                                        role == "coach"
                                    )
                                    putBoolean(
                                        "can_open_coach_drawer",
                                        role == "coach" &&
                                                resolvedCanOpenCoachDrawer
                                    )
                                    putBoolean(
                                        "can_view_trainees",
                                        role == "coach" &&
                                                resolvedCanViewTrainees
                                    )
                                    putBoolean(
                                        "can_manage_trainees",
                                        role == "coach" &&
                                                resolvedCanManageTrainees
                                    )
                                    putBoolean(
                                        "can_manage_attendance",
                                        role == "coach" &&
                                                resolvedCanManageAttendance
                                    )
                                    putBoolean(
                                        "can_manage_internal_exams",
                                        role == "coach" &&
                                                resolvedCanManageInternalExams
                                    )
                                    putBoolean(
                                        "can_view_payment_reports",
                                        role == "coach" &&
                                                resolvedCanViewPaymentReports
                                    )
                                    putBoolean(
                                        "can_manage_payments",
                                        role == "coach" &&
                                                resolvedCanManagePayments
                                    )
                                    putBoolean(
                                        "can_send_broadcasts",
                                        role == "coach" &&
                                                resolvedCanSendBroadcasts
                                    )
                                    putBoolean(
                                        "is_logged_in",
                                        deviceBindingResult.result ==
                                                DeviceBindingResult.ALLOWED
                                    )
                                }

                                kmiPrefs.username = username

                                if (
                                    deviceBindingResult.result ==
                                    DeviceBindingResult.TRANSFER_REQUIRED
                                ) {
                                    pendingTransferUid =
                                        resolvedLoginUid

                                    pendingTransferPreviousDeviceId =
                                        deviceBindingResult.activeDeviceId

                                    deviceTransferError =
                                        false

                                    showDeviceTransferDialog =
                                        true
                                } else {
                                    loginSucceeded =
                                        true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .height(58.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.primary,
                            contentColor =
                                MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation =
                            ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                    ) {
                        Text(
                            text = tr(
                                "התחבר",
                                "Login"
                            ),
                            style = KmiTypography.action.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        onClick = {
                            showRecoveryDialog = true
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(
                                    "שכחתי סיסמה / שם משתמש",
                                    "Forgot password / username"
                                ),
                                style = KmiTypography.body.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ===== קרדיט קבוע בתחתית המסך =====
            // מוצג רק כשהמקלדת סגורה.
            // כשהמקלדת פתוחה הוא מוסתר כדי לא לכסות את שדות ההתחברות.
            if (!isKeyboardVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = tr(
                            "❤️ פותח באהבה ע\"י יובל פולק ❤️",
                            "❤️ Developed with love by Yuval Polak ❤️"
                        ),
                        style = KmiTypography.body.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountRecoveryScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val langManager = remember {
        AppLanguageManager(ctx)
    }

    val isEnglish =
        langManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    fun tr(
        he: String,
        en: String
    ): String =
        if (isEnglish) en else he

    var recoveryMode by rememberSaveable {
        mutableStateOf("password")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var errorText by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var successText by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isSending by rememberSaveable {
        mutableStateOf(false)
    }

    val isUsernameRecovery =
        recoveryMode == "username"

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr(
                    "שחזור חשבון",
                    "Account Recovery"
                ),
                onBack = onBack,
                showTopHome = false,
                showTopSearch = false,
                showTopShare = false,
                showBottomActions = true,
                lockHome = true,
                lockSearch = true,
                lockAllActions = true,
                centerTitle = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = kmiSectionHeaderBrush()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isUsernameRecovery) {
                                    tr(
                                        "שכחת את שם המשתמש?",
                                        "Forgot your username?"
                                    )
                                } else {
                                    tr(
                                        "שכחת את הסיסמה?",
                                        "Forgot your password?"
                                    )
                                },
                            style =
                                KmiTypography.action.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .imePadding()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 18.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            if (isUsernameRecovery) {
                                tr(
                                    "נשלח את שם המשתמש לכתובת האימייל הרשומה בחשבון.",
                                    "We'll send your username to the email address registered on your account."
                                )
                            } else {
                                tr(
                                    "נשלח אליך קישור מאובטח ליצירת סיסמה חדשה.",
                                    "We'll send you a secure link to create a new password."
                                )
                            },
                        modifier =
                            Modifier.fillMaxWidth(0.88f),
                        style =
                            KmiTypography.body,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        Modifier.height(24.dp)
                    )

                    /*
                     * Segmented control
                     */
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(18.dp),
                        color =
                            Color.White.copy(alpha = 0.88f),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.42f)
                            ),
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                        ) {

                            Surface(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                shape =
                                    RoundedCornerShape(14.dp),
                                color =
                                    if (!isUsernameRecovery) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.24f)
                                    },
                                tonalElevation = 0.dp,
                                shadowElevation =
                                    if (!isUsernameRecovery) {
                                        2.dp
                                    } else {
                                        0.dp
                                    },
                                onClick = {
                                    if (!isSending) {
                                        recoveryMode =
                                            "password"

                                        errorText = null
                                        successText = null
                                    }
                                }
                            ) {
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize(),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        text = tr(
                                            "סיסמה",
                                            "Password"
                                        ),
                                        style =
                                            KmiTypography
                                                .action
                                                .copy(
                                                    fontWeight =
                                                        FontWeight.Bold
                                                ),
                                        color =
                                            if (!isUsernameRecovery) {
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                            } else {
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                            }
                                    )
                                }
                            }

                            Surface(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                shape =
                                    RoundedCornerShape(14.dp),
                                color =
                                    if (isUsernameRecovery) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.24f)
                                    },
                                tonalElevation = 0.dp,
                                shadowElevation =
                                    if (isUsernameRecovery) {
                                        2.dp
                                    } else {
                                        0.dp
                                    },
                                onClick = {
                                    if (!isSending) {
                                        recoveryMode =
                                            "username"

                                        errorText = null
                                        successText = null
                                    }
                                }
                            ) {
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize(),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        text = tr(
                                            "שם משתמש",
                                            "Username"
                                        ),
                                        style =
                                            KmiTypography
                                                .action
                                                .copy(
                                                    fontWeight =
                                                        FontWeight.Bold
                                                ),
                                        color =
                                            if (isUsernameRecovery) {
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                            } else {
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(28.dp)
                    )

                    /*
                     * שדה אימייל
                     */
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorText = null
                            successText = null
                        },
                        label = {
                            Text(
                                text = tr(
                                    "כתובת אימייל",
                                    "Email address"
                                ),
                                style =
                                    KmiTypography.caption,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 58.dp
                                ),
                        singleLine = true,
                        enabled = !isSending,
                        isError =
                            errorText != null,
                        textStyle =
                            KmiTypography.body,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Email,
                                imeAction =
                                    ImeAction.Done
                            ),
                        shape =
                            RoundedCornerShape(18.dp),
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedContainerColor =
                                        Color.White.copy(alpha = 0.96f),
                                    unfocusedContainerColor =
                                        Color.White.copy(alpha = 0.92f),
                                    disabledContainerColor =
                                        Color.White.copy(alpha = 0.92f),
                                    focusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    unfocusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    disabledTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(alpha = 0.82f),
                                    focusedBorderColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(alpha = 0.70f),
                                    unfocusedBorderColor =
                                        MaterialTheme
                                            .colorScheme
                                            .outline
                                            .copy(alpha = 0.34f),
                                    disabledBorderColor =
                                        MaterialTheme
                                            .colorScheme
                                            .outline
                                            .copy(alpha = 0.28f),
                                    focusedLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    unfocusedLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    disabledLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(alpha = 0.88f),
                                    cursorColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                    )

                    if (
                        !errorText
                            .isNullOrBlank()
                    ) {

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                errorText.orEmpty(),
                            modifier =
                                Modifier.fillMaxWidth(),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            style =
                                KmiTypography
                                    .caption
                                    .copy(
                                        fontWeight =
                                            FontWeight.Bold
                                    ),
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Right
                                }
                        )
                    }

                    if (
                        !successText
                            .isNullOrBlank()
                    ) {

                        Spacer(
                            Modifier.height(14.dp)
                        )

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(18.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                                    .copy(alpha = 0.72f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {

                            Text(
                                text =
                                    successText
                                        .orEmpty(),
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer,
                                style =
                                    KmiTypography
                                        .caption
                                        .copy(
                                            fontWeight =
                                                FontWeight.Bold
                                        ),
                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(22.dp)
                    )

                    /*
                     * CTA ראשי
                     */
                    Button(
                        enabled = !isSending,
                        onClick = {

                            val cleanEmail =
                                email.trim()

                            when {

                                cleanEmail.isBlank() -> {

                                    errorText =
                                        tr(
                                            "יש להזין כתובת אימייל.",
                                            "Please enter an email address."
                                        )
                                }

                                !Patterns
                                    .EMAIL_ADDRESS
                                    .matcher(
                                        cleanEmail
                                    )
                                    .matches() -> {

                                    errorText =
                                        tr(
                                            "כתובת האימייל אינה תקינה.",
                                            "Invalid email address."
                                        )
                                }

                                else -> {

                                    isSending = true
                                    errorText = null
                                    successText = null

                                    scope.launch {

                                        if (
                                            isUsernameRecovery
                                        ) {

                                            runCatching {

                                                FirebaseFunctions
                                                    .getInstance()
                                                    .getHttpsCallable(
                                                        "recoverUsername"
                                                    )
                                                    .call(
                                                        mapOf(
                                                            "email" to
                                                                    cleanEmail
                                                        )
                                                    )
                                                    .await()

                                            }.onSuccess {

                                                isSending =
                                                    false

                                                successText =
                                                    tr(
                                                        "אם האימייל קיים במערכת, שלחנו אליו את שם המשתמש.",
                                                        "If the email exists in our system, we've sent your username."
                                                    )

                                            }.onFailure {

                                                isSending =
                                                    false

                                                errorText =
                                                    tr(
                                                        "לא הצלחנו לבצע את הבקשה. נסה שוב בעוד מספר רגעים.",
                                                        "We couldn't process the request. Please try again shortly."
                                                    )
                                            }

                                        } else {

                                            runCatching {

                                                FirebaseFunctions
                                                    .getInstance()
                                                    .getHttpsCallable(
                                                        "recoverPassword"
                                                    )
                                                    .call(
                                                        mapOf(
                                                            "email" to
                                                                    cleanEmail,
                                                            "lang" to
                                                                    if (isEnglish) {
                                                                        "en"
                                                                    } else {
                                                                        "he"
                                                                    }
                                                        )
                                                    )
                                                    .await()

                                            }.onSuccess {

                                                isSending =
                                                    false

                                                successText =
                                                    tr(
                                                        "אם האימייל קיים במערכת, שלחנו אליו קישור מאובטח לאיפוס הסיסמה.",
                                                        "If the email exists in our system, we've sent a secure password reset link."
                                                    )

                                            }.onFailure {

                                                isSending =
                                                    false

                                                errorText =
                                                    tr(
                                                        "לא הצלחנו לבצע את הבקשה. נסה שוב בעוד מספר רגעים.",
                                                        "We couldn't process the request. Please try again shortly."
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        shape =
                            RoundedCornerShape(20.dp),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    contentColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary,
                                    disabledContainerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(alpha = 0.90f),
                                    disabledContentColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                            .copy(alpha = 0.92f)
                                ),
                        elevation =
                            ButtonDefaults
                                .buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 0.dp
                                )
                    ) {

                        Text(
                            text =
                                if (isSending) {
                                    tr(
                                        "שולח...",
                                        "Sending..."
                                    )
                                } else if (
                                    isUsernameRecovery
                                ) {
                                    tr(
                                        "שלח את שם המשתמש",
                                        "Send username"
                                    )
                                } else {
                                    tr(
                                        "שלח קישור לאיפוס",
                                        "Send reset link"
                                    )
                                },
                            style =
                                KmiTypography
                                    .action
                                    .copy(
                                        fontWeight =
                                            FontWeight.Bold
                                    ),
                            maxLines = 1
                        )
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    /*
                     * שורת אבטחה
                     */
                    Text(
                        text = tr(
                            "הפרטים שלך נשארים פרטיים ומאובטחים",
                            "Your account details remain private and secure"
                        ),
                        style =
                            KmiTypography.caption.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        onClick = {
                            if (!isSending) {
                                onBack()
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(
                                    "חזרה להתחברות",
                                    "Back to login"
                                ),
                                style =
                                    KmiTypography
                                        .action
                                        .copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(22.dp)
                    )
                }
            }
        }
    }
}