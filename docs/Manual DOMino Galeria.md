# Manual de DOMino Galería (para Claudio)

DOMino Galería es una app Android de The DOM Studio Web que busca fotos dentro del teléfono por lo que contienen (personas, vehículos, texto como "X5"), sin internet y sin costo de IA. Sirve para armar videos o presentaciones a partir de miles de fotos mezcladas.

## Pestañas
- **Indexar:** recorre toda la galería (cámara, WhatsApp, ediciones). Descarta las fotos repetidas (idénticas y casi idénticas) y analiza cada una en el teléfono: caras, objetos y texto leído. Muestra avance y tiempo estimado. Corre en segundo plano con una notificación, con la pantalla apagada, y se puede pausar y continuar. Conviene dejarlo con el cargador.
- **Buscar:** por palabras (X5, carpeta, auto, moto, persona, llaves, contrato...) o con atajos: Logo X5, Persona + vehículo, Persona + X5, Persona + carpeta/papel, Dice contrato o plan, Con llaves. Filtros por origen (WhatsApp o cámara), fechas y si ya están marcadas.
- **Marcadas:** cuenta cuántas Entregas y Planes se separaron, deja ver cada grupo y exporta.

## Cómo revisar y marcar
- Tocá una foto para abrirla en pantalla grande: botones **Entrega**, **Plan** o **Descartar**; al marcar pasa a la siguiente. Se puede deslizar con las flechas.
- Mantené apretada una foto (o usá **Seleccionar**) para elegir varias y marcarlas juntas.
- Criterio de X5: **Entrega** = persona con llaves en la mano o persona junto a un auto o una moto (nunca vehículos solos ni fotos de stock). **Plan** = persona con la carpeta blanca de contrato con el logo de X5.

## Ordenar (repetidas, papelera y carpetas)
- **Fotos repetidas**: el botón muestra todos los grupos. Cada grupo tiene un original ("SE QUEDA") y sus copias, marcadas por defecto con ✕. Se puede tocar una copia para desmarcarla, marcar o desmarcar todas, y "Eliminar" mueve las marcadas a la papelera. Solo se elimina una repetida si su original sigue existiendo; las que no tienen original localizado no se tocan.
- **Papelera**: Eliminar nunca borra de inmediato. Las fotos van a una papelera de la app (carpeta oculta `.DOMino-Papelera`) y se pueden restaurar a su lugar de origen. "Vaciar papelera" o "Borrar para siempre" las elimina de verdad y no se puede deshacer.
- **Eliminar una foto**: desde el visor o seleccionando varias, botón Eliminar (va a la papelera).
- **Carpetas**: seleccionás fotos, tocás Carpeta, escribís el nombre. Las fotos se mueven (no se copian) a Imágenes/DOMino-Carpetas/<nombre>. Las carpetas creadas aparecen en Ordenar.
- Limitación: todavía no se puede cambiar cuál es el original de un grupo.

## Exportar
"Copiar a carpeta y armar zip" copia las Entregas y los Planes a **Imágenes / X5-Historia** (subcarpetas Entregas y Planes, con la fecha al inicio del nombre), crea un `indice.csv` y un .zip en Imágenes. Los originales no se tocan.

## Permisos y límites
- Pide "Acceso a todos los archivos" (una sola vez) y permiso de notificaciones.
- Todo el análisis ocurre en el teléfono; las fotos no se suben a ningún lado.
- Detecta objetos genéricos y lee texto; no identifica personas. Lo fino (llaves en la mano, carpeta sin logo legible) puede no detectarse: por eso se revisa a ojo en la grilla.
- Tiempo de indexado: depende del teléfono; para unas 30.000 fotos puede llevar varias horas la primera vez. Después solo suma las nuevas.
