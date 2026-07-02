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

class RoutineDayCreate(BaseModel):
    day_number: int  # 1-7
    muscle_groups: list[str]  # List of muscle groups for this day

class RoutineDayResponse(BaseModel):
    routine_day_id: int
    user_id: int
    day_number: int
    muscle_group: str
    
    class Config:
        from_attributes = True

class RoutineSetup(BaseModel):
    days_per_week: int  # 1-7
    routine_days: list[RoutineDayCreate]  # Each day with its muscle groups

class RoutineResponse(BaseModel):
    days_per_week: int
    routine_days: dict[int, list[str]]  # day_number -> list of muscle groups


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