# FitJournal

A no-frills fitness tracker — web application + native Android mobile app — that feels like your paper notebook.

## Project Overview

FitJournal is a comprehensive fitness tracking application that provides a complete workout management system with routine planning, exercise tracking, and workout history visualization — available both as a web app and a native Android app.

## Features

### Web App
- ✅ User registration and authentication (JWT)
- ✅ User profile management (age, weight, height, unit preferences, timezone)
- ✅ 101 default exercises (copied to each user on registration via bulk insert)
- ✅ Exercise management with muscle group organization
- ✅ Custom routine builder (1-7 days per week, multiple muscle groups per day)
- ✅ Intelligent workout generation algorithm (3 exercises per muscle group, random tiebreaker)
- ✅ Daily workout tracking (Get WOD - Workout of the Day)
- ✅ Workout history calendar with multi-day filtering
- ✅ Automatic exercise weight tracking and updates
- ✅ Session-based workout logging
- ✅ Progress tracking by exercise frequency
- ✅ Timezone-aware workout date logging (per user timezone)

### Android Mobile App
- ✅ JWT authentication (login + register)
- ✅ Secure token storage (Android Keystore / EncryptedSharedPreferences)
- ✅ Auto-login on app startup using stored JWT
- ✅ Logout with token clearing and navigation reset
- ✅ User profile display in top bar (fetched from backend)
- ✅ OkHttp AuthInterceptor — automatic Bearer token injection on all API calls
- ✅ Auto-detection of emulator vs physical device for API URL
- ✅ Dashboard with Quick Stats (workouts this week) and Current Routine modules
- ✅ Current routine day highlighted in yellow on Dashboard
- ✅ User exercises — browse by muscle group, add, delete, edit weight
- ✅ Duplicate exercise name prevention
- ✅ Alphabetical exercise sorting
- ✅ Routine builder (1-7 days, multiple muscle groups per day)
- ✅ Workout screen — auto-generates workout for current day
- ✅ Drag-to-reorder exercises in workout list
- ✅ Inline weight editing from workout screen (popup dialog)
- ✅ Exercise completion tracking (tap to check off, strikethrough)
- ✅ Mark workout as complete — logs to backend, advances routine day
- ✅ Profile settings (name, sex, age, timezone, unit preference, height, weight)
- ✅ Timezone selector (28 common timezones)
- ✅ Unit preference (metric/imperial) with height/weight conversion
- ✅ Calendar UI with DatePicker
- ✅ Stopwatch timer
- ✅ MVI architecture
- ✅ Bottom navigation + profile menu
- ✅ Custom app launcher icon (kettlebell logo)
- ✅ AnalyticsLogger — 12+ event tracking (auth, workout, exercise, routine)
- ✅ Unit tests — 83% coverage (JaCoCo)
- ✅ UI tests — 2 instrumented tests (login sad path + happy path navigation)


## Tech Stack

### Backend
- **Framework:** FastAPI (Python)
- **ORM:** SQLAlchemy 2.0
- **Database:** MySQL (hosted on Aiven)
- **Authentication:** Bcrypt password hashing + JWT tokens (web + mobile)
- **Database Driver:** PyMySQL
- **Timezone:** pytz

