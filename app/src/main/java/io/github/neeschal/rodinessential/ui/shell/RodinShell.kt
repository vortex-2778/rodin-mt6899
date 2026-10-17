package io.github.neeschal.rodinessential.ui.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.RodinIgnorePointer
import io.github.neeschal.rodinessential.ui.motion.RodinBackMotion
import io.github.neeschal.rodinessential.ui.motion.RodinDetailTransition
import io.github.neeschal.rodinessential.ui.motion.RodinMotionDiagnostics
import io.github.neeschal.rodinessential.ui.motion.RodinPredictivePlane
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.screens.AdvancedConfigurationScreen
import io.github.neeschal.rodinessential.ui.screens.ChargingScreen
import io.github.neeschal.rodinessential.ui.screens.CpuControlScreen
import io.github.neeschal.rodinessential.ui.screens.DiagnosticsScreen
import io.github.neeschal.rodinessential.ui.screens.DisplayStudioScreen
import io.github.neeschal.rodinessential.ui.screens.HomeScreen
import io.github.neeschal.rodinessential.ui.screens.HubsScreen
import io.github.neeschal.rodinessential.ui.screens.MaliGpuScreen
import io.github.neeschal.rodinessential.ui.screens.PerAppControlsScreen
import io.github.neeschal.rodinessential.ui.screens.ResolutionScreen
import io.github.neeschal.rodinessential.ui.screens.SettingsScreen
import io.github.neeschal.rodinessential.ui.screens.SubsystemClocksScreen
import io.github.neeschal.rodinessential.ui.screens.SupportScreen
import io.github.neeschal.rodinessential.ui.screens.SystemColorsScreen
import io.github.neeschal.rodinessential.ui.screens.TouchBoostScreen
import io.github.neeschal.rodinessential.ui.screens.ZramSwapScreen
import io.github.neeschal.rodinessential.ui.theme.RodinAppearanceConfig
import io.github.neeschal.rodinessential.ui.theme.RodinAppearanceStore
import io.github.neeschal.rodinessential.ui.theme.RodinBackgroundStyle
import io.github.neeschal.rodinessential.ui.theme.RodinEssentialTheme
import io.github.neeschal.rodinessential.ui.theme.RodinThemeController
import io.github.neeschal.rodinessential.ui.theme.RodinThemePreference
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ROOT_ORDER = listOf(
    RodinScreen.home,
    RodinScreen.hubs,
    RodinScreen.support,
    RodinScreen.settings,
)

/**
 * The app shell: one backdrop, four root destinations on a direct-travel
 * pager, detail planes above them, and the floating dock below.
 */
@Composable
fun RodinShell() {
    val context = LocalContext.current
    var appearance by remember {
        mutableStateOf(RodinAppearanceStore.load(context) ?: RodinAppearanceConfig())
    }

    LaunchedEffect(appearance.themePreference) {
        RodinThemeController.setPreference(appearance.themePreference)
    }

    RodinEssentialTheme(config = appearance) {
        RodinShellScaffold(
            appearance = appearance,
            onPreviewAppearance = { appearance = it },
            onCommitAppearance = {
                appearance = it
                RodinAppearanceStore.save(context, it)
            },
        )
    }
}

