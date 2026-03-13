# User Stories — EPIC 10: Landing pública — Secciones de contenido institucional

**Prioridad:** P1 — Alto
**Objetivo:** Construir las secciones de contenido estático institucional de la landing: quiénes somos, qué es la AFA, comisiones, colegio y el flujo de captación de socios.
**Dependencias:**
- EPIC 9 completo (la estructura base y la navbar deben existir antes de añadir secciones)

**Referencia de diseño:** https://afavivers.org/
**Nota de contenido:** El contenido textual de cada sección es provisional (placeholder). El equipo de la asociación deberá revisarlo y personalizarlo antes del lanzamiento. Las historias definen la estructura y comportamiento, no el texto final.

**Orden de implementación recomendado:** US-10.1 → US-10.2 → US-10.3 → US-10.4 → US-10.5

---

## US-10.1 — Sección "Qué es la AFA"

**Como** visitante que no conoce la asociación,
**quiero** leer una explicación clara de qué es la AFA Lluís Braille,
**para que** entienda su misión, su papel en el colegio y por qué es relevante para las familias.

**Archivos afectados:**
- `frontend/src/components/WhatIsSection.jsx` — nuevo componente
- `frontend/src/styles/WhatIsSection.css` — nuevo archivo de estilos
- `frontend/src/pages/LandingPage.jsx` — integrar la sección

**Contexto técnico:**
Esta sección se accede desde el dropdown `AFA > Qué es` del navbar. Puede ser una sección ancla `#que-es` dentro de la landing o una sub-página `/que-es`. El contenido es estático (hardcodeado o desde un fichero de constantes, no requiere backend). El diseño sigue el patrón de bloque de texto + imagen lateral o fondo de color suave.

---

### Criterios de aceptación

**CA-10.1.1 — La sección es accesible desde el menú**
- **Dado** que el visitante hace clic en `AFA > Qué es` en la navbar
- **Entonces** la página hace scroll hasta la sección `#que-es` o navega a la página correspondiente sin error

**CA-10.1.2 — La sección tiene heading descriptivo**
- **Dado** que se renderiza la sección "Qué es"
- **Entonces** hay un `<h2>` con texto relevante (ej. `¿Qué es la AFA?` o `Qué es la AFA Lluís Braille`)

**CA-10.1.3 — La sección tiene contenido textual significativo**
- **Dado** que se renderiza la sección
- **Entonces** hay al menos dos párrafos de texto explicando la misión de la AFA, su relación con el colegio y las familias
- **Y** el texto no contiene Lorem Ipsum ni placeholders genéricos en la versión entregada

**CA-10.1.4 — La sección incluye los pilares básicos de la asociación**
- **Dado** que se renderiza la sección "Qué es"
- **Entonces** se muestran los pilares o valores fundamentales de la asociación (ej. estilo de vida saludable, educación para la ciudadanía, disciplina positiva, creatividad, igualdad)
- **Y** cada pilar tiene al menos un título y una frase descriptiva

**CA-10.1.5 — El diseño visual es coherente con la paleta**
- **Dado** que se renderiza la sección
- **Entonces** los acentos visuales (iconos, bordes, fondos de tarjeta) usan la paleta corporativa (`#8b935c`, `#f7b453`)

---

## US-10.2 — Sección "Quiénes somos"

**Como** visitante que quiere saber quién gestiona la asociación,
**quiero** ver una presentación del equipo directivo o junta de la AFA Lluís Braille,
**para que** sepa a quién dirigirme y me sienta más confiado para asociarme.

**Archivos afectados:**
- `frontend/src/components/WhoWeAreSection.jsx` — nuevo componente
- `frontend/src/styles/WhoWeAreSection.css` — nuevo archivo de estilos
- `frontend/src/pages/LandingPage.jsx` — integrar la sección

**Contexto técnico:**
Accesible desde `AFA > Quiénes somos`. El contenido es estático (no requiere backend). Puede incluir tarjetas de miembros del equipo con nombre y cargo. Si no hay fotos disponibles, usar avatares o iconos genéricos.

---

### Criterios de aceptación

**CA-10.2.1 — La sección es accesible desde el menú**
- **Dado** que el visitante hace clic en `AFA > Quiénes somos` en la navbar
- **Entonces** navega hasta la sección `#quienes-somos` o a la página correspondiente sin error

**CA-10.2.2 — La sección tiene heading descriptivo**
- **Dado** que se renderiza la sección
- **Entonces** hay un `<h2>` con texto `Quiénes somos` o equivalente

**CA-10.2.3 — La sección muestra información del equipo o junta directiva**
- **Dado** que se renderiza la sección
- **Entonces** se presenta al menos la estructura de cargos de la asociación (presidente/a, secretario/a, vocales o equivalentes)
- **Y** cada cargo tiene nombre o descripción del rol

