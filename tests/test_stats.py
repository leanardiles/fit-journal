"""
Admin dashboard figures.

Rows are written straight to the database here rather than through the API,
because the point is the arithmetic, not the endpoints. The cases that matter
are the ones where a plausible-looking implementation is quietly wrong:
counting sessions instead of people, letting a cohort denominator shrink, and
losing NULL clients.
"""

from datetime import datetime, timedelta

import pytest

from database import SessionLocal
import models
import stats


def _utc_now():
    return datetime.utcnow()


def _user(session, email, created=None, verified=True, source=None):
    u = models.User(
        user_email=email,
        user_password="x",
        user_email_verified=verified,
        signup_source=source,
    )
    if created is not None:
        u.user_created_at = created
    session.add(u)
    session.flush()
    return u


def _session_row(session, user_id, created=None, source=None, order=1):
    moment = created or _utc_now()
    row = models.WorkoutSession(
        user_id=user_id,
        routine_day_number=None,
        workout_date=moment.date(),
        session_order=order,
        source=source,
        created_at=moment,
    )
    session.add(row)
    session.flush()
    return row


@pytest.fixture
def db():
    s = SessionLocal()
    try:
        yield s
    finally:
        s.rollback()
        s.close()


# --- Counting people, not events -------------------------------------------

def test_two_logs_in_one_day_is_one_active_user(db):
    """
    The mistake this guards: counting sessions. Each Log WOD click writes its
    own row, so a morning and an evening log would read as two active users.
    """
    u = _user(db, "act1@example.com")
    _session_row(db, u.user_id, order=1)
    _session_row(db, u.user_id, order=2)
    db.commit()

    assert stats.compute_today(db)["dau"] == 1


def test_activation_counts_a_user_once(db):
    u = _user(db, "act2@example.com")
    for n in range(5):
        _session_row(db, u.user_id, created=_utc_now() - timedelta(days=n), order=n + 1)
    db.commit()

    assert stats.compute_today(db)["activated"] == 1


def test_a_registered_user_who_never_logged_is_not_activated(db):
    """The gap this is for: registered minus activated is the number to act on."""
    _user(db, "act3@example.com")
    db.commit()

    out = stats.compute_today(db)
    assert out["registered"] >= 1
    assert out["activated"] == 0


# --- Windows ----------------------------------------------------------------

def test_the_windows_nest(db):
    """A user active 10 days ago is in mau, not wau, not dau."""
    u = _user(db, "win1@example.com")
    _session_row(db, u.user_id, created=_utc_now() - timedelta(days=10))
    db.commit()

    out = stats.compute_today(db)
    assert (out["dau"], out["wau"], out["mau"]) == (0, 0, 1)


def test_activity_beyond_thirty_days_is_in_no_window(db):
    u = _user(db, "win2@example.com")
    _session_row(db, u.user_id, created=_utc_now() - timedelta(days=45))
    db.commit()

    out = stats.compute_today(db)
    assert (out["dau"], out["wau"], out["mau"]) == (0, 0, 0)


# --- The client split -------------------------------------------------------

def test_null_sources_become_unknown_rather_than_disappearing(db):
    """
    Every row written before the source columns existed is NULL, which is the
    majority of the data for months. Dropping them would make the split lie.
    """
    _user(db, "src-a@example.com", source="web")
    _user(db, "src-b@example.com", source=None)
    db.commit()

    registered = stats.compute_today(db)["by_source"]["registered"]
    assert registered.get("web") == 1
    assert registered.get(stats.UNKNOWN_CLIENT) >= 1


def test_signup_source_and_session_source_are_counted_separately(db):
    """Registering on the web then logging from the phone is the normal case."""
    u = _user(db, "src-c@example.com", source="web")
    _session_row(db, u.user_id, source="android")
    db.commit()

    by_source = stats.compute_today(db)["by_source"]
    assert by_source["registered"].get("web") == 1
    assert by_source["sessions_logged"].get("android") == 1
    assert "android" not in by_source["registered"]


# --- The snapshot -----------------------------------------------------------

