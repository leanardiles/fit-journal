"""Workout generation tests — the auto-generate selection logic."""


def _setup_routine_and_exercise(auth, muscle="Chest", with_exercise=True):
    """
    Helper: give the auth user a 1-day routine (day 1 = `muscle`),
    and optionally add one exercise for that muscle group.
    """
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]

    client.post(f"/routine/{uid}", headers=headers, json={
        "days_per_week": 1,
        "routine_days": [{"day_number": 1, "muscle_groups": [muscle]}]
    })

    if with_exercise:
        client.post(f"/exercises?user_id={uid}", headers=headers, json={
            "exercise_name": f"{muscle} Exercise",
            "exercise_muscle_group": muscle
        })


def test_generate_selects_exercises(auth):
    """Generating for a day with a matching exercise selects it."""
    _setup_routine_and_exercise(auth, muscle="Chest", with_exercise=True)

    response = auth["client"].post(
        f"/next-workout/generate/{auth['user_id']}?day_number=1",
        headers=auth["headers"]
    )
    assert response.status_code == 200
    body = response.json()
    assert body["exercises_selected"] > 0
    assert body["day_number"] == 1


def test_generate_with_no_exercises_selects_none(auth):
    """
    Generating for a day whose muscle group has no exercises in the user's
    library selects nothing (exercises_selected == 0) — the gap case.
    """
    _setup_routine_and_exercise(auth, muscle="Chest", with_exercise=False)

    response = auth["client"].post(
        f"/next-workout/generate/{auth['user_id']}?day_number=1",
        headers=auth["headers"]
    )
    assert response.status_code == 200
    assert response.json()["exercises_selected"] == 0


def test_generate_empty_day_rejected(auth):
    """
    Generating for a day with no muscle groups assigned returns 400
    (the backend's 'No muscle groups assigned to this day' guard).
    """
    # Routine only defines day 1; ask to generate for day 2 (no muscles)
    _setup_routine_and_exercise(auth, muscle="Chest", with_exercise=True)

    response = auth["client"].post(
        f"/next-workout/generate/{auth['user_id']}?day_number=2",
        headers=auth["headers"]
    )
    assert response.status_code == 400