"use strict";

/* ---------------------------------------------------------------------------
 * SoniLoko web — a mobile-friendly, installable PWA port of the native app.
 * Vanilla JS, no build step, no dependencies. Simplifications vs. the native
 * app (documented in the Help dialog too): no game mode, no SoundBox Fx / gain
 * boost audio effects, no recording trim tool, no image cropping. Custom
 * sounds/images are stored as data URLs in localStorage (browser-local, never
 * uploaded) — that has a small storage ceiling (a few MB), fine for personal
 * use but not for dozens of long recordings.
 * ------------------------------------------------------------------------- */

const BUNDLED_SOUNDS = [
  "sound_01_drum.mp3", "sound_02_explosion.mp3", "sound_03_honk.mp3", "sound_04_meow.mp3",
  "sound_05_bark.mp3", "sound_06_click.mp3", "sound_07_woosh.mp3", "sound_08_spooky.mp3",
  "sound_09_laugh.mp3", "sound_10_launch.mp3", "sound_11_sparkle.mp3", "sound_12_tada.mp3"
];

const THEMES = [
  { id: "classic_red", primary: "#E53935", background: "#130404", surface: "#1E0808" },
  { id: "ocean_blue", primary: "#2196F3", background: "#04090F", surface: "#081420" },
  { id: "forest_green", primary: "#43A047", background: "#040A04", surface: "#081408" },
  { id: "midnight_black", primary: "#9E9E9E", background: "#060606", surface: "#141414" },
  { id: "sunset_orange", primary: "#FF8F00", background: "#140A02", surface: "#1F1206" },
  { id: "royal_purple", primary: "#8E24AA", background: "#0D0512", surface: "#190919" }
];

const DURATION_PRESETS_MS = [1000, 2000, 3000, 5000, 10000, 15000, 30000];
const LONG_PRESS_MS = 500;
const MIN_PRESS_VISIBLE_MS = 120;

const DEFAULT_SETTINGS = {
  language: "system",
  masterVolume: 1,
  theme: "classic_red",
  previewSoundsEnabled: true,
  allowLongSounds: true,
  maxSoundDurationMs: 5000,
  allowSimultaneousSounds: true,
  hapticFeedbackEnabled: true
};

const STATE = {
  settings: loadJson("soniloko_settings", DEFAULT_SETTINGS),
  board: loadJson("soniloko_board", null),
  currentKitId: loadJson("soniloko_current_kit_id", null),
  customKits: loadJson("soniloko_custom_kits", []),
  customSounds: loadJson("soniloko_custom_sounds", []),
  builtinKits: [],
  activeAudios: [],
  editMode: false,
  editingButtonId: null,
  editingDraft: null,
  iconPickerTarget: null,
  soundPickerTarget: null,
  recordBlob: null,
  deferredInstallPrompt: null
};

function uid() {
  return (crypto.randomUUID && crypto.randomUUID()) || (Date.now() + "-" + Math.random().toString(16).slice(2));
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function loadJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (e) {
    return fallback;
  }
}
function save(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}
function saveSettings() { save("soniloko_settings", STATE.settings); }
function saveBoard() {
  save("soniloko_board", STATE.board);
  // Tag this save to whichever kit is active (falling back to the default kit if the user
  // never explicitly switched yet) so switching away and back restores the customization
  // instead of reloading the kit's pristine template — mirrors BoardRepository.kt.
  const kitId = STATE.currentKitId || defaultKitId();
  if (kitId) {
    const overrides = loadJson("soniloko_kit_overrides", {});
    overrides[kitId] = STATE.board;
    save("soniloko_kit_overrides", overrides);
  }
}

function defaultKitId() {
  const classic = STATE.builtinKits.find((k) => k.id === "classic");
  if (classic) return classic.id;
  return STATE.builtinKits.length ? STATE.builtinKits[0].id : null;
}
function saveCustomKits() { save("soniloko_custom_kits", STATE.customKits); }
function saveCustomSounds() { save("soniloko_custom_sounds", STATE.customSounds); }

function blankBoardButton(id, icon, sound) {
  return { id, icon, sound, volume: 1, customImage: null, customText: null };
}

/* ------------------------------- Bootstrap ------------------------------- */