def test_running_twice_in_a_day_updates_rather_than_duplicates(db):
    u = _user(db, "snap1@example.com")
    db.commit()
    stats.run_daily_stats(db)

    _session_row(db, u.user_id)
    db.commit()
    row = stats.run_daily_stats(db)

    same_day = db.query(models.DailyStat).filter(
        models.DailyStat.stat_date == row.stat_date
    ).count()
    assert same_day == 1
    assert row.dau == 1


def test_a_stored_row_survives_an_account_deletion(db):
    """
    The entire reason the table exists. Deleting a user cascades their
    sessions away, so every live figure silently changes. The snapshot has to
    keep saying what was true on the day.
    """
    u = _user(db, "snap2@example.com")
    _session_row(db, u.user_id)
    db.commit()

    row = stats.run_daily_stats(db)
    registered_then, dau_then = row.registered, row.dau
    assert dau_then == 1

    db.delete(u)
    db.commit()

    kept = db.query(models.DailyStat).filter(
        models.DailyStat.stat_date == row.stat_date
    ).first()
    assert kept.registered == registered_then
    assert kept.dau == dau_then
    # And the live figure has moved, which is what makes the snapshot worth it.
    assert stats.compute_today(db)["dau"] == 0


# --- Cohorts ----------------------------------------------------------------

def test_cohort_denominator_is_the_cohort_not_the_survivors(db):
    """
    Three signed up, one came back. The figure is 1 of 3, and `size` stays 3
    forever. A denominator that shrinks to the still-active is how a retention
    chart climbs while everyone leaves.
    """
    signup = stats._week_start(_utc_now()) + timedelta(hours=2)
    users = [_user(db, f"coh{i}@example.com", created=signup) for i in range(3)]
    _session_row(db, users[0].user_id, created=signup + timedelta(days=1))
    db.commit()

    grid = stats.compute_cohorts(db)
    row = next(r for r in grid if r["cohort_start"] == signup.date().isoformat())
    assert row["size"] == 3
    assert row["weeks"][0] == 1


def test_a_cohort_week_that_has_not_happened_is_absent(db):
    """A zero for a future week reads as churn, which would be a lie."""
    signup = stats._week_start(_utc_now()) + timedelta(hours=2)
    _user(db, "coh-future@example.com", created=signup)
    db.commit()

    grid = stats.compute_cohorts(db)
    row = next(r for r in grid if r["cohort_start"] == signup.date().isoformat())
    assert len(row["weeks"]) == 1


def test_signing_up_is_not_an_active_week(db):
    """Week 0 is not automatically the whole cohort: it answers who logged."""
    signup = stats._week_start(_utc_now()) + timedelta(hours=2)
    _user(db, "coh-quiet@example.com", created=signup)
    db.commit()

    grid = stats.compute_cohorts(db)
    row = next(r for r in grid if r["cohort_start"] == signup.date().isoformat())
    assert row["weeks"][0] == 0


# --- The scheduled entry point ----------------------------------------------

def test_the_lambda_handler_runs_the_job_for_a_schedule_event(db):
    """
    EventBridge sends something that is not an HTTP request, so Mangum raises
    on it. The handler has to recognise it before Mangum sees it.
    """
    import main

    out = main.handler({"source": "aws.events", "detail-type": "Scheduled Event"}, None)
    assert out["ok"] is True

    from datetime import date as _date

    row = db.query(models.DailyStat).filter(
        models.DailyStat.stat_date == _date.fromisoformat(out["stat_date"])
    ).first()
    assert row is not None


def test_anything_else_still_goes_to_the_web_app(monkeypatch):
    """
    The dispatch is on the one field EventBridge guarantees. Everything else,
    including an event with no source at all, is an HTTP request.
    """
    import main

    seen = {}
    monkeypatch.setattr(main, "_http_handler", lambda event, context: seen.setdefault("event", event))

    main.handler({"requestContext": {"http": {"method": "GET"}}}, None)
    assert "event" in seen


def test_an_unrecognised_event_says_so():
    """
    A schedule wired up without the right payload is the likely mistake, and
    Mangum's own failure for it is unreadable. This one names it.
    """
    import main
    import pytest as _pytest

    with _pytest.raises(ValueError, match="source=aws.events"):
        main.handler({"detail-type": "Scheduled Event"}, None)

