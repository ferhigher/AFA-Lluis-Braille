# Plan de implementación — US-2.1
## Modelo de roles en la base de datos y propagación al frontend

**EPIC 2 — Sistema de roles y permisos**
**Prioridad:** P1 — Alto
**Fecha de generación:** 2026-03-13

---

## Verificación de dependencias previas

| Dependencia | Estado | Notas |
|-------------|--------|-------|
| US-1.1 — Hashear contraseñas | ✅ Implementada | `UserService.createUser()` llama a `passwordEncoder.encode()`. `AuthService.registerUser()` también lo hace. |
| US-1.2 — Ocultar password en API | ✅ Implementada | `UserResponseDTO` existe y no incluye `password`. `UserController` devuelve el DTO. |
| US-1.3 — Proteger endpoints Telegram | ✅ Implementada | `SecurityConfig.java:64` permite GET `/api/telegram/messages` y autentica el resto. |
| US-1.4 — Externalizar secretos | ✅ Implementada | Variables de entorno en `application.properties`. |
| US-2.1 previa | ❌ No implementada | Es la US a implementar. `User.java` sin `role`, `UserDetailsServiceImpl:29` devuelve `new ArrayList<>()`. |

**Conclusión:** Todas las dependencias de EPIC 1 están satisfechas. Se puede proceder.

---

## 1. Resumen de la US

### Problema que resuelve

Sin el campo `role` en la entidad `User` y sin que `UserDetailsServiceImpl` cargue las authorities reales, el sistema de permisos de Spring Security es completamente inoperativo:

- `@PreAuthorize("hasRole('ADMIN')")` siempre falla porque `getAuthorities()` devuelve una lista vacía.
- El frontend no puede adaptar la interfaz porque `user.role` es `undefined`.
- Las US-2.2, US-2.3 y US-2.4 son bloqueadas por este estado.

US-2.1 establece la base técnica de todo el EPIC 2: columna en BD, propagación del rol por toda la cadena backend→JWT→localStorage→contexto React.

### Archivos afectados

| Archivo | Ruta completa |
|---------|---------------|
| `Role.java` (nuevo) | `backend/src/main/java/com/example/demo/model/Role.java` |
| `User.java` | `backend/src/main/java/com/example/demo/model/User.java` |
| `UserRepository.java` | `backend/src/main/java/com/example/demo/repository/UserRepository.java` |
| `UserDetailsServiceImpl.java` | `backend/src/main/java/com/example/demo/security/UserDetailsServiceImpl.java` |
| `AuthService.java` | `backend/src/main/java/com/example/demo/service/AuthService.java` |
| `JwtResponse.java` | `backend/src/main/java/com/example/demo/dto/JwtResponse.java` |
| `UserResponseDTO.java` | `backend/src/main/java/com/example/demo/dto/UserResponseDTO.java` |
| `UserService.java` | `backend/src/main/java/com/example/demo/service/UserService.java` |
| `AuthContext.jsx` | `frontend/src/context/AuthContext.jsx` |

### Dependencias con otras US del EPIC 2

- **US-2.2** (primer admin): necesita `Role.java` y `existsByRole()` en `UserRepository` — ambos creados en esta US.
- **US-2.3** (proteger endpoints): bloqueada hasta que esta US esté completada y verificada. Sin ella, `hasRole('ADMIN')` siempre falla.
- **US-2.4** (UI adaptada): necesita `user.role` en `AuthContext`, que se expone en esta US.

### Riesgos y efectos secundarios

| Riesgo | Mitigación |
|--------|-----------|
| Usuarios existentes sin `role` → columna nullable rompería las queries | Valor por defecto `'USER'` en la columna + anotación JPA `columnDefinition` |
| Constructor `@AllArgsConstructor` de `User` cambia firma → `UserService.createUser()` podría romper | `User` usa setters (`user.setXxx()`): sin impacto. Verificar que ningún `new User(...)` con todos los args esté en el código |
| `JwtResponse` tiene constructor explícito sin `role` en `AuthService` → compile error | El paso 7 actualiza la llamada en `AuthService.authenticateUser()` |
| `UserResponseDTO` constructor cambia → `UserService.toResponseDTO()` compila con error | El paso 9 actualiza la llamada |

---

## 2. Análisis del código actual

