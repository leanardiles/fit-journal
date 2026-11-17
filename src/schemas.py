import re

from pydantic import BaseModel, ConfigDict, EmailStr, field_validator

from models import MuscleGroupEnum
from typing import Dict, List, Optional
from datetime import datetime
from datetime import date

# ========== DASHBOARD SCHEMAS ==========

_NOTE_ID = re.compile(r"^[A-Za-z0-9_-]{1,40}$")
_MAX_NOTE_IDS = 40


def _clean_note_ids(value):
    """
    Note ids are written by the client and stored verbatim, so they are
    bounded here rather than trusted. The cap is not about any one user
    misbehaving with their own row: it is that an unbounded list in a JSON
    column is an unbounded write, and the only thing standing between that and
    the database is this validator.
    """
    if value is None:
        return []
    if len(value) > _MAX_NOTE_IDS:
        raise ValueError(f"at most {_MAX_NOTE_IDS} note ids")
    seen = []
    for item in value:
        if not isinstance(item, str) or not _NOTE_ID.match(item):
            raise ValueError("note ids must be 1 to 40 characters of A-Z, a-z, 0-9, _ or -")
        if item not in seen:          # duplicates would make the order ambiguous
            seen.append(item)
    return seen


_MAX_GOAL_SETS = 100
_VALID_MUSCLES = {m.value for m in MuscleGroupEnum}


class WeeklySetGoals(BaseModel):
    """
    Target sets per muscle group for a Monday-to-Sunday week.

    Weekly by definition, which is why the note that renders it has no range
    control: progress toward a weekly target measured over a rolling seven
    days is not a number that means anything.
    """
    goals: Dict[str, int] = {}

    @field_validator("goals")
    @classmethod
    def _validate_goals(cls, v):
        if v is None:
            return {}
        cleaned = {}
        for muscle, target in v.items():
            if muscle not in _VALID_MUSCLES:
                raise ValueError(f"unknown muscle group: {muscle}")
            if not isinstance(target, int) or isinstance(target, bool):
                raise ValueError("a target must be a whole number of sets")
            # Zero is refused rather than accepted and stored: a muscle with no
            # goal is one that is absent from the map, and allowing both would
            # give two spellings of the same thing for the UI to disagree about.
            if target < 1 or target > _MAX_GOAL_SETS:
                raise ValueError(f"a target must be between 1 and {_MAX_GOAL_SETS} sets")
            cleaned[muscle] = target
        return cleaned

    model_config = ConfigDict(from_attributes=True)


class DashboardNotes(BaseModel):
    """
    What the user sees on their dashboard. `order` is every note they have an
    opinion about, most important first; `hidden` is the subset switched off.
    A note in neither list is one this user has not met yet.
    """
    order: List[str] = []
    hidden: List[str] = []

    @field_validator("order", "hidden")
    @classmethod
    def _validate_ids(cls, v):
        return _clean_note_ids(v)

    model_config = ConfigDict(from_attributes=True)

# ========== USER SCHEMAS ==========

class UserBase(BaseModel):
    user_email: EmailStr

class UserCreate(UserBase):
    user_password: str

class UserLogin(UserBase):
    user_password: str

class AccountDeleteRequest(BaseModel):
    user_password: str

class EmailVerifyRequest(BaseModel):
    user_email: EmailStr
    code: str

class ResendVerificationRequest(BaseModel):
    user_email: EmailStr

class ForgotPasswordRequest(BaseModel):
    user_email: EmailStr

class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str    

class UserResponse(UserBase):
    user_id: int
    user_first_name: Optional[str] = None
    user_last_name: Optional[str] = None
    user_created_at: datetime

    model_config = ConfigDict(from_attributes=True)

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
    user_locale: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class UserProfileUpdate(BaseModel):
    user_first_name: Optional[str] = None
    user_sex: Optional[str] = None  # 'M', 'F', 'NB'
    user_age: Optional[int] = None
    user_unit_preference: Optional[str] = None  # 'metric', 'imperial'
    user_height: Optional[float] = None
    user_weight: Optional[float] = None
    user_timezone: Optional[str] = None
    user_locale: Optional[str] = None   # BCP 47 tag, e.g. "es-419"; None leaves unchanged

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
    comments: Optional[str] = None    

class ExerciseResponse(ExerciseBase):
    exercise_id: int
    user_id: int
    exercise_user_current_weight: Optional[float] = None
    exercise_times_performed: int
    exercise_created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ========== ROUTINE SCHEMAS ==========

class TrainingDayMuscleCreate(BaseModel):
    muscle_group: str            # validated against MuscleGroupEnum at the endpoint
    exercise_count: int = 3      # how many exercises to draw for this muscle (per_muscle)


class TrainingDayExerciseGroup(BaseModel):
    exercise_id: int
    group_index: int    # 0 = group A, 1 = B, ... (also the group's order)
    position: int       # order within the group (0-based)


class TrainingDayCreate(BaseModel):
    day_number: int                              # 1-7
    day_type: str                                # "per_muscle" or "manual"
    name: Optional[str] = None                   # optional label, e.g. "Lower A"
    muscles: list[TrainingDayMuscleCreate] = []  # per_muscle days: muscles + counts
    exercise_ids: list[int] = []                 # per_muscle: the pool; manual: exact list
    exercise_groups: list[TrainingDayExerciseGroup] = []  # manual days only: superset grouping


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
    group_index: Optional[int] = None    # manual days: which group (0=A...); NULL = ungrouped


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
    
    model_config = ConfigDict(from_attributes=True)

class WorkoutLogCreate(BaseModel):
    exercise_id: int
    sets_completed: int
    # None means the user left it blank, which is not the same as zero reps.
    reps_completed: Optional[int] = None
    weight_used: Optional[float] = None

class WorkoutComplete(BaseModel):
    day_number: int
    exercises: list[WorkoutLogCreate]
    # Optional so existing callers, including the Android app, keep working
    # unchanged. Omitted means today in the user's timezone.
    workout_date: Optional[date] = None

class WorkoutLogResponse(BaseModel):
    log_id: int
    user_id: int
    routine_day_number: int
    exercise_id: int
    sets_completed: int
    reps_completed: Optional[int]
    weight_used: Optional[float]
    workout_date: date
    
    model_config = ConfigDict(from_attributes=True)