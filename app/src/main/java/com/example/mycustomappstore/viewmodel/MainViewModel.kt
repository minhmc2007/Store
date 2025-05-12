package com.example.mycustomappstore.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycustomappstore.data.AppRepository
import com.example.mycustomappstore.model.AppMetadata
import com.example.mycustomappstore.util.DownloadHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<AppMetadata> = emptyList(),
    val errorMessage: String? = null,
    val downloadProgress: Map<String, Int> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val downloadHelper = DownloadHelper(application.applicationContext)

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadAppList()
    }

    fun loadAppList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAppList()
                .onSuccess { apps ->
                    _uiState.update { it.copy(isLoading = false, apps = apps) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Failed to load apps: ${error.localizedMessage}")
                    }
                }
        }
    }

    fun startDownload(app: AppMetadata) {
        if (downloadHelper.isDownloading(app.packageName) || _uiState.value.downloadProgress.containsKey(app.packageName)) {
            Toast.makeText(getApplication(), "${app.name} download is already active.", Toast.LENGTH_SHORT).show()
            return
        }
        _uiState.update { it.copy(downloadProgress = it.downloadProgress + (app.packageName to 0)) }

        downloadHelper.downloadAndInstallApk(
            app = app, scope = viewModelScope,
            onProgress = { progress ->
                _uiState.update {
                    it.copy(downloadProgress = it.downloadProgress + (app.packageName to progress))
                }
            },
            onComplete = { apkFile ->
                _uiState.update { it.copy(downloadProgress = it.downloadProgress - app.packageName) }
                apkFile?.let { downloadHelper.installApk(it) }
                    ?: Toast.makeText(getApplication(), "Download issue for ${app.name}.", Toast.LENGTH_LONG).show()
            },
            onError = { errorMessage ->
                _uiState.update { it.copy(downloadProgress = it.downloadProgress - app.packageName) }
                Toast.makeText(getApplication(), "Download failed for ${app.name}: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun cancelDownload(app: AppMetadata) {
        downloadHelper.cancelDownload(app.packageName)
        _uiState.update { it.copy(downloadProgress = it.downloadProgress - app.packageName) }
        Toast.makeText(getApplication(), "Download cancelled for ${app.name}", Toast.LENGTH_SHORT).show()
    }

    fun isAppDownloading(packageName: String): Boolean {
        return downloadHelper.isDownloading(packageName) || _uiState.value.downloadProgress.containsKey(packageName)
    }
}
