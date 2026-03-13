# EPICS — AFA Lluís Braille Fullstack App

Generado a partir de `PRD.md` · Marzo 2026

---

## EPIC 1 — Seguridad base
**Prioridad:** P0 — Crítico
**Objetivo:** Eliminar vulnerabilidades que hacen la app inutilizable en cualquier entorno real.
**Dependencias:** Ninguna. Debe hacerse antes que cualquier otro epic.

### Historias de usuario

**US-1.1 — Hashear contraseñas en el CRUD de usuarios**
> Como administrador, quiero que cualquier usuario creado desde el panel de gestión tenga su contraseña encriptada, para que ninguna contraseña se almacene en texto plano.

- Inyectar `PasswordEncoder` en `UserService`
- Hashear `user.getPassword()` antes de `userRepository.save()` en `createUser()`
- Verificar que el hash funciona en login posterior
- **Archivos:** `UserService.java:25`

**US-1.2 — Ocultar contraseña en respuestas de la API**
> Como consumidor de la API, no quiero recibir el campo `password` en ninguna respuesta de `/api/users`, aunque esté hasheado.

- Añadir `@JsonIgnore` al campo `password` en `User.java`, o
- Crear `UserResponseDTO` con solo los campos públicos (id, name, email, phone, username, createdAt, role)
- Actualizar `UserController` para devolver el DTO en lugar de la entidad
- **Archivos:** `User.java`, `UserController.java`

**US-1.3 — Proteger endpoints de escritura de Telegram**
> Como administrador, quiero que solo usuarios autenticados puedan crear noticias manuales o forzar sincronización con Telegram, para evitar que cualquiera inyecte contenido.

- En `SecurityConfig`, mover `/api/telegram/fetch` y `/api/telegram/manual` de `permitAll()` a `authenticated()`
- Mantener `GET /api/telegram/messages` público (lectura)
- **Archivos:** `SecurityConfig.java:62-66`

**US-1.4 — Externalizar secretos a variables de entorno**
> Como desarrollador, quiero que ningún secreto esté hardcodeado en el repositorio, para poder compartir el código sin exponer credenciales.

- Reemplazar en `application.properties`: `jwt.secret`, `spring.datasource.password`, `spring.datasource.username`, `telegram.bot.token` por referencias a variables de entorno (`${JWT_SECRET}`, etc.)
- Crear `.env.example` con las variables necesarias y valores de ejemplo
- Actualizar `.gitignore` para excluir cualquier `.env` real
- **Archivos:** `application.properties`

---

## EPIC 2 — Sistema de roles y permisos
**Prioridad:** P1 — Alto
**Objetivo:** Diferenciar qué puede hacer un administrador versus un usuario regular.
**Dependencias:** EPIC 1 (especialmente US-1.2, para no exponer el rol en respuestas indebidas)

### Historias de usuario

**US-2.1 — Modelo de roles en la base de datos**
> Como sistema, necesito distinguir entre usuarios con rol ADMIN y usuarios con rol USER para aplicar permisos distintos.

- Añadir campo `role` al modelo `User` (enum: `ADMIN`, `USER`)
- Valor por defecto al registrarse: `USER`
- Migración de datos: asignar `USER` a todos los usuarios existentes
- Añadir `role` al `JwtResponse` y al `UserDetailsImpl`
- **Archivos:** `User.java`, `SignupRequest.java`, `AuthService.java`

**US-2.2 — Asignación del primer admin**
> Como instalador del sistema, quiero poder designar un usuario como ADMIN sin necesidad de un panel especial, para que haya siempre al menos un administrador.

- Opción A (simple): script SQL para actualizar el rol manualmente (`UPDATE users SET role='ADMIN' WHERE username='...'`)
- Opción B (automática): si no existe ningún ADMIN en la base de datos, el primer usuario en registrarse recibe rol `ADMIN`
- Documentar el proceso en el README

**US-2.3 — Proteger endpoints según rol**
> Como sistema, quiero que solo los ADMIN puedan crear, editar o eliminar usuarios y publicar noticias, mientras que los USER solo puedan leer.

