"""
Authorization sweep — a logged-in user (A) must not access another user's (B)
data on any user-scoped endpoint. Same vulnerability class as the profile IDOR
that was found and fixed; these tests confirm the fix holds across resources.
"""

import pytest


def _make_second_user(client, email="userb@example.com", password="testpass123"):
    """Register + log in a second user (B); return their user_id and headers."""
    client.post("/v1/register", json={"user_email": email, "user_password": password})
    login = client.post("/v1/login", json={"user_email": email, "user_password": password})
    data = login.json()
    return {
        "user_id": data["user_id"],
        "headers": {"Authorization": f"Bearer {data['access_token']}"},
    }


# --- A reading B's data with A's token: should be rejected -------------------

def test_cannot_read_another_users_profile(auth):
    """(Existing) A cannot read B's profile."""
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/profile/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_exercises(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/exercises?user_id={b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_routine(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/routine/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_workout_logs(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/workout/logs/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_selections(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/next-workout/selections/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_workout_state(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/workout/state/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


def test_cannot_read_another_users_sessions(auth):
    client = auth["client"]
    b = _make_second_user(client)
    r = client.get(f"/v1/workout/sessions/{b['user_id']}", headers=auth["headers"])
    assert r.status_code in (401, 403, 404)


# --- A mutating B's data: should be rejected ---------------------------------

def test_cannot_modify_another_users_routine(auth):
    """A cannot overwrite B's routine."""
    client = auth["client"]
    b = _make_second_user(client)
    r = client.post(
        f"/v1/routine/{b['user_id']}",
        headers=auth["headers"],
        json={"days_per_week": 1, "routine_days": [{"day_number": 1, "muscle_groups": ["Chest"]}]}
    )
    assert r.status_code in (401, 403, 404)


def test_cannot_toggle_selection_for_another_user(auth):
    """
    The toggle endpoint reads user_id from the BODY (inline ownership check,
    not the verify_user_access dependency). A passes B's user_id in the body.
    """
    client = auth["client"]
    b = _make_second_user(client)
    r = client.post(
        "/v1/next-workout/toggle",
        headers=auth["headers"],
        json={"user_id": b["user_id"], "exercise_id": 1, "is_selected": True}
    )
    assert r.status_code in (401, 403, 404)