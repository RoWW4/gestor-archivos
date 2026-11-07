# Archivos

Gestor de archivos para Android, sencillo, **sin publicidad y sin acceso a internet**.

- Busca por nombre (sin importar tildes ni mayúsculas).
- Filtra por tipo (imágenes, vídeos, audio, documentos, APK, comprimidos), por fecha y por ubicación (Descargas, Cámara, Capturas, WhatsApp…, o cualquier carpeta).
- Ordena por fecha, nombre o tamaño.
- Abre, «abre con» y comparte. Mantén pulsado un archivo para ver sus datos.

## Descargar

Última versión: <https://github.com/RoWW4/gestor-archivos/releases/latest/download/gestor-archivos.apk>

La primera vez Android pedirá permitir «instalar apps desconocidas» y, al abrirla, el permiso de «acceso a todos los archivos».

## Cómo se crea

Cada cambio en `main` hace que GitHub Actions compile el APK (`.github/workflows/apk.yml`) y lo publique en *Releases*.
Código en Kotlin con Jetpack Compose (`app/src/main/java/app/gestorarchivos`).
