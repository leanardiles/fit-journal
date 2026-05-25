"""Authentication tests — register and login flows."""


def test_register_new_user(client):
    """Registering a brand-new user succeeds."""
    response = client.post("/register", json={
        "user_email": "newuser@example.com",
        "user_password": "testpass123"
    })
    assert response.status_code == 201


def test_login_correct_credentials(client):
    """A registered user can log in and receives an access token."""
    # Arrange: register the user first
    client.post("/register", json={
        "user_email": "loginuser@example.com",
        "user_password": "testpass123"
    })

    # Act: log in with those credentials
    response = client.post("/login", json={
        "user_email": "loginuser@example.com",
        "user_password": "testpass123"
    })

    # Assert: success + a token comes back
    assert response.status_code == 200
    assert "access_token" in response.json()

def test_register_duplicate_email(client):
    """Registering with an already-used email is rejected, not a crash."""
    # Arrange: register once
    client.post("/register", json={
        "user_email": "dupe@example.com",
        "user_password": "testpass123"
    })
    # Act: register the same email again
    response = client.post("/register", json={
        "user_email": "dupe@example.com",
        "user_password": "testpass123"
    })
    # Assert: a client error (not 2xx, not a 500 crash)
    assert response.status_code == 400


def test_login_wrong_password(client):
    """Login with the wrong password is rejected. (Guards the verify_password path.)"""
    client.post("/register", json={
        "user_email": "wrongpw@example.com",
        "user_password": "correctpass123"
    })
    response = client.post("/login", json={
        "user_email": "wrongpw@example.com",
        "user_password": "WRONGpassword"
    })
    assert response.status_code == 401


def test_login_nonexistent_email(client):
    """Login with an email that was never registered is rejected."""
    response = client.post("/login", json={
        "user_email": "ghost@example.com",
        "user_password": "whatever123"
    })
    assert response.status_code == 401    

def test_protected_endpoint_requires_token(client):
    """Hitting a protected endpoint with no token is rejected."""
    response = client.get("/exercises?user_id=1")  # no Authorization header
    assert response.status_code in (401, 403)    