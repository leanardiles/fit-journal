# FitJournal - Android Mobile App

Native Android application for FitJournal fitness tracking.

## Current Status

**Version:** 1.0 (May 2026 - Student Capstone Project)
**Status:** Core features complete — authentication, dashboard, exercises, routine builder, workout flow, profile, and testing are implemented. The Calendar screen is currently a placeholder; building it out to match the web app (workout history + per-day exercise selection) is the next major piece of work.

## Features

### Implemented ✅

#### Authentication
- JWT authentication (login + register)
- Secure token storage (Android Keystore / EncryptedSharedPreferences)
- Auto-login on app startup using stored JWT
- Logout with token clearing and navigation reset

#### Dashboard
- Quick Stats module — workouts completed this week
- Current Routine module — days per week + muscle groups per day
- Current routine day highlighted in yellow
- Refreshes on screen entry so the current day / stats stay current after changes made elsewhere
- Modular architecture for future customization

#### Exercises
- Browse exercises by muscle group
- Add, delete, and edit exercise weight
- Duplicate exercise name prevention
- Alphabetical exercise sorting
- Snackbar notifications for add/delete actions
- Retry button on error state
- Refreshes on screen entry so weight changes made while logging a workout are reflected

#### Routine Builder
- Create/edit routine (1-7 days per week)
- Assign multiple muscle groups per day
- View current routine with day-by-day breakdown

#### Workout Screen
- Auto-generates workout for current routine day
- Clears stale selections before generating
- Exercise list with muscle group, name, and weight
- Drag-to-reorder exercises (long press drag handle)
- Inline weight editing via popup dialog
- Tap to check off completed exercises (strikethrough)
- Mark Workout as Complete — logs to backend, advances routine day

#### Profile Settings
- Edit name, sex, age
- Timezone selector (28 common timezones)
- Unit preference toggle (metric/imperial) with automatic height/weight conversion
- Height displayed as feet'inches" in imperial mode

#### General
- User profile display in top bar (fetched from backend)
- OkHttp AuthInterceptor — automatic Bearer token injection on all API calls
- Auto-detection of emulator vs physical device for API URL
- Branded splash screen — notebook-style `#171717` background + kettlebell logo, via the AndroidX SplashScreen API (replaces the default white launch screen)
- Custom app launcher icon (kettlebell logo)
- Stopwatch timer (bottom sheet)
- MVI architecture throughout
- Bottom navigation (Calendar, Exercises, Home, Timer, WOD)
- Profile menu dropdown (Routine, Settings, Logout)

#### Analytics
- `AnalyticsLogger` with 12+ tracked events
- Events: `login_success`, `login_failure`, `register_success`, `workout_created`,
  `workout_completed`, `workout_error`, `exercise_added`, `exercise_deleted`,
  `exercise_weight_updated`, `routine_saved`, `screen_view`, `app_error`
- Filter Logcat by tag `FitJournalAnalytics` to view events

#### Testing
- Unit tests with 83% coverage (JaCoCo)
- 2 UI instrumented tests (login sad path + happy path navigation)
- Repository interfaces (`IUserExercisesRepository`, `IUserRoutineRepository`, `IAuthViewModel`)
  enable testability without Android context

### In Progress / Planned 🚧

- **Calendar screen** — currently a placeholder. Planned: show workout history/logs and let the user select exercises for the next workout (per day), matching the web app's calendar. The backend already supports this (same endpoints the web uses); the work is mobile-native UI/UX design plus a new screen + ViewModel.
- **Graceful session-expiry handling** — when a token expires (401), redirect to login with a friendly message rather than showing a raw error.
- **Re-theme to match the web's dark notebook aesthetic** — the app currently uses a light Material theme; the splash screen's `#171717` background is a step toward unifying with the web look.


## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVI (Model-View-Intent)
- **Database:** Room (SQLite)
- **Networking:** Retrofit 2 + Gson + OkHttp (AuthInterceptor)
- **Auth Storage:** EncryptedSharedPreferences (Android Keystore)
- **State:** StateFlow + Coroutines
- **Image Loading:** Coil + coil-gif
- **Splash:** AndroidX Core SplashScreen (`androidx.core:core-splashscreen`)
- **Drag & Drop:** sh.calvin.reorderable 2.4.0
- **Testing:** JUnit4, MockK, JaCoCo, Compose UI Testing


