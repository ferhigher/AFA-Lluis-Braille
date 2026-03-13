# User Stories — EPIC 2: Sistema de roles y permisos

**Prioridad:** P1 — Alto
**Objetivo:** Diferenciar qué puede hacer un administrador (ADMIN) versus un usuario regular (USER), tanto en el backend como en la interfaz.
**Dependencias:** EPIC 1 completo (las 4 US's de seguridad base deben estar implementadas)

**Estado del código antes de este epic:**
- `User.java` — sin campo `role`; la entidad solo tiene `id`, `name`, `email`, `phone`, `username`, `password`, `createdAt`
- `UserDetailsServiceImpl.java:29` — devuelve `new ArrayList<>()` como authorities (sin roles cargados)
- `JwtResponse.java` — sin campo `role`; el frontend no recibe el rol en el login
- `UserResponseDTO.java` — sin campo `role`
- `SecurityConfig.java` — `@EnableMethodSecurity` ya presente ✅; sin `@PreAuthorize` en ningún controlador todavía
- `UserController.java` — todos los métodos accesibles para cualquier usuario autenticado
- `AuthContext.jsx` — almacena el objeto `JwtResponse` sin rol; `isAuthenticated` es el único predicado disponible
- `UserList.jsx` — muestra "Editar" y "Eliminar" a cualquier usuario autenticado
- `News.jsx` — muestra "Actualizar Noticias" y el formulario "Crear Noticia Manual" a cualquier usuario autenticado

**Orden de implementación recomendado:** US-2.1 → US-2.2 → US-2.3 → US-2.4

---

## US-2.1 — Modelo de roles en la base de datos y propagación al frontend

**Como** sistema,
**necesito** distinguir entre usuarios con rol `ADMIN` y usuarios con rol `USER`,
**para que** el backend pueda aplicar permisos distintos y el frontend pueda adaptar la interfaz al rol del usuario autenticado.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/model/User.java`
- `backend/src/main/java/com/example/demo/dto/UserResponseDTO.java`
- `backend/src/main/java/com/example/demo/dto/JwtResponse.java`
- `backend/src/main/java/com/example/demo/dto/SignupRequest.java`
- `backend/src/main/java/com/example/demo/service/AuthService.java`
- `backend/src/main/java/com/example/demo/security/UserDetailsServiceImpl.java`
- `frontend/src/context/AuthContext.jsx`

**Contexto técnico:**
El rol se añade como un `enum` `{ ADMIN, USER }` en la entidad `User`. Al registrarse, el valor por defecto es `USER` (la lógica del primer ADMIN se define en US-2.2). `UserDetailsServiceImpl.loadUserByUsername()` actualmente devuelve `new ArrayList<>()` como authorities; debe pasar a devolver `List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))` para que `@PreAuthorize("hasRole('ADMIN')")` funcione en US-2.3. El rol debe llegar al frontend en el `JwtResponse` del login para que `AuthContext` pueda exponerlo.

---

### Criterios de aceptación

**CA-2.1.1 — La columna `role` existe en la tabla `users`**
- **Dado** que se arranca el backend
- **Entonces** la tabla `users` en PostgreSQL tiene una columna `role` de tipo `VARCHAR` (o equivalente) con valores posibles `ADMIN` y `USER`
- **Y** la columna tiene valor por defecto `USER`
- **Y** ningún registro existente tiene `role` en null (los usuarios previos reciben `USER` automáticamente en la migración)

**CA-2.1.2 — Un usuario nuevo registrado vía `/api/auth/signup` recibe rol `USER`**
- **Dado** que se hace `POST /api/auth/signup` con datos válidos
- **Cuando** el usuario se guarda en la base de datos
- **Entonces** el campo `role` del nuevo registro es `USER`
- **Y** el campo `role` no puede ser fijado por el cliente en el body del signup (se ignora si se envía)

**CA-2.1.3 — `UserDetailsServiceImpl` carga el rol como `GrantedAuthority`**
- **Dado** que existe un usuario con `role = ADMIN` en la base de datos
- **Cuando** Spring Security carga el `UserDetails` de ese usuario
- **Entonces** el objeto `UserDetails` tiene una authority `ROLE_ADMIN`
- **Y** para un usuario con `role = USER`, la authority es `ROLE_USER`
- **Y** la lista de authorities nunca está vacía (ya no se devuelve `new ArrayList<>()`)

**CA-2.1.4 — El login devuelve el rol en la respuesta**
- **Dado** que un usuario con `role = ADMIN` hace `POST /api/auth/login` con credenciales válidas
- **Entonces** la respuesta 200 contiene el campo `role` con valor `ADMIN`
- **Y** para un usuario `USER`, el campo `role` es `USER`
- **Y** el `JwtResponse` tiene la estructura: `{ token, type, id, username, email, name, role }`

**CA-2.1.5 — `UserResponseDTO` incluye el rol**
- **Dado** que se hace `GET /api/users` con JWT válido
- **Entonces** cada objeto del array contiene el campo `role` con valor `ADMIN` o `USER`
- **Y** el campo `password` sigue ausente (no regresión de US-1.2)

**CA-2.1.6 — El `AuthContext` expone el rol del usuario autenticado**
- **Dado** que un usuario hace login en el frontend
- **Cuando** el `AuthContext` almacena la respuesta del backend
- **Entonces** el hook `useAuth()` expone el campo `user.role` con el valor recibido (`ADMIN` o `USER`)
- **Y** el campo persiste correctamente en `localStorage` y se restaura al recargar la página
- **Y** el predicado `isAdmin` (o equivalente) está disponible como `user?.role === 'ADMIN'` en cualquier componente que use `useAuth()`

---

## US-2.2 — Asignación del primer administrador

**Como** instalador del sistema,
**quiero** que el primer usuario que se registre reciba automáticamente el rol `ADMIN`,
**para que** siempre exista al menos un administrador sin necesidad de ejecutar SQL manualmente.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/service/AuthService.java` — método `registerUser()`
- `backend/src/main/java/com/example/demo/repository/UserRepository.java` — nuevo método de consulta

