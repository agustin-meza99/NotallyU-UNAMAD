# Notally — Arquitectura

**Alcance:** Android nativo, un solo módulo Gradle `:app`. Este documento describe las capas reales, el contrato remoto y el orden en que se habilitará el sync. Si el código y este documento discrepan, gana el código y se corrige aquí en el mismo commit.

**Estado:** V1 offline-first. Room es la única fuente de verdad. La capa `data/remote/firestore` existe y está probada en JVM, pero **no hay Auth, no hay sync activo y la UI no lee Firestore**.

---

## 1. Stack y versiones reales

Verificado contra `gradle/libs.versions.toml` y `app/build.gradle.kts`.

| Pieza | Versión |
|---|---|
| AGP | 9.4.0-alpha08 |
| Kotlin | 2.2.10 |
| KSP | 2.3.12 |
| Gradle | 9.6.0 |
| Compose BOM | 2026.02.01 |
| material3 | 1.4.0 (resuelta por el BOM) |
| Room | 2.8.5 (runtime, ktx, compiler) |
| DataStore Preferences | 1.3.0 |
| Firebase BOM | 34.19.0 (`firebase-auth`, `firebase-firestore`) |
| minSdk / targetSdk / compileSdk | 30 / 37 / 37 |
| Java source/target | 11 |
| Módulos | 1 (`:app`) |

`google-services.json` está presente y el plugin `google-services` aplicado. Eso **no** inicializa Firebase: la inicialización ocurre recién cuando alguien usa `FirebaseFirestore.getInstance()` o `FirebaseAuth.getInstance()`, y hoy no se usa en el código de producción.

---

## 2. Decisión de arquitectura

**MVVM + flujo unidireccional (UDF) por pantalla**, con cuatro capas:

```
ui (Compose) → ViewModel → repository → local (Room)
                                      ↘ remote (Firestore, solo detrás de SyncEngine)
```

- **UI:** composables sin lógica de negocio. Reciben estado inmutable y emiten eventos al ViewModel. No conocen repositorios ni DAOs.
- **ViewModel:** uno por pantalla; expone `StateFlow<UiState>` y eventos one-shot por `SharedFlow`. Llama a repositorios y compone el estado que la pantalla necesita.
- **Repository:** única puerta de entrada a los datos. Traduce modelos de Room a operaciones de dominio y centraliza reglas de escritura (timestamps, `completedAt`, orden de subtareas). No conoce Compose.
- **local/remote:** detalles de persistencia. Room es la fuente de verdad; Firestore es un espejo que solo toca `data/sync`.

**DI manual con `AppContainer` en `core`.** Una instancia en `NotallyApp`, accesible por `Context.notallyContainer`. `core` es el punto de composición + utilidades transversales: construye base de datos, repositorios y (en el futuro) el `SyncEngine`.

**Un solo módulo Gradle.** El proyecto es pequeño y los límites se sostienen con reglas de dependencia + revisión. Extraer módulos antes de tener un problema real de build o de equipos solo agrega ceremonia.

### Por qué no hay use cases

Los repositorios ya exponen operaciones con nombre de intención (`toggleDone`, `setArchived`, `reorderSubtasks`); un use case por llamada sería una capa de reenvío sin valor. **Extraer use cases cuando se cumpla al menos uno de estos criterios:**

1. Dos o más features reutilizan la misma secuencia de pasos (p. ej. "crear tarea + notificar + encolar sync").
2. Un ViewModel acumula **más de 3 dependencias** de repositorio y empieza a coordinar transacciones entre ellas.
3. La lógica necesita probarse **sin Android** y hoy arrastra `Context`, `WorkManager` o `FirebaseFirestore`.

### Por qué no hay Hilt todavía

`AppContainer` son ~50 líneas, hay una sola pantalla raíz y cero scopes dinámicos. Hilt añadiría KSP, annotations y tiempo de build sin resolver un problema actual. **Extraer Hilt cuando:** haya ≥3 entry points con grafos distintos (Activity, Worker, Service), el `AppContainer` supere ~150 líneas, o aparezcan dependencias con scope (por usuario, por sesión de sync).

---

## 3. Árbol de carpetas actual y objetivo

Leyenda: **✓ existe** · **✗ nuevo en este cambio** · **◇ propuesto (no existe)**

