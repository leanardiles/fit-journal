from sqlalchemy import Column, Integer, String, DECIMAL, Enum, Boolean, TIMESTAMP, ForeignKey, Text, CheckConstraint, UniqueConstraint, text, Date
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base
import enum

# ========== ENUMS ==========

class SexEnum(str, enum.Enum):
    M = "M"
    F = "F"
    NB = "NB"

class UnitPreferenceEnum(str, enum.Enum):
    metric = "metric"
    imperial = "imperial"

class MuscleGroupEnum(str, enum.Enum):
    Abs = "Abs"
    Back = "Back"
    Biceps = "Biceps"
    Calves = "Calves"
    Chest = "Chest"
    Forearms = "Forearms"
    Glutes = "Glutes"
    Legs = "Legs"
    Shoulders = "Shoulders"
    Triceps = "Triceps"

class DayTypeEnum(str, enum.Enum):
    per_muscle = "per_muscle"
    manual = "manual"

class AuthTokenPurposeEnum(str, enum.Enum):
    email_verify = "email_verify"
    password_reset = "password_reset"

# ========== MODELS ==========

class User(Base):
    __tablename__ = "users"

    user_id = Column(Integer, primary_key=True, autoincrement=True)
    user_email = Column(String(100), unique=True, nullable=False)
    user_password = Column(String(255), nullable=False)
    user_first_name = Column(String(50), default=None)
    user_last_name = Column(String(50), default=None)
    user_sex = Column(Enum(SexEnum), default=None)
    user_age = Column(Integer, default=None)
    user_unit_preference = Column(Enum(UnitPreferenceEnum), default="metric")
    user_weight = Column(DECIMAL(5, 2), default=None)
    user_height = Column(Integer, default=None)
    user_timezone = Column(String(50), default='America/New_York')
    user_locale = Column(String(35), nullable=True)   # BCP 47 tag; NULL = auto-detect
    user_subscription = Column(Integer, default=0)  # TINYINT in MySQL, this is not being used for the time being
    user_is_active = Column(Boolean, default=True)
    user_email_verified = Column(Boolean, nullable=False, default=False, server_default=text("0"))
    user_created_at = Column(TIMESTAMP, server_default=func.current_timestamp())
    user_updated_at = Column(TIMESTAMP, server_default=func.current_timestamp(), onupdate=func.current_timestamp())
    next_workout_selections = relationship("NextWorkoutSelection", cascade="all, delete-orphan")


    # Relationships (ADD THE NEW ONES HERE)
    exercises = relationship("Exercise", back_populates="user", cascade="all, delete-orphan")
    workout_state = relationship("WorkoutState", back_populates="user", uselist=False, cascade="all, delete-orphan")  # ADD THIS
    workout_logs = relationship("WorkoutLog", back_populates="user", cascade="all, delete-orphan")  # ADD THIS
    training_days = relationship("TrainingDay", back_populates="user", cascade="all, delete-orphan")
    exercises_in_routine = relationship("ExerciseInRoutine", cascade="all, delete-orphan")

    # Table constraints
    __table_args__ = (
        CheckConstraint('user_age >= 0 AND user_age <= 100', name='check_user_age'),
        CheckConstraint('user_weight > 0 AND user_weight <= 300', name='check_user_weight'),
        CheckConstraint('user_height > 0 AND user_height <= 300', name='check_user_height'),
    )



class DefaultExercise(Base):
    __tablename__ = "default_exercises"

    default_exercise_id = Column(Integer, primary_key=True, autoincrement=True)
    exercise_name = Column(String(50), nullable=False)
    exercise_muscle_group = Column(Enum(MuscleGroupEnum), nullable=False)
    exercise_link = Column(String(500), default=None)



class Exercise(Base):
    __tablename__ = "exercises"

    exercise_id = Column(Integer, primary_key=True, autoincrement=True)
    exercise_name = Column(String(50), nullable=False)
    exercise_muscle_group = Column(Enum(MuscleGroupEnum), nullable=False)
    exercise_user_current_weight = Column(DECIMAL(5, 2), default=None)
    user_id = Column(Integer, ForeignKey('users.user_id', ondelete='CASCADE'), nullable=False)
    exercise_times_performed = Column(Integer, default=0)
    exercise_link = Column(String(500), default=None)
    comments = Column(String(300), default=None)
    exercise_created_at = Column(TIMESTAMP, server_default=func.current_timestamp())
    exercise_updated_at = Column(TIMESTAMP, server_default=func.current_timestamp(), onupdate=func.current_timestamp())

    # Relationship
    user = relationship("User", back_populates="exercises")

    # Table constraint
    __table_args__ = (
        CheckConstraint('exercise_user_current_weight >= 0 AND exercise_user_current_weight <= 300', name='check_exercise_weight'),
    )



class TrainingDay(Base):
    """
    A single day within a user's routine. First-class so it can carry a name
    and a type. day_type decides how the day is resolved at generation time:
    per_muscle (rotate within selected pools) or manual (fixed exercise list).
    """
    __tablename__ = "training_days"

    training_day_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    day_number = Column(Integer, nullable=False)  # 1-7, ordinal within the routine
    name = Column(String(50), default=None)  # optional label, e.g. "Lower A"
    day_type = Column(Enum(DayTypeEnum), nullable=False, default="per_muscle")
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))

    # Relationships
    user = relationship("User", back_populates="training_days")
    muscles = relationship("TrainingDayMuscle", back_populates="training_day", cascade="all, delete-orphan")
    exercises = relationship("TrainingDayExercise", back_populates="training_day", cascade="all, delete-orphan")

    __table_args__ = (
        UniqueConstraint('user_id', 'day_number', name='uq_training_day_user_day'),
    )



