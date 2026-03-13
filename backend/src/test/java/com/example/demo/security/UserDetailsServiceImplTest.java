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
