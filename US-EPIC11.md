# User Stories — EPIC 11: Landing pública — Secciones dinámicas (Avisos y Noticias)

**Prioridad:** P2 — Medio
**Objetivo:** Conectar la landing pública con el backend para mostrar avisos y noticias en tiempo real, sin requerir autenticación para la lectura.
**Dependencias:**
- EPIC 9 completo (estructura base de la landing)
- EPIC 10 (US-10.1 a US-10.5 para tener la landing con contenido estático)
- EPIC 1 (US-1.3) — `GET /api/telegram/messages` ya es público; los nuevos endpoints de avisos también deben serlo
- EPIC 3 (US-3.1 y US-3.2) — la integración Telegram debe estar operativa para que las noticias sean reales

**Orden de implementación recomendado:** US-11.1 → US-11.2 → US-11.3

---

## US-11.1 — Sección "Noticias" en la landing (pública)

**Como** visitante de `afalluisbraille.org`,
**quiero** ver las últimas noticias de la asociación en la landing principal,
**para que** esté informado de la actividad de la AFA sin necesidad de registrarme.

**Archivos afectados:**
- `frontend/src/components/NewsSection.jsx` — nuevo componente (versión pública, distinto al `News.jsx` del panel admin)
- `frontend/src/styles/NewsSection.css` — nuevo archivo de estilos
- `frontend/src/pages/LandingPage.jsx` — integrar la sección
- `frontend/src/services/api.js` — añadir llamada pública sin token a `GET /api/telegram/messages`

**Contexto técnico:**
El endpoint `GET /api/telegram/messages` ya existe y es público (EPIC 1 US-1.3). La sección de la landing muestra solo las últimas N noticias (ej. 6) en formato grid de tarjetas. No es la vista completa del panel admin; es una vista de resumen con un enlace "Ver todas las noticias" que lleva a `/noticias`. La llamada al backend no requiere token JWT.

---

### Criterios de aceptación

**CA-11.1.1 — La sección es accesible desde el menú**
- **Dado** que el visitante hace clic en `NOTICIAS` en la navbar
- **Entonces** la página navega hasta la sección `#noticias` o a la ruta `/noticias` sin error

**CA-11.1.2 — La sección carga noticias sin autenticación**
- **Dado** que un visitante sin token accede a la landing
- **Cuando** la sección de noticias se monta en el DOM
- **Entonces** se llama a `GET /api/telegram/messages` sin cabecera `Authorization`
- **Y** el servidor responde 200 con la lista de mensajes

**CA-11.1.3 — Se muestran solo las últimas N noticias**
- **Dado** que el backend tiene más de 6 noticias
- **Cuando** se carga la sección
- **Entonces** se muestran como máximo 6 noticias (o el número configurado), ordenadas de más reciente a más antigua

**CA-11.1.4 — Cada noticia muestra fecha y texto**
- **Dado** que se renderiza una tarjeta de noticia
- **Entonces** son visibles: la fecha de publicación formateada en español (ej. `12 de marzo de 2026`) y el texto del mensaje
- **Y** si el texto supera los 200 caracteres, se trunca con `...` y muestra un enlace `Leer más`

**CA-11.1.5 — Estado de carga mientras llegan los datos**
- **Dado** que la petición al backend tarda más de 0ms
- **Cuando** la sección se está cargando
- **Entonces** se muestra un indicador visual (skeleton, spinner o texto `Cargando noticias...`)

**CA-11.1.6 — Estado de error si el backend no responde**
- **Dado** que el backend no está disponible o responde con error
- **Cuando** la petición falla
- **Entonces** se muestra un mensaje no técnico al usuario (ej. `No se pudieron cargar las noticias en este momento`)
- **Y** el resto de la landing sigue renderizándose correctamente (error aislado a la sección)

**CA-11.1.7 — Estado vacío si no hay noticias**
- **Dado** que el backend responde con un array vacío
- **Entonces** se muestra un mensaje de estado vacío (ej. `Próximamente, nuevas noticias de la asociación`)
- **Y** no se muestra ningún error ni elemento de tarjeta vacío

**CA-11.1.8 — Enlace "Ver todas las noticias"**
- **Dado** que hay noticias cargadas o aunque no las haya
- **Entonces** hay un enlace o botón `Ver todas las noticias` que navega a `/noticias`

---

## US-11.2 — Sección "Avisos" en la landing (tablón público)

