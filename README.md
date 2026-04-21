# FitJournal

A no-frills fitness tracker — web application + native Android mobile app - that feels like your paper notebook.

## Project Overview

FitJournal is a comprehensive fitness tracking application that provides a complete workout management system with routine planning, exercise tracking, and workout history visualization — available both as a web app and a native Android app.

## Features

### Web App
- ✅ User registration and authentication
- ✅ User profile management (age, weight, height, unit preferences)
- ✅ 101 default exercises (copied to each user on registration)
- ✅ Exercise management with muscle group organization
- ✅ Custom routine builder (1-7 days per week, multiple muscle groups per day)
- ✅ Intelligent workout generation algorithm
- ✅ Daily workout tracking (Get WOD - Workout of the Day)
- ✅ Workout history calendar with multi-day filtering
- ✅ Automatic exercise weight tracking and updates
- ✅ Session-based workout logging
- ✅ Progress tracking by exercise frequency
### Android Mobile App
- ✅ Exercise browsing by muscle group (ExerciseDB API)
- ✅ Exercise details with instructions
- ✅ Offline-first caching (Room database)
- ✅ Calendar UI with DatePicker
- ✅ Stopwatch timer
- ✅ MVI architecture (Home & Exercises screens)
- ✅ JWT authentication (login + register)
- ✅ Secure token storage (Android Keystore / EncryptedSharedPreferences)
- ✅ Bottom navigation + profile menu
- 🚧 Workout logging
- 🚧 Routine builder
- 🚧 Offline sync with backend


## Tech Stack

### Backend
- **Framework:** FastAPI (Python)
- **ORM:** SQLAlchemy 2.0
- **Database:** MySQL (hosted on Aiven)
- **Authentication:** Bcrypt password hashing + JWT tokens (mobile)
- **Database Driver:** PyMySQL

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
- **Networking:** Retrofit 2 + Gson
- **Auth Storage:** EncryptedSharedPreferences (Android Keystore)
- **State:** StateFlow + Coroutines


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
│       ├── data/
│       │   ├── local/            # Room DB, TokenManager
│       │   ├── model/            # Data classes incl. AuthModels
│       │   ├── network/          # Retrofit, ApiConfig, FitJournalApiService
│       │   └── repository/       # AuthRepository, ExerciseRepository
│       ├── ui/
│       │   ├── auth/             # LoginScreen, AuthViewModel
│       │   ├── home/
│       │   ├── exercises/
│       │   ├── exercise_details/
│       │   ├── calendar/
│       │   ├── stopwatch/
│       │   └── shared/
│       ├── navigation/
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
 
# Git Bash / Linux / Mac
source venv/Scripts/activate
 
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
> **Note:** The mobile app connects to the backend via `10.0.2.2:8000` on the emulator. Physical devices require the machine's local IP address instead.

### Test User
You can use the following test user credentials:
- Email: test@test.com
- Password: testUSER123!


## API Endpoints
 
### Authentication
- `POST /register` — Register new user
- `POST /login` — Web login (returns user_id and email)
- `POST /login/mobile` — Mobile login (returns JWT access token)

### Profile
- `GET /profile/{user_id}` — Get user profile
- `PUT /profile/{user_id}` — Update user profile

### Exercises
- `GET /exercises?user_id={id}` — Get all exercises for user
- `POST /exercises?user_id={id}` — Create new exercise
- `PUT /exercises/{exercise_id}?user_id={id}` — Update exercise
- `DELETE /exercises/{exercise_id}?user_id={id}` — Delete exercise
- `PATCH /exercises/{exercise_id}/weight` — Update exercise weight

### Routines
- `GET /routine/{user_id}` — Get user's routine
- `POST /routine/{user_id}` — Create/update routine
- `DELETE /routine/{user_id}` — Delete routine

### Workout State
- `GET /workout/state/{user_id}` — Get current workout state

### Workout Sessions & Logs
- `POST /workout/complete/{user_id}` — Complete workout
- `GET /workout/sessions/{user_id}?limit={n}` — Get last N sessions
- `POST /workout/logs-by-sessions/{user_id}` — Get logs for specific sessions
- `GET /workout/logs/{user_id}?limit={n}` — Get workout logs