- Activar `@EnableMethodSecurity` (ya está en `SecurityConfig`)
- Añadir `@PreAuthorize("hasRole('ADMIN')")` en:
  - `UserController`: `createUser`, `updateUser`, `deleteUser`
  - `TelegramController`: `fetchMessages`, `createManualMessage`
- `GET /api/users` y `GET /api/users/{id}`: accesible para cualquier autenticado
- **Archivos:** `UserController.java`, `TelegramController.java`

**US-2.4 — UI adaptada al rol del usuario**
> Como usuario, quiero que la interfaz solo me muestre las acciones que puedo realizar según mi rol, para no ver botones que me darán error.

- Leer el rol del usuario desde el `AuthContext`
- Ocultar en `UserList`: botones "Editar", "Eliminar", "+ Nuevo Usuario" si el rol es `USER`
- Ocultar en `News`: botón "Actualizar Noticias" y formulario "Crear Noticia Manual" si el rol es `USER`
- **Archivos:** `AuthContext.jsx`, `UserList.jsx`, `News.jsx`, `App.jsx`

---

## EPIC 3 — Integración Telegram operativa
**Prioridad:** P1 — Alto
**Objetivo:** Que la sección de noticias funcione de verdad con el canal `@afa_lluis_braille`.
**Dependencias:** EPIC 1 (US-1.4 para el token), EPIC 2 (US-2.3 para proteger el endpoint de sync)

### Historias de usuario

**US-3.1 — Configurar bot de Telegram real**
> Como administrador, quiero conectar la app al canal de Telegram de la asociación para que las noticias reales aparezcan en la web.

- Crear bot con @BotFather y obtener token
- Añadir bot como administrador del canal `@afa_lluis_braille`
- Configurar token vía variable de entorno `TELEGRAM_BOT_TOKEN`
- Verificar que `fetchAndSaveMessages()` recibe y guarda mensajes reales

**US-3.2 — Scheduler automático de sincronización**
> Como administrador, quiero que las noticias se actualicen automáticamente sin tener que hacer clic en "Actualizar", para que el contenido esté siempre al día.

- Añadir `@EnableScheduling` en `DemoApplication.java`
- Añadir `@Scheduled(fixedRateString = "${telegram.scheduler.rate:3600000}")` en `TelegramService`
- Hacer la frecuencia configurable desde `application.properties`
- El botón manual "Actualizar" sigue existiendo para forzar sincronización inmediata

**US-3.3 — Manejo de errores en la sincronización**
> Como administrador, quiero saber si la sincronización con Telegram falla, para poder actuar.

- Si `fetchAndSaveMessages()` falla, registrar el error con suficiente contexto en los logs
- Respuesta clara en el endpoint `POST /api/telegram/fetch` cuando el bot no está configurado (actualmente falla silenciosamente)
- Considerar añadir un indicador de estado de la última sincronización en la UI

---

## EPIC 4 — Experiencia de navegación
**Prioridad:** P2 — Medio
**Objetivo:** Que la app se comporte como una web real con URLs navegables y navegación fluida.
**Dependencias:** EPIC 2 (los roles determinan qué rutas son accesibles)

### Historias de usuario

**US-4.1 — Implementar React Router**
> Como usuario, quiero que cada sección de la app tenga su propia URL, para poder usar el botón atrás del navegador y compartir enlaces directos.

- Instalar `react-router-dom`
- Definir rutas: `/login`, `/signup`, `/usuarios`, `/noticias`
- Crear componente `ProtectedRoute` que redirige a `/login` si no hay sesión
- Eliminar el estado `view` y `currentView` de `App.jsx` y reemplazar por `<Routes>`
- Actualizar `Navigation.jsx` para usar `<Link>` en lugar de `onClick`
- Configurar `vite.config.js` para que todas las rutas caigan en `index.html` (historyApiFallback)
- **Archivos:** `App.jsx`, `Navigation.jsx`, `vite.config.js`

**US-4.2 — Paginación en lista de usuarios**
> Como administrador, quiero ver los usuarios paginados, para que la lista no sea infinita cuando hay muchos registros.