### `User.java` (líneas 18-52)

**Estado actual:** 7 campos (`id`, `name`, `email`, `phone`, `username`, `password`, `createdAt`). Sin campo `role`.

**Qué debe cambiar:** Añadir campo `role` de tipo enum `Role` con valor por defecto `USER`. Usar `@Enumerated(EnumType.STRING)` para almacenarlo como `VARCHAR` en PostgreSQL (compatible con JPA, no usa tipo `ENUM` nativo de PostgreSQL — correcto per `/postgresql-code-review`: evita la rigidez del tipo ENUM nativo y permite añadir valores sin migración DDL).

**Bloque que cambia:** Añadir después del campo `createdAt`:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'USER'")
private Role role = Role.USER;
```

---

### `UserDetailsServiceImpl.java` (línea 26-30)

**Estado actual:** Línea 29 devuelve `new ArrayList<>()` — authorities siempre vacías.

```java
return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        new ArrayList<>()   // ← BUG CRÍTICO
);
```

**Qué debe cambiar:** Reemplazar `new ArrayList<>()` por `List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))`. Esto hace que `@PreAuthorize("hasRole('ADMIN')")` funcione en US-2.3.

---

### `AuthService.java` (línea 56 y método `registerUser()`)

**Estado actual:**
- `authenticateUser()` línea 56: `return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getName())` — sin `role`.
- `registerUser()` líneas 95-108: construye `User` con setters pero **no asigna `role`** → quedaría como `null` si el campo no tiene default en JPA.

**Qué debe cambiar:**
- `authenticateUser()`: añadir `user.getRole()` a la llamada del constructor `JwtResponse`.
- `registerUser()`: añadir `user.setRole(Role.USER)` explícitamente (la lógica del primer ADMIN se añade en US-2.2, pero el default USER se establece aquí).

---

### `JwtResponse.java` (líneas 8-23)

**Estado actual:** Sin campo `role`. Constructor en línea 16 acepta 5 parámetros.

**Qué debe cambiar:** Añadir campo `private Role role` y actualizar el constructor para incluirlo como 6.º parámetro.

---

### `UserResponseDTO.java` (líneas 10-17)

**Estado actual:** Sin campo `role`. Constructor con 6 parámetros sin rol.

**Qué debe cambiar:** Añadir campo `private Role role` y actualizar el constructor.

---

### `UserService.java` (línea 58-67, método `toResponseDTO`)

**Estado actual:** `toResponseDTO()` construye `UserResponseDTO` con 6 args sin `role`.

**Qué debe cambiar:** Añadir `user.getRole()` como 7.º argumento en la llamada al constructor.

---

### `UserRepository.java` (líneas 10-19)

**Estado actual:** Tiene `existsByEmail`, `existsByUsername`, `findByEmail`, `findByUsername`. Sin método de consulta por rol.

**Qué debe cambiar:** Añadir `boolean existsByRole(Role role)` — necesario para la lógica del primer ADMIN en US-2.2. Se crea aquí porque US-2.1 crea el enum `Role` y US-2.2 depende de este método.

---

### `AuthContext.jsx` (líneas 39-46)

**Estado actual:** El objeto `value` expone: `user`, `login`, `signup`, `logout`, `isAuthenticated`, `loading`. Sin `isAdmin`.

**Qué debe cambiar:** Añadir `isAdmin: user?.role === 'ADMIN'` al objeto `value`. `user.role` ya estará disponible tras US-2.1 porque el login devuelve el `JwtResponse` con `role`. El campo persiste en `localStorage` porque se guarda el objeto completo (`JSON.stringify(response)`) y se restaura en el `useEffect` inicial.

---

## 3. Plan de implementación paso a paso

### Paso 1 — Crear enum `Role`
**Archivo:** `backend/src/main/java/com/example/demo/model/Role.java` (nuevo)
**Motivo:** Define los valores posibles del rol. Debe existir antes de referenciarlo en `User`.
**Orden:** Primero de todos.

```java
package com.example.demo.model;

