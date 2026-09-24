"""Authentication tests — register and login flows."""

import models
from database import SessionLocal


def _verify(email):
    """Mark a registered user's email as verified (login is gated on it)."""
    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_email == email).first()
        u.user_email_verified = True
        session.commit()
    finally:
        session.close()


def test_register_new_user(client):
    """Registering a brand-new user succeeds."""
    response = client.post("/v1/register", json={
        "user_email": "newuser@example.com",
        "user_password": "testpass123"
    })
    assert response.status_code == 201


def test_login_correct_credentials(client):
    """A verified user can log in and receives an access token."""
    client.post("/v1/register", json={
        "user_email": "loginuser@example.com",
        "user_password": "testpass123"
    })
    _verify("loginuser@example.com")

    response = client.post("/v1/login", json={
        "user_email": "loginuser@example.com",
        "user_password": "testpass123"
    })
    assert response.status_code == 200
    assert "access_token" in response.json()


def test_login_blocked_until_verified(client):
    """A freshly registered, unverified user cannot log in yet."""
    client.post("/v1/register", json={
        "user_email": "pending@example.com",
        "user_password": "testpass123"
    })
    response = client.post("/v1/login", json={
        "user_email": "pending@example.com",
        "user_password": "testpass123"
    })
    assert response.status_code == 403
    detail = response.json()["detail"]
    assert isinstance(detail, dict) and detail.get("code") == "email_not_verified"


def test_register_duplicate_email(client):
    """Registering with an already-used email is rejected, not a crash."""
    client.post("/v1/register", json={
        "user_email": "dupe@example.com",
        "user_password": "testpass123"
    })
    response = client.post("/v1/register", json={
        "user_email": "dupe@example.com",
        "user_password": "testpass123"
    })
    assert response.status_code == 400


def test_login_wrong_password(client):
    """Login with the wrong password is rejected. (Guards the verify_password path.)"""
    client.post("/v1/register", json={
        "user_email": "wrongpw@example.com",
        "user_password": "correctpass123"
    })
    response = client.post("/v1/login", json={
        "user_email": "wrongpw@example.com",
        "user_password": "WRONGpassword"
    })
    assert response.status_code == 401


def test_login_nonexistent_email(client):
    """Login with an email that was never registered is rejected."""
    response = client.post("/v1/login", json={
        "user_email": "ghost@example.com",
        "user_password": "whatever123"
    })
    assert response.status_code == 401


def test_protected_endpoint_requires_token(client):
    """Hitting a protected endpoint with no token is rejected."""
    response = client.get("/v1/exercises?user_id=1")
    assert response.status_code in (401, 403)