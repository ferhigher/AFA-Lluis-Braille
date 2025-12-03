# 🚀 Guía de Inicio Rápido

Esta guía te ayudará a tener la aplicación funcionando en **menos de 5 minutos**.

## ⚡ Inicio Rápido (Sin configurar Telegram)

### 1️⃣ Iniciar el Backend

```bash
cd backend
mvn spring-boot:run
```

✅ El backend estará disponible en: `http://localhost:8080`

### 2️⃣ Iniciar el Frontend

**En otra terminal:**

```bash
cd frontend
npm install
npm run dev
```

✅ El frontend estará disponible en: `http://localhost:3000`

### 3️⃣ Usar la Aplicación

1. **Abre tu navegador** en `http://localhost:3000`
2. **Regístrate:**
   - Haz clic en "Regístrate aquí"
   - Completa el formulario
   - Ejemplo:
     - Nombre: Test User
     - Usuario: testuser
     - Email: test@example.com
     - Contraseña: test123
3. **Inicia sesión** con tus credenciales
4. **¡Listo!** Ya puedes usar la aplicación

## 📱 Funcionalidades Disponibles

### Sin configurar Telegram

✅ **Gestión de Usuarios**
- Ver lista de usuarios
- Crear nuevos usuarios
- Editar usuarios existentes
- Eliminar usuarios

✅ **Noticias (Modo Prueba)**
- Crear noticias manuales
- Ver todas las noticias
- Noticias ordenadas por fecha

### Con Telegram configurado

✅ **Todo lo anterior +**
- Sincronizar mensajes del canal @afa_lluis_braille
- Actualización automática de noticias
- Ver mensajes reales del canal

Ver `TELEGRAM_SETUP.md` para configurar Telegram.

## 🎯 Primeros Pasos Recomendados

### 1. Crear tu primer usuario
1. Navega a "Usuarios"
2. Clic en "+ Nuevo Usuario"
3. Completa el formulario
4. Guarda

### 2. Probar las noticias
1. Navega a "Noticias"
2. Encuentra el formulario "Crear Noticia Manual"
3. Escribe: "¡Bienvenido a la aplicación!"
4. Clic en "Crear Noticia"
5. Verás tu noticia en la lista

### 3. Explorar la base de datos
1. Abre `http://localhost:8080/h2-console`
2. Usa estas credenciales:
   - JDBC URL: `jdbc:h2:mem:testdb`
   - Usuario: `sa`
   - Password: (dejar vacío)
3. Ejecuta consultas SQL para ver los datos

## 🔧 Configuración Opcional

### Telegram Bot

Si quieres conectar con el canal real:

1. **Crea un bot** con @BotFather en Telegram
2. **Edita** `backend/src/main/resources/application.properties`:
   ```properties
   telegram.bot.token=TU_TOKEN_AQUI
   ```
3. **Reinicia** el backend
4. **En la app**, ve a "Noticias" → "Actualizar Noticias"

Ver **TELEGRAM_SETUP.md** para instrucciones completas.

### Cambiar Puertos

**Backend (puerto 8080):**

Edita `backend/src/main/resources/application.properties`:
```properties
server.port=8090
```

**Frontend (puerto 3000):**

Edita `frontend/vite.config.js`:
```javascript
server: {
  port: 3001
}
```

## 📊 Verificar que Todo Funciona

### ✅ Backend
```bash
curl http://localhost:8080/h2-console
```
Debería redirigir a la consola H2

### ✅ Frontend
Abre `http://localhost:3000` en tu navegador
Deberías ver la pantalla de login

### ✅ API
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","username":"test","email":"test@test.com","password":"123456"}'
```
Debería responder con mensaje de éxito

## ⚠️ Solución Rápida de Problemas

### Puerto 8080 ocupado
```bash
# Linux/Mac
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Puerto 3000 ocupado
```bash
# Linux/Mac
lsof -i :3000
kill -9 <PID>

# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F
```

### Maven no encontrado
```bash
# Instalar Maven
# Ubuntu/Debian
sudo apt install maven

# Mac
brew install maven

# Windows
# Descargar desde https://maven.apache.org
```

### Node no encontrado
```bash
# Instalar Node.js desde https://nodejs.org
# Verifica la instalación
node --version
npm --version
```

## 🎓 Próximos Pasos

Después de tener todo funcionando:

1. 📖 Lee **README.md** para documentación completa
2. 🔐 Revisa **TELEGRAM_SETUP.md** para configurar Telegram
3. 🧪 Consulta **TEST_DATA.md** para datos de prueba
4. 📝 Lee **CHANGELOG.md** para ver todas las funcionalidades

## 💡 Tips Útiles

### Hot Reload
- ✅ El backend se recarga automáticamente con Spring DevTools
- ✅ El frontend se recarga automáticamente con Vite
- No necesitas reiniciar después de hacer cambios

### Logs
- 📊 Backend: Los logs aparecen en la terminal donde ejecutaste `mvn spring-boot:run`
- 📊 Frontend: Los errores aparecen en la consola del navegador (F12)

### Datos de Prueba
- 🗄️ Los datos se guardan en H2 (memoria)
- 🔄 Se borran al reiniciar el backend
- 💾 Perfecto para desarrollo y pruebas

## 📞 ¿Necesitas Ayuda?

1. **Revisa los logs** en las terminales
2. **Consulta la documentación** en los archivos .md
3. **Verifica** que los puertos estén libres
4. **Asegúrate** de tener Java 17+ y Node 18+

## ✨ ¡Disfruta de tu aplicación!

Ya tienes una aplicación fullstack completa con:
- 🔐 Autenticación JWT
- 👥 Gestión de usuarios
- 📰 Sistema de noticias
- 🎨 UI moderna y responsive
- 🔒 Seguridad implementada

¡Hora de explorar y personalizar! 🚀
