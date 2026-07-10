# Manual — cómo añadir kits y sonidos iniciales (versión web)

Esta carpeta `web/` es autocontenida: contiene sus propias copias de `sounds/`, `fonts/`
y `kits.json`, así que para desplegarla solo tienes que subir **toda la carpeta `web/`**
a tu servidor (no hace falta nada de fuera de esta carpeta).

```
web/
  index.html
  manifest.json
  sw.js
  manual.md          ← este archivo
  kits.json           ← kits de fábrica (edítalo para añadir/cambiar los tuyos)
  css/style.css
  js/{app,i18n,fa-icons}.js
  icons/{icon,favicon}.svg
  fonts/fa-solid-900.otf
  sounds/*.mp3
```

## Añadir o editar los kits iniciales

Edita `kits.json`. Es el mismo archivo que la app nativa usa como plantilla, con este formato:

```json
{
  "kits": [
    {
      "id": "classic",
      "name": { "es": "Clásico", "en": "Classic" },
      "buttons": [
        { "id": 1, "icon": "drum", "sound": "sound_01_drum.mp3", "volume": 1.0 },
        { "id": 2, "icon": "bomb", "sound": "sound_02_explosion.mp3", "volume": 1.0 }
      ]
    }
  ]
}
```

- Cada kit necesita un `id` único (solo letras/números/guiones), un `name` con `es`/`en`,
  y exactamente 12 `buttons` (id del 1 al 12).
- `icon`: el nombre de un icono Font Awesome de la lista curada en `js/fa-icons.js`
  (por ejemplo `"cat"`, `"fire"`, `"star"`...). Abre ese archivo para ver el listado
  completo de nombres válidos — si pones un nombre que no existe ahí, el botón se
  queda sin icono.
- `sound`: el nombre de archivo tal cual está dentro de `sounds/` (ver siguiente sección).
- `volume`: de `0.0` a `1.0`.
- Puedes añadir tantos kits como quieras dentro del array `kits`; todos aparecerán
  en el menú de kits de la app, en el mismo orden en que estén en el archivo.

## Dónde poner los sonidos

Copia tus archivos de audio dentro de `sounds/`. Formato recomendado: **MP3** (máxima
compatibilidad entre navegadores). Luego referencia el nombre exacto del archivo
(con extensión) en el campo `sound` de `kits.json`.

Los sonidos que los propios usuarios graban o importan desde la app **no** se guardan
aquí — se quedan en el almacenamiento local del navegador (`localStorage`), no en el
servidor.

## Iconos

El listado completo de iconos disponibles (nombre + alias en español para el buscador)
está en `js/fa-icons.js`. Añadir un icono nuevo es una línea: copia el patrón
`["nombre", "hexcodepoint", ["alias1", "alias2"]]` usando el código hexadecimal del
glifo de Font Awesome 6 Free Solid.
