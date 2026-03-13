# User Stories — EPIC 9: Landing pública — Estructura base y navegación

**Prioridad:** P1 — Alto
**Objetivo:** Crear la landing pública de `afalluisbraille.org` con estructura de navegación completa, hero section y footer, accesible sin autenticación.
**Dependencias:**
- EPIC 4 (US-4.1 React Router) — la landing necesita la ruta pública `/` diferenciada del panel privado
- EPIC 5 (US-5.1 y US-5.2 Branding) — la paleta y el logo deben estar resueltos antes de maquetar

**Referencia de diseño:** https://afavivers.org/
**Paleta:**
- Verde primario: `#8b935c`
- Acento crema/naranja: `#f7b453`
- Fondo: `#ffffff` y `#f5f5f0`
- Fuente headings: Jost (Google Fonts)
- Fuente body: Roboto (Google Fonts)
- Fuente botones: Montserrat bold, uppercase

**Orden de implementación recomendado:** US-9.1 → US-9.2 → US-9.3 → US-9.4

---

## US-9.1 — Ruta pública raíz y estructura base de la landing

**Como** visitante de `afalluisbraille.org`,
**quiero** que al acceder a la URL raíz `/` vea una página pública de presentación de la asociación,
**para que** pueda conocer la AFA sin necesidad de estar registrado ni autenticado.

**Archivos afectados:**
- `frontend/src/App.jsx` — añadir ruta pública `/`
- `frontend/src/pages/LandingPage.jsx` — nuevo componente (página contenedor)
- `frontend/src/styles/LandingPage.css` — nuevo archivo de estilos
- `frontend/vite.config.js` — verificar historyApiFallback para SPA

**Contexto técnico:**
Actualmente la app arranca directamente en el formulario de login. Con React Router (US-4.1), hay que añadir una ruta pública `/` que cargue la landing antes del login. El resto de rutas privadas (`/panel`, `/usuarios`, `/noticias-admin`) siguen protegidas. La landing no debe requerir token.

---

### Criterios de aceptación

**CA-9.1.1 — La ruta `/` carga la landing sin autenticación**
- **Dado** que un visitante accede a `http://localhost:5173/`
- **Cuando** no tiene ningún token JWT almacenado
- **Entonces** ve la página de landing (no el formulario de login ni una redirección)

**CA-9.1.2 — La landing tiene estructura semántica correcta**
- **Dado** que se renderiza la landing
- **Entonces** el DOM contiene los elementos `<header>`, `<main>` y `<footer>` en ese orden
- **Y** el `<main>` contiene al menos las secciones identificables por `id`: `hero`, `noticias`, `comisiones`, `quienes-somos`, `colegio`

**CA-9.1.3 — El acceso al panel privado sigue requiriendo login**
- **Dado** que un visitante sin token intenta acceder a `/panel` o `/usuarios`
- **Cuando** React Router evalúa la ruta
- **Entonces** es redirigido a `/login`

**CA-9.1.4 — El usuario autenticado puede acceder a la landing**
- **Dado** que un usuario tiene un JWT válido almacenado
- **Cuando** accede a `/`
- **Entonces** ve la landing normalmente (la landing no redirige a usuarios autenticados al panel)

**CA-9.1.5 — La página tiene título y metadatos correctos**
- **Dado** que se carga la landing
- **Entonces** el `<title>` del documento es `AFA Lluís Braille`
- **Y** existe una meta descripción relevante en el `index.html`

**CA-9.1.6 — La landing es responsive**
- **Dado** que se accede desde un viewport de 375px de ancho (móvil)
- **Entonces** el contenido se adapta correctamente sin scroll horizontal
- **Y** el menú de navegación muestra un icono de hamburguesa o menú colapsado

---

## US-9.2 — Menú de navegación público

**Como** visitante de la landing,
**quiero** una barra de navegación con acceso a todas las secciones de la web,
**para que** pueda encontrar fácilmente la información que busco.

**Archivos afectados:**
- `frontend/src/components/NavbarPublic.jsx` — nuevo componente
- `frontend/src/styles/NavbarPublic.css` — nuevo archivo de estilos

**Estructura del menú:**
```
[Logo] AFA Lluís Braille    AFA ▾   ASOCIATE   COMISIONES   COLEGIO   AVISOS   NOTICIAS    [Acceder]
                               └─ Qué es
                               └─ Quiénes somos
```

