# User Stories — EPIC 1: Seguridad base

**Prioridad:** P0 — Crítico
**Objetivo:** Eliminar vulnerabilidades que hacen la app inutilizable en cualquier entorno real.
**Orden de implementación recomendado:** US-1.4 → US-1.1 → US-1.2 → US-1.3

---

## US-1.1 — Hashear contraseñas en el CRUD de usuarios

**Como** administrador que crea usuarios desde el panel de gestión,
**quiero** que la contraseña se encripte antes de guardarse en la base de datos,
**para que** ninguna contraseña quede almacenada en texto plano independientemente de cómo se haya creado el usuario.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/service/UserService.java` (línea 25)

**Contexto técnico:**
El método `AuthService.registerUser()` ya usa `BCryptPasswordEncoder` correctamente. El problema es que `UserService.createUser()` guarda el objeto `User` tal como llega del controlador, sin pasar por ningún encoder. Si alguien crea un usuario desde `POST /api/users`, su contraseña queda en plaintext.

---

### Criterios de aceptación

**CA-1.1.1 — Contraseña encriptada al crear usuario vía CRUD**
- **Dado** que un admin envía `POST /api/users` con `{ "password": "miPassword123" }`
- **Cuando** el usuario se guarda en la base de datos
- **Entonces** el campo `password` en la tabla `users` debe comenzar con `$2a$` (formato BCrypt) y nunca contener el valor original

**CA-1.1.2 — El usuario creado puede hacer login**
- **Dado** que se creó un usuario con contraseña "miPassword123" vía `POST /api/users`
- **Cuando** ese usuario intenta hacer login con `POST /api/auth/login` usando `{ "username": "...", "password": "miPassword123" }`
- **Entonces** recibe un JWT válido (respuesta 200 con campo `token`)

**CA-1.1.3 — El hashing no rompe el registro normal**
- **Dado** que un usuario se registra vía `POST /api/auth/signup`
- **Cuando** intenta hacer login con sus credenciales
- **Entonces** sigue funcionando correctamente (no hashear dos veces)

**CA-1.1.4 — Contraseña vacía es rechazada**
- **Dado** que se envía `POST /api/users` con `"password": ""`
- **Cuando** el sistema valida la petición
- **Entonces** responde 400 Bad Request antes de intentar guardar

---

## US-1.2 — Ocultar contraseña en respuestas de la API

**Como** consumidor de la API de usuarios,
**quiero** que el campo `password` nunca aparezca en ninguna respuesta JSON,
**para que** las contraseñas (aunque hasheadas) no viajen por la red innecesariamente.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/model/User.java`
- `backend/src/main/java/com/example/demo/controller/UserController.java`

**Contexto técnico:**
Actualmente `GET /api/users` devuelve la entidad `User` directamente, incluyendo el campo `password` con el hash BCrypt. Esto incumple el principio de mínima exposición de datos. La solución más limpia es crear un `UserResponseDTO` con solo los campos públicos y usarlo en todas las respuestas del `UserController`.

---

### Criterios de aceptación

**CA-1.2.1 — GET /api/users no devuelve password**
- **Dado** que hay usuarios en la base de datos
- **Cuando** se hace `GET /api/users` con un JWT válido
- **Entonces** ningún objeto del array contiene el campo `password` (ni en null, ni vacío, ni hasheado)

**CA-1.2.2 — GET /api/users/{id} no devuelve password**
- **Dado** que existe un usuario con id 1
- **Cuando** se hace `GET /api/users/1` con un JWT válido
- **Entonces** la respuesta no contiene el campo `password`

**CA-1.2.3 — POST /api/users (crear) no devuelve password en la respuesta**
- **Dado** que se crea un nuevo usuario vía `POST /api/users`
- **Cuando** la operación tiene éxito (201 Created)
- **Entonces** el body de respuesta contiene `id`, `name`, `email`, `phone`, `username`, `createdAt` pero NO `password`

**CA-1.2.4 — PUT /api/users/{id} (actualizar) no devuelve password**
- **Dado** que se actualiza un usuario vía `PUT /api/users/1`
- **Cuando** la operación tiene éxito (200 OK)
- **Entonces** el body de respuesta no contiene el campo `password`

**CA-1.2.5 — Los campos públicos sí están presentes**
- **Dado** cualquier respuesta exitosa del UserController
- **Entonces** la respuesta contiene todos estos campos: `id`, `name`, `email`, `phone`, `username`, `createdAt`

**CA-1.2.6 — El login sigue funcionando**
- **Dado** que se oculta el password en respuestas
- **Cuando** un usuario intenta hacer login
- **Entonces** Spring Security sigue pudiendo leer el password internamente para la autenticación (el `@JsonIgnore` o el DTO no afecta a `UserDetailsService`)

---

## US-1.3 — Proteger endpoints de escritura de Telegram

