"""
The admin dashboard endpoint.

GET /v1/admin/stats is the only privileged route in the app, so these tests
are mostly about the boundary rather than the numbers. The numbers have their
own tests in test_stats.py.
"""

from database import SessionLocal
import models


def _make_admin(user_id):
    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_id == user_id).first()
        u.is_admin = True
        session.commit()
    finally:
        session.close()


# --- The boundary -----------------------------------------------------------

def test_an_ordinary_user_is_refused(auth):
    """
    The default. is_admin is false for every account until someone runs an
    UPDATE by hand, so this is what the route does for everybody.
    """
    resp = auth["client"].get("/v1/admin/stats", headers=auth["headers"])
    assert resp.status_code == 403, resp.text


def test_no_token_is_refused(auth):
    resp = auth["client"].get("/v1/admin/stats")
    assert resp.status_code in (401, 403), resp.text


def test_an_admin_gets_the_numbers(auth):
    _make_admin(auth["user_id"])
    resp = auth["client"].get("/v1/admin/stats", headers=auth["headers"])
    assert resp.status_code == 200, resp.text

    body = resp.json()
    assert set(body) == {"today", "history", "cohorts"}
    assert body["today"]["registered"] >= 1
    assert isinstance(body["history"], list)
    assert isinstance(body["cohorts"], list)


def test_losing_the_flag_closes_the_door_again(auth):
    """
    The flag is read per request, not baked into the token. Revoking admin
    must not require the user to log out, or demoting someone would depend on
    them cooperating.
    """
    _make_admin(auth["user_id"])
    assert auth["client"].get("/v1/admin/stats", headers=auth["headers"]).status_code == 200

    session = SessionLocal()
    try:
        u = session.query(models.User).filter(models.User.user_id == auth["user_id"]).first()
        u.is_admin = False
        session.commit()
    finally:
        session.close()

    assert auth["client"].get("/v1/admin/stats", headers=auth["headers"]).status_code == 403


# --- Shape ------------------------------------------------------------------

def test_history_is_oldest_first(auth):
    """The page draws lines left to right, so the API hands them over in order."""
    import stats
    from database import SessionLocal as SL

    _make_admin(auth["user_id"])
    s = SL()
    try:
        stats.run_daily_stats(s)
    finally:
        s.close()

    history = auth["client"].get("/v1/admin/stats", headers=auth["headers"]).json()["history"]
    dates = [row["stat_date"] for row in history]
    assert dates == sorted(dates)


def test_today_is_live_before_any_snapshot_exists(auth):
    """
    The page has to be useful on the day it ships, before the scheduled job
    has ever run. An empty history is fine; an empty today is not.
    """
    _make_admin(auth["user_id"])
    body = auth["client"].get("/v1/admin/stats", headers=auth["headers"]).json()
    assert body["history"] == []
    assert body["today"]["registered"] >= 1
