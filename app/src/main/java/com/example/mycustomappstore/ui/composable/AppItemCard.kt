package com.example.mycustomappstore.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
// import com.example.mycustomappstore.R // Will be generated
import com.example.mycustomappstore.model.AppMetadata

@Composable
fun AppItemCard(
    app: AppMetadata,
    downloadProgress: Int?,
    isCurrentlyDownloadingThisApp: Boolean,
    onDownloadClick: (AppMetadata) -> Unit,
    onCancelClick: (AppMetadata) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconUrl).crossfade(true)
                    // .placeholder(R.drawable.ic_launcher_background) // Add placeholder
                    // .error(R.drawable.ic_launcher_foreground)       // Add error
                    .build(),
                contentDescription = "${app.name} icon",
                modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Version: ${app.version} • Size: ${app.sizeMb} MB", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isCurrentlyDownloadingThisApp && downloadProgress != null && downloadProgress >= 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                    Text("$downloadProgress%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                } else {
                    Text(app.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isCurrentlyDownloadingThisApp) {
                IconButton(onClick = { onCancelClick(app) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel Download", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(onClick = { onDownloadClick(app) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Filled.Download, contentDescription = "Download")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Install")
                }
            }
        }
    }
}
