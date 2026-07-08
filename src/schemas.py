from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from datetime import date

# ========== USER SCHEMAS ==========

class UserBase(BaseModel):
    user_email: EmailStr

class UserCreate(UserBase):
    user_password: str

class UserLogin(UserBase):
    user_password: str

class AccountDeleteRequest(BaseModel):
    user_password: str

class UserResponse(UserBase):
    user_id: int
    user_first_name: Optional[str] = None
    user_last_name: Optional[str] = None
    user_created_at: datetime

    class Config:
        from_attributes = True

# ========== PROFILE SCHEMAS ==========

class UserProfileResponse(BaseModel):
    user_id: int
    user_email: EmailStr
    user_first_name: Optional[str] = None
    user_sex: Optional[str] = None
    user_age: Optional[int] = None
    user_unit_preference: Optional[str] = "metric"
    user_height: Optional[float] = None
    user_weight: Optional[float] = None
    user_timezone: Optional[str] = "America/New_York"

    class Config:
        from_attributes = True

class UserProfileUpdate(BaseModel):
    user_first_name: Optional[str] = None
    user_sex: Optional[str] = None  # 'M', 'F', 'NB'
    user_age: Optional[int] = None
    user_unit_preference: Optional[str] = None  # 'metric', 'imperial'
    user_height: Optional[float] = None
    user_weight: Optional[float] = None
    user_timezone: Optional[str] = None

# ========== EXERCISE SCHEMAS ==========

class ExerciseBase(BaseModel):
    exercise_name: str
    exercise_muscle_group: str

class ExerciseCreate(ExerciseBase):
    exercise_user_current_weight: Optional[float] = None
    exercise_link: Optional[str] = None
    comments: Optional[str] = None

class ExerciseUpdate(BaseModel):
    exercise_name: Optional[str] = None
    exercise_muscle_group: Optional[str] = None
    exercise_user_current_weight: Optional[float] = None
    exercise_link: Optional[str] = None
    exercise_is_in_routine: Optional[bool] = None
    comments: Optional[str] = None    

class ExerciseResponse(ExerciseBase):
    exercise_id: int
    user_id: int
    exercise_user_current_weight: Optional[float] = None
    exercise_is_in_routine: bool
    exercise_times_performed: int
    exercise_created_at: datetime

    class Config:
        from_attributes = True


# ========== ROUTINE SCHEMAS ==========

class TrainingDayMuscleCreate(BaseModel):
    muscle_group: str            # validated against MuscleGroupEnum at the endpoint
    exercise_count: int = 3      # how many exercises to draw for this muscle (per_muscle)


class TrainingDayCreate(BaseModel):
    day_number: int                              # 1-7
    day_type: str                                # "per_muscle" or "manual"
    name: Optional[str] = None                   # optional label, e.g. "Lower A"
    muscles: list[TrainingDayMuscleCreate] = []  # per_muscle days: muscles + counts
    exercise_ids: list[int] = []                 # per_muscle: the pool; manual: exact list


class RoutineSetup(BaseModel):
    days: list[TrainingDayCreate]                # days_per_week is derived from len(days)


# --- Response shapes ---

class TrainingDayMuscleResponse(BaseModel):
    muscle_group: str
    exercise_count: int


class TrainingDayExerciseResponse(BaseModel):
    exercise_id: int
    exercise_name: str
    muscle_group: str
    position: Optional[int] = None


class TrainingDayResponse(BaseModel):
    training_day_id: int
    day_number: int
    day_type: str
    name: Optional[str] = None
    muscles: list[TrainingDayMuscleResponse] = []
    exercises: list[TrainingDayExerciseResponse] = []


class RoutineResponse(BaseModel):
    days_per_week: int                           # derived = len(days)
    days: list[TrainingDayResponse]


    # ========== WORKOUT SCHEMAS ==========

class WorkoutStateResponse(BaseModel):
    state_id: int
    user_id: int
    current_day_number: int
    last_workout_date: Optional[date] = None
    
    class Config:
        from_attributes = True

class WorkoutLogCreate(BaseModel):
    exercise_id: int
    sets_completed: int
    reps_completed: int
    weight_used: Optional[float] = None

class WorkoutComplete(BaseModel):
    day_number: int
    exercises: list[WorkoutLogCreate]

class WorkoutLogResponse(BaseModel):
    log_id: int
    user_id: int
    routine_day_number: int
    exercise_id: int
    sets_completed: int
    reps_completed: int
    weight_used: Optional[float]
    workout_date: date
    
    class Config:
        from_attributes = True