public enum Role {
    ADMIN,
    USER
}
```

---

### Paso 2 — Añadir campo `role` a `User.java`
**Archivo:** `backend/src/main/java/com/example/demo/model/User.java`
**Motivo:** Persiste el rol en la columna `role` de la tabla `users`. `columnDefinition` garantiza el valor por defecto en la BD para registros existentes que no pasen por JPA (migración automática). `@Enumerated(EnumType.STRING)` almacena `'ADMIN'`/`'USER'` como texto — más seguro que `ORDINAL` ante reordenaciones futuras del enum.
**Orden:** Después del Paso 1.

Añadir import y campo en `User.java`:
```java
import com.example.demo.model.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Dentro de la clase, después del campo createdAt:
@Enumerated(EnumType.STRING)
@Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'USER'")
private Role role = Role.USER;
```

**Nota PostgreSQL (`/postgresql-code-review`):** Se usa `VARCHAR(20)` con `DEFAULT 'USER'` como check implícito. Alternativa más estricta: añadir un check constraint en la migración SQL. No se usa `ENUM` nativo de PostgreSQL para mantener compatibilidad con JPA y facilitar añadir valores futuros sin `ALTER TYPE`.

---

### Paso 3 — Añadir `existsByRole` a `UserRepository`
**Archivo:** `backend/src/main/java/com/example/demo/repository/UserRepository.java`
**Motivo:** Necesario para que US-2.2 pueda comprobar si existe algún ADMIN antes de asignar rol al nuevo usuario. Se crea aquí porque `Role` ya existe desde el Paso 1.
**Orden:** Después del Paso 1.

```java
import com.example.demo.model.Role;

boolean existsByRole(Role role);
```

---

### Paso 4 — Corregir `UserDetailsServiceImpl` para cargar authorities reales
**Archivo:** `backend/src/main/java/com/example/demo/security/UserDetailsServiceImpl.java`
**Motivo:** Es el bloqueante más crítico del EPIC. Sin este cambio, `@PreAuthorize` siempre falla.
**Orden:** Después del Paso 2 (necesita `user.getRole()`).

Reemplazar:
```java
import java.util.ArrayList;
// ...
new ArrayList<>()
```

Por:
```java
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
// ...
List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
```

---

### Paso 5 — Actualizar `AuthService.registerUser()` para asignar `Role.USER` por defecto
**Archivo:** `backend/src/main/java/com/example/demo/service/AuthService.java`
**Motivo:** Sin asignación explícita, el campo `role` sería `null` en la BD si el valor por defecto JPA (`= Role.USER` en la entidad) no se propaga correctamente con todos los proveedores JPA. Se hace explícito para claridad y corrección.
**Orden:** Después del Paso 2.

Añadir después de `user.setPassword(encodedPassword)` en `registerUser()`:
```java
import com.example.demo.model.Role;
// ...
user.setRole(Role.USER);
```

**Nota (`/java-refactoring-extract-method`):** La lógica de asignación de rol en US-2.1 es una sola línea (`user.setRole(Role.USER)`), por lo que no supera el umbral de 10 líneas para extraer a método privado. En US-2.2, si la lógica condicional del primer ADMIN supera ese umbral, se extraerá a `private Role determineRoleForNewUser()`.

---

### Paso 6 — Añadir campo `role` a `JwtResponse`
**Archivo:** `backend/src/main/java/com/example/demo/dto/JwtResponse.java`
**Motivo:** El frontend necesita recibir el rol en la respuesta del login para exponerlo en `AuthContext`.
**Orden:** Antes del Paso 7 (que actualiza la llamada al constructor).

Reemplazar la clase entera:
```java
package com.example.demo.dto;

import com.example.demo.model.Role;
import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String name;
    private Role role;

    public JwtResponse(String token, Long id, String username, String email, String name, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.role = role;
    }
}
```

---

### Paso 7 — Actualizar la llamada al constructor `JwtResponse` en `AuthService.authenticateUser()`
**Archivo:** `backend/src/main/java/com/example/demo/service/AuthService.java`
**Motivo:** La llamada en la línea 56 usa el constructor antiguo (5 parámetros) — compile error sin esta actualización.
**Orden:** Después del Paso 6.

Reemplazar:
```java
return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getName());
```

Por:
```java
return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getName(), user.getRole());
```

---

### Paso 8 — Añadir campo `role` a `UserResponseDTO`
**Archivo:** `backend/src/main/java/com/example/demo/dto/UserResponseDTO.java`
**Motivo:** `GET /api/users` debe devolver el rol en cada usuario (CA-2.1.5). Sin este campo, los listados de usuarios no exponen el rol.
**Orden:** Antes del Paso 9.

```java
package com.example.demo.dto;

