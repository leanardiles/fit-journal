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