**Contexto técnico:**
En `AuthService.registerUser()` se añade una comprobación: si `userRepository.existsByRole(Role.ADMIN)` devuelve `false`, el usuario que se está creando recibe `role = ADMIN`; en caso contrario recibe `role = USER`. Esta lógica cubre el caso de instalación en blanco. Para entornos con datos previos sin ningún admin, el mismo mecanismo asigna ADMIN al próximo registro. Se documenta en el README.

---

### Criterios de aceptación

**CA-2.2.1 — El primer usuario registrado en una base de datos vacía recibe rol `ADMIN`**
- **Dado** que la tabla `users` está vacía (o no existe ningún usuario con `role = ADMIN`)
- **Cuando** se hace `POST /api/auth/signup` con datos válidos
- **Entonces** el usuario creado tiene `role = ADMIN` en la base de datos
- **Y** el login posterior de ese usuario devuelve `"role": "ADMIN"` en el `JwtResponse`

**CA-2.2.2 — El segundo usuario y siguientes reciben rol `USER`**
- **Dado** que ya existe al menos un usuario con `role = ADMIN`
- **Cuando** se hace `POST /api/auth/signup` para un nuevo usuario
- **Entonces** el nuevo usuario tiene `role = USER` en la base de datos
- **Y** el login de ese usuario devuelve `"role": "USER"`

**CA-2.2.3 — El rol no puede ser forzado desde el body del signup**
- **Dado** que se envía `POST /api/auth/signup` con body `{ ..., "role": "ADMIN" }`
- **Cuando** el sistema procesa la petición
- **Entonces** el campo `role` del body es ignorado
- **Y** el rol asignado sigue la regla del CA-2.2.1 o CA-2.2.2 según corresponda

**CA-2.2.4 — La lógica de asignación es concurrencia-safe**
- **Dado** que la comprobación `existsByRole(ADMIN)` y la asignación del rol son operaciones separadas
- **Entonces** la implementación usa la comprobación dentro de una transacción (`@Transactional`) para evitar condiciones de carrera en registros simultáneos

**CA-2.2.5 — El proceso está documentado en el README**
- **Dado** que un nuevo desarrollador clona el repositorio
- **Entonces** el `README.md` explica que el primer usuario en registrarse recibe `ADMIN` y cómo reasignar roles manualmente si fuera necesario (script SQL de ejemplo)

---

## US-2.3 — Proteger endpoints según rol

