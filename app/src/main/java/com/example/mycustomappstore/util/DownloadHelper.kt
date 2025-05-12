package com.example.mycustomappstore.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.mycustomappstore.MainActivity
import com.example.mycustomappstore.model.AppMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class DownloadHelper(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder().build()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloadJobs = mutableMapOf<String, Job>()

    companion object {
        private const val TAG = "DownloadHelper"
        private const val NOTIFICATION_CHANNEL_ID = "apk_download_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "APK Downloads"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for APK download progress"
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun isDownloading(packageName: String): Boolean {
        return downloadJobs[packageName]?.isActive == true
    }

    fun downloadAndInstallApk(
        app: AppMetadata,
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
        onComplete: (File?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isDownloading(app.packageName)) {
            Log.d(TAG, "Download already in progress for ${app.name}")
            return
        }

        if (!PermissionUtils.hasInstallPermission(context)) {
            val errorMsg = "Install permission not granted."
            Log.e(TAG, errorMsg)
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            onError(errorMsg)
            return
        }

        val notificationId = app.packageName.hashCode()

        val job = scope.launch(Dispatchers.IO) {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir == null || (!downloadDir.exists() && !downloadDir.mkdirs())) {
                 val errorMsg = "Cannot access or create download directory."
                Log.e(TAG, errorMsg)
                withContext(Dispatchers.Main) { onError(errorMsg) }
                return@launch
            }
            val sanitizedFileName = "${app.packageName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}_${app.version.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.apk"
            val apkFile = File(downloadDir, sanitizedFileName)

            val request = Request.Builder().url(app.apkUrl).build()
            var notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Downloading ${app.name}")
                .setContentText("Starting download...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, 0, true)

            if (PermissionUtils.hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notificationManager.notify(notificationId, notificationBuilder.build())
            }

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Download failed: ${response.code} ${response.message}")
                    val body = response.body ?: throw IOException("Empty response body")
                    val totalBytes = body.contentLength()
                    var bytesCopied: Long = 0

                    apkFile.outputStream().use { outputStream ->
                        body.source().use { source ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            var lastProgressUpdateTime = 0L

                            while (source.read(buffer).also { read = it } != -1 && isActive) {
                                outputStream.write(buffer, 0, read)
                                bytesCopied += read
                                val progress = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
                                if (isActive) {
                                    withContext(Dispatchers.Main) { onProgress(progress.coerceIn(0,100)) }
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastProgressUpdateTime > 500L || progress == 100) {
                                        lastProgressUpdateTime = currentTime
                                        notificationBuilder = notificationBuilder
                                            .setContentText("$progress% complete")
                                            .setProgress(100, progress.coerceIn(0,100), totalBytes <= 0)
                                        if (PermissionUtils.hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                            notificationManager.notify(notificationId, notificationBuilder.build())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isActive) {
                        val installPendingIntent = PendingIntent.getActivity(
                            context, notificationId, createInstallIntent(apkFile),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        notificationBuilder = notificationBuilder
                            .setContentTitle("${app.name} Download Complete")
                            .setContentText("Tap to install.")
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setOngoing(false).setAutoCancel(true).setProgress(0,0,false)
                            .setContentIntent(installPendingIntent)
                        if (PermissionUtils.hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                           notificationManager.notify(notificationId, notificationBuilder.build())
                        }
                        withContext(Dispatchers.Main) { onComplete(apkFile) }
                    } else {
                        apkFile.delete()
                        notificationManager.cancel(notificationId)
                        withContext(Dispatchers.Main) { onError("Download cancelled for ${app.name}") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during download for ${app.name}", e)
                withContext(Dispatchers.Main) { onError("Error for ${app.name}: ${e.localizedMessage}") }
                notificationManager.cancel(notificationId)
                apkFile.delete()
            } finally {
                downloadJobs.remove(app.packageName)
            }
        }
        downloadJobs[app.packageName] = job
        job.invokeOnCompletion { downloadJobs.remove(app.packageName) }
    }

    private fun createInstallIntent(apkFile: File): Intent {
        val apkUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun installApk(apkFile: File) {
        try {
            context.startActivity(createInstallIntent(apkFile))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Could not find an application to install the APK.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error installing APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun cancelDownload(packageName: String) {
        downloadJobs[packageName]?.cancel()
        Log.d(TAG, "Attempted to cancel download for $packageName")
    }
}