@Composable
private fun RodinShellScaffold(
    appearance: RodinAppearanceConfig,
    onPreviewAppearance: (RodinAppearanceConfig) -> Unit,
    onCommitAppearance: (RodinAppearanceConfig) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reducedState = rememberUpdatedState(rodinAnimationsDisabled())
    val pagerState = remember { RodinRootPagerState(pageCount = ROOT_ORDER.size) }

    var screen by remember { mutableStateOf(RodinScreen.home) }
    var detailBackTarget by remember { mutableStateOf(RodinScreen.home) }
    var nestedDetailBackTarget by remember { mutableStateOf<RodinScreen?>(null) }
    var rootSwipeLocked by remember { mutableStateOf(false) }
    var rootNavTarget by remember { mutableStateOf<RodinScreen?>(null) }
    var rootNavRevision by remember { mutableIntStateOf(0) }
    var bypassFocusRevision by remember { mutableIntStateOf(0) }
    var outgoingScreen by remember { mutableStateOf<RodinScreen?>(null) }
    var detailForward by remember { mutableStateOf(true) }

    val backSettle = remember { Animatable(0f) }
    val detailProgress = remember { Animatable(1f) }
    val outgoingProgress = remember { Animatable(1f) }

    fun detailDuration(baseMilliseconds: Int): Int =
        if (reducedState.value) 0 else RodinInteractionSettings.motionDuration(baseMilliseconds)

    fun syncNativeBackInterception() {
        RodinBackDispatch.setIntercept(screen != RodinScreen.home)
    }

    /** One switch: the arriving plane enters, the departing plane reverses. */
    fun beginScreenChange(previous: RodinScreen, next: RodinScreen) {
        if (previous == next) return
        detailForward = !next.isRoot
        if (!previous.isRoot) {
            outgoingScreen = previous
            scope.launch {
                outgoingProgress.stop()
                outgoingProgress.snapTo(detailProgress.value.coerceIn(0f, 1f))
                outgoingProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(detailDuration(220), easing = LinearEasing),
                )
            }
        } else {
            outgoingScreen = null
        }
        screen = next
        if (!next.isRoot) {
            scope.launch {
                detailProgress.stop()
                detailProgress.snapTo(0f)
                detailProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(detailDuration(320), easing = LinearEasing),
                )
            }
        }
    }

    fun selectRoot(target: RodinScreen) {
        if (!target.isRoot) return

        val index = ROOT_ORDER.indexOf(target).coerceAtLeast(0)
        val revision = ++rootNavRevision
        pagerState.prepareJump(index)
        RodinMotionDiagnostics.begin(false, label = "tab-${target.name}")
        RodinMotionDiagnostics.end()

        val previous = screen
        detailForward = !target.isRoot
        if (!previous.isRoot) {
            outgoingScreen = previous
            scope.launch {
                outgoingProgress.stop()
                outgoingProgress.snapTo(detailProgress.value.coerceIn(0f, 1f))
                outgoingProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(detailDuration(220), easing = LinearEasing),
                )
            }
        } else {
            outgoingScreen = null
        }

        screen = target
        detailBackTarget = target
        nestedDetailBackTarget = null
        rootNavTarget = target

        val duration = detailDuration(260)
        if (duration <= 0) {
            pagerState.jumpToPage(index)
            pagerState.finishJump()
            rootNavTarget = null
        } else {
            pagerState.animateToPage(index, duration, scope) {
                if (rootNavRevision == revision) {
                    pagerState.finishJump()
                    screen = target
                    rootNavTarget = null
                }
            }
        }

        syncNativeBackInterception()
    }

    fun interruptRootTap() {
        // Dock/page dragging takes ownership; a completed old A to B to A future
        // must never clear a newer transition merely because its target matches.
        rootNavRevision += 1
        if (rootNavTarget == null) return
        val visible = pagerState.visibleIndex()
        rootNavTarget = null
        pagerState.finishJump()
        pagerState.jumpToPage(visible)
        if (screen.isRoot) {
            screen = ROOT_ORDER[visible]
            detailBackTarget = screen
        }
        syncNativeBackInterception()
    }

    fun onRootPageChanged(index: Int) {
        if (index < 0 || index >= ROOT_ORDER.size) return

        val next = ROOT_ORDER[index]
        val target = rootNavTarget
        if (target != null && next != target) return
        if (screen == next) return

        screen = next
        detailBackTarget = next
        nestedDetailBackTarget = null
        syncNativeBackInterception()
    }

    fun openDetail(target: RodinScreen) {
        if (target.isRoot) {
            selectRoot(target)
            return
        }

        RodinBackMotion.committedProgress = 0f
        RodinMotionDiagnostics.begin(false, label = "open-${target.name}")
        RodinMotionDiagnostics.end()

        if (screen.isRoot) detailBackTarget = screen
        nestedDetailBackTarget = null
        beginScreenChange(screen, target)
        syncNativeBackInterception()
    }

    fun back() {
        if (RodinNestedBackController.handleBack()) return

        val nestedTarget = nestedDetailBackTarget
        if (nestedTarget != null && !screen.isRoot) {
            beginScreenChange(screen, nestedTarget)
            nestedDetailBackTarget = null
            syncNativeBackInterception()
            return
        }

        if (screen.isRoot) {
            if (screen != RodinScreen.home) selectRoot(RodinScreen.home)
            return
        }

        val target = detailBackTarget
        pagerState.jumpToPage(ROOT_ORDER.indexOf(target).coerceAtLeast(0))
        beginScreenChange(screen, target)
        nestedDetailBackTarget = null
        syncNativeBackInterception()
    }

    fun handleBackGesture(packet: Int) {
        val phase = packet shr 16
        when (phase) {
            1, 2 -> {
                if (phase == 1) RodinMotionDiagnostics.begin((packet and 32768) != 0)
                scope.launch {
                    backSettle.stop()
                    RodinBackMotion.committedProgress = 0f
                    RodinBackMotion.nested = RodinNestedBackController.canPreview
                    RodinBackMotion.rightEdge = (packet and 32768) != 0
                    backSettle.snapTo((packet and 32767) / 10000f)
                }
            }
            3 -> {
                RodinMotionDiagnostics.end()
                scope.launch {
                    backSettle.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.94f, stiffness = 500f),
                    )
                }
            }
            4 -> {
                if (screen != RodinScreen.home) {
                    RodinMotionDiagnostics.end()
                    RodinBackMotion.rightEdge = (packet and 32768) != 0
                    RodinBackMotion.committedProgress = backSettle.value.coerceIn(0f, 1f)
                    back()
                    scope.launch { backSettle.snapTo(0f) }
                }
            }
        }
    }

    // The release animation must keep publishing gesture progress.
    LaunchedEffect(backSettle) {
        snapshotFlow { backSettle.value }.collect {
            RodinBackMotion.progress = it.coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.page.roundToInt() }.collect { onRootPageChanged(it) }
    }

    DisposableEffect(Unit) {
        RodinBackDispatch.packetListener = { packet -> handleBackGesture(packet) }
        RodinBackDispatch.requestListener = {
            if (screen != RodinScreen.home) {
                RodinBackMotion.committedProgress = 0f
                back()
            }
        }
        RodinBackDispatch.bypassSettingsListener = {
            scope.launch {
                backSettle.stop()
                backSettle.snapTo(0f)
            }
            openDetail(RodinScreen.charging)
            bypassFocusRevision += 1
        }
        onDispose {
            RodinBackDispatch.packetListener = null
            RodinBackDispatch.requestListener = null
            RodinBackDispatch.bypassSettingsListener = null
            RodinBackDispatch.setIntercept(false)
            RodinBackMotion.progress = 0f
        }
    }

    val current = screen
    val navRoot = if (current.isRoot) current else detailBackTarget
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f

    Box(Modifier.fillMaxSize()) {
        RodinBackgroundLayer(appearance, Modifier.fillMaxSize())

        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                ),
        ) {
            RodinIgnorePointer(ignoring = !current.isRoot) {
                RodinRootPager(
                    state = pagerState,
                    swipeEnabled = !rootSwipeLocked,
                    onDragStart = { interruptRootTap() },
                    modifier = Modifier.fillMaxSize(),
                ) { index ->
                    RootPage(
                        screen = ROOT_ORDER[index],
                        appearance = appearance,
                        onOpen = { target -> openDetail(target) },
                        onSelectRoot = { target -> selectRoot(target) },
                        onThemePreferenceChanged = { preference ->
                            RodinThemeController.setPreference(preference)
                            onCommitAppearance(
                                appearance.copy(themePreference = preference),
                            )
                        },
                        onBackgroundStyleChanged = { style ->
                            onCommitAppearance(appearance.copy(backgroundStyle = style))
                        },
                        onCustomBackgroundSelected = { path ->
                            onCommitAppearance(
                                appearance.copy(
                                    backgroundStyle = RodinBackgroundStyle.custom,
                                    customPath = path,
                                ),
                            )
                        },
                        onBlurChanged = { value ->
                            onPreviewAppearance(
                                appearance.copy(
                                    backgroundBlur = value.coerceIn(0.0, 28.0),
                                ),
                            )
                        },
                        onBlurChangeEnd = { value ->
                            val next = appearance.copy(
                                backgroundBlur = value.coerceIn(0.0, 28.0),
                            )
                            onPreviewAppearance(next)
                            onCommitAppearance(next)
                        },
                        onHorizontalControlActive = { active -> rootSwipeLocked = active },
                        onResetAppearance = {
                            val defaults = RodinAppearanceConfig()
                            RodinThemeController.setPreference(defaults.themePreference)
                            onCommitAppearance(defaults)
                        },
                        onAccentChanged = { index ->
                            onCommitAppearance(appearance.copy(accentIndex = index))
                        },
                        onCardRadiusChanged = { radius ->
                            onCommitAppearance(appearance.copy(cardRadius = radius))
                        },
                        onCardStyleChanged = { style ->
                            onCommitAppearance(appearance.copy(cardStyle = style))
                        },
                        onHeroGlowChanged = { glow ->
                            onCommitAppearance(appearance.copy(heroGlow = glow))
                        },
                        onCoreVisualizerStyleChanged = { style ->
                            onCommitAppearance(appearance.copy(coreVisualizerStyle = style))
                        },
                    )
                }
            }

            val outgoing = outgoingScreen
            if (outgoing != null && !outgoing.isRoot && outgoing != current) {
                RodinDetailTransition(
                    progress = outgoingProgress.value,
                    reverse = true,
                    active = false,
                    forward = detailForward,
                    previewThrough = true,
                ) {
                    // Keep the departing detail above the destination during
                    // Back, so its solid surface reveals it continuously.
                    RodinPredictivePlane {
                        DetailPlane(
                            screen = outgoing,
                            appearance = appearance,
                            isDark = isDark,
                            focusRequest = bypassFocusRevision,
                        ) { back() }
                    }
                }
            }

            if (!current.isRoot) {
                RodinDetailTransition(
                    progress = detailProgress.value,
                    reverse = false,
                    active = true,
                    forward = detailForward,
                    previewThrough = true,
                ) {
                    RodinPredictivePlane {
                        DetailPlane(
                            screen = current,
                            appearance = appearance,
                            isDark = isDark,
                            focusRequest = bypassFocusRevision,
                        ) { back() }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to colors.surface.copy(alpha = 0f),
                                0.55f to colors.surface.copy(
                                    alpha = if (isDark) 0.74f else 0.86f,
                                ),
                                1f to colors.surface,
                            ),
                        ),
                    ),
            )
            RodinBottomBar(
                pageProvider = { pagerState.page },
                currentRoot = navRoot,
                onSelect = { target -> selectRoot(target) },
                onDragStart = { interruptRootTap() },
                onDockDrag = { dx, width -> pagerState.dragByDockPosition(dx, width) },
                modifier = Modifier.padding(top = RodinBottomLayout.DOCK_FADE.dp),
            )
        }
    }
}

