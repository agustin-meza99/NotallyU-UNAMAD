#!/usr/bin/env node
/**
 * Seed idempotente de Firestore para Notally.
 *
 * Replica `data/SeedData.kt` (4 cursos y 11 tareas) usando la API REST de
 * Firestore como el propio usuario autenticado, de modo que se respetan
 * `firestore.rules`. No usa Admin SDK ni service accounts.
 *
 * Uso:
 *   $env:SEED_EMAIL = '...'
 *   $env:SEED_PASSWORD = '...'
 *   node tools/seed-firestore/seed.mjs [--dry-run] [--reset]
 *
 * Sin dependencias npm: solo `fetch` global de Node 18+ (probado en Node 24).
 */

import { readFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(SCRIPT_DIR, '..', '..');
const GOOGLE_SERVICES_PATH = path.join(REPO_ROOT, 'app', 'google-services.json');

const IDENTITY_BASE = 'https://identitytoolkit.googleapis.com/v1';
const FIRESTORE_BASE = 'https://firestore.googleapis.com/v1';
const DAY_MS = 86_400_000;

// ---------------------------------------------------------------------------
// Datos: espejo exacto de app/src/main/java/com/example/notally/data/SeedData.kt
// ---------------------------------------------------------------------------

const CURSOS = [
  {
    id: 'course-calculo',
    name: 'Cálculo III',
    professor: 'Dra. Mariana Quispe',
    credits: 4,
  },
  {
    id: 'course-estructuras',
    name: 'Estructuras de Datos',
    professor: 'Ing. Carlos Mendoza',
    credits: 5,
  },
  {
    id: 'course-bases-datos',
    name: 'Bases de Datos',
    professor: 'Mg. Lucía Fernández',
    credits: 4,
  },
  {
    id: 'course-etica',
    name: 'Ética Profesional',
    professor: 'Dr. Jorge Salazar',
    credits: 2,
  },
];

// `courseId` es el id remoto determinista del curso, no un índice.
const TASKS = [
  {
    id: 'task-proyecto-lexico',
    title: 'Proyecto 1: analizador léxico',
    notes: 'Entregar el repositorio con el lexer y el informe de pruebas.',
    courseId: 'course-estructuras',
    dueInDays: 3,
    priority: 'HIGH',
    status: 'PENDING',
    createdDaysAgo: 4,
    subtaskTitles: [
      'Definir la gramática del lenguaje',
      'Implementar el analizador léxico',
      'Escribir pruebas unitarias',
    ],
  },
  {
    id: 'task-taller-normalizacion',
    title: 'Taller de normalización de bases de datos',
    notes: 'Normalizar el esquema hasta 3FN y justificar cada decisión.',
    courseId: 'course-bases-datos',
    dueInDays: 1,
    dueHour: 10,
    dueMinute: 0,
    priority: 'MEDIUM',
    status: 'PENDING',
    createdDaysAgo: 1,
    subtaskTitles: [],
  },
  {
    id: 'task-parcial-calculo',
    title: 'Parcial de Cálculo III',
    notes: 'Capítulos 4 y 5: integrales múltiples y campos vectoriales.',
    courseId: 'course-calculo',
    dueInDays: 5,
    dueHour: 8,
    dueMinute: 0,
    priority: 'HIGH',
    status: 'PENDING',
    createdDaysAgo: 7,
    subtaskTitles: [],
  },
  {
    id: 'task-ensayo-consentimiento',
    title: 'Ensayo sobre consentimiento informado',
    notes: 'Mínimo 1500 palabras con al menos tres fuentes académicas.',
    courseId: 'course-etica',
    dueInDays: 7,
    priority: 'MEDIUM',
    status: 'PENDING',
    createdDaysAgo: 2,
    subtaskTitles: [],
  },
  {
    id: 'task-lab-avl',
    title: 'Laboratorio de árboles AVL',
    notes: 'Implementar inserción, eliminación y rotaciones.',
    courseId: 'course-estructuras',
    dueInDays: 0,
    dueHour: 18,
    dueMinute: 0,
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    createdDaysAgo: 3,
    subtaskTitles: ['Rotaciones simples', 'Rotaciones dobles'],
  },
  {
    id: 'task-consultas-sql',
    title: 'Consultas SQL avanzadas',
    notes: 'Resolver los diez ejercicios del cuadernillo 3.',
    courseId: 'course-bases-datos',
    dueInDays: 2,
    priority: 'MEDIUM',
    status: 'PENDING',
    createdDaysAgo: 1,
    subtaskTitles: ['JOINs', 'Subconsultas', 'Funciones de ventana'],
  },
  {
    id: 'task-series-fourier',
    title: 'Ejercicios de series de Fourier',
    notes: 'Página 210, ejercicios impares.',
    courseId: 'course-calculo',
    dueInDays: 1,
    dueHour: 7,
    dueMinute: 0,
    priority: 'LOW',
    status: 'PENDING',
    createdDaysAgo: 2,
    subtaskTitles: [],
  },
  {
    id: 'task-exposicion-sesgos',
    title: 'Exposición: sesgos algorítmicos',
    notes: 'Preparar diapositivas y un caso práctico para el debate.',
    courseId: 'course-etica',
    dueInDays: 9,
    dueHour: 14,
    dueMinute: 0,
    priority: 'MEDIUM',
    status: 'PENDING',
    createdDaysAgo: 1,
    subtaskTitles: [],
  },
  {
    id: 'task-mer',
    title: 'Diseño del modelo entidad-relación',
    notes: 'Diagrama entregado y validado con el docente.',
    courseId: 'course-bases-datos',
    dueInDays: -2,
    priority: 'LOW',
    status: 'DONE',
    createdDaysAgo: 6,
    subtaskTitles: [],
  },
  {
    id: 'task-quiz-estructuras',
    title: 'Quiz de estructuras de datos',
    notes: 'Temas: pilas, colas y listas enlazadas.',
    courseId: 'course-estructuras',
    dueInDays: 4,
    dueHour: 10,
    dueMinute: 0,
    priority: 'MEDIUM',
    status: 'PENDING',
    createdDaysAgo: 1,
    subtaskTitles: [],
  },
  {
    id: 'task-carne',
    title: 'Renovar el carné universitario',
    notes: 'Llevar fotografía y comprobante de matrícula.',
    courseId: null,
    dueInDays: 2,
    dueHour: 12,
    dueMinute: 0,
    priority: 'LOW',
    status: 'PENDING',
    createdDaysAgo: 1,
    subtaskTitles: [],
  },
];

// IDs deterministas del seed anterior (`subjects`), usados solo para limpiar
// la colección heredada cuando las reglas impiden enumerarla.
const LEGACY_SUBJECT_IDS = [
  'subject-calculo',
  'subject-estructuras',
  'subject-bases-datos',
  'subject-etica',
];

// ---------------------------------------------------------------------------
// Codificación de valores Firestore
// ---------------------------------------------------------------------------

const str = (value) => ({ stringValue: value });
const int = (value) => ({ integerValue: String(value) });
const bool = (value) => ({ booleanValue: value });
const nullValue = () => ({ nullValue: null });
const arrayValue = (values) => ({ arrayValue: { values } });
const mapValue = (fields) => ({ mapValue: { fields } });

/** `dueAt` relativo a la fecha local de hoy, igual que SeedData. */
function dueAt(now, days, hour, minute) {
  const date = new Date(now);
  date.setDate(date.getDate() + days);
  date.setHours(hour, minute, 0, 0);
  return date.getTime();
}

function cursoFields(curso) {
  return {
    name: str(curso.name),
    professor: str(curso.professor),
    credits: int(curso.credits),
    deletedAt: nullValue(),
  };
}

function taskFields(task, now) {
  const fields = {
    title: str(task.title),
    notes: str(task.notes),
    courseId: task.courseId === null ? nullValue() : str(task.courseId),
    priority: str(task.priority),
    status: str(task.status),
    subtasks: arrayValue(
      task.subtaskTitles.map((title, index) =>
        mapValue({
          id: str(`${task.id}-sub-${index + 1}`),
          title: str(title),
          isDone: bool(false),
          position: int(index),
        }),
      ),
    ),
    completedAt: task.status === 'DONE' ? int(now - DAY_MS) : nullValue(),
    deletedAt: nullValue(),
  };

  fields.dueAt =
    task.dueInDays === undefined || task.dueInDays === null
      ? nullValue()
      : int(
          dueAt(
            now,
            task.dueInDays,
            task.dueHour ?? 23,
            task.dueMinute ?? 59,
          ),
        );

  return fields;
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

function fail(message) {
  console.error(`Error: ${message}`);
  process.exit(1);
}

function parseArgs(argv) {
  const flags = new Set(argv);
  const known = new Set(['--dry-run', '--reset']);
  for (const flag of flags) {
    if (!known.has(flag)) fail(`opción desconocida '${flag}'. Usa --dry-run o --reset.`);
  }
  return { dryRun: flags.has('--dry-run'), reset: flags.has('--reset') };
}

async function readFirebaseConfig() {
  let raw;
  try {
    raw = await readFile(GOOGLE_SERVICES_PATH, 'utf8');
  } catch {
    fail(`no se pudo leer ${GOOGLE_SERVICES_PATH}.`);
  }
  const config = JSON.parse(raw);
  const apiKey = config?.client?.[0]?.api_key?.[0]?.current_key;
  const projectId = config?.project_info?.project_id;
  if (!apiKey || !projectId) {
    fail('google-services.json no contiene client[0].api_key[0].current_key o project_info.project_id.');
  }
  return { apiKey, projectId };
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { raw: text };
    }
  }
  return { response, body };
}

