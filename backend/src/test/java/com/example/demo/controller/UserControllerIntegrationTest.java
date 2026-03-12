package com.example.demo.controller;

import com.example.demo.model.User;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(status().is2xxSuccessful());

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

    // CA-1.2.1: GET /api/users no devuelve el campo password
    @Test
    void test_CA_1_2_1_getAllUsersDoesNotExposePassword() throws Exception {
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
}
