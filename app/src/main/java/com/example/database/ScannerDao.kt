package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannerDao {
    // Scanned IPs
    @Query("SELECT * FROM scanned_ips ORDER BY latencyMs ASC")
    fun getAllScannedIps(): Flow<List<ScannedIp>>

    @Query("SELECT * FROM scanned_ips WHERE isFavorite = 1 ORDER BY latencyMs ASC")
    fun getFavoriteIps(): Flow<List<ScannedIp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIp(ip: ScannedIp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIps(ips: List<ScannedIp>)

    @Query("UPDATE scanned_ips SET isFavorite = :isFav WHERE ip = :ip")
    suspend fun toggleFavoriteIp(ip: String, isFav: Boolean)

    @Query("DELETE FROM scanned_ips WHERE ip = :ip")
    suspend fun deleteIp(ip: String)

    @Query("DELETE FROM scanned_ips")
    suspend fun clearAllIps()

    // Saved SNIs
    @Query("SELECT * FROM saved_snis ORDER BY isWorking DESC, latencyMs ASC")
    fun getAllSnis(): Flow<List<SavedSni>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSni(sni: SavedSni)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnis(snis: List<SavedSni>)

    @Query("DELETE FROM saved_snis WHERE host = :host")
    suspend fun deleteSni(host: String)

    @Query("DELETE FROM saved_snis")
    suspend fun clearAllSnis()
}