**Como** familiar del alumnado del colegio Lluís Braille,
**quiero** ver avisos importantes de la asociación y del colegio en la landing,
**para que** esté al tanto de comunicaciones urgentes o relevantes sin tener que buscarlos.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/controller/AvisoController.java` — nuevo controlador
- `backend/src/main/java/com/example/demo/service/AvisoService.java` — nuevo servicio
- `backend/src/main/java/com/example/demo/model/Aviso.java` — nueva entidad
- `backend/src/main/java/com/example/demo/repository/AvisoRepository.java` — nuevo repositorio
- `backend/src/main/java/com/example/demo/config/SecurityConfig.java` — añadir ruta pública `GET /api/avisos`
- `frontend/src/components/AvisosSection.jsx` — nuevo componente
- `frontend/src/styles/AvisosSection.css` — nuevo archivo de estilos
- `frontend/src/services/api.js` — añadir llamada a `GET /api/avisos`

**Contexto técnico:**
Los "avisos" son comunicaciones puntuales creadas manualmente por el admin desde el panel privado (no provienen de Telegram). Son independientes de las noticias. El endpoint de lectura `GET /api/avisos` es público; la creación/edición/borrado requiere rol ADMIN. El modelo es simple: título, texto, fecha, visible (boolean). Solo se muestran en la landing los avisos con `visible=true`.

---

### Criterios de aceptación

**CA-11.2.1 — Existe el endpoint público GET /api/avisos**
- **Dado** que no se envía token de autorización
- **Cuando** se hace `GET /api/avisos`
- **Entonces** el servidor responde 200 con array JSON de avisos activos
- **Y** cada aviso contiene: `id`, `titulo`, `texto`, `fechaPublicacion`, `visible`

**CA-11.2.2 — Solo se devuelven avisos activos en el endpoint público**
- **Dado** que existen avisos con `visible=true` y `visible=false`
- **Cuando** se hace `GET /api/avisos` sin token
- **Entonces** la respuesta solo incluye los avisos con `visible=true`

**CA-11.2.3 — Los avisos están ordenados del más reciente al más antiguo**
- **Dado** que hay múltiples avisos activos
- **Cuando** se hace `GET /api/avisos`
- **Entonces** están ordenados por `fechaPublicacion` descendente

**CA-11.2.4 — La creación de avisos requiere rol ADMIN**
- **Dado** que se hace `POST /api/avisos` sin token
- **Entonces** el servidor responde 401 Unauthorized
- **Dado** que se hace `POST /api/avisos` con token de rol USER
- **Entonces** el servidor responde 403 Forbidden
- **Dado** que se hace `POST /api/avisos` con token de rol ADMIN
- **Entonces** el servidor responde 201 Created con el aviso creado

**CA-11.2.5 — La sección "Avisos" en la landing es accesible desde el menú**
- **Dado** que el visitante hace clic en `AVISOS` en la navbar
- **Entonces** la página navega a la sección `#avisos` sin error

**CA-11.2.6 — La sección muestra los avisos activos**
- **Dado** que hay avisos con `visible=true` en el backend
- **Cuando** se carga la sección de avisos en la landing
- **Entonces** se muestran con título, texto y fecha formateada

**CA-11.2.7 — Estado de carga, error y vacío**
- **Dado** que la sección se está montando:
  - En carga: muestra indicador visual (skeleton o spinner)
  - En error: muestra mensaje `No se pudieron cargar los avisos en este momento`
  - En vacío: muestra `No hay avisos en este momento`
- **Y** ninguno de estos estados rompe el layout del resto de la landing

**CA-11.2.8 — El modelo Aviso tiene los campos correctos en base de datos**
- **Dado** que se arranca el backend
- **Entonces** se crea automáticamente la tabla `avisos` con columnas: `id` (PK autoincrement), `titulo` (varchar 255 not null), `texto` (text not null), `fecha_publicacion` (timestamp not null), `visible` (boolean default true)

---

## US-11.3 — Panel de administración de avisos (CRUD privado)

