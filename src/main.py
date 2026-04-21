# Standard library imports
from datetime import date, datetime, timedelta
from typing import List

# Third-party imports
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from jose import JWTError, jwt

# Local imports
import models
import schemas
from database import engine, get_db



# Create database tables (if they don't exist)
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="FitJournal API")


# ========== CORS CONFIGURATION ==========
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify your domain
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ========== PASSWORD HASHING ==========
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    """Hash a password using bcrypt"""
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash"""
    return pwd_context.verify(plain_password, hashed_password)


# ========== JWT CONFIGURATION ==========
SECRET_KEY = "fitjournal-secret-key-change-this-in-production"  # TODO: Move to .env
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 43200  # 30 days

def create_access_token(data: dict):
    """
    Create a JWT access token
    
    Args:
        data: Dictionary with user info (user_id, email, etc.)
    
    Returns:
        Encoded JWT token as string
    """
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


# ========== ROUTES ==========

@app.get("/")
def index():
    return {"message": "Welcome to FitJournal API"}


# ========== USER AUTHENTICATION ROUTES ==========

@app.post("/register", response_model=schemas.UserResponse, status_code=status.HTTP_201_CREATED)
def register(user: schemas.UserCreate, db: Session = Depends(get_db)):
    """
    Register a new user and copy all default exercises to their account
    """
    # Check if user already exists
    existing_user = db.query(models.User).filter(models.User.user_email == user.user_email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Create new user with hashed password
    hashed_pw = hash_password(user.user_password)
    new_user = models.User(
        user_email=user.user_email,
        user_password=hashed_pw
    )
    
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    # Copy all default exercises to user's exercises table
    default_exercises = db.query(models.DefaultExercise).all()
    for default_ex in default_exercises:
        user_exercise = models.Exercise(
            exercise_name=default_ex.exercise_name,
            exercise_muscle_group=default_ex.exercise_muscle_group,
            exercise_link=default_ex.exercise_link,
            user_id=new_user.user_id
        )
        db.add(user_exercise)
    
    db.commit()
    
    return new_user

@app.post("/login")
def login(user: schemas.UserLogin, db: Session = Depends(get_db)):
    """
    Login user with email and password
    """
    # Find user by email
    db_user = db.query(models.User).filter(models.User.user_email == user.user_email).first()
    
    # Verify user exists and password is correct
    if not db_user or not verify_password(user.user_password, db_user.user_password):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    # Check if user is active
    if not db_user.user_is_active:
        raise HTTPException(status_code=403, detail="Account is inactive")
    
    return {
        "message": "Login successful",
        "user_id": db_user.user_id,
        "user_email": db_user.user_email
    }


@app.post("/login/mobile")
def login_mobile(user: schemas.UserLogin, db: Session = Depends(get_db)):
    """
    Login endpoint for mobile app - returns JWT token
    
    Mobile apps use token-based auth instead of sessions
    Token is included in Authorization header for all requests
    """
    # Find user by email (same as regular login)
    db_user = db.query(models.User).filter(models.User.user_email == user.user_email).first()
    
    # Verify user exists and password is correct (same as regular login)
    if not db_user or not verify_password(user.user_password, db_user.user_password):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    # Check if user is active (same as regular login)
    if not db_user.user_is_active:
        raise HTTPException(status_code=403, detail="Account is inactive")
    
    # Create JWT token
    access_token = create_access_token(
        data={
            "sub": str(db_user.user_id),  # "sub" = subject (standard JWT field)
            "email": db_user.user_email
        }
    )
    
    # Return token + user info
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": db_user.user_id,
        "user_email": db_user.user_email
    }


# ========== PROFILE ROUTES ==========

@app.get("/profile/{user_id}", response_model=schemas.UserProfileResponse)
def get_profile(user_id: int, db: Session = Depends(get_db)):
    """
    Get user profile information
    """
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return user

@app.put("/profile/{user_id}", response_model=schemas.UserProfileResponse)
def update_profile(user_id: int, profile: schemas.UserProfileUpdate, db: Session = Depends(get_db)):
    """
    Update user profile information
    """
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Update fields
    if profile.user_first_name is not None:
        user.user_first_name = profile.user_first_name
    if profile.user_sex is not None:
        user.user_sex = profile.user_sex
    if profile.user_age is not None:
        user.user_age = profile.user_age    
    if profile.user_unit_preference is not None:
        user.user_unit_preference = profile.user_unit_preference
    if profile.user_height is not None:
        user.user_height = profile.user_height
    if profile.user_weight is not None:
        user.user_weight = profile.user_weight
    
    db.commit()
    db.refresh(user)
    
    return user


# ========== EXERCISE ROUTES ==========

@app.get("/exercises", response_model=List[schemas.ExerciseResponse])
def get_exercises(user_id: int, db: Session = Depends(get_db)):
    """
    Get all exercises for a specific user
    """
    exercises = db.query(models.Exercise).filter(models.Exercise.user_id == user_id).all()
    return exercises

@app.post("/exercises", response_model=schemas.ExerciseResponse, status_code=status.HTTP_201_CREATED)
def create_exercise(exercise: schemas.ExerciseCreate, user_id: int, db: Session = Depends(get_db)):
    # Validate user exists
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    new_exercise = models.Exercise(**exercise.dict(), user_id=user_id)
    db.add(new_exercise)
    db.commit()
    db.refresh(new_exercise)
    return new_exercise


@app.put("/exercises/{exercise_id}", response_model=schemas.ExerciseResponse)
def update_exercise(exercise_id: int, exercise: schemas.ExerciseCreate, user_id: int, db: Session = Depends(get_db)):
    """
    Update an existing exercise
    """
    db_exercise = db.query(models.Exercise).filter(
        models.Exercise.exercise_id == exercise_id,
        models.Exercise.user_id == user_id
    ).first()
    
    if not db_exercise:
        raise HTTPException(status_code=404, detail="Exercise not found")
    
    # Update exercise fields
    for key, value in exercise.dict().items():
        setattr(db_exercise, key, value)
    
    db.commit()
    db.refresh(db_exercise)
    return db_exercise

@app.delete("/exercises/{exercise_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_exercise(exercise_id: int, user_id: int, db: Session = Depends(get_db)):
    """
    Delete an exercise
    """
    exercise = db.query(models.Exercise).filter(
        models.Exercise.exercise_id == exercise_id,
        models.Exercise.user_id == user_id
    ).first()
    
    if not exercise:
        raise HTTPException(status_code=404, detail="Exercise not found")
    
    db.delete(exercise)
    db.commit()
    return None


# ========== DEFAULT EXERCISES ROUTES ==========

@app.get("/default-exercises")
def get_default_exercises(db: Session = Depends(get_db)):
    """
    Get all default exercises (template catalog)
    """
    exercises = db.query(models.DefaultExercise).all()
    return exercises


# ========== ROUTINE ROUTES (PLACEHOLDER) ==========

@app.get("/routines")
def get_routines(user_id: int, db: Session = Depends(get_db)):
    """
    Get all routines for a user (to be implemented)
    """
    routines = db.query(models.Routine).filter(models.Routine.user_id == user_id).all()
    return routines


# ========== ROUTINE ROUTES ==========

@app.get("/routine/{user_id}")
def get_routine(user_id: int, db: Session = Depends(get_db)):
    """
    Get user's routine configuration
    Returns days_per_week and muscle groups per day
    """
    # Get routine info
    routine = db.query(models.Routine).filter(models.Routine.user_id == user_id).first()
    
    if not routine:
        return {
            "days_per_week": 0,
            "routine_days": {}
        }
    
    # Get all routine muscles per day
    routine_muscles = db.query(models.RoutineMusclePerDay).filter(
        models.RoutineMusclePerDay.user_id == user_id
    ).order_by(models.RoutineMusclePerDay.day_number).all()
    
    # Organize by day
    days_dict = {}
    for rm in routine_muscles:
        if rm.day_number not in days_dict:
            days_dict[rm.day_number] = []
        days_dict[rm.day_number].append(rm.muscle_group)
    
    return {
        "days_per_week": routine.days_per_week,
        "routine_days": days_dict
    }

@app.post("/routine/{user_id}")
def create_or_update_routine(user_id: int, routine_data: schemas.RoutineSetup, db: Session = Depends(get_db)):
    """
    Create or update user's routine
    Allows multiple muscle groups per day
    """
    # Validate user exists
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Validate days_per_week
    if routine_data.days_per_week < 1 or routine_data.days_per_week > 7:
        raise HTTPException(status_code=400, detail="days_per_week must be between 1 and 7")
    
    # Delete existing routine and routine_muscles_per_day (clean slate)
    db.query(models.RoutineMusclePerDay).filter(models.RoutineMusclePerDay.user_id == user_id).delete()
    db.query(models.Routine).filter(models.Routine.user_id == user_id).delete()
    
    # Create new routine
    new_routine = models.Routine(
        user_id=user_id,
        days_per_week=routine_data.days_per_week
    )
    db.add(new_routine)
    
    # Create routine_muscles_per_day entries
    for day_data in routine_data.routine_days:
        # Validate day_number
        if day_data.day_number < 1 or day_data.day_number > routine_data.days_per_week:
            continue  # Skip invalid days
        
        for muscle_group in day_data.muscle_groups:
            routine_muscle = models.RoutineMusclePerDay(
                user_id=user_id,
                day_number=day_data.day_number,
                muscle_group=muscle_group
            )
            db.add(routine_muscle)
    
    db.commit()
    
    return {"message": "Routine saved successfully"}

@app.delete("/routine/{user_id}")
def delete_routine(user_id: int, db: Session = Depends(get_db)):
    """
    Delete user's routine
    """
    db.query(models.RoutineMusclePerDay).filter(models.RoutineMusclePerDay.user_id == user_id).delete()
    db.query(models.Routine).filter(models.Routine.user_id == user_id).delete()
    db.commit()
    
    return {"message": "Routine deleted successfully"}


# ========== WORKOUT ROUTES ==========

@app.get("/workout/state/{user_id}", response_model=schemas.WorkoutStateResponse)
def get_workout_state(user_id: int, db: Session = Depends(get_db)):
    """
    Get user's current workout state (which day they're on)
    """
    state = db.query(models.WorkoutState).filter(models.WorkoutState.user_id == user_id).first()
    
    if not state:
        # Create initial state for user
        state = models.WorkoutState(
            user_id=user_id,
            current_day_number=1
        )
        db.add(state)
        db.commit()
        db.refresh(state)
    
    return state

@app.post("/workout/complete/{user_id}")
def complete_workout(user_id: int, workout_data: schemas.CompleteWorkoutRequest, db: Session = Depends(get_db)):
    """
    Mark workout as complete
    Main orchestration function - delegates to helpers
    """
    from datetime import date
    
    # Validate and get required data
    user, routine = validate_user_and_routine(user_id, db)
    
    # Create workout session
    session = create_workout_session(user_id, workout_data.day_number, db)
    
    # Log all exercises
    completed_exercise_ids = log_workout_exercises(session, workout_data.exercises, user_id, workout_data.day_number, db)
    
    # Clean up and update state
    cleanup_after_workout(user_id, completed_exercise_ids, workout_data.day_number, routine.days_per_week, db)
    
    db.commit()
    
    return {
        "message": "Workout completed successfully",
        "next_day": (workout_data.day_number % routine.days_per_week) + 1,
        "session_id": session.session_id
    }


# ========== HELPER FUNCTIONS FOR COMPLETE_WORKOUT ==========

def validate_user_and_routine(user_id: int, db: Session):
    """Validate user exists and has a routine"""
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    routine = db.query(models.Routine).filter(models.Routine.user_id == user_id).first()
    if not routine:
        raise HTTPException(status_code=400, detail="No routine set up")
    
    return user, routine


def create_workout_session(user_id: int, day_number: int, db: Session):
    """Create a new workout session with incremented order"""
    from datetime import date
    
    # Get next session order
    last_session = db.query(models.WorkoutSession).filter(
        models.WorkoutSession.user_id == user_id
    ).order_by(models.WorkoutSession.session_order.desc()).first()
    
    next_session_order = (last_session.session_order + 1) if last_session else 1
    
    # Create session
    session = models.WorkoutSession(
        user_id=user_id,
        routine_day_number=day_number,
        workout_date=date.today(),
        session_order=next_session_order
    )
    db.add(session)
    db.flush()  # Get session_id
    
    return session


def log_workout_exercises(session, exercises_data, user_id: int, day_number: int, db: Session):
    """Log all exercises and update exercise metadata"""
    from datetime import date
    
    completed_exercise_ids = []
    
    for exercise_data in exercises_data:
        completed_exercise_ids.append(exercise_data.exercise_id)
        
        # Create log entry
        log = models.WorkoutLog(
            user_id=user_id,
            routine_day_number=day_number,
            exercise_id=exercise_data.exercise_id,
            sets_completed=exercise_data.sets_completed,
            reps_completed=exercise_data.reps_completed,
            weight_used=exercise_data.weight_used,
            workout_date=date.today(),
            session_id=session.session_id
        )
        db.add(log)
        
        # Update exercise metadata
        update_exercise_metadata(exercise_data, db)
    
    return completed_exercise_ids


def update_exercise_metadata(exercise_data, db: Session):
    """Increment performance count and update weight"""
    exercise = db.query(models.Exercise).filter(
        models.Exercise.exercise_id == exercise_data.exercise_id
    ).first()
    
    if exercise:
        # Increment times performed
        exercise.exercise_times_performed += 1
        
        # Update weight if changed
        if exercise_data.weight_used and exercise_data.weight_used > 0:
            exercise.exercise_user_current_weight = exercise_data.weight_used


def cleanup_after_workout(user_id: int, completed_exercise_ids: list, current_day: int, days_per_week: int, db: Session):
    """Deselect completed exercises and advance workout state"""
    from datetime import date
    
    # Deselect completed exercises
    db.query(models.NextWorkoutSelection).filter(
        models.NextWorkoutSelection.user_id == user_id,
        models.NextWorkoutSelection.exercise_id.in_(completed_exercise_ids)
    ).delete(synchronize_session=False)
    
    # Update or create workout state
    state = db.query(models.WorkoutState).filter(models.WorkoutState.user_id == user_id).first()
    if not state:
        state = models.WorkoutState(user_id=user_id, current_day_number=1)
        db.add(state)
    
    # Advance to next day
    next_day = (current_day % days_per_week) + 1
    state.current_day_number = next_day
    state.last_workout_date = date.today()

@app.get("/workout/logs/{user_id}")
def get_workout_logs(user_id: int, limit: int = 30, db: Session = Depends(get_db)):
    """
    Get user's workout history (last 30 days by default)
    """
    logs = db.query(models.WorkoutLog).filter(
        models.WorkoutLog.user_id == user_id
    ).order_by(models.WorkoutLog.workout_date.desc()).limit(limit).all()
    
    return logs


# ========== NEXT WORKOUT SELECTION ROUTES ==========

@app.get("/next-workout/selections/{user_id}")
def get_next_workout_selections(user_id: int, db: Session = Depends(get_db)):
    """
    Get user's manually selected exercises for next workout
    """
    selections = db.query(models.NextWorkoutSelection).filter(
        models.NextWorkoutSelection.user_id == user_id
    ).all()
    
    return selections

@app.post("/next-workout/toggle")
def toggle_next_workout_selection(data: dict, db: Session = Depends(get_db)):
    """
    Toggle exercise selection for next workout
    """
    user_id = data['user_id']
    exercise_id = data['exercise_id']
    is_selected = data['is_selected']
    
    # Check if selection exists
    selection = db.query(models.NextWorkoutSelection).filter(
        models.NextWorkoutSelection.user_id == user_id,
        models.NextWorkoutSelection.exercise_id == exercise_id
    ).first()
    
    if selection:
        selection.is_selected = is_selected
    else:
        selection = models.NextWorkoutSelection(
            user_id=user_id,
            exercise_id=exercise_id,
            is_selected=is_selected
        )
        db.add(selection)
    
    db.commit()
    return {"message": "Selection updated"}

@app.delete("/next-workout/clear/{user_id}")
def clear_next_workout_selections(user_id: int, day_number: int = None, db: Session = Depends(get_db)):
    """
    Clear next workout selections
    If day_number is specified, only clear exercises for that day's muscle groups
    """
    if day_number is None:
        # Clear all selections
        db.query(models.NextWorkoutSelection).filter(
            models.NextWorkoutSelection.user_id == user_id
        ).delete()
    else:
        # Get muscle groups for specified day
        muscle_groups_for_day = db.query(models.RoutineMusclePerDay.muscle_group).filter(
            models.RoutineMusclePerDay.user_id == user_id,
            models.RoutineMusclePerDay.day_number == day_number
        ).distinct().all()
        
        muscle_groups = [mg[0] for mg in muscle_groups_for_day]
        
        # Get exercises for these muscle groups
        exercises_for_day = db.query(models.Exercise.exercise_id).filter(
            models.Exercise.user_id == user_id,
            models.Exercise.exercise_muscle_group.in_(muscle_groups),
            models.Exercise.exercise_is_in_routine == True
        ).all()
        
        exercise_ids = [ex[0] for ex in exercises_for_day]
        
        # Clear only these exercises
        db.query(models.NextWorkoutSelection).filter(
            models.NextWorkoutSelection.user_id == user_id,
            models.NextWorkoutSelection.exercise_id.in_(exercise_ids)
        ).delete(synchronize_session=False)
    
    db.commit()
    
    return {"message": "Selections cleared"}


@app.post("/next-workout/generate/{user_id}")
def generate_next_workout(user_id: int, day_number: int = None, db: Session = Depends(get_db)):
    """
    Auto-generate next workout using algorithm
    Main orchestration function - delegates to helpers
    """
    # Determine which day to generate for
    target_day = determine_target_day(user_id, day_number, db)
    
    # Get muscle groups for the target day
    muscle_groups = get_muscle_groups_for_day(user_id, target_day, db)
    
    # Clear existing selections for this day
    clear_selections_for_day(user_id, muscle_groups, db)
    
    # Select exercises for each muscle group
    selected_count = select_exercises_for_workout(user_id, muscle_groups, db)
    
    db.commit()
    
    return {
        "message": "Next workout generated",
        "exercises_selected": selected_count,
        "day_number": target_day
    }


# ========== HELPER FUNCTIONS FOR GENERATE_NEXT_WORKOUT ==========

def determine_target_day(user_id: int, day_number: int, db: Session):
    """Determine which day to generate workout for"""
    if day_number is not None:
        return day_number
    
    # Use user's current workout state
    state = db.query(models.WorkoutState).filter(
        models.WorkoutState.user_id == user_id
    ).first()
    
    if not state:
        raise HTTPException(status_code=400, detail="No workout state found")
    
    return state.current_day_number


def get_muscle_groups_for_day(user_id: int, day_number: int, db: Session):
    """Get list of muscle groups assigned to a specific day"""
    muscle_groups_query = db.query(models.RoutineMusclePerDay.muscle_group).filter(
        models.RoutineMusclePerDay.user_id == user_id,
        models.RoutineMusclePerDay.day_number == day_number
    ).distinct().all()
    
    muscle_groups = [mg[0] for mg in muscle_groups_query]
    
    if not muscle_groups:
        raise HTTPException(status_code=400, detail="No muscle groups assigned to this day")
    
    return muscle_groups


def clear_selections_for_day(user_id: int, muscle_groups: list, db: Session):
    """Clear existing selections for exercises in specified muscle groups"""
    # Get exercises for these muscle groups
    exercises_for_day = db.query(models.Exercise.exercise_id).filter(
        models.Exercise.user_id == user_id,
        models.Exercise.exercise_muscle_group.in_(muscle_groups),
        models.Exercise.exercise_is_in_routine == True
    ).all()
    
    exercise_ids = [ex[0] for ex in exercises_for_day]
    
    # Clear selections
    db.query(models.NextWorkoutSelection).filter(
        models.NextWorkoutSelection.user_id == user_id,
        models.NextWorkoutSelection.exercise_id.in_(exercise_ids)
    ).delete(synchronize_session=False)


def select_exercises_for_workout(user_id: int, muscle_groups: list, db: Session):
    """Select 4 exercises per muscle group (lowest times_performed)"""
    selected_count = 0
    
    for muscle_group in muscle_groups:
        exercises = get_exercises_for_muscle_group(user_id, muscle_group, db)
        
        for exercise in exercises:
            create_exercise_selection(user_id, exercise.exercise_id, db)
            selected_count += 1
    
    return selected_count


def get_exercises_for_muscle_group(user_id: int, muscle_group: str, db: Session):
    """Get top 4 exercises for a muscle group (by lowest performance count)"""
    exercises = db.query(models.Exercise).filter(
        models.Exercise.user_id == user_id,
        models.Exercise.exercise_muscle_group == muscle_group,
        models.Exercise.exercise_is_in_routine == True
    ).order_by(
        models.Exercise.exercise_times_performed.asc()
    ).limit(10).all()
    
    return exercises


def create_exercise_selection(user_id: int, exercise_id: int, db: Session):
    """Create a next workout selection for an exercise"""
    selection = models.NextWorkoutSelection(
        user_id=user_id,
        exercise_id=exercise_id,
        is_selected=True
    )
    db.add(selection)


# ========== WORKOUT SESSION ROUTES ==========

@app.get("/workout/sessions/{user_id}")
def get_workout_sessions(user_id: int, limit: int = 10, db: Session = Depends(get_db)):
    """
    Get user's last N workout sessions (most recent first)
    """
    sessions = db.query(models.WorkoutSession).filter(
        models.WorkoutSession.user_id == user_id
    ).order_by(models.WorkoutSession.session_order.desc()).limit(limit).all()
    
    # Reverse to show oldest to newest (for display left to right)
    return list(reversed(sessions))


@app.post("/workout/logs-by-sessions/{user_id}")
def get_workout_logs_by_sessions(user_id: int, data: dict, db: Session = Depends(get_db)):
    """
    Get workout logs for specific session IDs
    """
    session_ids = data.get('session_ids', [])
    
    logs = db.query(models.WorkoutLog).filter(
        models.WorkoutLog.user_id == user_id,
        models.WorkoutLog.session_id.in_(session_ids)
    ).all()
    return logs


@app.get("/workout/state/{user_id}")
def get_workout_state(user_id: int, db: Session = Depends(get_db)):
    """Get user's current routine day"""

@app.get("/next-workout/selections/{user_id}")
def get_next_workout_selections(user_id: int, db: Session = Depends(get_db)):
    """Get exercises selected for next workout"""
