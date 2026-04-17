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
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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

    val accent = Color(0xFF16C47F)
    val accent2 = Color(0xFF31D6A0)
    val bgTop = Color(0xFF071019)
    val bgBottom = Color(0xFF0E1A26)
    val cardBg = Color(0xAA132231)
    val textPrimary = Color(0xFFF3F7FA)
    val textSecondary = Color(0xFFB8C7D3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom, Color(0xFF101F2E))
                )
            )
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
            Spacer(modifier = Modifier.height(18.dp))

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(392.dp)
                        .height(248.dp)
                        .scale(pulseScale)
                        .alpha(glowAlpha)
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.30f),
                                    accent.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .height(185.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFF0F1A26),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val scanX = maxWidth * scanOffset

                        KmiLoopingStartupVideo(
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(56.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isEnglish) "Krav Magen Israeli" else "קרב מגן ישראלי",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isEnglish) {
                    "Initializing premium training environment"
                } else {
                    "מאתחל סביבת אימון מתקדמת"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }

                        Text(
                            text = "${(progressAnimated * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent2
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    LinearProgressIndicator(
                        progress = { progressAnimated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.10f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LoadingChecklist(
                        stages = stages,
                        activeIndex = currentStageIndex,
                        completedStagesInCycle = completedStagesInCycle,
                        isEnglish = isEnglish,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isEnglish) {
                        "Please wait a few seconds..."
                    } else {
                        "אנא המתן מספר שניות..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }

        TextButton(
            onClick = onFinished,
            modifier = Modifier
                .align(androidx.compose.ui.AbsoluteAlignment.BottomLeft)
                .padding(start = 20.dp, bottom = 56.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = accent
            )
        ) {
            Text(
                text = if (isEnglish) "Skip" else "דלג",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        .size(18.dp)
                        .scale(scale)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isEnglish) stage.titleEn else stage.titleHe,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active || done) textPrimary else textSecondary
                )
            }
        }
    }
}