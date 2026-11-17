"""
Weekly set goals.

Covers GET and PUT /v1/goals/{user_id}: the unset case, the round trip, the
ownership guard, and the bounds. Goals arrive from a client and are stored
verbatim in a JSON column, so the validator is the only thing between a typed
number and the database.
"""

from database import SessionLocal
import models


def _make_second_user(client, email="goalsb@example.com", password="testpass123"):
    client.post("/v1/register", json={"user_email": email, "user_password": password})
    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_email == email).first()
        u.user_email_verified = True
        session.commit()
    finally:
        session.close()
    data = client.post("/v1/login", json={"user_email": email, "user_password": password}).json()
    return data["user_id"], {"Authorization": f"Bearer {data['access_token']}"}


def test_no_goals_returns_an_empty_map(auth):
    """Empty rather than 404, so the note has one code path for both cases."""
    resp = auth["client"].get(f"/v1/goals/{auth['user_id']}", headers=auth["headers"])
    assert resp.status_code == 200, resp.text
    assert resp.json() == {"goals": {}}


def test_goals_round_trip(auth):
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]
    body = {"goals": {"Chest": 12, "Back": 15, "Biceps": 9}}

    put = client.put(f"/v1/goals/{uid}", json=body, headers=headers)
    assert put.status_code == 200, put.text
    assert put.json() == body
    assert client.get(f"/v1/goals/{uid}", headers=headers).json() == body


def test_put_replaces_rather_than_merges(auth):
    """Removing a goal is sending the map without it, not a separate delete."""
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]
    client.put(f"/v1/goals/{uid}", json={"goals": {"Chest": 12, "Back": 15}}, headers=headers)
    client.put(f"/v1/goals/{uid}", json={"goals": {"Chest": 10}}, headers=headers)

    assert client.get(f"/v1/goals/{uid}", headers=headers).json() == {"goals": {"Chest": 10}}


# --- Ownership --------------------------------------------------------------

def test_cannot_read_another_users_goals(auth):
    other_id, _ = _make_second_user(auth["client"])
    resp = auth["client"].get(f"/v1/goals/{other_id}", headers=auth["headers"])
    assert resp.status_code in (403, 404), resp.text


def test_cannot_write_another_users_goals(auth):
    other_id, _ = _make_second_user(auth["client"], email="goalsc@example.com")
    resp = auth["client"].put(f"/v1/goals/{other_id}",
                              json={"goals": {"Chest": 12}}, headers=auth["headers"])
    assert resp.status_code in (403, 404), resp.text


def test_goals_require_a_token(auth):
    assert auth["client"].get(f"/v1/goals/{auth['user_id']}").status_code in (401, 403)


# --- Bounds -----------------------------------------------------------------

def test_unknown_muscle_is_rejected(auth):
    """Keys are stored verbatim, so they are checked against the enum."""
    resp = auth["client"].put(f"/v1/goals/{auth['user_id']}",
                              json={"goals": {"Lats": 12}}, headers=auth["headers"])
    assert resp.status_code == 422, resp.text


def test_muscle_names_are_case_sensitive(auth):
    """The enum stores Chest, so chest is a different string and not a goal."""
    resp = auth["client"].put(f"/v1/goals/{auth['user_id']}",
                              json={"goals": {"chest": 12}}, headers=auth["headers"])
    assert resp.status_code == 422, resp.text


def test_zero_is_rejected(auth):
    """
    A muscle with no goal is absent from the map. Accepting 0 as well would
    give two spellings of the same state for the UI to disagree about.
    """
    resp = auth["client"].put(f"/v1/goals/{auth['user_id']}",
                              json={"goals": {"Chest": 0}}, headers=auth["headers"])
    assert resp.status_code == 422, resp.text


def test_negative_and_absurd_targets_are_rejected(auth):
    for target in (-3, 101, 100000):
        resp = auth["client"].put(f"/v1/goals/{auth['user_id']}",
                                  json={"goals": {"Chest": target}}, headers=auth["headers"])
        assert resp.status_code == 422, f"{target}: {resp.text}"


def test_a_target_at_the_limit_is_accepted(auth):
    """100 is allowed; the test above fixes where the wall is, this fixes that
    the wall is not one step early."""
    resp = auth["client"].put(f"/v1/goals/{auth['user_id']}",
                              json={"goals": {"Chest": 100}}, headers=auth["headers"])
    assert resp.status_code == 200, resp.text