**Contexto técnico:**
El `NavbarPublic` es independiente del `Navigation.jsx` actual (que es el navbar del panel privado). Ambos coexisten: el público en la landing, el privado en el panel de administración. El botón "ASOCIATE" tiene tratamiento visual especial (CTA). El botón "Acceder" lleva a `/login`.

---

### Criterios de aceptación

**CA-9.2.1 — El menú muestra todos los items de navegación**
- **Dado** que se renderiza el `NavbarPublic`
- **Entonces** son visibles los siguientes elementos de navegación:
  - `AFA` (con indicador de dropdown)
  - `ASOCIATE` (botón CTA destacado)
  - `COMISIONES`
  - `COLEGIO`
  - `AVISOS`
  - `NOTICIAS`
  - `Acceder` (botón secundario)

**CA-9.2.2 — El dropdown de AFA funciona correctamente**
- **Dado** que el usuario hace clic (o hover) en `AFA`
- **Cuando** el dropdown se despliega
- **Entonces** aparecen los ítems: `Qué es` y `Quiénes somos`
- **Y** al hacer clic en cualquiera de ellos, navega a la sección correspondiente de la página (ancla o ruta)
- **Y** el dropdown se cierra al hacer clic fuera de él

**CA-9.2.3 — El botón ASOCIATE es visualmente destacado**
- **Dado** que se renderiza la navbar
- **Entonces** el botón `ASOCIATE` tiene color de fondo `#f7b453` (o la variable CSS equivalente)
- **Y** el texto es en mayúsculas, fuente Montserrat bold
- **Y** es visualmente distinguible del resto de ítems del menú

**CA-9.2.4 — Los enlaces de menú navegan correctamente**
- **Dado** que el usuario hace clic en `COMISIONES`
- **Entonces** la página hace scroll hasta la sección `#comisiones` o navega a `/comisiones`
- **Y** lo mismo aplica para `COLEGIO` → `#colegio`, `AVISOS` → `#avisos`, `NOTICIAS` → `#noticias`

**CA-9.2.5 — El botón "Acceder" lleva al login**
- **Dado** que el usuario hace clic en `Acceder`
- **Entonces** navega a `/login`

**CA-9.2.6 — El logo y nombre de la asociación están presentes**
- **Dado** que se renderiza la navbar
- **Entonces** el nombre `AFA Lluís Braille` es visible en el área izquierda de la navbar
- **Y** si hay logo disponible, se muestra junto al nombre

**CA-9.2.7 — La navbar es sticky en scroll**
- **Dado** que el usuario hace scroll hacia abajo en la landing
- **Entonces** la navbar permanece visible en la parte superior de la pantalla (position sticky o fixed)

**CA-9.2.8 — En móvil el menú es accesible**
- **Dado** que se accede desde un viewport < 768px
- **Entonces** los ítems del menú están colapsados y accesibles mediante un botón de menú (hamburguesa)
- **Y** al hacer clic en el botón de hamburguesa, el menú se despliega mostrando todos los ítems

---

## US-9.3 — Hero section de la landing

**Como** visitante que llega por primera vez a `afalluisbraille.org`,
**quiero** ver una sección de bienvenida clara con el nombre de la asociación y su propósito principal,
**para que** entienda inmediatamente de qué trata la web.

**Archivos afectados:**
- `frontend/src/components/HeroSection.jsx` — nuevo componente
- `frontend/src/styles/HeroSection.css` — nuevo archivo de estilos

**Contexto técnico:**
El hero es la primera sección visible de la landing (justo bajo la navbar). Incluye: nombre de la asociación, tagline descriptivo, imagen o gradiente de fondo en la paleta corporativa, y al menos un botón de acción principal. El diseño sigue el patrón de afavivers.org: imagen de fondo con overlay, texto centrado o alineado a la izquierda.

---

### Criterios de aceptación

**CA-9.3.1 — El hero muestra el nombre de la asociación como heading principal**
- **Dado** que se renderiza la landing
- **Entonces** existe un `<h1>` con el texto `AFA Lluís Braille` (o contenido equivalente) visible en la sección hero
- **Y** el `<h1>` es el único h1 de la página

