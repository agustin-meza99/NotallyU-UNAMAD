# Notally — Documento de diseño V1

**Alcance:** Hoy, Materias, Detalle/Editor de tarea, Ajustes. Android nativo, Kotlin + Jetpack Compose, Material 3, español.
**Carácter:** Material 3 con contención editorial tipo Notion: papel cálido, tinta, un acento funcional, contenido antes que decoración.
**Autoridad:** este documento manda sobre cualquier regla genérica de estilo. Cualquier desviación se anota aquí antes de implementarse.
**Vigencia técnica:** verificada el 2026-10-25 contra `material3 1.4.0`, que es la versión que resuelve el BOM `2026.02.01` del proyecto.

---

## 0. Decisiones cerradas (no re-discutir en implementación)

1. Un solo FAB en Hoy ("Nueva tarea"). Sin menú de FAB en V1.
2. Sin sombras difusas. La jerarquía se expresa con superficie tonal, borde de 1 dp y espacio.
3. Serif solo en títulos de pantalla. Cuerpo, controles y contenido editable en sans.
4. El acento de interacción es el azul tinta (`tertiary`). El color de materia nunca colorea botones ni texto de cuerpo.
5. Las tarjetas de materia no son clicables para "ver detalle": abren la hoja "Editar materia". No hay pantalla de materia en V1.
6. Los estados de carga de Room no inventan latencia: sin skeletons animados.
7. Ningún dato mostrado se calcula fuera de la base local (conteos reales).
8. La fecha se escribe en español con inicial mayúscula; la hora sigue el ajuste del sistema (12/24 h).

---

## 1. Principios y filtro anti-slop

### Principios
- **Contenido primero:** el título de la tarea es el elemento tipográfico más fuerte de su fila. No hay adornos a su alrededor.
- **Aire con ritmo:** 20 dp de margen lateral y bloques separados por 24–32 dp; dentro de una fila, compacto.
- **Color con significado:** el color de materia identifica, no decora; el azul indica interacción; el rojo solo marca lo que exige atención (vencimiento y prioridad alta).
- **Una acción primaria por pantalla.** Nunca dos botones compitiendo.
- **La app no celebra:** informa. "3 de 7 completadas", no "¡Vas genial!".

### Prohibiciones (cada una con su razón)

| Prohibido | Por qué |
|---|---|
| Gradientes morados/azules o neón | Son la firma visual de plantilla de IA; rompen la base papel/tinta y no aportan jerarquía. |
| Glassmorphism, blur decorativo | Baja el contraste del texto, cuesta GPU y no comunica estado. |
| Tarjetas con sombra difusa grande | La elevación M3 se expresa con tono; una sombra de 8+ dp crea niebla y un look de dashboard genérico. |
| Emojis como iconos | Métricas inconsistentes entre dispositivos, TalkBack los lee como texto y no pertenecen a la iconografía del sistema. |
| Métricas inventadas ("+32% productividad") | No existen en el dominio; toda cifra debe salir de Room. |
| Ilustraciones genéricas en estados vacíos | Pesan, no explican y retrasan la acción; texto + botón resuelve mejor. |
| Skeletons con shimmer infinito | La lectura local es casi instantánea; simular espera erosiona la confianza y distrae. |
| Badges "Beta", "AI-powered", "Nuevo" | No hay funciones beta ni IA; el badge es ruido publicitario. |
| Hero centrado con dos CTAs | Duplica decisión y rompe la lectura de agenda. |
| Cuadrícula de tarjetas idénticas | Aplana la jerarquía entre vencido, para hoy y sin hora. |
| Animación de entrada en cada fila | Fatiga visual y retrasa la lectura; solo se anima el cambio de estado. |
| Bordes redondeados > 28 dp en contenido | Convierte la interfaz en juguete y reduce el área útil; el radio debe corresponder al tamaño del componente. |
| Blanco puro y negro puro | Brillos y halos; el papel cálido y la tinta reducen fatiga en lectura larga. |
| Segundo color de acento compitiendo con materias | El acento debe ser escaso para seguir siendo acento. |
| Texto "Haz clic aquí", "Ups", "¡Genial!" | No nombra la acción ni el problema; tono de relleno. |
| Datos de ejemplo en la app publicada | Confunde y simula funciones que no existen. |

---

## 2. Color

### 2.1 Reglas
- Tokens en un solo archivo (`Color.kt` / `NotallyColors`); prohibido hex suelto en pantallas.
- `background` = `surface`; `onBackground` = `onSurface`.
- El estado seleccionado nunca se comunica solo con `primaryContainer` (contraste 1.25:1 contra superficie): siempre añade borde, texto o indicador.
- Los colores de materia se usan en: punto de 6 dp, barra de 3 dp, relleno de progreso y contenedor de la hoja de edición. Nunca en botones, FAB, nav ni texto largo.
- Modo claro y oscuro son paletas propias; no se derivan por inversión automática.

### 2.2 Paleta clara

| Rol M3 | Hex | Uso |
|---|---|---|
| primary | `#262320` | FAB, checkbox marcado, acción primaria, texto de máximo énfasis |
| onPrimary | `#FCFAF6` | Contenido sobre primary |
| primaryContainer | `#E7E1D6` | Indicador de nav, contenedor tonal seleccionado |
| onPrimaryContainer | `#262320` | Texto/icono sobre primaryContainer |
| secondary | `#5E594F` | Metadatos de énfasis, iconos secundarios |
| onSecondary | `#FFFFFF` | Contenido sobre secondary |
| secondaryContainer | `#EAE4D9` | Acciones secundarias, fondo de swipe "Mañana" |
| onSecondaryContainer | `#38342D` | Contenido sobre secondaryContainer |
| tertiary | `#2B5773` | Acento funcional: foco, enlaces, cursor, acción de swipe "Completar" |
| onTertiary | `#FFFFFF` | Contenido sobre tertiary |
| tertiaryContainer | `#D6E5EF` | Fondo de swipe completar, selección de filtro |
| onTertiaryContainer | `#103246` | Contenido sobre tertiaryContainer |
| error | `#A33A2C` | Vencimiento, prioridad alta, acción destructiva |
| onError | `#FFFFFF` | Contenido sobre error |
| errorContainer | `#F7DDD8` | Banner de error, contenedor destructivo |
| onErrorContainer | `#4A130B` | Texto sobre errorContainer |
| surface | `#FBFAF7` | Fondo de pantalla (papel) |
| onSurface | `#1F1C18` | Texto principal (tinta) |
| surfaceDim | `#E6E1D8` | Fondo atenuado (raro) |
| surfaceBright | `#FFFDF9` | Superficie más clara que surface |
| surfaceContainerLowest | `#FDFBF8` | Entradas y diálogos |
| surfaceContainerLow | `#F5F2ED` | Fila presionada, fondo de sección |
| surfaceContainer | `#F0ECE5` | Contenedor por defecto |
| surfaceContainerHigh | `#E9E4DC` | Toolbar flotante |
| surfaceContainerHighest | `#E2DCD2` | Estados elevados puntuales |
| surfaceVariant | `#E8E3DA` | Relleno de campos y chips sin seleccionar |
| onSurfaceVariant | `#5B554C` | Texto secundario, iconos inactivos |
| outline | `#8A8275` | Bordes de controles interactivos, checkbox |
| outlineVariant | `#D7D0C4` | Separadores y bordes de agrupación (decorativo) |
| inverseSurface | `#2E2A25` | Snackbar |
| inverseOnSurface | `#F6F2EA` | Texto de snackbar |
| inversePrimary | `#D6CEC0` | Acción de snackbar |

### 2.3 Paleta oscura