**Como** sistema,
**quiero** que solo los usuarios con rol `ADMIN` puedan crear, editar o eliminar usuarios y publicar noticias,
**para que** un usuario con rol `USER` no pueda realizar operaciones de escritura aunque esté autenticado.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/controller/UserController.java`
- `backend/src/main/java/com/example/demo/controller/TelegramController.java`

**Contexto técnico:**
`SecurityConfig` ya tiene `@EnableMethodSecurity` activo (línea 24), lo que habilita `@PreAuthorize`. Solo hay que añadir las anotaciones en los métodos correspondientes. La distinción de acceso es:
- `GET /api/users` y `GET /api/users/{id}` → cualquier usuario autenticado (`authenticated()` ya lo cubre, sin cambios)
- `POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` → solo `ADMIN`
- `POST /api/telegram/fetch`, `POST /api/telegram/manual` → solo `ADMIN`
- `GET /api/telegram/messages` → público (sin cambios, ya definido en `SecurityConfig`)

**Precondición:** US-2.1 debe estar implementada. Sin ella, `UserDetailsServiceImpl` devuelve authorities vacías y `hasRole('ADMIN')` siempre falla, bloqueando incluso a los admins.

---

### Criterios de aceptación

**CA-2.3.1 — `POST /api/users` requiere rol ADMIN**
- **Dado** que se envía un JWT válido de un usuario con `role = USER`
- **Cuando** se hace `POST /api/users` con un body válido
- **Entonces** el servidor responde `403 Forbidden`
- **Dado** que se envía un JWT válido de un usuario con `role = ADMIN`
- **Cuando** se hace `POST /api/users` con un body válido
- **Entonces** el servidor responde `201 Created`

**CA-2.3.2 — `PUT /api/users/{id}` requiere rol ADMIN**
- **Dado** que se envía un JWT de `role = USER`
- **Cuando** se hace `PUT /api/users/1` con un body válido
- **Entonces** el servidor responde `403 Forbidden`
- **Dado** que se envía un JWT de `role = ADMIN`
- **Entonces** el servidor responde `200 OK`

**CA-2.3.3 — `DELETE /api/users/{id}` requiere rol ADMIN**
- **Dado** que se envía un JWT de `role = USER`
- **Cuando** se hace `DELETE /api/users/1`
- **Entonces** el servidor responde `403 Forbidden`
- **Dado** que se envía un JWT de `role = ADMIN`
- **Entonces** el servidor responde `200 OK` (o `204 No Content`)

**CA-2.3.4 — `GET /api/users` es accesible para cualquier usuario autenticado**
- **Dado** que se envía un JWT válido de un usuario con `role = USER`
- **Cuando** se hace `GET /api/users`
- **Entonces** el servidor responde `200 OK` con la lista de usuarios
- **Y** sigue respondiendo `401` sin token (no regresión)

**CA-2.3.5 — `POST /api/telegram/manual` requiere rol ADMIN**
- **Dado** que se envía un JWT de `role = USER`
- **Cuando** se hace `POST /api/telegram/manual` con `{ "text": "noticia" }`
- **Entonces** el servidor responde `403 Forbidden`
- **Dado** que se envía un JWT de `role = ADMIN`
- **Entonces** el servidor responde `200 OK`

**CA-2.3.6 — `POST /api/telegram/fetch` requiere rol ADMIN**
- **Dado** que se envía un JWT de `role = USER`
- **Cuando** se hace `POST /api/telegram/fetch`
- **Entonces** el servidor responde `403 Forbidden`
- **Dado** que se envía un JWT de `role = ADMIN`
- **Entonces** el servidor responde `200 OK` (independientemente de si Telegram está configurado)

**CA-2.3.7 — `GET /api/telegram/messages` sigue siendo público**
- **Dado** que no se envía ningún token
- **Cuando** se hace `GET /api/telegram/messages`
- **Entonces** el servidor responde `200 OK`
- **Y** no se ha introducido ninguna regresión en el comportamiento público de este endpoint

**CA-2.3.8 — Sin token en cualquier endpoint protegido responde 401, no 403**
- **Dado** que no se envía ningún token
- **Cuando** se hace `POST /api/users`, `PUT /api/users/1`, o `DELETE /api/users/1`
- **Entonces** el servidor responde `401 Unauthorized` (no autenticado)
- **Y** solo cuando hay token pero el rol es insuficiente se responde `403 Forbidden`

---

## US-2.4 — Interfaz adaptada al rol del usuario

**Como** usuario del panel de administración,
**quiero** que la interfaz solo me muestre las acciones que puedo realizar según mi rol,
**para que** no vea botones que me devuelvan errores al pulsarlos.

**Archivos afectados:**
- `frontend/src/components/UserList.jsx` — ocultar botones de escritura si `role = USER`
- `frontend/src/components/UserForm.jsx` — ocultar o bloquear acceso si `role = USER`
- `frontend/src/components/News.jsx` — ocultar controles de admin si `role = USER`
- `frontend/src/App.jsx` — ocultar el botón "+ Nuevo Usuario" si `role = USER`

**Contexto técnico:**
`AuthContext.jsx` ya expone `user` con los datos del `JwtResponse`. Tras US-2.1, `user.role` contendrá `ADMIN` o `USER`. Los componentes usan `useAuth()` para leer el rol. La ocultación es puramente visual (UX): el backend ya protege los endpoints en US-2.3. No se trata de seguridad, sino de no confundir al usuario. Se usa el predicado `user?.role === 'ADMIN'` o un helper `isAdmin` en el `AuthContext`.

---

### Criterios de aceptación

**CA-2.4.1 — El botón "Editar" se oculta para usuarios con rol USER**
- **Dado** que un usuario con `role = USER` está autenticado y en la vista de usuarios
- **Cuando** se renderiza el componente `UserList`
- **Entonces** los botones "Editar" de cada fila de la tabla no son visibles en el DOM
- **Dado** que un usuario con `role = ADMIN` está autenticado
- **Entonces** los botones "Editar" sí son visibles

**CA-2.4.2 — El botón "Eliminar" se oculta para usuarios con rol USER**
- **Dado** que un usuario con `role = USER` está autenticado
- **Cuando** se renderiza `UserList`
- **Entonces** los botones "Eliminar" no están en el DOM
- **Dado** que el usuario es `ADMIN`
- **Entonces** los botones "Eliminar" son visibles

**CA-2.4.3 — El botón "+ Nuevo Usuario" se oculta para rol USER**
- **Dado** que un usuario con `role = USER` está en la vista de gestión de usuarios
- **Entonces** el botón que abre el formulario de creación de usuario no es visible
- **Dado** que el usuario es `ADMIN`
- **Entonces** el botón "+ Nuevo Usuario" (o equivalente) es visible

**CA-2.4.4 — El formulario de creación/edición de usuario no es accesible para rol USER**
- **Dado** que un usuario con `role = USER` intenta acceder al `UserForm` directamente
- **Entonces** el formulario no se renderiza o muestra un mensaje de acceso denegado
- **Y** no realiza ninguna llamada a la API

**CA-2.4.5 — El botón "Actualizar Noticias" se oculta para rol USER**
- **Dado** que un usuario con `role = USER` está autenticado y en la vista de noticias
- **Cuando** se renderiza el componente `News`
- **Entonces** el botón "Actualizar Noticias" no está en el DOM
- **Dado** que el usuario es `ADMIN`
- **Entonces** el botón "Actualizar Noticias" es visible

**CA-2.4.6 — El formulario "Crear Noticia Manual" se oculta para rol USER**
- **Dado** que un usuario con `role = USER` está en la vista de noticias
- **Entonces** la sección "Crear Noticia Manual (Para Pruebas)" y su formulario no están en el DOM
- **Dado** que el usuario es `ADMIN`
- **Entonces** el formulario de creación manual es visible

**CA-2.4.7 — Los usuarios USER pueden leer la lista de usuarios sin controles de escritura**
- **Dado** que un usuario con `role = USER` está autenticado
- **Cuando** navega a la sección de usuarios
- **Entonces** puede ver la tabla con `id`, `nombre`, `email`, `teléfono` de cada usuario
- **Y** la columna "Acciones" no aparece o aparece vacía (sin botones)
- **Y** no se muestra ningún error 403 en la interfaz

**CA-2.4.8 — La ocultación de controles no afecta a la funcionalidad de lectura**
- **Dado** que un usuario con `role = USER` abre la app
- **Entonces** puede ver y navegar por todas las secciones de lectura sin errores
- **Y** no aparece ningún error en consola relacionado con permisos denegados en las llamadas GET

---

## Orden de implementación sugerido

```
US-2.1 (modelo de roles + propagación al frontend)
  → base técnica necesaria para todo lo demás
  → sin ella, @PreAuthorize siempre falla (authorities vacías)

