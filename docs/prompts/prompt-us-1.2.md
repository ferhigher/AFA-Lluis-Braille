Actúa como un senior fullstack engineer especializado en Spring Boot y React.

Voy a pedirte que generes un **plan de implementación detallado** para la User Story US-1.1.

---

## Contexto del proyecto

Lee los siguientes documentos antes de generar el plan:
- `PRD.md` — requisitos del producto, bugs conocidos y prioridades
- `EPICS.md` — todos los epics, sus dependencias y el mapa de US's
- `US-EPIC1.md` — definición completa de la US, criterios de aceptación y DoD del epic

Explora el código fuente real de los archivos afectados indicados en la US antes de proponer cambios.

---

## Lo que debes generar

Genera un plan de implementación con las siguientes secciones:

### 1. Resumen de la US
- Qué problema resuelve
- Archivos afectados (con rutas completas)
- Dependencias con otras US's (según `EPICS.md`)

### 2. Análisis del código actual
- Lee y analiza el código actual de cada archivo afectado
- Identifica exactamente qué hay que cambiar y por qué
- Señala cualquier efecto secundario o riesgo

### 3. Plan de implementación paso a paso
Desglosa los cambios en pasos atómicos y ordenados. Para cada paso:
- Archivo a modificar (ruta completa)
- Cambio concreto (qué se añade, elimina o modifica)
- Motivo del cambio
- Si aplica: usa `/java-refactoring-extract-method` para extracciones de método en Java

Aplica los principios de `/clean-code` en cada cambio propuesto:
- Nombres expresivos y sin abreviaciones
- Funciones pequeñas con una sola responsabilidad
- Sin comentarios redundantes
- Sin código duplicado

Si hay código SQL o acceso a base de datos PostgreSQL, aplica `/postgresql-code-review` para validar el diseño.

Si hay componentes React nuevos o modificados, aplica `/vercel-react-best-practices` para optimizar rendimiento y estructura.

### 4. Tests que cubren los criterios de aceptación
Para cada CA definido en la US, genera el test correspondiente:

**Backend (Spring Boot):**
- Usa `/backend-testing` para generar los tests
- Tests de integración con `@SpringBootTest` + `MockMvc` + base de datos H2 para endpoints
- Tests unitarios con Mockito para servicios
- Nombra cada test según el CA que cubre: `test_CA_X_Y_descripcion()`
- Incluye casos happy path y error path

**Frontend (React + Vitest):**
- Usa Vitest + React Testing Library
- Tests de componente para comportamiento visible
- Mockea llamadas a API con `vi.mock`

Para cada test indica:
- Archivo donde va el test (ruta completa)
- Código completo del test
- CA que cubre

### 5. Verificación manual (levantar la app)
Incluye instrucciones completas para:

1. **Preparar el entorno:**
   ```bash
   # Copiar variables de entorno si no existe .env
   cp .env.example .env
   # Editar .env con los valores locales si es necesario
   ```

2. **Levantar el backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   # Verificar que arranca en http://localhost:8080
   ```

3. **Levantar el frontend:**
   ```bash
   cd frontend
   npm run dev
   # Verificar que arranca en http://localhost:5173
   ```

4. **Verificación de cada CA con curl/Postman:**
   Para cada criterio de aceptación de la US, proporciona el comando `curl` exacto (o descripción de la prueba en el navegador) que demuestra que el CA se cumple. Incluye:
   - El comando curl con headers, body y URL completa
   - La respuesta esperada (status code + body relevante)
   - Qué observar para confirmar que el CA pasa

5. **Verificación en el navegador:**
   Si la US tiene cambios en el frontend, describe paso a paso el flujo a seguir en `http://localhost:5173` para verificar el comportamiento.

### 6. Definición de hecho (DoD)
Checklist final antes de dar la US por terminada:
- [ ] El código compila sin errores ni warnings nuevos
- [ ] Todos los tests generados en el paso 4 pasan en verde
- [ ] Cada CA ha sido verificado manualmente (paso 5)
- [ ] No se han introducido nuevas rutas sin protección
- [ ] Los archivos modificados no contienen credenciales en texto plano
- [ ] `application.properties` no contiene ningún secreto en texto plano
- [ ] Usa `/code-refactoring` para revisar si hay duplicación o simplificación posible antes de cerrar

---

## Restricciones

- No generes código que no sea estrictamente necesario para esta US
- No refactorices código fuera del alcance de la US
- No añadas dependencias nuevas sin justificarlas
- Si la US tiene dependencias previas no implementadas, indícalo claramente al inicio del plan
- Respeta el orden de implementación sugerido en `US-EPIC<N>.md`

---

Genera el plan completo para **US-1.1**.