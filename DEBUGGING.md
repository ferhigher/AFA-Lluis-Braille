# 🔍 Guía de Debugging y Logs

Esta guía te ayudará a diagnosticar problemas en el registro de usuarios y otras funcionalidades.

## 📊 Sistema de Logs Mejorado

### ✅ Lo que hemos añadido:

#### Backend (Logs en Terminal)
- ✅ Logging detallado en cada capa (Controller → Service → Repository)
- ✅ Logs con formato claro y separadores visuales
- ✅ Información de validación de datos
- ✅ Stack traces completos en caso de error
- ✅ Logs de SQL con parámetros
- ✅ Timestamps en cada log

#### Frontend (Logs en Consola del Navegador)
- ✅ Logs en cada petición HTTP (request/response)
- ✅ Información detallada de errores
- ✅ Validaciones en tiempo real
- ✅ Estado de los datos antes de enviar
- ✅ Mensajes con emojis para fácil identificación

## 🚀 Cómo Ver los Logs

### Backend - Terminal

1. **Inicia el backend** con:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Los logs aparecerán automáticamente** en la terminal con este formato:
   ```
   ========================================
   POST /api/auth/signup - INICIO
   ========================================
   Datos recibidos en el controlador:
     Name: Juan Pérez
     Username: juanperez
     Email: juan@example.com
     Phone: +34600123456
     Password presente: true
   ```

### Frontend - Consola del Navegador

1. **Abre la consola del navegador:**
   - Chrome/Edge: `F12` o `Ctrl+Shift+I`
   - Firefox: `F12` o `Ctrl+Shift+K`
   - Safari: `Cmd+Option+I`

2. **Ve a la pestaña "Console"**

3. **Los logs aparecerán** con este formato:
   ```
   ========================================
   📝 SIGNUP FORM - SUBMIT
   ========================================
   📋 Datos del formulario: {name: "Juan", username: "juan", ...}
   ✅ Validación frontend exitosa
   🚀 Enviando datos al backend...
   ```

## 🔍 Debugging del Registro de Usuarios

### Paso 1: Verificar que el Backend está corriendo

**En la terminal del backend**, deberías ver:
```
Started DemoApplication in X.XXX seconds
```

Si no ves esto, el backend no está iniciado correctamente.

### Paso 2: Abrir la Consola del Navegador

1. Abre tu navegador en `http://localhost:3000`
2. Presiona `F12`
3. Ve a la pestaña "Console"
4. Ve a la pestaña "Network" también

### Paso 3: Intentar Registrarse

1. Haz clic en "Regístrate aquí"
2. **Llena el formulario** con estos datos de prueba:
   ```
   Nombre: Test User
   Usuario: testuser
   Email: test@example.com
   Contraseña: test123456
   Teléfono: +34600000001
   ```

3. Haz clic en "Registrarse"

### Paso 4: Observar los Logs

#### En la CONSOLA del Navegador verás:

✅ **Si todo va bien:**
```javascript
========================================
📝 SIGNUP FORM - SUBMIT
========================================
📋 Datos del formulario: {...}
✅ Validación frontend exitosa
🚀 Enviando datos al backend...

🚀 REQUEST: POST /api/auth/signup
📦 Data: {name: "Test User", username: "testuser", ...}

✅ RESPONSE: POST /api/auth/signup
📊 Status: 201
📦 Data: {message: "Usuario registrado exitosamente", ...}

✅ Signup exitoso
```

❌ **Si hay error:**
```javascript
========================================
❌ ERROR EN SIGNUP
========================================
🔍 Error completo: Error {...}
📊 Response data: {error: "El username ya está en uso"}
💬 Mensaje de error: El username ya está en uso
```

#### En la TERMINAL del Backend verás:

✅ **Si todo va bien:**
```
========================================
POST /api/auth/signup - INICIO
========================================
Datos recibidos en el controlador:
  Name: Test User
  Username: testuser
  Email: test@example.com
  
Validación inicial exitosa, procesando registro...

=== INICIO REGISTRO ===
Datos recibidos:
  - Nombre: Test User
  - Username: testuser
  - Email: test@example.com
  
Verificando si el username 'testuser' ya existe...
Username disponible: testuser

Verificando si el email 'test@example.com' ya existe...
Email disponible: test@example.com

Creando nuevo usuario...
Encriptando contraseña...
Guardando usuario en la base de datos...

=== USUARIO REGISTRADO EXITOSAMENTE ===
ID generado: 1
Username: testuser
Email: test@example.com
========================================
REGISTRO COMPLETADO EXITOSAMENTE
========================================
```

❌ **Si hay error:**
```
========================================
ERROR EN REGISTRO (RuntimeException)
Tipo: RuntimeException
Mensaje: Error: El username ya está en uso
========================================
```

## 🐛 Problemas Comunes y Soluciones

### Problema 1: "El username ya está en uso"

**Síntomas:**
- Error al registrarse con un username que ya usaste

**Logs esperados:**
```
USERNAME YA EXISTE: testuser
```

**Solución:**
- Usa un username diferente, o
- Reinicia el backend para limpiar la base de datos H2

### Problema 2: "El email ya está en uso"

**Síntomas:**
- Error al registrarse con un email que ya usaste

**Logs esperados:**
```
EMAIL YA EXISTE: test@example.com
```

**Solución:**
- Usa un email diferente, o
- Reinicia el backend

### Problema 3: Error 500 - Internal Server Error

**Síntomas:**
- Error genérico del servidor

**Logs esperados en backend:**
```
ERROR INESPERADO EN REGISTRO
Tipo: NullPointerException (o similar)
Stack trace: ...
```

**Solución:**
- Revisa el stack trace completo en la terminal
- Verifica que todos los campos obligatorios estén llenos
- Comprueba la configuración de la base de datos

