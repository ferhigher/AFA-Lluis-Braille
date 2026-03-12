package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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

        userService.createUser(validUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashedValue");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("miPassword123");
    }

    // CA-1.1.1: El hash no contiene el valor original
    @Test
    void test_CA_1_1_1_rawPasswordNeverStoredInDatabase() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$someHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(validUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).doesNotContain("miPassword123");
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
}