| Rol M3 | Hex | Uso |
|---|---|---|
| primary | `#E4DED3` | Acción primaria, checkbox marcado |
| onPrimary | `#23201C` | Contenido sobre primary |
| primaryContainer | `#3A342B` | Indicador de nav |
| onPrimaryContainer | `#F0EAE0` | Texto/icono sobre primaryContainer |
| secondary | `#C5BEB2` | Metadatos de énfasis |
| onSecondary | `#2A2620` | Contenido sobre secondary |
| secondaryContainer | `#37322A` | Swipe "Mañana" |
| onSecondaryContainer | `#E4DDD2` | Contenido sobre secondaryContainer |
| tertiary | `#A6CBE3` | Acento funcional |
| onTertiary | `#0D2D40` | Contenido sobre tertiary |
| tertiaryContainer | `#1D4157` | Selección de filtro |
| onTertiaryContainer | `#CDE6F5` | Contenido sobre tertiaryContainer |
| error | `#F2B8AD` | Vencimiento, prioridad alta |
| onError | `#5C160C` | Contenido sobre error |
| errorContainer | `#6E2418` | Banner de error |
| onErrorContainer | `#F9DCD6` | Texto sobre errorContainer |
| surface | `#151310` | Fondo (tinta cálida, no negra) |
| onSurface | `#ECE7DE` | Texto principal (papel) |
| surfaceDim | `#110F0D` | Fondo atenuado |
| surfaceBright | `#3A342C` | Superficie más clara |
| surfaceContainerLowest | `#100E0C` | Entradas y diálogos |
| surfaceContainerLow | `#1C1916` | Fila presionada |
| surfaceContainer | `#211E1A` | Contenedor por defecto |
| surfaceContainerHigh | `#29251F` | Toolbar flotante |
| surfaceContainerHighest | `#322D26` | Estados elevados puntuales |
| surfaceVariant | `#3A352E` | Relleno de campos |
| onSurfaceVariant | `#B3AB9F` | Texto secundario |
| outline | `#8B8377` | Bordes interactivos |
| outlineVariant | `#4A443C` | Separadores |
| inverseSurface | `#ECE7DE` | Snackbar |
| inverseOnSurface | `#322E28` | Texto de snackbar |
| inversePrimary | `#4A4339` | Acción de snackbar |

### 2.4 Contraste (WCAG 2.2)

Fórmula usada: `(L_claro + 0.05) / (L_oscuro + 0.05)`, con luminancia relativa sRGB (canal ≤ 0.04045 → /12.92; si no → ((c+0.055)/1.055)^2.4), calculada sobre los hex exactos de arriba. Texto normal exige 4.5:1; texto grande 3:1; bordes/controles 3:1.

| Par | Claro | Oscuro | Requisito | Resultado |
|---|---|---|---|---|
| onSurface / surface | 16.26 | 15.06 | 4.5 | Pasa |
| onSurfaceVariant / surface | 7.06 | 8.16 | 4.5 | Pasa |
| onSurfaceVariant / surfaceContainerLow | 6.60 | 7.70 | 4.5 | Pasa |
| onSurfaceVariant / surfaceVariant | 5.77 | 5.35 | 4.5 | Pasa |
| onPrimary / primary | 14.99 | 12.12 | 4.5 | Pasa |
| onPrimaryContainer / primaryContainer | 12.01 | 10.29 | 4.5 | Pasa |
| onSecondary / secondary | 6.96 | 8.15 | 4.5 | Pasa |
| onSecondaryContainer / secondaryContainer | 9.78 | 9.43 | 4.5 | Pasa |
| onTertiary / tertiary | 7.74 | 8.37 | 4.5 | Pasa |
| onTertiaryContainer / tertiaryContainer | 10.41 | 8.34 | 4.5 | Pasa |
| error / surface | 6.29 | 10.80 | 4.5 | Pasa |
| onError / error | 6.56 | 7.77 | 4.5 | Pasa |
| onErrorContainer / errorContainer | 11.73 | 8.42 | 4.5 | Pasa |
| tertiary / surface | 7.41 | 10.84 | 4.5 | Pasa |
| outline / surface | 3.64 | 4.95 | 3.0 | Pasa |
| outlineVariant / surface | 1.47 | 1.93 | — | Solo decorativo; nunca delimita un control |
| inverseOnSurface / inverseSurface | 12.76 | 10.95 | 4.5 | Pasa |
| inversePrimary / inverseSurface | 9.12 | 7.92 | 4.5 | Pasa |

### 2.5 Acentos de materia

Seis swatches fijos, con variante clara y oscura. El texto sobre el contenedor del swatch (chip de selección en "Editar materia") también cumple AA.

| Token | Nombre UI | Claro | Oscuro | Contenedor claro | Contenedor oscuro | Texto claro | Texto oscuro | Sobre surface claro/oscuro |
|---|---|---|---|---|---|---|---|---|
| subjectIndigo | Índigo | `#3A5A9E` | `#AFC3EE` | `#E9ECF5` | `#222B3F` | 5.67 | 7.99 | 6.42 / 10.48 |
| subjectForest | Bosque | `#2F6A4E` | `#9FC9B2` | `#E6EFE8` | `#1D2C24` | 5.43 | 7.98 | 6.11 / 10.13 |
| subjectTerracotta | Terracota | `#99482E` | `#E7AE97` | `#F4E8E2` | `#33231C` | 5.27 | 7.80 | 6.06 / 9.62 |
| subjectOchre | Ocre | `#7A5E14` | `#DCC27A` | `#F2ECDA` | `#2E2716` | 5.17 | 8.49 | 5.85 / 10.63 |
| subjectPlum | Ciruela | `#7B3F6D` | `#E0A9D1` | `#F2E7EF` | `#2F2130` | 6.29 | 7.79 | 7.25 / 9.51 |
| subjectPetrol | Petróleo | `#1D5F6A` | `#96CDD6` | `#E3EEEE` | `#182B2E` | 6.12 | 8.43 | 6.94 / 10.60 |

Reglas:
- Asignación por defecto: orden de la lista, sin relación semántica con el nombre de la materia. El swatch es una elección del usuario, no una categoría.
- El nombre del color ("Índigo") es la etiqueta accesible del swatch; no se muestra texto de color en la tarjeta. Los nombres son de swatch, no de categoría: el color no codifica el tipo de materia.
- La barra de progreso usa el color de materia sobre `outlineVariant`; no requiere AA (no es texto), pero el porcentaje nunca se comunica solo con color: siempre hay "3 de 7 completadas".

### 2.6 Color dinámico
- Desactivado por defecto. La identidad es la paleta de este documento.
- Si el usuario lo activa, se usa el esquema dinámico completo del sistema para roles M3 (incluye superficies). Los colores de materia no cambian. No se mezcla paleta propia con acentos dinámicos: el contraste se delega al esquema del sistema.
- El interruptor no debe reiniciar la pantalla ni perder scroll.

---

## 3. Tipografía

### 3.1 Fuentes y justificación
- **Display y títulos editoriales:** `FontFamily.Serif`. En AOSP resuelve a Noto Serif; no requiere assets ni red.
- **Cuerpo, controles y contenido editable:** `FontFamily.SansSerif` (Roboto en AOSP).
- No se descargan fuentes ni se empaquetan .ttf en V1. Motivo: offline garantizado, cero peso extra, y la app no depende de licencias ni de red en el primer arranque.
- **Riesgo asumido:** algunos fabricantes sustituyen la familia `serif` y cambian métricas. Por eso el serif se limita a títulos de 1–2 líneas, sin tracking negativo agresivo y con line-height generoso.

### 3.2 Escala y mapeo exacto a `Typography`