async function boot() {
  try {
    const res = await fetch("kits.json");
    const json = await res.json();
    STATE.builtinKits = json.kits;
  } catch (e) {
    STATE.builtinKits = [];
  }

  if (!STATE.board) {
    const classic = STATE.builtinKits.find((k) => k.id === "classic");
    STATE.board = classic
      ? classic.buttons.map((b) => blankBoardButton(b.id, b.icon, b.sound))
      : Array.from({ length: 12 }, (_, i) => blankBoardButton(i + 1, "star", BUNDLED_SOUNDS[i % BUNDLED_SOUNDS.length]));
    saveBoard();
  }

  applyTheme(STATE.settings.theme);
  applyStaticIcons(document);
  applyI18n();
  renderSpeakerDots();
  renderBoard();
  renderKitMenu();
  renderSettingsPanel();
  wireGlobalEvents();
  wireEditDialog();
  wireIconPicker();
  wireSoundPicker();
  wireRecordDialog();
  wireKitDialogs();
  wireHelpDialog();
  registerServiceWorker();
  setupInstallPrompt();
  attemptOrientationLock();
}

document.addEventListener("DOMContentLoaded", boot);

/* --------------------------------- Theme ---------------------------------- */

function applyTheme(themeId) {
  const theme = THEMES.find((t) => t.id === themeId) || THEMES[0];
  const root = document.documentElement.style;
  root.setProperty("--primary", theme.primary);
  root.setProperty("--background", theme.background);
  root.setProperty("--surface", theme.surface);
  document.querySelector('meta[name="theme-color"]').setAttribute("content", theme.background);
}

/* --------------------------------- i18n ----------------------------------- */

function applyI18n() {
  document.documentElement.lang = resolvedLang();
  document.querySelectorAll("[data-i18n]").forEach((el) => { el.textContent = t(el.getAttribute("data-i18n")); });
  document.querySelectorAll("[data-i18n-placeholder]").forEach((el) => { el.placeholder = t(el.getAttribute("data-i18n-placeholder")); });
}

/* ------------------------------ Speaker grille ----------------------------- */

function renderSpeakerDots() {
  const el = document.getElementById("speaker-dots");
  el.innerHTML = "";
  for (let i = 0; i < 14 * 6; i++) {
    const dot = document.createElement("div");
    dot.className = "speaker-dot";
    dot.style.animationDelay = (i % 14) * 0.05 + "s";
    el.appendChild(dot);
  }
}

function pulseSpeaker() {
  const speaker = document.getElementById("speaker");
  speaker.classList.add("pulse");
  setTimeout(() => speaker.classList.remove("pulse"), 350);
}

/* --------------------------------- Board ----------------------------------- */

function defaultTextFromSoundFile(fileName) {
  const dotIndex = fileName.lastIndexOf(".");
  const base = dotIndex === -1 ? fileName : fileName.slice(0, dotIndex);
  return base
    .replace(/_/g, " ")
    .split(" ")
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function soundDisplayName(name) {
  return defaultTextFromSoundFile(name);
}

function resolveSoundSrc(name) {
  const custom = STATE.customSounds.find((s) => s.name === name);
  if (custom) return custom.dataUrl;
  return "sounds/" + name;
}

let previewAudio = null;

function stopPreviewAudio() {
  if (previewAudio) {
    previewAudio.pause();
    previewAudio = null;
  }
}

/** Auditioning a sound in a picker should always stop whatever was previewing before —
 * unlike actual button taps, overlapping previews are never wanted. */
function playPreview(name) {
  stopPreviewAudio();
  const audio = new Audio(resolveSoundSrc(name));
  audio.volume = STATE.settings.masterVolume;
  audio.addEventListener("ended", () => { if (previewAudio === audio) previewAudio = null; });
  previewAudio = audio;
  audio.play().catch(() => {});
}

function renderBoard() {
  const el = document.getElementById("board");
  el.innerHTML = "";
  STATE.board.forEach((btn) => {
    const div = document.createElement("div");
    div.className = "board-btn" + (STATE.editMode ? " edit-mode" : "");
    div.dataset.id = btn.id;
    div.appendChild(buttonContentNode(btn));
    if (STATE.editMode) {
      const badge = document.createElement("div");
      badge.className = "edit-badge";
      badge.textContent = "✎";
      div.appendChild(badge);
    }
    attachPressHandlers(div, btn);
    el.appendChild(div);
  });
}

function buttonContentNode(btn) {
  if (btn.customImage) {
    const img = document.createElement("img");
    img.src = btn.customImage;
    img.alt = "";
    return img;
  }
  const wrap = document.createElement("div");
  wrap.className = "btn-icon-text";
  const iconSpan = document.createElement("span");
  iconSpan.className = "fa";
  iconSpan.textContent = faGlyph(btn.icon);
  const textSpan = document.createElement("span");
  textSpan.className = "btn-text";
  textSpan.textContent = btn.customText || defaultTextFromSoundFile(btn.sound);
  wrap.appendChild(iconSpan);
  wrap.appendChild(textSpan);
  return wrap;
}

function attachPressHandlers(div, btn) {
  let pressTimer = null;
  let isLongPress = false;
  let pressedAt = 0;

  const clearVisual = () => {
    const elapsed = performance.now() - pressedAt;
    const wait = Math.max(0, MIN_PRESS_VISIBLE_MS - elapsed);
    setTimeout(() => { div.classList.remove("pressed", "deep-pressed"); }, wait);
  };

  div.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    isLongPress = false;
    pressedAt = performance.now();
    div.classList.add("pressed");
    // Fire as soon as the long-press threshold is crossed, not on release — otherwise the
    // user is left holding their finger down with no feedback that anything happened.
    pressTimer = setTimeout(() => {
      isLongPress = true;
      div.classList.add("deep-pressed");
      openEditDialog(btn.id);
      div.classList.remove("pressed", "deep-pressed");
    }, LONG_PRESS_MS);
  });

  const finish = (triggerAction) => {
    clearTimeout(pressTimer);
    if (triggerAction && !isLongPress) {
      if (STATE.editMode) {
        openEditDialog(btn.id);
      } else {
        playButtonSound(btn);
      }
    }
    clearVisual();
  };

  div.addEventListener("pointerup", () => finish(true));
  div.addEventListener("pointercancel", () => finish(false));
  div.addEventListener("pointerleave", () => finish(false));
}