- Backend: modificar `UserController.getAllUsers()` para aceptar `?page=0&size=10`
- Backend: usar `Pageable` y `Page<UserResponseDTO>` en `UserService`
- Frontend: componente `Pagination` reutilizable (botones anterior/siguiente + indicador de página)
- Integrar paginación en `UserList.jsx`
- **Archivos:** `UserController.java`, `UserService.java`, `UserList.jsx`

**US-4.3 — Paginación en lista de noticias**
> Como usuario, quiero ver las noticias paginadas, para que la página cargue rápido aunque haya muchas noticias acumuladas.

- Backend: modificar `TelegramController.getMessages()` para aceptar `?page=0&size=10`
- Frontend: integrar el componente `Pagination` en `News.jsx`
- **Archivos:** `TelegramController.java`, `TelegramService.java`, `News.jsx`

---

## EPIC 5 — Identidad visual y branding
**Prioridad:** P1 — Alto
**Objetivo:** Que la app refleje la identidad de AFA Lluís Braille, no una app genérica de demo.
**Dependencias:** Ninguna técnica. Requiere assets (logo, colores) de la asociación.

### Historias de usuario

**US-5.1 — Nombre y logo correcto**
> Como usuario, quiero ver el nombre y logo real de la asociación en la app, para saber que estoy en el lugar correcto.

- Cambiar "Mi Aplicación" por "AFA Lluís Braille" en `Navigation.jsx:10`
- Añadir logo de la asociación en la navbar
- Actualizar `favicon.ico` y `<title>` en `index.html`
- **Archivos:** `Navigation.jsx`, `index.html`

**US-5.2 — Paleta de colores y tipografía corporativa**
> Como usuario, quiero que los colores de la app correspondan a los de la asociación, para una experiencia coherente.

- Identificar colores corporativos de AFA Lluís Braille
- Actualizar variables CSS en `index.css` y `App.css`
- Revisar botones, cabeceras y tarjetas de noticias

---

## EPIC 6 — Calidad de código y deuda técnica
**Prioridad:** P2 — Medio
**Objetivo:** Dejar el código en estado mantenible y sin ruido innecesario.
**Dependencias:** Ninguna. Puede hacerse en paralelo con otros epics.

### Historias de usuario

**US-6.1 — Eliminar logs de debug excesivos**
> Como desarrollador, quiero que los logs no contaminen la consola en producción, para poder detectar errores reales fácilmente.

- Frontend: eliminar o condicionar a `import.meta.env.DEV` todos los `console.log` de `api.js`
- Backend: crear `application-prod.properties` con `logging.level.root=WARN` y `logging.level.com.example.demo=INFO`
- Backend: activar el perfil `prod` en el entorno de producción con `-Dspring.profiles.active=prod`
- **Archivos:** `api.js`, `application.properties`

**US-6.2 — Validación de email duplicado en actualización de usuario**
> Como administrador, quiero que al editar un usuario no pueda asignarle un email que ya usa otro usuario.

- Añadir `existsByEmailAndIdNot(String email, Long id)` a `UserRepository`
- Validar en `UserService.updateUser()` antes de guardar
- Devolver error 400 con mensaje claro si hay conflicto
- **Archivos:** `UserRepository.java`, `UserService.java:32`

**US-6.3 — Manejo de errores centralizado en el frontend**
> Como usuario, quiero recibir mensajes de error coherentes independientemente de dónde falle la app.

- Definir un mapa de mensajes por código HTTP en `api.js` (401, 403, 404, 409, 500)
- El interceptor de respuesta de Axios transforma los errores al mensaje estándar antes de propagarlos
- Eliminar mensajes de error hardcodeados en cada componente; usar el mensaje del error recibido

---

## EPIC 7 — Tests
**Prioridad:** P3 — Bajo
**Objetivo:** Mínima cobertura para evitar regresiones en los flujos críticos.
**Dependencias:** EPIC 1, EPIC 2 (los tests deben reflejar la lógica final de seguridad)

### Historias de usuario

**US-7.1 — Tests unitarios de servicios backend**
> Como desarrollador, quiero tests unitarios para `AuthService` y `UserService`, para detectar regresiones al cambiar la lógica de negocio.

