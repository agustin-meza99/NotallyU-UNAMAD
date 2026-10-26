#!/usr/bin/env node
/**
 * Descubrimiento de tareas/actividades del Aula Virtual de UNAMAD.
 *
 * Reutiliza la lógica de sesión de explore.mjs (sin dependencias npm), pero
 * con un presupuesto duro de peticiones autenticadas y sin volcar credenciales.
 *
 * Flujo:
 *   1. Analiza estáticamente out/home.html y out/extra-1.html (sin red).
 *   2. Abre la página de UN curso (/web/course?s=<sectionId>) para descubrir
 *      los controladores JS propios de curso/sección/tareas.
 *   3. Descarga como máximo 4 JS candidatos y extrae rutas de endpoints.
 *   4. Consulta /web/section/get?s=<sectionId> de DOS cursos.
 *   5. Prueba UN endpoint derivado solo si el JS da una plantilla inequívoca.
 *   6. Prueba /web/calendar solo si el JS muestra parámetros de periodo.
 */

import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const ARCHIVO_ENV = path.join(AQUI, '.env');
const DIR_SALIDA = path.join(AQUI, 'out');
const BASE_POR_DEFECTO = 'https://aula-virtual.unamad.edu.pe';
const USER_AGENT = 'NotallyDiscover/1.0';
const TIMEOUT_MS = 20_000;
const RETARDO_MS = 1_000;
const LIMITE_PETICIONES = 10;
const MAX_JS = 4;

function salir(codigo, mensaje) {
  if (mensaje) console.error(mensaje);
  process.exit(codigo);
}

async function cargarEnvLocal() {
  let contenido;
  try {
    contenido = await readFile(ARCHIVO_ENV, 'utf8');
  } catch (error) {
    if (error?.code === 'ENOENT') return;
    console.warn(`Aviso: no se pudo leer ${path.relative(process.cwd(), ARCHIVO_ENV)}: ${error?.message ?? error}`);
    return;
  }

  for (const lineaCruda of contenido.split(/\r?\n/)) {
    const linea = lineaCruda.trim();
    if (!linea || linea.startsWith('#')) continue;

    const separador = linea.indexOf('=');
    if (separador === -1) continue;

    const clave = linea.slice(0, separador).trim();
    if (!clave) continue;

    let valor = linea.slice(separador + 1).trim();
    if (valor.length >= 2 && valor.startsWith('"') && valor.endsWith('"')) {
      valor = valor.slice(1, -1);
    } else if (valor.length >= 2 && valor.startsWith("'") && valor.endsWith("'")) {
      valor = valor.slice(1, -1);
    }

    if (process.env[clave] === undefined) process.env[clave] = valor;
  }
}

function analizarArgumentos(argv) {
  const opciones = { base: BASE_POR_DEFECTO, ayuda: false, dryRun: false };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--help' || arg === '-h') {
      opciones.ayuda = true;
    } else if (arg === '--dry-run') {
      opciones.dryRun = true;
    } else if (arg === '--base') {
      const valor = argv[i + 1];
      if (!valor) salir(1, 'Falta el valor de --base <url>.');
      opciones.base = valor;
      i += 1;
    } else if (arg.startsWith('--base=')) {
      opciones.base = arg.slice('--base='.length);
    } else {
      salir(1, `Argumento no reconocido: ${arg}\nUsa --help para ver las opciones.`);
    }
  }

  try {
    const url = new URL(opciones.base);
    if (!/^https?:$/.test(url.protocol)) throw new Error('protocolo no soportado');
    opciones.base = url.origin + url.pathname.replace(/\/+$/, '');
  } catch {
    salir(1, `La URL de --base no es válida: ${opciones.base}`);
  }

  return opciones;
}

function mostrarAyuda() {
  console.log(`Descubrimiento de tareas/actividades del Aula Virtual

Uso:
  node tools/aula/discover.mjs
  node tools/aula/discover.mjs --dry-run
  node tools/aula/discover.mjs --base https://aula-virtual.unamad.edu.pe

Credenciales: tools/aula/.env (AULA_USER / AULA_PASS) o variables de entorno.

Opciones:
  --base <url>   URL base alternativa
  --dry-run      Muestra el plan sin hacer login
  -h, --help     Muestra esta ayuda

Presupuesto: un solo login y como máximo ${LIMITE_PETICIONES} peticiones
autenticadas (1 por segundo). Salida en tools/aula/out/.`);
}

