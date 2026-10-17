package io.github.neeschal.rodinessential.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.expand
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

/** One image on the shared media volume. */
private data class RodinGalleryItem(val path: String, val modified: Long)

private const val GALLERY_MAX_ITEMS = 360

private val GALLERY_ROOTS = listOf(
    "/storage/emulated/0/DCIM",
    "/storage/emulated/0/Pictures",
    "/storage/emulated/0/Download",
)

private fun gallerySupported(path: String): Boolean {
    val lower = path.lowercase()
    return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp")
}

private fun galleryReadable(file: File): Boolean = try {
    file.inputStream().use { it.read() }
    true
} catch (_: Exception) {
    false
}

@Suppress("UNUSED_PARAMETER")
private fun galleryReadableQuiet(file: File): Boolean = try {
    file.inputStream().close()
    true
} catch (_: Exception) {
    false
}

private fun galleryCollect(): List<RodinGalleryItem> {
    val result = ArrayList<RodinGalleryItem>()
    val seen = HashSet<String>()
    for (rootPath in GALLERY_ROOTS) {
        if (result.size >= GALLERY_MAX_ITEMS) break
        val root = File(rootPath)
        if (!root.exists()) continue
        try {
            val stack = ArrayDeque<File>()
            stack.addLast(root)
            while (stack.isNotEmpty() && result.size < GALLERY_MAX_ITEMS) {
                val dir = stack.removeLast()
                val children = dir.listFiles() ?: continue
                for (child in children) {
                    if (result.size >= GALLERY_MAX_ITEMS) break
                    if (child.isDirectory) {
                        val symlink = try {
                            Files.isSymbolicLink(child.toPath())
                        } catch (_: Exception) {
                            false
                        }
                        if (!symlink) stack.addLast(child)
                    } else if (child.isFile) {
                        val path = child.path
                        if (!gallerySupported(path) || !seen.add(path)) continue
                        if (!galleryReadableQuiet(child)) continue
                        val modified = try {
                            child.lastModified()
                        } catch (_: Exception) {
                            0L
                        }
                        result.add(RodinGalleryItem(path = path, modified = modified))
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
    return result.sortedByDescending { it.modified }
}

private fun decodeThumbnail(path: String, target: Int): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
} catch (_: Exception) {
    null
}

/**
 * The private-storage photo picker the appearance settings open. Scans the
 * shared media volumes exactly like the Dart browser, newest first.
 */
@Composable
fun RodinGalleryBrowser(
    permissionState: Int,
    onCancel: () -> Unit,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<RodinGalleryItem>>(emptyList()) }

    produceState(Unit) {
        val loaded = withContext(Dispatchers.IO) { galleryCollect() }
        items = loaded
        loading = false
        if (loaded.isEmpty()) {
            error = if (permissionState == 1) {
                "Android granted selected-photo " +
                    "access, but no readable JPG, " +
                    "PNG or WebP files were exposed " +
                    "through the shared media paths."
            } else {
                "No readable JPG, PNG or WebP " +
                    "images were found in DCIM, " +
                    "Pictures or Download."
            }
        }
    }

    Surface(
        modifier = modifier,
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column {
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 38.dp, height = 4.dp)
                    .background(colors.outline.copy(alpha = 0.52f), RoundedCornerShape(99.dp)),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Choose background",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-0.25).sp,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (permissionState == 1) {
                            "Selected-photo access"
                        } else {
                            "Photos on this device"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
            Box(Modifier.weight(1f)) {
                when {
                    loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    error != null -> Box(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = error!!,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.onSurfaceVariant,
                                lineHeight = 1.35f * 14.sp,
                            ),
                        )
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        items(items = items, key = { it.path }) { item ->
                            RodinGalleryThumb(
                                path = item.path,
                                onTap = { onSelected(item.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RodinGalleryThumb(path: String, onTap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(path, 360) }
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(path) {
                detectTapGestures { onTap() }
            },
    ) {
        val picture = bitmap
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** One pill of the appearance settings: icon, label and a selected state. */
@Composable
fun RodinAppearanceChoice(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    PressScale(onTap = onTap, modifier = modifier) {
        Row(
            Modifier
                .background(
                    if (selected) {
                        colors.primary.copy(alpha = if (dark) 0.17f else 0.12f)
                    } else {
                        colors.surfaceContainer.copy(alpha = if (dark) 0.92f else 0.94f)
                    },
                    RoundedCornerShape(14.dp),
                )
                .border(
                    1.dp,
                    if (selected) {
                        colors.primary.copy(alpha = 0.32f)
                    } else {
                        colors.outline.copy(alpha = if (dark) 0.62f else 0.72f)
                    },
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.W800 else FontWeight.W600,
                ),
            )
        }
    }
}
