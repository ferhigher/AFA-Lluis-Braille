package com.example.demo.auth;

import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
                .andExpect(status().is2xxSuccessful());

        assertThat(userRepository.findByUsername("usertest"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(Role.USER));
    }

    @Test
    void test_CA_2_1_2_signup_ignoraRoleDelBody() throws Exception {
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
                .andExpect(status().is2xxSuccessful());

        assertThat(userRepository.findByUsername("hacker"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(Role.USER));
    }

    @Test
    void test_CA_2_1_4_login_devuelveRole_enJwtResponse() throws Exception {
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
                .andExpect(status().is2xxSuccessful());

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
}
