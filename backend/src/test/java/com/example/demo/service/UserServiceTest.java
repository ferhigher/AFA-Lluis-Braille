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
