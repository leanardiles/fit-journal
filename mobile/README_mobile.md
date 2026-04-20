# FitJournal - Android Mobile App

Native Android application for FitJournal fitness tracking.

## Current Status

**Version:** 0.5 (Week 10 - Student Project)  
**Status:** Development - Ready for backend integration

## Features

### Implemented ✅
- Exercise browsing by muscle group
- Exercise details with instructions
- Offline-first caching (Room database)
- Calendar UI with DatePicker
- Stopwatch timer
- MVI architecture (Home & Exercises screens)
- Profile menu with dropdown
- Bottom navigation

### In Development 🚧
- User authentication
- Backend integration with FastAPI
- Workout logging
- Routine builder
- Offline sync

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVI (Model-View-Intent)
- **Database:** Room (SQLite)
- **Networking:** Retrofit + Gson
- **State:** StateFlow + Coroutines

## Running Locally

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Minimum SDK: 24
- Target SDK: 36

### Setup

1. **Open in Android Studio:**
   - File → Open → Select this `mobile` folder
   - Wait for Gradle sync

2. **Run the app:**
   - Click Run (green play button)
   - Select emulator or physical device

### Connecting to Backend (Coming Soon)

When ready for integration:

1. **Start FastAPI backend:**
```bash
cd ../src
source ../venv/Scripts/activate
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

2. **Update API endpoint in mobile:**
```kotlin
// app/src/main/java/.../ApiConfig.kt (to be created)
const val BASE_URL = "http://10.0.2.2:8000/"  // For emulator
```

## Project Structure