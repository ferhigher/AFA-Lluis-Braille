package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        logger.debug("========================================");
        logger.debug("🔍 JWT FILTER - Procesando petición");
        logger.debug("Method: {} {}", request.getMethod(), request.getRequestURI());
        
        try {
            String jwt = parseJwt(request);
            
            if (jwt == null) {
                logger.warn("⚠️ No se encontró token JWT en el header Authorization");
                logger.warn("Headers disponibles:");
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    logger.warn("  - {}: {}", headerName, request.getHeader(headerName));
                }
            } else {
                logger.debug("✅ Token JWT encontrado (primeros 20 chars): {}...", jwt.substring(0, Math.min(20, jwt.length())));
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    logger.debug("✅ Token JWT válido");
                    
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    logger.debug("👤 Username extraído del token: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.debug("✅ UserDetails cargado para: {}", username);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("✅ Autenticación establecida en SecurityContext");
                } else {
                    logger.warn("❌ Token JWT inválido o expirado");
                }
            }
        } catch (Exception e) {
            logger.error("========================================");
            logger.error("❌ ERROR en JWT Filter");
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("Tipo: {}", e.getClass().getName());
            logger.error("Stack trace:", e);
            logger.error("========================================");
        }

        logger.debug("🔄 Continuando con la cadena de filtros...");
        logger.debug("========================================");
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        logger.debug("📦 Header Authorization: {}", headerAuth != null ? 
                    (headerAuth.length() > 30 ? headerAuth.substring(0, 30) + "..." : headerAuth) : "null");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            logger.debug("✂️ Token extraído (sin 'Bearer '): {}...", 
                        token.substring(0, Math.min(20, token.length())));
            return token;
        }

        logger.debug("⚠️ Header Authorization no tiene formato 'Bearer <token>'");
        return null;
    }
}
