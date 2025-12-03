# 🎯 Características de la Aplicación

## 📱 Vista General

Esta aplicación fullstack incluye:

```
┌─────────────────────────────────────────────────────────┐
│                   🌐 FRONTEND (React)                    │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │  Login   │  │  Signup  │  │    Navigation Bar    │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
│                                                          │
│  ┌────────────────────────┐  ┌────────────────────┐    │
│  │   Gestión Usuarios     │  │      Noticias      │    │
│  │  - Listar             │  │  - Ver mensajes    │    │
│  │  - Crear              │  │  - Actualizar      │    │
│  │  - Editar             │  │  - Crear manual    │    │
│  │  - Eliminar           │  │                    │    │
│  └────────────────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                          ↕️
              Comunicación vía REST + JWT
                          ↕️
┌─────────────────────────────────────────────────────────┐
│              ⚙️ BACKEND (Spring Boot)                    │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │           🔐 Spring Security + JWT                  │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │     Auth     │  │    Users     │  │   Telegram   │  │
│  │  Controller  │  │  Controller  │  │  Controller  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         ↓                 ↓                  ↓           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │     Auth     │  │    User      │  │   Telegram   │  │
│  │   Service    │  │   Service    │  │   Service    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         ↓                 ↓                  ↓           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │     User     │  │     User     │  │   Message    │  │
│  │  Repository  │  │  Repository  │  │  Repository  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                          ↓                               │
│              ┌─────────────────────┐                    │
│              │   📊 H2 Database    │                    │
│              │      (Memory)       │                    │
│              └─────────────────────┘                    │
└─────────────────────────────────────────────────────────┘
                          ↕️
                 Opcional: Telegram API
                          ↕️
                  📱 Canal de Telegram
                  @afa_lluis_braille
```

## 🔐 Sistema de Autenticación

### Flujo de Login
```
Usuario ingresa credenciales
        ↓
Frontend envía POST /api/auth/login
        ↓
Backend valida con Spring Security
        ↓
Se genera token JWT (válido 24h)
        ↓
Frontend guarda token en LocalStorage
        ↓
Usuario accede a la aplicación
```

### Flujo de Peticiones Protegidas
```
Usuario hace acción (ej: crear usuario)
        ↓
Frontend añade header: Authorization: Bearer <token>
        ↓
Backend valida token con filtro JWT
        ↓
Si válido: procesa petición
Si inválido: 401 Unauthorized
        ↓
Frontend redirige a login si 401
```

## 👥 Gestión de Usuarios

### Operaciones CRUD

| Operación | Endpoint | Método | Requiere Auth |
|-----------|----------|--------|---------------|
| Listar    | /api/users | GET | ✅ |
| Crear     | /api/users | POST | ✅ |
| Ver uno   | /api/users/{id} | GET | ✅ |
| Actualizar | /api/users/{id} | PUT | ✅ |
| Eliminar  | /api/users/{id} | DELETE | ✅ |

### Campos del Usuario
```javascript
{
  id: Long,           // Generado automáticamente
  name: String,       // Nombre completo
  username: String,   // Usuario único (4-20 chars)
  email: String,      // Email único
  password: String,   // Encriptado con BCrypt
  phone: String,      // Opcional
  createdAt: DateTime // Automático
}
```

## 📰 Sistema de Noticias

### Fuentes de Noticias

1. **Telegram Bot** (Automático)
   - Sincroniza mensajes del canal
   - Actualización manual vía botón
   - Requiere configuración de bot

2. **Creación Manual** (Para pruebas)
   - Formulario en la interfaz
   - No requiere bot de Telegram
   - Útil para desarrollo

### Estructura de Mensaje
```javascript
{
  id: Long,              // Generado
  messageId: Integer,    // ID de Telegram o timestamp
  text: String,          // Contenido del mensaje
  channelUsername: String, // @afa_lluis_braille
  messageDate: DateTime,  // Fecha del mensaje
  createdAt: DateTime    // Fecha de guardado
}
```

## 🎨 Interfaz de Usuario

### Páginas Principales

1. **Login/Signup** (No autenticado)
   - Diseño con gradiente morado/azul
   - Validación de formularios
   - Mensajes de error claros

2. **Dashboard** (Autenticado)
   - Barra de navegación superior
   - Nombre del usuario visible
   - Botón de logout
   - Navegación entre secciones

3. **Gestión de Usuarios**
   - Tabla responsive
   - Botones de acción (editar/eliminar)
   - Formulario modal para crear/editar
   - Estados de carga y error

4. **Noticias**
   - Diseño tipo tarjetas
   - Ordenadas por fecha descendente
   - Botón de actualización
   - Formulario de prueba

## 🔒 Seguridad Implementada

### Backend
- ✅ BCrypt para contraseñas (salt + hash)
- ✅ JWT firmado con HS512
- ✅ Validación de tokens en cada petición
- ✅ Spring Security configurado
- ✅ Endpoints públicos limitados
- ✅ Sesiones stateless
- ✅ CORS restringido