**Como** administrador de la AFA Lluís Braille,
**quiero** crear, editar, activar/desactivar y eliminar avisos desde el panel privado,
**para que** pueda gestionar las comunicaciones que se muestran en la landing sin tocar código.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/controller/AvisoController.java` — completar endpoints CRUD
- `backend/src/main/java/com/example/demo/service/AvisoService.java` — lógica de negocio completa
- `frontend/src/components/AvisosAdmin.jsx` — nuevo componente del panel admin
- `frontend/src/styles/AvisosAdmin.css` — nuevo archivo de estilos
- `frontend/src/App.jsx` — añadir ruta `/avisos-admin` al panel privado
- `frontend/src/components/NavbarPrivate.jsx` (o `Navigation.jsx`) — añadir enlace a "Avisos"
- `frontend/src/services/api.js` — añadir llamadas CRUD de avisos

**Contexto técnico:**
Esta historia completa el ciclo de vida de los avisos: el admin puede gestionarlos desde el panel y se publican en la landing. Requiere que EPIC 2 (roles) esté implementado, ya que solo los ADMIN pueden escribir. La UI del panel sigue el mismo patrón que `UserList.jsx`: tabla con botones de acción.

---

### Criterios de aceptación

**CA-11.3.1 — El endpoint POST /api/avisos crea un aviso correctamente**
- **Dado** que se hace `POST /api/avisos` con token ADMIN y body `{ "titulo": "Aviso importante", "texto": "...", "visible": true }`
- **Entonces** el servidor responde 201 Created
- **Y** el aviso aparece en `GET /api/avisos`

**CA-11.3.2 — El endpoint PUT /api/avisos/{id} actualiza el aviso**
- **Dado** que existe un aviso con id 1
- **Cuando** se hace `PUT /api/avisos/1` con token ADMIN y body con `visible: false`
- **Entonces** el servidor responde 200 OK
- **Y** el aviso ya no aparece en `GET /api/avisos` (público, solo devuelve visibles)

**CA-11.3.3 — El endpoint DELETE /api/avisos/{id} elimina el aviso**
- **Dado** que existe un aviso con id 1
- **Cuando** se hace `DELETE /api/avisos/1` con token ADMIN
- **Entonces** el servidor responde 204 No Content
- **Y** el aviso no aparece en ninguna consulta posterior

**CA-11.3.4 — El panel de admin muestra la lista de avisos**
- **Dado** que un usuario ADMIN accede a `/avisos-admin`
- **Entonces** ve una tabla o lista con todos los avisos (activos e inactivos)
- **Y** cada fila muestra: título, fecha, estado (visible/oculto) y botones de acción (editar, activar/desactivar, eliminar)

**CA-11.3.5 — El admin puede crear un nuevo aviso desde el panel**
- **Dado** que el admin hace clic en `+ Nuevo Aviso`
- **Cuando** rellena título, texto y hace clic en guardar
- **Entonces** el aviso se crea con `visible=true` por defecto y aparece en la lista

**CA-11.3.6 — El admin puede activar/desactivar un aviso**
- **Dado** que el admin ve un aviso en la lista
- **Cuando** hace clic en el toggle o botón de visibilidad
- **Entonces** el estado `visible` se invierte y la tabla se actualiza sin recargar la página completa

**CA-11.3.7 — El admin puede eliminar un aviso con confirmación**
- **Dado** que el admin hace clic en eliminar un aviso
- **Entonces** aparece un diálogo de confirmación antes de eliminar
- **Y** solo si confirma, el aviso se elimina del backend y de la tabla

**CA-11.3.8 — Los endpoints del CRUD responden correctamente ante errores**
- **Dado** que se intenta actualizar o eliminar un aviso con id inexistente
- **Entonces** el servidor responde 404 Not Found con mensaje descriptivo

---

## Orden de implementación sugerido

```
US-11.1 (Noticias en la landing)
  → solo requiere que GET /api/telegram/messages sea público (ya está en EPIC 1 US-1.3)
  → puede hacerse antes que US-11.2 y US-11.3

US-11.2 (Avisos en la landing — modelo + endpoint público)
  → requiere crear la entidad Aviso en el backend
  → puede hacerse en paralelo con US-11.1

US-11.3 (CRUD de avisos en panel admin)
  → requiere US-11.2 (el modelo y servicio deben existir)
  → requiere EPIC 2 US-2.3 (roles) para proteger los endpoints de escritura
```

## Definición de hecho (DoD) compartida para todas las US de este epic

- El código compila sin errores ni warnings nuevos
- Los endpoints públicos (`GET /api/avisos`, `GET /api/telegram/messages`) responden sin token
- Los endpoints de escritura de avisos responden 401/403 sin token ADMIN
- Cada componente nuevo tiene tests con Vitest + RTL que cubren:
  - El estado de carga
  - El renderizado correcto de datos (mock de la API)
  - El estado de error
  - El estado vacío
- El backend tiene tests de integración (`@SpringBootTest` + `MockMvc`) que cubren:
  - `GET /api/avisos` sin token → 200
  - `POST /api/avisos` sin token → 401
  - `POST /api/avisos` con token USER → 403
  - `POST /api/avisos` con token ADMIN → 201
- La sección de avisos y noticias no rompe el layout si el backend no está disponible
- Los archivos modificados no contienen credenciales en texto plano
