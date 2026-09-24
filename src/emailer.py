"""
Transactional email + short-lived auth secrets (verification codes, reset tokens).

- SES is called via boto3, imported lazily so the app runs without it when
  EMAIL_ENABLED is false (local dev and tests need no AWS credentials).
- Only SHA-256 hashes of codes/tokens are ever stored; see models.AuthToken.
- When settings.email_enabled is false, emails are printed to the console
  instead of sent, so you can develop the flow without sending anything.
"""

import hashlib
import secrets

from config import settings


# ---------- secret generation + hashing ----------

def generate_code() -> str:
    """A 6-digit numeric verification code, zero-padded (e.g. '004217')."""
    return f"{secrets.randbelow(1_000_000):06d}"


def generate_token() -> str:
    """A URL-safe, high-entropy token for password-reset links."""
    return secrets.token_urlsafe(32)


def hash_secret(value: str) -> str:
    """SHA-256 hex digest. We store this, never the plaintext code/token."""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


# ---------- low-level send ----------

_ses_client = None

def _client():
    global _ses_client
    if _ses_client is None:
        import boto3  # lazy: only imported when actually sending
        _ses_client = boto3.client("ses", region_name=settings.ses_region)
    return _ses_client


def send_email(to_address: str, subject: str, body_text: str, body_html: str | None = None) -> None:
    """Send one email via SES, or print it when email is disabled (local/test)."""
    if not settings.email_enabled:
        print(f"\n[EMAIL DISABLED] to={to_address}\nSubject: {subject}\n{body_text}\n")
        return

    body = {"Text": {"Data": body_text, "Charset": "UTF-8"}}
    if body_html:
        body["Html"] = {"Data": body_html, "Charset": "UTF-8"}

    _client().send_email(
        Source=settings.email_from,
        Destination={"ToAddresses": [to_address]},
        ReplyToAddresses=[settings.email_reply_to],
        Message={
            "Subject": {"Data": subject, "Charset": "UTF-8"},
            "Body": body,
        },
    )


# ---------- message templates ----------

def send_verification_code(to_address: str, code: str) -> None:
    minutes = settings.email_verify_code_ttl_minutes
    subject = "Your FitJournal verification code"
    text = (
        f"Welcome to FitJournal.\n\n"
        f"Your verification code is {code}. It expires in {minutes} minutes.\n\n"
        f"If you did not create an account, you can ignore this email."
    )
    html = (
        f"<p>Welcome to FitJournal.</p>"
        f"<p>Your verification code is <strong style='font-size:20px'>{code}</strong>. "
        f"It expires in {minutes} minutes.</p>"
        f"<p>If you did not create an account, you can ignore this email.</p>"
    )
    send_email(to_address, subject, text, html)


def send_password_reset(to_address: str, token: str) -> None:
    minutes = settings.password_reset_ttl_minutes
    link = f"{settings.frontend_base_url}/reset-password?token={token}"
    subject = "Reset your FitJournal password"
    text = (
        f"We received a request to reset your FitJournal password.\n\n"
        f"Use this link to set a new one (expires in {minutes} minutes, one-time use):\n{link}\n\n"
        f"If you did not request this, you can ignore this email."
    )
    html = (
        f"<p>We received a request to reset your FitJournal password.</p>"
        f"<p><a href='{link}'>Reset your password</a> "
        f"(expires in {minutes} minutes, one-time use).</p>"
        f"<p>If you did not request this, you can ignore this email.</p>"
    )
    send_email(to_address, subject, text, html)