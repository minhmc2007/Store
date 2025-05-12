package com.example.mycustomappstore.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycustomappstore.viewmodel.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(viewModel: PermissionViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions Required") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                "This app needs a few permissions to work correctly. Please grant them to continue.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PermissionItem(
                title = "Install Unknown Apps",
                description = "Required to install downloaded APKs from this app.",
                isGranted = uiState.installPermissionGranted,
                onRequest = { viewModel.requestInstallPermission(activity) }
            )

            PermissionItem(
                title = "Ignore Battery Optimizations",
                description = "Helps ensure downloads complete reliably in the background.",
                isGranted = uiState.batteryOptimizationIgnored,
                onRequest = { viewModel.requestBatteryOptimization(activity) }
            )

            PermissionItem(
                title = "Show Notifications",
                description = "To show download progress and completion.",
                isGranted = uiState.notificationPermissionGranted,
                onRequest = { viewModel.requestNotificationPermission(activity) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.markPermissionsScreenDoneIfAllGranted()
                },
                enabled = uiState.installPermissionGranted &&
                          uiState.batteryOptimizationIgnored &&
                          uiState.notificationPermissionGranted,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Continue to App")
            }
             if (!(uiState.installPermissionGranted &&
                        uiState.batteryOptimizationIgnored &&
                        uiState.notificationPermissionGranted)) {
                Text(
                    "Please grant all permissions to proceed.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onRequest, enabled = !isGranted) {
                Text(if (isGranted) "Granted" else "Grant")
            }
        }
    }
}