| Slot M3 | Familia | Tamaño | Line-height | Tracking | Peso | Uso |
|---|---|---|---|---|---|---|
| displayLarge | Serif | 56 sp | 64 sp | -0.50 sp | 400 | Reservado; no usar en V1 |
| displayMedium | Serif | 44 sp | 52 sp | -0.25 sp | 400 | Reservado; no usar en V1 |
| displaySmall | Serif | 36 sp | 44 sp | -0.25 sp | 400 | Título de pantalla ("Hoy", "Materias", "Ajustes") |
| headlineLarge | Serif | 32 sp | 40 sp | -0.20 sp | 400 | Reservado; no usar en V1 |
| headlineMedium | Serif | 28 sp | 36 sp | -0.20 sp | 400 | Reservado; no usar en V1 |
| headlineSmall | Sans | 24 sp | 32 sp | 0 sp | 500 | Título de diálogo y de estado vacío |
| titleLarge | Sans | 22 sp | 28 sp | 0 sp | 500 | Título de bottom sheet |
| titleMedium | Sans | 16 sp | 24 sp | 0.10 sp | 500 | Título de tarea y nombre de materia |
| titleSmall | Sans | 14 sp | 20 sp | 0.10 sp | 500 | Etiqueta de propiedad, botón de texto |
| bodyLarge | Sans | 16 sp | 24 sp | 0.10 sp | 400 | Texto de apoyo y diálogos |
| bodyMedium | Sans | 14 sp | 22 sp | 0.10 sp | 400 | Descripción secundaria |
| bodySmall | Sans | 12 sp | 16 sp | 0.20 sp | 400 | Metadatos de fila, conteos |
| labelLarge | Sans | 14 sp | 20 sp | 0.10 sp | 500 | Botones |
| labelMedium | Sans | 12 sp | 16 sp | 0.40 sp | 500 | Chips |
| labelSmall | Sans | 11 sp | 16 sp | 0.60 sp | 500 | Eyebrow de fecha y encabezados de sección (se aplica uppercase en uso) |

Estilos extra (fuera de `Typography`, en `NotallyType`):

| Token | Familia | Tamaño | Line-height | Tracking | Peso | Uso |
|---|---|---|---|---|---|---|
| editorTitle | Sans | 28 sp | 36 sp | -0.20 sp | 600 | Campo de título del editor |
| editorBody | Sans | 17 sp | 28 sp | 0 sp | 400 | Notas y texto editable largo |
| numeric | Sans | 12 sp | 16 sp | 0.20 sp | 500 | Conteos y progreso (`tnum` activado) |

### 3.3 Reglas de uso
- Máximo **un** estilo serif visible por pantalla (el título). El serif no se usa en botones, chips, metadatos ni contenido editable.
- El uppercase se aplica solo a `labelSmall` (fecha y secciones); nunca a títulos ni botones.
- `editorTitle` y `editorBody` usan `BasicTextField` sin caja: placeholder en `onSurfaceVariant`, cursor y selección en `tertiary`.
- Ancho de lectura máximo 680 dp; en pantallas anchas el contenido se centra.
- Máximo 3 líneas en la lista, con elipsis; el `contentDescription` de TalkBack incluye el título completo. En el editor, sin límite de líneas.
- Los conteos ("3 de 7") usan `numeric` para no bailar al actualizarse.

---

## 4. Espaciado, radios y elevación

### 4.1 Espaciado (múltiplos de 4)
| Token | dp | Uso |
|---|---|---|
| space1 | 4 | Entre título y metadatos; separación mínima |
| space2 | 8 | Icono–texto, meta interna, entre checkbox y contenido |
| space3 | 12 | Padding interno de tarjeta compacta, gap entre tarjetas |
| space4 | 16 | Padding interno estándar, gap entre propiedades |
| space5 | 24 | Entre secciones, margen superior de encabezado |
| space6 | 32 | Entre bloques mayores (título editorial → contenido) |
| space7 | 48 | Reserva superior en estados vacíos; target táctil |
- Margen lateral de pantalla: 20 dp (24 dp en ≥ 600 dp).
- Padding inferior de listas: 96 dp (barra de navegación + FAB) o 24 dp si no hay FAB.
- No se usa `Spacer` para separar secciones desplazables: va en `contentPadding`.

### 4.2 Radios
| Token | dp | Uso |
|---|---|---|
| shapeXS | 4 | Badges y detalles mínimos |
| shapeS | 8 | Contenedores internos pequeños |
| shapeM | 12 | Snackbar, campos compactos, fondo de swipe |
| shapeL | 16 | Tarjetas, toolbar flotante, FAB |
| shapeXL | 28 | Bottom sheets, diálogos |
| shapeFull | 50 % | Checkbox, chips, puntos, indicador de nav |

- El radio debe crecer con el tamaño del componente; nunca 28 dp en una fila de lista.
- Sin morphing de formas al presionar: en 1.4.0 no hay API pública y añade ruido.

### 4.3 Elevación y bordes
| Nivel | Uso | Sombra | Fondo |
|---|---|---|---|
| 0 | Pantallas, filas, tarjetas, secciones | 0 dp | surface / surfaceContainerLow |
| 1 | Fila presionada, selección | 0 dp | surfaceContainerLow (borde opcional) |
| 2 | FAB | 2 dp | primary |
| 2 | Toolbar flotante | 0 dp | surfaceContainerHigh + borde outlineVariant |
| 2 | Snackbar | 3 dp | inverseSurface |
| 3 | Diálogo | 3 dp | surfaceContainerLowest |
| — | Bottom sheet | 0 dp | surfaceContainerLow + scrim 32 % |
- Regla: **sombra solo si el componente se superpone al contenido**, y el borde sustituye a la sombra cuando el contraste tonal basta. Nunca borde + sombra difusa a la vez. Una tarjeta dentro del flujo nunca lleva sombra.
- Borde de 1 dp `outlineVariant` para agrupar; 1 dp `outline` para delimitar un control interactivo.
- Altura táctil mínima 48×48 dp en todo control; el checkbox dibuja 20 dp pero su target es 48 dp.

---

## 5. Motion

Se permiten **tres patrones**. Las únicas excepciones son la rotación del chevron (180°, 180 ms) y el fade del FAB (150 ms). Todo lo demás es cambio instantáneo de color.

Constantes propias (los valores de `MotionScheme` expressive de material3 están internal en 1.4.0; se copian como constantes de la app):

| Constante | Spec |
|---|---|
| spatialDefault | `spring(dampingRatio = 0.8f, stiffness = 380f)` |
| spatialFast | `spring(dampingRatio = 0.6f, stiffness = 800f)` |
| effectsDefault | `spring(dampingRatio = 1.0f, stiffness = 1600f)` |
| effectsFast | `spring(dampingRatio = 1.0f, stiffness = 3800f)` |

| Patrón | Interacción | Spec | Referencia |
|---|---|---|---|
| 1. Transición de contenido | Abrir editor / volver; cambio de pestaña | Fade-through 180 ms (`FastOutSlowInEasing`), sin slide en editor; en tabs, fade 120 ms | Debe sentirse inmediato; el editor no "vuela" desde abajo |
| 2. Expansión espacial | Expandir "Completadas", revelar subtareas, abrir hoja de FAB si existiera | `spatialDefault` sobre tamaño + fade 120 ms | ~300–400 ms hasta reposo; sin rebote en contenedores |
| 3. Confirmación de estado | Marcar tarea/subtarea, seleccionar chip, check del checkbox | color 120 ms (`effectsDefault`); trazo del check 160 ms; escala del check 0.9→1.0 con `spatialFast` solo en el icono | El único rebote permitido, y solo en el check |

Reglas:
- Sin animaciones de entrada por fila, sin parallax, sin stagger, sin animación de fondo.
- La lista puede animar reubicación al cambiar de sección con `animateItem` 220 ms; no animar el alta inicial.
- Se respeta la escala de animación del sistema (`MotionDurationScale` / "Quitar animaciones"): si es 0, todo es instantáneo. No se fuerza duración.
- Mostrar/ocultar el FAB al hacer scroll: fade + escala 0.9→1 en 150 ms (patrón 2); se detiene si el scroll cambia de dirección.
- Cambio de tema: sin crossfade largo; 150 ms como máximo.
- Haptics: solo al completar (una vez) y al cruzar el umbral de swipe. Nunca en cada tick, scroll o apertura.

