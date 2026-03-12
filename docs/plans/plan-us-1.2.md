# Plan de implementación — US-1.2: Ocultar contraseña en respuestas de la API

---

## 1. Resumen de la US

**Problema que resuelve:** `UserController` devuelve la entidad `User` directamente en todas sus respuestas. Esto expone el campo `password` (aunque sea el hash BCrypt) en `GET /api/users`, `GET /api/users/{id}`, `POST /api/users` y `PUT /api/users/{id}`. Las contraseñas hasheadas no deben viajar por la red innecesariamente.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/dto/UserResponseDTO.java` ← nuevo
- `backend/src/main/java/com/example/demo/controller/UserController.java` ← modificar
- `backend/src/main/java/com/example/demo/service/UserService.java` ← modificar (retorno DTO)
- `backend/src/test/java/com/example/demo/controller/UserControllerIntegrationTest.java` ← ampliar con CA-1.2.x

**Dependencias con otras US:**
- Requiere **US-1.1** completada (ya está): `createUser()` ya hashea la contraseña antes de guardar.
- No hay dependencias bloqueantes hacia adelante. US-1.3 no depende de esta.

---

## 2. Análisis del código actual

### `UserController.java` — devuelve entidad directa

```java
// línea 23-26
public ResponseEntity<List<User>> getAllUsers() {
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(users);    // ← expone password en JSON
}

// línea 29-33
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    return userService.getUserById(id)
            .map(ResponseEntity::ok)     // ← expone password en JSON
            .orElse(ResponseEntity.notFound().build());
}

// línea 36-45
public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
    User newUser = userService.createUser(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(newUser); // ← expone password
}

// línea 48-57
public ResponseEntity<?> updateUser(...) {
    User updatedUser = userService.updateUser(id, userDetails);
    return ResponseEntity.ok(updatedUser);  // ← expone password
}
```

### `User.java` — campos disponibles para el DTO

El modelo tiene: `id`, `name`, `email`, `phone`, `username`, `password`, `createdAt`.
El DTO debe incluir todos excepto `password`.

### Patrón DTO ya establecido

El proyecto ya tiene DTOs en `com.example.demo.dto` (`JwtResponse`, `LoginRequest`, `SignupRequest`), todos con `@Data` y `@AllArgsConstructor` de Lombok. Se sigue el mismo patrón.

### Riesgos y efectos secundarios

- **`UserDetailsServiceImpl`** carga el usuario desde la BD directamente con `userRepository.findByUsername()`, no pasa por el controlador. El DTO no afecta a Spring Security.
- **`UserControllerIntegrationTest`** ya escrito para US-1.1 hace `userRepository.findAll().get(0).getPassword()` — accede directamente al repositorio, no al JSON de respuesta. No se rompe.
- Los tests de CA-1.1.x siguen pasando sin cambios porque no leen `password` del JSON de respuesta.
- La firma de `UserService.getAllUsers()` y `getUserById()` cambia a devolver `UserResponseDTO`. Los tests unitarios de `UserServiceTest` no prueban esos métodos, así que no se rompen.

---

## 3. Plan de implementación paso a paso

### Paso 1 — Crear `UserResponseDTO`

**Archivo:** `backend/src/main/java/com/example/demo/dto/UserResponseDTO.java`

**Qué cambia:** Archivo nuevo. Contiene solo los campos públicos del usuario.

**Motivo:** Separar la representación de red de la entidad JPA. El DTO es el contrato de la API; la entidad es el contrato con la BD. Con Lombok `@Data` y `@AllArgsConstructor` se mantiene consistencia con el resto de DTOs del proyecto.

```java
package com.example.demo.dto;

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
}
```

---

### Paso 2 — Añadir método de conversión en `UserService`

**Archivo:** `backend/src/main/java/com/example/demo/service/UserService.java`

**Qué cambia:**
- Añadir método privado `toResponseDTO(User user)` que construye el DTO.
- Cambiar los retornos de `getAllUsers()`, `getUserById()`, `createUser()` y `updateUser()` para devolver `UserResponseDTO` en lugar de `User`.

**Motivo:** La conversión pertenece al servicio, no al controlador (separación de responsabilidades). El controlador solo coordina; el servicio decide qué datos exponer.

```java
package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id).map(this::toResponseDTO);
    }

    public UserResponseDTO createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return toResponseDTO(userRepository.save(user));
    }

    public UserResponseDTO updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());

        return toResponseDTO(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        userRepository.delete(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getUsername(),
                user.getCreatedAt()
        );
    }
}
```

---

### Paso 3 — Actualizar `UserController` para usar `UserResponseDTO`

**Archivo:** `backend/src/main/java/com/example/demo/controller/UserController.java`

**Qué cambia:**
- Importar `UserResponseDTO`.
- Cambiar las firmas de los métodos de respuesta de `User` a `UserResponseDTO`.
- Eliminar el import de `User` en los tipos de retorno (sigue usándose como `@RequestBody`).

**Motivo:** El controlador ahora recibe `User` en el body (para aprovechar las validaciones de Bean Validation en la entidad) y devuelve `UserResponseDTO` en la respuesta.

```java
package com.example.demo.controller;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            UserResponseDTO created = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            UserResponseDTO updated = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Usuario eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
```

---

## 4. Tests que cubren los criterios de aceptación

### 4.1 Tests unitarios — `UserServiceTest.java`

Añadir a `backend/src/test/java/com/example/demo/service/UserServiceTest.java`:

Cubre: CA-1.2.3 (createUser devuelve DTO sin password), CA-1.2.5 (campos públicos presentes)

```java
// CA-1.2.3 y CA-1.2.5: createUser devuelve DTO con campos públicos y sin password
@Test
void test_CA_1_2_3_createUserReturnsDTO() {
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User u = invocation.getArgument(0);
        u.setId(1L);
        return u;
    });

    UserResponseDTO result = userService.createUser(validUser);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("Test User");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getUsername()).isEqualTo("testuser");
    assertThat(result.getPhone()).isEqualTo("123456789");
}