function playButtonSound(btn) {
  if (!STATE.settings.allowSimultaneousSounds) stopAllAudio();
  const audio = new Audio(resolveSoundSrc(btn.sound));
  audio.volume = Math.min(1, Math.max(0, STATE.settings.masterVolume * btn.volume));
  STATE.activeAudios.push(audio);
  audio.addEventListener("ended", () => {
    STATE.activeAudios = STATE.activeAudios.filter((a) => a !== audio);
  });
  if (!STATE.settings.allowLongSounds) {
    setTimeout(() => { audio.pause(); }, STATE.settings.maxSoundDurationMs);
  }
  audio.play().catch(() => {});
  pulseSpeaker();
  vibrateIfEnabled();
}

function stopAllAudio() {
  STATE.activeAudios.forEach((a) => { a.pause(); });
  STATE.activeAudios = [];
}

function vibrateIfEnabled() {
  if (STATE.settings.hapticFeedbackEnabled && "vibrate" in navigator) {
    navigator.vibrate(15);
  }
}

/* -------------------------------- Kit menu --------------------------------- */

function renderKitMenu() {
  const builtinWrap = document.getElementById("kit-menu-builtin");
  const customWrap = document.getElementById("kit-menu-custom");
  builtinWrap.innerHTML = "";
  customWrap.innerHTML = "";

  STATE.builtinKits.forEach((kit) => {
    const btnEl = document.createElement("button");
    btnEl.className = "dropdown-item";
    btnEl.textContent = kit.name[resolvedLang()] || kit.name.en || kit.id;
    btnEl.addEventListener("click", () => applyKit(kit));
    builtinWrap.appendChild(btnEl);
  });

  STATE.customKits.forEach((kit) => {
    const btnEl = document.createElement("button");
    btnEl.className = "dropdown-item";
    btnEl.textContent = kit.name[resolvedLang()] || kit.name.en || kit.id;
    btnEl.addEventListener("click", () => applyKit(kit));
    customWrap.appendChild(btnEl);
  });
}

function applyKit(kit) {
  const overrides = loadJson("soniloko_kit_overrides", {});
  const override = overrides[kit.id];
  if (override) {
    STATE.board = JSON.parse(JSON.stringify(override));
  } else {
    STATE.board = kit.buttons.map((b) => blankBoardButton(b.id, b.icon, b.sound));
    STATE.board.forEach((b, i) => { b.volume = kit.buttons[i].volume || 1; });
  }
  STATE.currentKitId = kit.id;
  save("soniloko_current_kit_id", kit.id);
  saveBoard();
  renderBoard();
  hideEl("kit-menu");
}

function wireKitDialogs() {
  document.getElementById("btn-save-kit").addEventListener("click", () => {
    document.getElementById("save-kit-name-input").value = "";
    hideEl("kit-menu");
    showEl("save-kit-dialog");
  });
  document.getElementById("btn-save-kit-cancel").addEventListener("click", () => hideEl("save-kit-dialog"));
  document.getElementById("btn-save-kit-confirm").addEventListener("click", () => {
    const name = document.getElementById("save-kit-name-input").value.trim();
    if (!name) return;
    STATE.customKits.push({
      id: uid(),
      name: { es: name, en: name },
      buttons: STATE.board.map((b) => ({ id: b.id, icon: b.icon, sound: b.sound, volume: b.volume }))
    });
    saveCustomKits();
    renderKitMenu();
    hideEl("save-kit-dialog");
  });

  document.getElementById("btn-manage-kits").addEventListener("click", () => {
    hideEl("kit-menu");
    renderManageKitsList();
    showEl("manage-kits-dialog");
  });
  document.getElementById("btn-manage-kits-close").addEventListener("click", () => hideEl("manage-kits-dialog"));
}

