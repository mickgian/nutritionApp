package com.base.shared.auth

import com.base.shared.network.auth.AuthRepository
import com.base.shared.network.auth.AuthRepositoryImpl
import com.base.shared.storage.TokenStorage
import com.base.shared.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Authentication module that provides properly configured auth components
 * with full logging support.
 */
object AuthModule {
    
    private var _authManager: AuthManager? = null
    private var _authRepository: AuthRepository? = null
    
    // Create a coroutine scope for the module
    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Initialize the authentication system with platform context.
     * Call this once during app startup.
     */
    fun initialize(platformContext: Any?) {
        Logger.authInfo("MODULE_INIT_START", "Initializing authentication module")
        
        try {
            // Create TokenStorage with platform context
            val tokenStorage = TokenStorage(platformContext)
            Logger.authDebug("MODULE_INIT_STORAGE", "TokenStorage created successfully")
            
            // Create AuthManager with TokenStorage
            val authManager = AuthManager(tokenStorage)
            Logger.authDebug("MODULE_INIT_MANAGER", "AuthManager created successfully")
            
            // Create AuthRepository with AuthManager
            val authRepository = AuthRepositoryImpl(authManager = authManager)
            Logger.authDebug("MODULE_INIT_REPOSITORY", "AuthRepository created with AuthManager")
            
            // Store instances
            _authManager = authManager
            _authRepository = authRepository
            
            // Initialize the AuthManager to load existing tokens
            Logger.authDebug("MODULE_INIT_AUTH_MANAGER", "Initializing AuthManager...")
            moduleScope.launch {
                try {
                    authManager.initialize()
                    Logger.authInfo("MODULE_INIT_AUTH_MANAGER_SUCCESS", "AuthManager initialized successfully")
                } catch (e: Exception) {
                    Logger.authError("MODULE_INIT_AUTH_MANAGER_FAILED", "Failed to initialize AuthManager", e)
                }
            }
            
            Logger.authInfo("MODULE_INIT_SUCCESS", "Authentication module initialized successfully")
        } catch (e: Exception) {
            Logger.authError("MODULE_INIT_FAILED", "Failed to initialize auth module", e)
            throw e
        }
    }
    
    /**
     * Get the configured AuthManager instance.
     * Throws if not initialized.
     */
    fun getAuthManager(): AuthManager {
        return _authManager ?: throw IllegalStateException(
            "AuthModule not initialized. Call AuthModule.initialize() first."
        )
    }
    
    /**
     * Get the configured AuthRepository instance.
     * Throws if not initialized.
     */
    fun getAuthRepository(): AuthRepository {
        return _authRepository ?: throw IllegalStateException(
            "AuthModule not initialized. Call AuthModule.initialize() first."
        )
    }
    
    /**
     * Initialize AuthManager and start authentication state loading.
     * Call this after AuthModule.initialize().
     */
    suspend fun startAuthManager() {
        Logger.authInfo("MODULE_START_AUTH", "Starting AuthManager initialization")
        try {
            getAuthManager().initialize()
            Logger.authInfo("MODULE_START_SUCCESS", "AuthManager started successfully")
        } catch (e: Exception) {
            Logger.authError("MODULE_START_FAILED", "Failed to start AuthManager", e)
            throw e
        }
    }
    
    /**
     * Check if the module is initialized
     */
    fun isInitialized(): Boolean {
        return _authManager != null && _authRepository != null
    }
    
    /**
     * Clear the module (useful for testing or app restart)
     */
    fun clear() {
        Logger.authInfo("MODULE_CLEAR", "Clearing authentication module")
        _authManager = null
        _authRepository = null
    }
}