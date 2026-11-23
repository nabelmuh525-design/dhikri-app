package com.dhikri.islamic.domain.util

import kotlin.math.*

/**
 * Pure, testable mathematical utilities for Qibla direction, distance to Kaaba,
 * magnetic declination correction, and circular low-pass compass smoothing.
 */
object QiblaMath {
    // Verified WGS-84 coordinates of the Holy Kaaba, Masjid al-Haram, Makkah
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Great-circle initial bearing from (latitude, longitude) to the Holy Kaaba in degrees [0, 360).
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * Great-circle Haversine distance in kilometers from (latitude, longitude) to the Holy Kaaba.
     */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2.0 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * Converts magnetic azimuth to true geographic azimuth using magnetic declination (degrees East positive).
     */
    fun applyMagneticDeclination(magneticAzimuthDeg: Float, declinationDeg: Float): Float {
        return ((magneticAzimuthDeg + declinationDeg) % 360f + 360f) % 360f
    }

    /**
     * Shortest signed angular delta in [-180, +180].
     */
    fun shortestAngleDelta(targetDeg: Float, currentDeg: Float): Float {
        return ((targetDeg - currentDeg + 540f) % 360f) - 180f
    }

    /**
     * Circular low-pass filter to eliminate compass needle jitter across the 0°/360° wraparound.
     */
    fun smoothAzimuth(previousDeg: Float, newDeg: Float, alpha: Float = 0.18f): Float {
        val delta = shortestAngleDelta(newDeg, previousDeg)
        return ((previousDeg + alpha * delta) % 360f + 360f) % 360f
    }
}
