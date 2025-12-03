# Credenciales de Prueba

## 🔑 Usuarios de Prueba

Para facilitar las pruebas, puedes crear estos usuarios después de iniciar la aplicación:

### Usuario Administrador
- **Nombre:** Admin User
- **Username:** admin
- **Email:** admin@example.com
- **Password:** admin123
- **Teléfono:** +34 600 000 001

### Usuario Normal
- **Nombre:** Test User
- **Username:** testuser
- **Email:** test@example.com
- **Password:** test123
- **Teléfono:** +34 600 000 002

## 📝 Datos de Prueba para Telegram

Si no tienes acceso al bot de Telegram, puedes usar estos mensajes de ejemplo para crear noticias manuales:

### Noticia 1
```
📢 Reunión General de la AFA
Os recordamos que mañana jueves a las 18:00h tendremos la reunión mensual de la AFA en el salón de actos del centro. 
¡Os esperamos a todos!
```

### Noticia 2
```
🎓 Nuevo Curso de Braille
Informamos que el próximo mes empezará un nuevo curso de Braille nivel avanzado. 
Las inscripciones están abiertas hasta el día 30. 
Más información en secretaría.
```

### Noticia 3
```
🏆 Felicitaciones
Queremos felicitar a María López por su excelente trabajo en el proyecto de accesibilidad digital. 
¡Enhorabuena María!
```

### Noticia 4
```
📚 Biblioteca Actualizada
La biblioteca del centro ha incorporado 50 nuevos títulos en formato audio y Braille. 
Pasad por la biblioteca para consultar el catálogo completo.
```

## 🗄️ Configuración de Base de Datos

### H2 Console (Desarrollo)
- **URL:** http://localhost:8080/h2-console
- **JDBC URL:** jdbc:h2:mem:testdb
- **Usuario:** sa
- **Password:** (vacío)

### Consultas SQL Útiles

**Ver todos los usuarios:**
```sql
SELECT * FROM users;
```

**Ver todos los mensajes de Telegram:**
```sql
SELECT * FROM telegram_messages;
```

**Contar usuarios registrados:**
```sql
SELECT COUNT(*) FROM users;
```

**Ver mensajes ordenados por fecha:**
```sql
SELECT message_id, text, message_date 
FROM telegram_messages 
ORDER BY message_date DESC;
```

## 🔧 Configuración JWT

Los tokens JWT están configurados con:
- **Algoritmo:** HS512
- **Expiración:** 24 horas (86400000 ms)
- **Secret Key:** (definida en application.properties)

Para cambiar la expiración, modifica en `application.properties`:
```properties
jwt.expiration=86400000  # 24 horas en milisegundos
```

## 📱 Configuración de Telegram (Opcional)

Si tienes un bot de Telegram configurado:

```properties
telegram.bot.token=TU_TOKEN_AQUI
telegram.channel.username=@afa_lluis_braille
```

Ver `TELEGRAM_SETUP.md` para instrucciones detalladas.

## 🧪 Endpoints para Probar con Postman/cURL

### Registro
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "username": "testuser",
    "email": "test@example.com",
    "password": "test123",
    "phone": "+34 600 000 002"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123"
  }'
```

### Obtener usuarios (con token)
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### Crear noticia manual
```bash
curl -X POST http://localhost:8080/api/telegram/manual \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Esta es una noticia de prueba"
  }'
```

## 💡 Tips para Desarrollo

1. **Auto-reload:** El backend usa Spring DevTools y el frontend usa Vite, ambos tienen hot-reload activado
2. **Logs:** Los logs del backend se muestran en la consola, nivel DEBUG para desarrollo
3. **Errores:** Los errores de validación se muestran automáticamente en el frontend
4. **Persistencia:** Los datos se guardan en H2 (memoria), se perderán al reiniciar el backend

## 🔄 Reiniciar Datos

Si quieres limpiar todos los datos y empezar de nuevo:

1. **Detén el backend** (Ctrl+C)
2. **Reinicia el backend** (mvn spring-boot:run)
3. Todos los datos en H2 se habrán borrado
4. Vuelve a crear los usuarios de prueba

## 📊 Monitoreo

Para ver el estado de la aplicación:

- **Backend:** http://localhost:8080 (debería responder con error 401 si no estás autenticado)
- **Frontend:** http://localhost:3000
- **H2 Console:** http://localhost:8080/h2-console

## ⚠️ Notas Importantes

- La base de datos H2 está en **memoria**, los datos se pierden al reiniciar
- Los tokens JWT expiran después de 24 horas
- Las contraseñas se encriptan con BCrypt antes de guardarse
- El CORS está configurado solo para localhost (desarrollo)
