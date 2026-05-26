"""Authorization tests — users must not access each other's data."""


def test_cannot_read_another_users_profile(auth):
    """User A, with A's token, cannot fetch user B's profile."""
    client = auth["client"]
    user_a_headers = auth["headers"]  # token belongs to user A (authuser@example.com)

    # Create a second user, B
    client.post("/v1/register", json={
        "user_email": "userb@example.com",
        "user_password": "testpass123"
    })
    login_b = client.post("/v1/login", json={
        "user_email": "userb@example.com",
        "user_password": "testpass123"
    })
    user_b_id = login_b.json()["user_id"]

    # User A tries to read user B's profile using A's token
    response = client.get(
        f"/v1/profile/{user_b_id}",
        headers=user_a_headers
    )

    # Expectation: this should be forbidden (403) or otherwise rejected.
    # If it returns 200 with B's data, that's a real authorization gap.
    assert response.status_code in (403, 401, 404)