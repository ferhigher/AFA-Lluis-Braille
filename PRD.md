# PRD — AFA Lluís Braille Fullstack App
## Product Requirements Document: Pendientes

**Fecha:** Marzo 2026
**Estado actual del proyecto:** MVP funcional en desarrollo local, sin despliegue, con Telegram sin configurar.

---

## Resumen ejecutivo

La app es una plataforma web para la asociación AFA Lluís Braille con autenticación JWT, gestión de usuarios (CRUD) y sección de noticias integrada con Telegram. El backend corre en Spring Boot + PostgreSQL y el frontend en React + Vite. La base técnica está, pero hay bugs de seguridad, funcionalidades incompletas y la app no está lista para producción.

---

## 1. Bugs críticos (bloquean uso real)

### 1.1 Contraseñas en plaintext desde el CRUD de usuarios
- **Problema:** `UserController.createUser` guarda directamente el objeto `User` sin hashear la contraseña. Quien se crea desde el panel de administración (no desde `/signup`) tiene la contraseña en texto plano en la base de datos.
- **Archivos:** `UserService.java:25`, `UserController.java:36`
- **Solución:** Inyectar `PasswordEncoder` en `UserService` y hashear antes de guardar.

### 1.2 El response de usuarios expone la contraseña
- **Problema:** `GET /api/users` y todas las respuestas del `UserController` devuelven el objeto `User` completo, incluyendo el campo `password` (aunque sea hasheado, no debe salir).
- **Archivos:** `UserController.java`, `User.java`
- **Solución:** Añadir `@JsonIgnore` en el campo `password` del modelo, o crear un `UserDTO` de respuesta.

### 1.3 Endpoints de Telegram completamente públicos
- **Problema:** `POST /api/telegram/manual` permite crear noticias sin autenticación. Cualquiera puede inyectar contenido.
- **Archivos:** `SecurityConfig.java:62-66`
- **Solución:** Mover `/api/telegram/fetch` y `/api/telegram/manual` a rutas protegidas. Solo `/api/telegram/messages` (lectura) puede quedar público.

### 1.4 Secretos hardcodeados en application.properties
- **Problema:** El JWT secret y las credenciales de PostgreSQL están en el archivo commiteado al repositorio.
- **Archivos:** `application.properties`
- **Solución:** Usar variables de entorno con `${JWT_SECRET}`, `${DB_PASSWORD}`, etc.

---

## 2. Funcionalidades incompletas

### 2.1 Integración con Telegram
- **Estado:** Token placeholder `YOUR_BOT_TOKEN_HERE`. La sección de noticias no puede sincronizar con el canal real `@afa_lluis_braille`.
- **Requisitos:**
  - Configurar bot real con @BotFather y añadirlo al canal
  - Mover el token a variable de entorno
  - Validar que `fetchAndSaveMessages()` funciona con el canal configurado
  - Añadir scheduler automático para sincronizar periódicamente (ej. cada hora)

### 2.2 Sistema de roles
- **Estado:** No existe. Todos los usuarios autenticados tienen acceso a todo.
- **Requisitos:**
  - Añadir campo `role` al modelo `User` (valores: `ADMIN`, `USER`)
  - El primer usuario registrado, o uno designado manualmente, tiene rol `ADMIN`
  - Solo `ADMIN` puede: crear/editar/eliminar usuarios, publicar noticias manuales, forzar sync de Telegram
  - `USER` puede: ver usuarios (solo lectura), ver noticias
  - Proteger endpoints con `@PreAuthorize("hasRole('ADMIN')")`

### 2.3 Branding y personalización
- **Estado:** La barra de navegación muestra "Mi Aplicación" en lugar del nombre real.
- **Archivos:** `Navigation.jsx:10`
- **Requisitos:**
  - Cambiar nombre a "AFA Lluís Braille"
  - Añadir logo/favicon de la asociación
  - Revisar colores y tipografía para alinearse con identidad visual de la asociación

### 2.4 Navegación real (React Router)
- **Estado:** La navegación es por estado local, no hay URLs. No se puede compartir un enlace directo a "Noticias" o acceder por URL directamente.
- **Requisitos:**
  - Instalar `react-router-dom`
  - Rutas: `/login`, `/signup`, `/usuarios`, `/noticias`
  - Rutas protegidas que redirigen a `/login` si no hay sesión
  - El botón atrás del navegador debe funcionar correctamente

### 2.5 Paginación
- **Estado:** Listas sin paginación. Con muchos usuarios o noticias, la UI se vuelve inutilizable.
- **Requisitos:**
  - Backend: añadir parámetros `page` y `size` a `GET /api/users` y `GET /api/telegram/messages`
  - Frontend: componente de paginación en `UserList` y `News`

---

## 3. Funcionalidades nuevas a añadir

