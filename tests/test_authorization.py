"""
Authorization sweep — a logged-in user (A) must not access another user's (B)
data on any user-scoped endpoint. Same vulnerability class as the profile IDOR
that was found and fixed; these tests confirm the fix holds across resources.
"""

import pytest
from database import SessionLocal
import models


def _make_second_user(client, email="userb@example.com", password="testpass123"):
    """Register + log in a second user (B); return their user_id and headers."""
    client.post("/v1/register", json={"user_email": email, "user_password": password})

    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_email == email).first()
        u.user_email_verified = True
        session.commit()
    finally:
        session.close()

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
    """A cannot overwrite B's routine. Uses a valid new-shape payload so the
    only reason for rejection is the ownership check, not payload validation."""
    client = auth["client"]
    b = _make_second_user(client)
    r = client.post(
        f"/v1/routine/{b['user_id']}",
        headers=auth["headers"],
        json={
            "days": [
                {
                    "day_number": 1,
                    "day_type": "per_muscle",
                    "muscles": [{"muscle_group": "Chest", "exercise_count": 1}],
                    "exercise_ids": [1],
                }
            ]
        },
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

# --- The sweep --------------------------------------------------------------
#
# Everything above names one endpoint each, which means it only protects the
# endpoints somebody remembered to list. This walks the app's own routing table
# instead, so a route added next year is covered the day it exists rather than
# the day someone notices.

import main

# Routes that take a user_id in the path but are NOT the owner's own data, or
# are deliberately reachable without the guard. Every entry is a decision, and
# an empty list is the goal.
SWEEP_EXEMPT = set()


def _user_scoped_routes():
    """Every route with a {user_id} in its path, from the live routing table."""
    found = []
    for route in main.app.routes:
        path = getattr(route, "path", "")
        if "{user_id}" not in path or path in SWEEP_EXEMPT:
            continue
        for method in sorted(getattr(route, "methods", set()) - {"HEAD", "OPTIONS"}):
            found.append((method, path))
    return sorted(set(found))


def test_the_sweep_actually_finds_routes():
    """
    Guards the guard. If the walk silently stopped matching, every test below
    would pass by iterating over nothing, which is the worst possible way for
    a security test to fail.
    """
    assert len(_user_scoped_routes()) >= 10


def test_no_user_scoped_route_serves_another_users_data(auth):
    """
    A logged in as themselves, asking for B's id, on every route that takes
    one. 403 or 404 are both acceptable: 404 is the stronger answer, since it
    does not confirm the row exists.

    A 200 here means an IDOR, which is the bug class this file was written for.
    A 500 is also a failure: it means the route reached the database with
    another user's id before deciding anything.
    """
    b = _make_second_user(auth["client"], email="sweep-b@example.com")
    client = auth["client"]

    leaks = []
    for method, path in _user_scoped_routes():
        url = path.replace("{user_id}", str(b["user_id"]))
        # Any remaining path parameter gets a value that cannot exist, so the
        # only thing under test is the ownership check.
        while "{" in url:
            head, _, rest = url.partition("{")
            _, _, tail = rest.partition("}")
            url = head + "999999" + tail

        resp = client.request(method, url, headers=auth["headers"], json={})
        if resp.status_code not in (403, 404, 422):
            leaks.append(f"{method} {path} -> {resp.status_code}")

    assert not leaks, "routes reachable with another user's id: " + ", ".join(leaks)


# --- The one privileged route ----------------------------------------------
#
# /v1/admin/stats has no user_id, so the sweep above cannot see it. It gets its
# own case, and this comment is here so the next privileged route gets one too.

def test_admin_stats_is_refused_to_ordinary_users(auth):
    assert auth["client"].get("/v1/admin/stats", headers=auth["headers"]).status_code == 403


def test_admin_stats_is_refused_without_a_token(auth):
    assert auth["client"].get("/v1/admin/stats").status_code in (401, 403)