### Next Workout Management
- `GET /next-workout/selections/{user_id}` — Get selected exercises
- `POST /next-workout/toggle` — Toggle exercise selection
- `POST /next-workout/generate/{user_id}?day_number={n}` — Auto-generate workout
- `DELETE /next-workout/clear/{user_id}?day_number={n}` — Clear selections

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
- `user_age` - Integer (0-100)
- `user_unit_preference` - ENUM('metric', 'imperial')
- `user_weight`, `user_height` - Decimal/Integer
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
- `workout_date` - DATE
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
Each user has full control over their exercises without affecting others.
 
### Workout Algorithm
1. Gets muscle groups for the current routine day
2. Selects 4 exercises per muscle group
3. Prioritizes exercises with lowest `exercise_times_performed` count
4. Ensures variety and prevents overtraining specific exercises

### Mobile JWT Authentication
The mobile app uses stateless JWT authentication:
1. Login → FastAPI issues a JWT access token
2. Token encrypted and stored on-device via Android Keystore
3. Token attached to all subsequent API requests
4. Logout clears local storage only (no server-side session to invalidate)
## Current Status
 
### Web App ✅
- Complete backend API (20+ endpoints)
- Full user authentication system
- Exercise, routine, workout, and calendar management
- Dark theme UI/UX

### Mobile App ✅ / 🚧
- ✅ JWT authentication (login + register)
- ✅ Exercise browsing with offline caching
- ✅ Stopwatch, calendar UI, bottom navigation
- 🚧 Workout logging
- 🚧 Routine builder
- 🚧 Offline sync

### Known Limitations
- Web app uses localStorage for session (no JWT — planned)
- No password reset functionality
- No data export feature
- Physical device testing requires local IP configuration
- CORS allows all origins (development mode)

### To-Do (High Priority) 🔧
- [ ] Fix ADD/EDIT/DELETE modal on Exercises page (CSS z-index issue)
- [ ] Mobile: auto-login on app startup using stored JWT
- [ ] Mobile: wire logout button to clear token and return to login screen
- [ ] Mobile: OkHttp interceptor to attach Bearer token to all API calls

### Future Enhancements 📋
- [ ] Analytics dashboard (charts, progress graphs)
- [ ] Personal records (PR) tracking
- [ ] REST timer between sets
- [ ] Social features (share routines)
- [ ] Password reset via email
- [ ] Export workout data (CSV/PDF)
- [ ] Deployment (Vercel frontend + Railway backend)
- [ ] Rate limiting on API endpoints
- [ ] HTTPS for production
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
- ✅ JWT tokens for mobile authentication
- ✅ Tokens encrypted on-device via Android Keystore
- ⚠️ TODO: JWT for web app (currently localStorage)
- ⚠️ TODO: Rate limiting on API endpoints
- ⚠️ TODO: HTTPS for production
- ⚠️ TODO: CSRF protection

 
## Author
 
**Leandro Ardiles**
- MS Computer Science, Yeshiva University (Katz School)

## Repository
 
GitHub: [https://github.com/leanardiles/FitJournal](https://github.com/leanardiles/FitJournal)
 
## License
 
This project is for educational purposes.
 
---
 
*Last Updated: April 2026*




OLD
## Core Workflows

### 1. Registration & Profile Setup
1. Register new account with email/password
2. Set up profile (name, age, weight, height, unit preference)
3. 101 default exercises automatically copied to user account

### 2. Exercise Management
1. View exercises organized by 9 muscle groups (tabs)
2. Update current weight for each exercise
3. Toggle exercises in/out of routine
4. Track exercise performance count

### 3. Routine Creation
1. Select training days per week (1-7)
2. Assign muscle groups to each day
3. System stores routine structure for workout generation

### 4. Workout Selection (Calendar)
1. View calendar with multi-day filtering (Current Day, All Days, Day 1, Day 2, etc.)
2. Manually select exercises or auto-generate based on algorithm
3. Algorithm selects 4 exercises per muscle group (prioritizes least-performed)
4. View workout history with sessions displayed by date

### 5. Workout Execution (Get WOD)
1. Generate Workout of the Day based on current routine day
2. Pre-filled weights from exercise database
3. Enter sets/reps for each exercise
4. Mark as complete to log workout and advance to next day
5. Completed exercises automatically deselected for next planning cycle

### 6. Progress Tracking
1. Calendar displays workout history (last 10 sessions)
2. Track sets completed per exercise per session
3. Exercise performance count auto-increments
4. Weight progression tracked automatically