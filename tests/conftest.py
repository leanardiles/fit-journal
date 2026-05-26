"""
Pytest configuration and shared fixtures for FitJournal tests.

Tests run against a SEPARATE MySQL test database (fitjournaldb_test),
never the dev or production data. Tables are created fresh and dropped
around the test session, so tests start from a known-clean schema.
"""

import os
import sys

# --- 1. Make src/ importable -------------------------------------------------
# main.py uses flat imports (import models, from database import ...), so it
# expects to run from inside src/. Add src/ to the path so `import main` works
# from the tests/ folder.
SRC_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src")
sys.path.insert(0, SRC_DIR)

# --- 2. Point the app at the TEST database BEFORE importing it ---------------
# database.py reads these env vars at import time and builds the engine, so we
# must override them first. We reuse the existing local .env credentials but
# swap DB_NAME to the test schema.
from dotenv import load_dotenv

# Load the real .env (gets DB_HOST, DB_USER, DB_PASSWORD, etc. for local MySQL)
load_dotenv(os.path.join(SRC_DIR, ".env"))

# Override the database name to the dedicated test schema
os.environ["DB_NAME"] = "fitjournaldb_test"
# Ensure a SECRET_KEY exists for JWT operations during tests
os.environ.setdefault("SECRET_KEY", "test-secret-key-not-for-production")

# --- 3. Now it's safe to import the app and its DB objects -------------------
import pytest
from fastapi.testclient import TestClient

import models
from database import engine, SessionLocal, get_db
import main  # importing main triggers create_all on the (now test) engine


# --- 4. Schema lifecycle: build tables once, drop them after the run ---------
@pytest.fixture(scope="session", autouse=True)
def _setup_test_schema():
    """Create all tables in the test DB before tests, drop them after."""
    models.Base.metadata.create_all(bind=engine)
    yield
    models.Base.metadata.drop_all(bind=engine)


# --- 5. Per-test clean slate -------------------------------------------------
@pytest.fixture(autouse=True)
def _clean_tables():
    """
    Wipe all table rows before each test so tests are independent and
    repeatable (order-independent, no leftover data between tests).
    """
    yield  # run the test first...
    # ...then clean up after it
    session = SessionLocal()
    try:
        # Delete in reverse dependency order to respect foreign keys
        for table in reversed(models.Base.metadata.sorted_tables):
            session.execute(table.delete())
        session.commit()
    finally:
        session.close()


# --- 6. The test client ------------------------------------------------------
@pytest.fixture
def client():
    """
    A FastAPI TestClient that talks to the app in-memory (no real server).
    Use it in tests like: response = client.post("/v1/login", json={...})
    """
    return TestClient(main.app)

# --- 7. Auth fixture ------------------------------------------------------
@pytest.fixture
def auth(client):
    """
    Registers a test user, logs in, and returns the client plus the user's
    id and auth headers — ready for hitting protected endpoints.
    """
    email = "authuser@example.com"
    password = "testpass123"

    client.post("/v1/register", json={"user_email": email, "user_password": password})
    login = client.post("/v1/login", json={"user_email": email, "user_password": password})
    data = login.json()

    return {
        "client": client,
        "user_id": data["user_id"],
        "token": data["access_token"],
        "headers": {"Authorization": f"Bearer {data['access_token']}"},
    }