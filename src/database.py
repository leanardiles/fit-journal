from sqlalchemy import create_engine, text
from sqlalchemy.orm import declarative_base, sessionmaker

from config import settings

# All DB configuration comes from the central settings object (see config.py):
# settings.database_url already includes ?charset=utf8mb4, which forces the
# CONNECTION to speak full 4-byte UTF-8 (schema charset is enforced separately).
engine = create_engine(
    settings.database_url,
    pool_pre_ping=True,
    pool_recycle=3600,
    echo=settings.db_echo,
)

# Create SessionLocal class for database sessions
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for SQLAlchemy models
Base = declarative_base()

# Dependency for FastAPI routes
def get_db():
    """
    Dependency that provides a database session to FastAPI routes.
    Automatically closes the session after the request is complete.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Test connection function
def test_connection():
    """
    Test database connection
    Returns True if successful, False otherwise
    """
    try:
        # Try to connect
        connection = engine.connect()
        print("[SUCCESS] Connected to MySQL database")
        
        # Test a simple query - use text() for raw SQL
        result = connection.execute(text("SELECT VERSION()"))
        version = result.fetchone()
        print(f"[INFO] MySQL version: {version[0]}")
        
        connection.close()
        return True
    except Exception as e:
        print(f"[ERROR] Failed to connect to MySQL: {e}")
        return False

# Test connection when running this file directly
if __name__ == "__main__":
    test_connection()