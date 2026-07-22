package com.base.shared.utils

data class PasswordValidation(
    val isValid: Boolean,
    val missingRequirements: List<String> = emptyList(),
    val friendlyMessage: String = ""
)

object PasswordValidator {
    
    fun validatePassword(password: String): PasswordValidation {
        val missingRequirements = mutableListOf<String>()
        
        if (password.length < 8) {
            missingRequirements.add("at least 8 characters long")
        }
        
        if (!password.any { it.isUpperCase() }) {
            missingRequirements.add("at least one uppercase letter")
        }
        
        if (!password.any { it.isLowerCase() }) {
            missingRequirements.add("at least one lowercase letter")
        }
        
        if (!password.any { it.isDigit() }) {
            missingRequirements.add("at least one number")
        }
        
        if (!password.any { it in "!@#$%^&*(),.?\":{}|<>" }) {
            missingRequirements.add("at least one special character")
        }
        
        val isValid = missingRequirements.isEmpty()
        val friendlyMessage = if (isValid) {
            ""
        } else {
            formatRequirementsMessage(missingRequirements)
        }
        
        return PasswordValidation(isValid, missingRequirements, friendlyMessage)
    }
    
    private fun formatRequirementsMessage(missingRequirements: List<String>): String {
        return when (missingRequirements.size) {
            1 -> "Password must be ${missingRequirements[0]}."
            2 -> "Password must be ${missingRequirements[0]} and ${missingRequirements[1]}."
            else -> {
                val lastRequirement = missingRequirements.last()
                val otherRequirements = missingRequirements.dropLast(1).joinToString(", ")
                "Password must be $otherRequirements, and $lastRequirement."
            }
        }
    }
    
    fun getPasswordRequirements(): List<String> {
        return listOf(
            "At least 8 characters long",
            "At least one uppercase letter",
            "At least one lowercase letter", 
            "At least one number",
            "At least one special character (!@#$%^&*(),.?\":{}|<>)"
        )
    }
    
    fun checkRequirement(password: String, requirement: String): Boolean {
        return when (requirement) {
            "At least 8 characters long" -> password.length >= 8
            "At least one uppercase letter" -> password.any { it.isUpperCase() }
            "At least one lowercase letter" -> password.any { it.isLowerCase() }
            "At least one number" -> password.any { it.isDigit() }
            "At least one special character (!@#$%^&*(),.?\":{}|<>)" -> 
                password.any { it in "!@#$%^&*(),.?\":{}|<>" }
            else -> false
        }
    }
}