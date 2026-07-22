package com.base.shared

actual object ApiConfig {
    // iOS simulator needs host IP address, not localhost
    actual val BASE_URL = "http://192.168.1.9:8000/api/v1"
}