---

## 6. Navegación y composición

- **Barra inferior:** `ShortNavigationBar` (público y estable en 1.4.0) con 3 destinos: Hoy, Materias, Ajustes. Icono arriba, etiqueta siempre visible. Altura 64 dp + inset de navegación. Fallback si el componente falla en QA: `NavigationBar`.
- Colores de la barra: contenedor `surface`; indicador `primaryContainer`; icono seleccionado `onPrimaryContainer`; etiqueta seleccionada `onSurface`; inactivos `onSurfaceVariant`. Sin badges.
- El editor es una ruta completa sin barra inferior.
- Insets: edge-to-edge; `Scaffold` con `safeDrawing`; el editor aplica `imePadding` a la toolbar. Barras del sistema transparentes con iconos oscuros en tema claro y claros en oscuro.
- Teléfono: una columna. ≥ 600 dp: mismo layout centrado a 720 dp; no se añade rail en V1.
- Back: el editor guarda antes de salir; no se intercepta el gesto predictivo (sin `BackHandler` que bloquee).

---

## 7. Componentes

### 7.1 Encabezado editorial de Hoy
**Anatomía**
- Eyebrow: `labelSmall` uppercase, `onSurfaceVariant`, "DOMINGO, 25 DE OCTUBRE".
- Título: `displaySmall` serif, `onSurface`, "Hoy".
- Resumen: `bodySmall` + `numeric`, `onSurfaceVariant`, "3 de 7 completadas" (solo si hay tareas).
- Sin iconos, sin avatar, sin campana.

**Medidas:** top 24 dp desde el inset; eyebrow → título 4 dp; título → resumen 8 dp; bloque → primera sección 32 dp.

**Estados:** sin tareas → no se muestra resumen. Con tareas → el resumen se actualiza en el sitio, sin animación de conteo.

**Micro-interacción:** ninguna. El encabezado no es interactivo.

---

### 7.2 TaskRow

**Anatomía (izquierda → derecha)**
1. Checkbox circular: 20 dp visual (borde 2 dp `outline`; marcado: relleno `primary` + check `onPrimary`), target 48×48.
2. Separación 4 dp.
3. Columna de prioridad: 2 dp de ancho reservados siempre (evita jitter), radio full, alineada al inicio del título. Alta = `error` y 20 dp de alto; Media = `outline` y 12 dp; Baja y tareas hechas = transparente. La altura distingue alta de media sin depender del color.
4. Columna de texto (peso 1): título `titleMedium`; 4 dp; meta `bodySmall` = punto de materia 6 dp + nombre + " · " + fecha.
5. Sin chevron. La fila entera abre el editor.

**Medidas:** alto mínimo 56 dp (padding vertical 12), padding horizontal 0; el texto empieza a 8 dp de la barra; gap entre filas 4 dp; sin divisores.

**Meta**
- Materia: "Cálculo diferencial" (una línea a 1.0; hasta 2 con fuente grande).
- Fecha: "Hoy", "Mañana", "Vie 31 oct", "31 oct 2026".
- Sin materia: "Sin materia". Sin fecha: "Sin fecha".
- Vencida: "Atrasada · 2 días" en `error` (la palabra y los días, no solo el color). "Atrasada · ayer" para 1 día.

**Estados**
| Estado | Cambio visual |
|---|---|
| default | Fondo transparente |
| pressed | Ripple estándar M3 + fondo `surfaceContainerLow` 120 ms, sin cambio de tamaño |
| focused (teclado) | Anillo de 2 dp `tertiary` con offset 2 dp, radio 12 |
| disabled | No existe: una tarea nunca se deshabilita |
| done | Título tachado y `onSurfaceVariant`; checkbox `primary`; meta al 100 % (nunca opacidad global); barra de prioridad transparente (conserva el hueco); sin "Atrasada" |
| overdue | Fecha en `error` + texto "Atrasada"; fondo normal, sin rojo de fila |
| swapping | Fondo de acción `tertiaryContainer`/`secondaryContainer`, radio 12, icono 20 + etiqueta 12 |

**Micro-interacciones**
- Tap en checkbox: check 160 ms, fondo 120 ms, haptic ligero solo al completar. No reordena de inmediato: la fila se queda con tachado y aparece snackbar "Tarea completada · Deshacer" (5 s). Al cambiar de sección, `animateItem` 220 ms.
- Tap en la fila: abre editor con fade 180 ms.
- Swipe end→start: "Completar" (`tertiaryContainer`). Swipe start→end: "Mañana" (`secondaryContainer`). Umbral 45 % del ancho; haptic al cruzar; al soltar ejecuta y vuelve a `Settled` (no se expulsa la fila).
- Acciones equivalentes en el menú de accesibilidad de la fila: Completar, Posponer a mañana, Eliminar. El gesto nunca es la única vía.

---

### 7.3 Secciones de Hoy
- `labelSmall` uppercase, `onSurfaceVariant`, 11 sp, tracking 0.6; 8 dp antes de la primera fila, 24 dp respecto de la sección anterior.
- Secciones de Hoy, en orden: "Atrasadas" (solo si hay), "Para hoy", "Sin hora", "Próximas" (solo si hay), "Completadas".
- Atrasadas, Para hoy, Sin hora y Próximas muestran solo pendientes; Completadas muestra hechas.
- Orden: Atrasadas por fecha ascendente (la más antigua primero); Para hoy por hora ascendente; Sin hora por fecha de creación; Próximas por fecha ascendente; Completadas por fecha descendente. Desempate: Alta > Media > Baja y luego título.
- "Próximas" evita que una tarea con entrega futura desaparezca: lista todas las pendientes con fecha posterior a hoy, orden ascendente, colapsada por defecto y con contador. No se subdivide por fecha; cada fila muestra su fecha. Se expande con el mismo patrón que "Completadas". Se abre por defecto si no hay tareas en Atrasadas, Para hoy ni Sin hora.
- El resumen "3 de 7 completadas" cuenta solo el día: Atrasadas + Para hoy + Sin hora. "Próximas" queda fuera del conteo.
- "Completadas" es colapsable: chevron de 20 dp a la derecha, rotación 180° en 180 ms; contenido con `animateContentSize` (`spatialDefault`).
- Por defecto colapsada, salvo que no quede ninguna tarea pendiente en la agenda de hoy: entonces se abre para no dejar la pantalla vacía.
- La sección "Completadas" incluye solo las tareas de la agenda de hoy; el filtro "Completadas" muestra todo el historial.
- Contador en la etiqueta solo en Completadas y Próximas: "COMPLETADAS · 4", "PRÓXIMAS · 8".

---

### 7.4 SubjectCard (Materias)

**Anatomía**
- Contenedor `surface` con borde 1 dp `outlineVariant`, radio 16, padding 16. Sin sombra.
- Fila superior: barra vertical de materia 3×28 dp (radio full) + 12 dp + nombre `titleMedium` (máx. 2 líneas).
- Línea 2: `bodySmall` "7 tareas · 3 hechas".
- Línea 3: barra de progreso de 2 dp (track `outlineVariant`, fill color de materia, radio full) + 8 dp + "3/7" en `numeric`.
- Sin flecha, sin avatar, sin iconos por materia.

**Estados**
| Estado | Cambio visual |
|---|---|
| default | Borde `outlineVariant` |
| pressed | Fondo `surfaceContainerLow`, borde `outline`, 120 ms |
| focused | Anillo 2 dp `tertiary`, offset 2 dp |
| disabled | No aplica (siempre editable) |
| sin tareas | Progreso 0 %, texto "Sin tareas"; la barra se mantiene vacía |
| completo | Texto "7 de 7 completadas"; sin celebración ni color especial |

