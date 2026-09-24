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

# --- manual (off-routine) workout logging -----------------------------------

def _log_manual(auth, workout_date, exercises):
    return auth["client"].post(
        f"/v1/workout/log-manual/{auth['user_id']}",
        headers=auth["headers"],
        json={"workout_date": workout_date, "exercises": exercises},
    )


def test_manual_log_saves_with_null_routine_day(auth):
    """A manual log saves logs with no routine day and shows up in history."""
    ex = _make_exercise(auth, "Bench Press", "Chest")
    resp = _log_manual(auth, "2020-06-15", [
        {"exercise_id": ex, "sets_completed": 3, "reps_completed": 10, "weight_used": 50.0},
    ])
    assert resp.status_code == 200, resp.text

    logs = auth["client"].get(
        f"/v1/workout/logs/{auth['user_id']}", headers=auth["headers"]
    ).json()
    mine = [l for l in logs if l["exercise_id"] == ex]
    assert len(mine) == 1
    assert mine[0]["routine_day_number"] is None    # off-routine
    assert mine[0]["sets_completed"] == 3


def test_manual_log_rejects_future_date(auth):
    """A future workout date is rejected."""
    ex = _make_exercise(auth, "Bench Press", "Chest")
    resp = _log_manual(auth, "2099-01-01", [
        {"exercise_id": ex, "sets_completed": 3, "reps_completed": 8, "weight_used": None},
    ])
    assert resp.status_code == 400, resp.text


def test_manual_log_does_not_advance_cursor(auth):
    """Logging a manual workout must NOT move the routine's current_day_number."""
    uid = auth["user_id"]
    ex = _make_exercise(auth, "Bench Press", "Chest")

    before = auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"]).json()
    start_day = before["current_day_number"]

    _log_manual(auth, "2020-06-15", [
        {"exercise_id": ex, "sets_completed": 2, "reps_completed": 10, "weight_used": None},
    ])

    after = auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"]).json()
    assert after["current_day_number"] == start_day   # unchanged


def test_manual_log_rejects_unowned_exercise(auth):
    """A manual log referencing another user's exercise is rejected."""
    client = auth["client"]
    client.post("/v1/register", json={"user_email": "mlother@example.com", "user_password": "testpass123"})
    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_email == "mlother@example.com").first()
        u.user_email_verified = True
        session.commit()
    finally:
        session.close()
    other = client.post("/v1/login", json={"user_email": "mlother@example.com", "user_password": "testpass123"}).json()
    other_headers = {"Authorization": f"Bearer {other['access_token']}"}
    other_ex = client.post(
        f"/v1/exercises?user_id={other['user_id']}",
        headers=other_headers,
        json={"exercise_name": "Foreign", "exercise_muscle_group": "Chest"},
    ).json()["exercise_id"]

    resp = _log_manual(auth, "2020-06-15", [
        {"exercise_id": other_ex, "sets_completed": 3, "reps_completed": 10, "weight_used": None},
    ])
    assert resp.status_code == 400, resp.text


# --- set current day (skip / reorder which day to train) --------------------

def _pm_day(n, muscle, ids, count=1):
    return {"day_number": n, "day_type": "per_muscle",
            "muscles": [{"muscle_group": muscle, "exercise_count": count}], "exercise_ids": ids}


