# Gestor de archivos · instrucciones para Claude

App de Android de Rocío. Respóndele en español, breve y con palabras sencillas. El repositorio es público: nada de datos personales en código ni commits.

- Kotlin + Jetpack Compose, minSdk 30. Sin permiso de INTERNET (no añadirlo).
- `Modelo.kt` tipos y filtros · `Repositorio.kt` lee MediaStore · `VistaModelo.kt` filtra y ordena · `Utilidades.kt` abrir, compartir, formatos · `ui/` pantallas.
- Al hacer push a `main`, GitHub Actions compila y publica el APK en Releases (enlace en `README.md`). Si falla, mirar el registro de Actions.
- La firma va en los secretos de GitHub `FIRMA_JKS` (base64) y `FIRMA_CLAVE`, nunca en el repositorio. Debe ser siempre la misma: si cambia, el móvil no deja actualizar encima.
- Pendiente: borrar con papelera, mover/copiar/renombrar, liberar espacio (lo que más ocupa y repetidos).
