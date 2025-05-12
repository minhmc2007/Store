package com.example.mycustomappstore.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycustomappstore.ui.composable.AppItemCard
import com.example.mycustomappstore.viewmodel.MainViewModel
import com.example.mycustomappstore.viewmodel.AppListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Custom App Store") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadAppList() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh App List")
                    }
                }
            )
        }
    ) { paddingValues ->
        AppListContent(
            uiState = uiState,
            onDownloadClick = { app -> viewModel.startDownload(app) },
            onCancelClick = { app -> viewModel.cancelDownload(app) },
            isAppDownloading = { packageName -> viewModel.isAppDownloading(packageName) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun AppListContent(
    uiState: AppListUiState,
    onDownloadClick: (com.example.mycustomappstore.model.AppMetadata) -> Unit,
    onCancelClick: (com.example.mycustomappstore.model.AppMetadata) -> Unit,
    isAppDownloading: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.apps.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.errorMessage != null -> {
                Text(text = "Error: ${uiState.errorMessage}\nTap refresh.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            }
            uiState.apps.isEmpty() && !uiState.isLoading -> {
                 Text("No apps found. Check 'apps.json' and refresh.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.apps, key = { it.packageName }) { app ->
                        AppItemCard(
                            app = app,
                            downloadProgress = uiState.downloadProgress[app.packageName],
                            isCurrentlyDownloadingThisApp = isAppDownloading(app.packageName),
                            onDownloadClick = onDownloadClick,
                            onCancelClick = onCancelClick
                        )
                    }
                }
            }
        }
    }
}
