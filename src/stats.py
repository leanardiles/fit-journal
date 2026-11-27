"""
Numbers for the admin dashboard.

Three things live here: today's figures computed from the live tables, the
daily snapshot that persists them, and the cohort retention grid.

**Everything is UTC.** RDS runs UTC and a developer machine usually does not,
so "today" has to be pinned to one of them or the two environments disagree
about which day a signup belongs to. `ratelimit.py` learned this the expensive
way, when MySQL's CURRENT_TIMESTAMP evaluated in the session time zone and
made every row look five hours old.

**Activity means `created_at`, never `workout_date`.** The date picker lets a
user backdate a log, so `workout_date` says when training happened while
`created_at` says when the app was used. Only the second is engagement.
"""

from datetime import datetime, timedelta, timezone
from typing import Optional

from sqlalchemy import func
from sqlalchemy.orm import Session

import models

# Trailing windows, in days, for the active-user counts.
ACTIVE_WINDOWS = {"dau": 1, "wau": 7, "mau": 30}

# How much cohort history the grid returns, and how far each cohort is followed.
COHORT_WEEKS_BACK = 12
COHORT_WEEKS_FOLLOWED = 8

# What a NULL client is called once it reaches the dashboard. Every row written
# before the source columns existed says NULL, and that is a real answer rather
# than missing data.
UNKNOWN_CLIENT = "unknown"


def _utc_now() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def _utc_midnight(now: Optional[datetime] = None) -> datetime:
    return (now or _utc_now()).replace(hour=0, minute=0, second=0, microsecond=0)


def _tally(pairs) -> dict:
    """Count (source, value) pairs into a dict, NULL folded into unknown."""
    out = {}
    for source, value in pairs:
        key = source or UNKNOWN_CLIENT
        out[key] = out.get(key, 0) + int(value or 0)
    return out


def compute_today(db: Session) -> dict:
    """
    Every figure the dashboard shows, as at this moment.

    The same function serves the scheduled job and the live request, so there
    is one definition of each number rather than two that drift.
    """
    now = _utc_now()
    midnight = _utc_midnight(now)

    registered = db.query(func.count(models.User.user_id)).scalar() or 0
    verified = (
        db.query(func.count(models.User.user_id))
        .filter(models.User.user_email_verified.is_(True))
        .scalar()
        or 0
    )

    # Users who got as far as building a routine, and users who got as far as
    # logging anything. The second is the activation number.
    with_routine = (
        db.query(func.count(func.distinct(models.TrainingDay.user_id))).scalar() or 0
    )
    activated = (
        db.query(func.count(func.distinct(models.WorkoutSession.user_id))).scalar() or 0
    )

    new_signups = (
        db.query(func.count(models.User.user_id))
        .filter(models.User.user_created_at >= midnight)
        .scalar()
        or 0
    )
    sessions_logged = (
        db.query(func.count(models.WorkoutSession.session_id))
        .filter(models.WorkoutSession.created_at >= midnight)
        .scalar()
        or 0
    )

    actives = {}
    for name, days in ACTIVE_WINDOWS.items():
        actives[name] = (
            db.query(func.count(func.distinct(models.WorkoutSession.user_id)))
            .filter(models.WorkoutSession.created_at >= now - timedelta(days=days))
            .scalar()
            or 0
        )

    return {
        "stat_date": midnight.date(),
        "registered": registered,
        "verified": verified,
        "with_routine": with_routine,
        "activated": activated,
        "new_signups": new_signups,
        "sessions_logged": sessions_logged,
        **actives,
        "by_source": _by_source(db, now, midnight),
    }


