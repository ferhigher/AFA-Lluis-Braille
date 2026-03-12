# Plan de implementación — US-1.3: Proteger endpoints de escritura de Telegram

---

## 1. Resumen de la US

**Problema que resuelve:** `SecurityConfig` pone toda la ruta `/api/telegram/**` en `permitAll()`. Esto deja `POST /api/telegram/manual` y `POST /api/telegram/fetch` completamente públicos: cualquier persona anónima puede inyectar noticias falsas o forzar sincronizaciones sin autenticarse. Solo `GET /api/telegram/messages` (lectura pública de noticias) tiene sentido dejarlo sin protección.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/config/SecurityConfig.java` ← único cambio de producción
- `backend/src/test/java/com/example/demo/controller/TelegramControllerIntegrationTest.java` ← nuevo (tests integración)

**Dependencias con otras US:**
- US-1.1 y US-1.2 completadas (ya están). No hay dependencias bloqueantes para esta US.
- US-1.4 (variables de entorno) es recomendable, pero no requerida para que esta US funcione.

---

## 2. Análisis del código actual

### `SecurityConfig.java:61-68` — el problema

```java
.authorizeHttpRequests(auth ->
        auth.requestMatchers(
                        new AntPathRequestMatcher("/api/auth/**"),
                        new AntPathRequestMatcher("/api/telegram/**"),  // ← BUG: todo público
                        new AntPathRequestMatcher("/h2-console/**")
                ).permitAll()
                .anyRequest().authenticated()
);
```

La regla `AntPathRequestMatcher("/api/telegram/**")` cubre todos los métodos HTTP sobre todas las sub-rutas de Telegram. Incluye los endpoints de escritura que deben estar protegidos.

### `TelegramController.java` — los tres endpoints

| Endpoint | Método | Debería ser |
|----------|--------|-------------|
| `/api/telegram/messages` | GET | Público (`permitAll`) |
| `/api/telegram/fetch` | POST | Autenticado |
| `/api/telegram/manual` | POST | Autenticado |

### Solución: reglas granulares por método HTTP

Spring Security permite combinar path y método HTTP en un mismo matcher. La regla resultante queda:

```java
.authorizeHttpRequests(auth ->
        auth
            .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/telegram/messages", "GET")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
            .anyRequest().authenticated()
);
```

Así `POST /api/telegram/fetch` y `POST /api/telegram/manual` caen en `.anyRequest().authenticated()`.

### Riesgos y efectos secundarios

- **`GET /api/telegram/messages`** seguirá siendo público — el frontend lo usa sin token para mostrar noticias en la landing.
- **`POST /api/telegram/fetch` y `/manual`** requerirán JWT. El frontend ya envía el header `Authorization` mediante el interceptor de Axios para todas las llamadas a `/api/**` — no requiere cambios en el frontend.
- **Tests existentes:** No hay tests de Telegram actualmente. Los nuevos tests cubren todos los CA desde cero.
- **H2 console** (`/h2-console/**`) sigue en `permitAll`, necesario para los tests de integración.

---

## 3. Plan de implementación paso a paso

### Paso 1 — Reemplazar la regla `/api/telegram/**` por reglas granulares en `SecurityConfig`

**Archivo:** `backend/src/main/java/com/example/demo/config/SecurityConfig.java`

**Qué cambia:** Sustituir el `AntPathRequestMatcher("/api/telegram/**")` genérico por un matcher específico para `GET /api/telegram/messages`. Los endpoints POST quedan cubiertos por `.anyRequest().authenticated()`.

**Motivo:** Mínimo privilegio: solo la lectura de mensajes es contenido público. Crear o sincronizar noticias son acciones de escritura que requieren identidad.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                    auth
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/telegram/messages", "GET")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                        .anyRequest().authenticated()
            );

    // Para H2 Console
    http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

---

## 4. Tests que cubren los criterios de aceptación

### Tests de integración — `TelegramControllerIntegrationTest.java`

**Archivo nuevo:** `backend/src/test/java/com/example/demo/controller/TelegramControllerIntegrationTest.java`

Cubre: CA-1.3.1, CA-1.3.2, CA-1.3.3, CA-1.3.4, CA-1.3.5

**Nota sobre CA-1.3.4 y CA-1.3.5:** El bot de Telegram no está configurado en el entorno de tests (token placeholder). Los tests verifican que el endpoint **no devuelve 401** (la autenticación funciona), no que la operación de Telegram tenga éxito — eso depende de la configuración del bot real (US-3.1).

```java
package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelegramControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // CA-1.3.1: GET /api/telegram/messages es público (sin token → 200)
    @Test
    void test_CA_1_3_1_getMessagesIsPublic() throws Exception {
        mockMvc.perform(get("/api/telegram/messages"))
                .andExpect(status().isOk());
    }

    // CA-1.3.2: POST /api/telegram/manual sin token → 401
    @Test
    void test_CA_1_3_2_createManualMessageRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/telegram/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"noticia falsa\"}"))
                .andExpect(status().isUnauthorized());
    }

    // CA-1.3.3: POST /api/telegram/fetch sin token → 401
    @Test
    void test_CA_1_3_3_fetchMessagesRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/telegram/fetch"))
                .andExpect(status().isUnauthorized());
    }

    // CA-1.3.4: POST /api/telegram/manual con token válido → no es 401
    // (puede ser 200 o 400 según el texto, pero nunca 401)
    @Test
    void test_CA_1_3_4_createManualMessageWithAuthIsNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/telegram/manual")
                        .with(user("admin").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"noticia válida\"}"))
                .andExpect(status().isNot(401));
    }

    // CA-1.3.5: POST /api/telegram/fetch con token válido → no es 401
    // (puede ser 200 o error de Telegram si el bot no está configurado, pero nunca 401)
    @Test
    void test_CA_1_3_5_fetchMessagesWithAuthIsNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/telegram/fetch")
                        .with(user("admin").roles("USER")))
                .andExpect(status().isNot(401));
    }
}
```

**Nota sobre `status().isNot(401)`:** MockMvc no tiene `isNot()` directamente. Usar `status().is(result -> result.getStatus() != 401)` o verificar con `andExpect(status().is2xxSuccessful()).or(status().isBadRequest())`. La forma más limpia es usar un `ResultMatcher` personalizado:

```java
// Helper reutilizable para los tests CA-1.3.4 y CA-1.3.5
private static ResultMatcher isNotUnauthorized() {
    return result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
}
```

Código completo con el helper incluido:

```java
package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelegramControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // CA-1.3.1: GET /api/telegram/messages es público (sin token → 200)
    @Test
    void test_CA_1_3_1_getMessagesIsPublic() throws Exception {
        mockMvc.perform(get("/api/telegram/messages"))
                .andExpect(status().isOk());
    }

    // CA-1.3.2: POST /api/telegram/manual sin token → 401
    @Test
    void test_CA_1_3_2_createManualMessageRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/telegram/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"noticia falsa\"}"))
                .andExpect(status().isUnauthorized());
    }

    // CA-1.3.3: POST /api/telegram/fetch sin token → 401
    @Test
    void test_CA_1_3_3_fetchMessagesRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/telegram/fetch"))
                .andExpect(status().isUnauthorized());
    }

    // CA-1.3.4: POST /api/telegram/manual con token válido → no es 401
    @Test
    void test_CA_1_3_4_createManualMessageWithAuthIsNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/telegram/manual")
                        .with(user("admin").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"noticia válida\"}"))
                .andExpect(isNotUnauthorized());
    }

    // CA-1.3.5: POST /api/telegram/fetch con token válido → no es 401
    @Test
    void test_CA_1_3_5_fetchMessagesWithAuthIsNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/telegram/fetch")
                        .with(user("admin").roles("USER")))
                .andExpect(isNotUnauthorized());
    }

    private ResultMatcher isNotUnauthorized() {
        return result -> assertThat(result.getResponse().getStatus())
                .as("El endpoint no debe devolver 401 cuando hay autenticación")
                .isNotEqualTo(401);
    }
}
```

---

## 5. Verificación manual

### Preparar el entorno

```bash
cp .env.example .env
# Editar .env con los valores locales si es necesario
```

### Levantar el backend

```bash
cd backend
mvn spring-boot:run
# Verificar que arranca en http://localhost:8080
```

### Levantar el frontend

```bash
cd frontend
npm run dev
# Verificar que arranca en http://localhost:5173
```

---

### Verificación de cada CA con curl

Obtener un token primero:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
```

---

**CA-1.3.1 — GET /api/telegram/messages sigue siendo público**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/telegram/messages
```
Respuesta esperada: `200`

---

**CA-1.3.2 — POST /api/telegram/manual sin token → 401**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/telegram/manual \
  -H "Content-Type: application/json" \
  -d '{"text": "noticia falsa"}'
```
Respuesta esperada: `401`

---

**CA-1.3.3 — POST /api/telegram/fetch sin token → 401**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/telegram/fetch
```
Respuesta esperada: `401`

---

**CA-1.3.4 — POST /api/telegram/manual con token → no es 401**

```bash
curl -s -X POST http://localhost:8080/api/telegram/manual \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"text": "noticia válida"}'
```
Respuesta esperada: `200 OK` con la noticia guardada (o `400` si el texto está vacío, pero nunca `401`).

---

**CA-1.3.5 — POST /api/telegram/fetch con token → no es 401**

```bash
curl -s -X POST http://localhost:8080/api/telegram/fetch \
  -H "Authorization: Bearer $TOKEN"
```
Respuesta esperada: `200 OK` o `400` si el bot de Telegram no está configurado. Nunca `401`.

---

### Verificación en el navegador

Esta US no tiene cambios en el frontend. La verificación visual es:
1. Ir a `http://localhost:5173` → Login
2. Ir a la sección de Noticias → hacer clic en "Actualizar Noticias" → debe funcionar (el interceptor de Axios ya envía el token)
3. Abrir una ventana de incógnito (sin sesión) → la sección de Noticias debe seguir mostrando las noticias existentes (GET público)
4. Intentar hacer la petición POST desde DevTools sin token → debe responder 401

---

## 6. Definición de hecho (DoD)

- [ ] El código compila sin errores ni warnings nuevos (`mvn compile`)
- [ ] Todos los tests pasan en verde (`mvn test`)
- [ ] CA-1.3.1: `GET /api/telegram/messages` sin token → 200 — verificado con curl
- [ ] CA-1.3.2: `POST /api/telegram/manual` sin token → 401 — verificado con curl
- [ ] CA-1.3.3: `POST /api/telegram/fetch` sin token → 401 — verificado con curl
- [ ] CA-1.3.4: `POST /api/telegram/manual` con token válido → no 401 — verificado con curl
- [ ] CA-1.3.5: `POST /api/telegram/fetch` con token válido → no 401 — verificado con curl
- [ ] No se han introducido nuevas rutas sin protección
- [ ] El frontend sigue funcionando: "Actualizar Noticias" y "Crear Noticia" operan correctamente con sesión activa
- [ ] `application.properties` no contiene ningún secreto en texto plano *(pendiente US-1.4)*
