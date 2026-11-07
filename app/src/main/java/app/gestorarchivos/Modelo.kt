package app.gestorarchivos

import java.util.Locale

/** Un archivo del móvil, tal como lo devuelve el índice de Android. */
data class Archivo(
    val id: Long,
    val nombre: String,
    val ruta: String,
    val carpeta: String,     // ruta relativa, por ejemplo "Download/" o "DCIM/Camera/"
    val tamano: Long,        // bytes
    val fecha: Long,         // milisegundos
    val mime: String?,
    val tipo: Tipo,
    val clave: String = normalizar(nombre),   // nombre sin tildes ni mayúsculas, para buscar
)

enum class Tipo(val nombre: String) {
    IMAGEN("Imágenes"),
    VIDEO("Vídeos"),
    AUDIO("Audio"),
    DOCUMENTO("Documentos"),
    APK("APK"),
    COMPRIMIDO("Comprimidos"),
    OTRO("Otros");

    companion object {
        private val documentos = setOf(
            "pdf", "doc", "docx", "odt", "rtf", "txt", "md", "xls", "xlsx", "ods", "csv",
            "ppt", "pptx", "odp", "epub", "html", "htm", "xml", "json", "pages", "numbers", "key",
        )
        private val comprimidos = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")
        private val imagenes = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "dng", "svg")
        private val videos = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
        private val audios = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "mid")

        fun de(nombre: String, mime: String?): Tipo {
            val ext = extension(nombre)
            return when {
                ext == "apk" || ext == "apks" || ext == "xapk" -> APK
                ext in comprimidos -> COMPRIMIDO
                mime?.startsWith("image/") == true || ext in imagenes -> IMAGEN
                mime?.startsWith("video/") == true || ext in videos -> VIDEO
                mime?.startsWith("audio/") == true || ext in audios -> AUDIO
                ext in documentos || mime?.startsWith("text/") == true -> DOCUMENTO
                else -> OTRO
            }
        }
    }
}

private val marcas = Regex("\\p{Mn}+")

fun normalizar(s: String): String =
    java.text.Normalizer.normalize(s.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(marcas, "")

fun extension(nombre: String): String =
    nombre.substringAfterLast('.', "").lowercase(Locale.ROOT)

/** Accesos directos a las carpetas de siempre. */
enum class Lugar(val nombre: String, val prefijos: List<String>) {
    TODO("Todo", emptyList()),
    DESCARGAS("Descargas", listOf("Download/")),
    CAMARA("Cámara", listOf("DCIM/Camera/", "DCIM/")),
    CAPTURAS("Capturas", listOf("Pictures/Screenshots/", "DCIM/Screenshots/", "Pictures/Screen recordings/", "Movies/Screen recordings/")),
    WHATSAPP("WhatsApp", listOf("Android/media/com.whatsapp/", "WhatsApp/")),
    TELEGRAM("Telegram", listOf("Android/media/org.telegram.messenger/", "Telegram/", "Pictures/Telegram/", "Movies/Telegram/", "Download/Telegram/")),
    DOCUMENTOS("Documentos", listOf("Documents/")),
    IMAGENES("Imágenes", listOf("Pictures/")),
    MUSICA("Música", listOf("Music/", "Recordings/")),
    BLUETOOTH("Bluetooth", listOf("Download/Bluetooth/", "Bluetooth/"));

    fun contiene(a: Archivo): Boolean =
        prefijos.isEmpty() || prefijos.any { a.carpeta.startsWith(it, ignoreCase = true) }
}

enum class Periodo(val nombre: String) {
    TODO("Cualquier fecha"),
    HOY("Hoy"),
    SEMANA("Últimos 7 días"),
    MES("Últimos 30 días"),
    ANO("Este año"),
    RANGO("Elegir fechas…"),
}

enum class Orden(val nombre: String) {
    RECIENTES("Más recientes"),
    ANTIGUOS("Más antiguos"),
    NOMBRE("Nombre (A-Z)"),
    GRANDES("Más grandes"),
}

data class Filtros(
    val texto: String = "",
    val tipos: Set<Tipo> = emptySet(),          // vacío = todos
    val lugar: Lugar = Lugar.TODO,
    val carpeta: String? = null,                // carpeta concreta elegida a mano
    val periodo: Periodo = Periodo.TODO,
    val desde: Long? = null,                    // solo con RANGO
    val hasta: Long? = null,
    val orden: Orden = Orden.RECIENTES,
    val ocultos: Boolean = false,               // mostrar archivos y carpetas que empiezan por "."
)