function renderManageKitsList() {
  const list = document.getElementById("manage-kits-list");
  list.innerHTML = "";
  if (STATE.customKits.length === 0) {
    const p = document.createElement("p");
    p.textContent = t("no_custom_kits");
    list.appendChild(p);
    return;
  }
  STATE.customKits.forEach((kit) => {
    const row = document.createElement("div");
    row.className = "sound-row";
    const name = kit.name[resolvedLang()] || kit.name.en;
    row.innerHTML =
      '<span>' + escapeHtml(name) + '</span>' +
      '<span><button class="btn" data-action="rename" style="margin-right:6px;">' + escapeHtml(t("rename")) + '</button>' +
      '<button class="delete-icon" data-action="delete">✕</button></span>';
    row.querySelector('[data-action="rename"]').addEventListener("click", () => {
      const newName = prompt(t("kit_name"), name);
      if (newName && newName.trim()) {
        kit.name = { es: newName.trim(), en: newName.trim() };
        saveCustomKits();
        renderKitMenu();
        renderManageKitsList();
      }
    });
    row.querySelector('[data-action="delete"]').addEventListener("click", () => {
      showConfirm(t("delete_kit_confirm_title"), t("delete_kit_confirm_message"), () => {
        STATE.customKits = STATE.customKits.filter((k) => k.id !== kit.id);
        saveCustomKits();
        renderKitMenu();
        renderManageKitsList();
      });
    });
    list.appendChild(row);
  });
}

/* ------------------------------- Edit dialog -------------------------------- */

function openEditDialog(buttonId) {
  const btn = STATE.board.find((b) => b.id === buttonId);
  if (!btn) return;
  STATE.editingButtonId = buttonId;
  STATE.editingDraft = JSON.parse(JSON.stringify(btn));
  renderEditDialog();
  showEl("edit-dialog");
}

function renderEditDialog() {
  const draft = STATE.editingDraft;
  const preview = document.getElementById("edit-preview");
  preview.innerHTML = "";
  preview.appendChild(buttonContentNode(draft));
  document.getElementById("edit-text-input").value = draft.customText || "";
  document.getElementById("edit-volume").value = draft.volume;
  document.getElementById("btn-choose-sound").textContent = t("sound") + ": " + soundDisplayName(draft.sound);
}

function wireEditDialog() {
  document.getElementById("btn-choose-icon").addEventListener("click", () => {
    STATE.iconPickerTarget = "editingDraft";
    openIconPicker(STATE.editingDraft.icon);
  });

  document.getElementById("btn-choose-image").addEventListener("click", () => {
    document.getElementById("image-file-input").click();
  });
  document.getElementById("image-file-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      STATE.editingDraft.customImage = reader.result;
      renderEditDialog();
    };
    reader.readAsDataURL(file);
    e.target.value = "";
  });
  document.getElementById("btn-remove-image").addEventListener("click", () => {
    STATE.editingDraft.customImage = null;
    renderEditDialog();
  });

  document.getElementById("edit-text-input").addEventListener("input", (e) => {
    STATE.editingDraft.customText = e.target.value || null;
  });
  document.getElementById("edit-volume").addEventListener("input", (e) => {
    STATE.editingDraft.volume = parseFloat(e.target.value);
  });

  document.getElementById("btn-choose-sound").addEventListener("click", () => {
    STATE.soundPickerTarget = "editingDraft";
    openSoundPicker();
  });

  document.getElementById("btn-edit-cancel").addEventListener("click", () => {
    hideEl("edit-dialog");
    STATE.editingButtonId = null;
    STATE.editingDraft = null;
  });
  document.getElementById("btn-edit-save").addEventListener("click", () => {
    const idx = STATE.board.findIndex((b) => b.id === STATE.editingButtonId);
    if (idx >= 0) STATE.board[idx] = STATE.editingDraft;
    saveBoard();
    renderBoard();
    hideEl("edit-dialog");
    STATE.editingButtonId = null;
    STATE.editingDraft = null;
  });
}

/* ------------------------------- Icon picker -------------------------------- */

function openIconPicker(currentIcon) {
  document.getElementById("icon-search").value = "";
  renderIconGrid("", currentIcon);
  showEl("icon-picker-dialog");
}