**CA-10.2.4 — La sección incluye texto de presentación institucional**
- **Dado** que se renderiza la sección
- **Entonces** hay un párrafo introductorio describiendo el equipo o la junta de forma cercana y accesible

**CA-10.2.5 — El diseño es coherente con el resto de secciones**
- **Dado** que se renderiza la sección
- **Entonces** usa las mismas variables CSS de paleta y tipografía que el resto de la landing

---

## US-10.3 — Sección y página "Asóciate" (CTA principal)

**Como** familiar del alumnado del colegio Lluís Braille,
**quiero** encontrar fácilmente cómo asociarme a la AFA,
**para que** pueda hacerme socio/a y contribuir a la comunidad escolar.

**Archivos afectados:**
- `frontend/src/components/JoinSection.jsx` — nuevo componente (sección en la landing)
- `frontend/src/styles/JoinSection.css` — nuevo archivo de estilos
- `frontend/src/pages/LandingPage.jsx` — integrar la sección
- `frontend/src/pages/JoinPage.jsx` — nuevo componente (página completa de asociación, opcional)

**Contexto técnico:**
Esta es la sección con mayor peso de conversión de la web. El botón `ASOCIATE` del navbar lleva a esta sección o página. La sección debe ser visualmente prominente: fondo de acento `#f7b453`, texto claro, CTA grande. El formulario puede ser interno (datos guardados en backend) o un enlace a un formulario externo (Google Forms, etc.). Para esta US se implementa como enlace externo configurable; la gestión de socios vía backend es EPIC futuro.

---

### Criterios de aceptación

**CA-10.3.1 — El botón ASOCIATE de la navbar navega a la sección correcta**
- **Dado** que el visitante hace clic en el botón `ASOCIATE` de la navbar
- **Entonces** la página hace scroll hasta la sección `#asociate` o navega a `/asociate`

**CA-10.3.2 — La sección tiene heading y descripción de beneficios**
- **Dado** que se renderiza la sección "Asóciate"
- **Entonces** hay un `<h2>` con texto `Asóciate` o `Hazte socio/a`
- **Y** hay al menos un párrafo explicando qué implica ser socio/a (beneficios, implicación, cuota si aplica)

**CA-10.3.3 — La sección muestra los pasos para asociarse**
- **Dado** que se renderiza la sección
- **Entonces** se listan los pasos del proceso de asociación (ej. rellenar formulario, pagar cuota, recibir confirmación)

**CA-10.3.4 — El CTA principal es prominente y funcional**
- **Dado** que se renderiza la sección
- **Entonces** hay un botón grande y destacado (ej. `Quiero asociarme`) con fondo `#f7b453`
- **Y** al hacer clic, abre el formulario de asociación o redirige a la URL configurada para el formulario externo
- **Y** si el enlace externo abre en nueva pestaña, usa `target="_blank" rel="noopener noreferrer"`

**CA-10.3.5 — La URL del formulario externo es configurable**
- **Dado** que la URL del formulario de asociación puede cambiar
- **Entonces** está definida en una constante o variable de configuración del frontend (no hardcodeada en el JSX)

**CA-10.3.6 — La sección es visualmente diferenciada del resto**
- **Dado** que se renderiza la landing
- **Entonces** la sección "Asóciate" tiene un fondo visualmente distinto (color `#f7b453`, `#8b935c` o imagen) que la distingue del resto de secciones

---

## US-10.4 — Sección "Comisiones"

**Como** visitante que quiere entender cómo trabaja la AFA,
**quiero** ver las diferentes áreas de trabajo (comisiones) de la asociación,
**para que** sepa en qué iniciativas puedo participar o de qué se ocupa cada grupo.

**Archivos afectados:**
- `frontend/src/components/CommissionsSection.jsx` — nuevo componente
- `frontend/src/styles/CommissionsSection.css` — nuevo archivo de estilos
- `frontend/src/data/commissions.js` — nuevo fichero de datos estáticos (lista de comisiones)
- `frontend/src/pages/LandingPage.jsx` — integrar la sección

**Contexto técnico:**
El contenido de las comisiones es estático (se define en un fichero de datos, no requiere backend por ahora). Se muestra en formato grid de tarjetas. Referencia: sección de comisiones de afavivers.org con 8 áreas. Las comisiones de AFA Lluís Braille son a confirmar por la asociación; se usan placeholders razonables similares a afavivers.org.

**Comisiones de referencia (ajustar al contexto real de la asociación):**
1. Proyecto Educativo y Formación de Familias
2. Alimentación y Hábitos Saludables
3. Infraestructuras
4. Igualdad, Diversidad e Inclusión
5. Extraescolares
6. Fiestas y Eventos
7. Paz y Solidaridad
8. Huerto / Medio Ambiente

---

### Criterios de aceptación

**CA-10.4.1 — La sección es accesible desde el menú**
- **Dado** que el visitante hace clic en `COMISIONES` en la navbar
- **Entonces** la página navega hasta la sección `#comisiones` sin error