// CA-1.2.5: getAllUsers devuelve lista de DTOs
@Test
void test_CA_1_2_5_getAllUsersReturnsDTOList() {
    when(userRepository.findAll()).thenReturn(List.of(validUser));

    List<UserResponseDTO> results = userService.getAllUsers();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getEmail()).isEqualTo("test@example.com");
}
```

**Nota:** Para compilar los nuevos tests, añadir al `setUp()` del test existente `validUser.setId(null)` (ya no hace falta, el id lo asigna JPA). Añadir también el import de `UserResponseDTO` y `List`.

---

### 4.2 Tests de integración — `UserControllerIntegrationTest.java`

Añadir a `backend/src/test/java/com/example/demo/controller/UserControllerIntegrationTest.java`:

Cubre: CA-1.2.1, CA-1.2.2, CA-1.2.3, CA-1.2.4, CA-1.2.5, CA-1.2.6

```java
// CA-1.2.1: GET /api/users no devuelve el campo password
@Test
void test_CA_1_2_1_getAllUsersDoesNotExposePassword() throws Exception {
    // Crear un usuario primero (vía repositorio para no depender del endpoint)
    User user = new User();
    user.setName("Test");
    user.setUsername("nopasstest");
    user.setEmail("nopass@example.com");
    user.setPhone("000");
    user.setPassword(passwordEncoder.encode("secret"));
    userRepository.save(user);

    mockMvc.perform(get("/api/users")
                    .with(user("admin").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].password").doesNotExist())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].email").exists())
            .andExpect(jsonPath("$[0].username").exists());
}

