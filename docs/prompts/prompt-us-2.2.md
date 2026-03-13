Actúa como un senior fullstack engineer especializado en Spring Boot 3, Spring Security 6
y React 18, con experiencia en sistemas de autenticación y autorización basados en JWT.

Voy a pedirte que generes un **plan de implementación detallado** para la User Story
**US-2.2** del EPIC 2 — Sistema de roles y permisos.

---

## Contexto del proyecto

Lee los siguientes documentos antes de generar el plan:
- `PRD.md` — requisitos del producto, bugs conocidos y prioridades
- `EPICS.md` — todos los epics, sus dependencias y el mapa de US's
- `US-EPIC2.md` — definición completa de las US's del EPIC 2, criterios de aceptación y DoD

---

## Estado actual del código relevante para este EPIC

Antes de proponer cambios, verifica que el código sigue siendo el descrito aquí. Si ha
cambiado, adáptate al estado real. Los puntos críticos conocidos son:

**Backend:**
- `User.java` — NO tiene campo `role`. Solo tiene: `id`, `name`, `email`, `phone`,
  `username`, `password`, `createdAt`
- `UserDetailsServiceImpl.java:29` — devuelve `new ArrayList<>()` como authorities.
  Esto hace que `@PreAuthorize("hasRole('ADMIN')")` SIEMPRE FALLE aunque el rol esté
  en la BD. Es el bloqueante más crítico del EPIC.
- `SecurityConfig.java:24` — `@EnableMethodSecurity` ya está activo ✅. No hay que
  añadirlo.
- `UserController.java` — sin ningún `@PreAuthorize`. Todos los métodos accesibles para
  cualquier usuario autenticado.
- `JwtResponse.java` — sin campo `role`. El login no devuelve el rol al frontend.
- `UserResponseDTO.java` — sin campo `role`.
- `SignupRequest.java` — no tiene campo `role` (correcto; el rol no debe fijarse desde
  el cliente).

**Frontend:**
- `AuthContext.jsx` — almacena el objeto `JwtResponse` en localStorage. Sin campo `role`,
  `user.role` es `undefined` para todos los usuarios.
- `UserList.jsx` — muestra botones "Editar" y "Eliminar" a cualquier usuario autenticado.
- `News.jsx` — muestra "Actualizar Noticias" y el formulario "Crear Noticia Manual" a
  cualquier usuario autenticado.
- `App.jsx` — el botón "+ Nuevo Usuario" no tiene ninguna restricción de rol.

**Dependencia crítica de implementación:**
US-2.1 DEBE estar completada antes de implementar US-2.3. Sin que
`UserDetailsServiceImpl` cargue las authorities correctas, `@PreAuthorize` falla
y los endpoints bloquean incluso a los ADMIN. Verifica al inicio del plan si
US-2.1 ya está implementada.

---

## Lo que debes generar

### 1. Resumen de la US
- Qué problema resuelve esta US concretamente
- Lista de archivos afectados con rutas absolutas desde la raíz del proyecto
- Dependencias con otras US's del EPIC 2 (verificar si están implementadas)
- Riesgos o efectos secundarios conocidos

### 2. Análisis del código actual
Lee el código real de cada archivo afectado. Para cada uno indica:
- Estado actual (qué hay ahora)
- Qué debe cambiar exactamente y por qué
- Línea o bloque concreto que cambia

Presta especial atención a:
- Que `UserDetailsServiceImpl.loadUserByUsername()` cargue las authorities antes de
  añadir `@PreAuthorize` en los controladores (si aplica a esta US)
- Que el campo `role` fluya completo: BD → `User` → `UserDetailsServiceImpl` →
  `JwtResponse` → `localStorage` → `AuthContext` → componentes React (si aplica)

### 3. Plan de implementación paso a paso

Desglosa los cambios en pasos atómicos y ordenados. Para cada paso:
- Archivo a modificar (ruta completa)
- Cambio concreto: qué se añade, elimina o modifica
- Motivo del cambio
- Orden: indica si el paso debe hacerse antes o después de otro

Aplica los principios de `/clean-code` en cada cambio:
- Nombres expresivos: `role`, `isAdmin`, `hasAdminRole` en lugar de `r`, `flag`, `check`
- Un método, una responsabilidad: extraer la lógica de asignación de rol a un método
  privado en `AuthService` si supera 5 líneas
- Sin comentarios redundantes; el código debe ser autoexplicativo
- Sin duplicación: si el predicado `user.role === 'ADMIN'` se usa en más de un componente,
  exponerlo como `isAdmin` desde `AuthContext`

