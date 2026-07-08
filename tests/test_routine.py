"""
Routine save/read and validation tests for the training-day model.

A routine is a list of training days. Each day is either `per_muscle`
(muscles + counts, plus a selected exercise pool) or `manual` (an explicit
exercise list). The save endpoint validates the whole payload before touching
any existing data, so a rejected save must never wipe the current routine.
"""


# --- helpers ----------------------------------------------------------------

def _make_exercise(auth, name, muscle):
    """Create an exercise for the auth user and return its id."""
    r = auth["client"].post(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"],
        json={"exercise_name": name, "exercise_muscle_group": muscle},
    )
    assert r.status_code in (200, 201), r.text
    return r.json()["exercise_id"]


def _make_second_user(client):
    """Register + log in a second user; return {user_id, headers}."""
    email, password = "second@example.com", "testpass123"
    client.post("/v1/register", json={"user_email": email, "user_password": password})
    login = client.post("/v1/login", json={"user_email": email, "user_password": password}).json()
    return {
        "user_id": login["user_id"],
        "headers": {"Authorization": f"Bearer {login['access_token']}"},
    }


def _save(auth, days):
    """POST a routine payload of the new {days: [...]} shape."""
    return auth["client"].post(
        f"/v1/routine/{auth['user_id']}",
        headers=auth["headers"],
        json={"days": days},
    )


# --- saving & reading -------------------------------------------------------

def test_save_and_read_per_muscle_routine(auth):
    """
    A per_muscle routine (muscles + counts + a selected pool) saves, and
    get_routine returns the new shape: day_type, name, muscles, exercises.
    """
    chest1 = _make_exercise(auth, "Bench Press", "Chest")
    chest2 = _make_exercise(auth, "Incline Press", "Chest")
    back1 = _make_exercise(auth, "Row", "Back")

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "name": "Push",
            "muscles": [{"muscle_group": "Chest", "exercise_count": 2}],
            "exercise_ids": [chest1, chest2],
        },
        {
            "day_number": 2,
            "day_type": "per_muscle",
            "name": "Pull",
            "muscles": [{"muscle_group": "Back", "exercise_count": 1}],
            "exercise_ids": [back1],
        },
    ])
    assert resp.status_code in (200, 201), resp.text

    body = auth["client"].get(
        f"/v1/routine/{auth['user_id']}", headers=auth["headers"]
    ).json()

    assert body["days_per_week"] == 2
    assert len(body["days"]) == 2

    day1 = body["days"][0]
    assert day1["day_number"] == 1
    assert day1["day_type"] == "per_muscle"
    assert day1["name"] == "Push"
    assert day1["muscles"] == [{"muscle_group": "Chest", "exercise_count": 2}]
    assert {e["exercise_id"] for e in day1["exercises"]} == {chest1, chest2}
    # exercises carry name + muscle from the library
    assert {e["exercise_name"] for e in day1["exercises"]} == {"Bench Press", "Incline Press"}
    assert all(e["muscle_group"] == "Chest" for e in day1["exercises"])

    day2 = body["days"][1]
    assert day2["day_type"] == "per_muscle"
    assert day2["muscles"] == [{"muscle_group": "Back", "exercise_count": 1}]
    assert {e["exercise_id"] for e in day2["exercises"]} == {back1}


def test_save_manual_routine(auth):
    """A manual day stores an explicit exercise list and reads it back."""
    a = _make_exercise(auth, "Deadlift", "Back")
    b = _make_exercise(auth, "Curl", "Biceps")

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "manual",
            "name": "Full Body",
            "exercise_ids": [a, b],
        },
    ])
    assert resp.status_code in (200, 201), resp.text

    body = auth["client"].get(
        f"/v1/routine/{auth['user_id']}", headers=auth["headers"]
    ).json()

    assert body["days_per_week"] == 1
    day = body["days"][0]
    assert day["day_type"] == "manual"
    # a manual day carries no muscles, only the exact list
    assert day["muscles"] == []
    assert {e["exercise_id"] for e in day["exercises"]} == {a, b}


