import os
import json
import random
import unicodedata

def quitar_tildes(cadena):
    """Elimina tildes, diéresis y pasa a minúsculas para facilitar la comparación interna."""
    nfkd_form = unicodedata.normalize('NFKD', cadena)
    return "".join([c for c in nfkd_form if not unicodedata.combining(c)]).lower()

# Diccionario depurado estrictamente para Font Awesome 6 Free Solid
ICON_MAP = {
    # Memes específicos y videojuegos de la imagen
    'amongus': 'user-secret', 'sus': 'user-astronaut',
    'bob_esponja': 'soap', 'patricio': 'star',
    'fbi_open_door': 'door-open', 'fbi': 'building-shield',
    'gta_wasted': 'skull', 'gta_music': 'car', 'gta': 'star',
    'jurassic_park': 'dragon', 'dinosaurio': 'dragon', 'trex': 'dragon',
    'mario_moneda': 'coins', 'mario_salto': 'arrow-up', 'mario': 'gamepad',
    'roblox_muerte': 'skull', 'roblox': 'cube', 'oof': 'skull',
    'chanchanchan': 'masks-theater', 'dramatico': 'bolt',
    'creditos_finales': 'clapperboard', 'creditos': 'film',
    'flauta_mal_titanic': 'ship', 'flauta_mal': 'music', 'titanic': 'ship',
    'disco_rayado': 'compact-disc', 'rebobinar': 'backward-fast',
    
    # Expresiones y gritos
    'aaaa': 'face-surprise', 'ahh': 'face-surprise',
    'grito': 'face-dizzy', 'bra': 'face-dizzy', 'bruh': 'face-dizzy',
    'guuauu': 'face-surprise', 'wow': 'face-surprise',
    'sorpresa': 'exclamation', 'tension': 'heart-pulse',
    
    # Animales
    'grito_cabra': 'sheep', 'cabra': 'sheep',
    'risa_gato': 'cat', 'gato': 'cat', 'miau': 'cat',
    'pato': 'crow', 'cuervo': 'crow', 'gallo': 'crow',
    'tortuga_gimiendo': 'face-dizzy', 'tortuga': 'leaf',
    'grillos': 'bug', 'insecto': 'bug',
    
    # Percusión, música e instrumentos
    'tambor_comedia': 'drum', 'tambor_redoble': 'drum', 'tambor': 'drum', 'bateria': 'drum',
    'cancion_triste': 'face-sad-tear', 'cancion': 'music', 'musica': 'music',
    
    # Golpes y armas
    'bofetada': 'hand-sparkles', 'golpe': 'hand-fist', 'punetazo': 'hand-fist',
    'pistola_cargar': 'gun', 'pistola_disparo': 'gun', 'pistola': 'gun', 'disparo': 'gun',
    
    # Vehículos y alarmas
    'bocina': 'bullhorn', 'sirena': 'truck-medical', 'alarma': 'bell',
    
    # Reacciones y escatología
    'pedo': 'poop', 'erupto': 'face-dizzy', 'vomito': 'face-dizzy',
    'risa': 'face-laugh-squint', 'lloro': 'face-sad-cry',
    
    # Efectos de UI y dibujos animados
    'error': 'circle-xmark', 'fallo_comico': 'xmark', 'fallo': 'xmark',
    'cartoon_up': 'arrow-up', 'cartoon': 'tv',
    'campana': 'bell', 'timbre': 'bell',
    'trofeo': 'trophy', 'victoria': 'medal', 'acierto': 'circle-check'
}

FALLBACK_ICONS = [
    'music', 'play', 'wave-square', 'volume-high', 'circle-play', 
    'record-vinyl', 'headphones', 'compact-disc', 'file-audio', 
    'radio', 'microphone', 'sliders', 'tower-broadcast'
]

def obtener_icono_por_nombre(filename):
    nombre_sin_ext = os.path.splitext(filename)[0]
    nombre_limpio = quitar_tildes(nombre_sin_ext)
    
    claves_ordenadas = sorted(ICON_MAP.keys(), key=len, reverse=True)
    
    for palabra_clave in claves_ordenadas:
        if palabra_clave in nombre_limpio:
            return ICON_MAP[palabra_clave]
            
    return random.choice(FALLBACK_ICONS)

def main():
    archivos_mp3 = [f for f in os.listdir('.') if f.lower().endswith('.mp3')]
    archivos_mp3.sort()
    
    archivo_prioritario = "tambor_comedia.mp3"
    if archivo_prioritario in archivos_mp3:
        archivos_mp3.remove(archivo_prioritario)
        archivos_mp3.insert(0, archivo_prioritario)
        
    kits = []
    tamano_kit = 12
    
    for i in range(0, len(archivos_mp3), tamano_kit):
        chunk = archivos_mp3[i:i + tamano_kit]
        numero_kit = (i // tamano_kit) + 1
        
        kit_id = f"memes_{numero_kit:02d}"
        kit_name = f"Memes {numero_kit:02d}"
        
        botones = []
        for index_boton, filename in enumerate(chunk):
            botones.append({
                "id": index_boton + 1,
                "icon": obtener_icono_por_nombre(filename),
                "sound": filename,
                "volume": 1.0
            })
            
        kits.append({
            "id": kit_id,
            "name": { "es": kit_name, "en": kit_name },
            "buttons": botones
        })
        
    json_output = '{\n  "kits": [\n'
    
    for k_idx, kit in enumerate(kits):
        json_output += '    {\n'
        json_output += f'      "id": "{kit["id"]}",\n'
        json_output += f'      "name": {{ "es": "{kit["name"]["es"]}", "en": "{kit["name"]["en"]}" }},\n'
        json_output += '      "buttons": [\n'
        
        for b_idx, btn in enumerate(kit["buttons"]):
            btn_str = json.dumps(btn, ensure_ascii=False)
            coma_btn = ',' if b_idx < len(kit["buttons"]) - 1 else ''
            json_output += f'        {btn_str}{coma_btn}\n'
            
        json_output += '      ]\n'
        coma_kit = ',' if k_idx < len(kits) - 1 else ''
        json_output += f'    }}{coma_kit}\n'
        
    json_output += '  ]\n}\n'
    
    with open('kits.json', 'w', encoding='utf-8') as f:
        f.write(json_output)
        
    print(f"✅ Completado. Se ha creado 'kits.json' con {len(archivos_mp3)} sonidos.")

if __name__ == "__main__":
    main()