package com.example.scanner

import kotlin.random.Random

object IpUtils {
    // Convert IP string (e.g. 192.168.1.1) to long
    fun ipToLong(ipAddress: String): Long {
        val parts = ipAddress.split(".")
        if (parts.size != 4) return 0L
        var result: Long = 0L
        for (i in 0..3) {
            val part = parts[i].toLongOrNull() ?: return 0L
            result = result or (part shl (24 - i * 8))
        }
        return result
    }

    // Convert long to IP string
    fun longToIp(ip: Long): String {
        return ((ip shr 24) and 0xFF).toString() + "." +
                ((ip shr 16) and 0xFF).toString() + "." +
                ((ip shr 8) and 0xFF).toString() + "." +
                (ip and 0xFF).toString()
    }

    // Generate IPs from CIDR up to a max count, with randomized sampling to mix blocks
    fun parseCidr(cidr: String, maxCount: Int = 100): List<String> {
        val trimmed = cidr.trim()
        if (!trimmed.contains("/")) {
            // Direct IP
            return listOf(trimmed)
        }
        val parts = trimmed.split("/")
        if (parts.isEmpty()) return emptyList()
        val ipStr = parts[0]
        val maskStr = if (parts.size > 1) parts[1] else "32"
        val mask = maskStr.toIntOrNull() ?: 32

        val ipLong = ipToLong(ipStr)
        if (ipLong == 0L && ipStr != "0.0.0.0") return emptyList()

        val hostBits = 32 - mask
        val numberOfHosts = if (hostBits >= 31) 2L else (1L shl hostBits)

        if (numberOfHosts <= 1) {
            return listOf(ipStr)
        }

        val netmask = if (mask == 0) 0L else (0xFFFFFFFFL shl hostBits) and 0xFFFFFFFFL
        val networkLong = ipLong and netmask

        val results = mutableListOf<String>()
        if (numberOfHosts - 2 <= maxCount) {
            // Include all available IPs in this range
            val limit = (numberOfHosts - 2).toInt()
            for (i in 1..limit) {
                results.add(longToIp(networkLong + i))
            }
        } else {
            // Uniformly sample throughout the subnet
            val step = (numberOfHosts - 2) / maxCount
            val usedSet = mutableSetOf<Long>()
            val rand = Random(System.currentTimeMillis())
            for (i in 0 until maxCount) {
                val randOffset = if (step > 1) rand.nextLong(step) else 0L
                val offset = 1L + (i * step) + randOffset
                if (offset < numberOfHosts - 1) {
                    val sampledIp = networkLong + offset
                    if (usedSet.add(sampledIp)) {
                        results.add(longToIp(sampledIp))
                    }
                }
            }
        }
        return results
    }
}