def test_set_current_day_updates_state(auth):
    """Setting the current day updates workout_state.current_day_number."""
    uid = auth["user_id"]
    c = _make_exercise(auth, "Bench Press", "Chest")
    b = _make_exercise(auth, "Row", "Back")
    l = _make_exercise(auth, "Squat", "Legs")
    _save(auth, [_pm_day(1, "Chest", [c]), _pm_day(2, "Back", [b]), _pm_day(3, "Legs", [l])])

    resp = auth["client"].post(
        f"/v1/workout/state/{uid}/current-day?day_number=2", headers=auth["headers"]
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["current_day_number"] == 2

    state = auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"]).json()
    assert state["current_day_number"] == 2


def test_set_current_day_rejects_out_of_range(auth):
    """A day outside 1..N is rejected."""
    uid = auth["user_id"]
    c = _make_exercise(auth, "Bench Press", "Chest")
    b = _make_exercise(auth, "Row", "Back")
    _save(auth, [_pm_day(1, "Chest", [c]), _pm_day(2, "Back", [b])])

    too_high = auth["client"].post(
        f"/v1/workout/state/{uid}/current-day?day_number=5", headers=auth["headers"]
    )
    assert too_high.status_code == 400, too_high.text

    too_low = auth["client"].post(
        f"/v1/workout/state/{uid}/current-day?day_number=0", headers=auth["headers"]
    )
    assert too_low.status_code == 400, too_low.text


def test_skip_day_then_complete_advances_from_chosen(auth):
    """
    Interpretation A: skip to day 2, complete it, and the cursor advances to 3
    (the day after the one actually logged) rather than from the old day 1.
    """
    uid = auth["user_id"]
    c = _make_exercise(auth, "Bench Press", "Chest")
    b = _make_exercise(auth, "Row", "Back")
    l = _make_exercise(auth, "Squat", "Legs")
    _save(auth, [_pm_day(1, "Chest", [c]), _pm_day(2, "Back", [b]), _pm_day(3, "Legs", [l])])

    auth["client"].post(f"/v1/workout/state/{uid}/current-day?day_number=2", headers=auth["headers"])

    resp = auth["client"].post(
        f"/v1/workout/complete/{uid}",
        headers=auth["headers"],
        json={"day_number": 2, "exercises": [
            {"exercise_id": b, "sets_completed": 3, "reps_completed": 10, "weight_used": None},
        ]},
    )
    assert resp.status_code == 200, resp.text

    state = auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"]).json()
    assert state["current_day_number"] == 3   # advanced from the day logged (2 -> 3)

# --- next-workout manual selection (toggle + clear) -------------------------

def test_next_workout_toggle_and_clear(auth):
    """Toggling selects/deselects an exercise for the next workout; clear removes all."""
    uid = auth["user_id"]
    ex = _make_exercise(auth, "Bench Press", "Chest")

    def toggle(selected):
        return auth["client"].post(
            "/v1/next-workout/toggle",
            headers=auth["headers"],
            json={"user_id": uid, "exercise_id": ex, "is_selected": selected},
        )

    # On -> appears as selected
    assert toggle(True).status_code == 200
    assert ex in _selected_ids(auth)

    # Off -> no longer selected
    assert toggle(False).status_code == 200
    assert ex not in _selected_ids(auth)

    # On again, then clear-all wipes the selection rows
    toggle(True)
    r = auth["client"].delete(f"/v1/next-workout/clear/{uid}", headers=auth["headers"])
    assert r.status_code == 200, r.text
    remaining = auth["client"].get(
        f"/v1/next-workout/selections/{uid}", headers=auth["headers"]
    ).json()
    assert remaining == []


# --- calendar/dashboard reads (sessions + logs-by-sessions) -----------------

def test_sessions_and_logs_by_sessions(auth):
    """Completing a workout creates a session that the calendar read endpoints return."""
    uid = auth["user_id"]
    ex = _make_exercise(auth, "Bench Press", "Chest")
    _save(auth, [_pm_day(1, "Chest", [ex])])
    auth["client"].get(f"/v1/workout/state/{uid}", headers=auth["headers"])  # ensure state
    _generate(auth, day_number=1)

    completed = auth["client"].post(
        f"/v1/workout/complete/{uid}",
        headers=auth["headers"],
        json={"day_number": 1, "exercises": [
            {"exercise_id": ex, "sets_completed": 3, "reps_completed": 10, "weight_used": 60.0},
        ]},
    )
    assert completed.status_code == 200, completed.text
    session_id = completed.json()["session_id"]

    # sessions endpoint (calendar columns) includes the completed session
    sessions = auth["client"].get(
        f"/v1/workout/sessions/{uid}", headers=auth["headers"]
    ).json()
    assert any(s["session_id"] == session_id for s in sessions)

    # logs-by-sessions (calendar cell fill) returns the exercise logged in it
    logs = auth["client"].post(
        f"/v1/workout/logs-by-sessions/{uid}",
        headers=auth["headers"],
        json={"session_ids": [session_id]},
    ).json()
    assert any(l["exercise_id"] == ex for l in logs)
