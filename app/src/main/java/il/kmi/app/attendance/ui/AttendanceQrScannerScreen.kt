package il.kmi.app.attendance.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

internal data class KmiAttendanceQrPayload(
    val occurrenceId: String,
    val token: String
)

internal fun parseKmiAttendanceQr(
    rawValue: String
): KmiAttendanceQrPayload? {
    return runCatching {
        val json = JSONObject(rawValue)

        val type = json.optString("type").trim()
        val occurrenceId =
            json.optString("occurrenceId").trim()
        val token =
            json.optString("token").trim()

        if (
            type != "kami_attendance_qr" ||
            occurrenceId.isBlank() ||
            token.isBlank()
        ) {
            return@runCatching null
        }

        KmiAttendanceQrPayload(
            occurrenceId = occurrenceId,
            token = token
        )
    }.getOrNull()
}

/**
 * K.M.I — Secure Attendance QR Scanner.
 *
 * סריקת QR באמצעות CameraX ו-ML Kit.
 * אימות המשתמש ורישום הנוכחות מתבצעים
 * באמצעות scanKamiAttendanceQr בשרת.
 *
 * אין כתיבה ישירה של נוכחות מתוך המסך.
 * המצלמה נעצרת בזמן אימות, הצלחה או שגיאה.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun AttendanceQrScannerScreen(
    onBack: () -> Unit,
    onAttendanceRegistered: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val languageManager = remember(context) {
        AppLanguageManager(context)
    }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    val scope = rememberCoroutineScope()

    var scanInProgress by remember {
        mutableStateOf(false)
    }

    var scanCompleted by remember {
        mutableStateOf(false)
    }

    var alreadyRecorded by remember {
        mutableStateOf(false)
    }

    var scanError by remember {
        mutableStateOf<String?>(null)
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraError by remember {
        mutableStateOf(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted
        }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType =
                PreviewView.ScaleType.FILL_CENTER
        }
    }

    val scanAccepted = remember {
        AtomicBoolean(false)
    }

    val handleQrDetected: (KmiAttendanceQrPayload) -> Unit =
        { payload ->

            if (!scanInProgress && !scanCompleted) {

                scanInProgress = true
                scanError = null

                scope.launch {
                    try {
                        check(
                            FirebaseAuth.getInstance().currentUser != null
                        ) {
                            "Authentication required"
                        }

                        val result =
                            FirebaseFunctions
                                .getInstance("europe-west1")
                                .getHttpsCallable(
                                    "scanKamiAttendanceQr"
                                )
                                .call(
                                    mapOf(
                                        "occurrenceId" to
                                                payload.occurrenceId,
                                        "token" to payload.token
                                    )
                                )
                                .await()

                        val response =
                            result.data as? Map<*, *>

                        val accepted =
                            response?.get("accepted") == true

                        val returnedOccurrenceId =
                            response?.get("occurrenceId") as? String

                        if (
                            !accepted ||
                            returnedOccurrenceId != payload.occurrenceId
                        ) {
                            error("Invalid attendance response")
                        }

                        alreadyRecorded =
                            response["alreadyRecorded"] == true

                        scanCompleted = true

                    } catch (cancelled: CancellationException) {
                        throw cancelled

                    } catch (error: Exception) {

                        scanError =
                            if (
                                FirebaseAuth.getInstance()
                                    .currentUser == null
                            ) {
                                if (isEnglish) {
                                    "Please sign in to K.M.I and try again."
                                } else {
                                    "יש להתחבר ל־K.M.I ולנסות שוב."
                                }

                            } else if (error is FirebaseFunctionsException) {

                                when (error.code) {

                                    FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                                        if (isEnglish) {
                                            "Please sign in again."
                                        } else {
                                            "יש להתחבר מחדש לחשבון."
                                        }

                                    FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                                        if (isEnglish) {
                                            "Attendance could not be verified. Check your training group."
                                        } else {
                                            "לא ניתן לאמת נוכחות. יש לבדוק את השיוך לקבוצת האימון."
                                        }

                                    FirebaseFunctionsException.Code.NOT_FOUND ->
                                        if (isEnglish) {
                                            "This attendance QR is no longer available."
                                        } else {
                                            "קוד הנוכחות אינו זמין עוד."
                                        }

                                    FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                                        if (isEnglish) {
                                            "The scanned attendance QR is invalid."
                                        } else {
                                            "קוד ה־QR שנסרק אינו תקין."
                                        }

                                    FirebaseFunctionsException.Code.FAILED_PRECONDITION -> {
                                        val serverMessage =
                                            error.message.orEmpty()

                                        when {
                                            serverMessage.contains(
                                                "outside",
                                                ignoreCase = true
                                            ) ||
                                                    serverMessage.contains(
                                                        "window",
                                                        ignoreCase = true
                                                    ) ->
                                                if (isEnglish) {
                                                    "QR attendance is not available at this time."
                                                } else {
                                                    "לא ניתן לרשום נוכחות באמצעות QR בשעה זו."
                                                }

                                            serverMessage.contains(
                                                "cancelled",
                                                ignoreCase = true
                                            ) ->
                                                if (isEnglish) {
                                                    "This training has been cancelled."
                                                } else {
                                                    "האימון בוטל."
                                                }

                                            serverMessage.contains(
                                                "already saved",
                                                ignoreCase = true
                                            ) ->
                                                if (isEnglish) {
                                                    "Attendance for this training has already been closed."
                                                } else {
                                                    "דוח הנוכחות לאימון זה כבר נשמר ונסגר."
                                                }

                                            else ->
                                                if (isEnglish) {
                                                    "Attendance cannot be registered for this training."
                                                } else {
                                                    "לא ניתן לרשום נוכחות לאימון זה."
                                                }
                                        }
                                    }

                                    FirebaseFunctionsException.Code.UNAVAILABLE ->
                                        if (isEnglish) {
                                            "The service is temporarily unavailable. Please try again."
                                        } else {
                                            "השירות אינו זמין כרגע. יש לנסות שוב."
                                        }

                                    else ->
                                        if (isEnglish) {
                                            "Attendance registration failed. Please try again."
                                        } else {
                                            "רישום הנוכחות נכשל. יש לנסות שוב."
                                        }
                                }

                            } else {
                                if (isEnglish) {
                                    "Could not verify attendance. Please try again."
                                } else {
                                    "לא ניתן לאמת את הנוכחות. יש לנסות שוב."
                                }
                            }
                    } finally {
                        scanInProgress = false
                    }
                }
            }
        }

    val latestOnQrDetected by
    rememberUpdatedState(handleQrDetected)

    DisposableEffect(
        cameraPermissionGranted,
        lifecycleOwner,
        previewView,
        scanInProgress,
        scanCompleted,
        scanError
    ) {
        if (
            !cameraPermissionGranted ||
            scanInProgress ||
            scanCompleted ||
            scanError != null
        ) {
            onDispose { }
        } else {
            var disposed = false
            var cameraProvider: ProcessCameraProvider? = null
            var imageAnalysis: ImageAnalysis? = null

            val scannerOptions =
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()

            val barcodeScanner =
                BarcodeScanning.getClient(
                    scannerOptions
                )

            val providerFuture =
                ProcessCameraProvider.getInstance(context)

            providerFuture.addListener(
                {
                    if (!disposed) {
                        try {
                            val provider = providerFuture.get()

                            cameraProvider = provider

                            val preview =
                                Preview.Builder()
                                    .build()
                                    .also {
                                        it.surfaceProvider =
                                            previewView.surfaceProvider
                                    }

                            val analysis =
                                ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()

                            imageAnalysis = analysis

                            analysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context)
                            ) { imageProxy ->

                                if (disposed || scanAccepted.get()) {
                                    imageProxy.close()
                                } else {
                                    val mediaImage =
                                        imageProxy.image

                                    if (mediaImage == null) {
                                        imageProxy.close()
                                    } else {
                                        val image =
                                            InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->

                                                if (
                                                    !disposed &&
                                                    !scanAccepted.get()
                                                ) {
                                                    val payload =
                                                        barcodes.firstNotNullOfOrNull { barcode ->
                                                            barcode.rawValue
                                                                ?.let(::parseKmiAttendanceQr)
                                                        }

                                                    if (
                                                        payload != null &&
                                                        scanAccepted.compareAndSet(
                                                            false,
                                                            true
                                                        )
                                                    ) {
                                                        latestOnQrDetected(
                                                            payload
                                                        )
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    }
                                }
                            }

                            provider.unbindAll()

                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )

                            cameraError = false
                        } catch (_: Exception) {
                            cameraError = true
                        }
                    }
                },
                ContextCompat.getMainExecutor(context)
            )

            onDispose {
                disposed = true

                imageAnalysis?.clearAnalyzer()

                cameraProvider?.unbindAll()

                barcodeScanner.close()
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                if (isEnglish) LayoutDirection.Ltr
                else LayoutDirection.Rtl
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                KmiTopBar(
                    title = if (isEnglish) {
                        "Attendance QR Scanner"
                    } else {
                        "סריקת QR לנוכחות"
                    },
                    onHome = onBack,
                    showTopHome = true,
                    showTopShare = false,
                    showTopSearch = false,
                    showMenu = false,
                    showBottomActions = false,
                    showRoleBadge = false,
                    showModePill = false,
                    currentLang = if (isEnglish) "en" else "he"
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(kmiScreenBackgroundBrush())
                    .padding(innerPadding)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = if (isEnglish) {
                    "Scan training attendance QR"
                } else {
                    "סריקת QR לנוכחות באימון"
                },
                style = KmiTypography.cardTitle,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

        when {
            scanInProgress -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    KmiLoadingRings(
                        text = if (isEnglish) {
                            "Verifying attendance..."
                        } else {
                            "מאמת נוכחות מול השרת..."
                        },
                        size = 82.dp
                    )
                }
            }

            scanCompleted -> {
                Text(
                    text =
                        if (alreadyRecorded) {
                            if (isEnglish) {
                                "Your attendance has already been registered."
                            } else {
                                "הנוכחות שלך כבר נרשמה לאימון."
                            }
                        } else {
                            if (isEnglish) {
                                "Your attendance was registered successfully!"
                            } else {
                                "הנוכחות שלך נרשמה בהצלחה!"
                            }
                        },
                    color = MaterialTheme.colorScheme.primary,
                    style = KmiTypography.cardTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onAttendanceRegistered
                ) {
                    Text(
                        if (isEnglish) "Done" else "סיום"
                    )
                }
            }

            !cameraPermissionGranted -> {
                Text(
                    text = if (isEnglish) {
                        "Camera permission is required to scan QR codes."
                    } else {
                        "נדרשת הרשאת מצלמה לסריקת קוד QR."
                    }
                )

                Button(
                    onClick = {
                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                ) {
                    Text(
                        if (isEnglish) {
                            "Allow camera"
                        } else {
                            "אפשר שימוש במצלמה"
                        }
                    )
                }
            }

            cameraError -> {
                Text(
                    text = if (isEnglish) {
                        "Unable to open the camera."
                    } else {
                        "לא ניתן לפתוח את המצלמה."
                    }
                )
            }

            else -> {
                if (!scanInProgress && scanError == null) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Text(
                        text =
                            if (isEnglish) {
                                "Point the camera at the QR code shown by your coach."
                            } else {
                                "כוון את המצלמה לקוד ה־QR שמציג המאמן."
                            },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                scanError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = {
                            if (!scanInProgress && !scanCompleted) {
                                scanAccepted.set(false)
                                cameraError = false
                                scanError = null
                            }
                        },
                        enabled = !scanInProgress && !scanCompleted
                    ) {
                        Text(
                            if (isEnglish) {
                                "Scan again"
                            } else {
                                "סרוק שוב"
                            }
                        )
                    }
                }
            }
        }

                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Back"
                        } else {
                            "חזרה"
                        },
                        style = KmiTypography.action
                    )
                }
            }
        }
    }
}