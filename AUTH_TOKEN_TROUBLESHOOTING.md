# 🔒 Solución: Error de Autenticación en Endpoints Protegidos

## ❌ El Error que Estás Viendo

```
ERROR c.e.demo.security.AuthEntryPointJwt - Error de autenticación: 
Full authentication is required to access this resource
```

## 🎯 Causas Posibles

Este error ocurre cuando intentas acceder a un endpoint protegido (como `/api/users/**`) sin un token JWT válido o cuando el token no se está enviando correctamente.

### Causa 1: No Hiciste Login Después del Registro ⚠️

El flujo actual es:
1. Te registras → ✅ Usuario creado
2. Vuelves a la pantalla de login → ⚠️ **NO tienes token todavía**
3. Debes hacer login → ✅ Obtienes token
4. Ahora puedes ver usuarios → ✅ Token enviado

**Solución Rápida**: Después de registrarte, haz login con tus credenciales.

### Causa 2: Token No Se Está Enviando

El token JWT debe estar en el header `Authorization` de cada petición:
```
Authorization: Bearer <tu-token-jwt>
```

### Causa 3: Token Expirado

Los tokens JWT expiran después de 24 horas. Si tu sesión es antigua, el token puede haber expirado.

## 🔍 Cómo Diagnosticar el Problema

### Paso 1: Abre la Consola del Navegador (F12)

Ve a la pestaña "Console" y busca los logs:

```javascript
🚀 REQUEST: GET /api/users
📦 Data: undefined
🔐 Token añadido (primeros 20 chars): eyJhbGciOiJIUzUxMiJ9...
```

**Si ves "🔐 Token añadido"** → El token se está enviando ✅

**Si NO ves "🔐 Token añadido"** → El token NO se está enviando ❌

### Paso 2: Ve a la Pestaña "Network"

1. Abre "Network" en las DevTools (F12)
2. Intenta cargar la lista de usuarios
3. Haz clic en la petición `users`
4. Ve a la pestaña "Headers"
5. Busca en "Request Headers":
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

**Si NO ves este header** → El problema está en el frontend

### Paso 3: Revisa el Terminal del Backend

En la terminal donde corre el backend, busca:

```
========================================
🔍 JWT FILTER - Procesando petición
Method: GET /api/users
⚠️ No se encontró token JWT en el header Authorization
```

**Si ves este mensaje** → El token NO llegó al backend

```
========================================
🔍 JWT FILTER - Procesando petición
Method: GET /api/users
✅ Token JWT encontrado (primeros 20 chars): eyJhbGciOiJIUzUxMiJ9...
✅ Token JWT válido
👤 Username extraído del token: testuser
✅ Autenticación establecida en SecurityContext
```

**Si ves esto** → El token es válido y se procesó correctamente ✅

## ✅ Soluciones

### Solución 1: Hacer Login (La Más Común)

**El problema**: Te registraste pero no hiciste login.

**La solución**:
1. Después de registrarte, vuelves a la pantalla de login
2. Ingresa tus credenciales (username y password que acabas de crear)
3. Haz clic en "Iniciar Sesión"
4. Ahora deberías poder ver y crear usuarios

### Solución 2: Verificar que el Token se Guardó

Abre la consola del navegador y ejecuta:

```javascript
console.log('Token:', localStorage.getItem('token'));
console.log('User:', localStorage.getItem('user'));
```

**Si ves `null`** → No has hecho login

**Si ves el token** → El token está guardado ✅

### Solución 3: Limpiar y Volver a Hacer Login

Si el token está corrupto o expirado:

```javascript
// En la consola del navegador
localStorage.clear();
// Luego recarga la página y vuelve a hacer login
```

### Solución 4: Verificar la Configuración de CORS

Si el header Authorization no se está enviando, puede ser un problema de CORS.

Verifica en `backend/src/main/java/com/example/demo/config/CorsConfig.java`:

```java
configuration.setExposedHeaders(Arrays.asList("Authorization"));
configuration.setAllowedHeaders(Arrays.asList("*")); // Debe permitir todos los headers
```

## 🧪 Test Manual Completo

### Test 1: Registro

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "username": "testuser2",
    "email": "test2@example.com",
    "password": "test123456"
  }'
```

**Resultado esperado:**
```json
{
  "message": "Usuario registrado exitosamente",
  "username": "testuser2",
  "email": "test2@example.com",
  "id": 2
}
```

### Test 2: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "password": "test123456"
  }'
```