class TrainingDayMuscle(Base):
    """
    A muscle group targeted on a per_muscle day, plus how many exercises to
    draw for it. At generation the algorithm rotates within the day's selected
    pool for this muscle and picks exercise_count of them by least-performed.
    """
    __tablename__ = "training_day_muscles"

    training_day_muscle_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    training_day_id = Column(Integer, ForeignKey("training_days.training_day_id", ondelete="CASCADE"), nullable=False)
    muscle_group = Column(Enum(MuscleGroupEnum), nullable=False)
    exercise_count = Column(Integer, nullable=False, default=3)
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))

    # Relationships
    training_day = relationship("TrainingDay", back_populates="muscles")

    __table_args__ = (
        UniqueConstraint('training_day_id', 'muscle_group', name='uq_training_day_muscle'),
        CheckConstraint('exercise_count >= 1', name='check_exercise_count'),
    )



class TrainingDayExercise(Base):
    """
    An exercise attached to a training day.
    - per_muscle day: part of the selected pool the generator rotates within,
      grouped by the exercise's own muscle group.
    - manual day: an exact exercise to include every session.
    """
    __tablename__ = "training_day_exercises"

    training_day_exercise_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    training_day_id = Column(Integer, ForeignKey("training_days.training_day_id", ondelete="CASCADE"), nullable=False)
    exercise_id = Column(Integer, ForeignKey("exercises.exercise_id", ondelete="CASCADE"), nullable=False)
    position = Column(Integer, default=None)  # order within its group (0-based)
    group_index = Column(Integer, default=None)  # manual days: which group (0=A, 1=B...); NULL = ungrouped
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))

    # Relationships
    training_day = relationship("TrainingDay", back_populates="exercises")
    exercise = relationship("Exercise")

    __table_args__ = (
        UniqueConstraint('training_day_id', 'exercise_id', name='uq_training_day_exercise'),
    )


class ExerciseInRoutine(Base):
    """
    One row per (user, exercise) that is currently in the routine anywhere.
    Holds times_performed: the rotation counter shared across every training
    day that uses the exercise (the single pool). Row existence supersedes the
    old exercise_is_in_routine flag. Reconciled on each routine save: survivors
    keep their count, dropped exercises lose their row, and new exercises enter
    at their muscle's surviving minimum.
    """
    __tablename__ = "exercises_in_routine"

    exercise_in_routine_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    exercise_id = Column(Integer, ForeignKey("exercises.exercise_id", ondelete="CASCADE"), nullable=False)
    times_performed = Column(Integer, nullable=False, default=0)
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))

    # Relationships
    exercise = relationship("Exercise")

    __table_args__ = (
        UniqueConstraint('user_id', 'exercise_id', name='uq_exercise_in_routine'),
    )



class WorkoutState(Base):
    __tablename__ = "workout_state"
    
    state_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False, unique=True)
    current_day_number = Column(Integer, default=1)
    last_workout_date = Column(Date, default=None)
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
    
    # Relationship
    user = relationship("User", back_populates="workout_state")



class WorkoutLog(Base):
    __tablename__ = "workout_logs"
    
    log_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    routine_day_number = Column(Integer, nullable=True)
    exercise_id = Column(Integer, ForeignKey("exercises.exercise_id", ondelete="CASCADE"), nullable=False)
    sets_completed = Column(Integer, default=0)
    reps_completed = Column(Integer, default=0)
    weight_used = Column(DECIMAL(5, 2), default=None)
    workout_date = Column(Date, nullable=False)
    session_id = Column(Integer, ForeignKey("workout_sessions.session_id", ondelete="CASCADE"), nullable=True)  # ADD THIS LINE
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    
    # Relationships
    user = relationship("User", back_populates="workout_logs")
    exercise = relationship("Exercise")
    session = relationship("WorkoutSession")


class NextWorkoutSelection(Base):
    __tablename__ = "next_workout_selections"
    
    selection_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    exercise_id = Column(Integer, ForeignKey("exercises.exercise_id", ondelete="CASCADE"), nullable=False)
    is_selected = Column(Boolean, default=False)
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
    
    # Relationships
    user = relationship("User")
    exercise = relationship("Exercise")


class WorkoutSession(Base):
    __tablename__ = "workout_sessions"
    
    session_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    routine_day_number = Column(Integer, nullable=True)
    workout_date = Column(Date, nullable=False)
    session_order = Column(Integer, nullable=False)
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    
    # Relationship
    user = relationship("User")

class AuthToken(Base):
    """
    Short-lived secrets for email verification (6-digit code) and password
    reset (URL token). Only a SHA-256 hash of the value is stored, never the
    plaintext. Rows are single-use (used_at) and time-limited (expires_at);
    reissuing supersedes the previous unused row for that (user, purpose).
    """
    __tablename__ = "auth_tokens"

    auth_token_id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False, index=True)
    purpose = Column(Enum(AuthTokenPurposeEnum), nullable=False)
    token_hash = Column(String(64), nullable=False)   # sha256 hex digest
    expires_at = Column(TIMESTAMP, nullable=False)
    used_at = Column(TIMESTAMP, nullable=True)
    attempts = Column(Integer, nullable=False, default=0)
    created_at = Column(TIMESTAMP, server_default=text("CURRENT_TIMESTAMP"))

    user = relationship("User")    