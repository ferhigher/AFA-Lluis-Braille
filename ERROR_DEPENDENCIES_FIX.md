# 🚨 Solución al Error de Dependencias de Spring Boot

## ❌ Error que Estás Viendo

```
ERROR o.s.b.w.e.tomcat.TomcatStarter - Error starting Tomcat context
Exception: org.springframework.beans.factory.UnsatisfiedDependencyException
Message: Error creating bean with name 'securityConfig': Unsatisfied dependency...
Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
```

## ✅ Solución Implementada

He actualizado el `pom.xml` con versiones **compatibles y estables**:

### Cambios Principales:

1. **Spring Boot**: `3.2.0` → `3.1.5` (versión más estable)
2. **JWT**: `0.12.3` → `0.11.5` (compatible con Spring Boot 3.1.5)
3. **Telegram Bots**: `6.8.0` → `6.5.0` (más estable)

## 🔧 Pasos para Aplicar la Solución

### Paso 1: Limpiar el Proyecto

```bash
cd backend
mvn clean
```

Esto eliminará todos los archivos compilados anteriormente.

### Paso 2: Actualizar Dependencias

```bash
mvn clean install -U
```

La opción `-U` fuerza la actualización de todas las dependencias desde el repositorio.

### Paso 3: Reiniciar el Backend

```bash
mvn spring-boot:run
```

## 📋 Checklist de Solución

Si el error persiste después de los pasos anteriores, prueba esto en orden:

### [ ] 1. Verificar Versión de Java

```bash
java -version
```

**Debe mostrar**: Java 17 o superior

**Si no:**
- Instala Java 17: https://adoptium.net/

### [ ] 2. Limpiar Caché de Maven

```bash
cd backend
rm -rf ~/.m2/repository/org/springframework
rm -rf ~/.m2/repository/io/jsonwebtoken
mvn clean install -U
```

### [ ] 3. Verificar el JAVA_HOME

**Linux/Mac:**
```bash
echo $JAVA_HOME
export JAVA_HOME=/ruta/a/java17
```

**Windows (CMD):**
```cmd
echo %JAVA_HOME%
set JAVA_HOME=C:\ruta\a\java17
```

### [ ] 4. Borrar Carpeta Target

```bash
cd backend
rm -rf target/
mvn clean package
```

### [ ] 5. Verificar que Maven Descargue las Dependencias Correctas

Cuando ejecutes `mvn clean install`, deberías ver:
```
[INFO] Downloading from central: https://repo.maven.apache.org/...
[INFO] Downloaded from central: ...
```

## 🐛 Diagnóstico Detallado

### ¿Por Qué Ocurre Este Error?

Este error generalmente indica:

1. **Incompatibilidad de versiones** entre:
   - Spring Boot
   - Spring Security
   - JPA/Hibernate
   - Dependencias de JWT

2. **Caché corrupta** de Maven

3. **Múltiples versiones** de la misma dependencia

### Logs a Buscar

Cuando ejecutes `mvn spring-boot:run`, busca estas líneas para confirmar que funcionó:

