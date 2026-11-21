package app.gestorarchivos

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.gestorarchivos.ui.App

/** Otra app (WhatsApp, Gmail, el navegador…) nos pide que elijamos un archivo. */
data class Eleccion(val mimes: List<String>, val varios: Boolean)

/** Qué ha pedido quien abre la app. */
sealed interface Inicio {
    data object Normal : Inicio
    data object Descargas : Inicio
    data class Elegir(val eleccion: Eleccion) : Inicio
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val inicio = leerInicio(intent)
        setContent {
            App(
                inicio = inicio,
                onElegidos = { archivos -> devolver(archivos) },
            )
        }
    }

    private fun leerInicio(intent: Intent?): Inicio = when (intent?.action) {
        Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK -> {
            val extra = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList().orEmpty()
            val mimes = extra.ifEmpty { listOfNotNull(intent.type) }.filter { it != "*/*" }
            Inicio.Elegir(Eleccion(mimes, intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)))
        }
        "android.intent.action.VIEW_DOWNLOADS" -> Inicio.Descargas
        else -> Inicio.Normal
    }

    /** Devuelve los archivos elegidos a la app que los pidió, con permiso solo para leerlos. */
    private fun devolver(archivos: List<Archivo>) {
        if (archivos.isEmpty()) {
            setResult(Activity.RESULT_CANCELED); finish(); return
        }
        val uris = archivos.map { uriDe(this, it) }
        val datos = Intent().apply {
            if (archivos.size == 1) setDataAndType(uris[0], mimeDe(archivos[0])) else data = uris[0]
            clipData = ClipData.newRawUri("", uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, datos)
        finish()
    }
}
