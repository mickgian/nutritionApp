"""Application settings, loaded from environment / .env.

All configuration is centralized here. Never hardcode secrets, hosts, or
thresholds elsewhere in the codebase — read them from ``settings``.
"""

from functools import lru_cache

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

# Placeholder secret shipped in .env.example — must never survive into production.
_DEFAULT_SECRET = "change-me-in-production"
_MIN_SECRET_LENGTH = 32


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

    @model_validator(mode="after")
    def _require_strong_secret_in_production(self) -> "Settings":
        """Fail-closed: refuse to boot in production with a weak/default secret."""
        if self.is_production and (
            self.secret_key == _DEFAULT_SECRET or len(self.secret_key) < _MIN_SECRET_LENGTH
        ):
            raise ValueError(
                "SECRET_KEY must be overridden with a strong value "
                f"(>= {_MIN_SECRET_LENGTH} chars) in production."
            )
        return self


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