## Running Locally

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Minimum SDK: 24
- Compile / Target SDK: 36
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
   - Select a virtual emulator (Pixel 9, API 36 recommended)
   - Click Run (green play button)

> **Emulator:** The app auto-detects emulator and uses `10.0.2.2:8000` (emulator alias for localhost).
> **Physical device:** Set your machine's local IP in `ApiConfig.kt` under `LOCAL_IP`. Both devices must be on the same WiFi network.


## API Versioning

The backend's JSON API is versioned under a `/v1` prefix (e.g. `/v1/login`, `/v1/exercises`). All Retrofit endpoint paths in `FitJournalApiService.kt` are **relative** (no leading slash), so they append to the versioned base URL in `ApiConfig.kt` (`.../v1/`). To target a future API version, only the `BASE_URL` needs to change.


## Authentication Flow

### Login
1. User enters email and password on `LoginScreen`
2. App calls `POST /v1/login` on the backend
3. Backend returns a JWT access token + user info (including `token_type`)
4. Token is encrypted and stored via `TokenManager` (Android Keystore)
5. App navigates to HomeScreen

### Auto-Login
- On app startup, `TokenManager.isLoggedIn()` is checked
- If a token exists, the app skips the login screen and goes directly to HomeScreen

### Register
1. User switches to Register tab on `LoginScreen`
2. App calls `POST /v1/register`
3. Backend copies the default exercises to the user's account (bulk insert)
4. On success, user logs in with new credentials

### Logout
- User taps Logout from the profile dropdown in the top bar
- `TokenManager.clearAll()` removes token, userId, and email
- App navigates back to `LoginScreen` with full back stack cleared

> **Note:** The login endpoint was consolidated — the app calls the single `POST /v1/login` (a former `/login/mobile` variant was removed; `/v1/login` now returns both `message` and `token_type`).


## Workout Flow

1. User taps **Workout** in bottom navigation
2. Taps **Create Workout** — app clears stale selections and auto-generates for current day
3. Backend selects up to 3 exercises per muscle group (lowest `exercise_times_performed`, random tiebreaker)
4. Exercise list shown with muscle group tag, name, weight, and check button
5. User can drag-and-drop to reorder, tap weight to edit inline
6. User taps check circle on each completed exercise — strikethrough appears
7. User taps **Mark Workout as Complete**
8. Backend logs the session, increments exercise counts, advances to next routine day
9. App navigates back to Dashboard


## Project Structure

```
app/src/main/java/com/example/fitjournal_capstone_leandro/
├── analytics/
│   └── AnalyticsLogger.kt               # Event tracking (12+ events)
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
│   │   ├── AuthModels.kt                # All data models
│   │   └── Exercise.kt
│   ├── network/
│   │   ├── ApiConfig.kt                 # Emulator/physical device URL auto-detection + /v1 base
│   │   ├── FitJournalApiService.kt      # Retrofit interface + RetrofitClient
│   │   └── AuthInterceptor.kt           # OkHttp Bearer token injection
│   └── repository/
│       ├── AuthRepository.kt
│       ├── UserExercisesRepository.kt
│       ├── IUserExercisesRepository.kt  # Interface for testability
│       ├── UserRoutineRepository.kt
│       ├── IUserRoutineRepository.kt    # Interface for testability
│       ├── DashboardRepository.kt
│       └── WorkoutRepository.kt
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   ├── AuthViewModel.kt
│   │   └── IAuthViewModel.kt            # Interface for UI testing
│   ├── home/
│   │   ├── HomeScreen.kt                # Dashboard with module cards
│   │   ├── HomeViewModel.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── HomeScreenState.kt
│   │   ├── HomeScreenAction.kt
│   │   └── HomeScreenReducer.kt
│   ├── exercises/
│   │   ├── UserExercisesScreen.kt
│   │   └── UserExercisesViewModel.kt
│   ├── workout/
│   │   ├── WorkoutScreen.kt
│   │   └── WorkoutViewModel.kt
│   ├── routine/
│   │   ├── RoutineScreen.kt
│   │   └── RoutineViewModel.kt
│   ├── profile/
│   │   ├── ProfileSettingsScreen.kt
│   │   └── ProfileSettingsViewModel.kt
│   ├── calendar/
│   │   └── CalendarScreen.kt            # Placeholder — full build-out planned
│   ├── stopwatch/
│   │   ├── StopwatchViewModel.kt
│   │   └── StopwatchBottomSheet.kt
│   └── shared/
│       ├── ProfileTopBar.kt
│       └── BottomNavBar.kt
├── navigation/
│   ├── Navigation.kt
│   └── Routes.kt
└── MainActivity.kt
```


