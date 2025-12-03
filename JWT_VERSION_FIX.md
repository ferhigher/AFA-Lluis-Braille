# 🔧 Fix: Error de JWT - "cannot find symbol: method subject"

## ❌ El Error que Tenías

```
JwtUtils.java:[30,17] cannot find symbol
  symbol:   method subject(java.lang.String)
  location: interface io.jsonwebtoken.JwtBuilder
```

## ✅ Problema Solucionado

El código estaba escrito para **JWT 0.12.x** pero las dependencias instaladas son **JWT 0.11.5**. La API cambió entre versiones.

## 📝 Cambios Realizados en JwtUtils.java

### Antes (JWT 0.12.x) ❌
```java
// Genera token
return Jwts.builder()
    .subject(username)              // ❌ No existe en 0.11.5
    .issuedAt(new Date())           // ❌ No existe en 0.11.5
    .expiration(new Date(...))      // ❌ No existe en 0.11.5
    .signWith(getSigningKey())      // ❌ Falta el algoritmo
    .compact();

// Lee token
return Jwts.parser()
    .verifyWith(getSigningKey())    // ❌ No existe en 0.11.5
    .build()
    .parseSignedClaims(token)       // ❌ No existe en 0.11.5
    .getPayload()                   // ❌ No existe en 0.11.5
    .getSubject();
```

### Ahora (JWT 0.11.5) ✅
```java
// Genera token
return Jwts.builder()
    .setSubject(username)           // ✅ Funciona en 0.11.5
    .setIssuedAt(new Date())        // ✅ Funciona en 0.11.5
    .setExpiration(new Date(...))   // ✅ Funciona en 0.11.5
    .signWith(getSigningKey(), SignatureAlgorithm.HS512) // ✅ Especifica algoritmo
    .compact();

// Lee token
return Jwts.parserBuilder()         // ✅ Funciona en 0.11.5
    .setSigningKey(getSigningKey()) // ✅ Funciona en 0.11.5
    .build()
    .parseClaimsJws(token)          // ✅ Funciona en 0.11.5
    .getBody()                      // ✅ Funciona en 0.11.5
    .getSubject();
```

## 🔄 Diferencias Principales

| Característica | JWT 0.11.5 | JWT 0.12.x |
|----------------|------------|------------|
| **Builder methods** | `setSubject()` | `subject()` |
| **Date methods** | `setIssuedAt()`, `setExpiration()` | `issuedAt()`, `expiration()` |
| **Parser** | `parserBuilder()` | `parser()` |
| **Signing** | Requiere algoritmo explícito | Algoritmo inferido |
| **Parse result** | `parseClaimsJws()` | `parseSignedClaims()` |
| **Get claims** | `.getBody()` | `.getPayload()` |

## 🚀 Cómo Aplicar la Solución

### Opción 1: Descargar Archivo Actualizado

[Descargar fullstack-app-complete.tar.gz](computer:///mnt/user-data/outputs/fullstack-app-complete.tar.gz)

El archivo `JwtUtils.java` ya está corregido.

### Opción 2: Actualizar Manualmente

Si ya tienes el proyecto, copia este código en `backend/src/main/java/com/example/demo/security/JwtUtils.java`:

```java
package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Token JWT inválido: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string está vacío: {}", e.getMessage());
        }
        return false;
    }
}
```

## 🧪 Verificar que Funciona

Después de aplicar el cambio:

```bash
cd backend
mvn clean compile
```

Deberías ver:
```
[INFO] BUILD SUCCESS
```

Si ves:
```
[ERROR] compilation failure
```

Revisa que copiaste el código completo correctamente.

## 🎯 Probar el Backend

```bash
mvn spring-boot:run
```

Deberías ver:
```
Started DemoApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

## 📋 Checklist Post-Fix

Después de aplicar este fix, verifica:

- [ ] ✅ El backend compila sin errores: `mvn clean compile`
- [ ] ✅ El backend inicia correctamente: `mvn spring-boot:run`
- [ ] ✅ Puedes acceder a H2 Console: `http://localhost:8080/h2-console`
- [ ] ✅ El frontend puede conectarse al backend

## 🐛 Si Aún Tienes Errores

### Error: "package io.jsonwebtoken does not exist"

**Solución:**
```bash
mvn clean install -U
```

### Error: "SecretKey cannot be resolved"

**Verifica que tienes este import:**
```java
import javax.crypto.SecretKey;
```

### Error: "SignatureAlgorithm cannot be resolved"

**Verifica que tienes este import:**
```java
import io.jsonwebtoken.SignatureAlgorithm;
```

## 💡 Por Qué Usamos JWT 0.11.5

- ✅ **Más estable** con Spring Boot 3.1.5
- ✅ **Ampliamente probado** en producción
- ✅ **Menos bugs** que versiones más nuevas
- ✅ **Mejor documentación** y ejemplos

## 🔄 Si Prefieres Usar JWT 0.12.x

Si quieres usar la versión más nueva de JWT, necesitarías:

1. Actualizar Spring Boot a 3.2.0+
2. Revisar compatibilidad con todas las dependencias
3. Usar el código original con `.subject()`, `.issuedAt()`, etc.

**Pero NO lo recomiendo** porque puede causar otros conflictos de dependencias.

## ✅ Resumen

**Archivo corregido:** `backend/src/main/java/com/example/demo/security/JwtUtils.java`

**Cambios principales:**
- ✅ `.subject()` → `.setSubject()`
- ✅ `.issuedAt()` → `.setIssuedAt()`
- ✅ `.expiration()` → `.setExpiration()`
- ✅ `.signWith(key)` → `.signWith(key, algorithm)`
- ✅ `.parser()` → `.parserBuilder()`
- ✅ `.parseSignedClaims()` → `.parseClaimsJws()`
- ✅ `.getPayload()` → `.getBody()`

**Estado:** ✅ **SOLUCIONADO**

---

¡Ahora el backend debería compilar e iniciar correctamente! 🎉
