package com.example.demo.controller;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.model.User;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        logger.info("========================================");
        logger.info("POST /api/auth/login - INICIO");
        logger.info("Username recibido: {}", loginRequest.getUsername());
        logger.info("========================================");
        
        // Validar errores de binding
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
                logger.error("Error de validación en campo '{}': {}", error.getField(), error.getDefaultMessage());
            }
            logger.error("Login rechazado por errores de validación");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            logger.info("Login exitoso para usuario: {}", loginRequest.getUsername());
            logger.info("Token generado (primeros 20 chars): {}...", jwtResponse.getToken().substring(0, 20));
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("========================================");
            logger.error("ERROR EN LOGIN");
            logger.error("Usuario: {}", loginRequest.getUsername());
            logger.error("Tipo de excepción: {}", e.getClass().getName());
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("========================================");
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciales inválidas");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest, BindingResult bindingResult) {
        logger.info("========================================");
        logger.info("POST /api/auth/signup - INICIO");
        logger.info("========================================");
        logger.info("Datos recibidos en el controlador:");
        logger.info("  Name: {}", signUpRequest.getName());
        logger.info("  Username: {}", signUpRequest.getUsername());
        logger.info("  Email: {}", signUpRequest.getEmail());
        logger.info("  Phone: {}", signUpRequest.getPhone());
        logger.info("  Password presente: {}", signUpRequest.getPassword() != null && !signUpRequest.getPassword().isEmpty());
        
        // Validar errores de binding
        if (bindingResult.hasErrors()) {
            logger.error("========================================");
            logger.error("ERRORES DE VALIDACIÓN DETECTADOS:");
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                String errorMsg = error.getDefaultMessage();
                errors.put(error.getField(), errorMsg);
                logger.error("  Campo '{}': {}", error.getField(), errorMsg);
                logger.error("    Valor rechazado: {}", error.getRejectedValue());
                logger.error("    Código de error: {}", error.getCode());
            }
            logger.error("Total de errores: {}", bindingResult.getErrorCount());
            logger.error("========================================");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        
        logger.info("Validación inicial exitosa, procesando registro...");
        
        try {
            User user = authService.registerUser(signUpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuario registrado exitosamente");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("id", user.getId());
            
            logger.info("========================================");
            logger.info("REGISTRO COMPLETADO EXITOSAMENTE");
            logger.info("  ID: {}", user.getId());
            logger.info("  Username: {}", user.getUsername());
            logger.info("  Email: {}", user.getEmail());
            logger.info("========================================");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("========================================");
            logger.error("ERROR EN REGISTRO (RuntimeException)");
            logger.error("Tipo: {}", e.getClass().getName());
            logger.error("Mensaje: {}", e.getMessage());
            
            if (e.getCause() != null) {
                logger.error("Causa: {}", e.getCause().getMessage());
                logger.error("Tipo de causa: {}", e.getCause().getClass().getName());
            }
            
            logger.error("========================================");
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("========================================");
            logger.error("ERROR INESPERADO EN REGISTRO");
            logger.error("Tipo: {}", e.getClass().getName());
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("========================================");
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("details", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
