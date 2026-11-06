# DOMino Galería

App Android para **indexar y buscar las fotos del teléfono**: recorre la galería (cámara, WhatsApp, ediciones), descarta las repetidas y, con ML Kit de Google corriendo en el propio teléfono (sin internet y sin costo), anota en cada foto las caras, los objetos y el texto que se lee (por ejemplo el logo X5 en una carpeta de contrato). Después se busca por palabras, se revisan las candidatas en una grilla y se marcan como **Entrega** o **Plan**; lo marcado se copia a `Imágenes/X5-Historia` y se arma un zip. Los originales no se tocan.

## Descargar la app
Cada cambio en `main` compila la APK automáticamente (GitHub Actions) y la publica en **Releases → "DOMino Galería — última versión"**:
https://github.com/TheDomStudioWeb/domino-galeria/releases/download/apk/DOMino-Galeria.apk

Descargala desde el teléfono, permití "instalar apps de origen desconocido" e instalala. La primera vez pide el permiso "Acceso a todos los archivos".

## Estructura
| Carpeta | Qué es |
|---|---|
| `www/` | Pantallas (HTML/CSS/JS). Van dentro de la APK. |
| `android/` | Proyecto Android generado por Capacitor 8 + el código nativo (`GaleriaPlugin`, `IndexService`, `GaleriaDb`). |
| `docs/` | Manual para Claudio. |
| `.github/workflows/` | Compilación de la APK. |

No hace falta Android Studio: la APK se compila en GitHub.

Desarrollado por The DOM Studio Web · WhatsApp 261-3431216 · thedomstudioweb@gmail.com
