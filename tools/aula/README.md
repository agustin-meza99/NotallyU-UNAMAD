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

Comprobar el entorno sin tocar la red:

```powershell
$env:AULA_USER="demo"; $env:AULA_PASS="demo"
node tools/aula/explore.mjs --dry-run
```

También puedes validar que el script no tiene errores de sintaxis:

```bash
node --check tools/aula/explore.mjs
```

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
- **No compartas tus credenciales** ni las escribas en archivos, scripts o historiales de
  shell. Usa solo variables de entorno.
- **Usa únicamente tu propia cuenta.** No automatices cuentas ajenas ni extraigas datos de
  terceros.
- **Respeta los términos de servicio del Aula Virtual.** Este script es una herramienta de
  inspección personal; un uso intensivo o automatizado puede violar las condiciones del
  servicio o provocar bloqueos.
- Haz un solo intento por ejecución. Si falla el login, espera antes de volver a probar.
- El script no descarga material ni modifica el campus; solo lee endpoints y enlaces.
