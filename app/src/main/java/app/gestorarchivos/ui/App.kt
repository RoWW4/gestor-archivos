package app.gestorarchivos.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gestorarchivos.Archivo
import app.gestorarchivos.Filtros
import app.gestorarchivos.Lugar
import app.gestorarchivos.Orden
import app.gestorarchivos.Periodo
import app.gestorarchivos.Tipo
import app.gestorarchivos.VistaModelo
import app.gestorarchivos.abrir
import app.gestorarchivos.carpetaLegible
import app.gestorarchivos.compartir
import app.gestorarchivos.fechaCorta
import app.gestorarchivos.fechaLegible
import app.gestorarchivos.normalizar
import app.gestorarchivos.tamanoLegible

@Composable
fun App() {
    Tema {
        var permiso by remember { mutableStateOf(Environment.isExternalStorageManager()) }
        val vm: VistaModelo = viewModel()
        LifecycleResumeEffect(Unit) {
            permiso = Environment.isExternalStorageManager()
            // Al volver a la app se vuelve a leer, por si hay archivos nuevos
            if (permiso) vm.cargar()
            onPauseOrDispose { }
        }
        if (permiso) Principal(vm) else PedirPermiso()
    }
}

@Composable
private fun PedirPermiso() {
    val context = LocalContext.current
    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.FolderOpen, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Hace falta un permiso", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Para ver todos tus archivos, activa «Permitir acceso para gestionar todos los archivos». " +
                    "La app no tiene acceso a internet: nada sale de tu móvil.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + context.packageName),
                )
                runCatching { context.startActivity(intent) }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }) { Text("Dar permiso") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Principal(vm: VistaModelo) {
    val context = LocalContext.current
    val f by vm.filtros.collectAsStateWithLifecycle()
    val res by vm.resultado.collectAsStateWithLifecycle()
    val cargando by vm.cargando.collectAsStateWithLifecycle()

    var detalle by remember { mutableStateOf<Archivo?>(null) }
    var verCarpetas by remember { mutableStateOf(false) }
    var verRango by remember { mutableStateOf(false) }

    BackHandler(enabled = f.texto.isNotEmpty() || f.carpeta != null) {
        if (f.texto.isNotEmpty()) vm.cambiar { it.copy(texto = "") } else vm.cambiar { it.copy(carpeta = null) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Archivos", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${res.archivos.size} archivos · ${tamanoLegible(res.tamanoTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    MenuOrden(f.orden) { o -> vm.cambiar { it.copy(orden = o) } }
                    IconButton(onClick = { vm.cargar() }) { Icon(Icons.Filled.Refresh, "Actualizar") }
                    MenuMas(f.ocultos) { v -> vm.cambiar { it.copy(ocultos = v) } }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                value = f.texto,
                onValueChange = { t -> vm.cambiar { it.copy(texto = t) } },
                placeholder = { Text("Buscar por nombre") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (f.texto.isNotEmpty()) IconButton(onClick = { vm.cambiar { it.copy(texto = "") } }) {
                        Icon(Icons.Filled.Close, "Borrar")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            // Tipos
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = f.tipos.isEmpty(),
                    onClick = { vm.cambiar { it.copy(tipos = emptySet()) } },
                    label = { Text("Todos") },
                )
                Tipo.entries.forEach { t ->
                    FilterChip(
                        selected = t in f.tipos,
                        onClick = {
                            vm.cambiar { it.copy(tipos = if (t in it.tipos) it.tipos - t else it.tipos + t) }
                        },
                        label = { Text(t.nombre) },
                    )
                }
            }
            // Ubicación y fecha
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuLugar(
                    f = f,
                    onLugar = { l -> vm.cambiar { it.copy(lugar = l, carpeta = null) } },
                    onOtra = { verCarpetas = true },
                )
                MenuFecha(
                    f = f,
                    onPeriodo = { p ->
                        if (p == Periodo.RANGO) verRango = true
                        else vm.cambiar { it.copy(periodo = p, desde = null, hasta = null) }
                    },
                )
                if (f != Filtros(orden = f.orden, ocultos = f.ocultos)) {
                    TextButton(onClick = { vm.limpiar() }) { Text("Quitar filtros") }
                }
            }
            if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            else Spacer(Modifier.height(4.dp))

            if (res.archivos.isEmpty() && !cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay archivos con estos filtros", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(res.archivos, key = { it.id }) { a ->
                        Fila(
                            a,
                            onClick = { abrir(context, a) },
                            onLongClick = { detalle = a },
                        )
                    }
                }
            }
        }
    }

    detalle?.let { a ->
        Detalle(
            a,
            onCerrar = { detalle = null },
            onVerCarpeta = {
                vm.cambiar { it.copy(carpeta = a.carpeta, lugar = Lugar.TODO, texto = "") }
                detalle = null
            },
        )
    }
    if (verCarpetas) {
        ElegirCarpeta(
            carpetas = remember { vm.carpetas() },
            onElegir = { c -> vm.cambiar { it.copy(carpeta = c, lugar = Lugar.TODO) }; verCarpetas = false },
            onCerrar = { verCarpetas = false },
        )
    }
    if (verRango) {
        ElegirRango(
            onElegir = { d, h ->
                vm.cambiar { it.copy(periodo = Periodo.RANGO, desde = d, hasta = h ?: d) }
                verRango = false
            },
            onCerrar = { verRango = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Fila(a: Archivo, onClick: () -> Unit, onLongClick: () -> Unit) {
    ListItem(
        leadingContent = { Miniatura(a) },
        headlineContent = { Text(a.nombre, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${tamanoLegible(a.tamano)} · ${fechaLegible(a.fecha)} · ${carpetaLegible(a.carpeta)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@Composable
private fun ChipMenu(texto: String, activo: Boolean, icono: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = activo,
        onClick = onClick,
        label = { Text(texto, maxLines = 1) },
        leadingIcon = { Icon(icono, null, Modifier.size(18.dp)) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp)) },
    )
}

@Composable
private fun MenuLugar(f: Filtros, onLugar: (Lugar) -> Unit, onOtra: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val texto = f.carpeta?.let { carpetaLegible(it) } ?: if (f.lugar == Lugar.TODO) "Ubicación" else f.lugar.nombre
    Box {
        ChipMenu(texto, f.carpeta != null || f.lugar != Lugar.TODO, Icons.Filled.Folder) { abierto = true }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Lugar.entries.forEach { l ->
                DropdownMenuItem(
                    text = { Text(if (l == Lugar.TODO) "Todas las ubicaciones" else l.nombre) },
                    leadingIcon = { RadioButton(selected = f.carpeta == null && f.lugar == l, onClick = null) },
                    onClick = { onLugar(l); abierto = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Otra carpeta…") },
                leadingIcon = { Icon(Icons.Filled.FolderOpen, null) },
                onClick = { onOtra(); abierto = false },
            )
        }
    }
}

@Composable
private fun MenuFecha(f: Filtros, onPeriodo: (Periodo) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val texto = when (f.periodo) {
        Periodo.TODO -> "Fecha"
        Periodo.RANGO -> {
            val d = f.desde?.let { fechaCortaUtc(it) } ?: ""
            val h = f.hasta?.let { fechaCortaUtc(it) } ?: ""
            if (d == h) d else "$d – $h"
        }
        else -> f.periodo.nombre
    }
    Box {
        ChipMenu(texto, f.periodo != Periodo.TODO, Icons.Filled.CalendarMonth) { abierto = true }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Periodo.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.nombre) },
                    leadingIcon = { RadioButton(selected = f.periodo == p, onClick = null) },
                    onClick = { onPeriodo(p); abierto = false },
                )
            }
        }
    }
}

/** Las fechas del selector vienen a medianoche UTC. */
private fun fechaCortaUtc(utc: Long): String {
    val zona = java.util.TimeZone.getDefault()
    return fechaCorta(utc - zona.getOffset(utc) + 12L * 60 * 60 * 1000)
}

@Composable
private fun MenuOrden(actual: Orden, onOrden: (Orden) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Orden.entries.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o.nombre) },
                    leadingIcon = { RadioButton(selected = o == actual, onClick = null) },
                    onClick = { onOrden(o); abierto = false },
                )
            }
        }
    }
}

