package com.base.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientProvider {

    /** A single Ktor client shared across the app. */
    val client: HttpClient = HttpClient {
        /* ---------- time-outs ---------- */
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis  = 60_000
            requestTimeoutMillis = 60_000
        }

        /* ---------- JSON ---------- */
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true   // tolerate extra fields
                }
            )
        }

        /* ---------- logging ---------- */
        install(Logging) {
            logger = Logger.SIMPLE
            level  = LogLevel.INFO           // basic info only, no sensitive headers or bodies
        }

        /* we don’t need to specify an engine here; KMP will pick the
           platform-specific default (OkHttp on Android, CIO on desktop, etc.) */
    }
}
