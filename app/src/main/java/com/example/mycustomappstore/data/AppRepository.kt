package com.example.mycustomappstore.data

import android.content.Context
import android.util.Log
import com.example.mycustomappstore.model.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class AppRepository(private val context: Context) {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getAppList(): Result<List<AppMetadata>> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("apps.json").bufferedReader().use { it.readText() }
                val apps = jsonParser.decodeFromString<List<AppMetadata>>(jsonString)
                Result.success(apps)
            } catch (e: IOException) {
                Log.e("AppRepository", "Error reading apps.json from assets", e)
                Result.failure(e)
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e("AppRepository", "Error parsing apps.json", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("AppRepository", "Unexpected error loading app list", e)
                Result.failure(e)
            }
        }
    }
}
