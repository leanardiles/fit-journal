"""Routine tests — saving routines, including the no-rest-day rule."""


def test_save_valid_routine(auth):
    """A routine where every day has a muscle group saves successfully."""
    response = auth["client"].post(
        f"/routine/{auth['user_id']}",
        headers=auth["headers"],
        json={
            "days_per_week": 2,
            "routine_days": [
                {"day_number": 1, "muscle_groups": ["Chest", "Triceps"]},
                {"day_number": 2, "muscle_groups": ["Back", "Biceps"]}
            ]
        }
    )
    assert response.status_code in (200, 201)


def test_get_routine(auth):
    """After saving a routine, it can be fetched back."""
    auth["client"].post(
        f"/routine/{auth['user_id']}",
        headers=auth["headers"],
        json={
            "days_per_week": 1,
            "routine_days": [
                {"day_number": 1, "muscle_groups": ["Legs"]}
            ]
        }
    )
    response = auth["client"].get(
        f"/routine/{auth['user_id']}",
        headers=auth["headers"]
    )
    assert response.status_code == 200
    assert response.json()["days_per_week"] == 1


def test_save_routine_with_empty_day_rejected(auth):
    """The no-rest-day rule: a routine with an empty training day is rejected."""
    response = auth["client"].post(
        f"/routine/{auth['user_id']}",
        headers=auth["headers"],
        json={
            "days_per_week": 2,
            "routine_days": [
                {"day_number": 1, "muscle_groups": ["Chest"]},
                {"day_number": 2, "muscle_groups": []}   # empty day — should be rejected
            ]
        }
    )
    assert response.status_code == 400