def test_save_mixed_routine(auth):
    """One payload can mix per_muscle and manual days."""
    chest = _make_exercise(auth, "Bench Press", "Chest")
    back = _make_exercise(auth, "Row", "Back")
    manual_ex = _make_exercise(auth, "Plank", "Abs")

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
            "exercise_ids": [chest],
        },
        {
            "day_number": 2,
            "day_type": "manual",
            "exercise_ids": [manual_ex],
        },
        {
            "day_number": 3,
            "day_type": "per_muscle",
            "muscles": [{"muscle_group": "Back", "exercise_count": 1}],
            "exercise_ids": [back],
        },
    ])
    assert resp.status_code in (200, 201), resp.text

    days = auth["client"].get(
        f"/v1/routine/{auth['user_id']}", headers=auth["headers"]
    ).json()["days"]

    types = {d["day_number"]: d["day_type"] for d in days}
    assert types == {1: "per_muscle", 2: "manual", 3: "per_muscle"}


# --- validation rejections (each a 400) -------------------------------------

def test_reject_non_contiguous_day_numbers(auth):
    """Day numbers must be exactly 1..N; a gap is rejected."""
    chest = _make_exercise(auth, "Bench Press", "Chest")
    back = _make_exercise(auth, "Row", "Back")

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
            "exercise_ids": [chest],
        },
        {
            "day_number": 3,  # gap: no day 2
            "day_type": "per_muscle",
            "muscles": [{"muscle_group": "Back", "exercise_count": 1}],
            "exercise_ids": [back],
        },
    ])
    assert resp.status_code == 400, resp.text


def test_reject_per_muscle_muscle_with_empty_pool(auth):
    """
    Pool-coverage rule: a per_muscle day lists a muscle with zero selected
    exercises in the pool → rejected.
    """
    chest = _make_exercise(auth, "Bench Press", "Chest")

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "muscles": [
                {"muscle_group": "Chest", "exercise_count": 1},
                {"muscle_group": "Back", "exercise_count": 1},  # no Back exercise selected
            ],
            "exercise_ids": [chest],
        },
    ])
    assert resp.status_code == 400, resp.text


def test_reject_manual_day_with_no_exercises(auth):
    """A manual day with an empty exercise list is rejected."""
    resp = _save(auth, [
        {"day_number": 1, "day_type": "manual", "exercise_ids": []},
    ])
    assert resp.status_code == 400, resp.text


def test_reject_unowned_exercise(auth):
    """Referencing an exercise the user doesn't own is rejected."""
    other = _make_second_user(auth["client"])
    # An exercise owned by the second user
    other_ex = auth["client"].post(
        f"/v1/exercises?user_id={other['user_id']}",
        headers=other["headers"],
        json={"exercise_name": "Foreign Bench", "exercise_muscle_group": "Chest"},
    ).json()["exercise_id"]

    resp = _save(auth, [
        {
            "day_number": 1,
            "day_type": "manual",
            "exercise_ids": [other_ex],
        },
    ])
    assert resp.status_code == 400, resp.text


# --- atomicity --------------------------------------------------------------

def test_rejected_save_preserves_existing_routine(auth):
    """A rejected save must not wipe the user's existing routine."""
    chest = _make_exercise(auth, "Bench Press", "Chest")

    # Save a valid routine first
    assert _save(auth, [
        {
            "day_number": 1,
            "day_type": "per_muscle",
            "name": "Push",
            "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
            "exercise_ids": [chest],
        },
    ]).status_code in (200, 201)

    # Now attempt an invalid save (manual day with no exercises)
    bad = _save(auth, [
        {"day_number": 1, "day_type": "manual", "exercise_ids": []},
    ])
    assert bad.status_code == 400, bad.text

    # The original routine is still intact
    body = auth["client"].get(
        f"/v1/routine/{auth['user_id']}", headers=auth["headers"]
    ).json()
    assert body["days_per_week"] == 1
    assert body["days"][0]["day_type"] == "per_muscle"
    assert body["days"][0]["name"] == "Push"
    assert {e["exercise_id"] for e in body["days"][0]["exercises"]} == {chest}


# --- toggle endpoint accepts a string user_id -------------------------------

def test_toggle_accepts_string_user_id(auth):
    """
    The next-workout/toggle endpoint reads user_id from the body. The web
    client sends it as a string (from localStorage); the endpoint must coerce
    it and not 403 the ownership check.
    """
    ex = _make_exercise(auth, "Bench Press", "Chest")

    resp = auth["client"].post(
        "/v1/next-workout/toggle",
        headers=auth["headers"],
        json={
            "user_id": str(auth["user_id"]),  # string, as the web client sends
            "exercise_id": ex,
            "is_selected": True,
        },
    )
    assert resp.status_code == 200, resp.text