function esperar(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function extraerCookies(headers) {
  let crudas = [];
  if (typeof headers.getSetCookie === 'function') crudas = headers.getSetCookie();
  if (!crudas || crudas.length === 0) {
    const combinadas = headers.get('set-cookie');
    if (combinadas) crudas = combinadas.split(/,(?=\s*[!#$%&'*+\-.^_`|~0-9A-Za-z]+=)/);
  }
  return crudas
    .map((cookie) => cookie.split(';')[0].trim())
    .filter((cookie) => cookie.includes('='));
}

class Sesion {
  constructor(base) {
    this.base = base;
    this.cookies = new Map();
    this.ultimaPeticion = 0;
    this.peticiones = 0;
  }

  guardarCookies(headers) {
    for (const cookie of extraerCookies(headers)) {
      const separador = cookie.indexOf('=');
      this.cookies.set(cookie.slice(0, separador), cookie.slice(separador + 1));
    }
  }

  cabeceraCookie() {
    return [...this.cookies.entries()].map(([nombre, valor]) => `${nombre}=${valor}`).join('; ');
  }

  async esperarTurno() {
    const transcurrido = Date.now() - this.ultimaPeticion;
    const restante = RETARDO_MS - transcurrido;
    if (restante > 0) await esperar(restante);
    this.ultimaPeticion = Date.now();
  }

  async pedir(ruta, opciones = {}) {
    const { metodo = 'GET', cuerpo, cabeceras = {}, autenticada = true, contar = true } = opciones;

    if (contar) {
      if (this.peticiones >= LIMITE_PETICIONES) {
        throw new Error(`Presupuesto de ${LIMITE_PETICIONES} peticiones agotado; se omitió ${ruta}.`);
      }
      this.peticiones += 1;
      await this.esperarTurno();
    }

    const url = new URL(ruta, this.base).toString();
    const headers = { 'User-Agent': USER_AGENT, Accept: 'application/json, text/html', ...cabeceras };
    if (autenticada && this.cookies.size > 0) headers.Cookie = this.cabeceraCookie();

    try {
      return await fetch(url, {
        method: metodo,
        body: cuerpo,
        headers,
        redirect: 'manual',
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
    } catch (error) {
      const detalle = error?.name === 'TimeoutError'
        ? `tiempo de espera agotado (${TIMEOUT_MS / 1000} s)`
        : (error?.message ?? 'error desconocido');
      throw new Error(`Error de red en ${ruta}: ${detalle}`);
    }
  }
}

/**
 * Detecta una página de login real. No basta con buscar cadenas como
 * `ng-controller="login"`: las páginas autenticadas las incluyen dentro del
 * script de comprobación de sesión (`/session/ping`).
 */
function pareceLogin(html) {
  const cuerpo = html.match(/<body[^>]*>/i)?.[0] ?? '';
  if (/ng-controller\s*=\s*["']login["']/i.test(cuerpo)) return true;

  return html.split(/\r?\n/).some((linea) =>
    /^\s*<(?:button|form|div|input)\b/i.test(linea) && /id\s*=\s*["']m_login_signin_submit["']/i.test(linea));
}

async function guardar(nombre, respuesta, registro, ruta, extensionSugerida) {
  const texto = await respuesta.text();
  const tipo = respuesta.headers.get('content-type') ?? '';
  let esJson = /json/i.test(tipo);
  let analizado = null;

  if (esJson) {
    try {
      analizado = JSON.parse(texto);
    } catch {
      esJson = false;
    }
  } else {
    try {
      analizado = JSON.parse(texto);
      esJson = true;
    } catch {
      // No es JSON.
    }
  }

  const extension = esJson ? 'json' : (extensionSugerida ?? 'html');
  const archivo = `${nombre}.${extension}`;
  await writeFile(path.join(DIR_SALIDA, archivo), texto, 'utf8');

  const entrada = {
    nombre,
    ruta,
    status: respuesta.status,
    bytes: Buffer.byteLength(texto, 'utf8'),
    contentType: tipo || null,
    archivo,
    esJson,
  };
  registro.push(entrada);

  return { texto, esJson, analizado, archivo, entrada };
}

function comprobarSesion(respuesta, guardada, ruta) {
  if (respuesta.status === 401 || respuesta.status === 403) {
    salir(2, `${ruta} devolvió HTTP ${respuesta.status}. Se detiene para no insistir.`);
  }
  if (respuesta.status >= 300 && respuesta.status < 400) {
    const destino = respuesta.headers.get('location') ?? '(sin Location)';
    salir(2, `${ruta} devolvió HTTP ${respuesta.status} → ${destino}. Se detiene (posible sesión expirada).`);
  }
  if (!guardada.esJson && pareceLogin(guardada.texto)) {
    salir(2, `${ruta} devolvió la página de login. La sesión expiró; se detiene.`);
  }
}

/* ------------------------- Análisis estático ------------------------- */

function extraerRutasWeb(texto) {
  const encontradas = [];
  const lineas = texto.split(/\r?\n/);
  const re = /\/web\/[A-Za-z0-9_\-./]*(?:\?[^\s"'<>)]*)?/g;

  lineas.forEach((linea, indice) => {
    let coincidencia;
    re.lastIndex = 0;
    while ((coincidencia = re.exec(linea)) !== null) {
      const ruta = coincidencia[0].replace(/[.,;]+$/, '');
      encontradas.push({ ruta, linea: indice + 1 });
    }
  });

  return encontradas;
}

function extraerScripts(texto) {
  const scripts = [];
  const re = /<script\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>/gi;
  let coincidencia;
  while ((coincidencia = re.exec(texto)) !== null) {
    const src = coincidencia[1];
    if (/^https?:|^\/\//i.test(src)) continue;
    scripts.push(src);
  }
  return scripts;
}

/**
 * Normaliza concatenaciones tipo `'/web/section/' + id + '/activities'`
 * a `'/web/section/{id}/activities'` para poder extraer la plantilla.
 */
function normalizarConcatenaciones(linea) {
  return linea
    .replace(/['"]\s*\+\s*[A-Za-z_$][\w.$]*\s*\+\s*['"]/g, '{id}')
    .replace(/['"]\s*\+\s*[A-Za-z_$][\w.$]*\s*\+\s*['"]/g, '{id}');
}

const PATRONES_ENDPOINT = [
  { tipo: 'fetch', re: /fetch\(\s*['"`]([^'"`\n]+)['"`]/g },
  { tipo: '$http', re: /\$http\.(?:get|post|put|delete|patch)\(\s*['"`]([^'"`\n]+)['"`]/g },
  { tipo: 'url', re: /(?:url|Url|URL)\s*:\s*['"`]([^'"`\n]+)['"`]/g },
  { tipo: 'literal', re: /['"`](\/(?:web|api|periodos-academicos|session)[^'"`\n]*)['"`]/g },
  { tipo: 'metodo', re: /\.(?:get|post|put|delete|patch)\(\s*['"`]([^'"`\n]+)['"`]/g },
];

function extraerEndpointsJs(texto, archivo) {
  const encontrados = [];
  const vistos = new Set();
  const lineas = texto.split(/\r?\n/);

  lineas.forEach((lineaOriginal, indice) => {
    const linea = normalizarConcatenaciones(lineaOriginal);
    for (const patron of PATRONES_ENDPOINT) {
      patron.re.lastIndex = 0;
      let coincidencia;
      while ((coincidencia = patron.re.exec(linea)) !== null) {
        const ruta = coincidencia[1].trim();
        if (!ruta.startsWith('/')) continue;
        const clave = `${archivo}:${indice + 1}:${ruta}`;
        if (vistos.has(clave)) continue;
        vistos.add(clave);
        const contexto = [linea, lineas[indice + 1], lineas[indice + 2]]
          .filter(Boolean)
          .join(' ')
          .trim()
          .slice(0, 320);
        encontrados.push({ archivo, linea: indice + 1, tipo: patron.tipo, ruta, contexto });
      }
    }
  });

  return encontrados;
}

function puntuarJs(ruta) {
  const r = ruta.toLowerCase();
  let puntos = 0;
  if (/\/views\/calendar\//.test(r)) puntos += 20;
  if (/(task|tarea|homework|activity|actividad|assignment|eva)/.test(r)) puntos += 14;
  if (/(course|curso)/.test(r)) puntos += 12;
  if (/(section|seccion)/.test(r)) puntos += 9;
  if (/controller/.test(r)) puntos += 6;
  if (/calendar/.test(r)) puntos += 6;
  if (/\.min\.js$/.test(r)) puntos -= 8;
  if (/bundle/.test(r)) puntos -= 12;
  if (/(google|jquery|owl|quick-nav|upload)/.test(r)) puntos -= 25;
  return puntos;
}

function seleccionarJs(candidatos) {
  const porRuta = new Map();
  for (const candidato of candidatos) {
    const clave = candidato.src.split('?')[0];
    const previo = porRuta.get(clave);
    if (!previo || puntuarJs(candidato.src) > puntuarJs(previo.src)) porRuta.set(clave, candidato);
  }
  return [...porRuta.values()]
    .filter((c) => puntuarJs(c.src) > 0)
    .sort((a, b) => puntuarJs(b.src) - puntuarJs(a.src))
    .slice(0, MAX_JS);
}

/* ------------------------- Análisis de respuestas ------------------------- */

function clavesNivel1(payload) {
  if (Array.isArray(payload)) return `[array de ${payload.length}]`;
  if (payload && typeof payload === 'object') return Object.keys(payload);
  return typeof payload;
}

function describirArray(lista, profundidad = 0) {
  const descripcion = { elementos: lista.length, clavesPrimero: null, ejemplo: null };
  const primero = lista[0];
  if (primero && typeof primero === 'object' && !Array.isArray(primero)) {
    descripcion.clavesPrimero = Object.keys(primero);
    descripcion.ejemplo = anonimizar(primero);
  } else if (profundidad < 2) {
    const anidado = lista.find((item) => item && typeof item === 'object' && !Array.isArray(item));
    if (anidado) {
      descripcion.clavesPrimero = Object.keys(anidado);
      descripcion.ejemplo = anonimizar(anidado);
    }
  }
  return descripcion;
}

const CLAVES_PERSONALES = /^(name|nombre|fullName|student|alumno|studentName|username|email|correo|picture|dni|document)$/i;

function anonimizar(valor, profundidad = 0) {
  if (profundidad > 3) return '…';
  if (Array.isArray(valor)) return `[${valor.length} elementos]`;
  if (!valor || typeof valor !== 'object') return valor;
  const salida = {};
  for (const [clave, contenido] of Object.entries(valor)) {
    if (CLAVES_PERSONALES.test(clave) && typeof contenido === 'string') {
      salida[clave] = '[anonimizado]';
    } else {
      salida[clave] = anonimizar(contenido, profundidad + 1);
    }
  }
  return salida;
}

function resumirEstructura(payload, profundidad = 0) {
  if (profundidad > 2) return '…';
  if (Array.isArray(payload)) return `array[${payload.length}]`;
  if (!payload || typeof payload !== 'object') return typeof payload;
  const salida = {};
  for (const [clave, valor] of Object.entries(payload)) {
    salida[clave] = Array.isArray(valor) ? `array[${valor.length}]` : resumirEstructura(valor, profundidad + 1);
  }
  return salida;
}

function arraysDeNivel1(payload) {
  const salida = {};
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return salida;
  for (const [clave, valor] of Object.entries(payload)) {
    if (Array.isArray(valor)) salida[clave] = describirArray(valor);
  }
  return salida;
}

function buscarIdSeccion(payload) {
  if (!payload || typeof payload !== 'object') return null;
  const candidatos = [payload.id, payload.sectionId, payload.section?.id, payload.data?.id, payload.data?.sectionId];
  return candidatos.find((valor) => typeof valor === 'string' && valor.length > 8) ?? null;
}

function prioridadTarea(ruta) {
  const r = ruta.toLowerCase();
  if (r === '/web/homework/list') return 100;
  if (/qualification/.test(r)) return 5;
  if (/^\/web\/homework\/(?:get|delete|upload|scores)/.test(r)) return 0;
  if (/homework|tarea/.test(r)) return 40;
  if (/(^|\/)(activities|actividades|activity)(\/|$)/.test(r)) return 35;
  if (/assignment/.test(r)) return 30;
  if (/evaluation|evaluacion/.test(r)) return 20;
  if (actividadesPendientes(r)) return 10;
  return 0;
}

function actividadesPendientes(ruta) {
  return /actividades-pendientes/i.test(ruta);
}

/**
 * Comprueba que la línea de JS solo pida el parámetro `s`.
 * Evita endpoints que además necesitan `h`, `st`, etc.
 */
function soloParametroS(contexto) {
  const coincidencia = contexto.match(/params\s*:\s*\{([^}]*)\}/);
  if (!coincidencia) return false;
  const claves = [...coincidencia[1].matchAll(/["']?([A-Za-z_$][\w$]*)["']?\s*:/g)]
    .map((m) => m[1].toLowerCase());
  return claves.length === 1 && claves[0] === 's';
}

/**
 * Elige UNA plantilla de endpoint relacionada con tareas/actividades y le
 * inyecta el id de sección. Solo acepta rutas con evidencia explícita:
 *   - placeholder en la propia ruta (`{id}`, `{{...}}`, `:id`), o
 *   - `params: { s: ... }` en la línea del JS.
 */
function endpointDerivado(endpoints, idSeccion) {
  if (!idSeccion) return null;
  const id = encodeURIComponent(idSeccion);

  const candidatos = endpoints
    .filter((e) => prioridadTarea(e.ruta) > 0)
    .map((e) => {
      let ruta = e.ruta;
      if (/\{\{[^}]+\}\}/.test(ruta)) ruta = ruta.replace(/\{\{[^}]+\}\}/, id);
      else if (/\{id\}/i.test(ruta)) ruta = ruta.replace(/\{id\}/i, id);
      else if (/:id\b/.test(ruta)) ruta = ruta.replace(/:id\b/, id);
      else if (/[?&]s=\s*$/i.test(ruta)) ruta = ruta.replace(/[?&]s=\s*$/i, `?s=${id}`);
      else if (soloParametroS(e.contexto)) {
        ruta = `${ruta}${ruta.includes('?') ? '&' : '?'}s=${id}`;
      } else return null;

      if (!ruta.includes(idSeccion)) return null;
      return { ruta, origen: e, prioridad: prioridadTarea(e.ruta) };
    })
    .filter(Boolean)
    .sort((a, b) => b.prioridad - a.prioridad);

  const elegido = candidatos.find((c) => !actividadesPendientes(c.origen.ruta)) ?? candidatos[0] ?? null;
  return elegido;
}

function evidenciaCalendario(endpoints) {
  const pistas = endpoints.filter((e) => /calendar/i.test(e.ruta));
  const conParametros = pistas.filter((e) => /[?&](start|end|from|to|term|termId|periodo|period|date)=/i.test(e.ruta));
  return { pistas, conParametros };
}

function rutaCalendario(plantilla, idPeriodo) {
  return plantilla.replace(
    /([?&])(start|end|from|to|term|termId|periodo|period|date)=([^&]*)/gi,
    (todo, separador, nombre) => {
      if (/^(term|termId|periodo|period)$/i.test(nombre)) {
        return `${separador}${nombre}=${encodeURIComponent(idPeriodo)}`;
      }
      return todo;
    },
  );
}

/* ------------------------------- Principal ------------------------------- */

async function principal() {
  const opciones = analizarArgumentos(process.argv.slice(2));

  if (opciones.ayuda) {
    mostrarAyuda();
    process.exit(0);
  }

  await cargarEnvLocal();

  const usuario = process.env.AULA_USER?.trim();
  const contrasena = process.env.AULA_PASS;

  if (opciones.dryRun) {
    console.log('Plan (sin red):');
    console.log('  1) GET /web/course?s=<sectionId>  (descubrir controladores de curso)');
    console.log('  2) GET /web/homework?s=<sectionId>  (página de tareas del curso)');
    console.log(`  3) GET hasta ${MAX_JS} archivos JS propios`);
    console.log('  4) GET /web/section/get?s=<sectionId> x2');
    console.log('  5) GET 1 endpoint derivado (solo con plantilla o params claros en JS)');
    console.log('  6) GET /web/calendar (solo si el JS muestra parámetros de periodo)');
    console.log(`  Presupuesto: ${LIMITE_PETICIONES} peticiones + 1 login.`);
    process.exit(0);
  }

  if (!usuario || !contrasena) {
    salir(
      1,
      'Faltan credenciales.\n' +
        'Edita tools/aula/.env (copia tools/aula/.env.example) o define AULA_USER y AULA_PASS.\n' +
        'No se muestran credenciales en la salida.',
    );
  }

  await mkdir(DIR_SALIDA, { recursive: true });

  /* ---------- 1. Análisis estático local ---------- */
  const fuentesHtml = ['home.html', 'extra-1.html'];
  const rutasHtml = [];
  const scripts = [];

  for (const archivo of fuentesHtml) {
    let texto;
    try {
      texto = await readFile(path.join(DIR_SALIDA, archivo), 'utf8');
    } catch (error) {
      if (error?.code === 'ENOENT') {
        console.warn(`Aviso: no existe out/${archivo}; se omite.`);
        continue;
      }
      throw error;
    }
    for (const hallazgo of extraerRutasWeb(texto)) rutasHtml.push({ archivo, ...hallazgo });
    for (const src of extraerScripts(texto)) scripts.push({ origen: archivo, src });
  }

  let cursos = [];
  try {
    const crudo = JSON.parse(await readFile(path.join(DIR_SALIDA, 'courseinrole.json'), 'utf8'));
    cursos = Array.isArray(crudo) ? crudo : [];
  } catch (error) {
    if (error?.code !== 'ENOENT') throw error;
  }
  if (cursos.length < 2) salir(1, 'Se necesitan al menos 2 cursos en out/courseinrole.json.');

  let periodos = [];
  try {
    const crudo = JSON.parse(await readFile(path.join(DIR_SALIDA, 'periodos-academicos.json'), 'utf8'));
    periodos = Array.isArray(crudo) ? crudo : [];
  } catch (error) {
    if (error?.code !== 'ENOENT') throw error;
  }
  const periodoSeleccionado = periodos.find((p) => p.selected) ?? null;

  /* ---------- Login ---------- */
  const sesion = new Sesion(opciones.base);

  console.log(`Descubrimiento del Aula Virtual — ${opciones.base}`);
  console.log('Iniciando sesión (un solo intento)...');

  const formulario = new FormData();
  formulario.set('username', usuario);
  formulario.set('password', contrasena);

  let respuestaLogin;
  try {
    respuestaLogin = await sesion.pedir('/account/login', {
      metodo: 'POST',
      cuerpo: formulario,
      autenticada: false,
      contar: false,
    });
  } catch (error) {
    salir(4, `${error.message}\nNo se reintentará.`);
  }

  sesion.guardarCookies(respuestaLogin.headers);
  const textoLogin = await respuestaLogin.text();

  let jsonLogin = null;
  try {
    jsonLogin = JSON.parse(textoLogin);
  } catch {
    // HTML o redirección.
  }

  const redirige = respuestaLogin.status >= 300 && respuestaLogin.status < 400;
  if (redirige && sesion.cookies.size > 0) {
    console.warn(`Aviso: login HTTP ${respuestaLogin.status} con cookie de sesión; se continúa.`);
  } else if (!jsonLogin) {
    await writeFile(path.join(DIR_SALIDA, 'login-error.html'), textoLogin, 'utf8');
    salir(3, `El login devolvió HTML (HTTP ${respuestaLogin.status}). Revisa out/login-error.html.`);
  } else if (jsonLogin.status !== true) {
    salir(2, 'Login rechazado. No se reintentará.');
  }

  if (sesion.cookies.size === 0) salir(3, 'El login no devolvió cookies de sesión.');

  console.log('Sesión iniciada. Iniciando descubrimiento (1 petición/segundo)...');

  const registro = [];

  /* ---------- 2. Página de curso ---------- */
  const curso1 = cursos[0];
  const rutaCurso = `/web/course?s=${encodeURIComponent(curso1.sectionId)}`;
  let htmlCurso = '';
  try {
    const respuesta = await sesion.pedir(rutaCurso);
    const guardada = await guardar(`course-${curso1.codeCourse}-${curso1.codeSection}`, respuesta, registro, rutaCurso);
    comprobarSesion(respuesta, guardada, rutaCurso);
    htmlCurso = guardada.texto;
    for (const src of extraerScripts(htmlCurso)) scripts.push({ origen: 'course', src });
    for (const hallazgo of extraerRutasWeb(htmlCurso)) rutasHtml.push({ archivo: guardada.archivo, ...hallazgo });
    console.log(`  GET ${rutaCurso} → HTTP ${respuesta.status} (${guardada.archivo})`);
  } catch (error) {
    console.error(`  ${error.message}`);
  }

  /* ---------- 3. Página de tareas del curso ---------- */
  const rutaTareas = `/web/homework?s=${encodeURIComponent(curso1.sectionId)}`;
  try {
    const respuesta = await sesion.pedir(rutaTareas);
    const guardada = await guardar(`homework-${curso1.codeCourse}-${curso1.codeSection}`, respuesta, registro, rutaTareas);
    comprobarSesion(respuesta, guardada, rutaTareas);
    for (const src of extraerScripts(guardada.texto)) scripts.push({ origen: guardada.archivo, src });
    for (const hallazgo of extraerRutasWeb(guardada.texto)) rutasHtml.push({ archivo: guardada.archivo, ...hallazgo });
    console.log(`  GET ${rutaTareas} → HTTP ${respuesta.status} (${guardada.archivo})`);
  } catch (error) {
    console.error(`  ${error.message}`);
  }

  /* ---------- 4. Descarga de JS ---------- */
  const seleccionados = seleccionarJs(scripts);
  const endpointsJs = [];

  console.log(`JS candidatos: ${seleccionados.length} de ${scripts.length} referencias.`);
  for (const [indice, candidato] of seleccionados.entries()) {
    const nombre = `js-${indice + 1}`;
    try {
      const respuesta = await sesion.pedir(candidato.src);
      const guardada = await guardar(nombre, respuesta, registro, candidato.src, 'js');
      if (respuesta.status === 401 || respuesta.status === 403) {
        comprobarSesion(respuesta, guardada, candidato.src);
      }
      endpointsJs.push(...extraerEndpointsJs(guardada.texto, guardada.archivo));
      console.log(`  GET ${candidato.src} → HTTP ${respuesta.status} (${guardada.archivo}, ${guardada.entrada.bytes} B)`);
    } catch (error) {
      console.error(`  ${error.message}`);
    }
  }

  /* ---------- 5. Secciones ---------- */
  const secciones = [];
  for (const [indice, curso] of cursos.slice(0, 2).entries()) {
    const ruta = `/web/section/get?s=${encodeURIComponent(curso.sectionId)}`;
    const nombre = `section-${curso.codeSection}-${curso.codeCourse}`;
    try {
      const respuesta = await sesion.pedir(ruta);
      const guardada = await guardar(nombre, respuesta, registro, ruta);
      comprobarSesion(respuesta, guardada, ruta);
      secciones.push({
        curso: { codeCourse: curso.codeCourse, codeSection: curso.codeSection },
        ruta,
        status: respuesta.status,
        contentType: guardada.entrada.contentType,
        bytes: guardada.entrada.bytes,
        archivo: guardada.archivo,
        esJson: guardada.esJson,
        clavesNivel1: guardada.esJson ? clavesNivel1(guardada.analizado) : null,
        arrays: guardada.esJson ? arraysDeNivel1(guardada.analizado) : null,
        estructura: guardada.esJson ? resumirEstructura(guardada.analizado) : null,
        idSeccion: guardada.esJson ? buscarIdSeccion(guardada.analizado) : null,
      });
      console.log(`  GET ${ruta} → HTTP ${respuesta.status} (${guardada.archivo}, ${guardada.entrada.bytes} B)`);
    } catch (error) {
      console.error(`  ${error.message}`);
      secciones.push({ curso: { codeCourse: curso.codeCourse, codeSection: curso.codeSection }, ruta, error: error.message });
    }
  }

  /* ---------- 6. Endpoint derivado ---------- */
  let derivado = null;
  const idSeccion = secciones.find((s) => s.idSeccion)?.idSeccion ?? null;
  const candidatoDerivado = endpointDerivado(endpointsJs, idSeccion);

  if (!candidatoDerivado) {
    console.log('Endpoint derivado: sin plantilla inequívoca en el JS; no se prueba (no se adivina).');
  } else {
    try {
      const respuesta = await sesion.pedir(candidatoDerivado.ruta);
      const guardada = await guardar('derivado-1', respuesta, registro, candidatoDerivado.ruta);
      comprobarSesion(respuesta, guardada, candidatoDerivado.ruta);
      derivado = {
        ruta: candidatoDerivado.ruta,
        origen: `${candidatoDerivado.origen.archivo}:${candidatoDerivado.origen.linea}`,
        plantillaOriginal: candidatoDerivado.origen.ruta,
        status: respuesta.status,
        contentType: guardada.entrada.contentType,
        bytes: guardada.entrada.bytes,
        archivo: guardada.archivo,
        esJson: guardada.esJson,
        clavesNivel1: guardada.esJson ? clavesNivel1(guardada.analizado) : null,
        arrays: guardada.esJson ? arraysDeNivel1(guardada.analizado) : null,
      };
      console.log(`  GET ${candidatoDerivado.ruta} → HTTP ${respuesta.status} (${guardada.archivo})`);
    } catch (error) {
      console.error(`  ${error.message}`);
      derivado = { ruta: candidatoDerivado.ruta, error: error.message };
    }
  }

  /* ---------- 7. Calendario ---------- */
  const calendario = evidenciaCalendario(endpointsJs);
  let llamadaCalendario = null;

  if (calendario.conParametros.length === 0) {
    console.log(`Calendario: ${calendario.pistas.length} pista(s) en JS, pero sin parámetros de periodo; no se llama.`);
  } else if (!periodoSeleccionado) {
    console.log('Calendario: el JS sugiere parámetros, pero no hay periodo seleccionado; no se llama.');
  } else {
    const plantilla = calendario.conParametros[0].ruta;
    const ruta = rutaCalendario(plantilla, periodoSeleccionado.id);
    try {
      const respuesta = await sesion.pedir(ruta);
      const guardada = await guardar('calendar-periodo', respuesta, registro, ruta);
      comprobarSesion(respuesta, guardada, ruta);
      llamadaCalendario = {
        ruta,
        plantillaOriginal: plantilla,
        periodo: periodoSeleccionado.text,
        status: respuesta.status,
        contentType: guardada.entrada.contentType,
        bytes: guardada.entrada.bytes,
        archivo: guardada.archivo,
        esJson: guardada.esJson,
        clavesNivel1: guardada.esJson ? clavesNivel1(guardada.analizado) : null,
        arrays: guardada.esJson ? arraysDeNivel1(guardada.analizado) : null,
      };
      console.log(`  GET ${ruta} → HTTP ${respuesta.status} (${guardada.archivo})`);
    } catch (error) {
      console.error(`  ${error.message}`);
      llamadaCalendario = { ruta, error: error.message };
    }
  }

  /* ---------- Informe ---------- */
  const rutasWebUnicas = [...new Map(rutasHtml.map((r) => [r.ruta, r])).values()];
  const endpointsUnicos = [...new Map(endpointsJs.map((e) => [`${e.ruta}@${e.archivo}:${e.linea}`, e])).values()];
  const relevantes = endpointsUnicos.filter((e) =>
    /tarea|task|homework|actividad|activity|assignment|evaluacion|evaluation|foro|forum|contenido|content|calendar/i.test(e.ruta));

  const informe = {
    generadoEn: new Date().toISOString(),
    base: opciones.base,
    periodoSeleccionado: periodoSeleccionado ? { text: periodoSeleccionado.text, id: periodoSeleccionado.id } : null,
    peticiones: registro,
    presupuesto: { limite: LIMITE_PETICIONES, usadas: sesion.peticiones },
    rutasWebEnHtml: rutasWebUnicas,
    jsDescargados: seleccionados.map((s) => ({ origen: s.origen, src: s.src })),
    endpointsJs: endpointsUnicos,
    endpointsRelevantes: relevantes,
    secciones,
    derivado,
    calendario: {
      pistas: calendario.pistas.map((p) => ({ archivo: p.archivo, linea: p.linea, ruta: p.ruta })),
      conParametros: calendario.conParametros.map((p) => ({ archivo: p.archivo, linea: p.linea, ruta: p.ruta })),
      llamada: llamadaCalendario,
    },
  };

  await writeFile(
    path.join(DIR_SALIDA, 'tareas-descubrimiento.json'),
    `${JSON.stringify(informe, null, 2)}\n`,
    'utf8',
  );

  console.log('');
  console.log('Resumen:');
  console.log(`  Peticiones usadas: ${sesion.peticiones}/${LIMITE_PETICIONES}`);
  console.log(`  JS descargados: ${seleccionados.length}`);
  console.log(`  Endpoints hallados en JS: ${endpointsUnicos.length} (relevantes: ${relevantes.length})`);
  console.log(`  Secciones consultadas: ${secciones.filter((s) => !s.error).length}`);
  console.log(`  Endpoint derivado: ${derivado ? derivado.ruta : 'no probado'}`);
  console.log(`  Calendario: ${llamadaCalendario ? llamadaCalendario.ruta : 'no probado'}`);
  console.log(`  Informe: ${path.relative(process.cwd(), path.join(DIR_SALIDA, 'tareas-descubrimiento.json'))}`);
  console.log('Listo. No se muestran credenciales ni cookies.');
}

principal().catch((error) => {
  salir(4, `Error inesperado: ${error?.message ?? error}\nNo se reintentará.`);
});