Si la US incluye cambios en la entidad `User` o en queries de PostgreSQL, aplica
`/postgresql-code-review` para:
- Validar el tipo de columna para el enum `role` (recomendado: `VARCHAR` con check
  constraint, no tipo `ENUM` nativo de PostgreSQL, para compatibilidad con JPA)
- Verificar que el valor por defecto de la columna es correcto
- Confirmar que la migración no rompe registros existentes

Si la US incluye cambios en el enum de rol de Java, usa
`/java-refactoring-extract-method` si la lógica de asignación del primer ADMIN supera
10 líneas en `AuthService.registerUser()`.

Si la US incluye componentes React nuevos o modificados, aplica
`/vercel-react-best-practices`:
- No calcular `isAdmin` en cada render: leerlo directamente del contexto
- Evitar prop drilling del rol: usar `useAuth()` en el componente que lo necesita
- La ocultación de elementos por rol debe hacerse con renderizado condicional
  (`{isAdmin && <button>...</button>}`), no con CSS `display: none`

### 4. Tests que cubren los criterios de aceptación

Genera un test por cada CA definido en `US-EPIC2.md` para **US-2.1**.

---

#### Backend — Spring Boot (usa `/backend-testing`)

**Tests de integración** (`@SpringBootTest` + `MockMvc` + H2 en memoria):

Para cada endpoint afectado, genera los tres escenarios de seguridad obligatorios:

```
Escenario A — Sin token → 401 Unauthorized
Escenario B — Con token de rol USER → 403 Forbidden  (solo en endpoints de escritura)
Escenario C — Con token de rol ADMIN → 2xx (operación exitosa)
```

Usa estas anotaciones de Spring Security Test:
```java
// Sin autenticación
mockMvc.perform(post("/api/users"))
       // no se añade header Authorization

// Con rol USER
@WithMockUser(username = "user1", roles = {"USER"})

// Con rol ADMIN
@WithMockUser(username = "admin1", roles = {"ADMIN"})
```

**Tests unitarios** (Mockito) para servicios:
- Si la US toca `AuthService.registerUser()`: test de que el primer registro recibe ADMIN
  y los siguientes reciben USER
- Si la US toca `UserDetailsServiceImpl`: test de que las authorities cargadas coinciden
  con el rol del usuario

Nombra cada test: `test_CA_X_Y_descripcion()` donde X.Y es el número del CA.

Para cada test indica:
- Ruta completa del fichero de test
- Código completo del test
- CA que cubre
- Si es integración o unitario

---

#### Frontend — React (Vitest + React Testing Library)

Para cada componente modificado en esta US:

```javascript
// Patrón base para tests de rol en componentes
import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'

// Mock de AuthContext con rol USER
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'USER', name: 'Test' }, isAuthenticated: true })
}))

// Mock de AuthContext con rol ADMIN
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'ADMIN', name: 'Admin' }, isAuthenticated: true })
}))
```

Tests obligatorios para componentes con control de visibilidad por rol:
- Test: con `role = USER` → el elemento NO está en el documento (`not.toBeInTheDocument()`)
- Test: con `role = ADMIN` → el elemento SÍ está en el documento (`toBeInTheDocument()`)

Usa `queryByRole` o `queryByText` para verificar ausencia (no `getBy`, que lanza error
si el elemento no existe).

Para cada test indica:
- Ruta completa del fichero de test
- Código completo del test
- CA que cubre

### 5. Verificación manual — levantar la app

#### 5.1 Preparar el entorno
```bash
# Asegurarse de que el .env existe con las variables de EPIC 1
ls .env || cp .env.example .env
```

#### 5.2 Levantar el backend
```bash
./start-backend.sh
# Verificar arranque: curl -s http://localhost:8080/actuator/health 2>/dev/null || \
#   curl -s http://localhost:8080/api/auth/login -X POST \
#        -H "Content-Type: application/json" \
#        -d '{"username":"x","password":"x"}' | head -c 100
```

#### 5.3 Levantar el frontend
```bash
./start-frontend.sh
# Verificar en http://localhost:5173
```

#### 5.4 Matriz de verificación por rol

Para **cada CA de US-2.1**, proporciona el comando `curl` exacto que lo verifica.
Estructura cada verificación así:

