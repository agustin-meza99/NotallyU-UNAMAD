#!/usr/bin/env node
/**
 * Explorador local del Aula Virtual de UNAMAD.
 *
 * Solo usa módulos nativos de Node 24. No hay dependencias npm.
 * Hace un único intento de login por ejecución (el servidor bloquea
 * temporalmente tras varios intentos fallidos).
 */

import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const ARCHIVO_ENV = path.join(AQUI, '.env');
const DIR_SALIDA = path.join(AQUI, 'out');
const BASE_POR_DEFECTO = 'https://aula-virtual.unamad.edu.pe';
const USER_AGENT = 'NotallyExplorer/1.0';
const TIMEOUT_MS = 20_000;
const RETARDO_MS = 1_000;
const MAX_SECCIONES = 2;
const MAX_EXTRA = 2;
const LIMITE_TOTAL = 12;

const ENDPOINTS = [
  { nombre: 'user-info-data', ruta: '/web/user/info/data' },
  { nombre: 'system-courseinrole', ruta: '/web/user/info/system/courseinrole' },
  { nombre: 'courseinrole', ruta: '/web/user/info/courseinrole' },
  { nombre: 'calendar-list', ruta: '/web/calendar/list' },
  { nombre: 'periodos-academicos', ruta: '/periodos-academicos/disponibles/get' },
  { nombre: 'general-announcements', ruta: '/web/SystemInfo/generalAnnouncements' },
];

const FILTROS_ENLACES = [
  '/section/',
  '/web/',
  '/curso',
  '/tarea',
  '/actividad',
  '/evaluacion',
  '/foro',
];

function salir(codigo, mensaje) {
  if (mensaje) console.error(mensaje);
  process.exit(codigo);
}

/**
 * Carga tools/aula/.env (si existe) sin dependencias externas.
 * Solo define variables que aún no estén en process.env, para no pisar
 * valores pasados por el shell ni por `node --env-file`.
 */
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
  const opciones = { base: BASE_POR_DEFECTO, dryRun: false, ayuda: false, seguir: false };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--dry-run') {
      opciones.dryRun = true;
    } else if (arg === '--seguir') {
      opciones.seguir = true;
    } else if (arg === '--help' || arg === '-h') {
      opciones.ayuda = true;
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
  console.log(`Explorador local del Aula Virtual de UNAMAD

Uso:
  node tools/aula/explore.mjs
  node tools/aula/explore.mjs --dry-run
  node tools/aula/explore.mjs --seguir
  node tools/aula/explore.mjs --base https://aula-virtual.unamad.edu.pe

Credenciales:
  Rellena tools/aula/.env (copia tools/aula/.env.example) o define
  AULA_USER y AULA_PASS en el entorno.

Opciones:
  --base <url>   URL base del Aula Virtual (por defecto ${BASE_POR_DEFECTO})
  --dry-run      Valida el entorno y muestra el plan sin hacer login
  --seguir       Tras el login, consulta hasta ${MAX_EXTRA} enlaces candidatos a tareas
  -h, --help     Muestra esta ayuda

Variables de entorno:
  AULA_USER      Usuario del Aula Virtual
  AULA_PASS      Contraseña del Aula Virtual

Salida:
  tools/aula/out/  Respuestas crudas, enlaces.json y resumen.json (ignorada por git)

Códigos de salida:
  0  correcto
  1  configuración o argumentos inválidos
  2  credenciales rechazadas por el servidor
  3  el flujo de login cambió (respuesta HTML inesperada)
  4  error de red

Nota: se hace un solo intento de login por ejecución.`);
}

function mostrarPlan(base) {
  console.log('Plan de peticiones (modo --dry-run, no se hará red):');
  console.log(`  Base: ${base}`);
  console.log('  Login: POST /account/login (un solo intento, redirect manual)');
  console.log('  Autenticadas (1 petición/segundo, timeout 20 s):');
  console.log('    GET /');
  for (const endpoint of ENDPOINTS) {
    console.log(`    GET ${endpoint.ruta}`);
  }
  console.log(`    GET /web/section/get?s=<id>  (hasta ${MAX_SECCIONES} secciones detectadas en /)`);
  console.log(`  Seguimiento (--seguir): hasta ${MAX_EXTRA} enlaces candidatos (tarea/actividad/evaluacion/section/curso)`);
  console.log(`  Salida: ${path.join('tools', 'aula', 'out')}`);
  console.log('Entorno validado. No se ha contactado el servidor.');
}

