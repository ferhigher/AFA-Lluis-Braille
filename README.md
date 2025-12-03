# Aplicación Fullstack - Spring Boot + React

Aplicación web completa con backend en Spring Boot y frontend en React, que incluye autenticación JWT y visualización de noticias de Telegram.

## 📋 Características

### Backend (Spring Boot)
- ✅ API REST completa con endpoints CRUD
- ✅ **Autenticación JWT con Spring Security**
- ✅ **Sistema de registro e inicio de sesión**
- ✅ Arquitectura en capas (Controller, Service, Repository)
- ✅ JPA/Hibernate para persistencia
- ✅ Base de datos H2 (en memoria)
- ✅ Validación de datos
- ✅ Configuración CORS
- ✅ Manejo de errores
- ✅ **Integración con Telegram API**

### Frontend (React)
- ✅ Componentes funcionales con Hooks
- ✅ **Sistema de autenticación completo (Login/Signup)**
- ✅ **Context API para gestión de estado de autenticación**
- ✅ Gestión de estado con useState/useEffect
- ✅ Axios con interceptores para JWT
- ✅ **Navegación entre secciones**
- ✅ **Sección de noticias de Telegram**
- ✅ Formularios controlados
- ✅ Diseño responsivo
- ✅ Manejo de errores y loading
- ✅ LocalStorage para persistencia de sesión

## 🚀 Estructura del Proyecto

```
fullstack-app/
├── backend/                    # Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/demo/
│   │   │   │   ├── controller/     # Controladores REST
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   └── TelegramController.java
│   │   │   │   ├── service/        # Lógica de negocio
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── TelegramService.java
│   │   │   │   ├── repository/     # Acceso a datos
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── TelegramMessageRepository.java
│   │   │   │   ├── model/          # Entidades
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── TelegramMessage.java
│   │   │   │   ├── security/       # Seguridad JWT
│   │   │   │   │   ├── JwtUtils.java
│   │   │   │   │   ├── AuthTokenFilter.java
│   │   │   │   │   ├── AuthEntryPointJwt.java
│   │   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │   ├── dto/            # DTOs
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── SignupRequest.java
│   │   │   │   │   └── JwtResponse.java
│   │   │   │   ├── config/         # Configuración
│   │   │   │   │   ├── CorsConfig.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   └── DemoApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
└── frontend/                   # React
    ├── src/
    │   ├── components/         # Componentes React
    │   │   ├── Login.jsx
    │   │   ├── Signup.jsx
    │   │   ├── Navigation.jsx
    │   │   ├── UserList.jsx
    │   │   ├── UserForm.jsx
    │   │   └── News.jsx
    │   ├── context/            # Context API
    │   │   └── AuthContext.jsx
    │   ├── services/           # Servicios API
    │   │   └── api.js
    │   ├── styles/             # Estilos CSS
    │   │   ├── index.css
    │   │   ├── App.css
    │   │   ├── Auth.css
    │   │   ├── Navigation.css
    │   │   └── News.css
    │   ├── App.jsx             # Componente principal
    │   └── main.jsx            # Punto de entrada
    ├── public/
    ├── index.html
    ├── package.json
    └── vite.config.js
```

## 🛠️ Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+**
- **Node.js 18+** y npm
- Un IDE (IntelliJ IDEA, VS Code, etc.)

## 📦 Instalación y Configuración

### Backend (Spring Boot)

1. Navega a la carpeta del backend:
```bash
cd backend
```

2. Instala las dependencias y compila:
```bash
mvn clean install
```

3. Ejecuta la aplicación:
```bash
mvn spring-boot:run
```

El backend estará disponible en: `http://localhost:8080`

### Frontend (React)

1. Navega a la carpeta del frontend:
```bash
cd frontend
```

2. Instala las dependencias:
```bash
npm install
```

3. Ejecuta el servidor de desarrollo:
```bash
npm run dev
```

El frontend estará disponible en: `http://localhost:3000`

## 🔌 Endpoints de la API

### Autenticación (Público)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/auth/signup` | Registrar nuevo usuario |

### Usuarios (Requiere autenticación)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/users` | Obtener todos los usuarios |
| GET | `/api/users/{id}` | Obtener un usuario por ID |
| POST | `/api/users` | Crear un nuevo usuario |
| PUT | `/api/users/{id}` | Actualizar un usuario |
| DELETE | `/api/users/{id}` | Eliminar un usuario |

### Telegram (Público)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/telegram/messages` | Obtener mensajes del canal |
| POST | `/api/telegram/fetch` | Actualizar mensajes desde Telegram |
| POST | `/api/telegram/manual` | Crear mensaje manual (pruebas) |

### Ejemplo de Peticiones

**Registro (POST /api/auth/signup):**
```json
{
  "name": "Juan Pérez",
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "password123",
  "phone": "+34 600 123 456"
}
```

**Login (POST /api/auth/login):**
```json
{
  "username": "juanperez",
  "password": "password123"
}
```