import com.example.demo.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String username;
    private LocalDateTime createdAt;
    private Role role;
}
```

---

### Paso 9 — Actualizar `UserService.toResponseDTO()` para incluir `role`
**Archivo:** `backend/src/main/java/com/example/demo/service/UserService.java`
**Motivo:** El constructor de `UserResponseDTO` ahora requiere 7 argumentos. Sin este cambio, compile error.
**Orden:** Después del Paso 8.

Reemplazar el método `toResponseDTO`:
```java
private UserResponseDTO toResponseDTO(User user) {
    return new UserResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getUsername(),
            user.getCreatedAt(),
            user.getRole()
    );
}
```

---

### Paso 10 — Añadir `isAdmin` al contexto React `AuthContext.jsx`
**Archivo:** `frontend/src/context/AuthContext.jsx`
**Motivo:** Evitar duplicar el predicado `user?.role === 'ADMIN'` en múltiples componentes (`/vercel-react-best-practices`: no calcular en cada render, usar `useAuth()` directamente). `isAdmin` se calcula una vez en el contexto y queda disponible para todos los consumidores.
**Orden:** Después de que el backend compile y responda el rol en el login.

Modificar el objeto `value` en `AuthContext.jsx`:
```jsx
const value = {
  user,
  login,
  signup,
  logout,
  isAuthenticated: !!user,
  isAdmin: user?.role === 'ADMIN',
  loading
};
```

**Nota (`/vercel-react-best-practices`):**
- `isAdmin` se deriva de `user` en el mismo render del Provider, sin cálculo adicional.
- Los componentes consumen `isAdmin` directamente con `const { isAdmin } = useAuth()` — sin prop drilling.
- La ocultación en US-2.4 usará `{isAdmin && <button>...</button>}` (renderizado condicional, no `display: none`).

---

## 4. Tests

### CA-2.1.1 — La columna `role` existe en la tabla `users`

**Tipo:** Integración
**Archivo:** `backend/src/test/java/com/example/demo/auth/RoleModelIntegrationTest.java`

```java
package com.example.demo.auth;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleModelIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void test_CA_2_1_1_nuevaEntidad_tieneRoleUserPorDefecto() {
        User user = new User();
        user.setName("Test");
        user.setEmail("test@test.com");
        user.setUsername("testuser");
        user.setPassword("hashedpassword");

        User saved = userRepository.save(user);

        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void test_CA_2_1_1_roleNoEsNull_enUsuarioGuardado() {
        User user = new User();
        user.setName("Test");
        user.setEmail("test2@test.com");
        user.setUsername("testuser2");
        user.setPassword("hashedpassword");

        User saved = userRepository.save(user);

        assertThat(saved.getRole()).isNotNull();
    }
}
```

---

### CA-2.1.2 — Signup asigna `USER` e ignora `role` del body

**Tipo:** Integración
**Archivo:** `backend/src/test/java/com/example/demo/auth/AuthControllerIntegrationTest.java`

```java
package com.example.demo.auth;

