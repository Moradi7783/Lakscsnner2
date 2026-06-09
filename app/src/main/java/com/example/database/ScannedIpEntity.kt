package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_ips")
data class ScannedIp(
    @PrimaryKey val ip: String,
    val port: Int,
    val latencyMs: Long,
    val downloadSpeedKbps: Double,
    val provider: String,
    val connectionType: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