### Frontend
- ✅ Tokens en LocalStorage (no cookies)
- ✅ Interceptores de Axios automáticos
- ✅ Redirección en caso de 401
- ✅ Limpieza de datos al logout
- ✅ Validación de formularios
- ✅ No se exponen contraseñas

## 📊 Base de Datos

### H2 (Desarrollo)
```sql
-- Tabla de usuarios
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    created_at TIMESTAMP
);

-- Tabla de mensajes de Telegram
CREATE TABLE telegram_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id INTEGER UNIQUE,
    text TEXT,
    channel_username VARCHAR(255),
    message_date TIMESTAMP,
    created_at TIMESTAMP
);
```

### Características
- 💾 Base de datos en memoria
- 🔄 Se reinicia al reiniciar backend
- 🎯 Ideal para desarrollo
- 🖥️ Consola web en /h2-console
- 📊 SQL standard

## 🌐 API REST

### Endpoints Públicos (Sin autenticación)
```
POST   /api/auth/login        # Iniciar sesión
POST   /api/auth/signup       # Registrarse
GET    /api/telegram/messages # Ver noticias
POST   /api/telegram/fetch    # Actualizar noticias
POST   /api/telegram/manual   # Crear noticia manual
```

### Endpoints Protegidos (Con JWT)
```
GET    /api/users             # Listar usuarios
POST   /api/users             # Crear usuario
GET    /api/users/{id}        # Ver usuario
PUT    /api/users/{id}        # Actualizar usuario
DELETE /api/users/{id}        # Eliminar usuario
```

## 🎯 Ventajas de esta Arquitectura

### Separación de Responsabilidades
- Frontend: UI/UX y experiencia de usuario
- Backend: Lógica de negocio y datos
- Base de datos: Persistencia

### Escalabilidad
- Frontend y backend independientes
- Fácil de escalar horizontalmente
- Microservicios ready

### Seguridad
- Autenticación stateless con JWT
- No hay sesiones en servidor
- Tokens con expiración

### Mantenibilidad
- Código organizado en capas
- Separación clara de concerns
- Fácil de testear

### Flexibilidad
- Frontend puede cambiar (React → Vue)
- Backend puede cambiar (Spring → Node)
- Base de datos puede cambiar (H2 → MySQL)

## 📈 Casos de Uso

### Caso 1: Usuario nuevo se registra
1. Usuario completa formulario de registro
2. Frontend valida datos
3. Envía POST /api/auth/signup
4. Backend valida y encripta contraseña
5. Guarda usuario en BD
6. Retorna confirmación
7. Usuario puede hacer login

### Caso 2: Usuario ve noticias
1. Usuario hace login
2. Navega a sección "Noticias"
3. Frontend solicita GET /api/telegram/messages
4. Backend consulta BD
5. Retorna lista de noticias
6. Frontend las muestra ordenadas

### Caso 3: Usuario gestiona usuarios
1. Usuario autenticado accede a "Usuarios"
2. Ve lista actual
3. Clic en "Nuevo Usuario"
4. Completa formulario
5. Frontend envía POST /api/users con token
6. Backend valida token y datos
7. Guarda en BD
8. Actualiza lista en frontend

## 🔄 Flujo Completo de una Petición

```
Usuario hace click → Frontend
                        ↓
            Interceptor añade JWT
                        ↓
            POST/GET/PUT/DELETE → Backend
                        ↓
            AuthTokenFilter valida JWT
                        ↓
                    ¿Válido?
                   ↙️      ↘️
                 Sí       No
                 ↓         ↓
            Controller  401 Error
                 ↓         
             Service      
                 ↓         
            Repository   
                 ↓         
             Database    
                 ↓         
            Respuesta → Frontend
                        ↓
            Actualiza UI
```

## 🎨 Stack Tecnológico Completo

### Backend
```
Spring Boot 3.2.0
├── Spring Web (REST)
├── Spring Data JPA (Persistencia)
├── Spring Security (Autenticación)
├── H2 Database (Desarrollo)
├── Lombok (Reducir boilerplate)
├── JWT (io.jsonwebtoken)
├── Telegram Bots API
└── WebFlux (HTTP Client)
```

### Frontend
```
React 18
├── Vite (Build tool)
├── Axios (HTTP client)
├── Context API (Estado global)
├── React Hooks (useState, useEffect, useContext)
├── CSS3 (Estilos)
└── LocalStorage (Persistencia cliente)
```

## 📚 Recursos Adicionales

- 📖 **README.md** - Documentación completa
- 🚀 **QUICKSTART.md** - Inicio rápido
- 📱 **TELEGRAM_SETUP.md** - Configurar Telegram
- 🧪 **TEST_DATA.md** - Datos de prueba
- 📝 **CHANGELOG.md** - Historial de cambios

---

¡Aplicación fullstack profesional lista para usar! 🎉