import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void test_CA_2_1_2_signup_asignaRoleUser_porDefecto() throws Exception {
        String signupBody = """
            {
                "name": "Usuario Test",
                "username": "usertest",
                "email": "usertest@test.com",
                "password": "Password1",
                "phone": "600000000"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody))
                .andExpect(status().isOk());

        assertThat(userRepository.findByUsername("usertest"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(Role.USER));
    }

    @Test
    void test_CA_2_1_2_signup_ignoraRoleDelBody() throws Exception {
        // Aunque se envíe role=ADMIN en el body, SignupRequest no tiene ese campo
        // y el sistema asigna USER
        String signupBody = """
            {
                "name": "Atacante",
                "username": "hacker",
                "email": "hacker@test.com",
                "password": "Password1",
                "phone": "600000001",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody))
                .andExpect(status().isOk());

        assertThat(userRepository.findByUsername("hacker"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(Role.USER));
    }
}
```

---

### CA-2.1.3 — `UserDetailsServiceImpl` carga el rol como `GrantedAuthority`

**Tipo:** Unitario
**Archivo:** `backend/src/test/java/com/example/demo/security/UserDetailsServiceImplTest.java`

```java
package com.example.demo.security;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void test_CA_2_1_3_adminUser_tieneAuthority_ROLE_ADMIN() {
        User adminUser = buildUser("admin", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void test_CA_2_1_3_regularUser_tieneAuthority_ROLE_USER() {
        User regularUser = buildUser("user1", Role.USER);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        UserDetails details = userDetailsService.loadUserByUsername("user1");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void test_CA_2_1_3_authorities_nuncaEstaVacia() {
        User user = buildUser("anyuser", Role.USER);
        when(userRepository.findByUsername("anyuser")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("anyuser");

        assertThat(details.getAuthorities()).isNotEmpty();
    }

    private User buildUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("$2a$10$hashedpassword");
        user.setRole(role);
        return user;
    }
}
```

---

### CA-2.1.4 — El login devuelve el rol en la respuesta

**Tipo:** Integración
**Archivo:** `backend/src/test/java/com/example/demo/auth/AuthControllerIntegrationTest.java` (ampliado)

```java
    @Test
    void test_CA_2_1_4_login_devuelveRole_enJwtResponse() throws Exception {
        // Preparar: registrar un usuario
        String signupBody = """
            {
                "name": "Login Test",
                "username": "logintest",
                "email": "logintest@test.com",
                "password": "Password1",
                "phone": "600000002"
            }
            """;
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody))
                .andExpect(status().isOk());

        // Verificar: login devuelve role
        String loginBody = """
            {"username": "logintest", "password": "Password1"}
            """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("logintest"));
    }
```

Añadir import al fichero:
```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
```

---

### CA-2.1.5 — `UserResponseDTO` incluye el rol

**Tipo:** Integración
**Archivo:** `backend/src/test/java/com/example/demo/user/UserControllerIntegrationTest.java`

```java
package com.example.demo.user;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setName("Admin");
        user.setEmail("admin@test.com");
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("Password1"));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test_CA_2_1_5_getUsers_devuelveRole_sinPassword() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void test_CA_2_1_5_getUsers_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

### CA-2.1.6 — `AuthContext` expone el rol del usuario (Frontend)

**Tipo:** Test de componente (Vitest + React Testing Library)
**Archivo:** `frontend/src/context/__tests__/AuthContext.test.jsx`

```jsx
import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthProvider, useAuth } from '../AuthContext'

// Componente auxiliar para consumir el contexto en tests
const RoleDisplay = () => {
  const { user, isAdmin, isAuthenticated } = useAuth()
  return (
    <div>
      <span data-testid="role">{user?.role}</span>
      <span data-testid="isAdmin">{isAdmin ? 'true' : 'false'}</span>
      <span data-testid="isAuthenticated">{isAuthenticated ? 'true' : 'false'}</span>
    </div>
  )
}

vi.mock('../../services/api', () => ({
  authService: {
    login: vi.fn(),
    signup: vi.fn(),
  }
}))

describe('AuthContext — CA-2.1.6', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('test_CA_2_1_6_sinLogin_isAdmin_esFalse', () => {
    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('isAdmin').textContent).toBe('false')
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
  })

  it('test_CA_2_1_6_usuarioADMIN_enLocalStorage_isAdmin_esTrue', () => {
    const adminResponse = {
      token: 'test-token',
      id: 1,
      username: 'adminafa',
      email: 'admin@afa.org',
      name: 'Admin AFA',
      role: 'ADMIN'
    }
    localStorage.setItem('token', adminResponse.token)
    localStorage.setItem('user', JSON.stringify(adminResponse))

    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('role').textContent).toBe('ADMIN')
    expect(screen.getByTestId('isAdmin').textContent).toBe('true')
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
  })

  it('test_CA_2_1_6_usuarioUSER_enLocalStorage_isAdmin_esFalse', () => {
    const userResponse = {
      token: 'test-token',
      id: 2,
      username: 'user1',
      email: 'user@afa.org',
      name: 'Usuario Regular',
      role: 'USER'
    }
    localStorage.setItem('token', userResponse.token)
    localStorage.setItem('user', JSON.stringify(userResponse))

    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('role').textContent).toBe('USER')
    expect(screen.getByTestId('isAdmin').textContent).toBe('false')
  })

  it('test_CA_2_1_6_rolePersisteEnLocalStorage_trasSalvarLogin', async () => {
    const { authService } = await import('../../services/api')
    authService.login.mockResolvedValue({
      token: 'jwt-token',
      id: 1,
      username: 'adminafa',
      email: 'admin@afa.org',
      name: 'Admin AFA',
      role: 'ADMIN'
    })

    let loginFn
    const CaptureLogin = () => {
      const { login } = useAuth()
      loginFn = login
      return null
    }

    render(
      <AuthProvider>
        <CaptureLogin />
        <RoleDisplay />
      </AuthProvider>
    )

    await act(async () => {
      await loginFn('adminafa', 'Password1')
    })

    const storedUser = JSON.parse(localStorage.getItem('user'))
    expect(storedUser.role).toBe('ADMIN')
    expect(screen.getByTestId('isAdmin').textContent).toBe('true')
  })
})
```

