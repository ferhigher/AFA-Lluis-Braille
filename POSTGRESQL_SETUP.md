# Configuración de PostgreSQL

Este documento describe cómo configurar PostgreSQL para la aplicación AFA Fullstack.

## 📋 Requisitos Previos

- PostgreSQL 12 o superior instalado
- Permisos de administrador (sudo) en tu sistema

## 🚀 Instalación Rápida

### Opción 1: Script Automático (Recomendado)

Ejecuta el script de configuración incluido:

```bash
cd backend
./setup-postgres.sh
```

Este script:
- Verifica que PostgreSQL esté instalado
- Inicia el servicio si no está corriendo
- Crea el usuario `afa_user`
- Crea la base de datos `afa_fullstack_db`
- Configura los permisos necesarios

### Opción 2: Configuración Manual

Si prefieres configurar manualmente o el script automático no funciona:

#### 1. Instalar PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
```

**Fedora/RHEL/CentOS:**
```bash
sudo dnf install postgresql postgresql-server
sudo postgresql-setup --initdb
```

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

#### 2. Iniciar el servicio

**Linux:**
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql  # Para iniciar automáticamente
```

**macOS:**
```bash
brew services start postgresql
```

#### 3. Crear el usuario y la base de datos

Conectarse a PostgreSQL como usuario postgres:

```bash
sudo -u postgres psql
```

Dentro de psql, ejecutar:

```sql
-- Crear usuario
CREATE USER afa_user WITH PASSWORD 'afa_password';

-- Crear base de datos
CREATE DATABASE afa_fullstack_db OWNER afa_user;

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE afa_fullstack_db TO afa_user;

-- Conectar a la base de datos
\c afa_fullstack_db

-- Otorgar privilegios en el schema
GRANT ALL ON SCHEMA public TO afa_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO afa_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO afa_user;

-- Salir
\q
```

## 🔧 Configuración de la Aplicación

La configuración ya está actualizada en `backend/src/main/resources/application.properties`:

```properties
# PostgreSQL Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/afa_fullstack_db
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=afa_user
spring.datasource.password=afa_password

# JPA configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

### Cambiar las Credenciales (Opcional)

Si deseas usar credenciales diferentes:

1. Modifica el archivo `application.properties`
2. Actualiza los valores en los comandos SQL anteriores

## 🎯 Iniciar la Aplicación

Una vez configurada la base de datos:

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## ✅ Verificar la Conexión

Para verificar que la base de datos funciona correctamente:

```bash
# Conectarse a la base de datos
psql -U afa_user -d afa_fullstack_db -h localhost

# Listar las tablas (una vez que la aplicación haya creado el schema)
\dt

# Salir
\q
```

## 🔍 Comandos Útiles de PostgreSQL

```bash
# Ver todas las bases de datos
sudo -u postgres psql -c "\l"

# Ver todos los usuarios
sudo -u postgres psql -c "\du"

# Conectarse a la base de datos
psql -U afa_user -d afa_fullstack_db -h localhost

# Ver el estado del servicio
sudo systemctl status postgresql

# Reiniciar el servicio
sudo systemctl restart postgresql
```

## 🐛 Solución de Problemas

### Error: "FATAL: Peer authentication failed"

Edita el archivo `pg_hba.conf`:

```bash
sudo nano /etc/postgresql/*/main/pg_hba.conf
```

Cambia las líneas que dicen `peer` por `md5`:

```
# Antes:
local   all             all                                     peer

# Después:
local   all             all                                     md5
```

Reinicia PostgreSQL:

```bash
sudo systemctl restart postgresql
```

### Error: "Connection refused"

Verifica que PostgreSQL esté corriendo:

```bash
sudo systemctl status postgresql
sudo systemctl start postgresql
```

### Error: "database does not exist"

Asegúrate de haber ejecutado los comandos de creación de la base de datos o el script `setup-postgres.sh`.

## 📊 Migración desde H2

Si tenías datos en H2, necesitarás:

1. Exportar los datos de H2 (si es necesario)
2. La nueva base de datos PostgreSQL estará vacía
3. Las tablas se crearán automáticamente al iniciar la aplicación gracias a `ddl-auto=update`

## 🔐 Seguridad en Producción

Para entornos de producción:

1. **Cambia las credenciales**: No uses las credenciales por defecto
2. **Usa variables de entorno**:
   ```properties
   spring.datasource.username=${DB_USERNAME}
   spring.datasource.password=${DB_PASSWORD}
   ```
3. **Configura SSL**: Para conexiones seguras
4. **Restringe acceso**: Configura el firewall y `pg_hba.conf` apropiadamente
5. **Cambia `ddl-auto`**: Usa `validate` en lugar de `update` en producción

## 📝 Notas Adicionales

- Las tablas se crean automáticamente mediante Hibernate/JPA
- El schema se actualiza automáticamente con `spring.jpa.hibernate.ddl-auto=update`
- Los datos persisten entre reinicios de la aplicación
- Se pueden ver las queries SQL en los logs gracias a `spring.jpa.show-sql=true`

## 🆘 Soporte

Si encuentras problemas, verifica:

1. Que PostgreSQL esté instalado y corriendo
2. Que el usuario y la base de datos existan
3. Que las credenciales en `application.properties` sean correctas
4. Los logs de la aplicación Spring Boot para más detalles
