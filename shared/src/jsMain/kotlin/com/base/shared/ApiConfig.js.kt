package com.base.shared

actual object ApiConfig {
    // For development: try using the full URL with the actual backend
    // In production, this should be a relative URL or same-origin
    actual val BASE_URL = "http://localhost:8000/api/v1"
}