---

## 5. Verificación manual

### 5.1 Preparar el entorno

```bash
ls .env || cp .env.example .env
```

### 5.2 Levantar el backend

```bash
./start-backend.sh
# Verificar arranque:
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"x","password":"x"}' | head -c 100
```

### 5.3 Levantar el frontend

```bash
./start-frontend.sh
# Verificar en http://localhost:5173
```

### 5.4 Matriz de verificación por CA

---

**CA-2.1.1 — La columna `role` existe en la BD con valor por defecto `USER`**

```bash
# Verificar que Hibernate crea la columna (ver logs del arranque)
# O conectar a PostgreSQL y verificar:
# psql -U $DB_USER -d $DB_NAME -c "\d users"
# Debe mostrar columna: role | character varying(20) | not null | default 'USER'
```

---

**CA-2.1.2 — Signup asigna `USER` e ignora `role` del body**

```bash
# Registrar usuario (sin campo role → debe recibir USER)
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Usuario Test",
    "username": "usertest",
    "email": "usertest@afa.org",
    "password": "Password1",
    "phone": "600000000"
  }' | python3 -m json.tool

# Intentar forzar ADMIN desde el body (debe ignorarse)
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hacker",
    "username": "hacker",
    "email": "hacker@afa.org",
    "password": "Password1",
    "role": "ADMIN"
  }' | python3 -m json.tool

# Login del hacker → debe devolver role: USER
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker","password":"Password1"}' | python3 -m json.tool
# Verificar: "role": "USER"
```

Respuesta esperada: `{ ..., "role": "USER" }`
Qué confirma: el campo `role` del body del signup es ignorado.

---

**CA-2.1.3 — Authorities cargadas correctamente (verificación indirecta via token)**

```bash
# El token JWT codifica las authorities — se verifica en CA-2.1.4 y CA-2.3.x
# Verificación directa: intentar un endpoint protegido con token USER
# (cuando US-2.3 esté implementada). Por ahora verificar que el login no devuelve 500.
TOKEN_USER=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usertest","password":"Password1"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Token obtenido: ${TOKEN_USER:0:50}..."
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN_USER"
# Esperado: 200 (lectura permitida para cualquier autenticado)
```

---

**CA-2.1.4 — El login devuelve `role` en la respuesta**

```bash
# Registrar primer usuario → debe ser ADMIN (lógica de US-2.2)
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin AFA",
    "username": "adminafa",
    "email": "admin@afa.org",
    "password": "Admin1234",
    "phone": "600000000"
  }' | python3 -m json.tool

# Login del admin → verificar campo role en la respuesta
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminafa","password":"Admin1234"}' | python3 -m json.tool
# Verificar: "role": "ADMIN" (o "USER" si US-2.2 no está implementada aún)

# Login de usuario normal
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usertest","password":"Password1"}' | python3 -m json.tool
# Verificar: "role": "USER"
```

Respuesta esperada: `{ "token": "...", "type": "Bearer", "id": 1, "username": "adminafa", "email": "admin@afa.org", "name": "Admin AFA", "role": "ADMIN" }`
Qué confirma: CA-2.1.4 — el JwtResponse incluye el campo `role`.

---

**CA-2.1.5 — `GET /api/users` devuelve `role` y no `password`**

