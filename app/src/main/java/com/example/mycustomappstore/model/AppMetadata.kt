package com.example.mycustomappstore.model

import kotlinx.serialization.Serializable

@Serializable
data class AppMetadata(
    val packageName: String,
    val name: String,
    val version: String,
    val description: String,
    val iconUrl: String,
    val apkUrl: String,
    val sizeMb: Double
)
