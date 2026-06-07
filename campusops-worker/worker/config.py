"""
CampusOps AI — Configuration
"""
import os
from dotenv import load_dotenv

load_dotenv()


class Config:
    """Application configuration loaded from environment variables."""

    # Flask
    SECRET_KEY = os.getenv("SECRET_KEY", "campusops-worker-secret")
    DEBUG = os.getenv("FLASK_ENV", "production") == "development"

    # Backend API
    BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")

    # Google Sheets
    GOOGLE_CREDENTIALS_FILE = os.getenv(
        "GOOGLE_CREDENTIALS_FILE", "credentials/service-account.json"
    )
    GOOGLE_SHEETS_CREDENTIALS = os.getenv("GOOGLE_SHEETS_CREDENTIALS", "")
    GOOGLE_SHEETS_CREDENTIALS_B64 = os.getenv("GOOGLE_SHEETS_CREDENTIALS_B64", "")

    # Scheduler
    SCHEDULER_ENABLED = os.getenv("SCHEDULER_ENABLED", "true").lower() == "true"
    SYNC_INTERVAL_MINUTES = int(os.getenv("SYNC_INTERVAL_MINUTES", "30"))


config = Config()