```
app/src/main/java/com/example/notally/
├── NotallyApp.kt                    ✓ Application + AppContainer + seed demo
├── MainActivity.kt                  ✓ entry point Compose
├── data/
│   ├── model/                       ✓ Task, Subject, Subtask, relations, Settings
│   ├── local/                       ✓ TaskDao, SubjectDao, SubtaskDao
│   ├── repository/                  ✓ TaskRepository, SubjectRepository, SettingsRepository
│   ├── remote/firestore/            ✗ contrato y mappers puros (Task/Subject/Subtask)
│   ├── sync/                        ◇ SyncEngine, OutboxDao, SyncState (futuro)
│   ├── NotallyDatabase.kt           ✓ Room v1
│   ├── Converters.kt                ✓ enums persistidos por nombre
│   ├── DateRanges.kt                ✓ rangos de día en hora local
│   ├── SeedData.kt                  ✓ datos demo solo en debug
│   └── SubjectColors.kt             ✓ paleta de materias (fuente única)
├── core/
│   └── AppContainer.kt              ✓ punto de composición + utilidades transversales
├── ui/
│   ├── theme/                       ✓ tokens, tipografía, formas, movimiento
│   ├── components/                  ✓ componentes reutilizables
│   ├── today/                       ✓ Hoy, agrupación, reglas de postergar
│   ├── subjects/                    ✓ Materias, formulario, entregas de semana
│   ├── editor/                      ✓ detalle/editor de tarea
│   ├── settings/                    ✓ ajustes
│   ├── util/                        ✓ DateFormat, DateText (solo UI)
│   ├── NotallyRoot.kt               ✓ navegación y tabs
│   └── auth/                        ◇ pantalla de acceso (futuro)
└── util/                            ◇ helpers puros sin Android (futuro)
```

| Paquete | Responsabilidad | Puede importar |
|---|---|---|
| `data.model` | Tipos de dominio y entidades Room | Kotlin, Room |
| `data.local` | DAOs y SQL | `data.model` |
| `data.repository` | API de datos para ViewModels | `data.model`, `data.local` |
| `data.remote.firestore` | Contrato remoto + mappers puros | `data.model`, Kotlin |
| `data.sync` | Pull/push, outbox, LWW, persistencia | repos, remote, Room |
| `core` | Punto de composición + utilidades transversales | todo `data` |
| `ui.*` | Estado, pantallas, componentes | `data.model`, repos vía VM, `ui.*` |
| `util` | Fechas/formato puro sin Android | Kotlin/JVM |

**Reglas de dependencia**

1. `ui/**` **no** importa `data.remote.**` ni `com.google.firebase.firestore`. La UI solo ve modelos de dominio y estado de ViewModel.
2. `data.local.**` **no** importa `ui.**`.
3. `data.remote.firestore` no importa `com.google.firebase.*`: es Kotlin puro y se prueba en JVM.
4. Los ViewModels no importan DAOs ni `FirebaseFirestore`; hablan con repositorios.
5. Excepción documentada: `ui/theme/Color.kt` consume `data/SubjectColors.kt` para construir los swatches de materia. Es una dependencia de **tokens de color**, no de estado ni de persistencia; se mantiene para que la paleta tenga una sola fuente de verdad y no duplique hex en UI.

---

## 4. Flujo de datos

```
┌──────────────────────────────┐
│ Compose (ui/**)              │  UiState inmutable / eventos
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ ViewModel                    │  StateFlow + SharedFlow
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Repository                   │  reglas de escritura
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Room  ← FUENTE DE VERDAD     │
└──────────────▲───────────────┘
               │ pull en transacción
┌──────────────┴───────────────┐
│ SyncEngine (◇ futuro)        │  outbox → push
└──────────────┬───────────────┘
               ▼ (remoteEnabled + Auth)
┌──────────────────────────────┐
│ Firestore users/{uid}/...    │
└──────────────────────────────┘
```

La UI **nunca** lee Firestore y nunca espera a la red para pintar: pinta la emisión de Room. El sync es un detalle de background.

---

## 5. Contrato de datos remoto

Alineado con `firestore.rules` (deny-by-default, auth obligatoria). Rutas: `users/{uid}/tasks/{taskId}` y `users/{uid}/subjects/{subjectId}`.

### 5.1 Tarea (`validTask`)

Claves **permitidas** (`hasOnly`): `title`, `notes`, `subjectId`, `dueAt`, `priority`, `status`, `subtasks`, `createdAt`, `updatedAt`, `completedAt`, `deletedAt`. Obligatorias: `title`, `notes`, `priority`, `status`, `subtasks`, `createdAt`, `updatedAt`.

