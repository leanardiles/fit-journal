"""Page-load smoke tests — verify web routes render without erroring."""

import pytest


@pytest.mark.parametrize("path", [
    "/web/login",
    "/web/register",
    "/web/forgot-password",
])
def test_public_pages_load(client, path):
    """Public (no-auth) pages return 200."""
    response = client.get(path)
    assert response.status_code == 200

def test_dashboard_page_loads(client):
    """The dashboard page route renders (client-side JS handles the auth redirect)."""
    response = client.get("/web/dashboard")
    assert response.status_code == 200    