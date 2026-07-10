// Bilingual strings for the web version, mirroring the native app's strings.xml where relevant.
const I18N = {
  es: {
    settings: "Configuración",
    language: "Idioma",
    system_default: "Predeterminado del sistema",
    master_volume: "Volumen general",
    theme: "Tema",
    preview_sounds_setting: "Vista previa de sonidos",
    preview_sounds_setting_hint: "Reproduce el sonido al seleccionarlo en la lista.",
    haptic_feedback_setting: "Vibración al tocar",
    haptic_feedback_setting_hint: "Pequeña vibración al pulsar un botón (si el dispositivo lo soporta).",
    allow_long_sounds: "Permitir sonidos largos",
    allow_long_sounds_hint: "Si está desactivado, se cortan a la duración máxima.",
    max_sound_duration: "Duración máxima",
    custom_duration: "Personalizado",
    allow_simultaneous_sounds: "Sonidos simultáneos",
    allow_simultaneous_sounds_hint: "Permite superponer varios sonidos a la vez.",
    install_app: "Instalar aplicación",
    export: "Exportar",
    import_: "Importar",
    factory_reset: "Restablecer valores de fábrica",
    reset_confirm_title: "¿Estás seguro?",
    reset_confirm_message: "Esto borrará toda tu personalización y ajustes, volviendo a los valores de fábrica.",
    yes: "Sí",
    cancel: "Cancelar",
    save: "Guardar",
    close: "Cerrar",
    edit_button_title: "Editar botón",
    choose_icon: "Elegir icono",
    choose_image: "Elegir imagen",
    remove_image: "Quitar imagen",
    button_text: "Texto personalizado (opcional)",
    sound: "Sonido",
    volume: "Volumen",
    search_icons: "Buscar icono…",
    search_sounds: "Buscar sonido…",
    record_sound_option: "Grabar sonido…",
    import_sound_from_device: "Importar sonido desde el dispositivo…",
    record_sound_title: "Grabar sonido",
    record: "Grabar",
    stop_recording: "Detener",
    play_recording: "Reproducir",
    record_again: "Grabar de nuevo",
    sound_name: "Nombre del sonido",
    delete: "Eliminar",
    delete_sound_confirm_title: "¿Eliminar este sonido?",
    delete_sound_confirm_message: "Se quitará de tus sonidos propios.",
    save_current_kit: "Guardar kit actual",
    kit_name: "Nombre del kit",
    manage_kits_title: "Gestionar mis kits",
    no_custom_kits: "Aún no has guardado ningún kit propio.",
    delete_kit_confirm_title: "¿Eliminar este kit?",
    delete_kit_confirm_message: "Esta acción no se puede deshacer.",
    rename: "Renombrar",
    made_with: "Hecho con",
    by: "por",
    rotate_device_message: "Gira tu dispositivo: SoniLoko solo funciona en vertical.",
    help_title: "Acerca de SoniLoko",
    help_body: "<p>• Toca un botón para reproducir su sonido.</p>" +
      "<p>• Pulsa el icono de llave, o mantén pulsado un botón, para entrar en modo edición y cambiar su icono, imagen, texto, sonido y volumen.</p>" +
      "<p>• Puedes usar un icono Font Awesome, subir tu propia imagen, o escribir un texto personalizado.</p>" +
      "<p>• Al elegir el sonido de un botón puedes grabarlo con el micrófono, importarlo desde tu dispositivo, o usar uno de los ya incluidos.</p>" +
      "<p>• Los sonidos grabados o importados y las imágenes se guardan en este navegador (no se suben a ningún servidor). Ten en cuenta que el espacio de almacenamiento del navegador es limitado.</p>" +
      "<p>• El icono de música lista los kits de fábrica y tus propios kits guardados.</p>" +
      "<p>• Instala SoniLoko como aplicación desde Configuración para usarla a pantalla completa y sin conexión.</p>" +
      "<p>• Exporta o importa toda tu configuración como archivo .json desde Configuración.</p>" +
      "<p>• Esta versión web es una adaptación simplificada de la app: no incluye el modo juego, el efecto SoundBox Fx ni el recorte de grabaciones.</p>"
  },
  en: {
    settings: "Settings",
    language: "Language",
    system_default: "System default",
    master_volume: "Master volume",
    theme: "Theme",
    preview_sounds_setting: "Sound preview",
    preview_sounds_setting_hint: "Plays the sound when you select it in the list.",
    haptic_feedback_setting: "Vibrate on tap",
    haptic_feedback_setting_hint: "A short vibration on tap (if the device supports it).",
    allow_long_sounds: "Allow long sounds",
    allow_long_sounds_hint: "When off, sounds are cut off at the maximum duration.",
    max_sound_duration: "Maximum duration",
    custom_duration: "Custom",
    allow_simultaneous_sounds: "Simultaneous sounds",
    allow_simultaneous_sounds_hint: "Lets multiple sounds overlap at once.",
    install_app: "Install app",
    export: "Export",
    import_: "Import",
    factory_reset: "Factory reset",
    reset_confirm_title: "Are you sure?",
    reset_confirm_message: "This will erase all your customization and settings, returning to factory defaults.",
    yes: "Yes",
    cancel: "Cancel",
    save: "Save",
    close: "Close",
    edit_button_title: "Edit button",
    choose_icon: "Choose icon",
    choose_image: "Choose image",
    remove_image: "Remove image",
    button_text: "Custom text (optional)",
    sound: "Sound",
    volume: "Volume",
    search_icons: "Search icon…",
    search_sounds: "Search sound…",
    record_sound_option: "Record sound…",
    import_sound_from_device: "Import sound from device…",
    record_sound_title: "Record sound",
    record: "Record",
    stop_recording: "Stop",
    play_recording: "Play",
    record_again: "Record again",
    sound_name: "Sound name",
    delete: "Delete",
    delete_sound_confirm_title: "Delete this sound?",
    delete_sound_confirm_message: "This will remove it from your own sounds.",
    save_current_kit: "Save current kit",
    kit_name: "Kit name",
    manage_kits_title: "Manage my kits",
    no_custom_kits: "You haven't saved any custom kits yet.",
    delete_kit_confirm_title: "Delete this kit?",
    delete_kit_confirm_message: "This action can't be undone.",
    rename: "Rename",
    made_with: "Made with",
    by: "by",
    rotate_device_message: "Rotate your device: SoniLoko only works in portrait.",
    help_title: "About SoniLoko",
    help_body: "<p>• Tap a button to play its sound.</p>" +
      "<p>• Tap the wrench icon, or long-press a button, to enter edit mode and change its icon, image, text, sound and volume.</p>" +
      "<p>• You can use a Font Awesome icon, upload your own image, or type custom text.</p>" +
      "<p>• When picking a button's sound you can record it with the microphone, import it from your device, or use one of the built-in ones.</p>" +
      "<p>• Recorded/imported sounds and images are saved in this browser (never uploaded anywhere). Browser storage space is limited.</p>" +
      "<p>• The music icon lists the built-in kits and your own saved kits.</p>" +
      "<p>• Install SoniLoko as an app from Settings to use it full-screen and offline.</p>" +
      "<p>• Export or import your whole configuration as a .json file from Settings.</p>" +
      "<p>• This web version is a simplified port of the app: it doesn't include game mode, the SoundBox Fx effect, or recording trim.</p>"
  }
};

function resolvedLang() {
  const pref = (typeof STATE !== "undefined" && STATE.settings && STATE.settings.language) || "system";
  if (pref === "es" || pref === "en") return pref;
  return (navigator.language || "es").slice(0, 2) === "en" ? "en" : "es";
}

function t(key) {
  const dict = I18N[resolvedLang()] || I18N.es;
  return dict[key] || I18N.es[key] || key;
}