**Resultado esperado:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjIi...",
  "type": "Bearer",
  "id": 2,
  "username": "testuser2",
  "email": "test2@example.com",
  "name": "Test User"
}
```

**Copia el token** que se devuelve.

### Test 3: Listar Usuarios (Con Token)

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

Reemplaza `TU_TOKEN_AQUI` con el token que copiaste.

**Resultado esperado:**
```json
[
  {
    "id": 1,
    "name": "Test User",
    "username": "testuser",
    "email": "test@example.com",
    "phone": null
  },
  {
    "id": 2,
    "name": "Test User",
    "username": "testuser2",
    "email": "test2@example.com",
    "phone": null
  }
]
```

### Test 4: Listar Usuarios (Sin Token) - Debería Fallar

```bash
curl -X GET http://localhost:8080/api/users
```

**Resultado esperado (ERROR 401):**
```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

Esto es correcto, significa que la seguridad está funcionando.

## 🔧 Mejora Sugerida: Login Automático Después del Registro

Para evitar este problema en el futuro, podemos modificar el flujo para que después del registro se haga login automáticamente.

### Opción A: Modificar el Componente Signup

En `frontend/src/components/Signup.jsx`, después de un registro exitoso:

```javascript
const handleSubmit = async (e) => {
  e.preventDefault();
  setError('');
  setLoading(true);

  try {
    // 1. Registrar usuario
    await signup(formData);
    
    // 2. Hacer login automáticamente
    await login(formData.username, formData.password);
    
    // 3. Usuario ya autenticado, ir al dashboard
    onSuccess();
  } catch (err) {
    setError(err.response?.data?.error || 'Error al registrarse');
  } finally {
    setLoading(false);
  }
};
```

### Opción B: Modificar el Backend para Devolver Token en el Registro

En `AuthController.java`, modifica el método `registerUser`:

```java
@PostMapping("/signup")
public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    try {
        // Registrar usuario
        User user = authService.registerUser(signUpRequest);
        
        // Generar token automáticamente
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signUpRequest.getUsername(), 
                        signUpRequest.getPassword()
                )
        );
        String jwt = jwtUtils.generateJwtToken(authentication.getName());
        
        // Devolver token con los datos del usuario
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new JwtResponse(jwt, user.getId(), user.getUsername(), 
                               user.getEmail(), user.getName())
        );
    } catch (RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

## 📋 Checklist de Solución

Cuando tengas el error de autenticación:

- [ ] ¿Hiciste login después de registrarte?
- [ ] ¿Puedes ver el token en localStorage? (Consola: `localStorage.getItem('token')`)
- [ ] ¿El token se está enviando en el header Authorization? (Network tab)
- [ ] ¿El token llegó al backend? (Logs del backend)
- [ ] ¿El token es válido? (Backend debe decir "✅ Token JWT válido")
- [ ] ¿Tu sesión expiró? (Los tokens duran 24 horas)

## 🎯 Flujo Correcto Actual

```
1. REGISTRO
   Usuario → Frontend → POST /api/auth/signup → Backend
   ✅ Usuario creado (pero SIN token)

2. VOLVER A LOGIN
   ⚠️ Aquí es donde debes hacer login

3. LOGIN
   Usuario → Frontend → POST /api/auth/login → Backend
   ✅ Token JWT generado y guardado

4. ACCEDER A USUARIOS
   Usuario → Frontend → GET /api/users (con token) → Backend
   ✅ Lista de usuarios devuelta
```

## 💡 Tips Adicionales

### Ver el Token en el Navegador

1. Presiona F12
2. Ve a "Application" (Chrome) o "Storage" (Firefox)
3. Expande "Local Storage"
4. Haz clic en `http://localhost:3000`
5. Verás `token` y `user`

### Ver Cuándo Expira tu Token

El token JWT tiene la fecha de expiración codificada. Para verla:

```javascript
// En la consola del navegador
const token = localStorage.getItem('token');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Expira:', new Date(payload.exp * 1000));
```

### Renovar Token Expirado

Si tu token expiró, simplemente:
1. Haz logout (botón en la navbar)
2. Vuelve a hacer login
3. Obtendrás un token nuevo

## 🚨 Errores Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| "Full authentication is required" | No hay token o token inválido | Hacer login |
| "Token JWT expirado" | Token con más de 24 horas | Volver a hacer login |
| "Token JWT inválido" | Token corrupto | Limpiar localStorage y login |
| Header Authorization no aparece | Problema de CORS | Verificar CorsConfig |

---

**Resumen**: El problema más común es que **necesitas hacer login después de registrarte** para obtener el token JWT que te permite acceder a los endpoints protegidos como `/api/users`.

¡Prueba hacer login y debería funcionar! 🎉
