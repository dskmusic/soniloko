Esta carpeta contiene los sonidos "de fabrica" usados por los kits definidos en assets/kits.json
(el kit "classic" tambien hace de tablero por defecto en el primer arranque y en el restablecimiento
de fabrica).

Los 12 archivos .mp3 de esta carpeta son PLACEHOLDERS VACIOS. Sustituyelos por clips de audio
reales (cortos, idealmente <2s) manteniendo EXACTAMENTE el mismo nombre de archivo, para que
kits.json siga apuntando a ellos correctamente.

Por defecto, los sonidos empaquetados en assets deben ser .mp3 (formato mas compatible y estandar).
Formatos soportados por la app (tanto en assets como al importar desde el dispositivo):
.mp3, .wav, .ogg, .opus, .m4a

Nombres esperados:
    sound_01_drum.mp3
    sound_02_explosion.mp3
    sound_03_honk.mp3
    sound_04_meow.mp3
    sound_05_bark.mp3
    sound_06_click.mp3
    sound_07_woosh.mp3
    sound_08_spooky.mp3
    sound_09_laugh.mp3
    sound_10_launch.mp3
    sound_11_sparkle.mp3
    sound_12_tada.mp3

Ademas de estos sonidos empaquetados, la app permite importar sonidos propios directamente desde
el almacenamiento del dispositivo (boton "Importar sonido desde el dispositivo" en el selector de
sonido de cada boton). Los sonidos importados se copian de forma privada dentro de la app y quedan
disponibles para cualquier boton, sin necesidad de recompilar el proyecto.
