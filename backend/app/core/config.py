"""Application settings, loaded from environment / .env.

All configuration is centralized here. Never hardcode secrets, hosts, or
thresholds elsewhere in the codebase — read them from ``settings``.
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- App ---
    app_name: str = "Meridia API"
    environment: str = "development"  # development | qa | production
    debug: bool = False
    api_v1_prefix: str = "/api/v1"

    # --- Database (Docker PostgreSQL for local dev, port 5432) ---
    database_url: str = "postgresql+psycopg://meridia:devpass@localhost:5432/meridia"

    # --- Auth / JWT ---
    # SECURITY: override secret_key in every deployed environment via env var.
    secret_key: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30

    # --- CORS (KMP clients: Android/iOS/Web/Desktop) ---
    cors_origins: list[str] = ["*"]

    @property
    def is_production(self) -> bool:
        return self.environment == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
