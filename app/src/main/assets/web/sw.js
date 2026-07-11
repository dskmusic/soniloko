const CACHE_NAME = "soniloko-v3";
const SOUND_FILES = [
  "aaaaaahhhhh.mp3", "ahh.mp3", "amongus.mp3", "bob_esponja_error.mp3", "bocina.mp3",
  "bofetada.mp3", "bra.mp3", "cartoon_up.mp3", "chanchanchan_dramático.mp3",
  "créditos_finales.mp3", "disco_rayado.mp3", "error.mp3", "fallo_cómico.mp3",
  "fbi_open_door.mp3", "flauta_mal_titanic.mp3", "grillos.mp3", "grito_cabra.mp3",
  "gta_music.mp3", "gta_wasted.mp3", "guuauu.mp3", "jurassic_park1.mp3", "jurassic_park2.mp3",
  "mario_moneda.mp3", "mario_salto.mp3", "música_canción_triste.mp3", "música_expedienteX.mp3",
  "música_psicosis.mp3", "música_se_murió.mp3", "música_tensión.mp3", "música_tensión2.mp3",
  "música_tensión3.mp3", "música_tiburón.mp3", "música_y_se_marchó.mp3", "pato.mp3", "pedo.mp3",
  "pistola_cargar.mp3", "pistola_disparo.mp3", "rebobinar.mp3", "risa_gato.mp3",
  "roblox_muerte.mp3", "sorpresa.mp3", "sorpresa2.mp3", "tambor_comedia.mp3",
  "tambor_redoble.mp3", "tipu.mp3", "tortuga_gimiendo1.mp3", "tortuga_gimiendo2.mp3"
];
const CORE_ASSETS = [
  "./",
  "./index.html",
  "./css/style.css",
  "./js/i18n.js",
  "./js/fa-icons.js",
  "./js/app.js",
  "./manifest.json",
  "./icons/icon.svg",
  "./icons/favicon.svg",
  "./kits.json",
  "./fonts/fa-solid-900.otf",
  ...SOUND_FILES.map((f) => "./sounds/" + f)
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(CORE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// Cache-first, falling back to network and opportunistically caching new GET responses
// (covers user-imported/recorded content requested at runtime, e.g. re-fetched sound files).
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      return fetch(event.request)
        .then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        })
        .catch(() => cached);
    })
  );
});
