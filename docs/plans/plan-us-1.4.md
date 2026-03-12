# Plan de implementación — US-1.4: Externalizar secretos a variables de entorno

---

## 1. Resumen de la US

**Problema que resuelve:** `application.properties` contiene en texto plano el JWT secret, las credenciales de PostgreSQL y el token del bot de Telegram. Este archivo está commiteado en el repositorio. Cualquiera con acceso al repo tiene acceso a esos valores.

**Archivos afectados:**
- `backend/src/main/resources/application.properties` ← reemplazar valores reales por `${VAR}`
- `.env.example` ← nuevo archivo en la raíz del proyecto
- `.gitignore` ← verificar que `.env` está excluido

**Dependencias con otras US:**
- Ninguna bloqueante. Esta US puede hacerse en cualquier orden dentro del EPIC 1.
- El `application.properties` de test (`backend/src/test/resources/application.properties`) ya usa valores fijos seguros — no se toca.

---

## 2. Análisis del código actual

### `application.properties` — secretos en texto plano

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/afa_fullstack_db   # ← expuesto
spring.datasource.username=afa_user                                         # ← expuesto
spring.datasource.password=afa_password                                     # ← expuesto

jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437 # ← expuesto
jwt.expiration=86400000                                                      # no es secreto

telegram.bot.token=YOUR_BOT_TOKEN_HERE                                       # placeholder, debe ser var
telegram.channel.username=@afa_lluis_braille                                 # no es secreto
```

Las cinco propiedades marcadas deben externalizarse. `jwt.expiration` y `telegram.channel.username` son configuración no sensible — se dejan como valores directos.

### `.gitignore` — cobertura de `.env`

El `.gitignore` ya incluye:
```
*.env
*.env.*
!*.env.example
```

El patrón `*.env` en git **sí cubre** `.env` (el `*` hace match de string vacío). Está correcto, pero para máxima claridad añadimos `.env` explícito en la sección de datos sensibles.

### Mecanismo de Spring para variables de entorno

Spring Boot resuelve `${VAR_NAME}` en `application.properties` desde (por orden de precedencia):
1. Variables de entorno del sistema operativo (`export JWT_SECRET=...`)
2. Archivo `.env` si se usa un plugin como `spring-dotenv` — **no requerido aquí**
3. `application-local.properties` (perfil local) — opción alternativa documentada

Para entorno local sin plugin, la forma estándar es exportar las variables antes de `mvn spring-boot:run`, o definirlas en el IDE. El `.env.example` documenta qué variables hacen falta.

### Riesgo: arranque sin variables definidas

Si una variable no está definida, Spring Boot lanza:
```
Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
```
Esto cumple CA-1.4.3 de forma automática — no requiere código adicional.

### Tests de integración — sin impacto

Los tests usan `backend/src/test/resources/application.properties` con valores H2 hardcodeados seguros. No se ven afectados por este cambio.

---

## 3. Plan de implementación paso a paso

### Paso 1 — Reemplazar secretos en `application.properties`

**Archivo:** `backend/src/main/resources/application.properties`

**Qué cambia:** Sustituir los cinco valores sensibles por referencias a variables de entorno. El resto de propiedades (logging, JPA config, `jwt.expiration`, `telegram.channel.username`) no cambian.

```properties
# Server configuration
server.port=8080

# PostgreSQL Database configuration
spring.datasource.url=${DB_URL}
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# Telegram Configuration
telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.channel.username=@afa_lluis_braille

# Logging Configuration - MUY DETALLADO PARA DEBUGGING
logging.level.root=INFO
logging.level.org.springframework=INFO
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.com.example.demo=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Formato de logs más legible
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# Mostrar todos los parámetros de SQL
spring.jpa.properties.hibernate.use_sql_comments=true
```

---

### Paso 2 — Crear `.env.example` en la raíz del proyecto

**Archivo nuevo:** `.env.example` (en la raíz, junto a `backend/` y `frontend/`)

**Qué cambia:** Archivo nuevo con todas las variables requeridas, sus nombres correctos y valores de ejemplo.

**Motivo:** Documenta el contrato de configuración para cualquier desarrollador que clone el repo. No contiene valores reales — es seguro commitear.

```dotenv
# Copia este archivo a .env y rellena los valores reales
# El archivo .env real está excluido del repositorio (.gitignore)

# Base de datos PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/afa_fullstack_db
DB_USERNAME=afa_user
DB_PASSWORD=tu_password_aqui

# JWT — usa un string aleatorio largo (mínimo 32 caracteres)
JWT_SECRET=cambia_este_valor_por_un_string_aleatorio_largo_minimo_256_bits

# Telegram Bot (obtener de @BotFather)
TELEGRAM_BOT_TOKEN=tu_token_del_bot_aqui
```

---

### Paso 3 — Verificar y reforzar `.gitignore`

**Archivo:** `.gitignore` (raíz)

**Qué cambia:** Añadir `.env` explícito en la sección de datos sensibles, además del `*.env` ya existente, para máxima claridad.

En la sección `# Security and Sensitive Data`, añadir `.env` como entrada explícita:

```
# Environment and configuration files with sensitive data
.env
*.env
*.env.*
!*.env.example
```

---

## 4. Tests que cubren los criterios de aceptación

### Naturaleza de los CAs de esta US

Los CAs de US-1.4 son principalmente de infraestructura y configuración, no de lógica de negocio. No se generan tests de código nuevos por las siguientes razones:

