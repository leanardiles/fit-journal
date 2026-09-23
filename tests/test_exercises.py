"""Exercise CRUD tests — create and delete on protected endpoints."""


def test_create_exercise(auth):
    """Creating an exercise succeeds and returns it."""
    response = auth["client"].post(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"],
        json={
            "exercise_name": "Bench Press",
            "exercise_muscle_group": "Chest"
        }
    )
    assert response.status_code in (200, 201)
    body = response.json()
    assert body["exercise_name"] == "Bench Press"


def test_delete_exercise(auth):
    """An exercise can be created and then deleted."""
    # Arrange: create one
    created = auth["client"].post(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"],
        json={"exercise_name": "Squat", "exercise_muscle_group": "Legs"}
    )
    exercise_id = created.json()["exercise_id"]

    # Act: delete it
    response = auth["client"].delete(
        f"/v1/exercises/{exercise_id}?user_id={auth['user_id']}",
        headers=auth["headers"]
    )
    # Assert
    assert response.status_code in (200, 204)


def test_get_exercises(auth):
    """After creating exercises, they appear in the user's list."""
    auth["client"].post(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"],
        json={"exercise_name": "Deadlift", "exercise_muscle_group": "Back"}
    )
    response = auth["client"].get(
        f"/v1/exercises?user_id={auth['user_id']}",
        headers=auth["headers"]
    )
    assert response.status_code == 200
    names = [ex["exercise_name"] for ex in response.json()]
    assert "Deadlift" in names

def test_update_exercise(auth):
    """PUT updates the provided fields and leaves omitted ones unchanged (exclude_none)."""
    uid = auth["user_id"]
    created = auth["client"].post(
        f"/v1/exercises?user_id={uid}",
        headers=auth["headers"],
        json={"exercise_name": "Overhead Press", "exercise_muscle_group": "Chest"},
    )
    ex_id = created.json()["exercise_id"]

    # Update name + muscle group.
    resp = auth["client"].put(
        f"/v1/exercises/{ex_id}?user_id={uid}",
        headers=auth["headers"],
        json={"exercise_name": "Seated DB Press", "exercise_muscle_group": "Back"},
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["exercise_name"] == "Seated DB Press"
    assert body["exercise_muscle_group"] == "Back"

    # Partial update: change only the weight; name/muscle must persist.
    resp = auth["client"].put(
        f"/v1/exercises/{ex_id}?user_id={uid}",
        headers=auth["headers"],
        json={"exercise_user_current_weight": 40.0},
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["exercise_user_current_weight"] == 40.0
    assert body["exercise_name"] == "Seated DB Press"   # unchanged
    assert body["exercise_muscle_group"] == "Back"      # unchanged


def test_exercise_weight_stored_as_canonical_kg(auth):
    """
    Weight is stored canonical (kg) with 2-decimal precision. The frontend
    converts lb->kg to two decimals (e.g. 220.5 lb -> 100.02 kg); the backend
    must persist exactly that and round-trip half-kg values without drift.
    """
    uid = auth["user_id"]

    # Create with a 2-decimal kg value (what the lb->kg conversion produces).
    created = auth["client"].post(
        f"/v1/exercises?user_id={uid}",
        headers=auth["headers"],
        json={
            "exercise_name": "Bench Press",
            "exercise_muscle_group": "Chest",
            "exercise_user_current_weight": 100.02,
        },
    )
    assert created.status_code in (200, 201), created.text
    ex_id = created.json()["exercise_id"]
    assert created.json()["exercise_user_current_weight"] == 100.02

    # It survives a re-read from the list endpoint.
    got = auth["client"].get(
        f"/v1/exercises?user_id={uid}", headers=auth["headers"]
    ).json()
    mine = next(e for e in got if e["exercise_id"] == ex_id)
    assert mine["exercise_user_current_weight"] == 100.02

    # A half-kilogram value round-trips exactly (no snap to a whole number).
    resp = auth["client"].put(
        f"/v1/exercises/{ex_id}?user_id={uid}",
        headers=auth["headers"],
        json={"exercise_user_current_weight": 17.5},
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["exercise_user_current_weight"] == 17.5
