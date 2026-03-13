package com.example.demo.auth;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
