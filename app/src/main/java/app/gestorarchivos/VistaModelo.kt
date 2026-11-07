package app.gestorarchivos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class Resultado(
    val archivos: List<Archivo> = emptyList(),
    val tamanoTotal: Long = 0,
)

class VistaModelo(app: Application) : AndroidViewModel(app) {

    private val todos = MutableStateFlow<List<Archivo>>(emptyList())
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _filtros = MutableStateFlow(Filtros())
    val filtros: StateFlow<Filtros> = _filtros.asStateFlow()

    val resultado: StateFlow<Resultado> = combine(todos, _filtros) { lista, f -> filtrar(lista, f) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resultado())

    /** Carpetas con archivos, para elegir una concreta. */
    fun carpetas(): List<Pair<String, Int>> =
        todos.value.groupingBy { it.carpeta }.eachCount()
            .toList().sortedBy { it.first.lowercase(Locale.ROOT) }

    fun cargar() {
        if (_cargando.value) return
        _cargando.value = true
        viewModelScope.launch {
            val lista = withContext(Dispatchers.IO) {
                runCatching { Repositorio.leerTodo(getApplication()) }.getOrDefault(emptyList())
            }
            todos.value = lista
            _cargando.value = false
        }
    }

    fun cambiar(cambio: (Filtros) -> Filtros) = _filtros.update(cambio)

    fun limpiar() = _filtros.update { Filtros(orden = it.orden, ocultos = it.ocultos) }

    private fun filtrar(lista: List<Archivo>, f: Filtros): Resultado {
        val texto = normalizar(f.texto.trim())
        val (desde, hasta) = limites(f)
        val filtrados = lista.filter { a ->
            (f.ocultos || !oculto(a)) &&
                (f.tipos.isEmpty() || a.tipo in f.tipos) &&
                f.lugar.contiene(a) &&
                (f.carpeta == null || a.carpeta == f.carpeta) &&
                (desde == null || a.fecha >= desde) &&
                (hasta == null || a.fecha < hasta) &&
                (texto.isEmpty() || a.clave.contains(texto))
        }
        val ordenados = when (f.orden) {
            Orden.RECIENTES -> filtrados.sortedByDescending { it.fecha }
            Orden.ANTIGUOS -> filtrados.sortedBy { it.fecha }
            Orden.NOMBRE -> filtrados.sortedBy { it.clave }
            Orden.GRANDES -> filtrados.sortedByDescending { it.tamano }
        }
        return Resultado(ordenados, ordenados.sumOf { it.tamano })
    }

    private fun oculto(a: Archivo) =
        a.nombre.startsWith('.') || a.carpeta.startsWith('.') || a.carpeta.contains("/.")

    private fun limites(f: Filtros): Pair<Long?, Long?> {
        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dia = 24L * 60 * 60 * 1000
        return when (f.periodo) {
            Periodo.TODO -> null to null
            Periodo.HOY -> inicioHoy.timeInMillis to null
            Periodo.SEMANA -> inicioHoy.timeInMillis - 6 * dia to null
            Periodo.MES -> inicioHoy.timeInMillis - 29 * dia to null
            Periodo.ANO -> inicioHoy.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis to null
            // El selector de fechas da medianoche UTC; "hasta" incluye el día entero
            Periodo.RANGO -> f.desde?.let { inicioDiaLocal(it) } to f.hasta?.let { inicioDiaLocal(it) + dia }
        }
    }

    private fun inicioDiaLocal(utc: Long): Long {
        val u = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        return Calendar.getInstance().apply {
            clear()
            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