**Micro-interacción:** tap abre `ModalBottomSheet` "Editar materia" (nombre, 6 swatches, "Eliminar materia" en `error`). Sin animación de escala.

**Materias — encabezado:** título `displaySmall` "Materias"; subtítulo `bodySmall` "4 materias · 12 pendientes"; línea "5 entregas esta semana" (conteo real, afectado por el inicio de semana). Acción "Nueva materia" como `IconButton` de 48 dp con `+` y `contentDescription`, alineada al título; se oculta cuando el estado vacío ya ofrece esa acción. Orden de tarjetas: fecha de creación ascendente; sin reordenar en V1.

---

### 7.5 Editor / Detalle de tarea

**Anatomía**
- Barra superior de 56 dp: back 48 dp (`contentDescription` "Volver"), sin título; overflow 48 dp con "Eliminar tarea" (abre el diálogo de confirmación).
- Título editable: `editorTitle`, sin caja, placeholder "Título de la tarea"; crece con el contenido, sin límite de líneas.
- Propiedades (4 filas de 48 dp mínimo): Materia, Entrega, Prioridad, Estado. Etiqueta 96 dp `titleSmall` `onSurfaceVariant`; valor `titleSmall` `onSurface`; chevron 16 dp al 40 %. Sin divisores; gap 4 dp.
- Notas: `editorBody`, sin caja, placeholder "Notas", altura mínima 120 dp, crece.
- Subtareas (ver 7.6).
- Toolbar flotante inferior (ver abajo).

**Estados**
| Estado | Cambio visual |
|---|---|
| default | Sin cajas ni bordes; todo el ancho |
| field focused | Cursor y selección `tertiary`; la etiqueta de la propiedad no cambia de color |
| property pressed | Fondo `surfaceContainerLow`, radio 8, 120 ms |
| focused (teclado) | Anillo 2 dp `tertiary` |
| guardando | Sin spinner; autosave silencioso |
| error al guardar | Snackbar "No se pudo guardar el cambio." + "Reintentar"; el contenido no se pierde |
| título vacío | Permitido; se guarda como "Sin título" solo si hay notas o subtareas; si no hay nada, al volver se descarta sin diálogo |

**Propiedades:** cada fila abre `ModalBottomSheet` con título y lista de 56 dp: Materia (Sin materia + materias + "Nueva materia"), Entrega (date picker), Prioridad (Alta/Media/Baja), Estado (Pendiente/En curso/Completada). El valor de Prioridad se muestra como texto; si es Alta, además un punto de 6 dp `error`. Estado siempre con texto, nunca solo color.

**Autosave:** debounce 500 ms; flush al salir y al ir a segundo plano; sin botón "Guardar". Máximo una escritura por segundo.

**Toolbar flotante (emulada):** `Surface` de 56 dp, radio 16, `surfaceContainerHigh`, borde `outlineVariant`, márgenes 12 dp, `imePadding`. Visible solo con el teclado abierto. Tres acciones con icono 18 dp + etiqueta: "Subtarea", "Fecha", "Prioridad". Aplican a la tarea: "Subtarea" agrega una fila al final y lleva el foco al campo; "Fecha" y "Prioridad" abren su hoja. No duplica el guardado.

**Límites:** no se impone límite visible; el texto crece y la página hace scroll.

---

### 7.6 Subtareas
- Encabezado: "Subtareas" `titleSmall` + contador "2 de 3" `numeric`; a la derecha, botón de texto "Agregar".
- Fila de 48 dp: checkbox 20 dp (target 48), texto `bodyMedium`, botón de eliminar 24 dp en target 48 (`contentDescription` "Eliminar subtarea").
- Hecha: texto tachado + `onSurfaceVariant`; checkbox `primary`.
- Campo de alta al final: placeholder "Agregar subtarea", `bodyMedium`, sin caja, borde inferior 1 dp `outlineVariant` al enfocar; Enter agrega y mantiene el foco; vacío + Enter cierra el teclado.
- Eliminar no pide confirmación: snackbar "Subtarea eliminada" + "Deshacer" 5 s.
- Sin reordenar en V1 (no drag handle).
- Estados: default, pressed (`surfaceContainerLow`), focused (anillo `tertiary`), done (tachado + check), disabled no existe.

---

### 7.7 Chips de filtro
- En Hoy, 16 dp debajo del resumen: "Todas", "Pendientes", "Completadas". Selección única; el filtro no se persiste entre sesiones.
- `FilterChip` de 32 dp visual (target 48), radio full. Seleccionado: `tertiaryContainer` + `onTertiaryContainer` + check de 16 dp. No seleccionado: transparente + borde 1 dp `outlineVariant`, texto `onSurfaceVariant`. El check ocupa un slot fijo de 16 dp en ambos estados: seleccionar no cambia el ancho.
- Cambio de selección: solo color, 120 ms. Prohibido cambiar altura, padding o escala (evita jitter).
- Los tres chips caben en 360 dp a 1.0; a 200 % se usa un contenedor con scroll horizontal y sin barra visible.
- Semántica: "Pendientes" oculta "Completadas"; "Completadas" muestra una lista plana de completadas (sin secciones de tiempo). "Todas" restaura las secciones.
- Si el filtro no tiene resultados, se usa un estado vacío con acción "Ver todas" (o "Ver pendientes"); no se deja el área en blanco.
- El filtro no altera el conteo del encabezado: "3 de 7 completadas" siempre refleja el día completo.
- Estados del chip: default (borde `outlineVariant`), pressed (`surfaceContainerLow`), focused (anillo 2 dp `tertiary`), selected (`tertiaryContainer` + check); disabled no existe: un filtro sin resultados muestra su estado vacío.

---

### 7.8 Estados vacíos
- Alineados arriba, a 48 dp del encabezado; ancho máximo 360 dp; nunca centrados verticalmente en pantalla.
- Anatomía: título `headlineSmall`; 8 dp; cuerpo `bodyMedium` `onSurfaceVariant`; 16 dp; acción `TextButton` (o `FilledTonalButton` si es la única acción de la pantalla). Sin ilustración, sin icono decorativo.
- Siempre hay una acción real. Cuando el estado vacío es el de la pantalla completa, su CTA es la única acción: el FAB de Hoy y el "+" de Materias se ocultan en ese estado. En estados vacíos de filtro (hay tareas, pero ninguna coincide), el FAB se mantiene y la acción es "Ver todas"/"Ver pendientes".
- Hoy solo muestra estado vacío si no hay ninguna tarea en ninguna sección (ni próximas). Si hay próximas pero nada para hoy, se muestran las próximas sin mensaje intermedio.

---

### 7.9 Diálogo de confirmación y bottom sheets
- Diálogo: `AlertDialog` radio 28, título `headlineSmall`, cuerpo `bodyMedium`, acciones de texto. Foco inicial en "Cancelar". "Eliminar" en `error`. Sin icono de advertencia.
- Estados del diálogo: default (foco en Cancelar), "Eliminar" pressed (`errorContainer`), teclado (acciones accesibles sin taparlas). No se cierra con tap fuera; el gesto o la tecla back equivale a Cancelar.
- Bottom sheet: `ModalBottomSheet`, radio 28 arriba, drag handle visible 32×4, `surfaceContainerLow`, scrim 32 %, sin sombra.
- Estados del sheet: default, arrastrando (el scrim sigue al sheet), expandido. Cierra por swipe, back o tap fuera; no tiene estado disabled.
- Filas de opción: 56 dp, texto `bodyLarge`, control de selección a la izquierda; estado seleccionado con `tertiary`.
- El sheet se cierra al elegir; no requiere botón "Aplicar".

