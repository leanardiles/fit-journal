# Standard library imports
from datetime import date, datetime, timedelta
from typing import List
import os
import pytz

# Third-party imports
from fastapi import FastAPI, Depends, HTTPException, status, APIRouter
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi import Request
from passlib.context import CryptContext
from sqlalchemy import func
from sqlalchemy.orm import Session
from jose import JWTError, jwt

# Local imports
import models
import schemas
from database import engine, get_db


# Create database tables (if they don't exist)
models.Base.metadata.create_all(bind=engine)


app = FastAPI(title="FitJournal API")

api_v1 = APIRouter(prefix="/v1")


# Environment-aware Path
# Resolve static and template directories — works in both local dev (src/ subfolder) 
# and Lambda (/var/task/ root) environments
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Try sibling first (local: src/main.py + ../static), then same dir (Lambda: /var/task/main.py + /var/task/static)
def find_dir(name):
    sibling = os.path.join(BASE_DIR, "..", name)
    if os.path.isdir(sibling):
        return sibling
    return os.path.join(BASE_DIR, name)

STATIC_DIR = find_dir("static")
TEMPLATES_DIR = find_dir("templates")

# Mount static files
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")
# Jinja2 templates
templates = Jinja2Templates(directory=TEMPLATES_DIR)

# ========== CORS CONFIGURATION ==========
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://app.fit-journal.com",   # production (web app)
        "http://localhost:8000",          # local dev
        "http://127.0.0.1:8000",          # local dev
    ],
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
SECRET_KEY = os.getenv("SECRET_KEY")
if not SECRET_KEY:
    raise RuntimeError("SECRET_KEY environment variable not set")
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

# HTTP Bearer scheme for Swagger UI
security = HTTPBearer()

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
):
    """
    Validates JWT token from Authorization header.
    Raises 401 if token is missing, expired, or invalid.
    """
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired token",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        token = credentials.credentials
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = db.query(models.User).filter(
        models.User.user_id == int(user_id)
    ).first()
    if user is None:
        raise credentials_exception

    return user

def verify_user_access(
    user_id: int,
    current_user: models.User = Depends(get_current_user)
):
    """
    Authorization guard. Ensures a user can only access their own data:
    confirms the user_id in the request matches the authenticated user.
    Returns current_user so endpoints using it keep working unchanged.
    """
    if current_user.user_id != user_id:
        raise HTTPException(status_code=403, detail="Forbidden")
    return current_user


# ========== ROUTES ==========

# ── HTML Routes ──
@app.get("/")
async def root():
    return RedirectResponse(url="/web/login")

@app.get("/web/login")
async def login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request})

@app.get("/web/forgot-password")
async def forgot_password_page(request: Request):
    return templates.TemplateResponse("forgot-password.html", {"request": request})

@app.get("/web/delete-account")
async def delete_account_page(request: Request):
    return templates.TemplateResponse("delete-account.html", {"request": request})

@app.get("/web/register")
async def register_page(request: Request):
    return templates.TemplateResponse("register.html", {"request": request})

@app.get("/web/dashboard")
async def dashboard(request: Request):
    return templates.TemplateResponse("dashboard.html", {
        "request": request,
        "active_page": "dashboard",
        "page_title": "Dashboard"
    })

@app.get("/web/profile")
async def profile(request: Request):
    return templates.TemplateResponse("profile.html", {
        "request": request,
        "active_page": "profile",
        "page_title": "Profile"
    })

@app.get("/web/routine")
async def routine(request: Request):
    return templates.TemplateResponse("routine.html", {
        "request": request,
        "active_page": "routine",
        "page_title": "Routine"
    })

@app.get("/web/exercises")
async def exercises_page(request: Request):
    return templates.TemplateResponse("exercises.html", {
        "request": request,
        "active_page": "exercises",
        "page_title": "Exercises"
    })

@app.get("/web/calendar")
async def calendar_page(request: Request):
    return templates.TemplateResponse("calendar.html", {
        "request": request,
        "active_page": "calendar",
        "page_title": "Calendar"
    })

@app.get("/web/getwod")
async def getwod_page(request: Request):
    return templates.TemplateResponse("getwod.html", {
        "request": request,
        "active_page": "getwod",
        "page_title": "Get WOD"
    })

