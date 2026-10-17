package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.neeschal.rodinessential.RodinReleaseIdentity
import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class AuroraObject(
    var x: Float,
    var y: Float,
    val speed: Float,
    val meteor: Boolean,
)

/**
 * NEESCHAL's strictly recreational scheduler: stars get priority, rocks don't.
 * Normalized world coordinates make gameplay independent of DPI and FPS.
 */
class AuroraGame(seed: Int? = null) {
    private val random: Random = if (seed != null) Random(seed) else Random.Default
    val objects = mutableListOf<AuroraObject>()
    var playerX = 0.5f
        private set
    var targetX = 0.5f
        private set
    var elapsed = 0f
        private set
    var invulnerable = 0f
        private set
    private var spawn = 0f
    var score = 0
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    var lives = 3
        private set
    var running = false
        private set
    var finished = false
        private set

    val secondsLeft: Int
        get() = max(0, ceil(roundSeconds - elapsed).toInt())

    fun start() {
        objects.clear()
        playerX = 0.5f
        targetX = 0.5f
        elapsed = 0f
        invulnerable = 0f
        spawn = 0f
        score = 0
        combo = 0
        bestCombo = 0
        lives = 3
        finished = false
        running = true
    }

    fun steer(x: Float) {
        targetX = x.coerceIn(0.07f, 0.93f)
    }

    fun pause() {
        running = false
    }

    fun resume() {
        if (!finished) running = true
    }

    fun advance(dtRaw: Float) {
        if (!running || finished || !dtRaw.isFinite() || dtRaw <= 0f) return
        val dt = dtRaw.coerceIn(0f, 0.05f)
        elapsed += dt
        invulnerable = max(0f, invulnerable - dt)
        playerX += (targetX - playerX) * (1f - exp(-18.0 * dt).toFloat())
        spawn -= dt
        if (spawn <= 0f) {
            val difficulty = (elapsed / roundSeconds).coerceIn(0f, 1f)
            objects.add(
                AuroraObject(
                    0.08f + random.nextFloat() * 0.84f,
                    -0.07f,
                    0.25f + difficulty * 0.22f + random.nextFloat() * 0.06f,
                    meteor = random.nextFloat() < 0.28f + difficulty * 0.12f,
                ),
            )
            spawn = 0.42f - difficulty * 0.15f
        }
        for (i in objects.indices.reversed()) {
            val obj = objects[i]
            obj.y += obj.speed * dt
            if (abs(obj.x - playerX) < 0.075f && abs(obj.y - 0.82f) < 0.045f) {
                if (obj.meteor) {
                    if (invulnerable <= 0f) {
                        lives--
                        combo = 0
                        invulnerable = 1.1f
                    }
                } else {
                    combo++
                    bestCombo = max(bestCombo, combo)
                    score += 10 * min(combo, 8)
                }
                objects.removeAt(i)
            } else if (obj.y > 1.08f) {
                if (!obj.meteor) combo = 0
                objects.removeAt(i)
            }
        }
        if (lives <= 0 || elapsed >= roundSeconds) {
            finished = true
            running = false
        }
    }