---

### 7.10 FAB y menú de FAB
- **V1:** un solo `FloatingActionButton` de 56 dp, radio 16, `primary`/`onPrimary`, icono `+` de 24 dp, `contentDescription` "Nueva tarea", márgenes 16 dp sobre la barra de navegación, sombra 2 dp. Sin texto extendido (el contexto "Hoy" lo hace obvio).
- Se oculta al hacer scroll hacia abajo y reaparece al subir (solo en Hoy), con fade/scale 150 ms; nunca se mueve con el dedo. También se oculta cuando Hoy no tiene ninguna tarea: en ese caso la única acción es el CTA del estado vacío.
- **Menú de FAB (solo si en V1.1 aparece una segunda acción global):** emulación con overlay propio: 1 acción principal + hasta 3 secundarias en columna sobre el FAB, etiquetas a la izquierda en `labelLarge`, contenedor `surfaceContainerHigh` radio 16. Apertura 180 ms (fade + escala 0.9→1, `spatialFast`), cierre 120 ms; sin stagger; scrim 0; cierra con back, tap fuera y al elegir. No se implementa en V1.
- Estados del FAB: default, pressed (ripple, sin cambio de tamaño), focused (anillo 2 dp `tertiary`); disabled no existe. Menú: cerrado, abierto, ítem pressed/focused; sin estado disabled.

---

### 7.11 Iconografía
- Material Symbols Outlined, 24 dp, peso 400, sin relleno. Se copian como vector XML locales (12 iconos): check, add, calendar, book, settings, more_horiz, arrow_back, delete, flag, checklist, close, chevron_right.
- Motivo: `material3 1.4.0` ya no incluye la librería `material-icons`; añadirla engorda el build y está desaconsejada por Android. Sin red en runtime.
- Color: `onSurfaceVariant` inactivo; `onSurface` activo; `error` solo para eliminar. Tamaño visual 20–24 dp dentro de target 48.

---

### 7.12 Ajustes

**Anatomía**
- Título `displaySmall` "Ajustes"; sin barra superior ni iconos decorativos. Márgenes 20 dp; top 24 dp desde el inset; título → primera sección 32 dp.
- Secciones con eyebrow `labelSmall` uppercase: "APARIENCIA", "AGENDA", "ACERCA DE"; 24 dp entre secciones.
- Filas de 56 dp: etiqueta `bodyLarge` a la izquierda; valor `bodyMedium` `onSurfaceVariant` y control a la derecha. Sin iconos a la izquierda. Los switches tienen target de 48×48 dp aunque su dibujo sea 52×32.
- Separador de 1 dp `outlineVariant` entre filas de una misma sección; ninguno antes de la primera ni después de la última.

**Contenido**
- Apariencia → "Tema" (valor: Sistema / Claro / Oscuro) abre bottom sheet con 3 opciones y check `tertiary`. "Color dinámico" con switch; soporte "Usa el acento de tu fondo de pantalla.". En API 30 el switch queda deshabilitado y el soporte cambia a "Requiere Android 12 o superior.".
- Agenda → "La semana inicia en lunes" con switch, activado por defecto; soporte "Cambia el conteo de entregas de la semana.".
- Acerca de → "Versión 1.0" como texto no interactivo. Sin enlaces falsos ni redes.

**Estados**
| Estado | Cambio visual |
|---|---|
| default | Fondo `surface`, separadores `outlineVariant` |
| pressed | Fondo `surfaceContainerLow`, 120 ms |
| focused | Anillo 2 dp `tertiary`, offset 2 dp |
| switch off | Track `surfaceVariant`, borde 1 dp `outline`, thumb `outline` |
| switch on | Track `primary`, thumb `onPrimary` |
| disabled (API 30) | Etiqueta y switch al 38 %; soporte legible al 100 % |

**Micro-interacción:** el switch conmuta sin animación de tamaño de fila; el thumb usa la animación estándar del componente. Cambiar tema no reinicia la pantalla ni pierde la posición de scroll.

---

### 7.13 Carga, error y reintento
- **Carga:** sin skeleton animado. Si el primer dato de Room tarda más de 150 ms, se muestran 3 filas estáticas con bloques `surfaceContainerLow` (sin shimmer) y se sustituyen al llegar el dato. Antes de 150 ms no se muestra nada para evitar parpadeo.
- **Error sin datos:** bloque alineado arriba con título `headlineSmall` "No se pudieron cargar tus tareas." (o "…tus materias."), cuerpo `bodyMedium` y `TextButton` "Reintentar". Sin ilustración.
- **Error con datos en caché:** banner de 1 línea, fondo `errorContainer`, texto `onErrorContainer`, radio 12, padding 12, con acción "Reintentar"; no tapa el contenido ni se cierra solo.
- **Error al guardar:** snackbar "No se pudo guardar el cambio." + "Reintentar"; el formulario conserva todo lo escrito y el foco.
- **Éxito vacío:** se rige por 7.8; nunca se muestra un estado vacío mientras se está cargando.

---

## 8. Microcopia (español neutro)

**Tono:** segunda persona, imperativo, frase corta, sin exclamaciones, sin "nosotros", sin promesas. Sentence case salvo eyebrow/uppercase de sección. Sin emojis. Sin "fácil", "rápido", "potencia", "inteligente", "genial".

| Clave | Texto exacto |
|---|---|
| nav.hoy / materias / ajustes | "Hoy" / "Materias" / "Ajustes" |
| today.summary | "%1$d de %2$d completadas" |
| today.section.overdue / today / anytime / done | "Atrasadas" / "Para hoy" / "Sin hora" / "Completadas" |
| today.section.upcoming | "Próximas" |
| task.due.today / tomorrow | "Hoy" / "Mañana" |
| task.overdue | "Atrasada · %s" |
| task.noSubject / noDate | "Sin materia" / "Sin fecha" |
| priority.high / medium / low | "Alta" / "Media" / "Baja" |
| state.pending / doing / done | "Pendiente" / "En curso" / "Completada" |
| today.empty.title | "Nada para hoy" |
| today.empty.body | "Agrega la primera tarea del día." |
| today.empty.cta | "Nueva tarea" |
| filter.all / pending / done | "Todas" / "Pendientes" / "Completadas" |
| filter.empty.pending.title | "Nada pendiente" |
| filter.empty.pending.body | "Completaste todas las tareas de hoy." |
| filter.empty.pending.cta | "Ver todas" |
| filter.empty.done.title | "Aún no completaste tareas" |
| filter.empty.done.body | "Marca una tarea como completada y aparecerá aquí." |
| filter.empty.done.cta | "Ver pendientes" |
| subjects.summary | "%1$d materias · %2$d pendientes" |
| subjects.week | "%d entregas esta semana" |
| subject.progress | "%1$d de %2$d completadas" |
| subject.noTasks | "Sin tareas" |
| subjects.empty.title | "Aún no tienes materias" |
| subjects.empty.body | "Crea una materia para agrupar tus tareas y ver cuántas llevas." |
| subjects.empty.cta | "Nueva materia" |
| subjects.new | "Nueva materia" |
| subject.edit.title | "Editar materia" |
| subject.name | "Nombre" |
| subject.color | "Color" |
| subject.delete | "Eliminar materia" |
| editor.title.placeholder | "Título de la tarea" |
| editor.untitled | "Sin título" |
| editor.notes.placeholder | "Notas" |
| editor.property.subject / due / priority / state | "Materia" / "Entrega" / "Prioridad" / "Estado" |
| editor.delete | "Eliminar tarea" |
| editor.subtasks | "Subtareas" |
| editor.subtasks.add | "Agregar" (label visible; `contentDescription` "Agregar subtarea") |
| editor.subtasks.placeholder | "Agregar subtarea" |
| editor.saving.error | "No se pudo guardar el cambio." |
| task.completed | "Tarea completada" |
| task.deleted | "Tarea eliminada" |
| subtask.deleted | "Subtarea eliminada" |
| action.undo / cancel / delete / retry / add | "Deshacer" / "Cancelar" / "Eliminar" / "Reintentar" / "Agregar" |
| swipe.complete / tomorrow | "Completar" / "Mañana" |
| task.postpone | "Posponer a mañana" |
| error.load | "No se pudieron cargar tus tareas." |
| error.loadSubjects | "No se pudieron cargar tus materias." |
| error.refresh | "No se pudieron actualizar los datos." |
| error.retry | "Reintentar" |
| a11y.back | "Volver" |
| a11y.openTask | "Abrir tarea" |
| a11y.newTask | "Nueva tarea" |
| a11y.editSubject | "Editar materia" |
| a11y.deleteSubtask | "Eliminar subtarea" |
| a11y.completeTask | "Completar tarea: %s" |
| a11y.uncompleteTask | "Marcar como pendiente: %s" |
| confirm.deleteTask.title | "¿Eliminar la tarea?" |
| confirm.deleteTask.body | "Se eliminará «%1$s» con sus %2$d subtareas." |
| confirm.deleteTask.bodyNoSub | "Se eliminará «%s»." |
| confirm.deleteSubject.title | "¿Eliminar la materia?" |
| confirm.deleteSubject.body | "«%1$s» tiene %2$d tareas. Quedarán sin materia." |
| confirm.deleteSubject.bodyNoTasks | "Se eliminará «%s»." |
| settings.appearance | "Apariencia" |
| settings.theme / system / light / dark | "Tema" / "Sistema" / "Claro" / "Oscuro" |
| settings.dynamic | "Color dinámico" |
| settings.dynamic.support | "Usa el acento de tu fondo de pantalla." |
| settings.dynamic.unsupported | "Requiere Android 12 o superior." |
| settings.agenda | "Agenda" |
| settings.week | "La semana inicia en lunes" |
| settings.week.support | "Cambia el conteo de entregas de la semana." |
| settings.about | "Acerca de" |
| settings.version | "Versión %s" |

