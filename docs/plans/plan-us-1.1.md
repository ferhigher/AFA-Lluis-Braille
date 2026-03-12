# Plan de implementación — US-1.1: Hashear contraseñas en el CRUD de usuarios

---

## 1. Resumen de la US

**Problema que resuelve:** `UserService.createUser()` guarda el objeto `User` directamente en la base de datos sin codificar la contraseña. Cualquier usuario creado vía `POST /api/users` tiene su contraseña en texto plano. El signup normal (`AuthService.registerUser()`) ya usa BCrypt correctamente; el bug solo afecta al flujo del CRUD.

**Archivos afectados:**
- `backend/src/main/java/com/example/demo/service/UserService.java` ← único cambio de producción
- `backend/src/test/java/com/example/demo/service/UserServiceTest.java` ← nuevo (tests unitarios)
- `backend/src/test/java/com/example/demo/controller/UserControllerIntegrationTest.java` ← nuevo (tests integración)
- `backend/src/test/resources/application.properties` ← nuevo (config H2 para tests)

**Dependencias con otras US:** Ninguna bloqueante. `US-1.4` (variables de entorno) es recomendable hacerla antes, pero US-1.1 no la requiere para funcionar.

---

## 2. Análisis del código actual

### `UserService.java:25-29`
```java
public User createUser(User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
        throw new RuntimeException("El email ya está registrado");
    }
    return userRepository.save(user);   // ← BUG: password en plaintext
}
```
No hay ninguna referencia a `PasswordEncoder`. El objeto `user` llega del controlador con el password en texto plano y se persiste tal cual.

### `AuthService.java:31,102-105` — la referencia correcta
```java
@Autowired
private PasswordEncoder encoder;
// ...
String encodedPassword = encoder.encode(signUpRequest.getPassword());
user.setPassword(encodedPassword);
```
`AuthService` ya tiene `PasswordEncoder` inyectado y lo usa bien. El mismo bean está declarado en `SecurityConfig.java:52-54`.

### `User.java:41-43`
```java
@NotBlank(message = "La contraseña es obligatoria")
@Column(nullable = false)
private String password;
```
`@NotBlank` ya garantiza que password vacío retorna 400 cuando `@Valid` está activo en el controlador — CA-1.1.4 cubierto sin código adicional.

### `UserController.java:36`
```java
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
```
`@Valid` está presente, por lo que las validaciones de Bean Validation se ejecutan antes de llegar al servicio.

### Riesgos y efectos secundarios
- **Sin riesgo de doble-hash:** `AuthService.registerUser()` llama directamente a `userRepository.save()`, no a `UserService.createUser()`. Cambiar `UserService` no afecta al signup.
- **Usuarios existentes con plaintext:** Los usuarios ya creados en la DB con contraseña en claro no se migran automáticamente. Hay que documentarlo (o hacer un script SQL si hay datos reales). Para desarrollo local no es bloqueante.

---

## 3. Plan de implementación paso a paso

### Paso 1 — Inyectar `PasswordEncoder` en `UserService` y hashear en `createUser()`

**Archivo:** `backend/src/main/java/com/example/demo/service/UserService.java`

**Qué cambia:**
- Añadir import de `PasswordEncoder`
- Inyectar `PasswordEncoder` con `@Autowired`
- En `createUser()`, llamar a `passwordEncoder.encode(user.getPassword())` antes de `userRepository.save()`

**Motivo:** `PasswordEncoder` ya es un bean disponible en el contexto (`SecurityConfig.passwordEncoder()`). Inyectarlo aquí no añade dependencias nuevas.

```java
package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        userRepository.delete(user);
    }
}
```

**Nota clean-code:**
- Nombre `passwordEncoder` (no `encoder` ni `pe`) — expresivo y coherente con la responsabilidad.
- La lógica de encoding queda en el servicio, no en el controlador — única responsabilidad.
- No se añaden comentarios porque la línea `passwordEncoder.encode(...)` es autoexplicativa.

---

## 4. Tests que cubren los criterios de aceptación

### 4.1 Config de tests con H2

**Archivo nuevo:** `backend/src/test/resources/application.properties`

```properties
# H2 en memoria para tests
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# JWT para tests
jwt.secret=test_secret_key_long_enough_for_hs256_algorithm_minimum_256_bits
jwt.expiration=86400000

# Telegram (no usado en tests, pero requerido por la config)
telegram.bot.token=test_token
telegram.channel.username=@test_channel

logging.level.root=WARN
logging.level.com.example.demo=INFO
```

---

### 4.2 Tests unitarios de `UserService`

**Archivo nuevo:** `backend/src/test/java/com/example/demo/service/UserServiceTest.java`

Cubre: CA-1.1.1, CA-1.1.4

```java
package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setName("Test User");
        validUser.setUsername("testuser");
        validUser.setEmail("test@example.com");
        validUser.setPhone("123456789");
        validUser.setPassword("miPassword123");
    }

    // CA-1.1.1: La contraseña se hashea antes de guardar
    @Test
    void test_CA_1_1_1_passwordIsEncodedBeforeSaving() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("miPassword123")).thenReturn("$2a$10$hashedValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(validUser);

        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashedValue");
        assertThat(saved.getPassword()).isNotEqualTo("miPassword123");
        verify(passwordEncoder).encode("miPassword123");
        verify(userRepository).save(validUser);
    }

    // CA-1.1.1: El hash no contiene el valor original
    @Test
    void test_CA_1_1_1_rawPasswordNeverStoredInDatabase() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$someHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(validUser);

        assertThat(saved.getPassword()).doesNotContain("miPassword123");
    }

    // CA-1.1.1: passwordEncoder.encode() es llamado exactamente una vez
    @Test
    void test_CA_1_1_1_encoderCalledExactlyOnce() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(validUser);

        verify(passwordEncoder, times(1)).encode("miPassword123");
    }

    // CA-1.1.4: Email duplicado lanza excepción (regresión de validación existente)
    @Test
    void test_CA_1_1_existingEmailThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El email ya está registrado");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
}
```

