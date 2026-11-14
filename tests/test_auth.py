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

# ---- sign-in rate limiting --------------------------------------------------
# The counters live in the database because Lambda keeps nothing between
# invocations. See src/ratelimit.py for the thresholds and the lockout
# trade-off.

import ratelimit
from datetime import datetime, timedelta


def _register_and_verify(client, email="rl@example.com", password="testpass123"):
    client.post("/v1/register", json={"user_email": email, "user_password": password})
    _verify(email)
    return email, password


def _fail_login(client, email, times=1, password="wrongpass123"):
    last = None
    for _ in range(times):
        last = client.post("/v1/login", json={"user_email": email, "user_password": password})
    return last


def test_login_locks_after_five_failures(client):
    """The fifth failure is still a 401; the sixth attempt is refused."""
    email, _ = _register_and_verify(client)
    fifth = _fail_login(client, email, times=ratelimit.MAX_PER_EMAIL)
    assert fifth.status_code == 401, fifth.text

    blocked = client.post("/v1/login", json={"user_email": email, "user_password": "wrongpass123"})
    assert blocked.status_code == 429, blocked.text
    assert blocked.headers.get("Retry-After")


def test_lockout_refuses_even_the_correct_password(client):
    """Once locked, the right password is refused too, or the limit is theatre."""
    email, password = _register_and_verify(client)
    _fail_login(client, email, times=ratelimit.MAX_PER_EMAIL)

    resp = client.post("/v1/login", json={"user_email": email, "user_password": password})
    assert resp.status_code == 429, resp.text


def test_unknown_email_locks_the_same_way(client):
    """
    An address that was never registered must behave identically. If only real
    accounts could be locked, the lockout would reveal which emails exist.
    """
    ghost = "nobody@example.com"
    _fail_login(client, ghost, times=ratelimit.MAX_PER_EMAIL)

    resp = client.post("/v1/login", json={"user_email": ghost, "user_password": "wrongpass123"})
    assert resp.status_code == 429, resp.text


def test_successful_login_clears_the_counter(client):
    """A correct password resets that account's failures."""
    email, password = _register_and_verify(client)
    _fail_login(client, email, times=ratelimit.MAX_PER_EMAIL - 1)

    ok = client.post("/v1/login", json={"user_email": email, "user_password": password})
    assert ok.status_code == 200, ok.text

    # Four more failures would have tripped the limit without the reset.
    again = _fail_login(client, email, times=ratelimit.MAX_PER_EMAIL - 1)
    assert again.status_code == 401, again.text


def test_failures_outside_the_window_do_not_count(client):
    """Rows older than the window are ignored rather than accumulating forever."""
    email, password = _register_and_verify(client)
    _fail_login(client, email, times=ratelimit.MAX_PER_EMAIL)

    session = SessionLocal()
    try:
        stale = datetime.utcnow() - timedelta(minutes=ratelimit.WINDOW_MINUTES + 1)
        for row in session.query(models.LoginAttempt).all():
            row.attempted_at = stale
        session.commit()
    finally:
        session.close()

    resp = client.post("/v1/login", json={"user_email": email, "user_password": password})
    assert resp.status_code == 200, resp.text


def test_unverified_account_does_not_fill_the_counter(client):
    """
    A correct password on an unverified account is a state problem, not a
    guess. It must not count toward the lockout.
    """
    email, password = "unverified@example.com", "testpass123"
    client.post("/v1/register", json={"user_email": email, "user_password": password})

    for _ in range(ratelimit.MAX_PER_EMAIL + 1):
        resp = client.post("/v1/login", json={"user_email": email, "user_password": password})
        assert resp.status_code == 403, resp.text   # never 429


def test_address_limit_catches_spraying_across_accounts(client):
    """
    The attack the per-account limit cannot see: one password tried against
    many accounts, so no single account's counter ever reaches five. The
    address limit is what stops it.
    """
    for i in range(ratelimit.MAX_PER_IP):
        resp = client.post("/v1/login", json={
            "user_email": f"victim{i}@example.com",
            "user_password": "Winter2026!",
        })
        assert resp.status_code == 401, resp.text   # each account sees one failure

    blocked = client.post("/v1/login", json={
        "user_email": "victim-next@example.com",
        "user_password": "Winter2026!",
    })
    assert blocked.status_code == 429, blocked.text