### 3.1 Recuperación de contraseña
- **Descripción:** Un usuario que olvidó su contraseña debe poder recuperarla sin contactar a nadie.
- **Flujo:**
  1. Usuario introduce email en formulario "Olvidé mi contraseña"
  2. Backend genera token temporal (UUID, expira en 1h) y envía email con link
  3. Usuario hace clic en link → formulario de nueva contraseña
  4. Backend valida token y actualiza contraseña con BCrypt
- **Requiere:** Configurar servicio de email (SMTP o SendGrid)

### 3.2 Refresh tokens
- **Descripción:** El JWT actual dura 24h y no hay forma de renovarlo sin hacer login de nuevo. Si expira con la app abierta, el usuario pierde contexto.
- **Flujo:**
  - Al hacer login, el backend devuelve `accessToken` (15 min) + `refreshToken` (7 días, en cookie httpOnly)
  - El interceptor de Axios detecta 401, llama a `/api/auth/refresh` automáticamente
  - Si el refreshToken también expiró → logout forzado

### 3.3 Búsqueda y filtros
- **Usuarios:** Filtrar por nombre, email, fecha de registro
- **Noticias:** Filtrar por rango de fechas, buscar texto en el contenido

### 3.4 Scheduler automático para Telegram
- **Descripción:** Las noticias se actualizan solo cuando un admin hace clic en "Actualizar Noticias". Debería hacerse automáticamente.
- **Implementación:** `@Scheduled(fixedRate = 3600000)` en `TelegramService.fetchAndSaveMessages()`
- **Configurable:** Frecuencia vía `application.properties`

---

## 4. Deuda técnica y calidad de código

### 4.1 Limpiar logs de debug
- **Problema:** El frontend tiene `console.log` extensivos en cada petición (`api.js`). El backend tiene logs `DEBUG`/`TRACE` en producción.
- **Solución:**
  - Frontend: eliminar todos los `console.log` de `api.js` o envolverlos en un flag de desarrollo
  - Backend: cambiar nivel de log a `INFO` para producción; usar perfil Spring (`application-prod.properties`)

### 4.2 Tests
- **Estado actual:** Cero tests.
- **Mínimo requerido:**
  - Backend: tests de integración para `AuthController` (login, signup, token inválido)
  - Backend: tests unitarios para `AuthService`, `UserService`
  - Frontend: tests de componente para `Login`, `Signup`

### 4.3 Validación en updateUser
- **Problema:** `UserService.updateUser` no verifica si el nuevo email ya pertenece a otro usuario antes de guardar.
- **Archivos:** `UserService.java:32`
- **Solución:** Añadir `existsByEmailAndIdNot(email, id)` al repositorio y validar.

### 4.4 Gestión de errores global en el frontend
- **Problema:** Los errores se manejan de forma inconsistente en cada componente. Un error de red devuelve mensajes diferentes según dónde ocurra.
- **Solución:** Centralizar el manejo de errores en el interceptor de Axios con mensajes estándar por código HTTP.

---

## 5. Preparación para producción

### 5.1 Variables de entorno
- Extraer de `application.properties`: `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `TELEGRAM_TOKEN`
- Documentar en `.env.example` (sin valores reales)

### 5.2 Dockerización
- `Dockerfile` para el backend (imagen `eclipse-temurin:17-jre`)
- `Dockerfile` para el frontend (build Vite + nginx)
- `docker-compose.yml` con backend + frontend + postgres
- Objetivo: un solo `docker compose up` levanta todo el entorno

### 5.3 CORS para producción
- **Problema:** `CorsConfig` permite `localhost:3000` y `localhost:5173`. En producción fallará.
- **Solución:** Leer los orígenes permitidos desde variable de entorno `CORS_ALLOWED_ORIGINS`

### 5.4 HTTPS
- Configurar certificado SSL (Let's Encrypt o similar) si hay dominio propio
- Forzar redirección HTTP → HTTPS

### 5.5 Rate limiting
- Limitar intentos de login (ej. máx 5 intentos por IP en 15 min) para evitar fuerza bruta
- Implementar con `Bucket4j` o un filtro de Spring

---

## Prioridades sugeridas

| Prioridad | Item |
|-----------|------|
| **P0 — Crítico** | 1.1 Hashear contraseñas en CRUD · 1.2 Ocultar password en respuestas · 1.3 Proteger endpoints Telegram · 1.4 Variables de entorno |
| **P1 — Alto** | 2.1 Configurar bot Telegram real · 2.2 Roles admin/user · 2.3 Branding · 4.3 Validación email en update |
| **P2 — Medio** | 2.4 React Router · 2.5 Paginación · 3.4 Scheduler automático · 4.1 Limpiar logs · 4.4 Manejo de errores |
| **P3 — Bajo** | 3.1 Recuperación contraseña · 3.2 Refresh tokens · 3.3 Búsqueda y filtros · 4.2 Tests · 5.x Producción/Docker |