async function signIn(apiKey, email, password) {
  const { response, body } = await requestJson(
    `${IDENTITY_BASE}/accounts:signInWithPassword?key=${encodeURIComponent(apiKey)}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, returnSecureToken: true }),
    },
  );

  if (!response.ok) {
    const code = body?.error?.message ?? `HTTP ${response.status}`;
    fail(`autenticación rechazada (${code}). Revisa SEED_EMAIL/SEED_PASSWORD y que Email/Password esté habilitado.`);
  }
  if (!body?.idToken || !body?.localId) {
    fail('la respuesta de autenticación no incluyó idToken/localId.');
  }
  return { idToken: body.idToken, uid: body.localId };
}

function firestoreDocsBase(projectId) {
  return `${FIRESTORE_BASE}/projects/${projectId}/databases/(default)/documents`;
}

function docName(projectId, uid, collection, id) {
  return `projects/${projectId}/databases/(default)/documents/users/${uid}/${collection}/${id}`;
}

async function getDocument(idToken, projectId, uid, collection, id) {
  const { response, body } = await requestJson(
    `${FIRESTORE_BASE}/${docName(projectId, uid, collection, id)}`,
    { headers: { Authorization: `Bearer ${idToken}` } },
  );

  if (response.status === 404) return null;
  if (!response.ok) {
    const code = body?.error?.status ?? `HTTP ${response.status}`;
    fail(`lectura de ${collection}/${id} falló (${code}).`);
  }
  return body;
}

async function listDocumentIds(
  docsBase,
  idToken,
  uid,
  collection,
  { tolerateDenied = false } = {},
) {
  const ids = [];
  let pageToken;
  do {
    const query = new URLSearchParams({ pageSize: '300' });
    if (pageToken) query.set('pageToken', pageToken);
    const { response, body } = await requestJson(
      `${docsBase}/users/${uid}/${collection}?${query}`,
      { headers: { Authorization: `Bearer ${idToken}` } },
    );
    if (!response.ok) {
      // `subjects` es una colección heredada fuera del contrato actual: las
      // reglas pueden denegar su lectura. Quien llama decide si tolerarlo.
      if (tolerateDenied && response.status === 403) return null;
      const code = body?.error?.status ?? `HTTP ${response.status}`;
      fail(`listado de ${collection} falló (${code}).`);
    }
    for (const document of body?.documents ?? []) {
      ids.push(document.name.split('/').pop());
    }
    pageToken = body?.nextPageToken;
  } while (pageToken);
  return ids;
}

async function deleteDocument(
  projectId,
  idToken,
  uid,
  collection,
  id,
  { tolerateDenied = false } = {},
) {
  const { response, body } = await requestJson(
    `${FIRESTORE_BASE}/${docName(projectId, uid, collection, id)}`,
    { method: 'DELETE', headers: { Authorization: `Bearer ${idToken}` } },
  );
  if (!response.ok) {
    if (tolerateDenied && response.status === 403) return false;
    const code = body?.error?.status ?? `HTTP ${response.status}`;
    fail(`no se pudo borrar ${collection}/${id} (${code}).`);
  }
  return true;
}

async function resetSeedDocuments({ projectId, idToken, uid, docsBase }) {
  const collections = [
    { name: 'courses', seedIds: new Set(CURSOS.map((c) => c.id)) },
    { name: 'tasks', seedIds: new Set(TASKS.map((t) => t.id)) },
  ];

  let deleted = 0;
  for (const { name, seedIds } of collections) {
    const existing = await listDocumentIds(docsBase, idToken, uid, name);
    for (const id of existing) {
      if (!seedIds.has(id)) continue;
      await deleteDocument(projectId, idToken, uid, name, id);
      deleted += 1;
    }
  }

  // Migración de demo: la colección anterior `subjects` ya no existe en el
  // contrato; se borran todos sus documentos residuales (eran solo del seed).
  // Las reglas actuales solo cubren `courses`/`tasks`, así que listar o borrar
  // `subjects` puede devolver PERMISSION_DENIED: en ese caso se intentan los
  // IDs deterministas del seed anterior y se avisa sin abortar el seed.
  const listedSubjectIds = await listDocumentIds(
    docsBase,
    idToken,
    uid,
    'subjects',
    { tolerateDenied: true },
  );
  const legacySubjectIds = listedSubjectIds ?? LEGACY_SUBJECT_IDS;
  let blockedSubjects = 0;
  for (const id of legacySubjectIds) {
    const removed = await deleteDocument(
      projectId,
      idToken,
      uid,
      'subjects',
      id,
      { tolerateDenied: true },
    );
    if (removed) deleted += 1;
    else blockedSubjects += 1;
  }
  if (blockedSubjects > 0) {
    console.warn(
      `Aviso: ${blockedSubjects} documento(s) de subjects heredados no se pudieron borrar ` +
        '(PERMISSION_DENIED); requieren limpieza con credenciales de administrador.',
    );
  }

  console.log(
    `Reset: ${deleted} documento(s) eliminado(s) (seed actual y subjects heredados); el resto no se toca.`,
  );
}

// ---------------------------------------------------------------------------
// Flujo principal
// ---------------------------------------------------------------------------

async function main() {
  const { dryRun, reset } = parseArgs(process.argv.slice(2));
  const email = process.env.SEED_EMAIL;
  const password = process.env.SEED_PASSWORD;

  if (!email || !password) {
    fail('faltan SEED_EMAIL y SEED_PASSWORD. Defínelas en el entorno de la sesión; no las escribas en el repo.');
  }

  const { apiKey, projectId } = await readFirebaseConfig();
  const { idToken, uid } = await signIn(apiKey, email, password);
  const maskedUid = `${uid.slice(0, 4)}…${uid.slice(-2)}`;
  const docsBase = firestoreDocsBase(projectId);
  const startedAt = performance.now();

  console.log(`Proyecto: ${projectId} | usuario: ${maskedUid}`);
  console.log(`Modo: ${dryRun ? 'DRY RUN (sin escrituras)' : 'REAL'}${reset ? ' + reset' : ''}`);

  if (reset && !dryRun) {
    await resetSeedDocuments({ projectId, idToken, uid, docsBase });
  } else if (reset && dryRun) {
    console.log('Reset (dry-run): se borrarían solo los documentos con IDs del seed.');
  }

  const now = Date.now();
  const documents = [
    ...CURSOS.map((curso) => ({
      collection: 'courses',
      id: curso.id,
      fields: cursoFields(curso),
    })),
    ...TASKS.map((task) => ({
      collection: 'tasks',
      id: task.id,
      fields: taskFields(task, now),
    })),
  ];

  const writes = [];
  let creates = 0;
  let updates = 0;

  for (const document of documents) {
    // Con --reset (real o dry-run) los documentos del seed se borran/recrean,
    // así que el plan siempre es "crear" y no hace falta consultarlos.
    const existing = reset
      ? null
      : await getDocument(idToken, projectId, uid, document.collection, document.id);

    const write = {
      update: {
        name: docName(projectId, uid, document.collection, document.id),
        fields: document.fields,
      },
    };

    if (existing === null) {
      write.updateTransforms = [
        { fieldPath: 'createdAt', setToServerValue: 'REQUEST_TIME' },
        { fieldPath: 'updatedAt', setToServerValue: 'REQUEST_TIME' },
      ];
      creates += 1;
    } else {
      const createdAt = existing?.fields?.createdAt?.timestampValue;
      if (!createdAt) {
        fail(
          `${document.collection}/${document.id} ya existe sin createdAt; las reglas no permiten actualizarlo. Usa --reset.`,
        );
      }
      write.update.fields = {
        ...document.fields,
        createdAt: { timestampValue: createdAt },
      };
      write.updateTransforms = [
        { fieldPath: 'updatedAt', setToServerValue: 'REQUEST_TIME' },
      ];
      updates += 1;
    }

    writes.push(write);

    if (dryRun) {
      const action = existing === null ? 'crear' : 'actualizar';
      console.log(`  [${action}] users/<uid>/${document.collection}/${document.id}`);
    }
  }

  console.log(
    `Plan: ${CURSOS.length} courses + ${TASKS.length} tasks = ${documents.length} documentos ` +
      `(${creates} create, ${updates} update).`,
  );

  if (dryRun) {
    console.log(`DRY RUN: no se escribió nada. Tiempo de verificación: ${Math.round(performance.now() - startedAt)} ms.`);
    return;
  }

  const commitStartedAt = performance.now();
  const { response, body } = await requestJson(`${docsBase}:commit`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${idToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ writes }),
  });

  if (!response.ok) {
    const code = body?.error?.status ?? `HTTP ${response.status}`;
    const message = body?.error?.message ?? 'sin detalle';
    fail(`commit rechazado (${code}): ${message}`);
  }

  const commitMs = performance.now() - commitStartedAt;

  const [taskIds, cursoIds] = await Promise.all([
    listDocumentIds(docsBase, idToken, uid, 'tasks'),
    listDocumentIds(docsBase, idToken, uid, 'courses'),
  ]);

  console.log(
    `Escritos: ${documents.length} documentos (${creates} creados, ${updates} actualizados) en ${Math.round(commitMs)} ms.`,
  );
  console.log(`Remoto ahora: ${cursoIds.length} courses, ${taskIds.length} tasks bajo users/<uid>/.`);
  console.log(`Tiempo total: ${Math.round(performance.now() - startedAt)} ms.`);
}

main().catch((error) => {
  fail(error instanceof Error ? error.message : String(error));
});