def _by_source(db: Session, now: datetime, midnight: datetime) -> dict:
    """
    The client split, keyed counter-first so each map is complete on its own.

    Two different groupings live here and conflating them would be wrong:
    `registered` and `new_signups` group by `users.signup_source`, which is
    where the account came from, while `sessions_logged` and `wau` group by
    `workout_sessions.source`, which is where the logging came from. They
    genuinely differ, since registering on the web and then logging from the
    phone is the normal case.
    """
    registered = db.query(models.User.signup_source, func.count(models.User.user_id)).group_by(
        models.User.signup_source
    ).all()

    new_signups = (
        db.query(models.User.signup_source, func.count(models.User.user_id))
        .filter(models.User.user_created_at >= midnight)
        .group_by(models.User.signup_source)
        .all()
    )

    sessions_logged = (
        db.query(models.WorkoutSession.source, func.count(models.WorkoutSession.session_id))
        .filter(models.WorkoutSession.created_at >= midnight)
        .group_by(models.WorkoutSession.source)
        .all()
    )

    wau = (
        db.query(
            models.WorkoutSession.source,
            func.count(func.distinct(models.WorkoutSession.user_id)),
        )
        .filter(models.WorkoutSession.created_at >= now - timedelta(days=ACTIVE_WINDOWS["wau"]))
        .group_by(models.WorkoutSession.source)
        .all()
    )

    return {
        "registered": _tally(registered),
        "new_signups": _tally(new_signups),
        "sessions_logged": _tally(sessions_logged),
        "wau": _tally(wau),
    }


def run_daily_stats(db: Session) -> models.DailyStat:
    """
    Persist today's figures. Upsert, not insert: a manual invoke, a retry, or
    a schedule that fires twice updates the row rather than failing or
    duplicating it.
    """
    values = compute_today(db)
    stat_date = values.pop("stat_date")

    row = db.query(models.DailyStat).filter(models.DailyStat.stat_date == stat_date).first()
    if row is None:
        row = models.DailyStat(stat_date=stat_date)
        db.add(row)

    for field, value in values.items():
        setattr(row, field, value)
    row.computed_at = _utc_now()

    db.commit()
    db.refresh(row)
    return row


def _week_start(moment: datetime) -> datetime:
    """Monday 00:00 UTC of the week containing `moment`."""
    day = moment.replace(hour=0, minute=0, second=0, microsecond=0)
    return day - timedelta(days=day.weekday())


def compute_cohorts(
    db: Session,
    weeks_back: int = COHORT_WEEKS_BACK,
    weeks_followed: int = COHORT_WEEKS_FOLLOWED,
) -> list:
    """
    Weekly cohort retention, computed live from the raw tables.

    A cohort is everyone who signed up in one week. For each following week,
    the figure is how many of THAT COHORT logged a workout in it. The
    denominator is frozen at the cohort's size and never shrinks to the
    still-active, which is the mistake that makes retention charts look like
    they are improving while users leave.

    Week 0 is the signup week itself, and it is not automatically 100 percent:
    it answers how many logged anything at all in their first week, which is
    activation rather than retention. Signing up is not an active day.

    Live rather than stored: logging a workout IS the meaningful action, so no
    events table is needed, and the whole thing is two queries plus arithmetic
    on tables this size.
    """
    now = _utc_now()
    earliest = _week_start(now) - timedelta(weeks=weeks_back - 1)

    users = (
        db.query(models.User.user_id, models.User.user_created_at)
        .filter(models.User.user_created_at >= earliest)
        .all()
    )
    if not users:
        return []

    cohort_of = {}
    sizes = {}
    for user_id, created in users:
        if created is None:
            continue
        start = _week_start(created)
        cohort_of[user_id] = start
        sizes[start] = sizes.get(start, 0) + 1

    sessions = (
        db.query(models.WorkoutSession.user_id, models.WorkoutSession.created_at)
        .filter(models.WorkoutSession.created_at >= earliest)
        .all()
    )

    # cohort start -> week offset -> set of user ids active in it
    active = {}
    for user_id, created in sessions:
        start = cohort_of.get(user_id)
        if start is None or created is None:
            continue
        offset = (created - start).days // 7
        if offset < 0 or offset >= weeks_followed:
            continue
        active.setdefault(start, {}).setdefault(offset, set()).add(user_id)

    grid = []
    for start in sorted(sizes, reverse=True):
        per_week = active.get(start, {})
        # Only weeks that have actually elapsed are reported. A zero for a week
        # that has not happened yet reads as churn, which would be a lie.
        elapsed = min(weeks_followed, (now - start).days // 7 + 1)
        grid.append(
            {
                "cohort_start": start.date().isoformat(),
                "size": sizes[start],
                "weeks": [len(per_week.get(i, set())) for i in range(elapsed)],
            }
        )
    return grid