US-2.2 (asignación del primer admin)
  → requiere el campo role en User (US-2.1)
  → puede hacerse en paralelo con US-2.1 si se trabaja en la misma rama

US-2.3 (proteger endpoints)
  → requiere US-2.1 completada y verificada
  → @EnableMethodSecurity ya está activo en SecurityConfig

US-2.4 (UI adaptada al rol)
  → requiere US-2.1 (el frontend necesita recibir el rol en el login)
  → puede desarrollarse en paralelo con US-2.3
```

## Definición de hecho (DoD) compartida para todas las US de este epic

- El código compila sin errores ni warnings nuevos
- La columna `role` existe en la tabla `users` con valores `ADMIN` / `USER`
- Un usuario ADMIN puede realizar todas las operaciones (CRUD usuarios + escribir noticias)
- Un usuario USER puede leer usuarios y noticias, pero recibe 403 al intentar escribir
- Sin token → 401; con token USER en endpoint ADMIN → 403
- Los botones de escritura (Editar, Eliminar, Nuevo Usuario, Actualizar Noticias, Crear Noticia) no aparecen en el DOM para usuarios USER
- Todos los cambios han sido verificados manualmente con `curl` (backend) y en el navegador (frontend)
- Los archivos modificados no contienen credenciales en texto plano
- No se han introducido regresiones en los endpoints públicos (`GET /api/telegram/messages`, `POST /api/auth/login`, `POST /api/auth/signup`)
