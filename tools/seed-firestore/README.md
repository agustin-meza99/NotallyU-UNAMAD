# Seed de Firestore (Notally)

Script en Node.js puro que siembra en Firestore el mismo contenido de
`app/src/main/java/com/example/notally/data/SeedData.kt`: **4 materias y 11
tareas** en español, con fechas relativas a hoy, subtareas, una tarea `DONE` y
una `IN_PROGRESS`.

Escribe **como el propio usuario** con la API REST de Identity Toolkit y
Firestore. No usa el Admin SDK ni service accounts, así que respeta
`firestore.rules` (deny-by-default por `users/{uid}`).

## Requisitos

- Node.js 18+ (probado con Node 24). Usa `fetch` global: **sin dependencias npm**.
- Email/Password habilitado en Firebase Auth.
- `app/google-services.json` presente en el repo (de ahí se lee la API key; el
  proyecto sale de `project_info.project_id`).

## Uso

Define las credenciales **solo en el entorno de la sesión**:

```powershell
$env:SEED_EMAIL = 'tu-correo@ejemplo.com'
$env:SEED_PASSWORD = 'tu-password'

node tools/seed-firestore/seed.mjs --dry-run   # muestra el plan, no escribe
node tools/seed-firestore/seed.mjs             # crea o actualiza (idempotente)
node tools/seed-firestore/seed.mjs --reset     # borra primero los docs del seed y recrea
```

Al terminar, limpia las variables:

```powershell
Remove-Item Env:SEED_EMAIL, Env:SEED_PASSWORD
```

### Variables de entorno

| Variable        | Obligatoria | Descripción                          |
| --------------- | ----------- | ------------------------------------ |
| `SEED_EMAIL`    | Sí          | Correo de la cuenta de prueba.       |
| `SEED_PASSWORD` | Sí          | Contraseña de la cuenta de prueba.   |

Si falta alguna, el script termina con `exit 1` y un mensaje claro. **No hay
credenciales hardcodeadas.**

## Flags

- `--dry-run`: autentica y consulta el estado actual, imprime qué documentos
  crearía/actualizaría y no escribe nada.
- `--reset`: lista `users/{uid}/tasks` y `users/{uid}/subjects`, borra **solo**
  los documentos cuyos IDs pertenecen al seed y luego los recrea. No toca otros
  documentos del usuario.

## IDs e idempotencia

Los IDs son deterministas y legibles, por ejemplo `subject-calculo`,
`task-lab-avl` o `task-consultas-sql`. Volver a ejecutar el script actualiza los
mismos documentos en lugar de duplicarlos.

En creación, `createdAt` y `updatedAt` se escriben con `updateTransforms` y
`REQUEST_TIME`, como exigen las reglas. En una actualización, `createdAt` se
conserva con el `timestampValue` remoto (la regla exige
`createdAt == resource.data.createdAt`) y solo `updatedAt` se refresca con
`REQUEST_TIME`.

Nota: `SeedData.kt` usa `createdAt` históricos (por ejemplo, "hace 30 días" para
las materias), pero las reglas obligan a que `createdAt` sea la hora del servidor
en el alta. Por eso el seed remoto no replica esas marcas históricas; el resto
de los campos (títulos, notas, prioridades, estados, subtareas y `dueAt`
relativo a hoy) sí es idéntico.

## Seguridad

- **Nunca** hagas commit de `SEED_PASSWORD`, de un archivo `.env` ni de
  credenciales en texto plano. `tools/seed-firestore/node_modules/` y `.env`
  están en `.gitignore`.
- El script no imprime la API key, el `idToken` ni el `refreshToken`.
- Usa una cuenta de prueba, no la cuenta personal principal. Cambia la
  contraseña si es débil.

## Relación con el seed local

Este seed remoto es temporal mientras no exista sincronización. Cuando la app
tenga sync real, el seed local (`data/SeedData.kt`) se eliminará y estos datos
podrán migrarse o regenerarse desde Firestore.