@Composable
private fun RootPage(
    screen: RodinScreen,
    appearance: RodinAppearanceConfig,
    onOpen: (RodinScreen) -> Unit,
    onSelectRoot: (RodinScreen) -> Unit,
    onThemePreferenceChanged: (RodinThemePreference) -> Unit,
    onBackgroundStyleChanged: (RodinBackgroundStyle) -> Unit,
    onCustomBackgroundSelected: (String) -> Unit,
    onBlurChanged: (Double) -> Unit,
    onBlurChangeEnd: (Double) -> Unit,
    onHorizontalControlActive: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onAccentChanged: (Int) -> Unit,
    onCardRadiusChanged: (Double) -> Unit,
    onCardStyleChanged: (Int) -> Unit,
    onHeroGlowChanged: (Boolean) -> Unit,
    onCoreVisualizerStyleChanged: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        RodinBackgroundLayer(appearance, Modifier.fillMaxSize())
        when (screen) {
            RodinScreen.home -> HomeScreen(
                onOpen = onOpen,
                onHubs = { onSelectRoot(RodinScreen.hubs) },
                onSupport = { onSelectRoot(RodinScreen.support) },
            )
            RodinScreen.hubs -> HubsScreen(onOpen = onOpen)
            RodinScreen.support -> SupportScreen()
            RodinScreen.settings -> SettingsScreen(
                config = appearance,
                onThemePreferenceChanged = onThemePreferenceChanged,
                onBackgroundStyleChanged = onBackgroundStyleChanged,
                onCustomBackgroundSelected = onCustomBackgroundSelected,
                onBlurChanged = onBlurChanged,
                onBlurChangeEnd = onBlurChangeEnd,
                onHorizontalControlActive = onHorizontalControlActive,
                onResetAppearance = onResetAppearance,
                onAccentChanged = onAccentChanged,
                onCardRadiusChanged = onCardRadiusChanged,
                onCardStyleChanged = onCardStyleChanged,
                onHeroGlowChanged = onHeroGlowChanged,
                onCoreVisualizerStyleChanged = onCoreVisualizerStyleChanged,
            )
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun DetailPlane(
    screen: RodinScreen,
    appearance: RodinAppearanceConfig,
    isDark: Boolean,
    focusRequest: Int,
    onBack: () -> Unit,
) {
    val background = if (isDark) Color(0xFF090C11) else Color(0xFFF3F6FA)
    Box(
        Modifier
            .fillMaxSize()
            .rodinPlaneShadow(isDark)
            .background(background),
    ) {
        RodinBackgroundLayer(appearance, Modifier.fillMaxSize())
        when (screen) {
            RodinScreen.charging -> ChargingScreen(onBack = onBack, focusRequest = focusRequest)
            RodinScreen.touchBoost -> TouchBoostScreen(onBack = onBack)
            RodinScreen.displayStudio -> DisplayStudioScreen(onBack = onBack)
            RodinScreen.cpuControl -> CpuControlScreen(onBack = onBack)
            RodinScreen.advancedConfiguration -> AdvancedConfigurationScreen(onBack = onBack)
            RodinScreen.resolution -> ResolutionScreen(onBack = onBack)
            RodinScreen.diagnostics -> DiagnosticsScreen(onBack = onBack)
            RodinScreen.zramSwap -> ZramSwapScreen(onBack = onBack)
            RodinScreen.maliGpu -> MaliGpuScreen(onBack = onBack)
            RodinScreen.systemColors -> SystemColorsScreen(onBack = onBack)
            RodinScreen.perAppControls -> PerAppControlsScreen(onBack = onBack)
            RodinScreen.subsystemClocks -> SubsystemClocksScreen(onBack = onBack)
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

/** The detail plane's edge shadow, kept inside the transition's clip. */
private fun Modifier.rodinPlaneShadow(isDark: Boolean): Modifier = this.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.TRANSPARENT
        setShadowLayer(
            18f,
            -4f,
            0f,
            Color.Black.copy(alpha = if (isDark) 0.50f else 0.12f).toArgb(),
        )
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
