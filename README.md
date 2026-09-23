# CoDi — encuentra a tu roomie ideal

**CoDi** (**co**mpañeros de **di**vidir cuarto) es una app de Android para encontrar roomies compatibles. Funciona como Tinder: deslizas entre perfiles, haces match y chateas dentro de la app.

Esta es la **v1**, que solo cubre dos cosas: descubrir roomies y administrar tu cuenta y tu perfil.

<!-- Agrega aquí capturas de pantalla o un GIF de la app -->

---

## Características

- **Swipe entre 100 perfiles** con filtros de compatibilidad por presupuesto, horarios, limpieza, género y estilo de vida.
- **Match y chat**: cada conversación empieza con un match mutuo. Los perfiles del catálogo contestan solos.
- **Cuentas con Firebase**: registro con correo y contraseña, inicio de sesión y **Continuar con Google**.
- **Editor de perfil estilo Instagram**: foto de la galería, datos personales, bio, género y chips de intereses.
- **Ajustes**: tema claro/oscuro que se guarda, soporte y ayuda, cambio de contraseña y cierre de sesión.
- **Datos en la nube**: el perfil, los swipes y los chats viven en Cloud Firestore y siguen funcionando sin conexión gracias a su caché.
- **Notificaciones locales** de mensajes nuevos con WorkManager.
- **Accesible y adaptable**: descripciones de contenido, áreas táctiles de ≥ 48 dp, contraste ≥ 4.5:1 en los dos temas y navegación que se ajusta al tamaño de pantalla.

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje / UI | Kotlin, Jetpack Compose, Material 3 |
| Navegación | Navigation Compose |
| Autenticación | Firebase Authentication (correo/contraseña y Google con Credential Manager) |
| Datos | Cloud Firestore con caché offline |
| Tareas en segundo plano | WorkManager |
| SDK | `minSdk` 26 · `targetSdk` / `compileSdk` 35 |

---

## Requisitos

- Android Studio o Gradle desde la terminal
- JDK 17 o más reciente (probado con JDK 21)
- Un proyecto de Firebase propio (ver abajo)

## Configurar Firebase

`app/google-services.json` **no se incluye en el repositorio**. Para compilar la app necesitas tu propio proyecto de Firebase:

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/) y registra una app Android con el paquete `com.codi.app.v1`.
2. En **Authentication → Método de acceso**, habilita **Correo electrónico/contraseña** y **Google**.
3. En **Configuración del proyecto → Tus apps**, agrega la huella **SHA-1** del keystore que firma la app. Para el keystore de debug:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
4. Descarga `google-services.json` **después** de habilitar Google, para que incluya el cliente OAuth web, y colócalo en `app/`. Si ese cliente falta, el botón de Google le dice al usuario qué falta.
5. En **Firestore Database**, crea la base de datos y publica estas reglas:

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

## Compilar

```bash
# Debug
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release
./gradlew assembleRelease
# → app/build/outputs/apk/release/
```

Si tu `java` por defecto no es compatible con Gradle, apunta `JAVA_HOME` a un JDK 17–21:

```bash
JAVA_HOME=/ruta/a/jdk-21 ./gradlew assembleDebug
```

### Firma de release

Para firmar el release, el build lee `keystore.properties` en la raíz del proyecto, que apunta a un keystore en `keystore/`. Ninguno de los dos archivos está en el repositorio. Si no existen, `assembleRelease` genera un APK sin firmar.

---

## Arquitectura

```
com.codi.app
├── CoDiApplication.kt          # Crea los repositorios; inicializa SessionManager y ThemePreferences
├── MainActivity.kt             # Permiso de notificaciones; aplica el tema en la raíz de Compose
├── data/
│   ├── Models.kt               # CoDiProfile, ChatMessage, Conversation, UserProfile
│   ├── Mappers.kt              # Conversión a modelos de dominio
│   ├── CoDiRepository.kt       # Mazo de swipe, matches, chats y perfil de la sesión activa
│   ├── AuthRepository.kt       # Firebase Auth: correo/contraseña, Google, logout, cambio de contraseña
│   ├── SessionManager.kt       # uid de la sesión activa como StateFlow
│   ├── ThemePreferences.kt     # Preferencia claro/oscuro guardada en SharedPreferences
│   ├── remote/
│   │   ├── UserProfileStore.kt # Perfil en Firestore (users/{uid})
│   │   └── UserDataStore.kt    # Swipes y chats en Firestore
│   └── catalog/Catalog.kt      # Catálogo de 100 perfiles ficticios
├── notifications/              # NotificationHelper + NewMessageWorker (WorkManager)
└── ui/
    ├── navigation/             # CoDiApp (responsivo), rutas y NavHost con transiciones
    ├── theme/                  # Tokens de color dinámicos y ColorScheme claro/oscuro
    ├── components/             # Botones, chips y avatar reutilizables
    └── screens/
        ├── login/              # Login y registro
        ├── home/               # Swipe
        ├── codi/               # Detalle de un perfil
        ├── match/              # Pantalla de "¡Es un match!"
        ├── mensajes/, chat/    # Lista de conversaciones y chat
        ├── perfil/             # Editor de perfil
        └── settings/           # Ajustes
```

