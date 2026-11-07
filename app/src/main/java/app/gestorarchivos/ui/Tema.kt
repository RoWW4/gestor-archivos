package app.gestorarchivos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Colores del sistema (Material You): sigue el modo claro u oscuro del móvil. */
@Composable
fun Tema(contenido: @Composable () -> Unit) {
    val context = LocalContext.current
    val colores = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colores, content = contenido)
}
