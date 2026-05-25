"""Profile tests — fetching and updating the user profile."""


def test_get_profile(auth):
    """A freshly registered user's profile can be fetched."""
    response = auth["client"].get(
        f"/profile/{auth['user_id']}",
        headers=auth["headers"]
    )
    assert response.status_code == 200
    assert response.json()["user_email"] == "authuser@example.com"


def test_update_profile(auth):
    """Updating the profile persists the changes."""
    # Act: update some fields
    update = auth["client"].put(
        f"/profile/{auth['user_id']}",
        headers=auth["headers"],
        json={
            "user_first_name": "Leandro",
            "user_age": 30,
            "user_unit_preference": "metric"
        }
    )
    assert update.status_code == 200

    # Assert: re-fetch and confirm the change persisted
    profile = auth["client"].get(
        f"/profile/{auth['user_id']}",
        headers=auth["headers"]
    ).json()
    assert profile["user_first_name"] == "Leandro"
    assert profile["user_age"] == 30