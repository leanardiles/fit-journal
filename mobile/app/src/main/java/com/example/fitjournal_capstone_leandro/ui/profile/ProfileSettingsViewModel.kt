package com.example.fitjournal_capstone_leandro.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.UserProfileUpdate
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSettingsUiState {
    object Loading : ProfileSettingsUiState()
    object Success : ProfileSettingsUiState()
    object Saved : ProfileSettingsUiState()
    data class Error(val message: String) : ProfileSettingsUiState()
}

data class ProfileSettingsScreenState(
    val uiState: ProfileSettingsUiState = ProfileSettingsUiState.Loading,
    val email: String = "",
    val name: String = "",
    val sex: String? = null,
    val age: String = "",
    val unitPreference: String = "metric",
    val height: String = "",
    val weight: String = ""
)

class ProfileSettingsViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val apiService = RetrofitClient.apiService

    private val _state = MutableStateFlow(ProfileSettingsScreenState())
    val state: StateFlow<ProfileSettingsScreenState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = ProfileSettingsUiState.Loading)
            try {
                val userId = tokenManager.getUserId()
                val profile = apiService.getProfile(userId)
                _state.value = _state.value.copy(
                    uiState = ProfileSettingsUiState.Success,
                    email = profile.user_email,
                    name = profile.user_first_name ?: "",
                    sex = profile.user_sex,
                    age = profile.user_age?.toString() ?: "",
                    unitPreference = profile.user_unit_preference ?: "metric",
                    height = profile.user_height?.toString() ?: "",
                    weight = profile.user_weight?.toString() ?: ""
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uiState = ProfileSettingsUiState.Error(
                        e.message ?: "Failed to load profile"
                    )
                )
            }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateSex(sex: String) {
        _state.value = _state.value.copy(sex = sex)
    }

    fun updateAge(age: String) {
        _state.value = _state.value.copy(age = age)
    }

    fun updateUnitPreference(unit: String) {
        val current = _state.value.unitPreference
        if (unit == current) return

        val currentHeight = _state.value.height.toFloatOrNull()
        val currentWeight = _state.value.weight.toFloatOrNull()

        val (convertedHeight, convertedWeight) = if (unit == "imperial") {
            // metric → imperial
            val totalInches = currentHeight?.let { it / 2.54f }
            val feet = totalInches?.let { (it / 12).toInt() }
            val inches = totalInches?.let { (it % 12).toInt() }
            val heightDisplay = if (feet != null && inches != null) "$feet'$inches\"" else _state.value.height
            val weightLbs = currentWeight?.let { "%.1f".format(it * 2.20462f) } ?: _state.value.weight
            Pair(heightDisplay, weightLbs)
        } else {
            // imperial → metric: parse "5'11"" back to cm
            val heightStr = _state.value.height
            val feetInchesRegex = Regex("""(\d+)'(\d+)"""")
            val match = feetInchesRegex.find(heightStr)
            val heightCm = if (match != null) {
                val feet = match.groupValues[1].toInt()
                val inches = match.groupValues[2].toInt()
                "%.0f".format((feet * 12 + inches) * 2.54f)
            } else {
                currentHeight?.let { "%.0f".format(it * 30.48f) } ?: heightStr
            }
            val weightKg = currentWeight?.let { "%.1f".format(it / 2.20462f) } ?: _state.value.weight
            Pair(heightCm, weightKg)
        }

        _state.value = _state.value.copy(
            unitPreference = unit,
            height = convertedHeight,
            weight = convertedWeight
        )
    }

    fun updateHeight(height: String) {
        _state.value = _state.value.copy(height = height)
    }

    fun updateWeight(weight: String) {
        _state.value = _state.value.copy(weight = weight)
    }

    fun saveProfile() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                val s = _state.value

                val heightToSave = if (_state.value.unitPreference == "imperial") {
                    val match = Regex("""(\d+)'(\d+)"""").find(_state.value.height)
                    if (match != null) {
                        val feet = match.groupValues[1].toInt()
                        val inches = match.groupValues[2].toInt()
                        ((feet * 12 + inches) * 2.54f)
                    } else null
                } else {
                    _state.value.height.toFloatOrNull()
                }

                val weightToSave = if (_state.value.unitPreference == "imperial") {
                    _state.value.weight.toFloatOrNull()?.let { it / 2.20462f }
                } else {
                    _state.value.weight.toFloatOrNull()
                }

                val update = UserProfileUpdate(
                    user_first_name = s.name.ifBlank { null },
                    user_sex = s.sex,
                    user_age = s.age.toIntOrNull(),
                    user_unit_preference = s.unitPreference,
                    user_height = heightToSave,
                    user_weight = weightToSave
                )

                apiService.updateProfile(userId, update)
                _state.value = _state.value.copy(uiState = ProfileSettingsUiState.Saved)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uiState = ProfileSettingsUiState.Error(
                        e.message ?: "Failed to save profile"
                    )
                )
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(uiState = ProfileSettingsUiState.Success)
    }
}

class ProfileSettingsViewModelFactory(
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileSettingsViewModel::class.java)) {
            return ProfileSettingsViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}