**CA-10.4.2 — La sección muestra todas las comisiones definidas**
- **Dado** que se renderiza la sección
- **Entonces** se muestran todas las comisiones definidas en `commissions.js` (mínimo 4, máximo 10)
- **Y** cada comisión tiene al menos: nombre y descripción breve (una o dos frases)

**CA-10.4.3 — Las comisiones se muestran en formato grid responsivo**
- **Dado** que se renderiza la sección
- **Entonces** en viewport ≥ 768px se muestran al menos 2 columnas de tarjetas
- **Y** en viewport < 768px se muestra 1 columna
- **Y** todas las tarjetas tienen la misma altura (alineación visual)

**CA-10.4.4 — Cada tarjeta tiene icono o ilustración**
- **Dado** que se renderiza la sección de comisiones
- **Entonces** cada tarjeta tiene un icono representativo (puede ser un emoji, SVG o icono de librería)

**CA-10.4.5 — Los datos de comisiones están centralizados**
- **Dado** que el contenido de las comisiones puede cambiar
- **Entonces** está definido en `frontend/src/data/commissions.js` como array de objetos `{ id, nombre, descripcion, icono }`
- **Y** el componente `CommissionsSection` itera sobre ese array sin hardcodear texto en el JSX

**CA-10.4.6 — El diseño visual es coherente con la paleta**
- **Dado** que se renderizan las tarjetas de comisiones
- **Entonces** los bordes, fondos de tarjeta o cabeceras usan los colores `#8b935c` o `#f7b453`

---

## US-10.5 — Sección "Colegio"

**Como** familia interesada en el colegio Lluís Braille,
**quiero** encontrar información básica sobre el centro educativo,
**para que** pueda conocer el colegio antes de dirigirme a él.

**Archivos afectados:**
- `frontend/src/components/SchoolSection.jsx` — nuevo componente
- `frontend/src/styles/SchoolSection.css` — nuevo archivo de estilos
- `frontend/src/pages/LandingPage.jsx` — integrar la sección

**Contexto técnico:**
Accesible desde `COLEGIO` en la navbar. Contenido estático: nombre del centro, dirección, etapas educativas, enlace a la web oficial del colegio si existe. Esta sección no requiere backend. Es informativa y de apoyo a las familias.

---

### Criterios de aceptación

**CA-10.5.1 — La sección es accesible desde el menú**
- **Dado** que el visitante hace clic en `COLEGIO` en la navbar
- **Entonces** la página navega a la sección `#colegio` sin error

**CA-10.5.2 — La sección muestra el nombre y datos básicos del centro**
- **Dado** que se renderiza la sección
- **Entonces** son visibles: nombre del centro (`CEIP Lluís Braille` o equivalente), dirección, localidad
- **Y** hay un `<h2>` con el nombre de la sección o del centro

**CA-10.5.3 — La sección incluye enlace al mapa o dirección**
- **Dado** que se renderiza la sección colegio
- **Entonces** hay un enlace que abre la dirección del colegio en Google Maps u otro servicio de mapas
- **Y** se abre en nueva pestaña con `target="_blank" rel="noopener noreferrer"`

**CA-10.5.4 — La sección menciona las etapas educativas del centro**
- **Dado** que se renderiza la sección
- **Entonces** se indican las etapas educativas que ofrece el centro (ej. Infantil, Primaria)

**CA-10.5.5 — El diseño es coherente con el resto de la landing**
- **Dado** que se renderiza la sección
- **Entonces** usa las mismas variables CSS de paleta y tipografía que el resto de secciones

---

## Orden de implementación sugerido

```
US-10.1 (Qué es la AFA)
  → sección estática, sin dependencias de datos

US-10.2 (Quiénes somos)
  → sección estática, puede hacerse en paralelo con US-10.1

US-10.3 (Asóciate)
  → requiere claridad sobre el formulario externo o proceso antes de implementar

US-10.4 (Comisiones)
  → requiere que la asociación confirme sus comisiones reales; usar datos de referencia mientras tanto

US-10.5 (Colegio)
  → sección estática más simple, puede hacerse en cualquier momento
```

## Definición de hecho (DoD) compartida para todas las US de este epic

- El código compila sin errores ni warnings nuevos
- Cada sección es accesible desde el ítem de menú correspondiente
- Todos los componentes nuevos tienen al menos un test de renderizado con Vitest + RTL que verifica que el heading y el contenido principal se muestran
- No hay texto Lorem Ipsum ni placeholders genéricos en ninguna sección entregada
- Los datos estáticos están centralizados en ficheros de datos (`/src/data/`) y no hardcodeados en el JSX
- Los estilos usan variables CSS de la paleta corporativa
- La sección se visualiza correctamente en mobile (375px) y desktop (1280px)
- Los enlaces externos abren en nueva pestaña con `rel="noopener noreferrer"`
