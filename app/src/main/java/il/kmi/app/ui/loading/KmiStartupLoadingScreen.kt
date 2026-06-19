package il.kmi.app.ui.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import il.kmi.app.domain.Explanations

//-----------------------------------------------------------------------
private data class LoadingStage(
    val titleHe: String,
    val titleEn: String,
    val icon: ImageVector
)

@Composable
fun KmiStartupLoadingScreen(
    isEnglish: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    val stages = remember {
        listOf(
            LoadingStage(
                titleHe = "טעינת נתוני משתמש",
                titleEn = "Loading user data",
                icon = Icons.Filled.Person
            ),
            LoadingStage(
                titleHe = "טעינת נתוני מערכת",
                titleEn = "Loading system data",
                icon = Icons.Filled.Settings
            ),
            LoadingStage(
                titleHe = "בדיקת הרשאות ואבטחה",
                titleEn = "Checking permissions and security",
                icon = Icons.Filled.Security
            ),
            LoadingStage(
                titleHe = "סנכרון נתוני אימון",
                titleEn = "Syncing training data",
                icon = Icons.Filled.CloudSync
            ),
            LoadingStage(
                titleHe = "הכנת סביבת האימון",
                titleEn = "Preparing training environment",
                icon = Icons.Filled.CheckCircle
            )
        )
    }

    var currentStageIndex by remember { mutableIntStateOf(0) }
    var completedStagesInCycle by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val preloadJob = launch {
            runCatching {
                KmiStartupPreloader.preload(context)
            }
        }

        val totalDuration = 10_000L
        val tick = 100L
        val totalSteps = (totalDuration / tick).toInt()

        repeat(totalSteps) { step ->
            delay(tick)

            progress = (step + 1) / totalSteps.toFloat()

            val stageProgress = progress * stages.size
            val safeIndex = stageProgress.toInt().coerceAtMost(stages.lastIndex)

            currentStageIndex = safeIndex
            completedStagesInCycle = safeIndex
        }

        currentStageIndex = stages.lastIndex
        completedStagesInCycle = stages.size
        progress = 1f

        // נותן לטעינות אמת עוד רגע קצר להסתיים,
        // אבל לא תוקע את מסך הכניסה לזמן ארוך אם Firestore איטי.
        withTimeoutOrNull(2_500L) {
            preloadJob.join()
        }

        onFinished()
    }

    val currentStage = stages[currentStageIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "kmi_loading")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanOffset"
    )

    val progressAnimated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250),
        label = "progressAnimated"
    )

    val accent = Color(0xFF168BFF)
    val accent2 = Color(0xFF5B35F5)
    val cardBg = Color.White.copy(alpha = 0.94f)
    val textPrimary = Color(0xFF172033)
    val textSecondary = Color(0xFF667085)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.kmi_startup_loading_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        val isCompactHeight = maxHeight < 760.dp
        val isVeryCompactHeight = maxHeight < 690.dp

        val horizontalPadding = if (isCompactHeight) 18.dp else 22.dp

        // מיקומים יחסיים לפי גובה המסך, כדי שהמסך ייראה אחיד בין מכשירים
        val videoTopSpace = maxHeight * 0.060f
        val titleTopSpace = maxHeight * 0.195f
        val cardTopSpace = maxHeight * 0.580f

        val videoWidth = if (isVeryCompactHeight) 220.dp else if (isCompactHeight) 236.dp else 252.dp
        val videoHeight = if (isVeryCompactHeight) 96.dp else if (isCompactHeight) 104.dp else 112.dp
        val glowWidth = if (isVeryCompactHeight) 254.dp else if (isCompactHeight) 276.dp else 292.dp
        val glowHeight = if (isVeryCompactHeight) 104.dp else if (isCompactHeight) 112.dp else 120.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = videoTopSpace)
                    .fillMaxWidth()
                    .height(if (isVeryCompactHeight) 108.dp else if (isCompactHeight) 118.dp else 126.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(glowWidth)
                        .height(glowHeight)
                        .scale(pulseScale)
                        .alpha(glowAlpha)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.28f),
                                    accent.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .width(videoWidth)
                        .height(videoHeight),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F1A26),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val scanX = maxWidth * scanOffset

                        KmiLoopingStartupVideo(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.26f)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(46.dp)
                                .offset(x = scanX)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            accent.copy(alpha = 0.10f),
                                            accent2.copy(alpha = 0.14f),
                                            accent.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Text(
                text = if (isEnglish) "Krav Magen Israeli" else "קרב מגן ישראלי",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (isCompactHeight) 20.sp else 22.sp,
                    lineHeight = if (isCompactHeight) 23.sp else 25.sp
                ),
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = titleTopSpace)
                    .fillMaxWidth()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = cardTopSpace)
                    .fillMaxWidth(0.96f),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            top = if (isCompactHeight) 10.dp else 12.dp,
                            bottom = if (isCompactHeight) 4.dp else 6.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = currentStage.icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Current stage" else "שלב נוכחי",
                                style = MaterialTheme.typography.labelMedium,
                                color = textSecondary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (isEnglish) currentStage.titleEn else currentStage.titleHe,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "${(progressAnimated * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF123C7C),
                            modifier = Modifier.offset(y = (-10).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnimated.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF0FA36B))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LoadingChecklist(
                        stages = stages,
                        activeIndex = currentStageIndex,
                        completedStagesInCycle = completedStagesInCycle,
                        isEnglish = isEnglish,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        TextButton(
                            onClick = onFinished,
                            modifier = Modifier
                                .align(
                                    if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
                                ),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF123C7C)
                            ),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Skip" else "דלג",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isEnglish) {
                                "Please wait..."
                            } else {
                                "אנא המתן..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF123C7C),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 72.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KmiLoopingStartupVideo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoViewRef: VideoView? = null

    val videoUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${il.kmi.app.R.raw.kmi_startup_animation}")
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoView(ctx).apply {
                videoViewRef = this
                setVideoURI(videoUri)

                setOnPreparedListener { mediaPlayer: MediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { videoView ->
            videoViewRef = videoView
            if (!videoView.isPlaying) {
                videoView.start()
            }
        }
    )
}

@Composable
private fun LoadingChecklist(
    stages: List<LoadingStage>,
    activeIndex: Int,
    completedStagesInCycle: Int,
    isEnglish: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        stages.forEachIndexed { index, stage ->
            val done = index < completedStagesInCycle
            val active = index == activeIndex

            val targetAlpha = when {
                done -> 1f
                active -> 1f
                else -> 0.55f
            }

            val rowAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(350),
                label = "stageFade"
            )

            val iconTint = when {
                done -> accent
                active -> Color(0xFFFFD166)
                else -> textSecondary
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(rowAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (done) 1.2f else 1f,
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    ),
                    label = "checkBounce"
                )

                Icon(
                    imageVector = if (done) Icons.Filled.CheckCircle else stage.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scale)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isEnglish) stage.titleEn else stage.titleHe,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp
                    ),
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active || done) textPrimary else textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}