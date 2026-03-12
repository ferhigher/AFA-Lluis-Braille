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
