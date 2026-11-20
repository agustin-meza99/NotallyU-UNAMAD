tyo# Documentación de la API — Aula Virtual UNAMAD

Base URL: `https://aula-virtual.unamad.edu.pe`

> Referencia de origen: [tools/aula/README.md](tools/aula/README.md)

## Autenticación

- **Login:** `POST /account/login`
  - Cuerpo: `FormData` con credenciales (`AULA_USER`, `AULA_PASS`).
  - Cabecera recomendada: `User-Agent: NotallyExplorer/1.0`.
  - Respuesta: Devuelve cookie de sesión (puede responder con redirección `302` usando `redirect: 'manual'`).
  - **Importante:** No reintentar automáticamente si falla; el servidor bloquea temporalmente la cuenta tras varios intentos fallidos.

## Endpoints conocidos (GET autenticados)

Todos los endpoints siguientes requieren la cookie de sesión obtenida en el login:

- `GET /web/section/get?s=<sectionId>` — Detalle de una sección/curso.
- `GET /web/course?s=<sectionId>` — Vista de curso.
- `GET /web/homework?s=<sectionId>` — Tareas/actividades de una sección.

## Endpoints explorados por `tools/aula/explore.mjs`

El script local de inspección consulta y guarda las respuestas crudas en `tools/aula/out/` (ignorado por Git):

- `user-info-data.json`
- `system-courseinrole.json`
- `courseinrole.json`
- `calendar-list.json`
- `periodos-academicos.json`
- `general-announcements.json`
- `home.html` (portada autenticada)
- `section-<id>.json`
- `enlaces.json` (enlaces internos deduplicados)
- `resumen.json`

## Cómo ejecutar el explorador local

Requiere Node.js 24+ y credenciales en `tools/aula/.env`:

```powershell
Copy-Item tools/aula/.env.example tools/aula/.env
node tools/aula/explore.mjs --dry-run
node tools/aula/explore.mjs
```

Para descubrir endpoints de tareas y actividades:

```powershell
node tools/aula/discover.mjs --dry-run
node tools/aula/discover.mjs
```
