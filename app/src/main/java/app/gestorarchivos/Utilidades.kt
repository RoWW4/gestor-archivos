package app.gestorarchivos

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val espanol = Locale("es", "ES")

fun tamanoLegible(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(espanol, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(espanol, "%.1f MB", mb)
    return String.format(espanol, "%.2f GB", mb / 1024.0)
}

fun fechaLegible(ms: Long): String {
    val hoy = Calendar.getInstance()
    val f = Calendar.getInstance().apply { timeInMillis = ms }
    val mismoDia = hoy.get(Calendar.YEAR) == f.get(Calendar.YEAR) &&
        hoy.get(Calendar.DAY_OF_YEAR) == f.get(Calendar.DAY_OF_YEAR)
    val patron = when {
        mismoDia -> "'Hoy,' HH:mm"
        hoy.get(Calendar.YEAR) == f.get(Calendar.YEAR) -> "d MMM, HH:mm"
        else -> "d MMM yyyy"
    }
    return SimpleDateFormat(patron, espanol).format(Date(ms))
}

fun fechaCorta(ms: Long): String = SimpleDateFormat("d MMM yyyy", espanol).format(Date(ms))

/** Nombre bonito de una carpeta: "Download/" → "Descargas". */
fun carpetaLegible(carpeta: String): String {
    val c = carpeta.trimEnd('/')
    if (c.isEmpty()) return "Almacenamiento"
    return when (c.lowercase(Locale.ROOT)) {
        "download" -> "Descargas"
        "dcim/camera" -> "Cámara"
        "pictures/screenshots", "dcim/screenshots" -> "Capturas"
        "documents" -> "Documentos"
        "pictures" -> "Imágenes"
        "music" -> "Música"
        "movies" -> "Películas"
        else -> c.replace("Android/media/com.whatsapp/WhatsApp/Media/", "WhatsApp › ")
            .replace("Android/media/org.telegram.messenger/Telegram/", "Telegram › ")
            .replace("/", " › ")
    }
}

private fun uriDe(context: Context, a: Archivo): Uri =
    FileProvider.getUriForFile(context, context.packageName + ".archivos", File(a.ruta))

private fun mimeDe(a: Archivo): String =
    a.mime ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension(a.nombre)) ?: "*/*"

fun abrir(context: Context, a: Archivo, elegirApp: Boolean = false) {
    try {
        val uri = uriDe(context, a)
        val mime = if (a.tipo == Tipo.APK) "application/vnd.android.package-archive" else mimeDe(a)
        var intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (elegirApp) intent = Intent.createChooser(intent, "Abrir con")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay ninguna app para abrir este archivo", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "No se ha podido abrir", Toast.LENGTH_SHORT).show()
    }
}

fun compartir(context: Context, archivos: List<Archivo>) {
    if (archivos.isEmpty()) return
    try {
        val uris = archivos.map { uriDe(context, it) }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeDe(archivos[0])
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            val mimes = archivos.map { mimeDe(it) }.toSet()
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (mimes.size == 1) mimes.first() else "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        intent.clipData = ClipData.newRawUri("", uris[0]).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Compartir"))
    } catch (e: Exception) {
        Toast.makeText(context, "No se ha podido compartir", Toast.LENGTH_SHORT).show()
    }
}
