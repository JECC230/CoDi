# CoDi v1 — encuentra a tu compañero de cuarto ideal

**CoDi** es una app de Android estilo Tinder para encontrar **co**mpañeros de **di**vidir cuarto (roomies): perfiles compatibles se descubren con swipe, se hace match y se platica por chat. Esta es la **v1**: una versión enfocada exclusivamente en el descubrimiento de roomies y en la cuenta/perfil del usuario, sin la sección de cuartos en renta.

> **Relación con v2**: este proyecto es una copia independiente de la versión anterior (que sigue viviendo en `avanceCoDi/CoDi`, sin tocar, como "v2"). v1 quita la sección de Cuartos, agrega registro de cuenta completo, edición de perfil estilo Instagram (con foto de galería) y una pantalla de Ajustes con selector de tema claro/oscuro real. `applicationId` es `com.codi.app.v1` (distinto al de v2) para poder instalar ambas versiones a la vez en el mismo dispositivo y compararlas.

---

## Novedades de v1 frente a v2

| Área | v2 | v1 (este proyecto) |
|---|---|---|
| Navegación | Home · Cuartos · Mensajes · Perfil | **Home · Mensajes · Perfil** (sin Cuartos) |
| Catálogo del swipe | 5 perfiles | **100 perfiles** variados (5 escritos a mano + 95 generados con semilla fija) |
| Cuentas | Login con credenciales fijas hardcodeadas | **Firebase Auth**: registro (nombre, apellido, fecha de nacimiento, ocupación, bio, correo/contraseña), login y **Google Sign-In**; perfil en **Cloud Firestore** |
| Perfil | Solo bio editable | **Editor completo estilo Instagram**: foto desde galería, nombre, edad, ocupación, presupuesto, bio, género, chips de intereses/estilo de vida |
| Ajustes | No existía | **Pantalla de Ajustes**: modo oscuro/claro real, soporte y ayuda, cuenta y seguridad, cerrar sesión |
| Tema | Solo oscuro | **Oscuro y claro**, con switch persistente en Ajustes |
| Cuartos/listings | Sí (listados, favoritos, detalle) | **Eliminado por completo** (pantallas, rutas, tabla `listings`, DAO) |

Todo lo demás de la base (notificaciones locales con WorkManager, validaciones, accesibilidad, adaptabilidad, animaciones) se conserva igual que en v2.

---

## Firebase (Authentication + Cloud Firestore)

Proyecto de Firebase: **`codi-app-abe5b`**, app Android `com.codi.app.v1` (`app/google-services.json`).

- **Registro / login con correo y contraseña**: `createUserWithEmailAndPassword` / `signInWithEmailAndPassword` (`data/AuthRepository.kt`). Si el perfil no se puede guardar en Firestore al registrarse, la cuenta recién creada se borra para no dejar cuentas sin perfil.
- **Continuar con Google**: flujo nativo con Credential Manager (`GetGoogleIdOption` → `GoogleAuthProvider.getCredential`). El `serverClientId` es el `default_web_client_id` que genera el plugin de Google Services a partir de `google-services.json`, sin cadenas quemadas en el código. La primera vez se crea el perfil con el nombre de la cuenta de Google.
- **Cerrar sesión**: `FirebaseAuth.signOut()` + limpieza del estado de Credential Manager.
- **Cambiar contraseña**: `FirebaseUser.updatePassword` (las cuentas de Google no tienen contraseña propia).
- **Perfil en la nube**: documento `users/{uid}` con `firstName, lastName, birthDate (Long, millis UTC), email, occupation, city, bio, gender, budget, photoUri, interests` (`data/remote/UserProfileStore.kt`). Perfil lo lee en tiempo real (snapshot listener) y guarda directo ahí.
- **Todo en la nube** (sin base local): decisiones del swipe en `users/{uid}/swipes/{profileId}`, chats en `users/{uid}/chats/{contacto}` y sus mensajes en `.../messages/{autoId}` (`data/remote/UserDataStore.kt`). La caché offline de Firestore hace que todo se vea al instante y se sincronice solo al volver la red.
- **Chats**: una cuenta nueva empieza sin conversaciones; cada chat nace al hacer match o escribir, y los CoDis responden solos con mensajes genéricos al azar (`NewMessageWorker`).
- **Foto de perfil**: se comprime a 512 px JPEG y se guarda en el perfil como `data:image/jpeg;base64,...` (`ProfilePhotoEncoder`), así viaja a cualquier dispositivo sin requerir Cloud Storage (plan Blaze).
- El catálogo de 100 perfiles ficticios viaja dentro del APK (`data/catalog/Catalog.kt`).