## API Configuration

Controlled via `data/network/ApiConfig.kt`:

```kotlin
const val IS_DEVELOPMENT = false
private const val LOCAL_IP = "192.168.x.x"  // Your machine's IP for physical device

val BASE_URL = when {
    !IS_DEVELOPMENT -> "https://app.fit-journal.com/v1/"
    isEmulator() -> "http://10.0.2.2:8000/v1/"
    else -> "http://$LOCAL_IP:8000/v1/"
}
```

The base URL includes the `/v1/` API version prefix; since all Retrofit paths are relative, they append to it automatically. The `isEmulator()` function detects emulator vs physical device automatically using Android `Build` properties (including `ranchu` hardware for Pixel 9 AVD).


## Network Security

`res/xml/network_security_config.xml` allows cleartext HTTP for development:

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">10.0.2.2</domain>
    <domain includeSubdomains="true">192.168.x.x</domain>
</domain-config>
```


## Running Tests

```bash
cd mobile

# Unit tests + JaCoCo coverage report
./gradlew testDebugUnitTest jacocoTestReport

# Open coverage report
start app/build/reports/jacoco/jacocoTestReport/html/index.html

# UI instrumented tests (requires running emulator)
./gradlew connectedDebugAndroidTest
```


## Key Files

| File | Purpose |
|------|---------|
| `TokenManager.kt` | Encrypts/decrypts JWT using Android Keystore (AES256-GCM) |
| `AuthInterceptor.kt` | Attaches `Authorization: Bearer {token}` to every API request |
| `ApiConfig.kt` | Auto-detects emulator vs physical device, switches API URL (includes `/v1` base) |
| `AuthModels.kt` | All data classes matching FastAPI JSON contract |
| `FitJournalApiService.kt` | Retrofit interface — all backend endpoints (relative paths) |
| `AnalyticsLogger.kt` | Centralized event tracking (tag: FitJournalAnalytics) |
| `DashboardViewModel.kt` | Fetches routine + workout sessions for Dashboard |
| `WorkoutViewModel.kt` | Auto-generates workout, tracks completion, calls backend |
| `IUserExercisesRepository.kt` | Interface enabling unit testing without Android context |
| `IUserRoutineRepository.kt` | Interface enabling unit testing without Android context |
| `IAuthViewModel.kt` | Interface enabling UI testing without real backend |


## Troubleshooting

**"Failed to connect to 10.0.2.2:8000"**
- Make sure the FastAPI backend is running: `uvicorn main:app --reload --host 0.0.0.0`
- Make sure you are running on a virtual emulator

**"Failed to connect to 192.168.x.x:8000" (physical device)**
- Confirm both devices are on the same WiFi network
- Disable VPN on your computer
- Verify your IP in `ApiConfig.kt` matches `ipconfig` output

**isEmulator() returns false on emulator**
- The `isEmulator()` function checks `ranchu` hardware (Pixel 9 AVD)
- Add a log in `ApiConfig.kt` to inspect `Build.HARDWARE` if needed

**API returns 404 (Not Found)**
- The API is versioned under `/v1` — confirm `BASE_URL` ends with `/v1/` and Retrofit paths are relative (no leading slash)

**API returns 403 Forbidden**
- Token may be missing or expired — log out and log back in
- The backend enforces ownership checks: a request's `user_id` must match the authenticated user
- Verify `RetrofitClient.initialize(tokenManager)` is called in `MainActivity`

**Workout shows wrong exercises (stale selections)**
- The workout screen clears all selections before auto-generating
- Check `DELETE /v1/next-workout/clear/{user_id}` in backend logs


## Author

**Leandro Ardiles**

---

*Last Updated: May 2026*