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
}
