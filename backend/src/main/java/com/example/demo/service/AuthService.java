package com.example.demo.service;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("=== INICIO LOGIN ===");
        logger.info("Intento de login para usuario: {}", loginRequest.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication.getName());
            
            logger.info("Autenticación exitosa para: {}", loginRequest.getUsername());

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            logger.info("Usuario encontrado - ID: {}, Username: {}, Email: {}", 
                       user.getId(), user.getUsername(), user.getEmail());
            logger.info("=== FIN LOGIN EXITOSO ===");

            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getName());
        } catch (Exception e) {
            logger.error("=== ERROR EN LOGIN ===");
            logger.error("Usuario: {}", loginRequest.getUsername());
            logger.error("Tipo de error: {}", e.getClass().getName());
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("Stack trace completo:", e);
            throw e;
        }
    }

    public User registerUser(SignupRequest signUpRequest) {
        logger.info("=== INICIO REGISTRO ===");
        logger.info("Datos recibidos:");
        logger.info("  - Nombre: {}", signUpRequest.getName());
        logger.info("  - Username: {}", signUpRequest.getUsername());
        logger.info("  - Email: {}", signUpRequest.getEmail());
        logger.info("  - Teléfono: {}", signUpRequest.getPhone());
        logger.info("  - Password length: {}", signUpRequest.getPassword() != null ? signUpRequest.getPassword().length() : 0);

        try {
            // Validación de username
            logger.debug("Verificando si el username '{}' ya existe...", signUpRequest.getUsername());
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                logger.warn("USERNAME YA EXISTE: {}", signUpRequest.getUsername());
                throw new RuntimeException("Error: El username ya está en uso");
            }
            logger.debug("Username disponible: {}", signUpRequest.getUsername());

            // Validación de email
            logger.debug("Verificando si el email '{}' ya existe...", signUpRequest.getEmail());
            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                logger.warn("EMAIL YA EXISTE: {}", signUpRequest.getEmail());
                throw new RuntimeException("Error: El email ya está en uso");
            }
            logger.debug("Email disponible: {}", signUpRequest.getEmail());

            // Creación del usuario
            logger.debug("Creando nuevo usuario...");
            User user = new User();
            user.setName(signUpRequest.getName());
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPhone(signUpRequest.getPhone());
            
            logger.debug("Encriptando contraseña...");
            String encodedPassword = encoder.encode(signUpRequest.getPassword());
            logger.debug("Contraseña encriptada (primeros 20 chars): {}...", 
                        encodedPassword.substring(0, Math.min(20, encodedPassword.length())));
            user.setPassword(encodedPassword);

            logger.debug("Guardando usuario en la base de datos...");
            User savedUser = userRepository.save(user);
            
            logger.info("=== USUARIO REGISTRADO EXITOSAMENTE ===");
            logger.info("ID generado: {}", savedUser.getId());
            logger.info("Username: {}", savedUser.getUsername());
            logger.info("Email: {}", savedUser.getEmail());
            logger.info("Fecha creación: {}", savedUser.getCreatedAt());
            logger.info("=== FIN REGISTRO EXITOSO ===");
            
            return savedUser;
            
        } catch (Exception e) {
            logger.error("=== ERROR EN REGISTRO ===");
            logger.error("Datos del intento:");
            logger.error("  - Username: {}", signUpRequest.getUsername());
            logger.error("  - Email: {}", signUpRequest.getEmail());
            logger.error("Tipo de error: {}", e.getClass().getName());
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("Stack trace completo:", e);
            logger.error("=== FIN ERROR REGISTRO ===");
            throw e;
        }
    }
}