```
CA-X.Y.Z — [descripción del CA]
─────────────────────────────────────────────────────
# Paso 1: Obtener token ADMIN (si ya existe un admin)
TOKEN_ADMIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ADMIN_USER","password":"ADMIN_PASS"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Paso 2: Obtener token USER
TOKEN_USER=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"USER_USER","password":"USER_PASS"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Verificación sin token → esperar 401
curl -s -o /dev/null -w "%{http_code}" -X [METHOD] http://localhost:8080/[ENDPOINT]

# Verificación con token USER → esperar 403 (en endpoints de escritura)
curl -s -o /dev/null -w "%{http_code}" -X [METHOD] http://localhost:8080/[ENDPOINT] \
  -H "Authorization: Bearer $TOKEN_USER" \
  -H "Content-Type: application/json" \
  -d '[BODY]'

# Verificación con token ADMIN → esperar 2xx
curl -s -w "\nHTTP: %{http_code}\n" -X [METHOD] http://localhost:8080/[ENDPOINT] \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '[BODY]'

Respuesta esperada: [status code] + [campos JSON relevantes]
Qué confirma: [qué CA demuestra este resultado]
```

Incluye también el comando para registrar el primer usuario (que debe recibir ADMIN):
```bash
# Registrar primer usuario → debe recibir role: ADMIN
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin AFA","username":"adminafa","email":"admin@afa.org","password":"Admin1234","phone":"600000000"}' \
  | python3 -m json.tool
# Verificar que role = "ADMIN" en la respuesta de login posterior
```

#### 5.5 Verificación en el navegador (si la US tiene cambios en frontend)
Describe paso a paso el flujo en `http://localhost:5173`:
1. Login con usuario ADMIN → qué botones/controles deben verse
2. Logout → Login con usuario USER → qué botones/controles deben estar ocultos
3. Qué observar exactamente en el DOM (DevTools > Inspector si es necesario)

#### 5.6 Parar la app
```bash
./stop-frontend.sh
./stop-backend.sh
```

### 6. Definición de hecho (DoD)

Checklist específico del EPIC 2. Marca cada item solo cuando esté verificado:

**Corrección funcional:**
- [ ] La columna `role` existe en la tabla `users` con valores `ADMIN` / `USER`
- [ ] `UserDetailsServiceImpl` carga el rol como `SimpleGrantedAuthority("ROLE_X")`
      (authorities ya NO es `new ArrayList<>()`)
- [ ] El login devuelve `role` en el `JwtResponse`
- [ ] `AuthContext` expone `user.role` correctamente tras login y tras recarga de página
- [ ] El primer usuario en registrarse recibe `ADMIN`; los siguientes reciben `USER`
- [ ] El rol NO puede forzarse desde el body del signup

**Seguridad de endpoints:**
- [ ] Sin token → 401 en todos los endpoints protegidos
- [ ] Token USER en endpoint de escritura → 403 (no 401, no 200)
- [ ] Token ADMIN en endpoint de escritura → 2xx
- [ ] `GET /api/telegram/messages` sigue respondiendo 200 sin token (no regresión)
- [ ] `GET /api/users` responde 200 con token USER (lectura permitida)

**Interfaz:**
- [ ] Botones "Editar", "Eliminar" y "+ Nuevo Usuario" ausentes del DOM para rol USER
- [ ] "Actualizar Noticias" y formulario "Crear Noticia Manual" ausentes del DOM para USER
- [ ] Los usuarios USER pueden ver la lista de usuarios sin errores
- [ ] No hay llamadas 403 en la consola del navegador para usuarios USER navegando normalmente

**Calidad:**
- [ ] Todos los tests del paso 4 pasan en verde (`mvn test` + `npm test`)
- [ ] No hay `new ArrayList<>()` como authorities en `UserDetailsServiceImpl`
- [ ] El predicado de rol no está duplicado en múltiples componentes React
- [ ] Usa `/code-refactoring` para revisar duplicación antes de cerrar
- [ ] Usa `/simplify` para revisar calidad y eficiencia del código añadido
- [ ] `application.properties` no contiene ningún secreto en texto plano (no regresión EPIC 1)

---

## Restricciones

- No generes código que no sea estrictamente necesario para **US-2.1**
- No añadas `@PreAuthorize` en US-2.3 si US-2.1 no está verificada (bloqueante real)
- No refactorices código fuera del alcance de esta US
- No añadas dependencias nuevas sin justificarlas; Spring Security Test
  (`spring-security-test`) ya está disponible en el `pom.xml` de proyectos Spring Boot
- Si detectas que una dependencia previa no está implementada, indícalo al inicio del
  plan y detén la generación hasta recibir confirmación

---

Genera el plan completo para **US-2.1**.