| Campo | Tipo | Límite / valor |
|---|---|---|
| `title` | `string` | 1..300 |
| `notes` | `string` | 0..20000 |
| `subjectId` | `string \| null` | ≤128 |
| `dueAt` | `number \| null` | epoch ms |
| `priority` | `string` | `LOW`, `MEDIUM`, `HIGH` |
| `status` | `string` | `PENDING`, `IN_PROGRESS`, `DONE` |
| `subtasks` | `list` | ≤100; cada ítem `{id,title,isDone,position}` |
| `createdAt` / `updatedAt` | `timestamp` | **serverTimestamp** |
| `completedAt` | `number \| null` | epoch ms |
| `deletedAt` | `timestamp \| null` | tombstone |

En create, las reglas exigen `createdAt == request.time` y `updatedAt == request.time`. En update, `createdAt` debe conservar el valor existente y `updatedAt == request.time`. Por eso el mapper modela los timestamps como `Long?` epoch ms y documenta que `null` en el borde significa `FieldValue.serverTimestamp()`; el borde decide create vs update.

### 5.2 Materia (`validSubject`)

Claves permitidas: `name`, `colorArgb`, `professor`, `credits`, `createdAt`, `updatedAt`, `deletedAt`. Obligatorias: `name`, `colorArgb`, `createdAt`, `updatedAt`.

| Campo | Tipo | Límite / valor |
|---|---|---|
| `name` | `string` | 1..120 |
| `colorArgb` | `number` | ARGB opaco |
| `professor` | `string \| null` | ≤120 |
| `credits` | `number \| null` | ≥0 |
| `createdAt` / `updatedAt` | `timestamp` | **serverTimestamp** |
| `deletedAt` | `timestamp \| null` | tombstone |

### 5.3 Gap: `Task.isArchived` no existe en las reglas

Room tiene `isArchived: Boolean`; las reglas no permiten ese campo y `hasOnly` rechaza cualquier clave extra. **Decisión V1: no se sincronizan las tareas archivadas.** `Task.toRemote()` lanza `IllegalArgumentException` si `isArchived == true` en lugar de perder el dato en silencio. Una tarea archivada sigue viviendo solo en Room; las tareas borradas se representan con `deletedAt` (tombstone) cuando el sync exista.

**Deuda:** si se quiere sincronizar el archivo, añadir `archivedAt: timestamp | null` a la regla y a `RemoteTask`, y migrar `isArchived` a ese timestamp. No se hace ahora porque requiere redesplegar reglas (fuera de alcance de esta fase).

### 5.4 Gap de identidad: `Long` local vs docId estable

Room usa `Long` autogenerado. Firestore necesita un docId estable entre dispositivos. Mientras no haya sync, el mapper usa `id.toString()` y `toLocal()` exige que `subjectId` sea numérico (falla explícito si llega un UUID).

**Migración propuesta (cuando se active el sync):**

1. Añadir `remoteId: String = UUID.randomUUID().toString()` a `Task` y `Subject` (y, si se necesita diff de subtareas, a `Subtask`).
2. Room **v1 → v2**: `ALTER TABLE ... ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''`, backfill con UUID en la migración y crear índice único.
3. `docId = remoteId`; las referencias de materia usan `remoteId`, no `id`.
4. Cambiar los mappers para aceptar `remoteSubjectId` y resolverlo contra Room.
5. Nunca reutilizar `id.toString()` una vez migrado.

Hasta entonces, cualquier documento remoto con UUID en `subjectId` falla de forma explícita: mejor un error visible que una relación perdida.

---

## 6. Sincronización (diseño, no implementado)

`data/sync` no existe todavía. El diseño acordado:

- **Pull:** `addSnapshotListener(metadataChanges = INCLUDE)` sobre `users/{uid}/tasks` y `users/{uid}/subjects`. Los cambios se aplican a Room en una `withTransaction`, comparando `updatedAt`. La metadata permite distinguir datos de caché de datos confirmados por el servidor.
- **Escritura optimista:** primero Room (la UI responde al instante), luego el outbox. Si el push falla, la operación queda pendiente y se reintenta con backoff. La UI nunca se bloquea esperando red.
- **Outbox:** tabla Room con `operation` (UPSERT/DELETE), `entityType`, `entityLocalId`, `attempts`, `lastError`, `createdAt`. Se drena en orden y se elimina al confirmar el servidor.
- **Last-write-wins por `updatedAt`:** si el remoto es más nuevo, se aplica a Room; si el local es más nuevo, se encola. Empates: gana el remoto por determinismo.
- **Tombstones:** `deletedAt` (Timestamp) en el remoto. Hoy Room hace borrado duro; antes de activar sync hay que añadir borrado suave local (`deletedAt: Long?`) o registrar el delete en el outbox para propagarlo.
- **Cierre de sesión:** `FirebaseFirestore.clearPersistence()` y limpieza de datos del usuario en Room para no filtrar datos entre cuentas. Es requisito previo al flag, no un extra.
- **Límites:** sin queries compuestas (`where` + `orderBy`) hasta crear los índices; en V1 se escucha el subárbol del usuario y se filtra en Room. Sin paginación hasta que el volumen lo justifique.
- **Por qué la UI no lee Firestore:** Room ya es la fuente de verdad, funciona offline y es testeable; Firestore introduce latencia, costo por lectura y estados de error de red. Leerlo desde la UI duplicaría estado y rompería el UDF.

