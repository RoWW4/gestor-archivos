package app.gestorarchivos

import android.content.Context
import android.provider.MediaStore
import java.io.File

/**
 * Lee todos los archivos del almacenamiento a partir del índice de Android (MediaStore).
 * Con el permiso «Acceso a todos los archivos» el índice incluye también documentos, APK, etc.
 */
object Repositorio {

    fun leerTodo(context: Context): List<Archivo> {
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val columnas = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val lista = ArrayList<Archivo>(20000)
        context.contentResolver.query(uri, columnas, null, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(columnas[0])
            val iNombre = c.getColumnIndexOrThrow(columnas[1])
            val iRuta = c.getColumnIndexOrThrow(columnas[2])
            val iRel = c.getColumnIndexOrThrow(columnas[3])
            val iTam = c.getColumnIndexOrThrow(columnas[4])
            val iFecha = c.getColumnIndexOrThrow(columnas[5])
            val iMime = c.getColumnIndexOrThrow(columnas[6])
            while (c.moveToNext()) {
                val ruta = c.getString(iRuta) ?: continue
                val nombre = c.getString(iNombre) ?: ruta.substringAfterLast('/')
                val mime = c.getString(iMime)
                val tamano = c.getLong(iTam)
                // Las carpetas también están en el índice: se descartan
                if (mime == null && (tamano == 0L || !nombre.contains('.')) && File(ruta).isDirectory) continue
                val carpeta = c.getString(iRel) ?: relativa(ruta)
                lista += Archivo(
                    id = c.getLong(iId),
                    nombre = nombre,
                    ruta = ruta,
                    carpeta = carpeta,
                    tamano = tamano,
                    fecha = c.getLong(iFecha) * 1000L,
                    mime = mime,
                    tipo = Tipo.de(nombre, mime),
                )
            }
        }
        return lista
    }

    private fun relativa(ruta: String): String {
        val base = "/storage/emulated/0/"
        val sinBase = if (ruta.startsWith(base)) ruta.removePrefix(base) else ruta.trimStart('/')
        val i = sinBase.lastIndexOf('/')
        return if (i < 0) "" else sinBase.substring(0, i + 1)
    }
}