**CA-9.3.2 — El hero tiene un tagline o descripción breve**
- **Dado** que se renderiza la sección hero
- **Entonces** hay un párrafo o subtítulo visible que describe brevemente qué es la AFA (ej. "Asociación de Familias del Alumnado del colegio Lluís Braille")

**CA-9.3.3 — El hero incluye al menos un CTA visible**
- **Dado** que se renderiza la sección hero
- **Entonces** hay al menos un botón o enlace de acción visible (ej. `ASÓCIATE`, `Saber más`, o `Ver noticias`)
- **Y** el CTA principal tiene color de acento `#f7b453`

**CA-9.3.4 — El hero ocupa al menos el 60% del viewport inicial**
- **Dado** que se accede a la landing desde un escritorio (1280px)
- **Entonces** la sección hero ocupa al menos el 60% de la altura visible del navegador (min-height: 60vh)

**CA-9.3.5 — El contraste de texto sobre fondo es suficiente**
- **Dado** que el hero tiene imagen de fondo o color oscuro
- **Entonces** el texto del heading y tagline tiene contraste suficiente para lectura (ratio ≥ 4.5:1 WCAG AA)

---

## US-9.4 — Footer de la landing

**Como** visitante que ha llegado al final de la landing,
**quiero** encontrar información de contacto y enlaces legales,
**para que** pueda contactar con la asociación o consultar la política de privacidad.

**Archivos afectados:**
- `frontend/src/components/Footer.jsx` — nuevo componente
- `frontend/src/styles/Footer.css` — nuevo archivo de estilos

**Contexto técnico:**
El footer es compartido por todas las páginas públicas. Contiene: datos de contacto de la asociación, enlace a redes sociales (Instagram u otras), política de privacidad, y copyright. Referencia: footer de afavivers.org con fondo oscuro y texto claro.

---

### Criterios de aceptación

**CA-9.4.1 — El footer muestra el email de contacto**
- **Dado** que se renderiza el footer
- **Entonces** hay un enlace `mailto:` con el email de la asociación visible

**CA-9.4.2 — El footer muestra la dirección del colegio**
- **Dado** que se renderiza el footer
- **Entonces** hay texto con la dirección del colegio Lluís Braille

**CA-9.4.3 — El footer incluye enlace a política de privacidad**
- **Dado** que se renderiza el footer
- **Entonces** hay un enlace con texto `Política de privacidad` que navega a `/privacidad` o a una sección o documento correspondiente

**CA-9.4.4 — El footer incluye el copyright con el año actual**
- **Dado** que se renderiza el footer
- **Entonces** hay texto de copyright con el formato `AFA Lluís Braille © [año actual]`

**CA-9.4.5 — Los enlaces de redes sociales abren en nueva pestaña**
- **Dado** que hay iconos o enlaces a redes sociales (Instagram u otras)
- **Cuando** el usuario hace clic
- **Entonces** se abren en nueva pestaña (`target="_blank"` con `rel="noopener noreferrer"`)

**CA-9.4.6 — El footer es accesible semánticamente**
- **Dado** que se renderiza el footer
- **Entonces** está envuelto en la etiqueta `<footer>` y los enlaces tienen textos descriptivos (no solo iconos sin label)

---

## Orden de implementación sugerido

```
US-9.1 (ruta pública base + LandingPage.jsx)
  → sin dependencias internas

US-9.2 (navbar público)
  → requiere que exista LandingPage.jsx (US-9.1)

US-9.3 (hero section)
  → requiere LandingPage.jsx (US-9.1)

US-9.4 (footer)
  → puede hacerse en paralelo con US-9.2 y US-9.3
```

## Definición de hecho (DoD) compartida para todas las US de este epic

- El código compila sin errores ni warnings nuevos
- La landing es accesible en `http://localhost:5173/` sin token
- El panel privado sigue requiriendo autenticación
- Los componentes nuevos tienen al menos un test de renderizado con Vitest + RTL
- Ningún componente supera 150 líneas (separar en subcomponentes si es necesario)
- La paleta de colores respeta: `#8b935c` (verde primario), `#f7b453` (acento CTA)
- Los estilos usan variables CSS definidas en `index.css` (no valores hardcodeados en componentes)
- La landing se visualiza correctamente en mobile (375px) y desktop (1280px)
