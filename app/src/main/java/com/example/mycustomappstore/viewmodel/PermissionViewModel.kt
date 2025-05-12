package com.example.mycustomappstore.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycustomappstore.data.UserPreferencesRepository
import com.example.mycustomappstore.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PermissionViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val userPreferencesRepository = UserPreferencesRepository(context)

    private val _uiState = MutableStateFlow(PermissionScreenState())
    val uiState: StateFlow<PermissionScreenState> = _uiState.asStateFlow()

    data class PermissionScreenState(
        val isLoading: Boolean = true,
        val showPermissionScreen: Boolean = false,
        val installPermissionGranted: Boolean = false,
        val batteryOptimizationIgnored: Boolean = false,
        val notificationPermissionGranted: Boolean = false,
        val allPermissionsHandled: Boolean = false
    )

    init {
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        viewModelScope.launch {
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch.first()
            val installGranted = PermissionUtils.hasInstallPermission(context)
            val batteryOptIgnored = PermissionUtils.isBatteryOptimizationIgnored(context)
            val notificationGranted = PermissionUtils.hasNotificationPermission(context)

            val allRequiredGranted = installGranted && batteryOptIgnored && notificationGranted

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showPermissionScreen = isFirstLaunch && !allRequiredGranted,
                    installPermissionGranted = installGranted,
                    batteryOptimizationIgnored = batteryOptIgnored,
                    notificationPermissionGranted = notificationGranted,
                    allPermissionsHandled = !isFirstLaunch || allRequiredGranted
                )
            }
        }
    }

    fun refreshPermissionsState() {
        val installGranted = PermissionUtils.hasInstallPermission(context)
        val batteryOptIgnored = PermissionUtils.isBatteryOptimizationIgnored(context)
        val notificationGranted = PermissionUtils.hasNotificationPermission(context)
        val allRequiredGranted = installGranted && batteryOptIgnored && notificationGranted

        viewModelScope.launch {
            val currentIsFirstLaunch = userPreferencesRepository.isFirstLaunch.first()
            _uiState.update {
                it.copy(
                    installPermissionGranted = installGranted,
                    batteryOptimizationIgnored = batteryOptIgnored,
                    notificationPermissionGranted = notificationGranted,
                    allPermissionsHandled = !currentIsFirstLaunch || allRequiredGranted,
                    showPermissionScreen = currentIsFirstLaunch && !allRequiredGranted
                )
            }
            if (allRequiredGranted && currentIsFirstLaunch) {
                userPreferencesRepository.setFirstLaunchCompleted()
                 _uiState.update { it.copy(showPermissionScreen = false, allPermissionsHandled = true) }
            }
        }
    }

    fun requestInstallPermission(activity: Activity) {
        PermissionUtils.requestInstallPermission(activity)
    }

    fun requestBatteryOptimization(activity: Activity) {
        PermissionUtils.requestIgnoreBatteryOptimization(activity)
    }

    fun requestNotificationPermission(activity: Activity) {
        PermissionUtils.requestNotificationPermission(activity)
    }

    fun markPermissionsScreenDoneIfAllGranted() {
        viewModelScope.launch {
            if (_uiState.value.installPermissionGranted &&
                _uiState.value.batteryOptimizationIgnored &&
                _uiState.value.notificationPermissionGranted
            ) {
                userPreferencesRepository.setFirstLaunchCompleted()
                _uiState.update { it.copy(showPermissionScreen = false, allPermissionsHandled = true) }
            }
        }
    }
}
