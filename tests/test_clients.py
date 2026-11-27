"""
Client identification.

The X-FitJournal-Client header, stored on registration and on every logged
workout. This is the one column in the admin dashboard feature that cannot be
backfilled, so these tests guard the two things that would silently lose it:
the header not being read at a write site, and a valid value from a client
that did not exist yet being thrown away.
"""

from datetime import date

from clients import resolve_client
from database import SessionLocal
import models

HEADER = "X-FitJournal-Client"


# --- The helper, in isolation ----------------------------------------------

def test_known_values_pass_through():
    assert resolve_client("web") == "web"
    assert resolve_client("android") == "android"


def test_value_is_normalised():
    """A header is typed by a human once, somewhere, and arrives shouting."""
    assert resolve_client("Android") == "android"
    assert resolve_client("  iOS  ") == "ios"


def test_an_unknown_but_wellformed_client_is_kept():
    """
    The whole point of validating shape rather than membership. The day an iOS
    or watch app ships, its rows must be stored from the first request, not
    discarded until a backend deploy catches up.
    """
    assert resolve_client("ios") == "ios"
    assert resolve_client("watchos") == "watchos"
    assert resolve_client("web-beta") == "web-beta"


def test_malformed_values_become_none():
    for bad in ("1web", "web app", "<script>alert(1)</script>", "a" * 21, "", None, "-web", "WEB_2"):
        assert resolve_client(bad) is None, bad


# --- At the write sites -----------------------------------------------------

def _register(client, email, header=None):
    headers = {HEADER: header} if header else {}
    return client.post(
        "/v1/register",
        json={"user_email": email, "user_password": "testpass123"},
        headers=headers,
    )


def _source_of(email):
    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_email == email).first()
        return u.signup_source
    finally:
        session.close()


def test_registration_records_the_client(client):
    _register(client, "src1@example.com", "android")
    assert _source_of("src1@example.com") == "android"


def test_registration_without_the_header_records_nothing(client):
    """An older client, or curl. Unknown is a real answer, not an error."""
    _register(client, "src2@example.com")
    assert _source_of("src2@example.com") is None


def test_registration_with_a_junk_header_records_nothing(client):
    _register(client, "src3@example.com", "not a client name")
    assert _source_of("src3@example.com") is None


def test_logging_a_manual_workout_records_the_client(auth):
    """
    Logged separately from the signup source because they genuinely differ:
    registering on the web and then logging everything from the phone is the
    normal case, and the dashboard should be able to see it.
    """
    c, uid, headers = auth["client"], auth["user_id"], dict(auth["headers"])
    headers[HEADER] = "android"

    made = c.post(
        f"/v1/exercises?user_id={uid}",
        json={"exercise_name": "Source Test Row", "exercise_muscle_group": "Chest"},
        headers=auth["headers"],
    )
    assert made.status_code in (200, 201), made.text
    exercise_id = made.json()["exercise_id"]

    resp = c.post(
        f"/v1/workout/log-manual/{uid}",
        json={
            "workout_date": str(date.today()),
            "exercises": [{"exercise_id": exercise_id, "sets_completed": 3}],
        },
        headers=headers,
    )
    assert resp.status_code in (200, 201), resp.text

    session = SessionLocal()
    try:
        row = (
            session.query(models.WorkoutSession)
            .filter(models.WorkoutSession.user_id == uid)
            .order_by(models.WorkoutSession.session_id.desc())
            .first()
        )
        assert row is not None
        assert row.source == "android"
    finally:
        session.close()
