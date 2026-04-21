# FitJournal - Android Mobile App

Native Android application for FitJournal fitness tracking.

## Current Status

**Version:** 0.6 (April 2026 - Student Project)
**Status:** Development — JWT authentication complete, backend integration in progress

## Features

### Implemented ✅
- JWT authentication (login + register)
- Secure token storage (Android Keystore / EncryptedSharedPreferences)
- Exercise browsing by muscle group (ExerciseDB API)
- Exercise details with instructions and GIFs
- Offline-first caching (Room database)
- Calendar UI with DatePicker
- Stopwatch timer
- MVI architecture (Home & Exercises screens)
- Profile menu with dropdown
- Bottom navigation

### In Development 🚧
- Auto-login on app startup (check stored token)
- Logout wired to ProfileTopBar button
- OkHttp interceptor for Bearer token injection
- Workout logging
- Routine builder
- Offline sync with backend

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVI (Model-View-Intent)
- **Database:** Room (SQLite)
- **Networking:** Retrofit 2 + Gson
- **Auth Storage:** EncryptedSharedPreferences (Android Keystore)
- **State:** StateFlow + Coroutines

## Running Locally

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Minimum SDK: 24
- Target SDK: 37
- FastAPI backend running locally

### Setup

1. **Open in Android Studio:**
   - File → Open → Select the `mobile` folder
   - Wait for Gradle sync to complete

2. **Start the FastAPI backend** (required for login/register):
```bash
cd ../src
source ../venv/Scripts/activate
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

3. **Run the app:**
   - Select a virtual emulator (Pixel 9, API 37 recommended)
   - Click Run (green play button)

> **Important:** Use a virtual emulator, not a physical device. The app connects to the backend via `10.0.2.2:8000`, which is the emulator's alias for your machine's localhost. Physical devices cannot use this address.

## Authentication Flow

The app uses JWT (JSON Web Token) authentication against the FastAPI backend.

### Login
1. User enters email and password on the LoginScreen
2. App calls `POST /login/mobile` on the backend
3. Backend returns a JWT access token + user info
4. Token is encrypted and stored via `TokenManager` (Android Keystore)
5. App navigates to HomeScreen

### Register
1. User switches to Register tab on the LoginScreen
2. App calls `POST /register`
3. On success, user is prompted to log in with their new credentials

### Logout *(coming soon)*
- Calls `TokenManager.clearAll()` to remove token, userId, and email
- Navigates back to LoginScreen
- No API call needed — JWT is stateless

## Project Structure

```
app/src/main/java/com/example/fitjournal_capstone_leandro/
├── data/
│   ├── local/
│   │   ├── TokenManager.kt          # Encrypted JWT storage
│   │   ├── FitJournalDatabase.kt    # Room database
│   │   ├── ExerciseDao.kt
│   │   ├── MuscleDao.kt
│   │   ├── ExerciseEntity.kt
│   │   ├── MuscleEntity.kt
│   │   └── Converters.kt
│   ├── model/
│   │   ├── AuthModels.kt            # LoginRequest/Response, RegisterRequest/Response, User
│   │   └── Exercise.kt
│   ├── network/
│   │   ├── ApiConfig.kt             # Dev/prod URL switching
│   │   ├── FitJournalApiService.kt  # Retrofit interface + client
│   │   └── ApiService.kt            # ExerciseDB API service
│   └── repository/
│       ├── AuthRepository.kt        # Auth data layer
│       └── ExerciseRepository.kt
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt           # Login + Register Compose screen
│   │   └── AuthViewModel.kt         # Auth state management
│   ├── home/
│   ├── exercises/
│   ├── exercise_details/
│   ├── calendar/
│   ├── stopwatch/
│   └── shared/
├── navigation/
│   └── navigation.kt                # NavHost with login as start destination
└── MainActivity.kt
```

## API Configuration

Controlled via `data/network/ApiConfig.kt`:

```kotlin
// Toggle between local development and production
const val IS_DEVELOPMENT = true

val BASE_URL = if (IS_DEVELOPMENT) {
    "http://10.0.2.2:8000/"   // Emulator → localhost
} else {
    "https://api.fitjournal.com/"  // Production
}
```

Switch `IS_DEVELOPMENT = false` when deploying to production.

## Network Security

Android blocks cleartext HTTP by default. A security config at
`res/xml/network_security_config.xml` explicitly allows HTTP to `10.0.2.2` for development:

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">10.0.2.2</domain>
</domain-config>
```

This is development-only. Production deployment with HTTPS requires no special config.

## Key Files — Authentication

| File | Purpose |
|------|---------|
| `TokenManager.kt` | Encrypts/decrypts token using Android Keystore (AES256-GCM) |
| `AuthModels.kt` | Data classes matching the FastAPI JSON contract |
| `FitJournalApiService.kt` | Retrofit interface with `login()` and `register()` endpoints |
| `AuthRepository.kt` | Coordinates API calls and token storage, returns `Result<T>` |
| `AuthViewModel.kt` | Exposes `AuthUiState` (Idle/Loading/Success/Error) to the UI |
| `LoginScreen.kt` | Compose screen with Login/Register tab toggle |

## Troubleshooting

**"Failed to connect to 10.0.2.2:8000"**
- Make sure the FastAPI backend is running: `uvicorn main:app --reload --host 0.0.0.0`
- Make sure you are running on a virtual emulator, not a physical device

**"CLEARTEXT communication not permitted"**
- Verify `res/xml/network_security_config.xml` exists
- Verify `AndroidManifest.xml` references it via `android:networkSecurityConfig`

**Emulator has no internet**
- In Android Studio: Device Manager → three dots next to emulator → Cold Boot Now

**"Out of date" warning after changing package names**
- Build → Rebuild Project to clear the incremental build cache

## Author

**Leandro Ardiles**
- MS Computer Science, Yeshiva University (Katz School)
- Course: COM5210 Mobile Application Development

## AI Assistance

This mobile app was developed with the assistance of Claude.ai (Anthropic) for:
- Android architecture guidance (MVI, Jetpack Compose)
- JWT authentication implementation
- Room database and offline-first caching
- Bug fixing and debugging

---

*Last Updated: April 2026*