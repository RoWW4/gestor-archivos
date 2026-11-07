package app.gestorarchivos.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.gestorarchivos.Archivo
import app.gestorarchivos.Tipo
import app.gestorarchivos.extension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun icono(a: Archivo): ImageVector = when (a.tipo) {
    Tipo.IMAGEN -> Icons.Filled.Image
    Tipo.VIDEO -> Icons.Filled.Movie
    Tipo.AUDIO -> Icons.Filled.Audiotrack
    Tipo.DOCUMENTO -> if (extension(a.nombre) == "pdf") Icons.Filled.PictureAsPdf else Icons.Filled.Description
    Tipo.APK -> Icons.Filled.Android
    Tipo.COMPRIMIDO -> Icons.Filled.FolderZip
    Tipo.OTRO -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
fun Miniatura(a: Archivo, tam: Dp = 48.dp) {
    val forma = RoundedCornerShape(10.dp)
    val base = Modifier.size(tam).clip(forma).background(MaterialTheme.colorScheme.secondaryContainer)
    when (a.tipo) {
        Tipo.IMAGEN, Tipo.VIDEO -> {
            val context = LocalContext.current
            Box(base, contentAlignment = Alignment.Center) {
                Icon(icono(a), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(a.ruta))
                        .size(160)
                        .videoFrameMillis(1000)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(tam),
                )
            }
        }
        Tipo.APK -> {
            val context = LocalContext.current
            val dibujo by produceState<ImageBitmap?>(null, a.ruta) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val pm = context.packageManager
                        val info = pm.getPackageArchiveInfo(a.ruta, 0) ?: return@runCatching null
                        val app = info.applicationInfo ?: return@runCatching null
                        app.sourceDir = a.ruta
                        app.publicSourceDir = a.ruta
                        aBitmap(app.loadIcon(pm))
                    }.getOrNull()
                }
            }
            Box(base, contentAlignment = Alignment.Center) {
                val d = dibujo
                if (d != null) Image(d, null, Modifier.size(tam * 0.8f))
                else Icon(icono(a), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        else -> Box(base, contentAlignment = Alignment.Center) {
            Icon(icono(a), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

private fun aBitmap(d: Drawable): ImageBitmap {
    val w = d.intrinsicWidth.coerceIn(1, 192)
    val h = d.intrinsicHeight.coerceIn(1, 192)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    d.setBounds(0, 0, w, h)
    d.draw(c)
    return bmp.asImageBitmap()
}
