package com.dsk.soniloko.data

/**
 * Curated Font Awesome 6 Free "Solid" codepoints, stored as plain hex and converted to the
 * actual glyph character at runtime to keep the source file plain ASCII. Each icon carries a
 * few Spanish keywords as aliases so the picker's search works in both English and Spanish.
 *
 * This is a large curated subset (~140 icons), not the full ~1500-icon FA library: hand-typing
 * that many codepoints from memory risks silent wrong-glyph errors, and this app has no network
 * access at build time to pull the authoritative FA metadata. Adding more icons is a one-line
 * addition below (icon name + hex from the official cheatsheet + optional Spanish aliases).
 * If the ttf/otf placed in assets/fonts differs in version and a glyph doesn't match, adjust
 * the hex value here against that version's cheatsheet.
 */
object FontAwesomeIcons {
    data class Icon(val name: String, val codepoint: String, val aliases: List<String> = emptyList())

    private fun cp(hex: String): String = hex.toInt(16).toChar().toString()

    private fun icon(name: String, hex: String, vararg aliases: String) =
        Icon(name, cp(hex), aliases.toList())

    val all: List<Icon> = listOf(
        // Animals
        icon("cat", "f6be", "gato"),
        icon("dog", "f6d3", "perro"),
        icon("horse", "f6f0", "caballo"),
        icon("fish", "f578", "pez"),
        icon("dove", "f4ba", "paloma"),
        icon("spider", "f717", "arana"),
        icon("frog", "f52e", "rana"),
        icon("kiwi-bird", "f535", "kiwi", "pajaro"),
        icon("crow", "f520", "cuervo"),
        icon("hippo", "f6ed", "hipopotamo"),
        icon("otter", "f700", "nutria"),
        icon("dragon", "f6d5", "dragon"),
        icon("feather", "f52d", "pluma"),
        icon("paw", "f1b0", "pata", "huella"),
        icon("bug", "f188", "bicho", "insecto"),

        // Nature / weather
        icon("sun", "f185", "sol"),
        icon("moon", "f186", "luna"),
        icon("cloud", "f0c2", "nube"),
        icon("bolt", "f0e7", "rayo"),
        icon("snowflake", "f2dc", "copo de nieve", "nieve"),
        icon("wind", "f72e", "viento"),
        icon("rainbow", "f75b", "arcoiris"),
        icon("fire", "f06d", "fuego"),
        icon("tint", "f043", "agua", "gota"),
        icon("umbrella", "f0e9", "paraguas"),
        icon("tree", "f1bb", "arbol"),
        icon("leaf", "f06c", "hoja"),
        icon("seedling", "f4d8", "planta", "semilla"),

        // Music / sound
        icon("music", "f001", "musica"),
        icon("microphone", "f130", "microfono"),
        icon("microphone-alt", "f3c9", "microfono"),
        icon("volume-up", "f028", "volumen", "sonido"),
        icon("volume-down", "f027", "volumen bajo"),
        icon("volume-mute", "f6a9", "silencio", "mute"),
        icon("drum", "f569", "tambor", "bateria"),
        icon("guitar", "f7a6", "guitarra"),
        icon("headphones", "f025", "auriculares", "cascos"),
        icon("bell", "f0f3", "campana", "timbre"),
        icon("bullhorn", "f0a1", "megafono", "altavoz"),
        icon("broadcast-tower", "f519", "antena", "radio"),

        // Emotions
        icon("laugh", "f599", "risa", "reirse"),
        icon("smile", "f118", "sonrisa", "feliz"),
        icon("meh", "f11a", "indiferente"),
        icon("frown", "f119", "triste"),

        // Effects / mood
        icon("bomb", "f1e2", "bomba", "explosion"),
        icon("skull", "f54c", "calavera"),
        icon("skull-crossbones", "f714", "pirata", "veneno"),
        icon("ghost", "f6e2", "fantasma"),
        icon("radiation", "f7b9", "radiacion"),
        icon("biohazard", "f780", "biopeligro"),
        icon("magic", "f0d0", "magia", "varita"),

        // Transport
        icon("car", "f1b9", "coche", "carro"),
        icon("taxi", "f1ba", "taxi"),
        icon("truck", "f0d1", "camion"),
        icon("truck-monster", "f63b", "camion monstruo"),
        icon("motorcycle", "f21c", "moto"),
        icon("bicycle", "f206", "bicicleta", "bici"),
        icon("rocket", "f135", "cohete"),
        icon("plane", "f072", "avion"),
        icon("ship", "f21a", "barco"),
        icon("train", "f238", "tren"),
        icon("subway", "f239", "metro"),
        icon("helicopter", "f533", "helicoptero"),
        icon("tractor", "f722", "tractor"),
        icon("wheelchair", "f193", "silla de ruedas"),

        // Games / sports
        icon("futbol", "f1e3", "futbol", "balon", "pelota"),
        icon("gamepad", "f11b", "mando", "videojuego"),
        icon("dice", "f522", "dado"),
        icon("chess", "f439", "ajedrez"),
        icon("trophy", "f091", "trofeo", "copa"),
        icon("medal", "f5a2", "medalla"),

        // Food / drink
        icon("coffee", "f0f4", "cafe"),
        icon("pizza-slice", "f818", "pizza"),
        icon("hamburger", "f805", "hamburguesa"),
        icon("birthday-cake", "f1fd", "tarta", "cumpleanos"),
        icon("cocktail", "f561", "coctel"),
        icon("beer", "f0fc", "cerveza"),
        icon("wine-glass", "f4e3", "vino", "copa de vino"),
        icon("carrot", "f787", "zanahoria"),
        icon("utensils", "f2e7", "cubiertos", "comida"),

        // Tech
        icon("robot", "f544", "robot"),
        icon("laptop", "f109", "portatil", "ordenador"),
        icon("mobile-alt", "f3cd", "movil", "telefono"),
        icon("keyboard", "f11c", "teclado"),
        icon("camera", "f030", "camara"),
        icon("camera-retro", "f083", "camara retro"),
        icon("video", "f03d", "video"),
        icon("wifi", "f1eb", "wifi"),
        icon("satellite", "f7bf", "satelite"),
        icon("satellite-dish", "f7c0", "antena parabolica"),
        icon("microchip", "f2db", "chip"),
        icon("plug", "f1e6", "enchufe"),
        icon("battery-full", "f240", "bateria"),

        // Symbols
        icon("heart", "f004", "corazon"),
        icon("star", "f005", "estrella"),
        icon("crown", "f521", "corona"),
        icon("gem", "f3a5", "gema", "diamante"),
        icon("gift", "f06b", "regalo"),
        icon("flag", "f024", "bandera"),
        icon("lightbulb", "f0eb", "bombilla", "idea"),
        icon("key", "f084", "llave"),
        icon("lock", "f023", "candado", "bloqueo"),
        icon("unlock", "f09c", "desbloquear", "abierto"),
        icon("anchor", "f13d", "ancla"),
        icon("compass", "f14e", "brujula"),
        icon("clock", "f017", "reloj"),
        icon("hourglass", "f254", "reloj de arena"),
        icon("puzzle-piece", "f12e", "puzzle", "rompecabezas"),
        icon("shield-alt", "f3ed", "escudo"),
        icon("balance-scale", "f24e", "balanza"),
        icon("infinity", "f534", "infinito"),
        icon("recycle", "f1b8", "reciclar"),
        icon("thumbs-up", "f164", "pulgar arriba", "like"),
        icon("thumbs-down", "f165", "pulgar abajo", "dislike"),

        // People
        icon("child", "f1ae", "nino"),
        icon("baby", "f77c", "bebe"),
        icon("walking", "f554", "caminar", "andar"),
        icon("running", "f70c", "correr"),

        // Household
        icon("couch", "f4b8", "sofa"),
        icon("bed", "f236", "cama"),
        icon("bath", "f2cd", "banera", "baño"),

        // Misc common
        icon("phone", "f095", "telefono"),
        icon("envelope", "f0e0", "correo", "sobre"),
        icon("map-marker-alt", "f3c5", "ubicacion", "mapa"),
        icon("home", "f015", "casa", "hogar"),
        icon("building", "f1ad", "edificio"),
        icon("industry", "f275", "fabrica"),
        icon("wrench", "f0ad", "llave inglesa", "herramienta"),
        icon("hammer", "f6e3", "martillo"),
        icon("paint-brush", "f1fc", "pincel", "pintura"),
        icon("palette", "f53f", "paleta"),
        icon("theater-masks", "f630", "teatro", "mascaras"),
        icon("film", "f008", "pelicula", "cine"),
        icon("book", "f02d", "libro"),
        icon("graduation-cap", "f19d", "graduacion"),
        icon("briefcase", "f0b1", "maletin", "trabajo"),
        icon("globe", "f0ac", "mundo", "globo"),
        icon("map", "f279", "mapa")
    )

    private val byNameMap = all.associateBy { it.name }

    fun byName(name: String): Icon? = byNameMap[name]

    /** Matches against the icon's English name and its Spanish/English aliases. */
    fun search(query: String): List<Icon> {
        val q = query.trim()
        if (q.isBlank()) return all
        return all.filter { icon ->
            icon.name.contains(q, ignoreCase = true) || icon.aliases.any { it.contains(q, ignoreCase = true) }
        }
    }
}