---

### 4.3 Tests de integración del endpoint

**Archivo nuevo:** `backend/src/test/java/com/example/demo/controller/UserControllerIntegrationTest.java`

Cubre: CA-1.1.1, CA-1.1.2, CA-1.1.3, CA-1.1.4

```java
package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // CA-1.1.1: La contraseña persiste como BCrypt hash ($2a$)
    @Test
    void test_CA_1_1_1_passwordStoredAsBcryptHash() throws Exception {
        String requestBody = """
                {
                    "name": "Test User",
                    "username": "testuser",
                    "email": "test@example.com",
                    "phone": "123456789",
                    "password": "miPassword123"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(user("admin").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        String storedPassword = userRepository.findAll().get(0).getPassword();
        assertThat(storedPassword).startsWith("$2a$");
        assertThat(storedPassword).isNotEqualTo("miPassword123");
    }

    // CA-1.1.2: Usuario creado vía CRUD puede hacer login
    @Test
    void test_CA_1_1_2_userCreatedViaCrudCanLogin() throws Exception {
        String createBody = """
                {
                    "name": "Login Test",
                    "username": "logintest",
                    "email": "login@example.com",
                    "phone": "111222333",
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
                    "username": "logintest",
                    "password": "miPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // CA-1.1.4: Password vacío retorna 400 Bad Request
    @Test
    void test_CA_1_1_4_emptyPasswordReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "name": "Test User",
                    "username": "testuser2",
                    "email": "test2@example.com",
                    "phone": "123456789",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(user("admin").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // CA-1.1.3: El registro normal (signup) sigue funcionando (sin doble hash)
    @Test
    void test_CA_1_1_3_normalSignupStillWorks() throws Exception {
        String signupBody = """
                {
                    "name": "Signup User",
                    "username": "signupuser",
                    "email": "signup@example.com",
                    "phone": "999888777",
                    "password": "miPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isOk());

        String loginBody = """
                {
                    "username": "signupuser",
                    "password": "miPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
```

---

## 5. Verificación manual

### Preparar el entorno

```bash
# Si no existe .env, copiar desde el ejemplo
cp .env.example .env
# Editar .env con los valores locales (DB, JWT_SECRET, etc.)
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

**CA-1.1.1 — La contraseña se guarda como hash BCrypt**

Primero obtener un token (necesitas un usuario existente o usar el de signup):
```bash
TOKEN="<pega aquí tu JWT>"

curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test Hash","username":"testhash","email":"testhash@test.com","phone":"123","password":"miPassword123"}'
```
Respuesta esperada: `201 Created`.

Verificar en PostgreSQL:
```sql
SELECT username, password FROM users WHERE username = 'testhash';
-- El campo password debe comenzar con $2a$10$
```

---

**CA-1.1.2 — El usuario creado vía CRUD puede hacer login**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testhash","password":"miPassword123"}'
```
Respuesta esperada: `200 OK` con campo `token` en el body JSON.

---

**CA-1.1.3 — El signup normal sigue funcionando**

```bash
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Signup Normal","username":"signupnormal","email":"signupnormal@test.com","phone":"456","password":"pass123"}'

curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"signupnormal","password":"pass123"}'
```
Respuesta esperada: `200 OK` con `token` — confirma que no hay doble hash.

---

**CA-1.1.4 — Password vacío retorna 400**

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Sin Pass","username":"sinpass","email":"sinpass@test.com","phone":"789","password":""}'
```
Respuesta esperada: `400`

---

### Verificación en el navegador

Esta US no tiene cambios en el frontend. La verificación visual es:
1. Ir a `http://localhost:5173` → Login con un usuario existente
2. Ir al panel de gestión de usuarios → crear un nuevo usuario con contraseña
3. Cerrar sesión → hacer login con el nuevo usuario → debe funcionar

---

## 6. Definición de hecho (DoD)

- [ ] El código compila sin errores ni warnings nuevos (`mvn compile`)
- [ ] Todos los tests generados en el paso 4 pasan en verde (`mvn test`)
- [ ] CA-1.1.1: Verificado en PostgreSQL que el campo `password` empieza con `$2a$`
- [ ] CA-1.1.2: Usuario creado vía `POST /api/users` puede hacer login exitosamente
- [ ] CA-1.1.3: Usuario creado vía `POST /api/auth/signup` sigue pudiendo hacer login
- [ ] CA-1.1.4: `POST /api/users` con password vacío retorna 400
- [ ] No se han introducido nuevas rutas sin protección
- [ ] El archivo `UserService.java` modificado no contiene credenciales en texto plano
- [ ] `application.properties` no contiene ningún secreto en texto plano *(pendiente US-1.4)*

---

> **Nota sobre US pendientes:** US-1.4 (variables de entorno) debería implementarse primero o en paralelo — `application.properties` actualmente tiene el JWT secret y credenciales de DB en texto plano. Esta US-1.1 es independiente y puede implementarse igualmente, pero antes de commitear hay que asegurarse de no dejar secretos reales en el repo.
