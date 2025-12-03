# Guía de Configuración de Telegram Bot

Esta guía te ayudará a configurar un bot de Telegram para recibir noticias del canal "AFA Lluís Braille".

## 📋 Requisitos Previos

- Cuenta de Telegram
- Acceso al canal @afa_lluis_braille (o ser administrador)

## 🤖 Paso 1: Crear un Bot de Telegram

1. **Abre Telegram** y busca [@BotFather](https://t.me/botfather)

2. **Inicia una conversación** con BotFather y envía el comando:
   ```
   /newbot
   ```

3. **Sigue las instrucciones:**
   - Te pedirá un nombre para tu bot (ejemplo: "AFA Lluís Braille News Bot")
   - Luego te pedirá un username (debe terminar en 'bot', ejemplo: "afa_braille_news_bot")

4. **Guarda el token** que te proporciona BotFather. Se verá algo así:
   ```
   1234567890:ABCdefGHIjklMNOpqrsTUVwxyz-123456789
   ```
   ⚠️ **IMPORTANTE**: Guarda este token de forma segura, lo necesitarás más adelante.

## 🔑 Paso 2: Configurar Permisos del Bot

### Opción A: Si eres administrador del canal

1. **Abre el canal** @afa_lluis_braille en Telegram
2. Haz clic en el nombre del canal → **Administradores** → **Añadir Administrador**
3. Busca tu bot (por el username que creaste)
4. Asigna los siguientes permisos mínimos:
   - ✅ Ver mensajes
   - ✅ Enviar mensajes (opcional)

### Opción B: Si NO eres administrador

Contacta con los administradores del canal y solicita que añadan tu bot como administrador con permisos para ver mensajes.

## ⚙️ Paso 3: Configurar el Backend

1. **Navega al archivo de configuración:**
   ```
   backend/src/main/resources/application.properties
   ```

2. **Reemplaza los valores de configuración:**
   ```properties
   # Reemplaza YOUR_BOT_TOKEN_HERE con tu token real
   telegram.bot.token=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz-123456789
   
   # El username del canal (mantener el @)
   telegram.channel.username=@afa_lluis_braille
   ```

3. **Guarda el archivo**

## 🚀 Paso 4: Iniciar la Aplicación

1. **Inicia el backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Inicia el frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

3. **Abre la aplicación** en `http://localhost:3000`

## 📱 Paso 5: Probar la Conexión

1. **Inicia sesión** en la aplicación
2. Navega a la sección **"Noticias"**
3. Haz clic en **"Actualizar Noticias"**
4. Si todo está configurado correctamente, verás los mensajes del canal

## 🧪 Modo de Prueba (Sin Bot Real)

Si no tienes acceso al canal o prefieres probar sin configurar Telegram:

1. En la sección de **"Noticias"**, encontrarás un formulario para **"Crear Noticia Manual"**
2. Escribe un texto de prueba y haz clic en **"Crear Noticia"**
3. Esto te permite probar toda la funcionalidad sin necesidad del bot

## ⚠️ Solución de Problemas

### El bot no ve los mensajes

**Problema:** El bot no recibe actualizaciones del canal.

**Soluciones:**
1. Verifica que el bot sea administrador del canal
2. Asegúrate de que el bot tenga permiso para "Ver mensajes"
3. Revisa que el token esté correctamente configurado
4. Comprueba que el username del canal sea correcto (con @)

### Error de token inválido

**Problema:** "Error al actualizar mensajes: 401 Unauthorized"

**Soluciones:**
1. Verifica que el token esté correctamente copiado (sin espacios adicionales)
2. Asegúrate de que el token no haya expirado
3. Si es necesario, crea un nuevo bot con BotFather

### No se muestran mensajes antiguos

**Problema:** Solo aparecen mensajes nuevos después de configurar el bot.

**Explicación:** 
Por diseño de Telegram, los bots solo pueden ver mensajes enviados después de que fueron añadidos al canal. Los mensajes históricos no están disponibles a través de la API de bots.

**Alternativa:**
Usa la función "Crear Noticia Manual" para añadir contenido histórico manualmente.

## 🔐 Seguridad

### Mejores Prácticas

1. **No compartas tu token** en repositorios públicos
2. Considera usar **variables de entorno** en producción:
   ```properties
   telegram.bot.token=${TELEGRAM_BOT_TOKEN}
   ```

3. Para producción, crea un archivo `.env` o usa secretos de tu plataforma de hosting

### Ejemplo con variables de entorno

**En sistemas Unix/Linux/Mac:**
```bash
export TELEGRAM_BOT_TOKEN="1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"
mvn spring-boot:run
```

**En Windows (CMD):**
```cmd
set TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
mvn spring-boot:run
```

## 📊 Actualización Automática (Avanzado)

Para configurar actualizaciones automáticas cada cierto tiempo, puedes añadir un scheduler en Spring Boot:

```java
@Scheduled(fixedRate = 300000) // Cada 5 minutos
public void updateTelegramMessages() {
    telegramService.fetchAndSaveMessages();
}
```

Añade `@EnableScheduling` en tu clase principal:

```java
@SpringBootApplication
@EnableScheduling
public class DemoApplication {
    // ...
}
```

## 📞 Soporte

Si tienes problemas con la configuración:

1. Revisa los logs del backend para mensajes de error detallados
2. Verifica que todas las dependencias estén instaladas correctamente
3. Asegúrate de que los puertos 8080 y 3000 estén disponibles

## 🎉 ¡Listo!

Una vez configurado correctamente, tu aplicación estará conectada al canal de Telegram y podrás visualizar todas las noticias publicadas.
