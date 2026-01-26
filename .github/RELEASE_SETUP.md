# Configuración de GitHub Actions para Releases

Este documento explica cómo configurar el workflow de GitHub Actions para crear releases automáticos de Local Player.

## 📋 Requisitos previos

- Repositorio en GitHub
- Acceso a la configuración de Secrets del repositorio

## 🚀 Uso básico

### Crear un release automáticamente

1. **Crear un tag con formato de versión**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
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
4. Selecciona la rama y ejecuta

## 🔐 Firma de APK (Opcional pero recomendado)

Para firmar automáticamente las APKs con tu keystore:

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

Agrega los siguientes secrets:

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `KEYSTORE_BASE64` | Contenido del archivo `keystore.base64.txt` | `MIIJhgIBAzCCCU...` |
| `KEYSTORE_PASSWORD` | Contraseña del keystore | `mi_password_123` |
| `KEY_ALIAS` | Alias de la clave | `my-key-alias` |
| `KEY_PASSWORD` | Contraseña de la clave | `mi_key_password` |

### 4. Verificar

El siguiente push con tag creará una APK firmada:

```bash
git tag v1.0.1
git push origin v1.0.1
```

## 📝 Formato de versiones

El workflow espera tags con el formato: `vX.Y.Z`

Ejemplos válidos:
- `v1.0.0` - Primera versión
- `v1.0.1` - Parche
- `v1.1.0` - Minor update
- `v2.0.0` - Major update

## 🔄 Actualizar la versión en el código

Antes de crear un tag, actualiza la versión en `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.cvc953.localplayer"
    minSdk = 24
    targetSdk = 36
    versionCode = 2        // Incrementar en cada release
    versionName = "1.0.1"  // Debe coincidir con el tag
    // ...
}
```

## 📦 Artefactos generados

Cada release incluye:

1. **APK**: `LocalPlayer-X.Y.Z.apk`
2. **Checksums**: `checksums.txt` con SHA256
3. **Release Notes**: Generadas automáticamente

## 🛠️ Personalizar el workflow

Edita `.github/workflows/release.yml` para:

- Cambiar el formato de nombres de archivo
- Agregar más checks (lint, tests, etc.)
- Modificar las release notes
- Agregar notificaciones

## 🐛 Troubleshooting

### El workflow no se activa

- Verifica que el tag comience con `v`
- Asegúrate de hacer push del tag: `git push origin v1.0.0`

### Error al compilar

- Verifica que el proyecto compile localmente: `./gradlew assembleRelease`
- Revisa los logs en la pestaña Actions de GitHub

### APK sin firmar

- Si no configuras los secrets de keystore, la APK no estará firmada
- Esto es aceptable para testing, pero no para distribución pública
- Para producción, siempre firma las APKs

## 📚 Recursos adicionales

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Semantic Versioning](https://semver.org/)

## 💡 Mejores prácticas

1. **Siempre prueba localmente** antes de crear un release
2. **Actualiza el CHANGELOG** (si tienes uno)
3. **Usa versionCode incremental** en build.gradle.kts
4. **Mantén los secrets seguros** - nunca los commits en el código
5. **Prueba la APK** del release antes de publicarla oficialmente

---

**¿Necesitas ayuda?** Abre un issue en el repositorio.
