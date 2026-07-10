Coloca aqui el archivo de fuente FontAwesome Free Solid en formato OTF con el nombre exacto:

    fa-solid-900.otf

Puedes descargarlo gratuitamente desde la web oficial de Font Awesome (paquete "Free for Desktop",
carpeta otfs/ -> Font Awesome 6 Free-Solid-900.otf), o desde
https://github.com/FortAwesome/Font-Awesome/releases/latest
Solo renombra el archivo a fa-solid-900.otf si el zip lo trae con otro nombre.

Al colocarlo en esta carpeta la app funciona 100% offline y los iconos se renderizan dentro de
los botones. Mientras el archivo no este presente, la app compila y se ejecuta con normalidad,
pero los iconos se muestran vacios (texto en blanco).

Los nombres de icono usados en kits.json y en el selector visual (buscador de iconos)
estan definidos en:

    app/src/main/java/com/dsk/soniloko/data/FontAwesomeIcons.kt

Si tu version de FontAwesome difiere y algun glifo no coincide, ajusta el codepoint hexadecimal
correspondiente en ese fichero segun el cheatsheet oficial de tu version.
