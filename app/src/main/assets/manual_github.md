# Manual — desplegar SoniLoko en GitHub con auto-actualización

Esto pone en marcha el mismo sistema que ya tienes en DSK LoFi: cada día (o cuando tú
quieras) GitHub Actions comprueba si hay una versión nueva de NewPipeExtractor, y si la hay,
recompila la app, la firma, y publica un Release con el APK. La propia app comprueba ese
Release al abrir y te ofrece descargar e instalar la actualización.

Todo lo de **código** (`UpdateChecker.kt`, el manifest, el workflow `.yml`) ya está hecho.
Esto es lo que tienes que hacer tú, paso a paso.

## 1. Crear el repositorio en GitHub

1. Entra en tu cuenta de GitHub (la misma que usas para DSK LoFi) y crea un repositorio nuevo,
   por ejemplo `soniloko`. Puede ser **público** (recomendado, así `raw.githubusercontent.com`
   funciona sin autenticación — es lo que usa la app para leer `version.json`).
2. Anota el nombre exacto que le pongas (usuario/repositorio), lo necesitas en el paso 3.

## 2. Subir el proyecto

Desde la carpeta del proyecto (`D:\Documentos\_Android\_AndroidStudioApps\Soniloco`):

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/soniloko.git
git push -u origin main
```

Revisa antes que no subes nada sensible (el `.gitignore` de un proyecto Android estándar ya
excluye `local.properties`, `/build`, etc. — si no existe uno, dímelo y te lo genero).

## 3. Ajustar la URL en el código

Abre `app/src/main/java/com/dsk/soniloko/update/UpdateChecker.kt` y cambia esta línea si tu
usuario/repositorio no es exactamente `dskmusic/soniloko`:

```kotlin
private const val VERSION_JSON_URL =
    "https://raw.githubusercontent.com/TU_USUARIO/TU_REPO/main/version.json"
```

Vuelve a compilar y a subir el cambio (`git add`, `commit`, `push`) antes de continuar.

## 4. Generar un keystore de release (si no tienes uno ya)

Si ya tienes uno que uses para DSK LoFi puedes reutilizarlo o crear uno nuevo específico para
SoniLoko. Para crear uno nuevo (requiere el JDK, `keytool` viene incluido):

```bash
keytool -genkey -v -keystore soniloko-release.keystore -alias soniloko -keyalg RSA -keysize 2048 -validity 10000
```

Te pedirá una contraseña para el keystore y otra (puede ser la misma) para la clave. Guarda
ese archivo `.keystore` en un sitio seguro **fuera del repositorio** — nunca lo subas a GitHub.

## 5. Codificar el keystore en base64

El workflow necesita el keystore como texto (base64) para guardarlo como secreto de GitHub.

En PowerShell:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("soniloko-release.keystore")) | Set-Clipboard
```
Esto copia el resultado directamente al portapapeles.

## 6. Crear los secretos en GitHub

En el repositorio: **Settings → Secrets and variables → Actions → New repository secret**.
Crea estos cuatro, exactamente con estos nombres:

| Nombre | Valor |
|---|---|
| `KEYSTORE_BASE64` | El texto largo que copiaste en el paso 5 |
| `KEYSTORE_PASSWORD` | La contraseña del keystore |
| `KEY_ALIAS` | El alias que usaste (`soniloko` en el ejemplo del paso 4) |
| `KEY_PASSWORD` | La contraseña de la clave |

## 7. Dar permisos de escritura a las Actions

Esto es fácil de pasar por alto y hace que falle el `git push`/la creación del Release:
**Settings → Actions → General → Workflow permissions** → marca **"Read and write permissions"**
→ Guardar.

## 8. Primera ejecución manual

Ve a la pestaña **Actions** del repositorio → selecciona el workflow **"Check NewPipeExtractor
& Release"** → **Run workflow**. Para la primera vez, activa la opción **`skip_check`** (así
compila y publica ya, sin esperar a que cambie NewPipeExtractor) y ejecútalo.

Si todo va bien, en unos minutos tendrás:
- Un nuevo commit automático (sube `versionCode`/`versionName`).
- Un Release nuevo en la pestaña **Releases**, con el APK adjunto.
- Un archivo `version.json` en la raíz del repositorio.

## 9. Comprobar que la app lo detecta

Instala la app (la que compilaste tú mismo, con un `versionCode` menor al que acaba de publicar
el workflow) y ábrela. Debería aparecer el diálogo de actualización. Al pulsar "Descargar e
instalar", Android pedirá permiso para instalar aplicaciones de esta fuente la primera vez
(activa el permiso cuando lo pida) y luego se abrirá el instalador normal.

## A partir de aquí

El workflow ya corre solo cada día a las 06:00 UTC. También puedes lanzarlo a mano cuando
quieras desde la pestaña Actions, con dos opciones:
- **`force_build`**: recompila y publica aunque NewPipeExtractor no haya cambiado (útil si tú
  mismo has tocado código de la app y quieres publicar una versión).
- **`skip_check`**: compila y publica sin ni siquiera comprobar NewPipeExtractor, usando la
  versión que ya esté en el repo.

Cada vez que se publica una versión nueva, el `versionCode` sube en 1 automáticamente, así que
la app siempre sabrá distinguir cuál es más reciente.