function renderIconGrid(query, currentIcon) {
  const grid = document.getElementById("icon-grid");
  grid.innerHTML = "";
  faSearch(query).forEach((name) => {
    const cell = document.createElement("div");
    cell.className = "icon-cell fa" + (name === currentIcon ? " selected" : "");
    cell.textContent = faGlyph(name);
    cell.addEventListener("click", () => {
      STATE.editingDraft.icon = name;
      STATE.editingDraft.customImage = null;
      renderEditDialog();
      hideEl("icon-picker-dialog");
    });
    grid.appendChild(cell);
  });
}

function wireIconPicker() {
  document.getElementById("icon-search").addEventListener("input", (e) => {
    renderIconGrid(e.target.value, STATE.editingDraft && STATE.editingDraft.icon);
  });
  document.getElementById("btn-icon-picker-close").addEventListener("click", () => hideEl("icon-picker-dialog"));
}

/* ------------------------------- Sound picker -------------------------------- */

function allSoundNames() {
  return BUNDLED_SOUNDS.concat(STATE.customSounds.map((s) => s.name));
}

function openSoundPicker() {
  document.getElementById("sound-search").value = "";
  renderSoundList("");
  showEl("sound-picker-dialog");
}

function renderSoundList(query) {
  const list = document.getElementById("sound-list");
  list.innerHTML = "";
  const q = (query || "").toLowerCase();
  allSoundNames().filter((n) => n.toLowerCase().includes(q)).forEach((name) => {
    const isOwn = STATE.customSounds.some((s) => s.name === name);
    const row = document.createElement("div");
    row.className = "sound-row";
    row.innerHTML =
      "<span>" + escapeHtml(name) + "</span>" +
      '<span class="select-icon">✓</span>' +
      (isOwn ? '<span class="rename-icon">✎</span><span class="delete-icon">✕</span>' : "");
    // Tapping the row previews the sound; the checkmark actually assigns it — otherwise every
    // preview immediately closes the picker, making it hard to compare a few in a row.
    row.addEventListener("click", (e) => {
      if (e.target.closest(".select-icon, .rename-icon, .delete-icon")) return;
      if (STATE.settings.previewSoundsEnabled) {
        playPreview(name);
      }
    });
    row.querySelector(".select-icon").addEventListener("click", (e) => {
      e.stopPropagation();
      stopPreviewAudio();
      if (STATE.editingDraft) STATE.editingDraft.sound = name;
      renderEditDialog();
    });
    const ren = row.querySelector(".rename-icon");
    if (ren) {
      ren.addEventListener("click", (e) => {
        e.stopPropagation();
        const own = STATE.customSounds.find((s) => s.name === name);
        if (!own) return;
        const typed = prompt(t("sound_name"), name);
        if (!typed || !typed.trim() || typed.trim() === name) return;
        let finalName = typed.trim();
        const existing = new Set(allSoundNames().filter((n) => n !== name));
        let counter = 1;
        while (existing.has(finalName)) { finalName = typed.trim() + "_" + counter; counter++; }
        own.name = finalName;
        saveCustomSounds();
        STATE.board.forEach((b) => { if (b.sound === name) b.sound = finalName; });
        saveBoard();
        if (STATE.editingDraft && STATE.editingDraft.sound === name) STATE.editingDraft.sound = finalName;
        renderBoard();
        renderEditDialog();
        renderSoundList(document.getElementById("sound-search").value);
      });
    }
    const del = row.querySelector(".delete-icon");
    if (del) {
      del.addEventListener("click", (e) => {
        e.stopPropagation();
        showConfirm(t("delete_sound_confirm_title"), t("delete_sound_confirm_message"), () => {
          STATE.customSounds = STATE.customSounds.filter((s) => s.name !== name);
          saveCustomSounds();
          renderSoundList(document.getElementById("sound-search").value);
        });
      });
    }
    list.appendChild(row);
  });
}

function wireSoundPicker() {
  document.getElementById("sound-search").addEventListener("input", (e) => renderSoundList(e.target.value));
  document.getElementById("btn-sound-picker-close").addEventListener("click", () => { stopPreviewAudio(); hideEl("sound-picker-dialog"); });

  document.getElementById("btn-import-sound").addEventListener("click", () => {
    document.getElementById("sound-file-input").click();
  });
  document.getElementById("sound-file-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      let name = file.name;
      const existing = new Set(allSoundNames());
      let counter = 1;
      const base = name.replace(/\.[^/.]+$/, "");
      const ext = name.includes(".") ? name.slice(name.lastIndexOf(".")) : "";
      while (existing.has(name)) { name = base + "_" + counter + ext; counter++; }
      STATE.customSounds.push({ name, dataUrl: reader.result });
      saveCustomSounds();
      if (STATE.editingDraft) STATE.editingDraft.sound = name;
      renderSoundList(document.getElementById("sound-search").value);
      renderEditDialog();
    };
    reader.readAsDataURL(file);
    e.target.value = "";
  });

  document.getElementById("btn-record-sound").addEventListener("click", () => {
    hideEl("sound-picker-dialog");
    openRecordDialog();
  });
}

