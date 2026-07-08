"""
Reconcile tests: the exercises_in_routine rotation-pool sync inside the
routine save endpoint.

On save, create_or_update_routine reconciles the exercises_in_routine table
(which holds the times_performed rotation counter) against the new routine:

  - exercises still in the routine keep their counter untouched  (survivors)
  - exercises no longer in the routine lose their row            (removed)
  - newly added exercises enter at the MINIMUM counter among the surviving
    exercises of the SAME muscle, or 0 if that muscle has no survivors (added)

These assert the actual counter values, not just a 200 response — a reconcile
bug is silent, so the counters themselves are what must be checked.
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


def _per_muscle_day(day_number, muscle, exercise_ids, count=1):
    return {
        "day_number": day_number,
        "day_type": "per_muscle",
        "muscles": [{"muscle_group": muscle, "exercise_count": count}],
        "exercise_ids": exercise_ids,
    }


def _counter(user_id, exercise_id):
    """times_performed for (user, exercise) in exercises_in_routine, or None if no row."""
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
    """Directly bump a pool counter, to distinguish survivors from fresh rows."""
    session = SessionLocal()
    try:
        row = session.query(models.ExerciseInRoutine).filter(
            models.ExerciseInRoutine.user_id == user_id,
            models.ExerciseInRoutine.exercise_id == exercise_id,
        ).first()
        assert row is not None, "expected an exercises_in_routine row to bump"
        row.times_performed = value
        session.commit()
    finally:
        session.close()


# --- tests ------------------------------------------------------------------

def test_first_save_creates_pool_rows_at_zero(auth):
    """Saving a routine creates an exercises_in_routine row per pool exercise, at 0."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")

    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2])])

    assert _counter(uid, c1) == 0
    assert _counter(uid, c2) == 0


def test_survivors_keep_their_counter(auth):
    """Exercises that stay in the routine across a re-save keep times_performed."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")

    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2])])
    _set_counter(uid, c1, 5)
    _set_counter(uid, c2, 3)

    # Re-save the same routine: both exercises survive.
    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2])])

    assert _counter(uid, c1) == 5
    assert _counter(uid, c2) == 3


def test_removed_exercise_loses_its_row(auth):
    """An exercise dropped from the routine loses its exercises_in_routine row."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")

    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2])])
    _set_counter(uid, c1, 8)
    _set_counter(uid, c2, 6)

    # Re-save keeping only c1.
    _save(auth, [_per_muscle_day(1, "Chest", [c1])])

    assert _counter(uid, c1) == 8       # survivor unchanged
    assert _counter(uid, c2) is None    # removed -> row gone


def test_added_exercise_enters_at_muscle_minimum(auth):
    """
    A newly added exercise starts at the minimum counter among the surviving
    exercises of the same muscle (not 0, not the max).
    """
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    c2 = _make_exercise(auth, "Incline Press", "Chest")
    c3 = _make_exercise(auth, "Cable Fly", "Chest")

    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2])])
    _set_counter(uid, c1, 10)
    _set_counter(uid, c2, 4)

    # Add c3 to the Chest pool.
    _save(auth, [_per_muscle_day(1, "Chest", [c1, c2, c3])])

    assert _counter(uid, c1) == 10      # survivor
    assert _counter(uid, c2) == 4       # survivor
    assert _counter(uid, c3) == 4       # entered at min(10, 4) among surviving Chest


def test_added_exercise_new_muscle_starts_at_zero(auth):
    """An added exercise whose muscle has no surviving pool members starts at 0."""
    uid = auth["user_id"]
    c1 = _make_exercise(auth, "Bench Press", "Chest")
    b1 = _make_exercise(auth, "Row", "Back")

    _save(auth, [_per_muscle_day(1, "Chest", [c1])])
    _set_counter(uid, c1, 7)

    # Add a Back muscle + exercise; Back has no surviving pool members.
    _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "muscles": [
                {"muscle_group": "Chest", "exercise_count": 1},
                {"muscle_group": "Back", "exercise_count": 1},
            ],
            "exercise_ids": [c1, b1],
        }
    ])

    assert _counter(uid, c1) == 7       # survivor
    assert _counter(uid, b1) == 0       # new muscle, no survivors -> 0


def test_manual_day_exercises_join_the_pool(auth):
    """A manual day's exercises are reconciled into the pool the same way."""
    uid = auth["user_id"]
    a = _make_exercise(auth, "Deadlift", "Back")
    b = _make_exercise(auth, "Curl", "Biceps")

    _save(auth, [
        {"day_number": 1, "day_type": "manual", "exercise_ids": [a, b]},
    ])

    assert _counter(uid, a) == 0
    assert _counter(uid, b) == 0