Reglas de formato:
- Fechas: "Hoy", "Mañana", "Ayer", "Vie 31 oct" (día abreviado de 3 letras, sin punto), "31 oct 2026" si es otro año. Español con inicial mayúscula en el eyebrow.
- Hora: 24 h si el sistema está en 24 h; si no, "11:59 p. m." con espacio fino. Nunca "23:59 h".
- Números: sin decimales; conteos con cifras ("3 de 7"), nunca "tres de siete".
- Sin plurales automáticos mal formados. Se implementan con `<plurals>`: `today.summary`, `subjects.summary`, `subjects.week`, `subject.progress`, `confirm.deleteTask.body`, `confirm.deleteSubject.body`. Los textos de la tabla muestran la forma plural.
- El título de una tarea sin texto se guarda como "Sin título" solo si tiene notas o subtareas.

---

## 9. Accesibilidad

### 9.1 Contraste y tamaño
- Texto normal ≥ 4.5:1; texto ≥ 18 sp regular o ≥ 14 sp bold ≥ 3:1. Todos los pares de la sección 2.4 pasan.
- `outlineVariant` no se usa como único límite de un control (1.47:1); para inputs y checkbox se usa `outline` (3.64:1 claro).
- Targets de 48×48 dp; separación mínima entre targets 8 dp. Chips y checkboxes usan `minimumInteractiveComponentSize`.
- No se usa el color como único portador de estado: "Atrasada" lleva texto, "Completada" lleva tachado + check, y la prioridad cambia de altura de barra además de color.

### 9.2 TalkBack (textos exactos)
- **TaskRow (contenedor):** "Entregar informe. Cálculo diferencial. Entrega hoy, 23:59. Prioridad alta." Rol botón; `onClickLabel` = "Abrir tarea".
- **Checkbox (nodo separado):** etiqueta "Completar tarea: Entregar informe"; `stateDescription` "Pendiente" / "Completada". Al cambiar: "Marcar como pendiente: Entregar informe".
- No fusionar checkbox y fila en un solo nodo: se corre el riesgo de que un doble tap active lo incorrecto.
- **Acciones personalizadas de la fila:** "Completar", "Posponer a mañana", "Eliminar".
- **SubjectCard:** "Cálculo diferencial. 3 de 7 tareas completadas."; `onClickLabel` "Editar materia".
- **Progreso:** la barra se marca como decorativa; el conteo "3 de 7" es el texto que se anuncia. No se crea un nodo de progreso duplicado.
- **Encabezados:** `heading()` en títulos de pantalla y de sección ("Atrasadas", "Para hoy").
- **FAB:** "Nueva tarea" (no "Agregar", que es ambiguo).
- **Snackbar:** `liveRegion = Polite`; "Tarea completada" se anuncia con la acción "Deshacer".
- **Errores inline:** `liveRegion = Assertive` solo para el banner de error; el texto no desaparece solo.
- Iconos decorativos con `contentDescription = null`; iconos de acción con descripción en español.

### 9.3 Fuente grande (hasta 200 %)
- Sin alturas fijas en filas: `heightIn(min = 48.dp)`, nunca `height(56.dp)`.
- Título de tarea: máximo 3 líneas; meta: máximo 2 líneas; sin truncar el nombre de la materia en el editor.
- Fila de propiedad: si `fontScale ≥ 1.3`, etiqueta arriba y valor debajo (columna), no en dos columnas.
- Toolbar flotante: si `fontScale ≥ 1.5`, iconos sin etiqueta, manteniendo `contentDescription`.
- Barra de navegación: etiquetas en 1–2 líneas; la barra puede crecer hasta 88 dp. Prohibido ellipsis en "Materias"/"Ajustes".
- `maxLines` nunca en notas ni en el título del editor.
- Probar 1.0, 1.3, 1.5 y 2.0 en 360 dp de ancho; sin recortes. El único scroll horizontal permitido es el de chips a 200 %.

### 9.4 Teclado y foco
- Orden de foco: back → título → propiedades → notas → subtareas → toolbar.
- Anillo de foco visible 2 dp `tertiary` con offset 2 dp; no se depende del ripple.
- La toolbar no tapa el campo activo: `imePadding` + `BringIntoViewRequester`.
- Enter en subtarea agrega; el foco permanece para escribir varias.

---

## 10. Riesgos de implementación y APIs Expressive

**Hallazgo verificado (2026-10-25):** el BOM `2026.02.01` resuelve `material3 1.4.0` (POM oficial). En 1.4.0 **no están disponibles las APIs Expressive clave**: `MaterialExpressiveTheme` y `MotionScheme` son `internal`; `MaterialShapes`, `FloatingActionButtonMenu`, `HorizontalFloatingToolbar`, `ButtonGroup`, `SplitButton` y `LoadingIndicator` no existen en el artefacto. La release 1.4.0-beta01 (2025-07-30) eliminó las APIs marcadas `ExperimentalMaterial3ExpressiveApi`; la 1.4.0 estable es del 2025-09-24. Por tanto, este documento **emula** la capa Expressive y no depende de alphas.

Regla de decisión: **usar** si el símbolo compila con material3 1.4.0; **emular** si es internal o no existe; **no** subir a alpha sin aprobación del orquestador.

