from sqlalchemy import create_engine, text  # Add 'text' to imports
from sqlalchemy.orm import declarative_base, sessionmaker
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database configuration
DB_HOST = os.getenv('DB_HOST')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USER')
DB_PASSWORD = os.getenv('DB_PASSWORD')
DB_NAME = os.getenv('DB_NAME')

# Create database URL for SQLAlchemy
DATABASE_URL = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Create SQLAlchemy engine
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=3600,
    echo=os.getenv("DB_ECHO", "false").lower() == "true"
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