- Test: `AuthService.registerUser()` falla si username ya existe
- Test: `AuthService.registerUser()` falla si email ya existe
- Test: `AuthService.authenticateUser()` devuelve JWT válido con credenciales correctas
- Test: `UserService.updateUser()` falla si el nuevo email ya pertenece a otro usuario
- Usar Mockito para mockear repositorios

**US-7.2 — Tests de integración de los controladores de auth**
> Como desarrollador, quiero tests de integración para el flujo de autenticación completo, para garantizar que login y signup funcionan end-to-end.

- Test: `POST /api/auth/signup` con datos válidos → 201
- Test: `POST /api/auth/signup` con username duplicado → 400
- Test: `POST /api/auth/login` con credenciales correctas → 200 con token
- Test: `POST /api/auth/login` con contraseña incorrecta → 401
- Test: `GET /api/users` sin token → 401
- Test: `GET /api/users` con token de USER → 200 (solo lectura)
- Test: `DELETE /api/users/{id}` con token de USER → 403
- Usar `@SpringBootTest` + `MockMvc` + base de datos H2 en test

**US-7.3 — Tests de componentes frontend**
> Como desarrollador, quiero tests de los componentes de autenticación, para garantizar que el formulario de login valida correctamente.

- Test: `Login` muestra error si campos vacíos
- Test: `Login` llama a `authService.login` con los datos del formulario
- Test: `Signup` muestra error si contraseñas no coinciden (si se añade confirmación)
- Usar Vitest + React Testing Library

---

## EPIC 8 — Preparación para producción
**Prioridad:** P3 — Bajo
**Objetivo:** Que la app pueda desplegarse en un servidor real de forma reproducible y segura.
**Dependencias:** EPIC 1 (variables de entorno), todos los epics anteriores resueltos.

### Historias de usuario

**US-8.1 — Dockerización del proyecto**
> Como equipo, queremos poder levantar toda la aplicación con un solo comando, para simplificar el despliegue y evitar problemas de entorno.

- `backend/Dockerfile`: imagen `eclipse-temurin:17-jre`, copia el JAR, expone puerto 8080
- `frontend/Dockerfile`: fase build con Node + Vite, fase producción con nginx
- `docker-compose.yml`: servicios `postgres`, `backend`, `frontend` con dependencias correctas
- Variables de entorno en `docker-compose.yml` referenciadas desde `.env`

**US-8.2 — CORS configurable para producción**
> Como administrador, quiero que los orígenes permitidos en CORS se configuren por variable de entorno, para no tener que modificar el código al desplegar.

- Leer `CORS_ALLOWED_ORIGINS` desde `application.properties` → `${CORS_ALLOWED_ORIGINS:http://localhost:3000}`
- **Archivos:** `CorsConfig.java`

**US-8.3 — Rate limiting en endpoints de autenticación**
> Como sistema, quiero limitar los intentos de login por IP para proteger contra ataques de fuerza bruta.

- Añadir dependencia `bucket4j-spring-boot-starter`
- Limitar `POST /api/auth/login` a máx 5 intentos por IP en ventana de 15 minutos
- Responder 429 Too Many Requests cuando se supera el límite

**US-8.4 — HTTPS y redirección**
> Como usuario, quiero que la conexión sea siempre cifrada, para que mis credenciales no viajen en texto plano.

- Configurar certificado SSL con Let's Encrypt (o similar)
- Redirigir HTTP → HTTPS en nginx o en el propio Spring Boot
- Documentar el proceso en el README

---

---

## EPIC 9 — Landing pública: estructura base y navegación
**Prioridad:** P1 — Alto
**Objetivo:** Crear la landing pública de `afalluisbraille.org` con estructura de navegación completa, hero section y footer, accesible sin autenticación.
**Dependencias:** EPIC 4 (US-4.1 React Router), EPIC 5 (Branding)

### Historias de usuario

**US-9.1 — Ruta pública raíz y estructura base de la landing**
> Como visitante, quiero que `/` cargue una landing pública sin requerir autenticación, para conocer la asociación antes de registrarme.

**US-9.2 — Menú de navegación público**
> Como visitante, quiero un navbar con la estructura `AFA ▾ | ASOCIATE | COMISIONES | COLEGIO | AVISOS | NOTICIAS | Acceder`, para navegar a cualquier sección de la web.

