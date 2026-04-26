# FitJournal - Android Mobile App

Native Android application for FitJournal fitness tracking.

## Current Status

**Version:** 0.7 (April 2026 - Student Project)
**Status:** Development — JWT authentication and exercises backend integration complete

## Features

### Implemented ✅
- JWT authentication (login + register)
- Secure token storage (Android Keystore / EncryptedSharedPreferences)
- Auto-login on app startup using stored JWT
- Logout with token clearing and navigation reset
- User profile display in top bar (fetched from backend)
- OkHttp AuthInterceptor — automatic Bearer token injection on all API calls
- User exercises from backend (browse by muscle group, add, delete)
- Duplicate exercise name prevention
- Alphabetical exercise sorting
- Snackbar notifications for add/delete actions
- Calendar UI with DatePicker
- Stopwatch timer
- MVI architecture
- Profile menu with dropdown
- Bottom navigation (Calendar, Exercises, Home, Timer, WOD)

### In Development 🚧
- Workout logging
- Routine builder
- WOD (Workout of the Day) screen
- Offline sync with backend

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVI (Model-View-Intent)
- **Database:** Room (SQLite)
- **Networking:** Retrofit 2 + Gson + OkHttp (AuthInterceptor)
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

2. **Start the FastAPI backend** (required for all features):
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

### Auto-Login
- On app startup, `TokenManager.isLoggedIn()` is checked
- If a token exists, the app skips the login screen and goes directly to HomeScreen

### Register
1. User switches to Register tab on the LoginScreen
2. App calls `POST /register`
3. On success, user is prompted to log in with their new credentials

### Logout
- User taps Log out from the profile dropdown in the top bar
- `TokenManager.clearAll()` removes token, userId, and email
- App navigates back to LoginScreen with full back stack cleared
- No API call needed — JWT is stateless

## Exercises Flow

1. User navigates to the Exercises tab
2. App fetches user's exercises from `GET /exercises?user_id={id}`
3. Muscle groups are derived from the user's exercise list
4. User selects a muscle group from the dropdown
5. Exercises for that group are displayed alphabetically
6. User can add a new exercise (duplicate names blocked)
7. User can delete an exercise with confirmation dialog
8. Snackbar confirms add/delete actions

## Project Structure

```
app/src/main/java/com/example/fitjournal_capstone_leandro/
├── data/
│   ├── local/
│   │   ├── TokenManager.kt              # Encrypted JWT storage
│   │   ├── FitJournalDatabase.kt        # Room database
│   │   ├── ExerciseDao.kt
│   │   ├── MuscleDao.kt
│   │   ├── ExerciseEntity.kt
│   │   ├── MuscleEntity.kt
│   │   └── Converters.kt
│   ├── model/
│   │   ├── AuthModels.kt                # Login/Register models, UserProfile, UserExercise
│   │   └── Exercise.kt
│   ├── network/
│   │   ├── ApiConfig.kt                 # Dev/prod URL switching
│   │   ├── FitJournalApiService.kt      # Retrofit interface + RetrofitClient
│   │   └── AuthInterceptor.kt           # OkHttp interceptor for Bearer token
│   └── repository/
│       ├── AuthRepository.kt            # Auth + profile data layer
│       └── UserExercisesRepository.kt   # Exercises CRUD data layer
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt               # Login + Register Compose screen
│   │   └── AuthViewModel.kt             # Auth + profile state management
│   ├── home/
│   ├── exercises/
│   │   ├── UserExercisesScreen.kt       # Exercises screen with CRUD
│   │   └── UserExercisesViewModel.kt    # Exercises state management
│   ├── exercise_details/
│   ├── calendar/
│   ├── stopwatch/
│   └── shared/
│       ├── ProfileTopBar.kt             # Top bar with user name + dropdown
│       └── BottomNavBar.kt              # Bottom navigation bar
├── navigation/
│   ├── Navigation.kt                    # NavHost with auto-login routing
│   └── Routes.kt                        # Route string constants
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

## Key Files

| File | Purpose |
|------|---------|
| `TokenManager.kt` | Encrypts/decrypts JWT using Android Keystore (AES256-GCM) |
| `AuthInterceptor.kt` | Attaches `Authorization: Bearer {token}` to every API request |
| `AuthModels.kt` | Data classes matching the FastAPI JSON contract |
| `FitJournalApiService.kt` | Retrofit interface — all backend endpoints |
| `AuthRepository.kt` | Auth operations and profile fetch, returns `Result<T>` |
| `UserExercisesRepository.kt` | Exercise CRUD operations against backend |
| `AuthViewModel.kt` | Exposes `AuthUiState` and `userProfile` to the UI |
| `UserExercisesViewModel.kt` | Exercises state — fetch, add, delete, sort |
| `LoginScreen.kt` | Compose screen with Login/Register tab toggle |
| `UserExercisesScreen.kt` | Exercises screen with dropdown, list, add/delete dialogs |
| `Routes.kt` | Centralized route string constants |

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

**API returns 401 Unauthorized**
- Token may have expired — log out and log back in
- Verify `RetrofitClient.initialize(tokenManager)` is called in `MainActivity`

## Author

**Leandro Ardiles**
- MS Computer Science, Yeshiva University (Katz School)
- Course: COM5210 Mobile Application Development

## AI Assistance

This mobile app was developed with the assistance of Claude.ai (Anthropic) for:
- Android architecture guidance (MVI, Jetpack Compose)
- JWT authentication implementation
- Backend integration (exercises CRUD, profile fetch)
- OkHttp interceptor for automatic token injection
- Bug fixing and debugging

---

*Last Updated: 26 April 2026*