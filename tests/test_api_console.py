"""
The interactive API console.

/docs, /redoc and /openapi.json are developer tools. In production they are a
working client aimed at the live database, so they are gated on the
environment. These tests guard the wiring rather than the policy: the policy
is one boolean, and the way it breaks is someone hardcoding a url back into
the FastAPI constructor without noticing which environment that reaches.
"""

from config import Settings, settings
import main


def test_console_matches_the_environment():
    """
    Whatever environment the suite runs in, the app must agree with it. This
    fails if a url is hardcoded past the flag.
    """
    if settings.is_production:
        assert main.app.docs_url is None
        assert main.app.redoc_url is None
        assert main.app.openapi_url is None
    else:
        assert main.app.docs_url == "/docs"
        assert main.app.openapi_url == "/openapi.json"


def test_console_routes_are_absent_when_off(client):
    """
    The schema matters as much as the two pages: left reachable it lets anyone
    rebuild the console themselves, which would make switching off the other
    two cosmetic.
    """
    if not settings.is_production:
        return

    for path in ("/docs", "/redoc", "/openapi.json"):
        assert client.get(path).status_code == 404, path


def test_environment_defaults_to_production():
    """
    The default has to be the safe one. If this ever flips, forgetting the
    variable in AWS silently publishes the console, and nothing on the running
    service says so.
    """
    assert Settings.model_fields["environment"].default == "production"


def test_is_production_is_not_fooled_by_case_or_space():
    """ENVIRONMENT is typed into a console by a human at least once."""
    common = dict(secret_key="x", db_host="h", db_user="u", db_password="p", db_name="n")
    assert Settings(environment=" Production ", **common).is_production is True
    assert Settings(environment="local", **common).is_production is False
