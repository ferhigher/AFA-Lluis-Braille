# 🔍 Guía Rápida de Logs

## ⚡ Ver Logs en 3 Pasos

### 1️⃣ Backend - Terminal

Cuando inicies el backend con `mvn spring-boot:run`, verás logs automáticamente:

```
========================================
POST /api/auth/signup - INICIO
========================================
Datos recibidos:
  Name: Juan Pérez
  Username: juanperez
  Email: juan@example.com
```

✅ **Logs claros con separadores visuales**
✅ **Toda la información importante**
✅ **Errores con stack traces completos**

### 2️⃣ Frontend - Consola del Navegador

1. Presiona `F12` en tu navegador
2. Ve a la pestaña "Console"
3. Verás logs con emojis:

```javascript
========================================
📝 SIGNUP FORM - SUBMIT
========================================
📋 Datos del formulario: {...}
✅ Validación frontend exitosa
🚀 Enviando datos al backend...
```

### 3️⃣ Network - Pestaña Network

En la misma ventana de DevTools (F12):
1. Ve a la pestaña "Network"
2. Intenta registrarte
3. Haz clic en la petición `signup`
4. Ve a:
   - **Headers**: Para ver la URL y método
   - **Payload**: Para ver los datos enviados
   - **Response**: Para ver la respuesta del servidor

## 🎯 Ejemplo Completo: Debugging de Registro

### Escenario: Error al registrarse

**Usuario intenta registrarse con username "testuser"**

#### 📱 Frontend (Consola)
```javascript
========================================
📝 SIGNUP FORM - SUBMIT
========================================
📋 Datos del formulario: {
  name: "Test User",
  username: "testuser",
  email: "test@example.com",
  passwordLength: 8
}
✅ Validación frontend exitosa
🚀 Enviando datos al backend...

🚀 REQUEST: POST /api/auth/signup
📦 Data: {name: "Test User", username: "testuser", ...}

❌ ERROR EN RESPONSE
📊 Status: 400
📦 Response Data: {error: "El username ya está en uso"}
💬 Mensaje de error: El username ya está en uso
```

#### 🖥️ Backend (Terminal)
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
Verificando si el username 'testuser' ya existe...
USERNAME YA EXISTE: testuser

========================================
ERROR EN REGISTRO (RuntimeException)
Mensaje: Error: El username ya está en uso
========================================
```

#### 💡 Diagnóstico
**Problema identificado:** El username "testuser" ya existe en la base de datos

**Solución:** Usa otro username como "testuser2"

## 🔥 Logs Importantes a Buscar

### ✅ Registro Exitoso

**Frontend:**
```javascript
✅ RESPONSE: POST /api/auth/signup
📊 Status: 201
✅ Signup exitoso
🎉 Llamando a onSuccess()
```

**Backend:**
```
=== USUARIO REGISTRADO EXITOSAMENTE ===
ID generado: 1
Username: testuser
Email: test@example.com
```

### ❌ Errores Comunes

#### Error 1: Username Duplicado
```
USERNAME YA EXISTE: testuser
```
**Solución:** Usa otro username

#### Error 2: Email Duplicado
```
EMAIL YA EXISTE: test@example.com
```
**Solución:** Usa otro email

#### Error 3: Validación Fallida
```
ERRORES DE VALIDACIÓN DETECTADOS:
  Campo 'password': La contraseña debe tener al menos 6 caracteres
```
**Solución:** Password más largo

#### Error 4: Backend No Responde
```javascript
❌ ERROR EN RESPONSE
📡 No se recibió respuesta del servidor
💡 Posibles causas:
   - El servidor no está corriendo
```
**Solución:** Inicia el backend con `mvn spring-boot:run`

## 🎨 Significado de los Emojis

| Emoji | Significado |
|-------|-------------|
| 🚀 | Petición HTTP iniciada |
| ✅ | Operación exitosa |
| ❌ | Error ocurrido |
| 📦 | Datos/Payload |
| 📊 | Status/Estadísticas |
| 📍 | URL/Ubicación |
| 🔐 | Token/Autenticación |
| 📝 | Formulario/Input |
| 💬 | Mensaje |
| 🔍 | Debugging/Información detallada |
| ⚠️ | Advertencia |
| 🎉 | Éxito total |

## 📋 Checklist de Debugging

Cuando algo falle, verifica en orden:

1. [ ] ¿Abrí la consola del navegador? (F12)
2. [ ] ¿Qué dice el error en la UI?
3. [ ] ¿Qué dice el log en la consola?
4. [ ] ¿Qué dice el log en la terminal del backend?
5. [ ] ¿La petición llegó al backend? (busca "POST /api/auth/...")
6. [ ] ¿Hay errores de validación?
7. [ ] ¿Hay errores de SQL?

## 💡 Pro Tips

### Tip 1: Buscar Rápido
En la consola del navegador, usa `Ctrl+F` para buscar:
- `❌` para encontrar errores
- `POST /api/auth/signup` para encontrar tu petición
- `Status: 400` para encontrar errores específicos

### Tip 2: Copiar Logs
Para copiar un log completo:
1. Click derecho en el log
2. "Copy message"
3. Pégalo en un archivo de texto

### Tip 3: Limpiar la Consola
Si hay muchos logs:
- Consola: Click en el icono 🚫 (Clear console)
- O presiona `Ctrl+L`

### Tip 4: Filtrar Logs
En la consola del navegador:
- Usa el campo de búsqueda arriba
- Filtra por nivel: Errors, Warnings, Info

## 🧪 Test Rápido del Sistema de Logs

### Test 1: Verificar Backend Logs
```bash
cd backend
mvn spring-boot:run
```
Deberías ver:
```
Started DemoApplication in X.XXX seconds
```

### Test 2: Verificar Frontend Logs
1. Abre `http://localhost:3000`
2. Abre consola (F12)
3. Haz clic en "Regístrate aquí"
4. Deberías ver:
```javascript
📝 Campo 'name' cambiado: ...
```

### Test 3: Test Completo de Registro
1. Llena el formulario de registro
2. Haz clic en "Registrarse"
3. En la **consola** deberías ver:
   ```javascript
   ========================================
   📝 SIGNUP FORM - SUBMIT
   ...
   ```
4. En la **terminal** deberías ver:
   ```
   ========================================
   POST /api/auth/signup - INICIO
   ...
   ```

## 🆘 ¿Sigues Teniendo Problemas?

Si después de revisar los logs no encuentras el problema:

1. **Copia todos los logs** desde el inicio hasta el error
2. **Copia el stack trace** de la terminal
3. **Toma una captura** de la consola del navegador
4. Revisa el archivo **DEBUGGING.md** para problemas más específicos

---

**Recuerda:** Los logs son tu mejor amigo para encontrar bugs. ¡Úsalos! 🔍