function esperar(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function extraerCookies(headers) {
  let crudas = [];
  if (typeof headers.getSetCookie === 'function') {
    crudas = headers.getSetCookie();
  }
  if (!crudas || crudas.length === 0) {
    const combinadas = headers.get('set-cookie');
    if (combinadas) {
      crudas = combinadas.split(/,(?=\s*[!#$%&'*+\-.^_`|~0-9A-Za-z]+=)/);
    }
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
    const { metodo = 'GET', cuerpo, cabeceras = {}, autenticada = true, limitar = true } = opciones;
    if (limitar) await this.esperarTurno();

    const url = new URL(ruta, this.base).toString();
    const headers = {
      'User-Agent': USER_AGENT,
      Accept: 'application/json, text/html',
      ...cabeceras,
    };
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

async function guardarRespuesta(nombre, respuesta, resumenPeticiones, ruta) {
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
      // No es JSON: se guarda como HTML.
    }
  }

  const extension = esJson ? 'json' : 'html';
  const archivo = `${nombre}.${extension}`;
  await writeFile(path.join(DIR_SALIDA, archivo), texto, 'utf8');

  resumenPeticiones.push({
    nombre,
    ruta,
    status: respuesta.status,
    bytes: Buffer.byteLength(texto, 'utf8'),
    contentType: tipo || null,
    archivo,
  });

  return { texto, esJson, analizado, archivo };
}

function buscarLista(payload, claves, profundidad = 0) {
  if (payload == null || profundidad > 5) return null;
  if (Array.isArray(payload)) return payload;
  if (typeof payload !== 'object') return null;

  for (const clave of claves) {
    if (Array.isArray(payload[clave])) return payload[clave];
  }
  for (const clave of ['data', 'result', 'items']) {
    const encontrada = buscarLista(payload[clave], claves, profundidad + 1);
    if (encontrada) return encontrada;
  }
  return null;
}

function contarCursos(datos) {
  for (const nombre of ['courseinrole', 'system-courseinrole']) {
    const lista = buscarLista(datos[nombre], ['courses', 'cursos', 'courseinrole', 'data']);
    if (lista) return lista.length;
  }
  return 0;
}

function contarEventos(datos) {
  const lista = buscarLista(datos['calendar-list'], ['events', 'eventos', 'calendar', 'data']);
  return lista ? lista.length : 0;
}

function normalizarEnlace(href, base) {
  const limpio = href.trim().replace(/&amp;/g, '&');
  if (!limpio || limpio.startsWith('#') || /^(javascript|mailto|tel):/i.test(limpio)) return null;

  try {
    const url = new URL(limpio, `${base}/`);
    if (url.origin !== new URL(base).origin) return null;
    return url.pathname + url.search;
  } catch {
    return null;
  }
}

function extraerEnlaces(html, base, idsSecciones) {
  const encontrados = new Map();
  const regex = /href\s*=\s*["']([^"']+)["']/gi;
  let coincidencia;

  while ((coincidencia = regex.exec(html)) !== null) {
    const ruta = normalizarEnlace(coincidencia[1], base);
    if (!ruta) continue;

    const minuscula = ruta.toLowerCase();
    if (!FILTROS_ENLACES.some((filtro) => minuscula.includes(filtro))) continue;

    if (!encontrados.has(ruta)) {
      encontrados.set(ruta, {
        ruta,
        url: new URL(ruta, base).toString(),
        tipo: clasificarEnlace(minuscula),
      });
    }

    const seccion = ruta.match(/\/section\/(\d+)/i) ?? ruta.match(/[?&]s=(\d+)/i);
    if (seccion) idsSecciones.add(seccion[1]);
  }

  return [...encontrados.values()];
}

function clasificarEnlace(ruta) {
  if (ruta.includes('/section/')) return 'seccion';
  if (ruta.includes('/foro')) return 'foro';
  if (ruta.includes('/tarea')) return 'tarea';
  if (ruta.includes('/evaluacion')) return 'evaluacion';
  if (ruta.includes('/actividad')) return 'actividad';
  if (ruta.includes('/curso')) return 'curso';
  return 'web';
}

/**
 * Ordena los enlaces de la portada por probabilidad de contener tareas.
 * Excluye las rutas de sección ya consultadas para no repetir peticiones.
 */
function candidatosSeguimiento(enlaces, seccionesConsultadas) {
  const yaConsultadas = new Set(
    seccionesConsultadas.map((id) => `/web/section/get?s=${encodeURIComponent(id)}`),
  );

  const puntuar = (ruta) => {
    const r = ruta.toLowerCase();
    let puntos = 0;
    if (r.includes('tarea') || r.includes('assignment')) puntos += 10;
    if (r.includes('actividad') || r.includes('activity')) puntos += 8;
    if (r.includes('evaluacion') || r.includes('evaluation')) puntos += 6;
    if (r.includes('foro') || r.includes('forum')) puntos += 4;
    if (r.includes('/section/')) puntos += 3;
    if (r.includes('curso') || r.includes('course')) puntos += 2;
    if (r === '/' || r.startsWith('/web/user') || r.startsWith('/account')) puntos -= 10;
    return puntos;
  };

  return enlaces
    .filter((enlace) => !yaConsultadas.has(enlace.ruta))
    .map((enlace) => ({ ...enlace, puntos: puntuar(enlace.ruta) }))
    .filter((enlace) => enlace.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos || a.ruta.localeCompare(b.ruta))
    .slice(0, MAX_EXTRA);
}

async function principal() {
  const opciones = analizarArgumentos(process.argv.slice(2));

  if (opciones.ayuda) {
    mostrarAyuda();
    process.exit(0);
  }

  await cargarEnvLocal();

  const usuario = process.env.AULA_USER?.trim();
  const contrasena = process.env.AULA_PASS;

  if (!usuario || !contrasena) {
    salir(
      1,
      'Faltan credenciales.\n' +
        'Edita tools/aula/.env (copia tools/aula/.env.example si aún no existe) ' +
        'o define AULA_USER y AULA_PASS en el entorno.\n' +
        'Ejemplo (PowerShell):\n' +
        '  $env:AULA_USER="tu_usuario"; $env:AULA_PASS="tu_contraseña"\n' +
        '  node tools/aula/explore.mjs --dry-run',
    );
  }

  if (opciones.dryRun) {
    mostrarPlan(opciones.base);
    process.exit(0);
  }

  await mkdir(DIR_SALIDA, { recursive: true });

  const sesion = new Sesion(opciones.base);
  const resumenPeticiones = [];
  const datos = {};

  console.log(`Explorador del Aula Virtual — ${opciones.base}`);
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
      limitar: false,
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
    // Puede ser HTML (flujo cambiado) o una redirección.
  }

  const esRedireccion = respuestaLogin.status >= 300 && respuestaLogin.status < 400;

  if (esRedireccion && sesion.cookies.size > 0) {
    console.warn(`Aviso: el login respondió ${respuestaLogin.status} con cookie de sesión; se continúa.`);
  } else if (!jsonLogin) {
    const archivo = path.join(DIR_SALIDA, 'login-error.html');
    await writeFile(archivo, textoLogin, 'utf8');
    salir(
      3,
      `El login devolvió HTML (HTTP ${respuestaLogin.status}) en vez de JSON.\n` +
        `Se guardó ${path.relative(process.cwd(), archivo)}.\n` +
        'Probablemente el flujo de autenticación cambió; revisa el archivo y actualiza el explorador.',
    );
  } else if (jsonLogin.status !== true) {
    const mensaje = typeof jsonLogin.data === 'string' && jsonLogin.data
      ? jsonLogin.data
      : 'El servidor rechazó el inicio de sesión.';
    salir(2, `Login rechazado: ${mensaje}\nNo se reintentará (bloqueo temporal por intentos fallidos).`);
  }

  if (sesion.cookies.size === 0) {
    salir(3, 'El login no devolvió cookies de sesión. Probablemente cambió el flujo de autenticación.');
  }

  console.log('Sesión iniciada. Descargando endpoints (1 petición por segundo)...');

  for (const endpoint of ENDPOINTS) {
    try {
      const respuesta = await sesion.pedir(endpoint.ruta);
      const guardada = await guardarRespuesta(endpoint.nombre, respuesta, resumenPeticiones, endpoint.ruta);
      console.log(`  GET ${endpoint.ruta} → HTTP ${respuesta.status} (${guardada.archivo})`);
      if (guardada.esJson) datos[endpoint.nombre] = guardada.analizado;
    } catch (error) {
      console.error(`  ${error.message}`);
      resumenPeticiones.push({
        nombre: endpoint.nombre,
        ruta: endpoint.ruta,
        error: error.message,
      });
    }
  }

  let htmlHome = '';
  try {
    const respuestaHome = await sesion.pedir('/');
    const guardada = await guardarRespuesta('home', respuestaHome, resumenPeticiones, '/');
    htmlHome = guardada.texto;
    console.log(`  GET / → HTTP ${respuestaHome.status} (${guardada.archivo})`);
  } catch (error) {
    console.error(`  ${error.message}`);
    resumenPeticiones.push({ nombre: 'home', ruta: '/', error: error.message });
  }

  const idsSecciones = new Set();
  const enlaces = extraerEnlaces(htmlHome, opciones.base, idsSecciones);
  await writeFile(
    path.join(DIR_SALIDA, 'enlaces.json'),
    `${JSON.stringify({ base: opciones.base, total: enlaces.length, enlaces }, null, 2)}\n`,
    'utf8',
  );
  console.log(`Enlaces internos detectados: ${enlaces.length}`);

  const secciones = [...idsSecciones].slice(0, MAX_SECCIONES);
  const seccionesConsultadas = [];
  const extrasConsultadas = [];
  for (const id of secciones) {
    const ruta = `/web/section/get?s=${encodeURIComponent(id)}`;
    try {
      const respuesta = await sesion.pedir(ruta);
      const guardada = await guardarRespuesta(`section-${id}`, respuesta, resumenPeticiones, ruta);
      seccionesConsultadas.push(id);
      console.log(`  GET ${ruta} → HTTP ${respuesta.status} (${guardada.archivo})`);
    } catch (error) {
      console.error(`  ${error.message}`);
      resumenPeticiones.push({ nombre: `section-${id}`, ruta, error: error.message });
    }
  }

  if (opciones.seguir) {
    const candidatos = candidatosSeguimiento(enlaces, seccionesConsultadas);
    if (candidatos.length === 0) {
      console.log('Seguimiento: no hay enlaces candidatos (tarea/actividad/evaluacion/section/curso).');
    } else {
      console.log(`Seguimiento: consultando ${candidatos.length} enlace(s) candidato(s)...`);
    }
    for (const [indice, candidato] of candidatos.entries()) {
      const nombre = `extra-${indice + 1}`;
      try {
        const respuesta = await sesion.pedir(candidato.ruta);
        const guardada = await guardarRespuesta(nombre, respuesta, resumenPeticiones, candidato.ruta);
        extrasConsultadas.push({
          nombre,
          ruta: candidato.ruta,
          status: respuesta.status,
          archivo: guardada.archivo,
          esJson: guardada.esJson,
        });
        console.log(`  GET ${candidato.ruta} → HTTP ${respuesta.status} (${guardada.archivo})`);
      } catch (error) {
        console.error(`  ${error.message}`);
        resumenPeticiones.push({ nombre, ruta: candidato.ruta, error: error.message });
      }
    }
  }

  const infoUsuario = datos['user-info-data']?.data ?? datos['user-info-data'] ?? {};
  const usuarioResumen = {
    username: infoUsuario?.username ?? infoUsuario?.user ?? null,
    id: infoUsuario?.id ?? infoUsuario?.userId ?? null,
  };

  const resumen = {
    generadoEn: new Date().toISOString(),
    base: opciones.base,
    usuario: usuarioResumen,
    cursos: contarCursos(datos),
    eventos: contarEventos(datos),
    enlaces: enlaces.length,
    seccionesDetectadas: [...idsSecciones].length,
    seccionesConsultadas,
    extrasConsultadas,
    peticiones: resumenPeticiones,
  };

  await writeFile(
    path.join(DIR_SALIDA, 'resumen.json'),
    `${JSON.stringify(resumen, null, 2)}\n`,
    'utf8',
  );

  console.log('');
  console.log('Resumen:');
  console.log(`  Usuario: ${usuarioResumen.username ?? '(no disponible)'} (id: ${usuarioResumen.id ?? 'n/d'})`);
  console.log(`  Cursos: ${resumen.cursos}`);
  console.log(`  Eventos: ${resumen.eventos}`);
  console.log(`  Enlaces: ${resumen.enlaces}`);
  console.log(`  Secciones consultadas: ${seccionesConsultadas.length}`);
  console.log(`  Enlaces de seguimiento: ${extrasConsultadas.length}`);
  console.log(`  Archivos: ${path.relative(process.cwd(), DIR_SALIDA)}${path.sep}`);
  console.log('Listo. No se muestran credenciales ni cookies.');
}

principal().catch((error) => {
  salir(4, `Error inesperado: ${error?.message ?? error}\nNo se reintentará.`);
});