- **CA-1.4.1** (application.properties sin secretos): verificación estática del archivo — cubierta por inspección visual y el commit.
- **CA-1.4.2** (backend arranca con vars definidas): los **21 tests de integración existentes** ya cubren esto. Todos usan `@SpringBootTest` con `test/resources/application.properties` que tiene los valores necesarios. Si el contexto falla, los tests fallan.
- **CA-1.4.3** (error claro si falta var): comportamiento por defecto de Spring Boot — no requiere test de código.
- **CA-1.4.4** (existe `.env.example`): verificación de fichero — no requiere test de código.
- **CA-1.4.5** (`.env` excluido del repo): verificación de `.gitignore` — no requiere test de código.
- **CA-1.4.6** (dev puede arrancar con `.env.example` como base): verificación manual.

**Verificación automática relevante:** ejecutar `mvn test` tras el cambio confirma que los 21 tests existentes siguen pasando con el `application.properties` de test (H2), lo que valida CA-1.4.2.

---

## 5. Verificación manual

### Preparar el entorno

```bash
# En la raíz del proyecto
cp .env.example .env
# Editar .env con los valores reales del entorno local
```

Contenido mínimo del `.env` local:
```dotenv
DB_URL=jdbc:postgresql://localhost:5432/afa_fullstack_db
DB_USERNAME=afa_user
DB_PASSWORD=afa_password
JWT_SECRET=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
TELEGRAM_BOT_TOKEN=YOUR_BOT_TOKEN_HERE
```

### Levantar el backend exportando las variables

```bash
# Opción A: exportar variables manualmente y luego arrancar
export $(cat .env | xargs)
cd backend
mvn spring-boot:run
```

```bash
# Opción B: pasar las vars directamente a Maven
cd backend
DB_URL=jdbc:postgresql://localhost:5432/afa_fullstack_db \
DB_USERNAME=afa_user \
DB_PASSWORD=afa_password \
JWT_SECRET=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437 \
TELEGRAM_BOT_TOKEN=YOUR_BOT_TOKEN_HERE \
mvn spring-boot:run
```

### Levantar el frontend

```bash
cd frontend
npm run dev
# Verificar que arranca en http://localhost:5173
```

---

### Verificación de cada CA

**CA-1.4.1 — `application.properties` no contiene valores secretos**

```bash
grep -E "(password|secret|token|username)" \
  backend/src/main/resources/application.properties
```
Respuesta esperada: todas las líneas muestran `${...}`, ninguna contiene un valor real.

---

**CA-1.4.2 — El backend arranca correctamente con las variables definidas**

```bash
# Con las vars exportadas:
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/telegram/messages
```
Respuesta esperada: `200` — el backend arrancó y responde.

---

**CA-1.4.3 — El backend falla con mensaje claro si falta una variable**

```bash
# Arrancar SIN definir JWT_SECRET
cd backend
DB_URL=jdbc:postgresql://localhost:5432/afa_fullstack_db \
DB_USERNAME=afa_user \
DB_PASSWORD=afa_password \
TELEGRAM_BOT_TOKEN=test \
mvn spring-boot:run 2>&1 | grep -i "placeholder\|JWT_SECRET\|Could not resolve"
```
Respuesta esperada: línea de error indicando `Could not resolve placeholder 'JWT_SECRET'`.

---

**CA-1.4.4 — Existe `.env.example` documentado**

```bash
cat .env.example
```
Respuesta esperada: archivo con las 5 variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `TELEGRAM_BOT_TOKEN`) con valores de ejemplo.

---

**CA-1.4.5 — El archivo `.env` real está excluido del repositorio**

```bash
# Crear .env de prueba si no existe
touch .env
git status .env
```
Respuesta esperada: `.env` no aparece en los archivos a commitear (está ignored).

```bash
git check-ignore -v .env
```
Respuesta esperada: muestra la línea del `.gitignore` que lo excluye.

---

**CA-1.4.6 — Un desarrollador puede arrancar con `.env.example` como base**

```bash
cp .env.example .env
# Editar .env con los valores locales reales
export $(cat .env | xargs)
cd backend && mvn spring-boot:run
```
Respuesta esperada: el backend arranca sin errores en `http://localhost:8080`.

---

### Verificación en el navegador

Esta US no tiene cambios en el frontend. La verificación visual es:
1. Con el backend arrancado desde variables de entorno, ir a `http://localhost:5173`
2. Hacer login → debe funcionar (JWT se genera correctamente con el secret de la variable)
3. Ver la sección de Noticias → debe cargarse (DB conectada con las credenciales de la variable)

---

## 6. Definición de hecho (DoD)

- [ ] El código compila sin errores ni warnings nuevos (`mvn compile`)
- [ ] Los 21 tests existentes siguen pasando en verde (`mvn test`)
- [ ] CA-1.4.1: `grep` sobre `application.properties` no muestra ningún valor real — solo `${...}`
- [ ] CA-1.4.2: Backend arranca y responde en `http://localhost:8080` con las variables definidas
- [ ] CA-1.4.3: Sin `JWT_SECRET` definido, Spring Boot lanza error de startup con mensaje claro
- [ ] CA-1.4.4: Existe `.env.example` en la raíz con las 5 variables documentadas
- [ ] CA-1.4.5: `git check-ignore -v .env` confirma que `.env` está excluido
- [ ] CA-1.4.6: Un desarrollador puede seguir las instrucciones de `.env.example` y arrancar el backend
- [ ] `application.properties` commiteado no contiene ningún secreto real
- [ ] El archivo `.env` local con valores reales nunca aparece en `git status`