/* ------------------------------ Record dialog -------------------------------- */

let mediaRecorder = null;
let recordStream = null;
let recordedChunks = [];
let recordTimerHandle = null;
let recordStartedAt = 0;

function openRecordDialog() {
  resetRecordUI();
  showEl("record-dialog");
}

function resetRecordUI() {
  STATE.recordBlob = null;
  document.getElementById("record-timer").textContent = "00:00";
  document.getElementById("btn-record-play").classList.add("hidden");
  document.getElementById("record-name-input").classList.add("hidden");
  document.getElementById("record-name-input").value = "";
  document.getElementById("btn-record-save").classList.add("hidden");
  const toggleBtn = document.getElementById("btn-record-toggle");
  toggleBtn.classList.remove("recording");
  toggleBtn.querySelector(".fa").setAttribute("data-icon", "microphone");
  applyStaticIcons(toggleBtn);
  document.getElementById("record-toggle-label").textContent = t("record");
}

function updateRecordTimer() {
  const elapsed = Date.now() - recordStartedAt;
  const s = Math.floor(elapsed / 1000);
  document.getElementById("record-timer").textContent =
    String(Math.floor(s / 60)).padStart(2, "0") + ":" + String(s % 60).padStart(2, "0");
}

async function startRecording() {
  try {
    recordStream = await navigator.mediaDevices.getUserMedia({ audio: true });
  } catch (e) {
    alert(t("record_permission_needed"));
    return;
  }
  recordedChunks = [];
  mediaRecorder = new MediaRecorder(recordStream);
  mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) recordedChunks.push(e.data); };
  mediaRecorder.onstop = () => {
    STATE.recordBlob = new Blob(recordedChunks, { type: mediaRecorder.mimeType || "audio/webm" });
    document.getElementById("btn-record-play").classList.remove("hidden");
    document.getElementById("record-name-input").classList.remove("hidden");
    document.getElementById("btn-record-save").classList.remove("hidden");
  };
  mediaRecorder.start();
  recordStartedAt = Date.now();
  recordTimerHandle = setInterval(updateRecordTimer, 200);
  document.getElementById("btn-record-toggle").classList.add("recording");
  document.getElementById("btn-record-toggle").querySelector(".fa").setAttribute("data-icon", "volume-mute");
  applyStaticIcons(document.getElementById("btn-record-toggle"));
  document.getElementById("record-toggle-label").textContent = t("stop_recording");
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== "inactive") mediaRecorder.stop();
  if (recordStream) recordStream.getTracks().forEach((tr) => tr.stop());
  clearInterval(recordTimerHandle);
  document.getElementById("btn-record-toggle").classList.remove("recording");
  document.getElementById("btn-record-toggle").querySelector(".fa").setAttribute("data-icon", "microphone");
  applyStaticIcons(document.getElementById("btn-record-toggle"));
  document.getElementById("record-toggle-label").textContent = t("record");
}

function wireRecordDialog() {
  document.getElementById("btn-record-toggle").addEventListener("click", () => {
    if (mediaRecorder && mediaRecorder.state === "recording") stopRecording();
    else startRecording();
  });
  document.getElementById("btn-record-play").addEventListener("click", () => {
    if (!STATE.recordBlob) return;
    new Audio(URL.createObjectURL(STATE.recordBlob)).play().catch(() => {});
  });
  document.getElementById("btn-record-cancel").addEventListener("click", () => {
    if (mediaRecorder && mediaRecorder.state === "recording") stopRecording();
    hideEl("record-dialog");
    showEl("sound-picker-dialog");
  });
  document.getElementById("btn-record-save").addEventListener("click", () => {
    const name = document.getElementById("record-name-input").value.trim();
    if (!name || !STATE.recordBlob) return;
    const reader = new FileReader();
    reader.onload = () => {
      let finalName = name;
      const existing = new Set(allSoundNames());
      let counter = 1;
      while (existing.has(finalName)) { finalName = name + "_" + counter; counter++; }
      STATE.customSounds.push({ name: finalName, dataUrl: reader.result });
      saveCustomSounds();
      if (STATE.editingDraft) STATE.editingDraft.sound = finalName;
      hideEl("record-dialog");
      showEl("sound-picker-dialog");
      renderSoundList(document.getElementById("sound-search").value);
      renderEditDialog();
    };
    reader.readAsDataURL(STATE.recordBlob);
  });
}

