"""
Transactional email + short-lived auth secrets (verification codes, reset tokens).

- Email is sent through Resend's HTTP API. We use the standard library only
  (urllib), so there is no extra dependency to bundle for Lambda.
- Only SHA-256 hashes of codes/tokens are ever stored; see models.AuthToken.
- When settings.email_enabled is false, emails are printed to the console
  instead of sent, so you can develop the flow without sending anything.
"""

import hashlib
import json
import secrets
import urllib.error
import urllib.request

from config import settings

RESEND_ENDPOINT = "https://api.resend.com/emails"


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


# ---------- low-level send (Resend) ----------

def send_email(to_address: str, subject: str, body_text: str, body_html: str | None = None) -> None:
    """Send one email via Resend, or print it when email is disabled (local/test)."""
    if not settings.email_enabled:
        print(f"\n[EMAIL DISABLED] to={to_address}\nSubject: {subject}\n{body_text}\n")
        return

    payload = {
        "from": f"FitJournal <{settings.email_from}>",
        "to": [to_address],
        "reply_to": settings.email_reply_to,
        "subject": subject,
        "text": body_text,
    }
    if body_html:
        payload["html"] = body_html

    request = urllib.request.Request(
        RESEND_ENDPOINT,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={
            "Authorization": f"Bearer {settings.resend_api_key}",
            "Content-Type": "application/json",
            "User-Agent": "FitJournal/1.0 (+https://fit-journal.com)",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as resp:
            resp.read()
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        raise RuntimeError(f"Resend send failed ({e.code}): {body}") from e

# ---------- shared email shell (dark, on-brand) ----------

APP_NAME = "FitJournal"
LOGO_URL = "https://fit-journal.com/kettlebell.png"  # served from web-react/public/ after deploy
BG = "#171717"        # app dark background
CARD = "#1e1e1e"
TEXT = "#d8d8d8"
MUTED = "#8a8a8a"
RED = "#c0392b"       # brand red
YELLOW = "#f4d93e"    # app primary-button yellow
BORDER = "#2c2c2c"


def _shell(inner_html: str) -> str:
    """Wrap inner content in the dark, on-brand email frame (email-client-safe)."""
    return f"""\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<meta name="supported-color-schemes" content="dark">
</head>
<body style="margin:0; padding:0; background-color:{BG};">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:{BG};">
    <tr>
      <td align="center" style="padding:32px 16px;">
        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="width:480px; max-width:480px; background-color:{CARD}; border:1px solid {BORDER}; border-radius:12px;">
          <tr>
            <td align="center" style="padding:28px 32px 8px 32px;">
              <img src="{LOGO_URL}" alt="{APP_NAME}" width="90"
                   style="display:block; border:0; width:90px; height:auto; color:{RED}; font-family:Georgia,serif; font-size:24px; font-weight:bold;">
            </td>
          </tr>
          <tr>
            <td style="padding:8px 32px 24px 32px; font-family:Helvetica,Arial,sans-serif; color:{TEXT}; font-size:15px; line-height:1.6;">
              {inner_html}
            </td>
          </tr>
          <tr>
            <td style="padding:16px 32px 28px 32px; border-top:1px solid {BORDER}; font-family:Helvetica,Arial,sans-serif; color:{MUTED}; font-size:12px; line-height:1.5;">
              This is an automated message about your {APP_NAME} account. Questions? Reply to this email or reach us at info@fit-journal.com.
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""


# ---------- message templates ----------

def send_verification_code(to_address: str, code: str) -> None:
    minutes = settings.email_verify_code_ttl_minutes
    subject = "Your FitJournal verification code"
    text = (
        f"Welcome to FitJournal.\n\n"
        f"Your verification code is {code}. It expires in {minutes} minutes.\n\n"
        f"If you did not create an account, you can ignore this email."
    )
    inner = f"""
      <p style="margin:0 0 16px 0; color:{TEXT};">Welcome to FitJournal. Enter this code to verify your email and activate your account:</p>
      <div style="text-align:center; margin:8px 0 20px 0;">
        <span style="display:inline-block; background-color:{BG}; border:1px solid {BORDER}; border-radius:8px; padding:14px 22px; font-family:'Courier New',monospace; font-size:30px; font-weight:bold; letter-spacing:8px; color:#ffffff;">{code}</span>
      </div>
      <p style="margin:0 0 8px 0; color:{MUTED};">This code expires in {minutes} minutes.</p>
      <p style="margin:0; color:{MUTED};">If you did not create an account, you can ignore this email.</p>
    """
    send_email(to_address, subject, text, _shell(inner))


def send_password_reset(to_address: str, token: str) -> None:
    minutes = settings.password_reset_ttl_minutes
    link = f"{settings.frontend_base_url}/reset-password?token={token}"
    subject = "Reset your FitJournal password"
    text = (
        f"We received a request to reset your FitJournal password.\n\n"
        f"Use this link to set a new one (expires in {minutes} minutes, one-time use):\n{link}\n\n"
        f"If you did not request this, you can ignore this email."
    )
    inner = f"""
      <p style="margin:0 0 20px 0; color:{TEXT};">We received a request to reset your FitJournal password. Click the button to set a new one:</p>
      <div style="text-align:center; margin:0 0 20px 0;">
        <a href="{link}" style="display:inline-block; background-color:{YELLOW}; color:#171717; text-decoration:none; font-family:Helvetica,Arial,sans-serif; font-weight:bold; font-size:15px; padding:12px 28px; border-radius:8px;">Reset password</a>
      </div>
      <p style="margin:0 0 16px 0; color:{MUTED}; font-size:13px;">Or paste this link into your browser:<br>
        <a href="{link}" style="color:{YELLOW}; word-break:break-all;">{link}</a></p>
      <p style="margin:0 0 8px 0; color:{MUTED};">This link expires in {minutes} minutes and can be used once.</p>
      <p style="margin:0; color:{MUTED};">If you did not request this, you can ignore this email.</p>
    """
    send_email(to_address, subject, text, _shell(inner))