| API Expressive | Estado real en 1.4.0 | Decisión |
|---|---|---|
| `MaterialExpressiveTheme` | internal; no compila | Usar `MaterialTheme` + paleta propia |
| `MotionScheme` / `MaterialTheme.motionScheme` | internal; no configurable | Constantes propias con los valores de los tokens (sección 5) |
| `MaterialShapes` / morphing | no existe | 5 shapes + full, sin morphing |
| `FloatingActionButtonMenu` | no existe | FAB único en V1; emulación con overlay si llega la segunda acción |
| `HorizontalFloatingToolbar` | no existe | `Surface` + `Row` (sección 7.5) |
| `ButtonGroup`, `SplitButton` | no existen | `SegmentedButton` (estable) o `Row` |
| `LoadingIndicator` / wavy | no existen | `CircularProgressIndicator` solo si hace falta |
| `LargeFlexibleTopAppBar`, `TwoRowsTopAppBar` | internal; no públicos | Encabezado propio (sección 7.1) |
| `ShortNavigationBar` | público y estable | Usar; fallback `NavigationBar` |
| List items Expressive / `SegmentedListItem` | no disponibles | Filas propias; `ListItem` estándar donde encaje |

**Alternativa si el orquestador decide subir a `material3 1.5.0-alpha`:** sobrescribir solo `androidx.compose.material3:material3` (no todo el BOM) y usar `MaterialExpressiveTheme`, `MotionScheme`, `MaterialShapes`, `FAB menu` y `HorizontalFloatingToolbar`. Riesgo: cambio de API entre alphas y QA visual; la paleta y la tipografía de este documento siguen siendo válidas. No mezclar ambas rutas en la misma entrega.

**Otros riesgos**
1. **Color dinámico activo por defecto** en el `Theme.kt` actual (`dynamicColor: Boolean = true`). Debe pasar a `false`; si no, la identidad desaparece en Android 12+ y los contrastes calculados no aplican.
2. **Iconos:** `material3 1.4.0` ya no arrastra `material-icons-core`; `Icons.Default.*` no compila sin dependencia. Añadir 12 vector XML de Material Symbols; no añadir la librería completa.
3. **Serif del sistema:** fabricantes pueden sustituir la familia. Validar en Samsung/Xiaomi; si las métricas rompen, el fallback es `SansSerif` solo en `displaySmall`, nunca en toda la escala.
4. **`enableEdgeToEdge` + IME:** el editor debe usar `imePadding` y `Scaffold` con insets; sin esto la toolbar queda debajo del teclado.
5. **Autosave y back predictivo:** guardar con debounce y flush en `DisposableEffect`/`onStop`; no bloquear el gesto del sistema.
6. **Ajuste "semana inicia lunes":** debe afectar el conteo "entregas esta semana" de Materias. Si ese conteo no se implementa, el interruptor se oculta: no se publican ajustes muertos.
7. **Toolchain:** AGP `9.4.0-alpha08` y `compileSdk 37` son preview; cualquier fallo de build ajeno al diseño se reporta, no se parchea desde UI.
8. **Datos:** Room/DataStore aún no están en el proyecto. Los estados de carga/error se implementan contra flujos reales; prohibido rellenar con datos de ejemplo.

---

## 11. Checklist anti-slop (auditable)

- [ ] No hay ningún gradiente en la app (buscar `Brush.` / `linearGradient`).
- [ ] No hay `blur`, `graphicsLayer` con blur ni superficies translúcidas.
- [ ] Ninguna tarjeta usa sombra > 0 dp; solo FAB, snackbar y diálogo tienen 2–3 dp; la toolbar flotante usa borde, no sombra.
- [ ] No hay emojis en strings ni en iconos; todos los iconos son vector XML de 24 dp.
- [ ] Ninguna cifra mostrada es constante o de ejemplo: todos los conteos vienen de Room.
- [ ] Los empty states tienen texto + acción y no ilustración.
- [ ] No existe shimmer ni skeleton animado; la carga local no simula latencia.
- [ ] No hay badges "Beta", "Nuevo" ni "AI".
- [ ] Ningún título de sección usa serif; solo los títulos de pantalla.
- [ ] Máximo un color de acento de interacción (`tertiary`); las materias no colorean botones ni nav.
- [ ] No hay dos CTAs primarios en la misma vista.
- [ ] Ninguna animación de entrada por fila, parallax ni stagger.
- [ ] El radio máximo en contenido es 28 dp (solo sheets/diálogos); las filas usan 0–12.
- [ ] `surface` no es `#FFFFFF` y `onSurface` no es `#000000` en claro; en oscuro tampoco.
- [ ] Todos los textos de UI están en español y sin signos de exclamación.
- [ ] Todo control mide al menos 48×48 dp y tiene etiqueta accesible.
- [ ] "Atrasada" y "Completada" se distinguen sin depender solo del color.
- [ ] Marcar o desmarcar una tarea no cambia padding, altura ni posición del texto (sin jitter).
- [ ] El editor no tiene botón "Guardar"; guarda solo y no pierde contenido al volver.
- [ ] El ajuste de semana tiene un efecto verificable o no se muestra.
- [ ] No se usa `Color(0xFF...)` fuera de los tokens.

---

## 12. Fuentes

| Afirmación | Fuente | Fecha |
|---|---|---|
| BOM `2026.02.01` → `material3 1.4.0` | POM oficial: `https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.02.01/compose-bom-2026.02.01.pom` | consultado 2026-10-25 |
| 1.4.0 estable; eliminación de `ExperimentalMaterial3ExpressiveApi` en beta01; fin de `material-icons` | Release notes de Compose Material 3: `https://developer.android.com/jetpack/androidx/releases/compose-material3` (1.4.0: 2025-09-24; beta01: 2025-07-30) | consultado 2026-10-25 |
| `MaterialExpressiveTheme`/`MotionScheme` internal; shapes 4/8/12/16/28/48; springs expressive 0.8/380, 0.6/800, 1.0/1600, 1.0/3800; `ShortNavigationBar` público | Inspección del artefacto `material3-android-1.4.0-sources.jar` (Maven de Google) | 2026-10-25 |
| Fórmula de contraste y umbrales 4.5/3.0 | WCAG 2.2, `https://www.w3.org/TR/WCAG22/#dfn-contrast-ratio` | consultado 2026-10-25 |
| Intención de Material 3 Expressive (motion/shapes) | `https://m3.material.io/blog/building-with-m3-expressive` | 2025-05 (consulta 2026-10-25) |

Los ratios de la sección 2.4 y 2.5 se calcularon con la fórmula anterior sobre los hex listados, no se copiaron de una herramienta externa.

---

## 13. Desviaciones aprobadas (2026-10-25)

Estas excepciones están implementadas y verificadas; el resto del documento sigue vigente.

1. **Hoja de Entrega abierta para elegir hora (§7.9).** La hoja de Entrega no se cierra al elegir la fecha: permanece abierta para ofrecer "Elegir hora"/"Quitar hora" y solo se cierra al tocar fuera, con el gesto de vuelta o al elegir "Sin fecha". Se mantiene porque la hora es opcional y el date picker no debe forzar una segunda apertura.
2. **`SwipeTaskRow` usa el constructor deprecado de `SwipeToDismissBoxState`.** En material3 1.4.0 el constructor principal perdió `confirmValueChange`; la alternativa pública es `onDismiss` + `reset()`, que deja que la fila llegue al ancho completo antes de volver (salto visible). Se usa el constructor deprecado, todavía público en 1.4.0, con `confirmValueChange` devolviendo `false` para ejecutar la acción y volver a `Settled` sin expulsar la fila. Se retirará cuando el componente estable ofrezca el mismo control sin parpadeo.
3. **Navegación y back.** El editor no intercepta el gesto predictivo: `PredictiveBackHandler` hace el flush y sale. En las pestañas secundarias se conserva un `BackHandler` únicamente para volver a Hoy (navegación interna); en Hoy el back lo gestiona el sistema. Esta excepción no aplica al editor, donde §6/§10.5 se cumple literalmente.
4. **Etiqueta "Atrasada · ayer".** Se implementa con `<plurals quantity="one">` en lugar de la clave única `task.overdue` de §8, para que singular y plural compartan una sola fuente de verdad.
