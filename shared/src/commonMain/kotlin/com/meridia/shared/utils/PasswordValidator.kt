package com.meridia.shared.utils

data class PasswordValidation(
    val isValid: Boolean,
    val missingRequirements: List<String> = emptyList(),
    val friendlyMessage: String = "",
)

/**
 * Client-side password policy (Italian copy). Stricter than the backend's minimum
 * (≥8 chars) so the UI can guide the user before submitting.
 */
object PasswordValidator {

    private const val REQ_LENGTH = "Almeno 8 caratteri"
    private const val REQ_UPPER = "Almeno una lettera maiuscola"
    private const val REQ_LOWER = "Almeno una lettera minuscola"
    private const val REQ_DIGIT = "Almeno un numero"
    private const val REQ_SPECIAL = "Almeno un carattere speciale (!@#\$%^&*(),.?\":{}|<>)"

    private const val SPECIALS = "!@#\$%^&*(),.?\":{}|<>"

    fun validatePassword(password: String): PasswordValidation {
        val missing = mutableListOf<String>()
        if (password.length < 8) missing.add("almeno 8 caratteri")
        if (password.none { it.isUpperCase() }) missing.add("almeno una lettera maiuscola")
        if (password.none { it.isLowerCase() }) missing.add("almeno una lettera minuscola")
        if (password.none { it.isDigit() }) missing.add("almeno un numero")
        if (password.none { it in SPECIALS }) missing.add("almeno un carattere speciale")

        val isValid = missing.isEmpty()
        val message = if (isValid) "" else formatRequirementsMessage(missing)
        return PasswordValidation(isValid, missing, message)
    }

    private fun formatRequirementsMessage(missing: List<String>): String = when (missing.size) {
        1 -> "La password deve avere ${missing[0]}."
        2 -> "La password deve avere ${missing[0]} e ${missing[1]}."
        else -> {
            val last = missing.last()
            val others = missing.dropLast(1).joinToString(", ")
            "La password deve avere $others, e $last."
        }
    }

    fun getPasswordRequirements(): List<String> =
        listOf(REQ_LENGTH, REQ_UPPER, REQ_LOWER, REQ_DIGIT, REQ_SPECIAL)

    fun checkRequirement(password: String, requirement: String): Boolean = when (requirement) {
        REQ_LENGTH -> password.length >= 8
        REQ_UPPER -> password.any { it.isUpperCase() }
        REQ_LOWER -> password.any { it.isLowerCase() }
        REQ_DIGIT -> password.any { it.isDigit() }
        REQ_SPECIAL -> password.any { it in SPECIALS }
        else -> false
    }
}