    companion object {
        const val roundSeconds = 30f
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuroraDrift(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduced = rodinAnimationsDisabled()
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val game = remember { AuroraGame() }
    val scoreFile = remember { File(context.filesDir, "aurora-drift-best.json") }

    var best by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf("") }
    var recordSaved by remember { mutableStateOf(false) }
    var frameTick by remember { mutableIntStateOf(0) }
    var worldSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val saved = JSONObject(scoreFile.readText())
                    .getInt("best")
                    .coerceIn(0, 1_000_000)
                best = max(best, saved)
            } catch (_: Exception) {
                // Missing or invalid recreational data never affects controls.
            }
        }
    }

    fun saveBest() {
        if (game.score <= best) return
        best = game.score
        val value = best
        scope.launch(Dispatchers.IO) {
            try {
                val pending = File(scoreFile.path + ".tmp")
                val bytes = JSONObject().put("best", value).toString().toByteArray()
                FileOutputStream(pending).use { stream ->
                    stream.write(bytes)
                    stream.flush()
                    stream.fd.sync()
                }
                pending.renameTo(scoreFile)
            } catch (_: Exception) {
                // Gameplay still works if storage is unavailable.
            }
        }
    }

    fun startGame() {
        RodinHaptics.confirm()
        recordSaved = false
        game.start()
        started = true
        paused = false
    }

    fun pauseGame() {
        if (!game.running) return
        game.pause()
        paused = true
    }

    fun resumeGame() {
        RodinHaptics.confirm()
        game.resume()
        paused = false
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (started && !paused && !game.finished) {
                    if (last != 0L) {
                        val lives = game.lives
                        val score = game.score
                        game.advance((now - last) / 1_000_000_000f)
                        if (game.lives < lives) {
                            RodinPlatform.haptic(2)
                        } else if (game.score > score && game.combo % 3 == 0) {
                            RodinHaptics.tap()
                        }
                    }
                    last = now
                    val next = "${game.score}:${game.lives}:${game.secondsLeft}:" +
                        "${game.combo}:${game.finished}"
                    if (next != hud) hud = next
                    frameTick++
                    if (game.finished && !recordSaved) {
                        recordSaved = true
                        saveBest()
                    }
                } else {
                    last = 0L
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) pauseGame()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val heightDp = with(density) { windowInfo.containerSize.height.toDp() }
    // Reading the tick keeps score, lives and combo live without a second timer.
    val tick = frameTick
    val showOverlay = tick >= 0 && (!started || paused || game.finished)

    Column(
        Modifier
            .fillMaxWidth()
            .height((heightDp * 0.7f).coerceIn(300.dp, 580.dp)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back to secret room",
                    tint = colors.onSurfaceVariant,
                )
            }
            Text(
                text = "Aurora Drift",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.W700,
                    color = colors.onSurface,
                ),
            )
            if (game.running) {
                IconButton(onClick = { pauseGame() }) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Pause game",
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(
                14.dp,
                Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${game.score} pts",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.W700,
                ),
            )
            Text("${game.secondsLeft}s", style = MaterialTheme.typography.bodyMedium)
            Text("${game.lives} lives", style = MaterialTheme.typography.bodyMedium)
            Text("Best $best", style = MaterialTheme.typography.bodyMedium)
            if (game.combo > 1) {
                Text(
                    text = "\u00d7${min(game.combo, 8)} combo",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.secondary),
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .onSizeChanged { worldSize = it },
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            game.steer(down.position.x / size.width.toFloat())
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                game.steer(change.position.x / size.width.toFloat())
                                change.consume()
                            }
                        }
                    },
            ) {
                if (frameTick < 0) return@Canvas
                drawAuroraWorld(game, reduced)
            }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        if (frameTick < 0) {
                            IntOffset.Zero
                        } else {
                            IntOffset(
                                (game.playerX * worldSize.width).roundToInt(),
                                (worldSize.height * 0.82f).roundToInt(),
                            )
                        }
                    },
            ) {
                Text(
                    text = "R",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.W800,
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationX = -size.width / 2f
                        translationY = -size.height / 2f
                    },
                )
            }
            if (showOverlay) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xDD080F20)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = when {
                                game.finished && game.lives == 0 ->
                                    "That meteor wasn\u2019t in the changelog."
                                game.finished -> "A stellar little detour."
                                paused -> "Taking a breather."
                                else -> "Catch stars. Dodge rocks."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W700,
                                lineHeight = 28.6.sp,
                            ),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = when {
                                game.finished ->
                                    "${game.score} points \u00b7 best combo ${game.bestCombo}"
                                paused -> "Resume when you\u2019re ready."
                                else ->
                                    "Drag left and right to steer R.\n" +
                                        "Build combos. You have three lives and 30 seconds."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFB8C6DF),
                                fontSize = 13.sp,
                                lineHeight = 19.5.sp,
                            ),
                        )
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = {
                                if (paused) resumeGame() else startGame()
                            },
                        ) {
                            Text(
                                text = when {
                                    paused -> "Resume"
                                    game.finished -> "Drift again"
                                    else -> "Let\u2019s drift"
                                },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Aurora \u00b7 v${RodinReleaseIdentity.versionName} \u00b7 just for fun",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = colors.onSurfaceVariant,
            ),
        )
    }
}

private fun DrawScope.drawAuroraWorld(game: AuroraGame, reduced: Boolean) {
    drawRect(Color(0xFF080F20))
    val time = if (reduced) 0f else game.elapsed
    val bandColors = listOf(
        Color(0x194EF2CB),
        Color(0x197E8CFF),
        Color(0x199165FF),
    )
    for (band in 0 until 3) {
        val path = Path()
        path.moveTo(-20f, size.height * 0.25f)
        var x = -20f
        while (x <= size.width + 20f) {
            val y = size.height * (0.25f + band * 0.14f) +
                sin(x / 90f + time * 0.35f + band) * 30f
            path.lineTo(x, y)
            x += 12f
        }
        drawPath(path, bandColors[band], style = Stroke(width = 26f))
    }
    val starPaint = Color(0x665D739D)
    for (i in 0 until 30) {
        val starX = ((i * 71) % 293) / 293f * size.width
        val starY = (((i * 47) % 257) / 257f * size.height + time * 5f) % size.height
        drawCircle(
            color = starPaint,
            radius = if (i % 2 == 0) 1f else 1.6f,
            center = Offset(starX, starY),
        )
    }
    for (obj in game.objects) {
        val center = Offset(obj.x * size.width, obj.y * size.height)
        if (obj.meteor) {
            drawCircle(Color(0xFFFC8F83), 10f, center)
            drawCircle(Color(0xFFBC5E62), 3f, center + Offset(-3f, -2f))
            drawCircle(Color(0xFFBC5E62), 2f, center + Offset(4f, 3f))
        } else {
            val star = Path()
            for (i in 0 until 8) {
                val angle = i * (PI_F / 4f)
                val radius = if (i % 2 == 0) 10f else 4f
                val point = center + Offset(cos(angle) * radius, sin(angle) * radius)
                if (i == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
            }
            star.close()
            drawPath(star, Color(0xFFF9D98B))
        }
    }
    val player = Offset(game.playerX * size.width, size.height * 0.82f)
    if (game.invulnerable > 0f) {
        drawCircle(Color(0xCC83DDFF), 23f, player, style = Stroke(width = 2f))
    }
    val bodyTopLeft = player - Offset(17f, 17f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF16C8D7), Color(0xFF374FE0)),
            start = Offset(bodyTopLeft.x, player.y),
            end = Offset(bodyTopLeft.x + 34f, player.y),
        ),
        topLeft = bodyTopLeft,
        size = Size(34f, 34f),
        cornerRadius = CornerRadius(10f, 10f),
    )
}

private const val PI_F = 3.1415927f