**Como** administrador de la aplicación,
**quiero** que solo los usuarios autenticados puedan crear noticias o forzar sincronización con Telegram,
**para que** cualquier persona anónima no pueda inyectar contenido falso en la sección de noticias.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/config/SecurityConfig.java` (líneas 62-66)

**Contexto técnico:**
En `SecurityConfig`, la regla actual pone `/api/telegram/**` completo en `permitAll()`. Esto significa que `POST /api/telegram/manual` y `POST /api/telegram/fetch` son accesibles sin ningún token. Solo la lectura (`GET /api/telegram/messages`) tiene sentido pública, ya que las noticias son contenido de la asociación.

---

### Criterios de aceptación

**CA-1.3.1 — GET /api/telegram/messages sigue siendo público**
- **Dado** que no se envía ningún token de autorización
- **Cuando** se hace `GET /api/telegram/messages`
- **Entonces** el servidor responde 200 con la lista de mensajes

**CA-1.3.2 — POST /api/telegram/manual requiere autenticación**
- **Dado** que no se envía ningún token de autorización
- **Cuando** se hace `POST /api/telegram/manual` con `{ "text": "noticia falsa" }`
- **Entonces** el servidor responde 401 Unauthorized

**CA-1.3.3 — POST /api/telegram/fetch requiere autenticación**
- **Dado** que no se envía ningún token de autorización
- **Cuando** se hace `POST /api/telegram/fetch`
- **Entonces** el servidor responde 401 Unauthorized

**CA-1.3.4 — Usuario autenticado puede crear noticias manuales**
- **Dado** que se envía un JWT válido en el header `Authorization: Bearer <token>`
- **Cuando** se hace `POST /api/telegram/manual` con `{ "text": "noticia válida" }`
- **Entonces** el servidor responde 200 y la noticia se guarda en la base de datos

**CA-1.3.5 — Usuario autenticado puede forzar sincronización**
- **Dado** que se envía un JWT válido
- **Cuando** se hace `POST /api/telegram/fetch`
- **Entonces** el servidor responde 200 (independientemente de si Telegram está configurado o no, no debe ser 401)

**CA-1.3.6 — El frontend actualiza las llamadas protegidas**
- **Dado** que el usuario está logueado en la app
- **Cuando** hace clic en "Actualizar Noticias" o "Crear Noticia"
- **Entonces** la petición incluye el header `Authorization` con el token (esto ya lo hace el interceptor de Axios, solo hay que verificar que no haya llamadas que lo omitan)

---

## US-1.4 — Externalizar secretos a variables de entorno

**Como** desarrollador del proyecto,
**quiero** que ninguna credencial ni secreto esté hardcodeado en los archivos del repositorio,
**para que** el código pueda compartirse o publicarse sin exponer datos sensibles del entorno de producción.

**Archivos afectados:**
- `backend/src/main/resources/application.properties`
- `.gitignore` (raíz del proyecto)
- `.env.example` (nuevo archivo a crear en la raíz)

**Contexto técnico:**
Actualmente `application.properties` contiene en texto plano: el JWT secret, las credenciales de PostgreSQL y el token del bot de Telegram. Este archivo está commiteado en el repositorio. Cualquiera con acceso al repo tiene acceso a esos valores.

---

### Criterios de aceptación

**CA-1.4.1 — application.properties no contiene valores secretos**
- **Dado** el archivo `application.properties` commiteado en el repositorio
- **Entonces** los siguientes campos referencian variables de entorno y no contienen valores reales:
  - `jwt.secret=${JWT_SECRET}`
  - `spring.datasource.password=${DB_PASSWORD}`
  - `spring.datasource.username=${DB_USERNAME}`
  - `spring.datasource.url=${DB_URL}`
  - `telegram.bot.token=${TELEGRAM_BOT_TOKEN}`

**CA-1.4.2 — El backend arranca correctamente con las variables definidas**
- **Dado** que las variables de entorno `JWT_SECRET`, `DB_PASSWORD`, `DB_USERNAME`, `DB_URL` y `TELEGRAM_BOT_TOKEN` están definidas en el entorno
- **Cuando** se arranca el backend con `mvn spring-boot:run`
- **Entonces** arranca sin errores y los endpoints responden correctamente

**CA-1.4.3 — El backend falla con mensaje claro si falta una variable**
- **Dado** que una variable requerida (ej. `JWT_SECRET`) no está definida en el entorno
- **Cuando** se intenta arrancar el backend
- **Entonces** Spring Boot lanza un error de startup indicando qué propiedad no se pudo resolver (comportamiento por defecto de Spring al no encontrar `${VAR}`)

**CA-1.4.4 — Existe un archivo .env.example documentado**
- **Dado** el repositorio del proyecto
- **Entonces** existe un archivo `.env.example` en la raíz con todas las variables necesarias, sus nombres correctos y un valor de ejemplo o descripción:
  ```
  JWT_SECRET=cambia_este_valor_por_un_string_aleatorio_largo
  DB_URL=jdbc:postgresql://localhost:5432/afa_fullstack_db
  DB_USERNAME=afa_user
  DB_PASSWORD=tu_password_aqui
  TELEGRAM_BOT_TOKEN=tu_token_del_bot_aqui
  ```

**CA-1.4.5 — El archivo .env real está excluido del repositorio**
- **Dado** que existe un archivo `.env` en el entorno local con valores reales
- **Entonces** el `.gitignore` lo excluye (`*.env` o `.env`) y nunca aparece en `git status` como archivo a commitear

**CA-1.4.6 — El entorno de desarrollo puede arrancar con valores por defecto**
- **Dado** que un desarrollador clona el repositorio por primera vez
- **Cuando** copia `.env.example` a `.env` y rellena los valores de su entorno local
- **Entonces** puede arrancar el backend sin modificar ningún archivo commiteado

---

## Orden de implementación sugerido

```
US-1.4 (variables de entorno)
  → sin dependencias, base para todo lo demás

US-1.1 (hashear contraseñas)
  → requiere que el entorno esté configurado (US-1.4)

US-1.2 (ocultar password en respuestas)
  → puede hacerse en paralelo con US-1.1
  → conveniente hacerla antes de US-1.3 para limpiar la API

US-1.3 (proteger endpoints Telegram)
  → puede hacerse en cualquier momento, cambio de una sola línea en SecurityConfig
```

## Definición de hecho (DoD) compartida para todas las US de este epic

- El código compila sin errores ni warnings nuevos
- La funcionalidad se ha probado manualmente con curl o Postman
- No se han introducido nuevas rutas sin protección
- Los archivos modificados han sido revisados para no dejar credenciales accidentales
- El `application.properties` no contiene ningún secreto en texto plano