**Respuesta de Login:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "juanperez",
  "email": "juan@example.com",
  "name": "Juan Pérez"
}
```

**Crear usuario (POST /api/users) - Con token:**
```bash
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```
```json
{
  "name": "María García",
  "username": "mariagarcia",
  "email": "maria@example.com",
  "password": "password456",
  "phone": "+34 600 654 321"
}
```

## 🗄️ Base de Datos

La aplicación usa **H2 Database** (base de datos en memoria) para desarrollo.

### Acceso a la Consola H2

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Usuario: `sa`
- Password: (dejar en blanco)

## 📱 Configuración de Telegram

Para conectar con el canal de Telegram "AFA Lluís Braille":

1. **Crear un Bot de Telegram:**
   - Habla con [@BotFather](https://t.me/botfather) en Telegram
   - Usa el comando `/newbot` y sigue las instrucciones
   - Guarda el token que te proporciona

2. **Agregar el Bot al Canal:**
   - Añade tu bot como administrador del canal @afa_lluis_braille
   - Asegúrate de que el bot tenga permisos para leer mensajes

3. **Configurar el Backend:**
   - Edita `backend/src/main/resources/application.properties`
   - Reemplaza `YOUR_BOT_TOKEN_HERE` con tu token:
   ```properties
   telegram.bot.token=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
   telegram.channel.username=@afa_lluis_braille
   ```

4. **Modo de Prueba (Sin Bot):**
   - Si no tienes acceso al bot, puedes usar la función "Crear Noticia Manual"
   - Esto te permite probar la funcionalidad sin conectar con Telegram

### Actualización Automática de Noticias

El backend incluye un endpoint para actualizar manualmente las noticias desde Telegram. En un entorno de producción, podrías configurar un scheduler para actualizarlas automáticamente cada cierto tiempo.

## 🎨 Uso de la Aplicación

1. **Primera vez:**
   - Abre tu navegador en `http://localhost:3000`
   - Haz clic en "Regístrate aquí"
   - Completa el formulario de registro
   - Inicia sesión con tus credenciales

2. **Gestión de Usuarios:**
   - Navega a la sección "Usuarios"
   - Crea, edita o elimina usuarios
   - Los cambios se guardan en tiempo real

3. **Ver Noticias:**
   - Navega a la sección "Noticias"
   - Si has configurado el bot de Telegram, haz clic en "Actualizar Noticias"
   - También puedes crear noticias manuales para pruebas

4. **Cerrar Sesión:**
   - Haz clic en "Cerrar Sesión" en la barra de navegación
   - Tu sesión se mantendrá guardada hasta que cierres sesión

## 🔒 Seguridad

- Las contraseñas se encriptan con BCrypt
- Los tokens JWT expiran después de 24 horas
- Los endpoints protegidos requieren autenticación
- CORS configurado para desarrollo local
- Interceptores de Axios para manejo automático de tokens

## 🔧 Personalización

### Cambiar el puerto del backend

Edita `backend/src/main/resources/application.properties`:
```properties
server.port=8080
```

### Cambiar el puerto del frontend

Edita `frontend/vite.config.js`:
```javascript
server: {
  port: 3000
}
```

### Usar otra base de datos

Para usar MySQL o PostgreSQL, actualiza las dependencias en `pom.xml` y la configuración en `application.properties`.

## 📚 Tecnologías Utilizadas

### Backend
- Spring Boot 3.2.0
- Spring Security (JWT)
- Spring Data JPA
- H2 Database
- Lombok
- JJWT (JSON Web Tokens)
- Telegram Bots API
- WebFlux (HTTP Client)
- Maven

### Frontend
- React 18
- Vite
- Axios (con interceptores)
- Context API
- React Hooks
- CSS3
- LocalStorage API

## 🚀 Próximos Pasos

Algunas ideas para expandir la aplicación:

- [x] Autenticación y autorización (Spring Security + JWT) ✅
- [x] Integración con Telegram para noticias ✅
- [ ] Implementar roles de usuario (admin, user)
- [ ] Añadir refresh tokens
- [ ] Implementar paginación y filtrado
- [ ] Añadir validaciones más complejas
- [ ] Usar una base de datos persistente (MySQL/PostgreSQL)
- [ ] Añadir testing (JUnit, Jest, React Testing Library)
- [ ] Implementar búsqueda de usuarios
- [ ] Agregar más entidades (productos, pedidos, etc.)
- [ ] Añadir subida de archivos/imágenes de perfil
- [ ] Implementar notificaciones en tiempo real
- [ ] Crear panel de administración
- [ ] Añadir recuperación de contraseña
- [ ] Implementar modo oscuro
- [ ] Configurar actualización automática de noticias con scheduler
- [ ] Añadir integración con más canales de Telegram
- [ ] Implementar cache para mejorar rendimiento

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la Licencia MIT.

## 👤 Autor

Tu nombre aquí

---

¿Necesitas ayuda? Abre un issue o contacta al equipo de desarrollo.
