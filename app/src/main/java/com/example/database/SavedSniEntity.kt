package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_snis")
data class SavedSni(
    @PrimaryKey val host: String,
    val isWorking: Boolean,
    val latencyMs: Long,
    val isCustom: Boolean,
    val note: String = "",
    val lastTestedAt: Long = System.currentTimeMillis()
)
