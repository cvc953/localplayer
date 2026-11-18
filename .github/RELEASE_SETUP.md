# Configuración de GitHub Actions para Releases

Este documento explica cómo configurar el workflow de GitHub Actions para crear releases automáticos de Local Player.

## 📋 Requisitos previos

- Repositorio en GitHub
- Acceso a la configuración de Secrets del repositorio

## 🚀 Uso básico

### Antes de crear el tag

El PR de release debe actualizar, en el mismo commit que se va a taggear:

1. `versionCode` y `versionName` en `app/build.gradle.kts`.
2. La sección de la versión en los tres `CHANGELOG*.md`.
3. El archivo `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.

El workflow verifica el punto 1 y falla si no coincide con el tag. Los puntos 2 y 3 son los
que muestran F-Droid y GitHub.

### Crear un release automáticamente

1. **Crear un tag con formato de versión**:
   ```bash
   git tag v1.1.4
   git push origin v1.1.4
   ```

2. El workflow se activará automáticamente y:
   - Compilará la APK release
   - Creará checksums SHA256
   - Publicará un GitHub Release con la APK

3. El release estará disponible en: `https://github.com/cvc953/localplayer/releases`

### Crear un release manual

1. Ve a la pestaña "Actions" en GitHub
2. Selecciona "Android Release Build"
3. Click en "Run workflow"
4. Escribe la versión (por ejemplo `1.1.4`) y ejecuta

El workflow crea el tag y el release con esa versión.

## 🏷️ La versión se escribe en dos lugares y el workflow los verifica

El checker de actualizaciones de F-Droid (`UpdateCheckMode: Tags`) no compila nada: parsea
`app/build.gradle.kts` con regex estáticos y necesita leer `versionCode = <número>` y
`versionName = "<entre comillas>"`. No evalúa variables, tags ni propiedades de Gradle. Si
esos valores son expresiones, F-Droid no detecta la actualización.

Por eso el PR de release actualiza ambos literales en `defaultConfig`:

```kotlin
versionCode = 10106
versionName = "1.1.6"
```

El workflow verifica que coincidan con el tag antes de construir y falla con un mensaje claro
si están desactualizados. `-PappVersionName` sigue inyectando el nombre completo en el APK
(útil para pre-releases) y `-PappVersionCode` puede sobrescribir el código.

### Formato aceptado

`vMAJOR.MINOR.PATCH` con un sufijo de pre-release opcional.

| Tag | Tipo | `versionName` en el APK | `versionCode` |
|-----|------|-------------------------|---------------|
| `v1.1.6` | Estable | `1.1.6` | `10106` |
| `v1.1.6-beta.1` | Pre-release | `1.1.6-beta.1` | `10106` |
| `v1.1.6-rc.2` | Pre-release | `1.1.6-rc.2` | `10106` |
| `v2.0.0` | Estable | `2.0.0` | `20000` |

Cualquier sufijo después del guion convierte el release en pre-release. Sin sufijo, es
un release estable.

### Cómo se calcula `versionCode`

```text
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
```

`minor` y `patch` deben estar entre 0 y 99; fuera de ese rango el workflow falla con un
mensaje explícito.

El valor se escribe como literal en `app/build.gradle.kts` (no se calcula en runtime) porque
F-Droid lo lee con un regex. Esto reemplaza el contador manual que se llevaba antes (14, 15,
16...). El salto no rompe nada: `10106` es mayor que `14`, así que la actualización se instala
normalmente sobre cualquier versión anterior.

Una pre-release comparte el `versionCode` de su versión base. Android acepta una
actualización con el mismo `versionCode`, así que el release estable se instala encima
de la pre-release sin pedir desinstalación.

## 🧪 Pre-releases

Un tag con sufijo publica un release marcado como **pre-release** en GitHub:

```bash
git tag v1.1.4-beta.1
git push origin v1.1.4-beta.1
```

Consecuencias:

- GitHub **no** lo marca como "Latest", así que la página de releases sigue apuntando a
  la última versión estable.
- El cuerpo del release incluye un aviso de que es una versión de prueba.
- El APK se publica como `LocalPlayer-1.1.4-beta.1.apk`.
- Si el CHANGELOG no tiene una sección `## [1.1.4-beta.1]`, se usa la de `## [1.1.4]`.

