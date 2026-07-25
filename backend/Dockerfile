# Meridia backend image.
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Install uv for fast, reproducible dependency installs.
RUN pip install --no-cache-dir uv

COPY pyproject.toml ./
RUN uv pip install --system .

COPY . .

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