### Modelo de datos en Firestore

```
users/{uid}                          # Perfil del usuario
├── swipes/{profileId}               # Decisión de swipe sobre un perfil del catálogo
└── chats/{contacto}                 # Conversación
    └── messages/{autoId}            # Mensajes
```

El documento de perfil guarda estos campos: `firstName`, `lastName`, `birthDate` (Long, milisegundos UTC), `email`, `occupation`, `city`, `bio`, `gender`, `budget`, `photoUri` e `interests`.

### Autenticación

- **Correo y contraseña**: usa `createUserWithEmailAndPassword` y `signInWithEmailAndPassword`. Si el perfil no se puede guardar en Firestore durante el registro, la cuenta recién creada se borra para que no queden cuentas sin perfil.
- **Google**: flujo nativo con Credential Manager (`GetGoogleIdOption` → `GoogleAuthProvider.getCredential`). El `serverClientId` es el `default_web_client_id` que genera el plugin de Google Services, así que no hay IDs escritos en el código. En el primer inicio de sesión se crea el perfil con el nombre de la cuenta de Google.
- **Sesión**: `SessionManager` escucha `FirebaseAuth.addAuthStateListener`. Firebase guarda la sesión entre aperturas, así que un usuario con sesión activa entra directo a Home. `CoDiRepository` aplica `flatMapLatest` sobre el `uid`, y así el perfil, el mazo, los matches y los chats cambian solos al cambiar de cuenta.
- **Cambio de contraseña**: `FirebaseUser.updatePassword`. No aplica a cuentas de Google.

### Foto de perfil

La imagen se elige con `ActivityResultContracts.GetContent()`, se reduce a 512 px en JPEG y se guarda en el perfil como `data:image/jpeg;base64,...` (`ProfilePhotoEncoder`). Así aparece en cualquier dispositivo sin depender de Cloud Storage.

### Tema claro/oscuro

`ThemePreferences` expone la preferencia como un `StateFlow<Boolean>`. `MainActivity` la recolecta y se la pasa a `CoDiTheme(darkTheme = ...)`, que la publica en `LocalIsDarkTheme`. Los tokens de color (`TextPrimary`, `SurfaceCard`, `BackgroundDark`, etc.) son propiedades `@Composable` que leen ese `CompositionLocal`. Por eso el cambio de tema llega a toda la app sin que cada pantalla tenga que manejarlo.

### Catálogo de perfiles

El catálogo trae 100 perfiles ficticios dentro del APK: 5 escritos a mano y 95 generados con una semilla fija (`Random(42)`) a partir de listas de nombres, zonas, ocupaciones, horarios, presupuestos e intereses. Siempre sale el mismo entre instalaciones y alcanza para probar el swipe, los filtros y el reinicio del mazo vacío.

Los avatares del catálogo son círculos de color que salen de un `colorSeed`. Solo el perfil del usuario usa una foto real.

### Chats

Una cuenta nueva empieza sin conversaciones. Cada chat se crea al hacer match o al escribir, y `NewMessageWorker` manda respuestas automáticas de los perfiles del catálogo junto con su notificación local.

---

## Landing page

La landing page, con el APK para descargar, está en la rama [`gh-pages`](../../tree/gh-pages) y se publica con GitHub Pages.

## Ficha de Google Play

- **Título**: CoDi — encuentra tu roomie
- **Categoría**: Estilo de vida
- **Descripción corta**: Encuentra compañeros de cuarto compatibles y chatea al hacer match.
- **Descripción completa**: CoDi te ayuda a encontrar a tu compañero(a) de cuarto ideal en segundos. Desliza entre perfiles compatibles según presupuesto, horarios, limpieza, género y estilo de vida; cuando hay match mutuo, chatea directo en la app. Crea tu cuenta con correo o continúa con Google, personaliza tu perfil con foto y descripción, y elige el tema claro u oscuro.

## Privacidad

Las cuentas se administran con Firebase Authentication. El perfil, la foto, los matches y los mensajes de cada usuario se guardan en Cloud Firestore, y las reglas de seguridad limitan el acceso a su propia cuenta. Los perfiles del mazo son ficticios.

