# Explorador local del Aula Virtual (UNAMAD)

Script de inspección del [Aula Virtual de UNAMAD](https://aula-virtual.unamad.edu.pe) que corre
localmente con Node 24. **No usa dependencias npm** y no forma parte de la app Notally.

Sirve para volcar las respuestas crudas de los endpoints conocidos y descubrir enlaces internos
(secciones, cursos, tareas, foros, etc.). No modifica nada en el servidor: solo hace `GET` tras
un único `POST /account/login`.

## Requisitos

- Node.js 24 o superior.
- Una cuenta propia del Aula Virtual.

## Uso

Hay dos vías para dar las credenciales.

### 1. Archivo `.env` local (recomendado)

Copia la plantilla y rellena tus datos:

```powershell
Copy-Item tools/aula/.env.example tools/aula/.env
# Edita tools/aula/.env y escribe tu usuario y contraseña
node tools/aula/explore.mjs
```

El archivo `tools/aula/.env` tiene este formato (sin comillas es lo normal; también se
admiten comillas simples o dobles):

```dotenv
# Comentario
AULA_USER=tu_usuario
AULA_PASS=tu_contraseña
```

El explorador lee `tools/aula/.env` automáticamente al arrancar. Si una variable ya está
definida en el entorno, se respeta ese valor (por lo que también funciona con
`node --env-file=tools/aula/.env tools/aula/explore.mjs`).

### 2. Variables de entorno

PowerShell:

```powershell
$env:AULA_USER="tu_usuario"
$env:AULA_PASS="tu_contraseña"
node tools/aula/explore.mjs
```

Bash:

```bash
export AULA_USER="tu_usuario"
export AULA_PASS="tu_contraseña"
node tools/aula/explore.mjs
```

Variables:

| Variable    | Obligatoria | Descripción                     |
|-------------|-------------|---------------------------------|
| `AULA_USER` | Sí          | Usuario del Aula Virtual        |
| `AULA_PASS` | Sí          | Contraseña del Aula Virtual     |

Opciones:

| Opción         | Descripción                                                        |
|----------------|--------------------------------------------------------------------|
| `--base <url>` | URL base alternativa (por defecto `https://aula-virtual.unamad.edu.pe`) |
| `--dry-run`    | Valida el entorno y muestra el plan sin hacer red ni login         |
| `--help`       | Muestra la ayuda                                                   |

Comprobar el entorno sin tocar la red (con `.env` relleno o con variables de entorno):

```powershell
node tools/aula/explore.mjs --dry-run
```

`--dry-run` solo comprueba que las credenciales existen y muestra el plan de peticiones;
no hace login ni contacta el servidor.

También puedes validar que el script no tiene errores de sintaxis:

```bash
node --check tools/aula/explore.mjs
```

## Descubrimiento de tareas/actividades

`discover.mjs` es un segundo modo, centrado en encontrar dónde viven las tareas:

```powershell
node tools/aula/discover.mjs
node tools/aula/discover.mjs --dry-run
```

Hace un login y como máximo 10 peticiones autenticadas (1/s):

1. Analiza estáticamente `out/home.html`, `out/extra-1.html` y las páginas que descarga.
2. `GET /web/course?s=<sectionId>` y `GET /web/homework?s=<sectionId>` del primer curso.
3. Descarga hasta 4 JS propios (controladores de curso/tareas y calendario).
4. Consulta `/web/section/get?s=<sectionId>` de dos cursos.
5. Prueba **un** endpoint derivado solo si el JS da una plantilla o `params` inequívocos.
6. Solo llama al calendario si el JS muestra parámetros de periodo (`start`, `end`, `term`…).

Escribe `out/tareas-descubrimiento.json` con rutas, códigos, tamaños, claves y el
detalle de endpoints hallados. No imprime cookies ni credenciales.

## Qué guarda

Todo se escribe en `tools/aula/out/`, que está ignorado por git:

- `user-info-data.json`, `system-courseinrole.json`, `courseinrole.json`,
  `calendar-list.json`, `periodos-academicos.json`, `general-announcements.json`:
  respuestas crudas de los endpoints conocidos (extensión `.html` si la respuesta no es JSON).
- `home.html`: la portada autenticada.
- `section-<id>.json`: detalle de hasta 15 secciones detectadas en la portada
  (`/web/section/get?s=<id>`).
- `enlaces.json`: enlaces internos deduplicados que contienen `/section/`, `/web/`,
  `/curso`, `/tarea`, `/actividad`, `/evaluacion` o `/foro`.
- `resumen.json`: usuario (`username` e `id` solamente), códigos HTTP, tamaños,
  número de cursos, eventos, enlaces y secciones.
- `login-error.html`: solo si el login devuelve HTML en vez de JSON.

La consola muestra el progreso en español, sin credenciales, sin cookies y sin volcar
cuerpos completos.

## Comportamiento y códigos de salida

- Login: `POST /account/login` con `FormData`, `redirect: 'manual'` y
  `User-Agent: NotallyExplorer/1.0`. Se acepta la cookie de sesión aunque el servidor
  responda con una redirección.
- **Un solo intento de login por ejecución.** No hay reintentos automáticos, porque el
  servidor bloquea temporalmente la cuenta tras varios fallos.
- Endpoints autenticados: 1 petición por segundo y timeout de 20 s.
- Si el login devuelve HTML, se guarda `login-error.html` y se explica que el flujo
  probablemente cambió.
- Los errores de red se informan y no se reintentan.

| Código | Significado                                      |
|--------|--------------------------------------------------|
| 0      | Ejecución correcta                               |
| 1      | Faltan variables o argumentos inválidos          |
| 2      | Credenciales rechazadas por el servidor          |
| 3      | Flujo de login cambiado o sin cookie de sesión   |
| 4      | Error de red o fallo inesperado                  |

## Advertencias

- **No hagas commit de `tools/aula/out/`.** Contiene datos de tu cuenta y respuestas del
  campus. Ya está en `.gitignore`.
- **No hagas commit de `tools/aula/.env`.** Contiene tus credenciales y está ignorado por
  git. La plantilla versionada es `tools/aula/.env.example` (sin datos). Nunca commitees
  `.env` ni ningún `.env.*` distinto de `.env.example`.
- **No compartas tus credenciales** ni las escribas en scripts versionados o historiales de
  shell. Usa `tools/aula/.env` o variables de entorno.
- **Usa únicamente tu propia cuenta.** No automatices cuentas ajenas ni extraigas datos de
  terceros.
- **Respeta los términos de servicio del Aula Virtual.** Este script es una herramienta de
  inspección personal; un uso intensivo o automatizado puede violar las condiciones del
  servicio o provocar bloqueos.
- Haz un solo intento por ejecución. Si falla el login, espera antes de volver a probar.
- El script no descarga material ni modifica el campus; solo lee endpoints y enlaces.
