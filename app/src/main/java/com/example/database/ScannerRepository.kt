package com.example.database

import kotlinx.coroutines.flow.Flow

class ScannerRepository(private val dao: ScannerDao) {
    val allScannedIps: Flow<List<ScannedIp>> = dao.getAllScannedIps()
    val favoriteIps: Flow<List<ScannedIp>> = dao.getFavoriteIps()
    val allSnis: Flow<List<SavedSni>> = dao.getAllSnis()

    suspend fun insertIp(ip: ScannedIp) {
        dao.insertIp(ip)
    }

    suspend fun insertIps(ips: List<ScannedIp>) {
        dao.insertIps(ips)
    }

    suspend fun toggleFavorite(ip: String, isFav: Boolean) {
        dao.toggleFavoriteIp(ip, isFav)
    }

    suspend fun deleteIp(ip: String) {
        dao.deleteIp(ip)
    }

    suspend fun clearAllIps() {
        dao.clearAllIps()
    }

    suspend fun insertSni(sni: SavedSni) {
        dao.insertSni(sni)
    }

    suspend fun insertSnis(snis: List<SavedSni>) {
        dao.insertSnis(snis)
    }

    suspend fun deleteSni(host: String) {
        dao.deleteSni(host)
    }

    suspend fun clearAllSnis() {
        dao.clearAllSnis()
    }
}
