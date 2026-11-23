# NotallyU UNAMAD — Gestión Académica Móvil (ISO/IEC 25010)

Aplicación móvil nativa para **Android** desarrollada en **Kotlin + Jetpack Compose + Material Design 3** con la **paleta institucional de la Universidad Nacional Amazónica de Madre de Dios (UNAMAD)**, integración al **Aula Virtual UNAMAD**, respaldo en **Firebase (Auth + Cloud Firestore)** y diseño alineado a la norma **ISO/IEC 25010**.

- **Curso:** Aplicaciones Móviles II (EPISI — UNAMAD)
- **Docente:** Laura Salvatierra, Leonel Christofher
- **Integrantes:** Gonzales Flores, Jhordy · Meza Huaman, Juan Agustin
- **Plantillas en Figma (UI Kit + 5 Pantallas Editables):** https://www.figma.com/design/dCJeQPr56N5SgSVi8kCz7J

---

## Mejoras implementadas sobre la base original

1. **Identidad visual UNAMAD (Claro + Oscuro + Sistema):**
   - Paleta institucional oficial del portal **UNAMAD** (`unamad.edu.pe`): **Guinda / Magenta UNAMAD (`#C1034B` · gradiente `#D60654` → `#A80443`)**, **Azul Marino (`#1E2A3A`)**, **Dorado Institucional (`#F5BE48`)** y **Verde Éxito (`#16794A`)** con contrastes WCAG 2.2 AA.
   - Soporte completo para **Modo Claro**, **Modo Oscuro** y **Sistema**, persistido en DataStore con barras Edge-to-Edge adaptativas.
2. **Interfaz dinámica e interactiva:**
   - **Banner Hero UNAMAD** en *Hoy* con saludo contextual por hora del día, anillo de progreso animado, métricas rápidas y promedio vigesimal del Aula Virtual.
   - **Búsqueda instantánea** en tiempo real en *Hoy* y *Cursos*.
   - **Tarjeta Focal renovada** con acento dorado, badge de prioridad y escala táctil animada.
   - **Tarjetas de tareas y cursos enriquecidas** con barra lateral semántica, insignias (*Entregada*, *No entregada*, *Grupal*, *Nota X/20*, *Aula UNAMAD*) y anillos de avance animados.
   - **Tarjeta de bienvenida UNAMAD** cuando aún no se ha iniciado sesión en Firebase.
3. **Cumplimiento de la norma ISO/IEC 25010:**
   - Matriz completa en [docs/ISO_25010_COMPLIANCE.md](docs/ISO_25010_COMPLIANCE.md).
   - **Fiabilidad:** persistencia offline explícita de Firestore (`PersistentCacheSettings`), monitor reactivo de conectividad (`NetworkMonitor`) con banner de modo sin conexión y propagación controlada de errores con recuperación.
   - **Seguridad:** cifrado local **AES-256-GCM** con Android Keystore (`EncryptedSharedPreferences`) excluido de backups, cookies en RAM y reglas `firestore.rules` con validación de esquema y cierre `deny-by-default`.
   - **Portabilidad y Mantenibilidad:** toolchain Gradle alineado a JBR 21 de Android Studio y **10 suites de pruebas unitarias JVM** (`failures = 0`).

---

## Cómo abrir y ejecutar en Android Studio

1. Clona este repositorio y ábrelo en **Android Studio**.
2. Espera la sincronización de Gradle (usa el **JDK 21** integrado de Android Studio automáticamente).
3. Ejecuta en tu emulador o dispositivo Android (API 30+):

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:installDebug
```