/* -------------------------------- Settings ----------------------------------- */

function renderSettingsPanel() {
  const langWrap = document.getElementById("language-options");
  langWrap.innerHTML = "";
  [["system", t("system_default")], ["es", "Español"], ["en", "English"]].forEach(([value, label]) => {
    const row = document.createElement("label");
    row.className = "radio-option";
    row.innerHTML =
      '<input type="radio" name="language" value="' + value + '"' +
      (STATE.settings.language === value ? " checked" : "") + "> " + escapeHtml(label);
    row.querySelector("input").addEventListener("change", () => {
      STATE.settings.language = value;
      saveSettings();
      refreshAllText();
    });
    langWrap.appendChild(row);
  });

  document.getElementById("master-volume").value = STATE.settings.masterVolume;

  const swatchWrap = document.getElementById("theme-swatches");
  swatchWrap.innerHTML = "";
  THEMES.forEach((theme) => {
    const sw = document.createElement("div");
    sw.className = "swatch" + (STATE.settings.theme === theme.id ? " selected" : "");
    sw.style.background = theme.primary;
    sw.addEventListener("click", () => {
      STATE.settings.theme = theme.id;
      saveSettings();
      applyTheme(theme.id);
      renderSettingsPanel();
    });
    swatchWrap.appendChild(sw);
  });

  document.getElementById("toggle-preview").checked = STATE.settings.previewSoundsEnabled;
  document.getElementById("toggle-haptics").checked = STATE.settings.hapticFeedbackEnabled;
  document.getElementById("toggle-long-sounds").checked = STATE.settings.allowLongSounds;
  document.getElementById("toggle-simultaneous").checked = STATE.settings.allowSimultaneousSounds;
  document.getElementById("duration-selector-wrap").classList.toggle("hidden", STATE.settings.allowLongSounds);
  renderDurationChips();
}

function renderDurationChips() {
  const wrap = document.getElementById("duration-chips");
  wrap.innerHTML = "";
  const current = STATE.settings.maxSoundDurationMs;
  const isCustom = !DURATION_PRESETS_MS.includes(current);
  DURATION_PRESETS_MS.forEach((ms) => {
    const chip = document.createElement("div");
    chip.className = "chip" + (!isCustom && current === ms ? " selected" : "");
    chip.textContent = ms / 1000 + "s";
    chip.addEventListener("click", () => {
      STATE.settings.maxSoundDurationMs = ms;
      saveSettings();
      renderDurationChips();
    });
    wrap.appendChild(chip);
  });
  const customChip = document.createElement("div");
  customChip.className = "chip" + (isCustom ? " selected" : "");
  customChip.textContent = t("custom_duration");
  customChip.addEventListener("click", () => {
    if (!isCustom) { STATE.settings.maxSoundDurationMs = 20000; saveSettings(); renderDurationChips(); }
  });
  wrap.appendChild(customChip);

  if (isCustom) {
    const slider = document.createElement("input");
    slider.type = "range";
    slider.min = "1000";
    slider.max = "60000";
    slider.value = String(current);
    slider.style.width = "100%";
    slider.style.marginTop = "10px";
    slider.addEventListener("input", (e) => {
      STATE.settings.maxSoundDurationMs = parseInt(e.target.value, 10);
      saveSettings();
    });
    wrap.appendChild(slider);
  }
}

function refreshAllText() {
  applyI18n();
  renderKitMenu();
  renderSettingsPanel();
  if (!document.getElementById("edit-dialog").classList.contains("hidden")) renderEditDialog();
  renderHelpBody();
  const isRecording = mediaRecorder && mediaRecorder.state === "recording";
  document.getElementById("record-toggle-label").textContent = t(isRecording ? "stop_recording" : "record");
}

function factoryReset() {
  ["soniloko_settings", "soniloko_board", "soniloko_custom_kits", "soniloko_custom_sounds", "soniloko_current_kit_id", "soniloko_kit_overrides"].forEach((k) => localStorage.removeItem(k));
  location.reload();
}