// CA-1.2.2: GET /api/users/{id} no devuelve el campo password
@Test
void test_CA_1_2_2_getUserByIdDoesNotExposePassword() throws Exception {
    User user = new User();
    user.setName("Test");
    user.setUsername("nopassbyid");
    user.setEmail("nopassbyid@example.com");
    user.setPhone("111");
    user.setPassword(passwordEncoder.encode("secret"));
    User saved = userRepository.save(user);

    mockMvc.perform(get("/api/users/" + saved.getId())
                    .with(user("admin").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.username").value("nopassbyid"));
}

// CA-1.2.3: POST /api/users no devuelve password en la respuesta
@Test
void test_CA_1_2_3_createUserResponseDoesNotExposePassword() throws Exception {
    String requestBody = """
            {
                "name": "Create Test",
                "username": "createtest",
                "email": "create@example.com",
                "phone": "222333444",
                "password": "miPassword123"
            }
            """;

    mockMvc.perform(post("/api/users")
                    .with(user("admin").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Create Test"))
            .andExpect(jsonPath("$.email").value("create@example.com"))
            .andExpect(jsonPath("$.username").value("createtest"))
            .andExpect(jsonPath("$.createdAt").exists());
}

// CA-1.2.4: PUT /api/users/{id} no devuelve password
@Test
void test_CA_1_2_4_updateUserResponseDoesNotExposePassword() throws Exception {
    User user = new User();
    user.setName("Before Update");
    user.setUsername("updatetest");
    user.setEmail("update@example.com");
    user.setPhone("555");
    user.setPassword(passwordEncoder.encode("secret"));
    User saved = userRepository.save(user);

    String requestBody = """
            {
                "name": "After Update",
                "username": "updatetest",
                "email": "update@example.com",
                "phone": "999",
                "password": "somepass"
            }
            """;

    mockMvc.perform(put("/api/users/" + saved.getId())
                    .with(user("admin").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.name").value("After Update"));
}

// CA-1.2.5: Todos los campos públicos están presentes en la respuesta
@Test
void test_CA_1_2_5_publicFieldsArePresent() throws Exception {
    String requestBody = """
            {
                "name": "Fields Test",
                "username": "fieldstest",
                "email": "fields@example.com",
                "phone": "123",
                "password": "miPassword123"
            }
            """;

    mockMvc.perform(post("/api/users")
                    .with(user("admin").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Fields Test"))
            .andExpect(jsonPath("$.email").value("fields@example.com"))
            .andExpect(jsonPath("$.phone").value("123"))
            .andExpect(jsonPath("$.username").value("fieldstest"))
            .andExpect(jsonPath("$.createdAt").exists());
}

// CA-1.2.6: El login sigue funcionando tras ocultar el password en respuestas
@Test
void test_CA_1_2_6_loginStillWorksAfterHidingPassword() throws Exception {
    String createBody = """
            {
                "name": "Login After DTO",
                "username": "logindto",
                "email": "logindto@example.com",
                "phone": "777",
                "password": "miPassword123"
            }
            """;

    mockMvc.perform(post("/api/users")
                    .with(user("admin").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
            .andExpect(status().isCreated());

    String loginBody = """
            {
                "username": "logindto",
                "password": "miPassword123"
            }
            """;

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
}
```

**Imports adicionales necesarios en `UserControllerIntegrationTest`:**
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

Primero obtener un token:
```bash
# Registrar un usuario de prueba
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","username":"admin","email":"admin@test.com","phone":"000","password":"admin123"}'

# Hacer login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
```

---

**CA-1.2.1 — GET /api/users no devuelve password**

```bash
curl -s -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" | jq '.[0] | keys'
```
Respuesta esperada: array de keys que NO incluye `"password"`. Debe incluir `"id"`, `"name"`, `"email"`, `"phone"`, `"username"`, `"createdAt"`.

---

**CA-1.2.2 — GET /api/users/{id} no devuelve password**

```bash
curl -s -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer $TOKEN" | jq 'keys'
```
Respuesta esperada: sin campo `"password"`.

---

**CA-1.2.3 — POST /api/users no devuelve password en la respuesta**

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test DTO","username":"testdto","email":"testdto@test.com","phone":"111","password":"pass123"}' \
  | jq 'keys'
```
Respuesta esperada: `201 Created`, body sin campo `"password"`.

---

**CA-1.2.4 — PUT /api/users/{id} no devuelve password**

```bash
curl -s -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Updated Name","username":"admin","email":"admin@test.com","phone":"999","password":"admin123"}' \
  | jq 'keys'
```
Respuesta esperada: `200 OK`, body sin campo `"password"`.

---

**CA-1.2.5 — Los campos públicos están presentes**

```bash
curl -s -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```
Respuesta esperada: objeto con `id`, `name`, `email`, `phone`, `username`, `createdAt`.

---

**CA-1.2.6 — El login sigue funcionando**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq '.token'
```
Respuesta esperada: token JWT no nulo.

---

### Verificación en el navegador

Esta US no tiene cambios en el frontend. La verificación visual es:
1. Ir a `http://localhost:5173` → Login
2. Abrir DevTools → Network → hacer cualquier operación en la sección "Usuarios"
3. Inspeccionar la respuesta JSON del endpoint `GET /api/users` → confirmar que no hay campo `password`

---

## 6. Definición de hecho (DoD)

- [ ] El código compila sin errores ni warnings nuevos (`mvn compile`)
- [ ] Todos los tests pasan en verde (`mvn test`)
- [ ] CA-1.2.1: `GET /api/users` no devuelve `password` — verificado con curl + jq
- [ ] CA-1.2.2: `GET /api/users/{id}` no devuelve `password` — verificado con curl
- [ ] CA-1.2.3: `POST /api/users` no devuelve `password` en la respuesta — verificado con curl
- [ ] CA-1.2.4: `PUT /api/users/{id}` no devuelve `password` en la respuesta — verificado con curl
- [ ] CA-1.2.5: Todos los campos públicos (`id`, `name`, `email`, `phone`, `username`, `createdAt`) están presentes
- [ ] CA-1.2.6: El login sigue funcionando correctamente tras el cambio
- [ ] No se han introducido nuevas rutas sin protección
- [ ] `UserDetailsServiceImpl` sigue leyendo el password directamente del repositorio (no del DTO)
- [ ] `application.properties` no contiene ningún secreto en texto plano *(pendiente US-1.4)*
