package io.github.neeschal.rodinessential.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.neeschal.rodinessential.backend.PerAppBackend
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.motion.RodinDetailTransition
import io.github.neeschal.rodinessential.ui.motion.RodinDialog
import io.github.neeschal.rodinessential.ui.motion.RodinPredictivePlane
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.shell.RodinNestedBackController
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

// Presentation-only cache; live ownership and saved profiles stay daemon-owned.
private object PerAppPresentationCache {
    var apps: List<Map<String, Any?>> = emptyList()
    val icons: MutableMap<String, Bitmap> = mutableMapOf()
    val iconAttempts: MutableSet<String> = mutableSetOf()
}

private fun perAppValue(value: Any?): Any? = when {
    value == null -> null
    value === JSONObject.NULL -> null
    value is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to perAppValue(item) }
    value is List<*> -> value.map { perAppValue(it) }
    else -> value
}

private fun perAppMap(value: Any?): Map<String, Any?>? = perAppValue(value) as? Map<String, Any?>

private fun deepCopy(value: Any?): Any? = when (value) {
    is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to deepCopy(item) }
    is List<*> -> value.map { deepCopy(it) }
    else -> value
}

private fun jsonEncode(value: Any?): String = when (value) {
    null -> "null"
    is String -> JSONObject.quote(value)
    is Boolean -> value.toString()
    is Number -> value.toString()
    is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (key, item) ->
        JSONObject.quote(key.toString()) + ":" + jsonEncode(item)
    }
    is List<*> -> value.joinToString(",", "[", "]") { jsonEncode(it) }
    else -> JSONObject.quote(value.toString())
}

private fun filteredApps(
    apps: List<Map<String, Any?>>,
    query: String,
    showSystem: Boolean,
): List<Map<String, Any?>> {
    val needle = query.trim().lowercase()
    return apps.filter { app ->
        (showSystem || app["system"] != true) &&
            "${app["label"] ?: ""} ${app["package"] ?: ""}".lowercase().contains(needle)
    }
}

private fun numbersOf(capabilities: Map<String, Any?>, key: String): List<Int> =
    ((capabilities[key] as? List<*>) ?: emptyList<Any?>())
        .mapNotNull { (it as? Number)?.toInt() }

/**
 * Per-app touch, refresh rate, GPU and CPU profiles. The editor is a nested
 * plane which owns Back, exactly like the Dart page.
 */
