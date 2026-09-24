"""Email verification and password reset flows."""

import emailer


def test_verify_email_success(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(emailer, "send_verification_code",
                        lambda to, code: captured.__setitem__("code", code))

    email = "verify1@example.com"
    r = client.post("/v1/register", json={"user_email": email, "user_password": "testpass123"})
    assert r.status_code == 201
    code = captured["code"]

    # wrong code is rejected
    r = client.post("/v1/verify-email", json={"user_email": email, "code": "000000"})
    assert r.status_code == 400

    # correct code verifies and returns a token
    r = client.post("/v1/verify-email", json={"user_email": email, "code": code})
    assert r.status_code == 200
    assert "access_token" in r.json()

    # login now works
    r = client.post("/v1/login", json={"user_email": email, "user_password": "testpass123"})
    assert r.status_code == 200


def test_verify_email_already_verified(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(emailer, "send_verification_code",
                        lambda to, code: captured.__setitem__("code", code))
    email = "verify2@example.com"
    client.post("/v1/register", json={"user_email": email, "user_password": "testpass123"})
    client.post("/v1/verify-email", json={"user_email": email, "code": captured["code"]})
    # a second attempt on an already-verified account is a client error
    r = client.post("/v1/verify-email", json={"user_email": email, "code": captured["code"]})
    assert r.status_code == 400


def test_resend_verification_is_generic(client, monkeypatch):
    sent = []
    monkeypatch.setattr(emailer, "send_verification_code", lambda to, code: sent.append(to))
    r = client.post("/v1/resend-verification", json={"user_email": "nobody@example.com"})
    assert r.status_code == 200
    assert sent == []   # unknown email: no send, generic 200


def test_password_reset_flow(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(emailer, "send_password_reset",
                        lambda to, token: captured.__setitem__("token", token))

    email = "reset1@example.com"
    client.post("/v1/register", json={"user_email": email, "user_password": "oldpass123"})

    r = client.post("/v1/forgot-password", json={"user_email": email})
    assert r.status_code == 200
    token = captured["token"]

    # too-short password is rejected (before the token is consumed)
    r = client.post("/v1/reset-password", json={"token": token, "new_password": "short"})
    assert r.status_code == 400

    # valid reset succeeds
    r = client.post("/v1/reset-password", json={"token": token, "new_password": "newpass456"})
    assert r.status_code == 200

    # reset also verifies the email, so login with the NEW password works
    r = client.post("/v1/login", json={"user_email": email, "user_password": "newpass456"})
    assert r.status_code == 200

    # the token is single-use: reusing it fails
    r = client.post("/v1/reset-password", json={"token": token, "new_password": "another789"})
    assert r.status_code == 400


def test_forgot_password_unknown_email_is_generic(client, monkeypatch):
    sent = []
    monkeypatch.setattr(emailer, "send_password_reset", lambda to, token: sent.append(to))
    r = client.post("/v1/forgot-password", json={"user_email": "ghost@example.com"})
    assert r.status_code == 200
    assert sent == []


def test_reset_password_bad_token(client):
    r = client.post("/v1/reset-password", json={"token": "not-a-real-token", "new_password": "whatever8"})
    assert r.status_code == 400