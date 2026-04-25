package com.example.einkaufsliste.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

data class HouseholdState(
    val code: String,
    val name: String
)

class HouseholdStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "household_preferences",
        Context.MODE_PRIVATE
    )

    private val _household = MutableStateFlow(loadHousehold())
    val household: StateFlow<HouseholdState> = _household

    val current: HouseholdState
        get() = _household.value

    fun joinHousehold(rawCode: String, name: String? = null): Boolean {
        val code = normalizeHouseholdCode(rawCode)
        if (!isValidHouseholdCode(code)) return false

        saveHousehold(
            HouseholdState(
                code = code,
                name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Gemeinsame Liste"
            )
        )
        return true
    }

    fun createNewHousehold() {
        saveHousehold(
            HouseholdState(
                code = generateHouseholdCode(),
                name = "Gemeinsame Liste"
            )
        )
    }

    fun updateName(name: String) {
        saveHousehold(
            _household.value.copy(
                name = name.trim().ifBlank { "Gemeinsame Liste" }
            )
        )
    }

    private fun loadHousehold(): HouseholdState {
        val code = preferences.getString(KEY_CODE, null)
            ?.let(::normalizeHouseholdCode)
            ?.takeIf(::isValidHouseholdCode)
            ?: generateHouseholdCode()
        val name = preferences.getString(KEY_NAME, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Gemeinsame Liste"

        val household = HouseholdState(code = code, name = name)
        persist(household)
        return household
    }

    private fun saveHousehold(household: HouseholdState) {
        persist(household)
        _household.value = household
    }

    private fun persist(household: HouseholdState) {
        preferences.edit()
            .putString(KEY_CODE, household.code)
            .putString(KEY_NAME, household.name)
            .apply()
    }

    companion object {
        private const val KEY_CODE = "household_code"
        private const val KEY_NAME = "household_name"
        private const val CODE_LENGTH = 6
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun normalizeHouseholdCode(input: String): String =
            input.uppercase().filter { it.isLetterOrDigit() }

        fun isValidHouseholdCode(code: String): Boolean =
            code.length in 4..12 && code.all { it.isLetterOrDigit() }

        private fun generateHouseholdCode(): String =
            buildString {
                repeat(CODE_LENGTH) {
                    append(CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)])
                }
            }
    }
}
