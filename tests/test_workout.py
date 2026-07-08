"""
Workout generation + completion tests for the training-day model.

Generation:
  - per_muscle day: picks the least-performed exercises from that day's pool,
    up to each muscle's exercise_count, writing NextWorkoutSelection rows.
  - manual day: selects the whole day list.
Completion:
  - logs the exercises and increments each one's exercises_in_routine counter.
"""

from database import SessionLocal
import models


# --- helpers ----------------------------------------------------------------

def _make_exercise(auth, name, muscle):
    r = auth["client"].post(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"],
        json={"exercise_name": name, "exercise_muscle_group": muscle},
    )
    assert r.status_code in (200, 201), r.text
    return r.json()["exercise_id"]


def _save(auth, days):
    r = auth["client"].post(
        f"/v1/routine/{auth['user_id']}",
        headers=auth["headers"],
        json={"days": days},
    )
    assert r.status_code in (200, 201), r.text
    return r


def _generate(auth, day_number=1):
    r = auth["client"].post(
        f"/v1/next-workout/generate/{auth['user_id']}?day_number={day_number}",
        headers=auth["headers"],
    )
    assert r.status_code == 200, r.text
    return r.json()


def _selected_ids(auth):
    """The exercise ids currently marked is_selected for the user."""
    r = auth["client"].get(
        f"/v1/next-workout/selections/{auth['user_id']}", headers=auth["headers"]
    )
    assert r.status_code == 200, r.text
    return {s["exercise_id"] for s in r.json() if s["is_selected"]}


def _counter(user_id, exercise_id):
    session = SessionLocal()
    try:
        row = session.query(models.ExerciseInRoutine).filter(
            models.ExerciseInRoutine.user_id == user_id,
            models.ExerciseInRoutine.exercise_id == exercise_id,
        ).first()
        return row.times_performed if row else None
    finally:
        session.close()


def _set_counter(user_id, exercise_id, value):
    session = SessionLocal()
    try:
        row = session.query(models.ExerciseInRoutine).filter(
            models.ExerciseInRoutine.user_id == user_id,
            models.ExerciseInRoutine.exercise_id == exercise_id,
        ).first()
        assert row is not None
        row.times_performed = value
        session.commit()
    finally:
        session.close()


# --- generation: per_muscle -------------------------------------------------

def test_generate_per_muscle_selects_count_from_pool(auth):
    """A per_muscle day selects exercise_count exercises drawn from its pool."""
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")
    c3 = _make_exercise(auth, "Cable Fly", "Chest")

    _save(auth, [{
        "day_number": 1,
        "day_type": "per_muscle",
        "muscles": [{"muscle_group": "Chest", "exercise_count": 2}],
        "exercise_ids": [c1, c2, c3],
    }])

    result = _generate(auth, day_number=1)
    assert result["exercises_selected"] == 2

    selected = _selected_ids(auth)
    assert len(selected) == 2
    assert selected.issubset({c1, c2, c3})   # drawn from the pool


def test_generate_per_muscle_rotates_least_performed(auth):
    """With count=1, generation picks the least-performed exercise in the pool."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")
    c3 = _make_exercise(auth, "Cable Fly", "Chest")

    _save(auth, [{
        "day_number": 1,
        "day_type": "per_muscle",
        "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
        "exercise_ids": [c1, c2, c3],
    }])

    # Make c3 clearly the least-performed.
    _set_counter(uid, c1, 5)
    _set_counter(uid, c2, 5)
    _set_counter(uid, c3, 0)

    _generate(auth, day_number=1)

    selected = _selected_ids(auth)
    assert selected == {c3}


# --- generation: manual -----------------------------------------------------

def test_generate_manual_selects_whole_list(auth):
    """A manual day selects every exercise in its list."""
    a = _make_exercise(auth, "Deadlift", "Back")
    b = _make_exercise(auth, "Curl", "Biceps")
    c = _make_exercise(auth, "Plank", "Abs")

    _save(auth, [{
        "day_number": 1,
        "day_type": "manual",
        "exercise_ids": [a, b, c],
    }])

    result = _generate(auth, day_number=1)
    assert result["exercises_selected"] == 3
    assert _selected_ids(auth) == {a, b, c}


# --- generation: bad day ----------------------------------------------------

def test_generate_missing_day_rejected(auth):
    """Generating for a day the routine doesn't define is rejected."""
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    _save(auth, [{
        "day_number": 1,
        "day_type": "per_muscle",
        "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
        "exercise_ids": [c1],
    }])

    r = auth["client"].post(
        f"/v1/next-workout/generate/{auth['user_id']}?day_number=2",
        headers=auth["headers"],
    )
    assert r.status_code == 400, r.text


# --- completion -------------------------------------------------------------

def test_complete_workout_increments_pool_counter(auth):
    """Completing a workout increments exercises_in_routine.times_performed."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")

    _save(auth, [{
        "day_number": 1,
        "day_type": "per_muscle",
        "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
        "exercise_ids": [c1],
    }])

    # Ensure workout_state exists (lazily created by the state endpoint).
    auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"])

    _generate(auth, day_number=1)
    assert _counter(uid, c1) == 0   # not yet performed

    resp = auth["client"].post(
        f"/v1/workout/complete/{uid}",
        headers=auth["headers"],
        json={
            "day_number": 1,
            "exercises": [
                {"exercise_id": c1, "sets_completed": 3, "reps_completed": 10, "weight_used": 50.0},
            ],
        },
    )
    assert resp.status_code == 200, resp.text

    assert _counter(uid, c1) == 1   # incremented on completion