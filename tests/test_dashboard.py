"""
Dashboard note preferences.

Covers GET and PUT /v1/dashboard/notes/{user_id}: the never-customized case,
the round trip, the ownership guard, and the bounds the schema puts on ids
that arrive from a client.
"""

from database import SessionLocal
import models


def _make_second_user(client, email="dashb@example.com", password="testpass123"):
    """Register, verify and log in a second user; return their id and headers."""
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


# --- Reading ----------------------------------------------------------------

def test_never_customized_returns_empty_lists(auth):
    """
    A user who has never opened the modal gets empty lists, not a 404 and not
    a null. The client falls back to its own default order, so first run and
    a partial list take the same path.
    """
    resp = auth["client"].get(f"/v1/dashboard/notes/{auth['user_id']}", headers=auth["headers"])
    assert resp.status_code == 200, resp.text
    assert resp.json() == {"order": [], "hidden": []}


def test_saved_preference_round_trips(auth):
    """What was saved is what comes back, in the order it was saved in."""
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]
    body = {"order": ["setsPerMuscle", "quickStats", "currentRoutine"], "hidden": ["quickStats"]}

    put = client.put(f"/v1/dashboard/notes/{uid}", json=body, headers=headers)
    assert put.status_code == 200, put.text
    assert put.json() == body

    assert client.get(f"/v1/dashboard/notes/{uid}", headers=headers).json() == body


def test_put_replaces_rather_than_merges(auth):
    """
    A second save is the whole truth. Merging would make the result depend on
    which of two edits landed last, field by field.
    """
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]
    client.put(f"/v1/dashboard/notes/{uid}",
               json={"order": ["a", "b", "c"], "hidden": ["b"]}, headers=headers)

    client.put(f"/v1/dashboard/notes/{uid}",
               json={"order": ["c"], "hidden": []}, headers=headers)

    assert client.get(f"/v1/dashboard/notes/{uid}", headers=headers).json() == {
        "order": ["c"], "hidden": []
    }


# --- Ownership --------------------------------------------------------------

def test_cannot_read_another_users_dashboard(auth):
    """The dashboard is per user, so the ownership guard has to hold here too."""
    other_id, _ = _make_second_user(auth["client"])
    resp = auth["client"].get(f"/v1/dashboard/notes/{other_id}", headers=auth["headers"])
    assert resp.status_code in (403, 404), resp.text


def test_cannot_write_another_users_dashboard(auth):
    other_id, _ = _make_second_user(auth["client"], email="dashc@example.com")
    resp = auth["client"].put(f"/v1/dashboard/notes/{other_id}",
                              json={"order": ["quickStats"], "hidden": []},
                              headers=auth["headers"])
    assert resp.status_code in (403, 404), resp.text


def test_dashboard_notes_require_a_token(auth):
    resp = auth["client"].get(f"/v1/dashboard/notes/{auth['user_id']}")
    assert resp.status_code in (401, 403), resp.text


# --- Bounds -----------------------------------------------------------------

def test_too_many_ids_is_rejected(auth):
    """An unbounded list in a JSON column is an unbounded write."""
    resp = auth["client"].put(
        f"/v1/dashboard/notes/{auth['user_id']}",
        json={"order": [f"note{i}" for i in range(41)], "hidden": []},
        headers=auth["headers"],
    )
    assert resp.status_code == 422, resp.text


def test_a_malformed_id_is_rejected(auth):
    """Ids are stored verbatim, so the shape is checked rather than trusted."""
    resp = auth["client"].put(
        f"/v1/dashboard/notes/{auth['user_id']}",
        json={"order": ["<script>alert(1)</script>"], "hidden": []},
        headers=auth["headers"],
    )
    assert resp.status_code == 422, resp.text


def test_an_overlong_id_is_rejected(auth):
    resp = auth["client"].put(
        f"/v1/dashboard/notes/{auth['user_id']}",
        json={"order": ["x" * 41], "hidden": []},
        headers=auth["headers"],
    )
    assert resp.status_code == 422, resp.text


def test_duplicate_ids_collapse(auth):
    """Two of the same id would make its position ambiguous."""
    client, uid, headers = auth["client"], auth["user_id"], auth["headers"]
    resp = client.put(f"/v1/dashboard/notes/{uid}",
                      json={"order": ["quickStats", "quickStats", "setsPerMuscle"], "hidden": []},
                      headers=headers)
    assert resp.status_code == 200, resp.text
    assert resp.json()["order"] == ["quickStats", "setsPerMuscle"]