### Configuración pendiente en Firebase Console

1. **Authentication → Método de acceso**: habilitar **Correo electrónico/contraseña** y **Google**.
2. **Configuración del proyecto → Tus apps → com.codi.app.v1**: agregar la huella **SHA-1** del keystore con que se firma la app (debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`).
3. **Volver a descargar `google-services.json`** y reemplazar `app/google-services.json` (con Google habilitado trae el cliente OAuth web; sin él, el botón de Google avisa qué falta).
4. **Firestore Database → Crear base de datos**, y en **Reglas**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## Arquitectura

```
com.codi.app
├── CoDiApplication.kt        # Crea repository/authRepository; inicializa SessionManager y ThemePreferences
├── MainActivity.kt           # Pide permiso de notificaciones; eleva el tema (claro/oscuro) a la raíz de Compose
├── data/
│   ├── Models.kt               # CoDiProfile, ChatMessage, Conversation, UserProfile (con email/gender/photoUri)
│   ├── Mappers.kt               # Entity -> modelo de dominio
│   ├── CoDiRepository.kt       # Perfiles del swipe, chat/mensajes, perfil de la cuenta con sesión activa
│   ├── AuthRepository.kt       # Firebase Auth: correo/contraseña, Google (Credential Manager), logout, cambio de contraseña
│   ├── SessionManager.kt       # uid de la sesión activa de Firebase como StateFlow
│   ├── remote/UserProfileStore.kt # Perfil propio en Cloud Firestore (users/{uid})
│   ├── ThemePreferences.kt     # Preferencia de tema claro/oscuro, persistida en SharedPreferences
│   └── catalog/Catalog.kt      # 100 perfiles ficticios (contenido de la app)
├── notifications/              # NotificationHelper + NewMessageWorker (WorkManager), igual que v2
└── ui/
    ├── navigation/              # CoDiApp (responsivo), Routes, NavHost con transiciones
    ├── theme/                   # Color.kt (tokens claro/oscuro dinámicos), Theme.kt (dos ColorScheme)
    ├── components/               # Botones, chips, avatar reutilizables
    └── screens/
        ├── login/                # LoginScreen + RegisterScreen (y sus ViewModels)
        ├── home/                 # Swipe de 100 perfiles
        ├── codi/                 # Detalle de un candidato a CoDi
        ├── match/                # "¡Es un match!"
        ├── mensajes/ , chat/      # Lista de conversaciones y chat
        ├── perfil/                # Editor de perfil completo (foto, datos, intereses)
        └── settings/              # Ajustes: apariencia, soporte, cuenta y seguridad
```

### Cómo funciona el tema claro/oscuro

`ThemePreferences` guarda la preferencia en `SharedPreferences` como un `MutableStateFlow<Boolean>`. `MainActivity` lo colecta y se lo pasa a `CoDiTheme(darkTheme = ...)`, que expone ese valor mediante `CompositionLocalProvider(LocalIsDarkTheme provides darkTheme)`. Los tokens de color que ya usaba cada pantalla (`TextPrimary`, `SurfaceCard`, `BackgroundDark`, etc., en `ui/theme/Color.kt`) se convirtieron de constantes a propiedades `@Composable` que leen ese `CompositionLocal` y devuelven el valor claro u oscuro correspondiente — así el switch de Ajustes cambia toda la app **sin tener que tocar ninguna pantalla**.

### Cómo funciona la sesión (registro/login)

- `SessionManager` escucha `FirebaseAuth.addAuthStateListener` y expone el `uid` activo como `StateFlow`. Firebase persiste la sesión, así que si no cierras sesión entras directo a Home (ver `CoDiNavHost`).
- `CoDiRepository` hace `flatMapLatest` sobre ese `uid`: perfil, mazo, matches y chats (todo en Firestore) cambian solos al cambiar de cuenta.

---

## Catálogo de 100 perfiles

`SeedData.kt` combina 5 perfiles escritos a mano (referenciados por nombre en las conversaciones semilla de Mensajes) con 95 generados de forma procedural con una semilla fija (`Random(42)`), combinando nombres, zonas, ocupaciones, horarios, presupuestos e intereses de listas variadas — así el catálogo es siempre el mismo entre instalaciones, pero con abundancia y variedad real para probar el swipe, filtros de compatibilidad y el mazo vacío/reinicio.

Los avatares siguen siendo círculos de color (`PlaceholderAvatar`) derivados de un `colorSeed`, no fotos reales — igual que en v2. La foto real de galería solo aplica a **tu propio perfil** (la cuenta con sesión activa), no al catálogo simulado.

---

## Editor de perfil (estilo Instagram)

- **Foto**: botón de cámara sobre el avatar abre el selector de imágenes del sistema (`ActivityResultContracts.GetContent()`); la imagen se comprime y se guarda en el perfil de Firestore, así que aparece en cualquier dispositivo.
- **Datos editables**: nombre, edad, ocupación, presupuesto, bio (con contador de 280 caracteres), género (chips: Mujer/Hombre/Otro/Prefiero no decir) e intereses/estilo de vida (chips multi-selección: Mascotas, No fumador, Estudio nocturno, Música/DJ, Deportes, etc.).
- Un solo botón **"Guardar cambios"**, habilitado solo si hay cambios y todos los campos son válidos; confirmación visual con ícono + texto ("Guardado en tu perfil") al terminar.

## Ajustes (estilo Instagram)

Se llega desde el ícono de engrane en la esquina superior de Perfil:

- **Apariencia**: switch de modo oscuro/claro, con efecto inmediato en toda la app.
- **Soporte y ayuda**: Centro de ayuda, Reportar un problema, Términos y condiciones — cada uno abre un diálogo informativo (simulado, sin backend de soporte real).
- **Cuenta y seguridad**: Cambiar contraseña (Firebase Auth) y un botón claro de **Cerrar sesión**, que limpia la sesión y regresa a Login.

---

## Accesibilidad y adaptabilidad

Se conservan íntegras las auditorías de v2: `contentDescription` en íconos accionables, targets táctiles ≥48dp (incluido el nuevo botón de cambiar foto), contraste verificado (≥4.5:1) —incluido el nuevo tema claro, verificado con los mismos pares de color reescalados—, y `BoxWithConstraints` para alternar entre `NavigationBar` (compacto) y `NavigationRail` (≥600dp).

---

## Landing page y APK

La landing page (con el APK para descargar) vive en la rama **`gh-pages`** de este repositorio, separada del código de la app, y se publica con GitHub Pages.

El release se firma con un keystore propio (`keystore/codi-release.jks` + `keystore.properties`). **Ninguno de los dos está en el repositorio** (ver `.gitignore`): guárdalos en un lugar seguro; sin ellos no se pueden publicar actualizaciones. La SHA-1 de esa firma debe estar registrada en Firebase para que Google Sign-In funcione en el APK de release.

## Cómo compilar

**Antes de compilar:** `app/google-services.json` no está en el repositorio. Descárgalo de Firebase Console → Configuración del proyecto → Tus apps → `com.codi.app.v1` y guárdalo en `app/`. Sin él, Gradle no puede armar la app.

Requiere JDK 17+ (probado con JDK 21).

```bash
JAVA_HOME=/ruta/a/tu/jdk-21 ./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk

JAVA_HOME=/ruta/a/tu/jdk-21 ./gradlew assembleRelease
# APK en: app/build/outputs/apk/release/app-release-unsigned.apk
```

Ambos se verificaron localmente (`clean assembleDebug assembleRelease`): compilan sin errores ni warnings del compilador de Kotlin.

---

## Guía para el video demostrativo

1. **Registro**: tocar "Crear cuenta", llenar el formulario (mostrar una validación fallando) y crear la cuenta; mostrar el documento nuevo en `users/{uid}` en Firebase Console.
2. **Login**: cerrar sesión desde Ajustes y volver a entrar con correo/contraseña (mostrar la validación en tiempo real).
3. **Google**: desde Login, tocar "Continuar con Google" y elegir una cuenta.
4. **Home**: swipe entre varios de los 100 perfiles, mostrar la animación de la tarjeta y llegar a un Match (con su notificación).
5. **Chat**: enviar un mensaje y esperar la respuesta simulada + notificación.
6. **Perfil**: tarjeta con gradiente (misma familia visual que Home), cambiar la foto, tocar el lápiz, editar nombre/bio/intereses/género y guardar; mostrar el cambio reflejado en Firestore.
7. **Ajustes**: activar el tema claro (mostrar que toda la app cambia), abrir un diálogo de ayuda, cambiar la contraseña, y por último "Cerrar sesión" (regresa a Login).

---

## Simulación de publicación en Google Play

Los metadatos son los mismos que en v2 (categoría Lifestyle, `targetSdk`/`compileSdk` 35, `minSdk` 26), salvo que el título/descripción ya no mencionan cuartos en renta:

- **Título**: CoDi — encuentra tu roomie
- **Descripción corta**: Encuentra compañeros de cuarto compatibles y chatea al hacer match.
- **Descripción completa**: CoDi te ayuda a encontrar compañero(a) de cuarto ideal en segundos. Desliza entre perfiles compatibles según presupuesto, horarios, limpieza, género y estilo de vida; cuando hay match mutuo, chatea directo en la app. Crea tu cuenta con correo o continúa con Google, personaliza tu perfil con foto y descripción, y elige el tema claro u oscuro que prefieras.
- **Política de privacidad**: tu cuenta se administra con Firebase Authentication y tu perfil, tu foto, tus matches y tus mensajes se guardan en Cloud Firestore, accesibles solo para tu cuenta; los perfiles del mazo son ficticios.

---

## Checklist de la petición de v1

| Cambio pedido | Estado |
|---|---|
| Eliminar por completo la sección de Cuartos (bottom bar, riel, rutas, pantallas, DAO, tabla) | ✅ |
| Aumentar el catálogo simulado a 100 perfiles | ✅ (5 de mano + 95 generados) |
| Autenticación y registro completo con Firebase Auth | ✅ (requiere activar los proveedores en la consola) |
| Login con Google (Credential Manager) | ✅ código listo; requiere SHA-1 + `google-services.json` actualizado |
| Perfil 100% en Cloud Firestore (`users/{uid}`) | ✅ |
| Login sin credenciales de prueba | ✅ |
| Edición completa de perfil con foto de galería, bio con contador, género e intereses | ✅ |
| Botón "Guardar cambios" con confirmación visual | ✅ |
| Pantalla de Ajustes con modo oscuro/claro real | ✅ |
| Soporte y ayuda (Centro de ayuda, Reportar un problema, Términos y condiciones) | ✅ diálogos informativos |
| Cuenta y seguridad (cambiar contraseña, cerrar sesión) | ✅ |
| Sin referencias rotas a Cuartos | ✅ verificado con búsqueda global |
| `./gradlew assembleDebug` limpio | ✅ verificado |
| README actualizado | ✅ este documento |