Para promover una pre-release a estable, publica el tag sin sufijo (`v1.1.4`) sobre el
commit que quieras publicar. No hay que "convertir" nada: es un release nuevo.

## 🔐 Firma de APK (obligatoria)

El workflow **falla si faltan los secrets de firma**. No publica APKs sin firmar.

### 1. Generar un keystore (si no tienes uno)

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias
```

### 2. Convertir el keystore a Base64

```bash
base64 my-release-key.jks > keystore.base64.txt
```

### 3. Configurar secrets en GitHub

Ve a: `Settings` → `Secrets and variables` → `Actions` → `New repository secret`

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `SIGNING_KEYSTORE_BASE64` | Contenido del archivo `keystore.base64.txt` | `MIIJhgIBAzCCCU...` |
| `ANDROID_SIGNING_STORE_PASSWORD` | Contraseña del keystore | `mi_password_123` |
| `ANDROID_SIGNING_KEY_ALIAS` | Alias de la clave | `my-key-alias` |
| `ANDROID_SIGNING_KEY_PASSWORD` | Contraseña de la clave | `mi_key_password` |

En un keystore PKCS12 la contraseña de la clave es la misma que la del keystore. Si el
keystore es JKS, pueden ser distintas.

### 4. Verificar

El siguiente push con tag creará una APK firmada:

```bash
git tag v1.1.4
git push origin v1.1.4
```

### ⚠️ Respalda el keystore

Si pierdes el keystore, pierdes la identidad de firma de la app: no vas a poder publicar
actualizaciones que instalen encima de las versiones ya distribuidas, porque Android
rechaza una APK firmada con otra clave. Guarda el `.jks` y las contraseñas en un lugar
seguro, fuera del repositorio.

## 📦 Artefactos generados

Cada release incluye:

1. **APK**: `LocalPlayer-X.Y.Z.apk` (o el nombre completo con sufijo de pre-release)
2. **Checksums**: `checksums.txt` con SHA256
3. **Release Notes**: Generadas automáticamente desde los CHANGELOG

## 🛠️ Personalizar el workflow

Edita `.github/workflows/release.yml` para:

- Cambiar el formato de nombres de archivo
- Agregar más checks (lint, tests, etc.)
- Modificar las release notes
- Agregar notificaciones

## 🐛 Troubleshooting

### El workflow no se activa

- Verifica que el tag comience con `v`
- Asegúrate de hacer push del tag: `git push origin v1.1.4`

### `Invalid version` al inicio del workflow

La versión no respeta `MAJOR.MINOR.PATCH`. Ejemplos válidos: `1.1.4`, `1.1.4-beta.1`,
`2.0.0-rc.2`. Ejemplos inválidos: `1.1`, `v1.1.4` con la `v` incluida en el input,
`1.1.4.1`.

### `Signing secrets are incomplete`

Falta alguno de los cuatro secrets de firma. Revisa los nombres exactos en la tabla de
arriba: el workflow usa `SIGNING_KEYSTORE_BASE64` y las tres variables `ANDROID_SIGNING_*`.

### Error al compilar

- Verifica que el proyecto compile localmente: `./gradlew assembleRelease`
- Revisa los logs en la pestaña Actions de GitHub

### Android rechaza la instalación de una pre-release

Android bloquea instalar una versión con `versionCode` menor al ya instalado
(`INSTALL_FAILED_VERSION_DOWNGRADE`). Si estás en una versión estable y necesitas probar una
pre-release de una versión anterior, desinstala primero. Para pre-releases de la misma
versión base esto no aplica: comparten el `versionCode`.

## 📚 Recursos adicionales

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Semantic Versioning](https://semver.org/)

## 💡 Mejores prácticas

1. **Nunca publiques un tag sin probar la APK** de ese commit
2. **Actualiza el CHANGELOG** antes de crear el tag, incluyendo la sección de la pre-release si necesitas notas propias
3. **No incrementes `versionCode` a mano**: se deriva de la versión
4. **Mantén los secrets seguros** - nunca los incluyas en el código
5. **Usa pre-releases** para pruebas con usuarios antes de publicar el estable

---

**¿Necesitas ayuda?** Abre un issue en el repositorio.