### Web Frontend
- **CSS Framework:** PaperCSS (dark mode)
- **JavaScript:** Vanilla JS (ES6+)
- **HTTP Server:** Python http.server (development)
- **UI Design:** Custom dark theme (#171717 background)

### Android Mobile App
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVI (Model-View-Intent)
- **Database:** Room (SQLite)
- **Networking:** Retrofit 2 + Gson + OkHttp (AuthInterceptor)
- **Auth Storage:** EncryptedSharedPreferences (Android Keystore)
- **State:** StateFlow + Coroutines
- **Image Loading:** Coil (with GIF support)
- **Drag & Drop:** sh.calvin.reorderable 2.4.0
- **Testing:** JUnit4, MockK, JaCoCo, Compose UI Testing


## Project Structure
```
FitJournal/
├── src/                          # Backend source code
│   ├── main.py                   # FastAPI application & routes
│   ├── database.py               # SQLAlchemy database connection
│   ├── models.py                 # SQLAlchemy ORM models
│   ├── schemas.py                # Pydantic validation schemas
│   ├── .env                      # Environment variables (not in repo)
│   └── venv/                     # Virtual environment (not in repo)
├── frontend/                     # Web frontend
│   ├── index.html
│   ├── login.html
│   ├── registration.html
│   ├── dashboard.html
│   ├── profile.html
│   ├── exercises.html
│   ├── routine.html
│   ├── calendar.html
│   ├── getwod.html
│   ├── css/
│   │   ├── paper.css
│   │   └── style.css
│   ├── js/
│   │   └── api.js
│   └── images/
│       └── logo_only.png
├── mobile/                       # Android mobile app
│   └── app/src/main/java/.../
│       ├── analytics/            # AnalyticsLogger
│       ├── data/
│       │   ├── local/            # Room DB, TokenManager
│       │   ├── model/            # Data classes incl. AuthModels, UserExercise
│       │   ├── network/          # Retrofit, ApiConfig, FitJournalApiService, AuthInterceptor
│       │   └── repository/       # AuthRepository, UserExercisesRepository,
│       │                         # UserRoutineRepository, DashboardRepository,
│       │                         # WorkoutRepository, IUserExercisesRepository,
│       │                         # IUserRoutineRepository
│       ├── ui/
│       │   ├── auth/             # LoginScreen, AuthViewModel, IAuthViewModel
│       │   ├── home/             # HomeScreen, HomeViewModel, DashboardViewModel
│       │   ├── exercises/        # UserExercisesScreen, UserExercisesViewModel
│       │   ├── workout/          # WorkoutScreen, WorkoutViewModel
│       │   ├── routine/          # RoutineScreen, RoutineViewModel
│       │   ├── profile/          # ProfileSettingsScreen, ProfileSettingsViewModel
│       │   ├── calendar/         # CalendarScreen
│       │   ├── stopwatch/        # StopwatchViewModel, StopwatchBottomSheet
│       │   └── shared/           # ProfileTopBar, BottomNavBar
│       ├── navigation/           # Navigation.kt, Routes.kt
│       └── MainActivity.kt
├── docs/
│   └── ER_diagram.png
├── requirements.txt
├── README.md
└── README_mobile.md
```

## Setup Instructions

### Prerequisites
- Python 3.8+
- MySQL database (or Aiven account)
- Git
- Modern web browser
- Android Studio Hedgehog (2023.1.1) or later *(for mobile)*

### Backend Setup

1. Clone the repository:
```bash
git clone https://github.com/leanardiles/FitJournal.git
cd FitJournal
```

2. Create and activate virtual environment:
```bash
python -m venv venv

# Git Bash
source venv/Scripts/activate

# Mac /Linux
source venv/bin/activate

# Windows PowerShell
venv\Scripts\Activate.ps1

# Windows CMD
venv\Scripts\activate.bat
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Create `.env` file in `src/` folder:
```env
DB_HOST=your-aiven-host.aivencloud.com
DB_PORT=12345
DB_USER=avnadmin
DB_PASSWORD=your-password
DB_NAME=fitjournalDB
```

5. Test database connection:
```bash
cd src
python database.py
```

6. Run the FastAPI application:
```bash
cd src
uvicorn main:app --reload
```

7. Access API documentation at: `http://127.0.0.1:8000/docs`

### Web Frontend Setup

1. Open a **new terminal** (keep backend running in the first one)

```bash
cd frontend
python -m http.server 8080
```

2. Access the application at: `http://localhost:8080/login.html`

### Mobile App Setup

See **README_mobile.md** for full Android setup instructions.

**Quick start:**
1. Open the `mobile/` folder in Android Studio
2. Start the FastAPI backend: `uvicorn main:app --reload --host 0.0.0.0`
3. Run on a virtual emulator (Pixel 9, API 37 recommended)

> **Note:** The mobile app auto-detects emulator vs physical device. Emulator uses `10.0.2.2:8000`. Physical devices require the machine's local IP address set in `ApiConfig.kt`.

### Test User
You can use the following test user credentials:
- Email: test@test.com
- Password: testUSER123!


## API Endpoints

### Authentication
- `POST /register` — Register new user
- `POST /login` — Web login (returns JWT access token + user info)
- `POST /login/mobile` — Mobile login (returns JWT access token)

### Profile
- `GET /profile/{user_id}` — Get user profile *(JWT required)*
- `PUT /profile/{user_id}` — Update user profile *(JWT required)*

### Exercises
- `GET /exercises?user_id={id}` — Get all exercises for user *(JWT required)*
- `POST /exercises?user_id={id}` — Create new exercise *(JWT required)*
- `PUT /exercises/{exercise_id}?user_id={id}` — Update exercise *(JWT required)*
- `DELETE /exercises/{exercise_id}?user_id={id}` — Delete exercise *(JWT required)*

### Routines
- `GET /routine/{user_id}` — Get user's routine *(JWT required)*
- `POST /routine/{user_id}` — Create/update routine *(JWT required)*
- `DELETE /routine/{user_id}` — Delete routine *(JWT required)*

### Workout State
- `GET /workout/state/{user_id}` — Get current workout state *(JWT required)*

### Workout Sessions & Logs
- `POST /workout/complete/{user_id}` — Complete workout *(JWT required)*
- `GET /workout/sessions/{user_id}?limit={n}` — Get last N sessions *(JWT required)*
- `POST /workout/logs-by-sessions/{user_id}` — Get logs for specific sessions *(JWT required)*
- `GET /workout/logs/{user_id}?limit={n}` — Get workout logs *(JWT required)*

### Next Workout Management
- `GET /next-workout/selections/{user_id}` — Get selected exercises *(JWT required)*
- `POST /next-workout/toggle` — Toggle exercise selection *(JWT required)*
- `POST /next-workout/generate/{user_id}?day_number={n}` — Auto-generate workout *(JWT required)*
- `DELETE /next-workout/clear/{user_id}` — Clear all selections *(JWT required)*
- `DELETE /next-workout/clear/{user_id}?day_number={n}` — Clear selections for a day *(JWT required)*

### Default Exercises
- `GET /default-exercises` — Get all 101 default exercises


## Database Schema

### Tables

#### `users`
- `user_id` (PK) - Auto-increment user ID
- `user_email` - Unique email address
- `user_password` - Bcrypt hashed password
- `user_first_name`, `user_last_name` - Optional profile fields
- `user_sex` - ENUM('M', 'F', 'NB')
- `user_age` - Integer (0-120)
- `user_unit_preference` - ENUM('metric', 'imperial')
- `user_weight`, `user_height` - Decimal/Integer (always stored in metric)
- `user_timezone` - VARCHAR(50), default 'America/New_York'
- `user_subscription` - TINYINT (0 or 1)
- `user_is_active` - Boolean
- `user_created_at`, `user_updated_at` - Timestamps

#### `default_exercises`
- `default_exercise_id` (PK)
- `exercise_name` - VARCHAR(50)
- `exercise_muscle_group` - ENUM (9 muscle groups)
- `exercise_link` - VARCHAR(500) - URL to exercise demo

#### `exercises`
- `exercise_id` (PK)
- `exercise_name` - VARCHAR(50)
- `exercise_muscle_group` - ENUM (9 muscle groups)
- `exercise_user_current_weight` - DECIMAL(5,2)
- `user_id` (FK → users.user_id)
- `exercise_is_in_routine` - Boolean
- `exercise_times_performed` - Integer (auto-increments on workout completion)
- `exercise_link` - VARCHAR(500)
- `comments` - VARCHAR(300)
- `exercise_created_at`, `exercise_updated_at` - Timestamps

#### `routine_days`
- `routine_id` (PK)
- `user_id` (FK → users.user_id)
- `days_per_week` - Integer (1-7)
- `created_at`, `updated_at` - Timestamps

#### `routine_muscles_per_day`
- `routine_day_id` (PK)
- `user_id` (FK → users.user_id)
- `day_number` - Integer (1-7)
- `muscle_group` - ENUM (Biceps, Back, Triceps, Shoulders, Legs, Glutes, Chest, Calves, Abs)
- `created_at`, `updated_at` - Timestamps

#### `workout_state`
- `state_id` (PK)
- `user_id` (FK → users.user_id) UNIQUE
- `current_day_number` - Integer (which day in routine user is on)
- `last_workout_date` - DATE
- `updated_at` - Timestamp

#### `workout_sessions`
- `session_id` (PK)
- `user_id` (FK → users.user_id)
- `routine_day_number` - Integer
- `workout_date` - DATE (stored in user's timezone)
- `session_order` - Integer (1, 2, 3... incrementing order)
- `created_at` - Timestamp

#### `workout_logs`
- `log_id` (PK)
- `user_id` (FK → users.user_id)
- `session_id` (FK → workout_sessions.session_id)
- `routine_day_number` - Integer
- `exercise_id` (FK → exercises.exercise_id)
- `sets_completed` - Integer
- `reps_completed` - Integer
- `weight_used` - DECIMAL(5,2) (snapshot of weight at time of workout)
- `workout_date` - DATE
- `created_at` - Timestamp

#### `next_workout_selections`
- `selection_id` (PK)
- `user_id` (FK → users.user_id)
- `exercise_id` (FK → exercises.exercise_id)
- `is_selected` - Boolean
- `updated_at` - Timestamp
- UNIQUE constraint on (user_id, exercise_id)


## Design Philosophy

### Two-Table Exercise Approach
FitJournal uses a **copy-on-registration** approach:
1. **`default_exercises`** — Template catalog (101 exercises)
2. **`exercises`** — User's personal copy (linked by `user_id`)

Each user has full control over their exercises without affecting others. Registration uses a bulk insert for performance.

### Workout Algorithm
1. Gets muscle groups for the current routine day
2. Selects **3 exercises per muscle group** (configurable in future)
3. Prioritizes exercises with lowest `exercise_times_performed` count
4. Uses `func.rand()` as tiebreaker for exercises with equal counts — ensures workout variety
5. Ensures progressive overload by rotating through least-performed exercises

### JWT Authentication (Web + Mobile)
FitJournal uses stateless JWT authentication across both platforms:
1. Login → FastAPI issues a JWT access token (30-day expiry)
2. **Web:** Token stored in `localStorage`, attached to all API calls via `authHeaders()` in `api.js`
3. **Mobile:** Token encrypted on-device via Android Keystore, auto-injected into all requests via OkHttp `AuthInterceptor`
4. Logout clears token locally — no server-side session to invalidate
5. All protected endpoints require a valid Bearer token (`403` returned otherwise)

### Timezone Support
Workout dates are stored in the user's local timezone (not UTC), preventing off-by-one date issues for users in non-UTC timezones. The backend uses `pytz` to convert UTC to the user's stored timezone before recording dates.

### MVI Architecture (Mobile)
The mobile app follows strict MVI (Model-View-Intent) pattern:
- **Model:** `ScreenState` data classes hold all UI state
- **View:** Compose screens observe state via `collectAsState()`
- **Intent:** Actions dispatched to ViewModels via sealed classes
- **Reducer:** Pure functions transform state deterministically (e.g. `homeScreenReducer`)


## Testing

### Unit Tests (83% coverage)
- `HomeViewModelTest` — 3 tests (muscle group fetching, success/error states)
- `HomeScreenReducerTest` — 7 tests (all reducer actions)
- `UserExercisesViewModelTest` — 11 tests (CRUD operations, error handling)
- `RoutineViewModelTest` — 13 tests (load, edit, save, cancel flows)

Repositories tested via interfaces (`IUserExercisesRepository`, `IUserRoutineRepository`) using fake implementations.

### UI Tests
- `loginScreen_showsErrorMessage_whenCredentialsAreInvalid` — sad path, 1 screen
- `loginScreen_navigatesToHome_onSuccessfulLogin` — happy path, 2 screens

### Running Tests
```bash
# Unit tests + coverage report
cd mobile
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report
# Windows
start app/build/reports/jacoco/jacocoTestReport/html/index.html

# Mac
source venv/bin/activate

# UI tests (requires running emulator)
./gradlew connectedDebugAndroidTest
```


## Enhancements

| Enhancement | Points | Details |
|---|---|---|
| Login flow with real data | 3 | JWT auth via FastAPI, EncryptedSharedPreferences |
| Create/edit/delete data | 2 | Exercises CRUD + weight editing |
| Image loading with caching | 2 | Coil + coil-gif for exercise GIFs |
| Loading animations | 1 | CircularProgressIndicator on all loading states |
| Analytics/logging | 1 | AnalyticsLogger with 12+ events (tag: FitJournalAnalytics) |
| Custom app icon | 1 | Kettlebell logo in all mipmap densities |
| **Total** | **10** | |


## Current Status

### Web App ✅ Complete
- Full backend API (20+ endpoints), all protected with JWT
- Full user authentication system with JWT
- Exercise, routine, workout, and calendar management
- Timezone-aware workout date logging
- Dark theme UI/UX

### Mobile App ✅ Complete
- ✅ JWT authentication (login + register + auto-login + logout)
- ✅ Secure token storage and automatic injection via AuthInterceptor
- ✅ Dashboard with Quick Stats and Current Routine modules
- ✅ User exercises — browse, add, delete, edit weight
- ✅ Routine builder — full create/edit flow
- ✅ Workout screen — auto-generation, checklist, drag-to-reorder, completion
- ✅ Profile settings — timezone, units, personal info
- ✅ Stopwatch, calendar UI, bottom navigation
- ✅ Unit tests (83% coverage) + UI tests


### Known Limitations
- No password reset functionality
- No data export feature
- Physical device testing requires local IP configuration in `ApiConfig.kt`
- CORS allows all origins (development mode)
- No offline support — requires active backend connection

### Future Enhancements 📋
- [ ] ExerciseDB legacy code cleanup (ExerciseRepository, ExerciseDetailsViewModel)
- [ ] User-configurable exercises per muscle group in routine setup
- [ ] Analytics dashboard (charts, progress graphs)
- [ ] Personal records (PR) tracking
- [ ] REST timer between sets
- [ ] Social features (share routines)
- [ ] Password reset via email
- [ ] Export workout data (CSV/PDF)
- [ ] Deployment (cloud backend + web hosting)
- [ ] Rate limiting on API endpoints
- [ ] HTTPS for production
- [ ] Offline sync with Room database


## Development Workflow

```bash
# Terminal 1: Backend
cd FitJournal/src
source ../venv/Scripts/activate
uvicorn main:app --reload --host 0.0.0.0

# Terminal 2: Web frontend
cd FitJournal/frontend
python -m http.server 8080

# Terminal 3: Git
cd FitJournal
git status
git add .
git commit -m "your message"
git push
```

## Security Notes

- ✅ Passwords hashed with bcrypt
- ✅ `.env` excluded from Git
- ✅ SQL injection protection (SQLAlchemy ORM)
- ✅ Input validation (Pydantic schemas)
- ✅ JWT tokens for both web and mobile authentication
- ✅ All backend endpoints protected with JWT (`get_current_user` dependency)
- ✅ Tokens encrypted on-device via Android Keystore (mobile)
- ✅ AuthInterceptor auto-injects Bearer token on all mobile API calls
- ✅ Exercise update uses `exclude_none=True` to prevent null overwrites
- ⚠️ TODO: Rate limiting on API endpoints
- ⚠️ TODO: HTTPS for production
- ⚠️ TODO: CSRF protection


## Author

**Leandro Ardiles**
- MS Computer Science, Yeshiva University (Katz School)

## Repository

GitHub: [https://github.com/leanardiles/FitJournal](https://github.com/leanardiles/FitJournal)


---

*Last Updated: May 2026*