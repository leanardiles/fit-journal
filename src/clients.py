"""
Which client app a request came from.

Captured on registration and on every logged workout so the admin dashboard
can tell web users from Android ones. This is the one piece of that feature
that cannot be added retroactively: a row written before the column existed
says NULL forever, and no amount of later analysis recovers which app it came
from. Hence it ships ahead of everything else it is for.

The contract: header X-FitJournal-Client, lowercase, today "web" or "android".
"""

import re
from typing import Optional

CLIENT_HEADER = "X-FitJournal-Client"

# Shape, not membership. A strict allowlist would mean that the day an iOS
# app, a watch app or anything else ships, every row it writes is discarded
# until a backend deploy catches up, which is exactly the data loss this
# column exists to prevent, at exactly the moment the data matters most.
#
# The risk this accepts is a buggy client inventing a bucket. That is bounded
# by the pattern and by the column width, it only ever affects a grouping on
# an admin page, it is visible the moment anyone looks at it, and one UPDATE
# fixes it. Losing a platform's first weeks is not fixable at all.
_CLIENT_PATTERN = re.compile(r"^[a-z][a-z0-9-]{0,19}$")

# For ordering and labelling buckets on the dashboard. NOT used to validate:
# a value missing from this list is still stored, it just sorts last.
KNOWN_CLIENTS = ("web", "android", "ios")


def resolve_client(header_value: Optional[str]) -> Optional[str]:
    """
    Normalise the client header into something storable, or None.

    None means unknown, which covers a missing header, a malformed one, and
    every row written before this existed. It is a real value on the
    dashboard rather than an error.
    """
    if not header_value:
        return None
    value = header_value.strip().lower()
    return value if _CLIENT_PATTERN.match(value) else None
