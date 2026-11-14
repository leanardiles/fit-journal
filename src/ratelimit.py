"""
Rate limiting for the password sign-in.

Lambda keeps no state between invocations, so the counters live in the
database (models.LoginAttempt). Two independent limits:

  per account  5 failures in 15 minutes  blocks that email for 15 minutes
  per address  20 failures in 15 minutes blocks that IP for 15 minutes

The per-account limit stops someone grinding passwords against one person.
The per-address limit stops the other shape of the same attack, a few common
passwords tried against many accounts, which never trips any single account's
counter.

A deliberate trade-off: an account lockout is itself a denial of service.
Anyone who knows an address can fail five sign-ins and lock its owner out.
The fifteen-minute window is what keeps that tolerable, and it recovers by
itself. The alternative, exponential backoff per account, degrades instead of
blocking but is more code and harder to reason about; at this size the
lockout is the better trade.
"""

from datetime import datetime, timedelta
from typing import Optional

from fastapi import HTTPException, Request
from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session

import models

WINDOW_MINUTES = 15
MAX_PER_EMAIL = 5
MAX_PER_IP = 20
RETENTION_HOURS = 24


def client_ip(request: Request) -> Optional[str]:
    """
    The address API Gateway saw, via Mangum's scope["client"], which it fills
    from requestContext.http.sourceIp.

    Not X-Forwarded-For. API Gateway appends to that header but preserves what
    the caller sent, so a client can put anything at the front of it. Keying a
    limit on a value the attacker controls is worse than having no limit,
    because it reads as protection and is not.
    """
    return request.client.host if request.client else None


def _window_start() -> datetime:
    return datetime.utcnow() - timedelta(minutes=WINDOW_MINUTES)


def _count(db: Session, column, value) -> int:
    if value is None:
        return 0
    return db.scalar(
        select(func.count())
        .select_from(models.LoginAttempt)
        .where(column == value, models.LoginAttempt.attempted_at >= _window_start())
    ) or 0


def check_login_allowed(db: Session, email: str, ip: Optional[str], detail: str) -> None:
    """
    Raise 429 if either limit is exceeded. Call this BEFORE looking the user
    up: a lockout that only applied to real accounts would tell an attacker
    which addresses are registered, which is the leak /forgot-password and
    /resend-verification already go out of their way to avoid.

    Checking first also means a blocked request never reaches verify_password.
    bcrypt is deliberately expensive, so this protects the compute bill too.
    """
    email = (email or "").strip().lower()
    too_many = (
        _count(db, models.LoginAttempt.user_email, email) >= MAX_PER_EMAIL
        or _count(db, models.LoginAttempt.ip, ip) >= MAX_PER_IP
    )
    if too_many:
        raise HTTPException(
            status_code=429,
            detail=detail,
            headers={"Retry-After": str(WINDOW_MINUTES * 60)},
        )


def record_failure(db: Session, email: str, ip: Optional[str]) -> None:
    """
    Record one failed sign-in, and sweep expired rows while we are here so the
    table does not need a scheduled job to stay small.
    """
    db.add(models.LoginAttempt(
        user_email=(email or "").strip().lower(),
        ip=ip,
        # Written from the app's clock, deliberately. The column has a
        # CURRENT_TIMESTAMP server default, but MySQL evaluates that in the
        # session time zone: a local MySQL uses system time while RDS runs
        # UTC. The window is compared against datetime.utcnow(), so letting
        # the database fill it meant a row written a second ago could look
        # hours old and never fall inside the window. One clock, both sides.
        attempted_at=datetime.utcnow(),
    ))
    db.execute(
        delete(models.LoginAttempt).where(
            models.LoginAttempt.attempted_at
            < datetime.utcnow() - timedelta(hours=RETENTION_HOURS)
        )
    )
    db.commit()


def clear_failures(db: Session, email: str) -> None:
    """A successful sign-in clears that account's counter."""
    db.execute(
        delete(models.LoginAttempt).where(
            models.LoginAttempt.user_email == (email or "").strip().lower()
        )
    )
    db.commit()