# ========== USER AUTHENTICATION ROUTES ==========

@api_v1.post("/register", response_model=schemas.UserResponse, status_code=status.HTTP_201_CREATED)
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
    
    # Copy all default exercises to user's exercises table (bulk insert)
    default_exercises = db.query(models.DefaultExercise).all()
    user_exercises = [
        models.Exercise(
            exercise_name=default_ex.exercise_name,
            exercise_muscle_group=default_ex.exercise_muscle_group,
            exercise_link=default_ex.exercise_link,
            user_id=new_user.user_id
        )
        for default_ex in default_exercises
    ]
    db.bulk_save_objects(user_exercises)
    
    db.commit()
    
    return new_user

@api_v1.post("/login")
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

    # Create JWT token
    access_token = create_access_token(
        data={
            "sub": str(db_user.user_id),
            "email": db_user.user_email
        }
    )

    return {
        "message": "Login successful",
        "token_type": "bearer",
        "user_id": db_user.user_id,
        "user_email": db_user.user_email,
        "access_token": access_token
    }


# ========== PROFILE ROUTES ==========

@api_v1.get("/profile/{user_id}", response_model=schemas.UserProfileResponse)
def get_profile(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get user profile information
    """
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return user

@api_v1.put("/profile/{user_id}", response_model=schemas.UserProfileResponse)
def update_profile(user_id: int, profile: schemas.UserProfileUpdate, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
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
    if profile.user_timezone is not None:
        user.user_timezone = profile.user_timezone        
    if profile.user_unit_preference is not None:
        user.user_unit_preference = profile.user_unit_preference
    if profile.user_height is not None:
        user.user_height = profile.user_height
    if profile.user_weight is not None:
        user.user_weight = profile.user_weight
    
    db.commit()
    db.refresh(user)
    
    return user


# ========== ACCOUNT MANAGEMENT ROUTES ==========

@api_v1.delete("/account/{user_id}")
def delete_account(
    user_id: int,
    payload: schemas.AccountDeleteRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(verify_user_access)
):
    """
    Permanently delete the authenticated user's account and all associated data.

    Requires the account password in the request body as a re-authentication
    step, layered on top of the JWT and the verify_user_access ownership check.
    Deleting the user row cascades to exercises, routine, routine muscles,
    workout state, workout logs, workout sessions, and next-workout selections
    (all foreign keys are ON DELETE CASCADE).
    """
    user = db.query(models.User).filter(models.User.user_id == user_id).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Re-authenticate before destroying data: confirm the password matches.
    if not verify_password(payload.user_password, user.user_password):
        raise HTTPException(status_code=401, detail="Incorrect password")

    db.delete(user)
    db.commit()

    return {"message": "Account deleted successfully"}


# ========== EXERCISE ROUTES ==========

@api_v1.get("/exercises", response_model=List[schemas.ExerciseResponse])
def get_exercises(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get all exercises for a specific user
    """
    exercises = db.query(models.Exercise).filter(models.Exercise.user_id == user_id).all()
    return exercises

@api_v1.post("/exercises", response_model=schemas.ExerciseResponse, status_code=status.HTTP_201_CREATED)
def create_exercise(user_id: int, exercise: schemas.ExerciseCreate, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    # Validate user exists
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    new_exercise = models.Exercise(**exercise.dict(), user_id=user_id)
    db.add(new_exercise)
    db.commit()
    db.refresh(new_exercise)
    return new_exercise


@api_v1.put("/exercises/{exercise_id}", response_model=schemas.ExerciseResponse)
def update_exercise(exercise_id: int, user_id: int, exercise: schemas.ExerciseUpdate, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    db_exercise = db.query(models.Exercise).filter(
        models.Exercise.exercise_id == exercise_id,
        models.Exercise.user_id == user_id
    ).first()
    
    if not db_exercise:
        raise HTTPException(status_code=404, detail="Exercise not found")
    
    # Only update fields that are not None
    update_data = exercise.dict(exclude_none=True)
    for key, value in update_data.items():
        setattr(db_exercise, key, value)
    
    db.commit()
    db.refresh(db_exercise)
    return db_exercise


@api_v1.delete("/exercises/{exercise_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_exercise(exercise_id: int, user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
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

@api_v1.get("/default-exercises")
def get_default_exercises(db: Session = Depends(get_db)):
    """
    Get all default exercises (template catalog)
    """
    exercises = db.query(models.DefaultExercise).all()
    return exercises



# ========== ROUTINE ROUTES ==========

def _enum_str(value):
    """Return the plain string for a SQLAlchemy enum value (or pass through a str)."""
    return value.value if hasattr(value, "value") else value


@api_v1.get("/routine/{user_id}", response_model=schemas.RoutineResponse)
def get_routine(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get the user's routine as an ordered list of training days. Each day carries
    its type (per_muscle or manual), optional name, its muscles + counts
    (per_muscle only), and its selected exercises (the pool for per_muscle, the
    exact list for manual). days_per_week is derived from the number of days.
    """
    days = db.query(models.TrainingDay).filter(
        models.TrainingDay.user_id == user_id
    ).order_by(models.TrainingDay.day_number).all()

    if not days:
        return {"days_per_week": 0, "days": []}

    day_ids = [d.training_day_id for d in days]

    # Muscles grouped by day
    muscles_by_day = {}
    muscle_rows = db.query(models.TrainingDayMuscle).filter(
        models.TrainingDayMuscle.training_day_id.in_(day_ids)
    ).all()
    for m in muscle_rows:
        muscles_by_day.setdefault(m.training_day_id, []).append({
            "muscle_group": _enum_str(m.muscle_group),
            "exercise_count": m.exercise_count,
        })

    # Selected exercises grouped by day (joined to the library for name + muscle)
    exercises_by_day = {}
    tde_rows = db.query(models.TrainingDayExercise, models.Exercise).join(
        models.Exercise, models.TrainingDayExercise.exercise_id == models.Exercise.exercise_id
    ).filter(
        models.TrainingDayExercise.training_day_id.in_(day_ids)
    ).order_by(models.TrainingDayExercise.position).all()
    for tde, ex in tde_rows:
        exercises_by_day.setdefault(tde.training_day_id, []).append({
            "exercise_id": ex.exercise_id,
            "exercise_name": ex.exercise_name,
            "muscle_group": _enum_str(ex.exercise_muscle_group),
            "position": tde.position,
        })

    result_days = [
        {
            "training_day_id": d.training_day_id,
            "day_number": d.day_number,
            "day_type": _enum_str(d.day_type),
            "name": d.name,
            "muscles": muscles_by_day.get(d.training_day_id, []),
            "exercises": exercises_by_day.get(d.training_day_id, []),
        }
        for d in days
    ]

    return {"days_per_week": len(days), "days": result_days}


@api_v1.post("/routine/{user_id}")
def create_or_update_routine(user_id: int, routine_data: schemas.RoutineSetup, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Replace the user's routine with the submitted set of training days.

    Validates the whole payload before touching existing data so a rejected
    request never wipes the current routine, then does a clean-slate replace of
    the training days (cascading to muscles + exercises) inside one transaction.
    """
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    days = routine_data.days
    if not days:
        raise HTTPException(status_code=400, detail="A routine must have at least one day.")

    n_days = len(days)
    if n_days > 7:
        raise HTTPException(status_code=400, detail="A routine can have at most 7 days.")

    # Day numbers must be exactly 1..n_days (no gaps or duplicates)
    if sorted(d.day_number for d in days) != list(range(1, n_days + 1)):
        raise HTTPException(
            status_code=400,
            detail=f"Day numbers must be 1 through {n_days} with no gaps or duplicates."
        )

    valid_muscles = {m.value for m in models.MuscleGroupEnum}

    # Map the user's own exercises to their muscle group (for ownership + matching)
    ex_muscle = {
        eid: _enum_str(mg)
        for eid, mg in db.query(
            models.Exercise.exercise_id, models.Exercise.exercise_muscle_group
        ).filter(models.Exercise.user_id == user_id).all()
    }

    def _dedup(ids):
        seen = set()
        out = []
        for i in ids:
            if i not in seen:
                seen.add(i)
                out.append(i)
        return out

    # ---- Validate every day before any write ----
    for d in days:
        if d.day_type not in ("per_muscle", "manual"):
            raise HTTPException(status_code=400, detail=f"Day {d.day_number}: invalid day_type '{d.day_type}'.")

        ex_ids = _dedup(d.exercise_ids)

        # All referenced exercises must belong to the user
        for eid in ex_ids:
            if eid not in ex_muscle:
                raise HTTPException(status_code=400, detail=f"Day {d.day_number}: exercise {eid} does not belong to you.")

        if d.day_type == "manual":
            if not ex_ids:
                raise HTTPException(status_code=400, detail=f"Day {d.day_number} (manual) must include at least one exercise.")
        else:  # per_muscle
            if not d.muscles:
                raise HTTPException(status_code=400, detail=f"Day {d.day_number} (per muscle) must have at least one muscle group.")

            day_muscles = set()
            for m in d.muscles:
                if m.muscle_group not in valid_muscles:
                    raise HTTPException(status_code=400, detail=f"Day {d.day_number}: invalid muscle group '{m.muscle_group}'.")
                if m.muscle_group in day_muscles:
                    raise HTTPException(status_code=400, detail=f"Day {d.day_number}: muscle '{m.muscle_group}' listed more than once.")
                if m.exercise_count < 1:
                    raise HTTPException(status_code=400, detail=f"Day {d.day_number}: exercise count for {m.muscle_group} must be at least 1.")
                day_muscles.add(m.muscle_group)

            # Every selected exercise must belong to one of the day's muscles
            for eid in ex_ids:
                if ex_muscle[eid] not in day_muscles:
                    raise HTTPException(
                        status_code=400,
                        detail=f"Day {d.day_number}: exercise {eid} ({ex_muscle[eid]}) is not one of the day's muscle groups."
                    )

            # Pool coverage: each muscle needs at least one selected exercise
            covered = {ex_muscle[eid] for eid in ex_ids}
            missing = [mg for mg in day_muscles if mg not in covered]
            if missing:
                raise HTTPException(
                    status_code=400,
                    detail=f"Day {d.day_number}: no exercises selected for {', '.join(sorted(missing))}. Add or select at least one exercise for each muscle."
                )

    # ---- Passed validation: clean-slate replace (atomic) ----
    # Deleting the training days cascades to their muscles and exercises via FK.
    db.query(models.TrainingDay).filter(
        models.TrainingDay.user_id == user_id
    ).delete(synchronize_session=False)

    for d in days:
        training_day = models.TrainingDay(
            user_id=user_id,
            day_number=d.day_number,
            name=d.name,
            day_type=d.day_type,
        )
        db.add(training_day)
        db.flush()  # assign training_day_id

        if d.day_type == "per_muscle":
            for m in d.muscles:
                db.add(models.TrainingDayMuscle(
                    user_id=user_id,
                    training_day_id=training_day.training_day_id,
                    muscle_group=m.muscle_group,
                    exercise_count=m.exercise_count,
                ))

        for position, eid in enumerate(_dedup(d.exercise_ids)):
            db.add(models.TrainingDayExercise(
                user_id=user_id,
                training_day_id=training_day.training_day_id,
                exercise_id=eid,
                position=position,
            ))

    # ---- Reconcile the rotation pool (exercises_in_routine) ----
    # Exercises used anywhere in the new routine (per_muscle pools + manual lists).
    routine_exercise_ids = set()
    for d in days:
        routine_exercise_ids.update(_dedup(d.exercise_ids))

    existing_rows = {
        r.exercise_id: r
        for r in db.query(models.ExerciseInRoutine).filter(
            models.ExerciseInRoutine.user_id == user_id
        ).all()
    }
    existing_ids = set(existing_rows.keys())

    # Drop exercises that are no longer in the routine anywhere.
    removed_ids = existing_ids - routine_exercise_ids
    if removed_ids:
        db.query(models.ExerciseInRoutine).filter(
            models.ExerciseInRoutine.user_id == user_id,
            models.ExerciseInRoutine.exercise_id.in_(removed_ids)
        ).delete(synchronize_session=False)

    # Survivors keep their counts. Compute the per-muscle minimum among survivors
    # BEFORE inserting new rows, so several new exercises in the same muscle all
    # anchor to the same baseline instead of seeding off one another.
    survivor_ids = existing_ids & routine_exercise_ids
    survivor_min_by_muscle = {}
    for eid in survivor_ids:
        muscle = ex_muscle[eid]
        count = existing_rows[eid].times_performed
        if muscle not in survivor_min_by_muscle or count < survivor_min_by_muscle[muscle]:
            survivor_min_by_muscle[muscle] = count

    # New exercises enter at their muscle's surviving minimum (0 if the muscle is
    # new to the routine and has no survivors to anchor to).
    added_ids = routine_exercise_ids - existing_ids
    for eid in added_ids:
        db.add(models.ExerciseInRoutine(
            user_id=user_id,
            exercise_id=eid,
            times_performed=survivor_min_by_muscle.get(ex_muscle[eid], 0),
        ))

    # Keep workout_state pointer in range if the routine shrank
    state = db.query(models.WorkoutState).filter(
        models.WorkoutState.user_id == user_id
    ).first()
    if state and (state.current_day_number is None or state.current_day_number > n_days):
        state.current_day_number = 1

    db.commit()

    return {"message": "Routine saved successfully"}

@api_v1.delete("/routine/{user_id}")
def delete_routine(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Delete user's routine
    """
    # Deleting the training days cascades to their muscles and exercises;
    # also drop the rotation-pool rows.
    db.query(models.TrainingDay).filter(models.TrainingDay.user_id == user_id).delete(synchronize_session=False)
    db.query(models.ExerciseInRoutine).filter(models.ExerciseInRoutine.user_id == user_id).delete(synchronize_session=False)
    db.commit()
    
    return {"message": "Routine deleted successfully"}


# ========== WORKOUT ROUTES ==========

@api_v1.get("/workout/state/{user_id}", response_model=schemas.WorkoutStateResponse)
def get_workout_state(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
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

@api_v1.post("/workout/complete/{user_id}")
def complete_workout(user_id: int, workout_data: schemas.WorkoutComplete, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Mark workout as complete
    Main orchestration function - delegates to helpers
    """
    from datetime import date
    
    # Validate and get required data
    user, days_per_week = validate_user_and_routine(user_id, db)
    
    # Create workout session
    session = create_workout_session(user_id, workout_data.day_number, db)
    
    # Log all exercises
    completed_exercise_ids = log_workout_exercises(session, workout_data.exercises, user_id, workout_data.day_number, db)
    
    # Clean up and update state
    cleanup_after_workout(user_id, completed_exercise_ids, workout_data.day_number, days_per_week, db)
    
    db.commit()
    
    return {
        "message": "Workout completed successfully",
        "next_day": (workout_data.day_number % days_per_week) + 1,
        "session_id": session.session_id
    }


# ========== HELPER FUNCTIONS FOR COMPLETE_WORKOUT ==========

def validate_user_and_routine(user_id: int, db: Session):
    """Validate user exists and has a routine"""
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    days_per_week = db.query(models.TrainingDay).filter(
        models.TrainingDay.user_id == user_id
    ).count()
    if days_per_week == 0:
        raise HTTPException(status_code=400, detail="No routine set up")
    
    return user, days_per_week


def get_user_date(user_id: int, db: Session) -> date:
    """Get current date in user's timezone"""
    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    tz_str = user.user_timezone if user and user.user_timezone else 'America/New_York'
    tz = pytz.timezone(tz_str)
    result = datetime.now(tz).date()
    print(f"DEBUG get_user_date: user_id={user_id}, tz={tz_str}, date={result}")
    return result

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
        workout_date=get_user_date(user_id, db),
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
            workout_date=get_user_date(user_id, db),
            session_id=session.session_id
        )
        db.add(log)
        
        # Update exercise metadata
        update_exercise_metadata(exercise_data, user_id, db)
    
    return completed_exercise_ids


def update_exercise_metadata(exercise_data, user_id: int, db: Session):
    """Bump the lifetime + rotation counters and update the current weight."""
    exercise = db.query(models.Exercise).filter(
        models.Exercise.exercise_id == exercise_data.exercise_id
    ).first()

    if exercise:
        # Lifetime count (kept for stats)
        exercise.exercise_times_performed += 1

        # Update the current weight if a value was logged
        if exercise_data.weight_used and exercise_data.weight_used > 0:
            exercise.exercise_user_current_weight = exercise_data.weight_used

    # Rotation counter: the single per-exercise pool counter generation reads.
    pool_row = db.query(models.ExerciseInRoutine).filter(
        models.ExerciseInRoutine.user_id == user_id,
        models.ExerciseInRoutine.exercise_id == exercise_data.exercise_id
    ).first()
    if pool_row:
        pool_row.times_performed += 1


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
    state.last_workout_date = get_user_date(user_id, db)

@api_v1.get("/workout/logs/{user_id}")
def get_workout_logs(user_id: int, limit: int = 30, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get user's workout history (last 30 days by default)
    """
    logs = db.query(models.WorkoutLog).filter(
        models.WorkoutLog.user_id == user_id
    ).order_by(models.WorkoutLog.workout_date.desc()).limit(limit).all()
    
    return logs


# ========== NEXT WORKOUT SELECTION ROUTES ==========

@api_v1.get("/next-workout/selections/{user_id}")
def get_next_workout_selections(user_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get user's manually selected exercises for next workout
    """
    selections = db.query(models.NextWorkoutSelection).filter(
        models.NextWorkoutSelection.user_id == user_id
    ).all()
    
    return selections

@api_v1.post("/next-workout/toggle")
def toggle_next_workout_selection(data: dict, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    """
    Toggle exercise selection for next workout
    """
    # Coerce ids to int: the body may arrive with a string user_id (localStorage),
    # and a strict int-vs-string compare would wrongly 403 the ownership check.
    user_id = int(data['user_id'])
    exercise_id = int(data['exercise_id'])
    is_selected = data['is_selected']

    # Authorization: the body's user_id must match the logged-in user
    if current_user.user_id != user_id:
        raise HTTPException(status_code=403, detail="Forbidden")
    
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

@api_v1.delete("/next-workout/clear/{user_id}")
def clear_next_workout_selections(user_id: int, day_number: int = None, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
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
        # Clear only the selections for exercises in that training day's pool.
        training_day = db.query(models.TrainingDay).filter(
            models.TrainingDay.user_id == user_id,
            models.TrainingDay.day_number == day_number
        ).first()

        pool_ids = []
        if training_day:
            pool_ids = [
                row.exercise_id
                for row in db.query(models.TrainingDayExercise.exercise_id).filter(
                    models.TrainingDayExercise.training_day_id == training_day.training_day_id
                ).all()
            ]

        if pool_ids:
            db.query(models.NextWorkoutSelection).filter(
                models.NextWorkoutSelection.user_id == user_id,
                models.NextWorkoutSelection.exercise_id.in_(pool_ids)
            ).delete(synchronize_session=False)
    
    db.commit()
    
    return {"message": "Selections cleared"}


@api_v1.post("/next-workout/generate/{user_id}")
def generate_next_workout(user_id: int, day_number: int = None, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Auto-generate the next workout for a training day.

    per_muscle day: for each muscle, take the least-performed exercises from that
    day's pool for the muscle, up to the muscle's exercise_count.
    manual day: take every exercise in the day's list.
    The chosen exercises are written as NextWorkoutSelection rows.
    """
    target_day = determine_target_day(user_id, day_number, db)

    training_day = db.query(models.TrainingDay).filter(
        models.TrainingDay.user_id == user_id,
        models.TrainingDay.day_number == target_day
    ).first()
    if not training_day:
        raise HTTPException(status_code=400, detail=f"No routine day found for day {target_day}.")

    chosen_ids = choose_exercises_for_day(user_id, training_day, db)

    # Replace this day's selections with the freshly chosen ones
    clear_selections_for_training_day(user_id, training_day, db)
    db.bulk_save_objects([
        models.NextWorkoutSelection(user_id=user_id, exercise_id=eid, is_selected=True)
        for eid in chosen_ids
    ])

    db.commit()

    return {
        "message": "Next workout generated",
        "exercises_selected": len(chosen_ids),
        "day_number": target_day
    }


# ========== HELPER FUNCTIONS FOR GENERATE_NEXT_WORKOUT ==========

def determine_target_day(user_id: int, day_number: int, db: Session):
    """Determine which day to generate for (explicit arg, else the current-state day)."""
    if day_number is not None:
        return day_number

    state = db.query(models.WorkoutState).filter(
        models.WorkoutState.user_id == user_id
    ).first()

    if not state:
        raise HTTPException(status_code=400, detail="No workout state found")

    return state.current_day_number


def clear_selections_for_training_day(user_id: int, training_day, db: Session):
    """Clear NextWorkoutSelection rows for the exercises in this day's pool."""
    pool_ids = [
        row.exercise_id
        for row in db.query(models.TrainingDayExercise.exercise_id).filter(
            models.TrainingDayExercise.training_day_id == training_day.training_day_id
        ).all()
    ]
    if pool_ids:
        db.query(models.NextWorkoutSelection).filter(
            models.NextWorkoutSelection.user_id == user_id,
            models.NextWorkoutSelection.exercise_id.in_(pool_ids)
        ).delete(synchronize_session=False)


def choose_exercises_for_day(user_id: int, training_day, db: Session):
    """
    Decide which exercises make up the generated workout for this day.

    manual: every exercise in the day's list (ordered by position).
    per_muscle: for each muscle, the least-performed exercises from the day's
    pool for that muscle (by exercises_in_routine.times_performed, random
    tiebreak), up to that muscle's exercise_count.
    """
    import random

    day_type = _enum_str(training_day.day_type)

    if day_type == "manual":
        rows = db.query(models.TrainingDayExercise.exercise_id).filter(
            models.TrainingDayExercise.training_day_id == training_day.training_day_id
        ).order_by(models.TrainingDayExercise.position).all()
        return [r[0] for r in rows]

    # per_muscle: how many exercises to draw for each muscle on this day
    counts = {
        _enum_str(m.muscle_group): m.exercise_count
        for m in db.query(models.TrainingDayMuscle).filter(
            models.TrainingDayMuscle.training_day_id == training_day.training_day_id
        ).all()
    }

    # The day's pool joined to each exercise's muscle group and rotation counter
    pool = db.query(
        models.TrainingDayExercise.exercise_id,
        models.Exercise.exercise_muscle_group,
        models.ExerciseInRoutine.times_performed
    ).join(
        models.Exercise,
        models.TrainingDayExercise.exercise_id == models.Exercise.exercise_id
    ).outerjoin(
        models.ExerciseInRoutine,
        (models.ExerciseInRoutine.exercise_id == models.TrainingDayExercise.exercise_id) &
        (models.ExerciseInRoutine.user_id == user_id)
    ).filter(
        models.TrainingDayExercise.training_day_id == training_day.training_day_id
    ).all()

    # Group the pool by muscle
    by_muscle = {}
    for exercise_id, muscle_group, times_performed in pool:
        mg = _enum_str(muscle_group)
        by_muscle.setdefault(mg, []).append(
            (exercise_id, times_performed if times_performed is not None else 0)
        )

    chosen = []
    for mg, items in by_muscle.items():
        # Randomize first so equal counters break randomly, then sort by counter asc
        random.shuffle(items)
        items.sort(key=lambda t: t[1])
        take = counts.get(mg, 0)
        chosen.extend(exercise_id for exercise_id, _ in items[:take])

    return chosen


# ========== WORKOUT SESSION ROUTES ==========

@api_v1.get("/workout/sessions/{user_id}")
def get_workout_sessions(user_id: int, limit: int = 10, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get user's last N workout sessions (most recent first)
    """
    sessions = db.query(models.WorkoutSession).filter(
        models.WorkoutSession.user_id == user_id
    ).order_by(models.WorkoutSession.session_order.desc()).limit(limit).all()
    
    # Reverse to show oldest to newest (for display left to right)
    return list(reversed(sessions))


@api_v1.post("/workout/logs-by-sessions/{user_id}")
def get_workout_logs_by_sessions(user_id: int, data: dict, db: Session = Depends(get_db), current_user: models.User = Depends(verify_user_access)):
    """
    Get workout logs for specific session IDs
    """
    session_ids = data.get('session_ids', [])
    
    logs = db.query(models.WorkoutLog).filter(
        models.WorkoutLog.user_id == user_id,
        models.WorkoutLog.session_id.in_(session_ids)
    ).all()
    return logs

app.include_router(api_v1)

from mangum import Mangum
handler = Mangum(app)