@Composable
private fun MenuMas(ocultos: Boolean, onOcultos: (Boolean) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) { Icon(Icons.Filled.MoreVert, "Más") }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text("Mostrar archivos ocultos") },
                leadingIcon = { Checkbox(checked = ocultos, onCheckedChange = null) },
                onClick = { onOcultos(!ocultos); abierto = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Detalle(a: Archivo, onCerrar: () -> Unit, onVerCarpeta: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Miniatura(a, 64.dp)
                Spacer(Modifier.width(16.dp))
                Text(a.nombre, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            Dato("Tipo", a.tipo.nombre)
            Dato("Tamaño", tamanoLegible(a.tamano))
            Dato("Fecha", fechaLegible(a.fecha))
            Dato("Ubicación", a.ruta)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { abrir(context, a); onCerrar() }) { Text("Abrir") }
                FilledTonalButton(onClick = { compartir(context, listOf(a)); onCerrar() }) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Compartir")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { abrir(context, a, elegirApp = true); onCerrar() }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Abrir con")
                }
                OutlinedButton(onClick = onVerCarpeta) {
                    Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ver carpeta")
                }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ElegirCarpeta(carpetas: List<Pair<String, Int>>, onElegir: (String) -> Unit, onCerrar: () -> Unit) {
    var busca by remember { mutableStateOf("") }
    val lista = remember(busca, carpetas) {
        val b = normalizar(busca.trim())
        if (b.isEmpty()) carpetas else carpetas.filter { normalizar(it.first).contains(b) }
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        title = { Text("Elegir carpeta") },
        text = {
            Column {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = { Text("Buscar carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(400.dp)) {
                    items(lista, key = { it.first }) { (c, n) ->
                        ListItem(
                            headlineContent = { Text(if (c.isEmpty()) "Almacenamiento (raíz)" else c.trimEnd('/')) },
                            supportingContent = { Text("$n archivos") },
                            leadingContent = { Icon(Icons.Filled.Folder, null) },
                            modifier = Modifier.clickable { onElegir(c) },
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegirRango(onElegir: (Long, Long?) -> Unit, onCerrar: () -> Unit) {
    val estado = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                enabled = estado.selectedStartDateMillis != null,
                onClick = { estado.selectedStartDateMillis?.let { onElegir(it, estado.selectedEndDateMillis) } },
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) {
        DateRangePicker(state = estado, modifier = Modifier.weight(1f))
    }
}
