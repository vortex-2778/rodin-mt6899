package io.github.neeschal.rodinessential.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.neeschal.rodinessential.RodinReleaseIdentity
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.shell.RodinNestedBackController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val eggEaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
private val secretEaseOutBack = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

// NEESCHAL's tiny break room. The daemon stays on duty; the jokes do not.
//     [ CPU ]  [ GPU ]
//         \\    /
//       ( snack )       No frequency tables were harmed making this sandwich.
private val SECRET_JOKES = listOf(
    "Four taps. Zero secret overclock.",
    "I asked the GPU for a sandwich. It rendered the bread.",
    "My governor says I need a nap. Finally, sensible scheduling.",
    "Touch grass. OEM adaptive mode.",
    "Zero DEX. Full dad jokes.",
    "More cores, fewer chores. The laundry still won\u2019t compile.",
    "NEESCHAL hid this here. The compiler pretended not to notice.",
    "The daemon works in the background. I tell jokes in the foreground.",
    "Your phone has eight cores. Somehow, you\u2019re still doing all the work.",
    "Battery at 1%: suddenly every charger becomes your best friend.",
    "I cleared my cache. Still thinking about that embarrassing thing from 2017.",
    "My screen time report just asked if I live here.",
    "I opened one notification. Three hours later, I know how otters hold hands.",
    "Airplane mode is on. Still waiting for the phone to take off.",
    "My phone has a dark mode. My sleep schedule has only chaos mode.",
    "I told my phone to chill. It opened the weather app.",
    "128 tabs open. All of them are apparently essential.",
    "My charger and I have a connection. Sometimes it\u2019s complicated.",
    "You found the Easter egg. The chicken is still compiling.",
    "NEESCHAL promised fewer bugs. Nobody said anything about these jokes.",
)

/**
 * Four taps on the version pill open the secret room. A real pressable in a
 * real settings row: no hidden gestures anywhere else.
 */
@Composable
fun RodinVersionSecret(version: String, modifier: Modifier = Modifier) {
    val reduced = rodinAnimationsDisabled()
    var taps by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        delay(2_000)
        taps = 0
    }

    val scale by animateFloatAsState(
        targetValue = 1f + taps * 0.025f,
        animationSpec = tween(
            durationMillis = if (reduced) 0 else RodinInteractionSettings.motionDuration(160),
            easing = eggEaseOutCubic,
        ),
        label = "version-tap-scale",
    )

    Box(
        modifier.semantics {
            role = Role.Button
            contentDescription = "Rodin Essential version $version"
        },
    ) {
        PressScale(
            enableHaptics = false,
            onTap = {
                if (!open) {
                    RodinHaptics.confirm()
                    taps += 1
                    if (taps == 4) {
                        taps = 0
                        open = true
                    }
                }
            },
        ) {
            Box(
                Modifier
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                StatusPill(label = version, accent = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (open) {
        RodinSecretRoomHost(onDismiss = { open = false })
    }
}

@Composable
private fun RodinSecretRoomHost(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val reduced = rodinAnimationsDisabled()
        val density = LocalDensity.current
        val progress = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!reduced) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        RodinInteractionSettings.motionDuration(340),
                        easing = LinearEasing,
                    ),
                )
            }
        }
        val p = progress.value.coerceIn(0f, 1f)
        val scale = 0.94f + 0.06f * secretEaseOutBack.transform(p)
        val maxHeight = (LocalConfiguration.current.screenHeightDp.dp - 100.dp)
            .coerceAtLeast(0.dp)

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * p))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .safeDrawingPadding()
                    .padding(22.dp)
                    .widthIn(max = 360.dp)
                    .heightIn(max = maxHeight)
                    .graphicsLayer {
                        alpha = p
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                SurfaceCard(padding = PaddingValues(20.dp)) {
                    RodinSecretRoom(onClose = onDismiss)
                }
            }
        }
    }
}