---

## 7. Auth diferido

- `firebase-auth` y `firebase-firestore` están en Gradle, pero **no se llama a `FirebaseAuth` ni se inicializa Firebase**.
- El proveedor Email/Password está deshabilitado en la consola. Cualquier intento de login devolvería `OPERATION_NOT_ALLOWED`.
- Las reglas desplegadas son deny-by-default y exigen `request.auth != null`. **No hay reglas abiertas ni usuarios anónimos.**
- El sync queda detrás de un flag `remoteEnabled = false`. Ubicación propuesta: `AppContainer` (const o preferencia), leído por el `SyncEngine`. Con el flag apagado no se crea listener ni se escribe nada remoto.

Para habilitarlo: activar Email/Password en la consola → añadir `ui/auth` y sesión en `AppContainer` → adjuntar `uid` a las rutas → probar en dispositivo real → recién entonces poner `remoteEnabled = true`. Sin ese orden no se toca el flag.

---

## 8. Convenciones de código

- **Nombres:** paquetes en minúsculas; clases en `PascalCase`; funciones y propiedades en `camelCase`; constantes en `UPPER_SNAKE_CASE`. Los mappers se nombran `XMapper.kt` con extensiones `toRemote()` / `toLocal()`.
- **Tamaño:** límite orientativo **600 líneas por archivo**. `EditorScreen.kt` (~1023 líneas) es deuda conocida y debe dividirse por secciones.
- **Un ViewModel por pantalla.** Estado inmutable (`data class UiState`) expuesto con `StateFlow`.
- **Eventos one-shot** (navegación, snackbar) por `SharedFlow`, nunca dentro del `UiState`.
- **Textos:** todo lo visible en `res/values/strings.xml`. Nada hardcodeado en composables.
- **Fechas:** solo con `ui/util/DateFormat` (y `DateText`). No formatear con `SimpleDateFormat` suelto en pantallas.
- **Colores:** solo tokens de `ui/theme/Color.kt`. Prohibido `Color(0xFF…)` en pantallas o componentes; los colores de materia salen de `data/SubjectColors`.
- **Errores de contrato:** los mappers fallan con `require` y mensaje accionable; no se ignoran campos.

---

## 9. Checklist para añadir una feature

1. **Modelo:** entidad/campo en `data/model` (si toca Room, subir versión y escribir migración + test).
2. **DAO:** consulta en `data/local`, con `Flow` si es lectura observable.
3. **Repositorio:** método con nombre de intención; centralizar timestamps y reglas de negocio.
4. **ViewModel:** `UiState` inmutable, `StateFlow`, eventos por `SharedFlow`.
5. **Pantalla:** composable sin lógica; textos a `strings.xml`; fechas con `DateFormat`; colores desde tokens.
6. **Contrato remoto (si aplica):** campo en `Remote*`, mapper, validación de límites y actualización de `firestore.rules` (fuera de este repo si ya está desplegado).
7. **Tests:** reglas puras y mappers en JVM; UI/instrumentados solo si hay navegación o interacción real.
8. **Verificación targeted:** `.\gradlew.bat :app:compileDebugKotlin` y `.\gradlew.bat :app:testDebugUnitTest`; revisar el conteo y que `failures=0`.

---

## 10. Deuda y próximos pasos (priorizados)

1. **Auth:** habilitar Email/Password, construir `ui/auth`, adjuntar `uid`, y solo después evaluar `remoteEnabled`.
2. **Sync v1:** `data/sync` con listener, outbox, LWW y `clearPersistence()` al cerrar sesión; test de integración con emulador de Firestore.
3. **Migración UUID:** `remoteId` en `Task`/`Subject`, Room v1→v2 y mappers por `remoteId`. Prerrequisito para multi-dispositivo real.
4. **Tombstones locales:** borrado suave + `deletedAt` para propagar deletes sin perder el rastro.
5. **Split de `EditorScreen.kt`:** extraer secciones/hojas para volver al límite de 600 líneas.
6. **Tests instrumentados:** navegación, editor y sync con Room real.
7. **Índices Firestore:** crear solo cuando aparezcan queries compuestas; no antes.