function wireGlobalEvents() {
  document.getElementById("btn-settings").addEventListener("click", () => {
    renderSettingsPanel();
    showEl("settings-panel");
  });
  document.getElementById("btn-settings-back").addEventListener("click", () => hideEl("settings-panel"));

  document.getElementById("btn-kits").addEventListener("click", (e) => {
    e.stopPropagation();
    document.getElementById("kit-menu").classList.toggle("hidden");
  });
  document.addEventListener("click", (e) => {
    const menu = document.getElementById("kit-menu");
    if (!menu.classList.contains("hidden") && !menu.contains(e.target) && e.target.id !== "btn-kits") {
      menu.classList.add("hidden");
    }
  });

  document.getElementById("btn-edit-mode").addEventListener("click", () => {
    STATE.editMode = !STATE.editMode;
    renderBoard();
  });

  document.getElementById("master-volume").addEventListener("input", (e) => {
    STATE.settings.masterVolume = parseFloat(e.target.value);
    saveSettings();
  });
  document.getElementById("toggle-preview").addEventListener("change", (e) => {
    STATE.settings.previewSoundsEnabled = e.target.checked;
    saveSettings();
  });
  document.getElementById("toggle-haptics").addEventListener("change", (e) => {
    STATE.settings.hapticFeedbackEnabled = e.target.checked;
    saveSettings();
  });
  document.getElementById("toggle-long-sounds").addEventListener("change", (e) => {
    STATE.settings.allowLongSounds = e.target.checked;
    saveSettings();
    renderSettingsPanel();
  });
  document.getElementById("toggle-simultaneous").addEventListener("change", (e) => {
    STATE.settings.allowSimultaneousSounds = e.target.checked;
    saveSettings();
  });

  document.getElementById("btn-export").addEventListener("click", exportConfig);
  document.getElementById("btn-import").addEventListener("click", () => document.getElementById("import-file-input").click());
  document.getElementById("import-file-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try { importConfig(JSON.parse(reader.result)); } catch (err) { /* invalid file, ignore */ }
    };
    reader.readAsText(file);
    e.target.value = "";
  });

  document.getElementById("btn-factory-reset").addEventListener("click", () => {
    showConfirm(t("reset_confirm_title"), t("reset_confirm_message"), factoryReset);
  });

  document.getElementById("btn-install").addEventListener("click", () => {
    if (STATE.deferredInstallPrompt) {
      STATE.deferredInstallPrompt.prompt();
      STATE.deferredInstallPrompt = null;
    }
  });

  document.addEventListener("contextmenu", (e) => e.preventDefault());
  document.addEventListener("selectstart", (e) => e.preventDefault());
  document.addEventListener("gesturestart", (e) => e.preventDefault());
}

function exportConfig() {
  const payload = { settings: STATE.settings, buttons: STATE.board, customKits: STATE.customKits, customSounds: STATE.customSounds };
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "soniloko_config.json";
  a.click();
}

function importConfig(payload) {
  if (payload.settings) { STATE.settings = Object.assign({}, DEFAULT_SETTINGS, payload.settings); saveSettings(); }
  if (payload.buttons) { STATE.board = payload.buttons; saveBoard(); }
  if (payload.customKits) { STATE.customKits = payload.customKits; saveCustomKits(); }
  if (payload.customSounds) { STATE.customSounds = payload.customSounds; saveCustomSounds(); }
  applyTheme(STATE.settings.theme);
  refreshAllText();
  renderBoard();
}

/* --------------------------------- Help --------------------------------------- */

function renderHelpBody() {
  document.getElementById("help-body").innerHTML = t("help_body");
}

function wireHelpDialog() {
  document.getElementById("btn-help").addEventListener("click", () => {
    renderHelpBody();
    showEl("help-dialog");
  });
  document.getElementById("btn-help-close").addEventListener("click", () => hideEl("help-dialog"));
}

/* ----------------------------- Confirm dialog ---------------------------------- */

function showConfirm(title, message, onConfirm) {
  document.getElementById("confirm-title").textContent = title;
  document.getElementById("confirm-message").textContent = message;
  showEl("confirm-dialog");
  const okBtn = document.getElementById("btn-confirm-ok");
  const cancelBtn = document.getElementById("btn-confirm-cancel");
  const cleanup = () => {
    okBtn.replaceWith(okBtn.cloneNode(true));
    cancelBtn.replaceWith(cancelBtn.cloneNode(true));
    hideEl("confirm-dialog");
  };
  document.getElementById("btn-confirm-ok").addEventListener("click", () => { cleanup(); onConfirm(); }, { once: true });
  document.getElementById("btn-confirm-cancel").addEventListener("click", cleanup, { once: true });
}

/* ---------------------------------- Utils --------------------------------------- */

function showEl(id) { document.getElementById(id).classList.remove("hidden"); }
function hideEl(id) { document.getElementById(id).classList.add("hidden"); }

function attemptOrientationLock() {
  try {
    if (screen.orientation && screen.orientation.lock) {
      screen.orientation.lock("portrait").catch(() => {});
    }
  } catch (e) { /* not supported outside installed/fullscreen context */ }
}

function setupInstallPrompt() {
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    STATE.deferredInstallPrompt = e;
    document.getElementById("install-card").style.display = "block";
  });
}

function registerServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
  }
}