```bash
TOKEN_ADMIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminafa","password":"Admin1234"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN_ADMIN" | python3 -m json.tool
# Verificar: cada objeto tiene "role": "ADMIN"|"USER" y NO tiene "password"
```

Respuesta esperada: array con objetos `{ id, name, email, phone, username, createdAt, role }` sin `password`.
Qué confirma: CA-2.1.5 — `UserResponseDTO` incluye el rol; CA-1.2 no regresionada.

---

**CA-2.1.6 — El `AuthContext` expone `user.role` correctamente**

```bash
# No aplicable con curl — se verifica en el navegador (ver 5.5)
```

### 5.5 Verificación en el navegador

1. Abrir `http://localhost:5173`
2. Login con usuario ADMIN:
   - En DevTools > Application > Local Storage: verificar que el objeto `user` tiene `"role": "ADMIN"`
   - En DevTools > Console: `JSON.parse(localStorage.getItem('user')).role` → debe devolver `"ADMIN"`
3. Logout → Login con usuario USER:
   - Verificar que `JSON.parse(localStorage.getItem('user')).role` devuelve `"USER"`
4. Recargar la página con sesión activa:
   - El rol debe persistir (se restaura del localStorage en el `useEffect` de `AuthProvider`)

### 5.6 Parar la app

```bash
./stop-frontend.sh
./stop-backend.sh
```

---

## 6. Definición de hecho (DoD)

### Corrección funcional

- [ ] La columna `role` existe en la tabla `users` con tipo `VARCHAR(20)`, valor por defecto `'USER'`, `NOT NULL`
- [ ] `UserDetailsServiceImpl` devuelve `List.of(new SimpleGrantedAuthority("ROLE_X"))` — ya NO devuelve `new ArrayList<>()`
- [ ] El login (`POST /api/auth/login`) devuelve el campo `role` en el `JwtResponse`
- [ ] `AuthContext` expone `user.role` e `isAdmin` correctamente tras login y tras recarga de página
- [ ] Un usuario registrado vía signup recibe `role = USER` por defecto
- [ ] El campo `role` enviado en el body del signup es ignorado

### Seguridad de endpoints (pre-US-2.3)

- [ ] `GET /api/users` responde `200` con token USER (lectura permitida)
- [ ] `GET /api/telegram/messages` sigue respondiendo `200` sin token (no regresión)
- [ ] Sin token → `401` en endpoints protegidos (no regresión)

### Interfaz (pre-US-2.4)

- [ ] `user.role` disponible en `localStorage` tras login
- [ ] `isAdmin` disponible como predicado en cualquier componente via `useAuth()`

### Calidad

- [ ] Todos los tests del paso 4 pasan en verde (`mvn test` + `npm test`)
- [ ] No hay `new ArrayList<>()` como authorities en `UserDetailsServiceImpl`
- [ ] El predicado `isAdmin` no está duplicado en componentes React (vive solo en `AuthContext`)
- [ ] `SignupRequest.java` sigue sin campo `role` (el rol no puede forzarse desde el cliente)
- [ ] `application.properties` no contiene ningún secreto en texto plano (no regresión EPIC 1)
- [ ] El código compila sin errores ni warnings nuevos (`mvn compile`)

---

## Resumen de cambios por archivo

| Archivo | Tipo de cambio | Paso |
|---------|---------------|------|
| `Role.java` | Nuevo — enum con ADMIN/USER | 1 |
| `User.java` | Añadir campo `role` con `@Enumerated(STRING)` | 2 |
| `UserRepository.java` | Añadir `existsByRole(Role)` | 3 |
| `UserDetailsServiceImpl.java` | Reemplazar `new ArrayList<>()` por `SimpleGrantedAuthority` | 4 |
| `AuthService.java` | Añadir `user.setRole(Role.USER)` + actualizar constructor JwtResponse | 5, 7 |
| `JwtResponse.java` | Añadir campo `role` + actualizar constructor | 6 |
| `UserResponseDTO.java` | Añadir campo `role` + actualizar constructor | 8 |
| `UserService.java` | Añadir `user.getRole()` en `toResponseDTO()` | 9 |
| `AuthContext.jsx` | Añadir `isAdmin` al objeto `value` | 10 |
