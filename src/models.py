from sqlalchemy import Column, Integer, String, DECIMAL, Enum, Boolean, TIMESTAMP, ForeignKey, Text, CheckConstraint, text, Date
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
    user_subscription = Column(Integer, default=0)  # TINYINT in MySQL, this is not being used for the time being
    user_is_active = Column(Boolean, default=True)
    user_created_at = Column(TIMESTAMP, server_default=func.current_timestamp())
    user_updated_at = Column(TIMESTAMP, server_default=func.current_timestamp(), onupdate=func.current_timestamp())
    next_workout_selections = relationship("NextWorkoutSelection", cascade="all, delete-orphan")


    # Relationships (ADD THE NEW ONES HERE)
    exercises = relationship("Exercise", back_populates="user", cascade="all, delete-orphan")
    routines = relationship("Routine", back_populates="user", cascade="all, delete-orphan")
    routine_muscles = relationship("RoutineMusclePerDay", back_populates="user", cascade="all, delete-orphan")
    workout_state = relationship("WorkoutState", back_populates="user", uselist=False, cascade="all, delete-orphan")  # ADD THIS
    workout_logs = relationship("WorkoutLog", back_populates="user", cascade="all, delete-orphan")  # ADD THIS

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
    exercise_is_in_routine = Column(Boolean, default=True)
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



class Routine(Base):
    __tablename__ = "routine_days"
    
    routine_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    days_per_week = Column(Integer, CheckConstraint('days_per_week >= 1 AND days_per_week <= 7'), nullable=False)
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
    
    # Relationship
    user = relationship("User", back_populates="routines")
    
    # Table constraint
    __table_args__ = (
        CheckConstraint('days_per_week >= 1 AND days_per_week <= 7', name='check_days_per_week'),
    )



class RoutineMusclePerDay(Base):
    __tablename__ = "routine_muscles_per_day"
    
    routine_day_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    day_number = Column(Integer, nullable=False)  # 1-7
    muscle_group = Column(Enum(MuscleGroupEnum), nullable=False)  # Use MuscleGroupEnum
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    updated_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
    
    # Relationship
    user = relationship("User", back_populates="routine_muscles")



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
    routine_day_number = Column(Integer, nullable=False)
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
    session = relationship("WorkoutSession")  # ADD THIS LINE


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
    routine_day_number = Column(Integer, nullable=False)
    workout_date = Column(Date, nullable=False)
    session_order = Column(Integer, nullable=False)
    created_at = Column(TIMESTAMP, server_default=text('CURRENT_TIMESTAMP'))
    
    # Relationship
    user = relationship("User")