**US-9.3 — Hero section**
> Como visitante nuevo, quiero ver una sección de bienvenida con el nombre de la asociación y un CTA, para entender inmediatamente de qué trata la web.

**US-9.4 — Footer**
> Como visitante, quiero un footer con contacto, redes sociales y política de privacidad, para poder contactar con la asociación.

---

## EPIC 10 — Landing pública: secciones de contenido institucional
**Prioridad:** P1 — Alto
**Objetivo:** Construir las secciones estáticas de la landing: qué es la AFA, quiénes somos, cómo asociarse, comisiones y colegio.
**Dependencias:** EPIC 9

### Historias de usuario

**US-10.1 — Sección "Qué es la AFA"**
> Como visitante, quiero leer una explicación clara de la misión de la AFA Lluís Braille y sus pilares, para entender su propósito.

**US-10.2 — Sección "Quiénes somos"**
> Como visitante, quiero ver una presentación del equipo directivo de la AFA, para saber a quién dirigirme y confiar en la asociación.

**US-10.3 — Sección y CTA "Asóciate"**
> Como familiar del alumnado, quiero encontrar fácil y rápido cómo hacerme socio/a, para contribuir a la comunidad escolar.

**US-10.4 — Sección "Comisiones"**
> Como visitante, quiero ver las áreas de trabajo de la AFA en formato grid de tarjetas, para saber en qué iniciativas puedo participar.

**US-10.5 — Sección "Colegio"**
> Como familia interesada, quiero encontrar información básica del centro educativo Lluís Braille, para conocer el colegio antes de contactar.

---

## EPIC 11 — Landing pública: secciones dinámicas (Avisos y Noticias)
**Prioridad:** P2 — Medio
**Objetivo:** Conectar la landing con el backend para mostrar noticias y avisos en tiempo real sin autenticación.
**Dependencias:** EPIC 9, EPIC 10, EPIC 1 (US-1.3 endpoints públicos), EPIC 2 (US-2.3 roles para escritura), EPIC 3 (Telegram operativo)

### Historias de usuario

**US-11.1 — Sección "Noticias" en la landing (pública)**
> Como visitante, quiero ver las últimas noticias de la asociación en la landing sin registrarme, para estar informado de la actividad de la AFA.

**US-11.2 — Sección "Avisos" en la landing y modelo backend**
> Como familiar, quiero ver avisos importantes en la landing sin autenticación, para estar al tanto de comunicaciones urgentes del colegio o la AFA.

**US-11.3 — CRUD de avisos en el panel de administración**
> Como administrador, quiero gestionar avisos desde el panel privado (crear, editar, activar/desactivar, eliminar), para controlar qué se muestra en la landing sin tocar código.

---

## Mapa de dependencias

```
EPIC 1 (Seguridad base)
  └── EPIC 2 (Roles)
        └── EPIC 3 (Telegram operativo)
        └── EPIC 4 (Navegación)             ←─ EPIC 9 (Landing base)
        └── EPIC 7 (Tests)                         └── EPIC 10 (Contenido institucional)
              └── EPIC 8 (Producción)                     └── EPIC 11 (Secciones dinámicas)

EPIC 5 (Branding)        → prerrequisito de EPIC 9
EPIC 6 (Deuda técnica)  → independiente, paralelo
```

---

## Resumen por prioridad

| Epic | Título | Prioridad | US |
|------|--------|-----------|-----|
| EPIC 1 | Seguridad base | P0 | 4 |
| EPIC 2 | Roles y permisos | P1 | 4 |
| EPIC 3 | Telegram operativo | P1 | 3 |
| EPIC 5 | Branding | P1 | 2 |
| EPIC 9 | Landing: estructura y navegación | P1 | 4 |
| EPIC 10 | Landing: contenido institucional | P1 | 5 |
| EPIC 4 | Navegación (Router + Paginación) | P2 | 3 |
| EPIC 6 | Deuda técnica | P2 | 3 |
| EPIC 11 | Landing: secciones dinámicas | P2 | 3 |
| EPIC 7 | Tests | P3 | 3 |
| EPIC 8 | Producción | P3 | 4 |
