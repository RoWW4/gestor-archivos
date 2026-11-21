package app.gestorarchivos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeloTest {

    @Test
    fun sinTiposPedidosValeTodo() {
        assertTrue(coincideMime(null, emptyList()))
        assertTrue(coincideMime("image/jpeg", emptyList()))
    }

    @Test
    fun comodines() {
        assertTrue(coincideMime("image/png", listOf("image/*")))
        assertFalse(coincideMime("video/mp4", listOf("image/*")))
        assertTrue(coincideMime(null, listOf("*/*")))
        assertFalse(coincideMime(null, listOf("image/*")))
    }

    @Test
    fun tipoExactoYVarios() {
        assertTrue(coincideMime("application/pdf", listOf("application/pdf")))
        assertTrue(coincideMime("APPLICATION/PDF", listOf("application/pdf")))
        assertFalse(coincideMime("application/zip", listOf("application/pdf")))
        assertTrue(coincideMime("video/mp4", listOf("image/*", "video/*")))
    }

    @Test
    fun tiposPorExtension() {
        assertEquals(Tipo.APK, Tipo.de("app.apk", null))
        assertEquals(Tipo.IMAGEN, Tipo.de("foto.HEIC", null))
        assertEquals(Tipo.DOCUMENTO, Tipo.de("factura.pdf", "application/pdf"))
    }

    @Test
    fun buscarSinTildes() {
        assertEquals("cancion", normalizar("Canción"))
    }
}