@Composable
fun RodinSecretRoom(onClose: () -> Unit) {
    val reduced = rodinAnimationsDisabled()
    val animate = !reduced
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val currentOnClose by rememberUpdatedState(onClose)

    var holding by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var joke by remember { mutableIntStateOf(0) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val owner = remember { Any() }

    val burst = remember { Animatable(if (animate) 0f else 1f) }
    val hold = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (animate) {
            burst.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    RodinInteractionSettings.motionDuration(1100),
                    easing = LinearEasing,
                ),
            )
        }
    }

    fun next() {
        RodinHaptics.confirm()
        joke = (joke + 1) % SECRET_JOKES.size
        if (animate) {
            scope.launch {
                burst.snapTo(0f)
                burst.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        RodinInteractionSettings.motionDuration(1100),
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    fun beginHold() {
        holding = true
        ready = false
        holdJob?.cancel()
        holdJob = scope.launch {
            hold.snapTo(0f)
            hold.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    RodinInteractionSettings.motionDuration(900),
                    easing = LinearEasing,
                ),
            )
            if (holding) {
                RodinHaptics.confirm()
                ready = true
            }
        }
    }

    fun endHold(cancelled: Boolean = false) {
        val launch = ready && !cancelled
        holdJob?.cancel()
        holdJob = null
        scope.launch { hold.snapTo(0f) }
        holding = false
        ready = false
        playing = launch
        if (launch) RodinHaptics.confirm()
    }

    DisposableEffect(owner) {
        // Back leaves the game first, then the room: a nested owner, not a route.
        RodinNestedBackController.attach(
            owner = owner,
            handler = {
                if (playing) {
                    playing = false
                    true
                } else {
                    currentOnClose()
                    true
                }
            },
        )
        onDispose { RodinNestedBackController.detach(owner) }
    }

    if (playing) {
        AuroraDrift(onBack = { playing = false })
        return
    }

    val colorsForJoke: ColorScheme = colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "The secret room",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.W700,
                    color = colors.onSurface,
                ),
            )
            IconButton(
                onClick = {
                    RodinHaptics.confirm()
                    currentOnClose()
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close secret room",
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${RodinReleaseIdentity.releaseCodename} \u00b7 a little less serious.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            RodinSparkles(
                progress = burst.value,
                primary = colors.primary,
                secondary = colors.secondary,
                modifier = Modifier.fillMaxSize(),
            )
            val pulse = sin(burst.value * PI.toFloat())
            val rotation = when {
                !animate -> 0f
                holding -> hold.value * hold.value * PI.toFloat() * 8f
                else -> sin(burst.value * PI.toFloat() * 6f) * (1f - burst.value) * 0.10f
            }
            val logoScale = 1f + pulse * 0.10f
            Box(
                Modifier
                    .graphicsLayer {
                        translationY = -pulse * 7f * density
                        rotationZ = rotation * 180f / PI.toFloat()
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val released = tryAwaitRelease()
                                if (holding) endHold(cancelled = !released)
                            },
                            onLongPress = { beginHold() },
                            onTap = { next() },
                        )
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Rodin Essential logo"
                    },
            ) {
                RodinAppEmblem(size = 72.dp)
            }
        }
        Text(
            text = when {
                ready -> "Release to drift."
                holding -> "Winding up\u2026"
                else -> "Tap for a laugh. Hold for an adventure."
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.5.sp,
                color = colors.primary,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(12.dp))
        if (!animate) {
            JokeText(joke, colorsForJoke)
        } else {
            Box(
                Modifier.animateContentSize(
                    animationSpec = tween(
                        RodinInteractionSettings.motionDuration(280),
                        easing = eggEaseOutCubic,
                    ),
                ),
            ) {
                AnimatedContent(
                    targetState = joke,
                    transitionSpec = {
                        val spec = tween<Float>(
                            RodinInteractionSettings.motionDuration(260),
                            easing = eggEaseOutCubic,
                        )
                        val slideSpec = tween<IntOffset>(
                            RodinInteractionSettings.motionDuration(260),
                            easing = eggEaseOutCubic,
                        )
                        (
                            fadeIn(spec) + slideInVertically(slideSpec) {
                                (it * 0.07f).roundToInt()
                            }
                            ).togetherWith(
                            fadeOut(spec) + slideOutVertically(slideSpec) {
                                (it * 0.07f).roundToInt()
                            },
                        )
                    },
                    label = "secret-joke",
                ) { index ->
                    JokeText(index, colorsForJoke)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        FilledTonalButton(onClick = { next() }) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("One more joke")
        }
        TextButton(
            onClick = {
                RodinHaptics.confirm()
                playing = true
            },
        ) {
            Text("Play Aurora Drift")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Made with a straight face by NEESCHAL \ud83c\uddf3\ud83c\uddf5",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Just for fun. No device settings changed.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun JokeText(index: Int, colors: ColorScheme) {
    Text(
        text = SECRET_JOKES[index],
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            lineHeight = 26.1.sp,
            color = colors.onSurface,
        ),
    )
}

/** A one-shot burst, never a looping animation. */
@Composable
private fun RodinSparkles(
    progress: Float,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        val radius = size.minDimension * (0.23f + 0.33f * eggEaseOutCubic.transform(progress))
        val middle = center
        drawCircle(
            color = primary.copy(alpha = 0.2f * (1f - progress)),
            radius = radius * 0.9f,
            center = middle,
            style = Stroke(width = 1.2f),
        )
        for (i in 0 until 20) {
            val angle = i * PI.toFloat() * 2f / 20f +
                progress * (if (i % 2 == 0) 0.15f else -0.15f)
            val point = middle +
                Offset(cos(angle) * radius, sin(angle) * radius) +
                Offset(0f, progress * progress * 18f)
            val color = (if (i % 2 == 0) primary else secondary).copy(alpha = 1f - progress)
            rotate(degrees = (angle + progress * 2f) * 180f / PI.toFloat(), pivot = point) {
                if (i % 3 == 0) {
                    drawRoundRect(
                        color = color,
                        topLeft = point + Offset(-1.5f, -3f),
                        size = Size(3f, 6f),
                        cornerRadius = CornerRadius(1f, 1f),
                    )
                } else {
                    drawCircle(
                        color = color,
                        radius = 2.5f * (1f - progress * 0.5f),
                        center = point,
                    )
                }
            }
        }
    }
}
