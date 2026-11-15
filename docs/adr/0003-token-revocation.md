# ADR 0003: Revocation by version counter, not by refresh tokens

**Status:** Accepted, November 2026
**Context item:** security audit item 2, piece A

## Context

Login issued a stateless JWT with a 30-day expiry and no server-side record of
it. Nothing in the system could end a session early. The practical consequences:

1. A leaked token was valid for up to 30 days, and the only way to invalidate it
   was to rotate `SECRET_KEY`, which signs out every user of the app.
2. Password reset was theatre. A user who resets because they believe their
   account is compromised changed the password while the attacker's existing
   token kept working.
3. There was no "sign out everywhere", and no operator action for a single
   compromised account.

The deployment shapes the options. The API is a Lambda behind API Gateway with
MySQL on RDS, and no cache layer. Anything that needs a second store, or a
background sweeper, is new infrastructure to run and pay for on a portfolio
project.

Four options were considered.

**(a) Leave it.** Rejected. Item 2 exists because 30 days of unstoppable
validity is the single largest window in the app.

**(b) Server-side sessions, or a denylist of token ids.** Every issued token
gets a `jti`, stored on issue or on revoke, and checked on every request. This
is the most granular answer: it can kill one device and leave the others. It
costs a table that grows with logins, a sweeper to prune expired rows, and a
lookup on every request that the request does not otherwise make. It also does
not answer "revoke everything for this user" without a second mechanism.

**(c) Short access tokens plus refresh tokens.** The standard answer, and the
one most writing on JWTs recommends. It does not revoke anything by itself. It
shortens the window during which a stolen access token works, and moves the
revocation problem onto the refresh token, which still needs storage. Done
safely it means a refresh endpoint, rotation, reuse detection, and 401-retry
handling in both clients: the React SPA and the Android Compose app.

**(d) A version counter on the user row.** `users.token_version`, issued into
the token as a `ver` claim, compared on every authenticated request. Bumping the
integer invalidates every token ever issued to that user.

## Decision

> Revocation is a `token_version` integer on the user row, compared against a
> `ver` claim on every authenticated request. Shortening the token lifetime is a
> separate, later change, and does not replace this one.

Option (d), for three reasons.

`get_current_user` already loads the user row on every authenticated request, to
confirm the subject still exists. The check is therefore free: one integer
comparison on a row that is already in hand. Option (b) would add a lookup that
no request currently makes.

The schema cost is one column with a default, applied by one `ALTER TABLE`.
Option (b) is a new table plus the job that prunes it.

It answers the cases that actually exist. Password reset bumps the counter, so a
reset now ends every session. An operator can revoke one account with a single
`UPDATE`. Neither of those needs per-device granularity, which is the only thing
(b) buys over (d).

The check is strict: a token carrying no `ver` claim at all is refused, not
treated as version 0. That signed out every existing session on deploy, which
was accepted deliberately. Treating a missing claim as 0 would have left the
pre-deploy 30-day tokens valid, and those are exactly the tokens with no
revocation story.

Option (c) is still wanted and is not replaced by this. The two solve different
halves: the counter makes sessions revocable, a short expiry shrinks the window
in which nobody has noticed there is anything to revoke. The counter went first
because it was the half that needed a schema change, and because without it a
short-lived token still could not be killed on reset.

## Consequences

Good:

1. Revocation is immediate and total per user, with no new infrastructure.
2. Password reset now means what a user assumes it means.
3. There is a one-line operator action for a compromised account, documented in
   `scripts/add_user_token_version.sql`.

Bad:

1. Granularity is the user, not the device. Signing out one phone signs out
   everything. No feature needs otherwise today.
2. The claim must be added at every point that issues a token. There are two,
   login and the verification flow, and a third would be easy to miss. The tests
   cover both existing ones.
3. Revocation still depends on someone noticing. The 30-day window is now
   stoppable but it is not short, which is precisely what piece B is for.
4. The strict check means any future token format change logs everyone out on
   deploy.

## Revisit when

1. **Piece B lands.** Refresh tokens do not remove the counter. The refresh
   token becomes the thing the counter invalidates, and the access token gets
   short enough that checking `ver` on it stops mattering.
2. **Per-device revocation is a feature.** A session list with "sign out this
   device" needs option (b). The counter stays as the revoke-all.
3. **Something other than this API verifies tokens.** The counter requires a
   database read, so a verifier without database access cannot apply it. That
   case needs short expiries or a shared denylist instead.
4. **The users table becomes write-hot.** Bumping is rare today, once per reset.
   If it ever happens per request or per device, the counter belongs on its own
   row rather than on `users`.