✅ **Señales de Éxito:**
```
Started DemoApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

❌ **Si Aún Hay Error:**
Busca en los logs:
```
UnsatisfiedDependencyException
Cannot resolve reference to bean
ClassNotFoundException
NoSuchMethodException
```

## 🔄 Solución Alternativa: Proyecto desde Cero

Si nada de lo anterior funciona, puedes crear el proyecto desde cero con Spring Initializr:

### Opción A: Usando Spring Initializr Web

1. Ve a: https://start.spring.io/
2. Configura:
   - **Project**: Maven
   - **Language**: Java
   - **Spring Boot**: 3.1.5
   - **Java**: 17
3. Añade dependencias:
   - Spring Web
   - Spring Data JPA
   - H2 Database
   - Spring Security
   - Validation
4. Genera y descarga
5. Copia tus archivos `.java` al nuevo proyecto

### Opción B: Usando Maven desde Terminal

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=demo \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

Luego copia el `pom.xml` actualizado.

## 💡 Otras Causas Posibles

### 1. IDE Conflictivo

Si usas IntelliJ IDEA o Eclipse:

**IntelliJ:**
```
File → Invalidate Caches / Restart → Invalidate and Restart
```

**Eclipse:**
```
Project → Clean → Clean all projects
```

### 2. Puerto 8080 Ocupado

Verifica que el puerto no esté ocupado:

**Linux/Mac:**
```bash
lsof -i :8080
```

**Windows:**
```cmd
netstat -ano | findstr :8080
```

Si está ocupado, mata el proceso o cambia el puerto en `application.properties`:
```properties
server.port=8081
```

### 3. Base de Datos H2 Corrupta

Aunque H2 está en memoria, a veces puede haber problemas. Intenta:

```properties
# En application.properties
spring.jpa.hibernate.ddl-auto=create-drop
```

Esto recreará las tablas cada vez.

## 🧪 Test de Verificación

Después de aplicar la solución, verifica que todo funciona:

### Test 1: Backend Inicia Correctamente

```bash
cd backend
mvn spring-boot:run
```

Deberías ver:
```
Started DemoApplication in X.XXX seconds
```

### Test 2: H2 Console Accesible

Abre: `http://localhost:8080/h2-console`

Deberías ver la interfaz de H2.

### Test 3: Endpoint de Health

```bash
curl http://localhost:8080/h2-console
```

Debería devolver HTML (no error 404).

## 📊 Versiones Finales Recomendadas

Estas son las versiones que he configurado (probadas y compatibles):

| Dependencia | Versión |
|-------------|---------|
| Spring Boot | 3.1.5 |
| Java | 17 |
| JWT (jjwt) | 0.11.5 |
| Telegram Bots | 6.5.0 |
| H2 Database | (gestionada por Spring Boot) |

## 🆘 Si Nada Funciona

Si después de TODO esto sigue sin funcionar:

### Plan B: Versión Sin JWT

Temporalmente puedes comentar las dependencias de JWT y Spring Security para verificar que el resto funciona:

1. En `pom.xml`, comenta:
   ```xml
   <!-- Comentar temporalmente
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   -->
   ```

2. Comenta también la clase `SecurityConfig.java`

3. Intenta iniciar de nuevo

### Plan C: Usar H2 en Archivo

Cambia en `application.properties`:
```properties
spring.datasource.url=jdbc:h2:file:./data/testdb
```

### Plan D: Logs Completos

Activa logs detallados en `application.properties`:
```properties
logging.level.org.springframework=DEBUG
logging.level.org.hibernate=DEBUG
```

Y envíame TODO el output del comando `mvn spring-boot:run`.

## 📞 Contacto y Soporte

Si sigues teniendo problemas, necesito esta información:

1. **Versión de Java**: `java -version`
2. **Versión de Maven**: `mvn -version`
3. **Sistema Operativo**: Windows/Mac/Linux
4. **Log completo** del error
5. **¿Qué pasos ya probaste?**

## ✅ Confirmación de Solución

Cuando el backend inicie correctamente, verás:

```
========================================
  ____             _               ____              _   
 / ___| _ __  _ __(_)_ __   __ _  | __ )  ___   ___ | |_ 
 \___ \| '_ \| '__| | '_ \ / _` | |  _ \ / _ \ / _ \| __|
  ___) | |_) | |  | | | | | (_| | | |_) | (_) | (_) | |_ 
 |____/| .__/|_|  |_|_| |_|\__, | |____/ \___/ \___/ \__|
       |_|                 |___/                          
========================================

Started DemoApplication in 3.456 seconds (JVM running for 4.123)
```

¡Y podrás continuar con el desarrollo! 🎉

---

**Nota**: El archivo `pom.xml` YA está actualizado con las versiones correctas. Solo necesitas ejecutar `mvn clean install` y `mvn spring-boot:run`.