### Problema 4: No llega la petición al backend

**Síntomas:**
- La petición nunca llega al backend
- En la consola del navegador ves error de red

**Logs esperados en navegador:**
```
❌ ERROR EN RESPONSE
📡 No se recibió respuesta del servidor
💡 Posibles causas:
   - El servidor no está corriendo
   - Problemas de red
   - CORS no configurado correctamente
```

**Solución:**
1. Verifica que el backend esté corriendo:
   ```bash
   curl http://localhost:8080/h2-console
   ```
2. Verifica la configuración de CORS en `CorsConfig.java`
3. Revisa que el puerto 8080 no esté ocupado

### Problema 5: Error de validación

**Síntomas:**
- Campos no válidos
- Mensaje de error específico

**Logs esperados:**
```
ERRORES DE VALIDACIÓN DETECTADOS:
  Campo 'username': El username debe tener entre 4 y 20 caracteres
  Campo 'password': La contraseña debe tener al menos 6 caracteres
```

**Solución:**
- Revisa que los datos cumplan con los requisitos:
  - Nombre: no vacío
  - Username: 4-20 caracteres
  - Email: formato válido
  - Password: mínimo 6 caracteres

## 📝 Verificar Datos en la Base de Datos

### Opción 1: H2 Console

1. Abre `http://localhost:8080/h2-console`
2. Usa estas credenciales:
   - JDBC URL: `jdbc:h2:mem:testdb`
   - Usuario: `sa`
   - Password: (vacío)
3. Ejecuta esta consulta:
   ```sql
   SELECT * FROM users;
   ```

### Opción 2: Logs de SQL

En la terminal del backend verás las consultas SQL:
```sql
Hibernate: 
    select
        u1_0.id,
        u1_0.created_at,
        u1_0.email,
        u1_0.name,
        u1_0.password,
        u1_0.phone,
        u1_0.username 
    from
        users u1_0 
    where
        u1_0.username=?
```

## 🎯 Checklist de Debugging

Cuando tengas un problema, sigue este checklist:

- [ ] ¿Está el backend corriendo? (verifica la terminal)
- [ ] ¿Está el frontend corriendo? (verifica que `http://localhost:3000` carga)
- [ ] ¿Has abierto la consola del navegador? (F12)
- [ ] ¿Has mirado la pestaña "Console"?
- [ ] ¿Has mirado la pestaña "Network"?
- [ ] ¿Qué dice el mensaje de error en la UI?
- [ ] ¿Qué logs aparecen en la consola del navegador?
- [ ] ¿Qué logs aparecen en la terminal del backend?
- [ ] ¿Aparece algún error de SQL?
- [ ] ¿Los datos están llegando al backend?

## 🔧 Comandos Útiles para Testing

### Test de Registro con cURL

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "username": "testuser2",
    "email": "test2@example.com",
    "password": "test123456",
    "phone": "+34600000002"
  }' \
  -v
```

La opción `-v` (verbose) te mostrará todos los detalles de la petición y respuesta.

### Verificar que el Backend Responde

```bash
curl http://localhost:8080/h2-console
```

Debería devolver HTML de la consola H2.

### Ver Usuarios Registrados

```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

## 📊 Niveles de Logging

El sistema de logs tiene diferentes niveles configurados:

| Nivel | Qué muestra |
|-------|-------------|
| **TRACE** | TODO - Parámetros de SQL, etc. |
| **DEBUG** | Información detallada de debugging |
| **INFO** | Información general de operaciones |
| **WARN** | Advertencias (username duplicado, etc.) |
| **ERROR** | Errores que necesitan atención |

Puedes cambiar los niveles en `application.properties`:
```properties
logging.level.com.example.demo=DEBUG  # Cambiar a INFO, WARN, ERROR
```

## 💡 Tips para Debugging Efectivo

1. **Siempre abre la consola ANTES de reproducir el error**
2. **Lee los logs de arriba hacia abajo**
3. **Los emojis te ayudan a identificar el tipo de log:**
   - 🚀 = Peticiones
   - ✅ = Éxito
   - ❌ = Error
   - 📦 = Datos
   - 🔍 = Información de debugging
   - ⚠️ = Advertencias

4. **Busca los separadores `========`** para encontrar el inicio de cada operación

5. **Si algo falla en el backend**, el error aparecerá primero en la consola del navegador y luego en la terminal del backend

6. **Copia los logs completos** si necesitas pedir ayuda

## 🚨 Mensajes de Error Importantes

### Frontend

| Mensaje | Causa | Solución |
|---------|-------|----------|
| `Network Error` | Backend no está corriendo | Inicia el backend |
| `401 Unauthorized` | Token inválido/expirado | Vuelve a hacer login |
| `400 Bad Request` | Datos inválidos | Revisa los datos del formulario |
| `500 Internal Server Error` | Error en el servidor | Revisa logs del backend |

### Backend

| Mensaje | Causa | Solución |
|---------|-------|----------|
| `El username ya está en uso` | Usuario duplicado | Usa otro username |
| `El email ya está en uso` | Email duplicado | Usa otro email |
| `Usuario no encontrado` | ID no existe | Verifica el ID |
| `DataIntegrityViolationException` | Violación de restricción de BD | Verifica datos únicos |

## 📞 Ayuda Adicional

Si después de revisar todos los logs sigues teniendo problemas:

1. **Copia TODO el log** desde el inicio del registro hasta el error
2. **Toma una captura** de la consola del navegador
3. **Copia el stack trace** de la terminal del backend
4. Revisa la documentación en los archivos .md del proyecto

---

¡Con este sistema de logs podrás diagnosticar cualquier problema fácilmente! 🎉