@Composable
fun PerAppControlsScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val reduced = rodinAnimationsDisabled()
    val owner = remember { Any() }

    var stateMap by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var capabilities by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var apps by remember { mutableStateOf(PerAppPresentationCache.apps) }
    var icons by remember { mutableStateOf(PerAppPresentationCache.icons.toMap()) }
    val iconAttempts = remember { PerAppPresentationCache.iconAttempts }
    var selected by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var closingApp by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var polling by remember { mutableStateOf(false) }
    var limit by remember { mutableIntStateOf(40) }
    var showSystem by remember { mutableStateOf(false) }
    var forward by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var editorVisible by remember { mutableStateOf(false) }
    var iconsLoading by remember { mutableStateOf(false) }
    var iconTick by remember { mutableIntStateOf(0) }
    var dialogAll by remember { mutableStateOf<Boolean?>(null) }
    var dialogResume by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val progress = remember { Animatable(0f) }
    val scrollState = rememberScrollState()

    suspend fun command(text: String): Any? = withContext(Dispatchers.IO) {
        PerAppBackend.instance.command(text)
    }

    fun scheduleIcons() {
        iconTick += 1
    }

    suspend fun load() {
        if (busy) return
        busy = true
        error = null
        try {
            val rawApps = command("GET app.list")
            val list = (rawApps as? List<*>)
                ?.mapNotNull { perAppMap(it) }
                ?.sortedBy { "${it["label"] ?: it["package"]}".lowercase() }
                ?: emptyList()
            apps = list
            PerAppPresentationCache.apps = list
            scheduleIcons()
            val state = command("GET app.controls")
            val caps = command("GET app.capabilities")
            stateMap = perAppMap(state)
            capabilities = perAppMap(caps) ?: emptyMap()
        } catch (caught: Exception) {
            error = caught.message ?: caught.toString()
        } finally {
            busy = false
        }
    }

    suspend fun refreshStatus() {
        if (busy || polling || stateMap == null) return
        polling = true
        try {
            stateMap = perAppMap(command("GET app.controls"))
        } catch (_: Exception) {
            // Keep the last acknowledged state; don't manufacture a switch.
        } finally {
            polling = false
        }
    }

    suspend fun write(text: String, background: Boolean = false): Boolean {
        if (busy) return false
        busy = !background
        error = null
        if (!background) RodinHaptics.tap()
        return try {
            val reply = command(text)
            stateMap = perAppMap(reply)
            true
        } catch (caught: Exception) {
            error = caught.message ?: caught.toString()
            false
        } finally {
            busy = false
        }
    }

    suspend fun loadIcons() {
        if (iconsLoading) return
        val filtered = filteredApps(apps, searchQuery, showSystem)
        val packages = filtered.take(limit)
            .mapNotNull { it["package"] as? String }
            .filter { !iconAttempts.contains(it) }
            .take(40)
        if (packages.isEmpty()) return
        iconsLoading = true
        iconAttempts.addAll(packages)
        try {
            val loaded = withContext(Dispatchers.IO) {
                PerAppBackend.instance.icons(packages)
            }
            PerAppPresentationCache.icons.putAll(loaded)
            while (PerAppPresentationCache.icons.size > 512) {
                val firstKey = PerAppPresentationCache.icons.keys.first()
                PerAppPresentationCache.icons.remove(firstKey)
            }
            icons = icons + loaded
        } catch (_: Exception) {
            // Removed/inaccessible packages retain their fallback, not a broken list.
        } finally {
            iconsLoading = false
            scheduleIcons()
        }
    }

    fun profileOf(app: Map<String, Any?>): Map<String, Any?> {
        val profiles = (
            (stateMap?.get("config") as? Map<*, *>)?.get("profiles") as? List<*>
            ) ?: emptyList<Any?>()
        for (item in profiles) {
            val entry = item as? Map<*, *> ?: continue
            if (entry["user"] == app["user"] && entry["package"] == app["package"]) {
                @Suppress("UNCHECKED_CAST")
                return (entry["profile"] as? Map<String, Any?>) ?: emptyMap()
            }
        }
        return mapOf("enabled" to true)
    }

    fun confirmReset(all: Boolean, resume: (Boolean) -> Unit) {
        dialogAll = all
        dialogResume = resume
    }

    fun finishDialog(result: Boolean) {
        val resume = dialogResume
        dialogAll = null
        dialogResume = null
        resume?.invoke(result)
    }

    fun consumeBack(): Boolean {
        val current = selected ?: return false
        closingApp = current
        forward = false
        selected = null
        error = null
        return true
    }

    LaunchedEffect(Unit) {
        load()
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                refreshStatus()
                delay(3000)
            }
        }
    }
    LaunchedEffect(iconTick) {
        if (iconTick > 0) {
            delay(100)
            loadIcons()
        }
    }
    LaunchedEffect(selected) {
        if (selected != null) {
            editorVisible = true
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (reduced) {
                        0
                    } else {
                        RodinInteractionSettings.motionDuration(320)
                    },
                    easing = LinearEasing,
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = if (reduced) {
                        0
                    } else {
                        RodinInteractionSettings.motionDuration(220)
                    },
                    easing = LinearEasing,
                ),
            )
            editorVisible = false
            closingApp = null
        }
    }

    DisposableEffect(owner) {
        RodinNestedBackController.attach(
            owner,
            handler = { consumeBack() },
            canPreview = { selected != null },
        )
        onDispose { RodinNestedBackController.detach(owner) }
    }

    val overridesEnabled = (stateMap?.get("config") as? Map<*, *>)?.get("enabled") == true
    val serviceEnabled = stateMap?.get("serviceEnabled") != false
    val ownerMap = stateMap?.get("owner") as? Map<*, *>
    val backendError = stateMap?.get("error")?.toString()

    val listPage: @Composable () -> Unit = {
        AppListPage(
            apps = apps,
            overridesEnabled = overridesEnabled,
            serviceEnabled = serviceEnabled,
            backendError = backendError,
            ownerPackage = ownerMap?.get("package")?.toString(),
            busy = busy,
            error = error,
            limit = limit,
            showSystem = showSystem,
            searchQuery = searchQuery,
            icons = icons,
            scrollState = scrollState,
            onBack = onBack,
            onRefresh = { scope.launch { load() } },
            onSearchChange = { text ->
                searchQuery = text
                limit = 40
                scheduleIcons()
            },
            onToggleSystem = { value ->
                RodinHaptics.tap()
                showSystem = value
                limit = 40
                scheduleIcons()
            },
            onToggleOverrides = { value ->
                scope.launch { write("SET app.enabled ${if (value) 1 else 0}") }
            },
            onExtend = {
                limit += 40
                scheduleIcons()
            },
            onOpenApp = { app ->
                RodinHaptics.tap()
                forward = true
                selected = app
                error = null
            },
            onResetAll = { confirmReset(true) { confirmed -> if (confirmed) scope.launch { write("ACTION app.reset_all") } } },
            profileOf = { app -> profileOf(app) },
        )
    }

    val editorApp = selected ?: closingApp
    val editorPage: @Composable () -> Unit = {
        editorApp?.let { app ->
            AppEditorPage(
                app = app,
                appIcon = icons[app["package"]],
                initial = profileOf(app),
                overridesEnabled = overridesEnabled,
                serviceEnabled = serviceEnabled,
                active = ownerMap?.get("package") == app["package"] &&
                    ownerMap?.get("user") == app["user"] &&
                    stateMap?.get("error") == null,
                busy = busy,
                error = error ?: backendError,
                capabilities = capabilities,
                onBack = { consumeBack() },
                onSave = { profile ->
                    write(
                        "SET app.profile " + jsonEncode(
                            linkedMapOf(
                                "user" to app["user"],
                                "package" to app["package"],
                                "profile" to profile,
                            ),
                        ),
                        background = true,
                    )
                },
                onReset = {
                    write("ACTION app.reset ${app["user"]} ${app["package"]}")
                },
                onEnableOverrides = { write("SET app.enabled 1") },
                requestReset = { resume -> confirmReset(false, resume) },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (selected == null && editorVisible && progress.value > 0.001f) {
            RodinDetailTransition(
                progress = 1f - progress.value,
                reverse = true,
                active = true,
                forward = false,
            ) {
                listPage()
            }
        } else {
            listPage()
        }

        if (selected != null) {
            RodinPredictivePlane(
                nested = true,
                destination = { listPage() },
            ) {
                if (progress.value >= 0.999f) {
                    editorPage()
                } else {
                    RodinDetailTransition(
                        progress = progress.value,
                        reverse = false,
                        active = true,
                        forward = true,
                    ) {
                        editorPage()
                    }
                }
            }
        } else if (editorVisible && editorApp != null) {
            RodinDetailTransition(
                progress = progress.value,
                reverse = false,
                active = false,
                forward = false,
            ) {
                editorPage()
            }
        }
    }

    if (dialogAll != null) {
        RodinDialog(onDismissRequest = { finishDialog(false) }) {
            SurfaceCard(
                modifier = Modifier.widthIn(max = 360.dp),
                padding = PaddingValues(22.dp),
                accent = colors.primary,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (dialogAll == true) {
                            "Reset all app profiles?"
                        } else {
                            "Reset this app?"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = (-0.3).sp,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (dialogAll == true) {
                            "Remove per-app selections and return to your global setup. " +
                                "Your global settings stay unchanged."
                        } else {
                            "Remove this app’s selections and follow your global setup again."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                RodinHaptics.tap()
                                finishDialog(false)
                            },
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                RodinHaptics.tap()
                                finishDialog(true)
                            },
                        ) {
                            Text(if (dialogAll == true) "Reset profiles" else "Reset app")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListPage(
    apps: List<Map<String, Any?>>,
    overridesEnabled: Boolean,
    serviceEnabled: Boolean,
    backendError: String?,
    ownerPackage: String?,
    busy: Boolean,
    error: String?,
    limit: Int,
    showSystem: Boolean,
    searchQuery: String,
    icons: Map<String, Bitmap>,
    scrollState: androidx.compose.foundation.ScrollState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSystem: (Boolean) -> Unit,
    onToggleOverrides: (Boolean) -> Unit,
    onExtend: () -> Unit,
    onOpenApp: (Map<String, Any?>) -> Unit,
    onResetAll: () -> Unit,
    profileOf: (Map<String, Any?>) -> Map<String, Any?>,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val filtered = filteredApps(apps, searchQuery, showSystem)

    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 &&
            scrollState.maxValue - scrollState.value < 500 &&
            filtered.size > limit
        ) {
            onExtend()
        }
    }

    RodinScrollPage(
        header = RodinPageHeader.Detail(
            title = "Per-App Controls",
            onBack = onBack,
            trailing = {
                PressScale(onTap = if (busy) null else ({ onRefresh() })) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(
                                if (dark) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color.Black.copy(alpha = 0.05f)
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        ),
        scrollState = scrollState,
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Set individual touch, refresh rate, CPU and GPU controls for each app.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
            ),
        )
        Spacer(Modifier.height(16.dp))
        HeroCard(
            icon = Icons.Rounded.Tune,
            accent = colors.primary,
            title = if (overridesEnabled) "Individual control" else "Your global setup",
            subtitle = if (overridesEnabled) {
                "Saved app profiles take over only while their app is in focus."
            } else {
                "Per-App Controls starts off. Set profiles first, then enable when ready."
            },
        )
        Spacer(Modifier.height(14.dp))
        SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Per-app overrides",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Off means no app overrides. Your global setup stays saved.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.2.sp,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = overridesEnabled,
                        onCheckedChange = if (busy) {
                            null
                        } else {
                            { value -> onToggleOverrides(value) }
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = if (ownerPackage == null) {
                            Icons.Rounded.Public
                        } else {
                            Icons.Rounded.Apps
                        },
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when {
                            !serviceEnabled ->
                                "Rodin Essential is disabled. No app profile is applied."
                            ownerPackage != null -> "Active app: $ownerPackage"
                            else -> "Your global controls are active."
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.4.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
        val shownError = error ?: backendError
        if (shownError != null) {
            Text(
                text = shownError,
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.error),
            )
        }
        Spacer(Modifier.height(16.dp))
        SurfaceCard(padding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search installed apps")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Show system apps",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                ),
            )
            Switch(
                checked = showSystem,
                onCheckedChange = { value -> onToggleSystem(value) },
                modifier = Modifier.size(width = 52.dp, height = 32.dp),
            )
        }
        Text(
            text = "${filtered.size} apps · " +
                if (showSystem) "Third-party and system" else "Third-party only",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        if (filtered.isNotEmpty()) {
            val shown = filtered.take(limit)
            SurfaceCard(padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Column {
                    shown.forEachIndexed { index, app ->
                        val profile = profileOf(app)
                        val configured = profile.keys.any { it != "enabled" }
                        val tap: (() -> Unit)? = if (busy) null else ({ onOpenApp(app) })
                        PressScale(onTap = tap) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val bitmap = icons[app["package"]]
                                if (bitmap != null) {
                                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                                    Image(
                                        bitmap = image,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (app["system"] == true) {
                                            Icons.Rounded.Android
                                        } else {
                                            Icons.Rounded.Apps
                                        },
                                        contentDescription = null,
                                        tint = if (configured) {
                                            colors.primary
                                        } else {
                                            colors.onSurfaceVariant
                                        },
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${app["label"] ?: app["package"]}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.W600,
                                        ),
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            !configured -> if (app["system"] == true) {
                                                if (app["launchable"] == false) {
                                                    "System component · no launcher entry"
                                                } else {
                                                    "System app · Follow global"
                                                }
                                            } else {
                                                "Follow global"
                                            }
                                            profile["enabled"] != true -> "Profile disabled"
                                            overridesEnabled -> "Profile ready"
                                            else -> "Saved · overrides off"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            color = colors.onSurfaceVariant,
                                        ),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (index != shown.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 62.dp),
                                thickness = 0.8.dp,
                                color = RodinSurfaceLines.divider(colors),
                            )
                        }
                    }
                }
            }
        }
        if (apps.isEmpty() && !busy) {
            Text(
                text = "No installed-app list is available. Tap refresh to retry.",
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onResetAll,
            enabled = !busy,
        ) {
            Icon(
                imageVector = Icons.Rounded.Restore,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text("Reset all app profiles")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppEditorPage(
    app: Map<String, Any?>,
    appIcon: Bitmap?,
    initial: Map<String, Any?>,
    overridesEnabled: Boolean,
    serviceEnabled: Boolean,
    active: Boolean,
    busy: Boolean,
    error: String?,
    capabilities: Map<String, Any?>,
    onBack: () -> Unit,
    onSave: suspend (Map<String, Any?>) -> Boolean,
    onReset: suspend () -> Boolean,
    onEnableOverrides: suspend () -> Boolean,
    requestReset: ((Boolean) -> Unit) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var draft by remember {
        @Suppress("UNCHECKED_CAST")
        mutableStateOf((deepCopy(initial) as? Map<String, Any?>) ?: emptyMap())
    }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var draggingSlider by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }

    fun numbers(key: String): List<Int> = numbersOf(capabilities, key)

    suspend fun saveDraft() {
        if (saving || !dirty || draggingSlider) return
        saving = true
        try {
            while (dirty && !draggingSlider) {
                val revisionAtStart = revision
                @Suppress("UNCHECKED_CAST")
                val snapshot = (deepCopy(draft) as? Map<String, Any?>) ?: emptyMap()
                val saved = onSave(snapshot)
                if (!saved) break // Show the failure; never loop against the daemon.
                if (revisionAtStart == revision) dirty = false
                // New taps replace the pending draft, not a queue of stale selections.
            }
        } finally {
            saving = false
        }
    }

    fun set(key: String, value: Any?) {
        RodinHaptics.tap()
        val next = draft.toMutableMap()
        if (value == null) {
            next.remove(key)
        } else {
            next[key] = value
        }
        draft = next
        dirty = true
        revision += 1
        if (!draggingSlider) scope.launch { saveDraft() }
    }

    fun cpuGet(policy: Int): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return (draft["cpu"] as? Map<String, Any?>)?.get("$policy") as? Map<String, Any?> ?: emptyMap()
    }

    fun cpuSet(policy: Int, key: String, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        val cpu = ((deepCopy(draft["cpu"]) as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val cluster = ((cpu["$policy"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
        if (value == null) {
            cluster.remove(key)
        } else {
            cluster[key] = value
        }
        if (cluster.isEmpty()) {
            cpu.remove("$policy")
        } else {
            cpu["$policy"] = cluster
        }
        set("cpu", if (cpu.isEmpty()) null else cpu)
    }

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "App profile", onBack = onBack),
    ) {
        Spacer(Modifier.height(12.dp))
        HeroCard(
            icon = Icons.Rounded.Apps,
            accent = colors.primary,
            title = "${app["label"] ?: app["package"]}",
            subtitle = when {
                active -> "Profile active · daemon-controlled"
                !serviceEnabled -> "Rodin Essential is disabled"
                !overridesEnabled -> "Saved only · per-app overrides are off"
                draft["enabled"] != true -> "This profile is disabled"
                else -> "Ready · applies when this app is in focus"
            },
            leading = if (appIcon == null) {
                null
            } else {
                {
                    val image = remember(appIcon) { appIcon.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        if (!overridesEnabled) {
            SurfaceCard(accent = colors.primary) {
                Column {
                    Text(
                        text = "Overrides are off",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You can save this profile now. Turn on Per-App Controls " +
                            "to apply it when you open this app.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.8.sp,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            RodinHaptics.tap()
                            scope.launch { onEnableOverrides() }
                        },
                        enabled = !busy && serviceEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Enable Per-App Controls")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (app["launchable"] == false) {
            Text(
                text = "This package has no launcher entry. Its profile can only " +
                    "apply if an activity from this package becomes the focused app.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.8.sp,
                ),
            )
            Spacer(Modifier.height(12.dp))
        }
        if (error != null) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Not applied: $error",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.error,
                            fontSize = 12.sp,
                            lineHeight = 16.8.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        SurfaceCard(accent = Color(0xFFB087FF)) {
            Column {
                PerAppSectionTitle(
                    title = "Profile preferences",
                    icon = Icons.Rounded.Tune,
                    accent = Color(0xFFB087FF),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enable this profile",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                    Switch(
                        checked = draft["enabled"] == true,
                        onCheckedChange = if (busy) {
                            null
                        } else {
                            { value -> set("enabled", value) }
                        },
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Selected controls take priority over your ROM and global " +
                        "setup while this app is in focus. Leaving the app restores " +
                        "your previous settings. Follow global leaves a control unchanged.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.4.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SurfaceCard {
            Column {
                PerAppSectionTitle(
                    title = "Touch & display",
                    icon = Icons.Rounded.TouchApp,
                    accent = Color(0xFF4CC79E),
                )
                Spacer(Modifier.height(14.dp))
                PerAppChoice(
                    title = "Touch response",
                    selected = draft["touch"],
                    choices = numbers("touch").associate {
                        "$it" to if (it == 0) "OEM adaptive" else rodinTouchLabel(it)
                    },
                    busy = busy,
                    change = { value -> set("touch", value?.let { it.toInt() }) },
                    onSliderStart = { draggingSlider = true },
                    onSliderEnd = {
                        draggingSlider = false
                        scope.launch { saveDraft() }
                    },
                )
                PerAppChoice(
                    title = "Refresh rate",
                    selected = draft["refresh"],
                    choices = numbers("refresh").associate {
                        "$it" to if (it == 0) "OEM adaptive" else "$it Hz"
                    },
                    busy = busy,
                    change = { value -> set("refresh", value?.let { it.toInt() }) },
                    onSliderStart = { draggingSlider = true },
                    onSliderEnd = {
                        draggingSlider = false
                        scope.launch { saveDraft() }
                    },
                )
                Text(
                    text = "Uses supported display modes. Refresh rate is not the same as app FPS.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.4.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SurfaceCard {
            Column {
                PerAppSectionTitle(
                    title = "Mali GPU",
                    icon = Icons.Rounded.SportsEsports,
                    accent = Color(0xFFFF5252),
                )
                Spacer(Modifier.height(14.dp))
                PerAppChoice(
                    title = "GPU profile",
                    selected = draft["gpu"],
                    choices = numbers("gpu").associate { "$it" to rodinPerformanceLabel(it) },
                    busy = busy,
                    change = { value -> set("gpu", value?.let { it.toInt() }) },
                    onSliderStart = { draggingSlider = true },
                    onSliderEnd = {
                        draggingSlider = false
                        scope.launch { saveDraft() }
                    },
                )
                Text(
                    text = "Changes GPU controls only.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.4.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        for (policy in listOf(0, 4, 7)) {
            PerAppClusterCard(
                policy = policy,
                capabilities = capabilities,
                cpuPolicy = cpuGet(policy),
                busy = busy,
                setRange = { range -> cpuSet(policy, "range", range) },
                setGovernor = { governor -> cpuSet(policy, "governor", governor) },
                onSliderStart = { draggingSlider = true },
                onSliderEnd = {
                    draggingSlider = false
                    scope.launch { saveDraft() }
                },
            )
        }
        if (capabilities["cores"] == true) {
            SurfaceCard {
                Column {
                    PerAppSectionTitle(
                        title = "CPU cores",
                        icon = Icons.Rounded.Memory,
                        accent = Color(0xFF59BDFC),
                    )
                    Spacer(Modifier.height(14.dp))
                    PerAppChoice(
                        title = "Core control",
                        selected = if (draft["cores"] == null) null else "manual",
                        choices = mapOf("manual" to "Choose online cores"),
                        busy = busy,
                        change = { value -> set("cores", if (value == null) null else 255) },
                        onSliderStart = { draggingSlider = true },
                        onSliderEnd = {
                            draggingSlider = false
                            scope.launch { saveDraft() }
                        },
                    )
                    if (draft["cores"] != null) {
                        val cores = (draft["cores"] as? Number)?.toInt() ?: 255
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (core in 0 until 8) {
                                val chipSelected = (cores and (1 shl core)) != 0
                                FilterChip(
                                    selected = chipSelected,
                                    onClick = {
                                        set(
                                            "cores",
                                            if (chipSelected) {
                                                cores and (1 shl core).inv()
                                            } else {
                                                cores or (1 shl core)
                                            },
                                        )
                                    },
                                    enabled = core != 0 && !busy,
                                    label = { Text("CPU $core") },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "CPU 0 stays online. Follow global preserves your existing " +
                            "automatic/manual core preference.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.8.sp,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                saving -> "Saving your selection…"
                dirty -> "Selection not saved"
                !overridesEnabled -> "Selections saved · overrides are off."
                else -> "Selections save automatically."
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        if (dirty && !saving && error != null) {
            TextButton(onClick = { scope.launch { saveDraft() } }) {
                Text("Retry saving")
            }
        }
        OutlinedButton(
            onClick = {
                requestReset { confirmed ->
                    if (!confirmed) return@requestReset
                    scope.launch {
                        val reset = onReset()
                        if (reset) {
                            draft = mapOf("enabled" to true)
                            dirty = false
                        }
                    }
                }
            },
            enabled = !busy && !saving,
        ) {
            Icon(
                imageVector = Icons.Rounded.Restore,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text("Reset this app to global")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PerAppChoice(
    title: String,
    selected: Any?,
    choices: Map<String, String>,
    busy: Boolean,
    change: (String?) -> Unit,
    onSliderStart: () -> Unit,
    onSliderEnd: () -> Unit,
    followGlobal: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val accent = colors.primary
    val value = selected?.toString() ?: "global"
    val all = linkedMapOf<String, String>()
    if (followGlobal) all["global"] = "Follow global"
    all.putAll(choices)
    if (!all.containsKey(value)) {
        all[value] = "$value · Unavailable on this kernel"
    }
    val steps = choices.keys.toList()
    val frequency = !followGlobal &&
        steps.isNotEmpty() &&
        steps.all { it.toIntOrNull() != null }
    var sliderDragging by remember { mutableStateOf(false) }

    @Composable
    fun tile(key: String, label: String, wide: Boolean) {
        val active = value == key
        val background by animateColorAsState(
            targetValue = if (active) {
                accent.copy(alpha = if (dark) 0.14f else 0.075f)
            } else {
                colors.onSurface.copy(alpha = 0.025f)
            },
            animationSpec = tween(
                RodinInteractionSettings.motionDuration(160),
                easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f),
            ),
            label = "perAppChoiceBg",
        )
        val checkAlpha by animateFloatAsState(
            targetValue = if (active) 1f else 0f,
            animationSpec = tween(RodinInteractionSettings.motionDuration(120)),
            label = "perAppChoiceCheck",
        )
        val tap: (() -> Unit)? = if (busy) {
            null
        } else {
            ({ change(if (key == "global") null else key) })
        }
        PressScale(onTap = tap) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(background, RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (wide) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = if (active) accent else colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
                        color = if (active) colors.primary else colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(16.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(checkAlpha),
                    )
                }
            }
        }
    }

    Column(Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
            ),
        )
        Spacer(Modifier.height(7.dp))
        if (frequency) {
            Text(
                text = all[value] ?: value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W700,
                    color = accent,
                ),
            )
            Slider(
                value = steps.indexOf(value).coerceIn(0, steps.size - 1).toFloat(),
                onValueChange = { raw ->
                    if (!sliderDragging) {
                        sliderDragging = true
                        onSliderStart()
                    }
                    val next = steps[raw.roundToInt().coerceIn(0, steps.size - 1)]
                    if (next != value) change(next)
                },
                onValueChangeFinished = {
                    sliderDragging = false
                    onSliderEnd()
                },
                valueRange = 0f..(if (steps.size > 1) (steps.size - 1).toFloat() else 1f),
                steps = if (steps.size > 1) steps.size - 2 else 0,
                enabled = !busy && steps.size >= 2,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = accent.copy(alpha = 0.12f),
                ),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = choices[steps.first()] ?: steps.first(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = choices[steps.last()] ?: steps.last(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        } else {
            if (followGlobal) {
                tile("global", "Follow global", wide = true)
                if (choices.isNotEmpty()) Spacer(Modifier.height(8.dp))
            }
            BoxWithConstraints {
                val fontScale = LocalDensity.current.fontScale
                val twoColumns = maxWidth >= 260.dp && 12f * fontScale < 18f
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    all.entries.filter { it.key != "global" }.forEach { entry ->
                        Box(
                            Modifier.width(
                                if (twoColumns) (maxWidth - 8.dp) / 2 else maxWidth,
                            ),
                        ) {
                            tile(entry.key, entry.value, wide = false)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerAppClusterCard(
    policy: Int,
    capabilities: Map<String, Any?>,
    cpuPolicy: Map<String, Any?>,
    busy: Boolean,
    setRange: (List<Int>?) -> Unit,
    setGovernor: (String?) -> Unit,
    onSliderStart: () -> Unit,
    onSliderEnd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val caps = ((capabilities["cpu"] as? Map<*, *>)?.get("$policy") as? Map<*, *>)
        ?: emptyMap<String, Any?>()
    val frequencies = ((caps["frequencies"] as? List<*>) ?: emptyList<Any?>())
        .mapNotNull { (it as? Number)?.toInt() }
    if (frequencies.isEmpty()) return
    val range = (cpuPolicy["range"] as? List<*>)
        ?.mapNotNull { (it as? Number)?.toInt() }
        ?.takeIf { it.size >= 2 }
    val name = when (policy) {
        0 -> "Efficiency cluster"
        4 -> "Performance cluster"
        else -> "Prime core"
    }

    Column(Modifier.padding(bottom = 14.dp)) {
        SurfaceCard {
            Column {
                PerAppSectionTitle(
                    title = name,
                    icon = Icons.Rounded.Memory,
                    accent = Color(0xFF67C2FF),
                )
                Spacer(Modifier.height(14.dp))
                PerAppChoice(
                    title = "Frequency control",
                    selected = if (range == null) null else "range",
                    choices = mapOf("range" to "Set minimum and maximum"),
                    busy = busy,
                    change = { value ->
                        setRange(if (value == null) null else listOf(frequencies.first(), frequencies.last()))
                    },
                    onSliderStart = onSliderStart,
                    onSliderEnd = onSliderEnd,
                )
                if (range != null) {
                    val minCandidate = range[0]
                    val maxCandidate = range[1]
                    PerAppChoice(
                        title = "Minimum frequency",
                        selected = minCandidate,
                        choices = buildMap {
                            for (frequency in frequencies) {
                                if (frequency <= maxCandidate) put("$frequency", "$frequency MHz")
                            }
                        },
                        busy = busy,
                        change = { value ->
                            if (value != null) {
                                setRange(listOf(value.toInt(), maxCandidate))
                            }
                        },
                        onSliderStart = onSliderStart,
                        onSliderEnd = onSliderEnd,
                        followGlobal = false,
                    )
                    PerAppChoice(
                        title = "Maximum frequency",
                        selected = maxCandidate,
                        choices = buildMap {
                            for (frequency in frequencies) {
                                if (frequency >= minCandidate) put("$frequency", "$frequency MHz")
                            }
                        },
                        busy = busy,
                        change = { value ->
                            if (value != null) {
                                setRange(listOf(minCandidate, value.toInt()))
                            }
                        },
                        onSliderStart = onSliderStart,
                        onSliderEnd = onSliderEnd,
                        followGlobal = false,
                    )
                    Text(
                        text = "Use the same minimum and maximum for an exact frequency target.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.8.sp,
                        ),
                    )
                    Spacer(Modifier.height(14.dp))
                }
                PerAppChoice(
                    title = "Governor",
                    selected = cpuPolicy["governor"],
                    choices = ((caps["governors"] as? List<*>) ?: emptyList<Any?>())
                        .associate { "$it" to "$it" },
                    busy = busy,
                    change = { value -> setGovernor(value) },
                    onSliderStart = onSliderStart,
                    onSliderEnd = onSliderEnd,
                )
                Text(
                    text = "Governor selection is independent of the frequency range.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.8.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PerAppSectionTitle(
    title: String,
    icon: ImageVector,
    accent: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.065f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.2).sp,
            ),
        )
    }
}
