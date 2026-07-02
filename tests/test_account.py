"""
Account deletion flow.

Covers the DELETE /v1/account/{user_id} endpoint: successful self-deletion with
password re-authentication, rejection on a wrong password, the auth and
ownership guards, and confirmation that the user's data is cascade-deleted.
"""

import pytest

from database import SessionLocal
import models


def _make_second_user(client, email="userb@example.com", password="testpass123"):
    """Register + log in a second user (B); return their user_id, headers, password."""
    client.post("/v1/register", json={"user_email": email, "user_password": password})
    login = client.post("/v1/login", json={"user_email": email, "user_password": password})
    data = login.json()
    return {
        "user_id": data["user_id"],
        "headers": {"Authorization": f"Bearer {data['access_token']}"},
        "password": password,
    }


# --- Happy path -------------------------------------------------------------

def test_delete_account_success(auth):
    """Correct password deletes the account and returns 200."""
    client = auth["client"]
    r = client.request(
        "DELETE",
        f"/v1/account/{auth['user_id']}",
        headers=auth["headers"],
        json={"user_password": "testpass123"},
    )
    assert r.status_code == 200


def test_deleted_user_cannot_log_in(auth):
    """After deletion the credentials no longer work."""
    client = auth["client"]
    client.request(
        "DELETE",
        f"/v1/account/{auth['user_id']}",
        headers=auth["headers"],
        json={"user_password": "testpass123"},
    )
    login = client.post(
        "/v1/login",
        json={"user_email": "authuser@example.com", "user_password": "testpass123"},
    )
    assert login.status_code == 401


def test_deleted_user_token_is_rejected(auth):
    """The old JWT is useless once the user row is gone (get_current_user 401s)."""
    client = auth["client"]
    client.request(
        "DELETE",
        f"/v1/account/{auth['user_id']}",
        headers=auth["headers"],
        json={"user_password": "testpass123"},
    )
    r = client.get(f"/v1/profile/{auth['user_id']}", headers=auth["headers"])
    assert r.status_code == 401


def test_delete_account_cascades_user_data(auth):
    """Deleting the account cascade-removes the user's rows (checked via exercises)."""
    client = auth["client"]
    user_id = auth["user_id"]

    # Seed a row to watch disappear. We create it through the API rather than
    # relying on the default-exercise copy at registration: the test database's
    # default_exercises table is empty, so a fresh user starts with none.
    created = client.post(
        f"/v1/exercises?user_id={user_id}",
        headers=auth["headers"],
        json={"exercise_name": "Bench Press", "exercise_muscle_group": "Chest"},
    )
    assert created.status_code in (200, 201)

    session = SessionLocal()
    try:
        before = session.query(models.Exercise).filter(
            models.Exercise.user_id == user_id
        ).count()
    finally:
        session.close()
    assert before > 0

    client.request(
        "DELETE",
        f"/v1/account/{user_id}",
        headers=auth["headers"],
        json={"user_password": "testpass123"},
    )

    session = SessionLocal()
    try:
        after = session.query(models.Exercise).filter(
            models.Exercise.user_id == user_id
        ).count()
        user_row = session.query(models.User).filter(
            models.User.user_id == user_id
        ).first()
    finally:
        session.close()
    assert after == 0
    assert user_row is None


# --- Rejections -------------------------------------------------------------

def test_delete_account_wrong_password(auth):
    """A wrong password is rejected and the account survives."""
    client = auth["client"]
    r = client.request(
        "DELETE",
        f"/v1/account/{auth['user_id']}",
        headers=auth["headers"],
        json={"user_password": "wrongpass"},
    )
    assert r.status_code == 401

    # Account is still usable.
    profile = client.get(f"/v1/profile/{auth['user_id']}", headers=auth["headers"])
    assert profile.status_code == 200


def test_delete_account_requires_auth(auth):
    """No token -> not allowed."""
    client = auth["client"]
    r = client.request(
        "DELETE",
        f"/v1/account/{auth['user_id']}",
        json={"user_password": "testpass123"},
    )
    assert r.status_code in (401, 403)


def test_cannot_delete_another_users_account(auth):
    """
    A cannot delete B's account, even with B's correct password: the
    verify_user_access ownership check rejects the mismatched user_id before
    the password is ever checked.
    """
    client = auth["client"]
    b = _make_second_user(client)

    r = client.request(
        "DELETE",
        f"/v1/account/{b['user_id']}",
        headers=auth["headers"],        # A's token
        json={"user_password": b["password"]},  # B's real password
    )
    assert r.status_code in (401, 403)

    # B still exists.
    session = SessionLocal()
    try:
        b_row = session.query(models.User).filter(
            models.User.user_id == b["user_id"]
        ).first()
    finally:
        session.close()
    assert b_row is not None