package com.example.mycustomappstore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycustomappstore.ui.screen.AppListScreen
import com.example.mycustomappstore.ui.screen.PermissionScreen
import com.example.mycustomappstore.ui.theme.MyCustomAppStoreTheme
import com.example.mycustomappstore.util.PermissionUtils
import com.example.mycustomappstore.viewmodel.MainViewModel
import com.example.mycustomappstore.viewmodel.PermissionViewModel

class MainActivity : ComponentActivity() {

    private val permissionViewModel: PermissionViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyCustomAppStoreTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigator(permissionViewModel, mainViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionViewModel.refreshPermissionsState()
    }

    @Deprecated("Deprecated but used for ACTION_MANAGE_UNKNOWN_APP_SOURCES")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionUtils.UNKNOWN_SOURCES_REQUEST_CODE) {
            permissionViewModel.refreshPermissionsState()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            permissionViewModel.refreshPermissionsState()
        }
    }
}

@Composable
fun AppNavigator(permissionViewModel: PermissionViewModel, mainViewModel: MainViewModel) {
    val permissionUiState by permissionViewModel.uiState.collectAsStateWithLifecycle()

    when {
        permissionUiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        permissionUiState.showPermissionScreen && !permissionUiState.allPermissionsHandled -> {
            PermissionScreen(viewModel = permissionViewModel)
        }
        else -> {
            AppListScreen(viewModel = mainViewModel)
        }
    }
}
