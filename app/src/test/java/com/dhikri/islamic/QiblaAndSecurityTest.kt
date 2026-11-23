package com.dhikri.islamic

import com.dhikri.islamic.data.update.AppUpdater
import com.dhikri.islamic.domain.util.QiblaMath
import org.junit.Assert.*
import org.junit.Test

class QiblaAndSecurityTest {

    @Test
    fun qiblaBearing_knownCities_matchesSphericalGeodesy() {
        // Amman -> ~160.9° (SSE)
        val amman = QiblaMath.calculateQiblaBearing(31.9539, 35.9106)
        assertEquals(160.9, amman, 1.5)

        // Cairo -> ~136.1° (SE)
        val cairo = QiblaMath.calculateQiblaBearing(30.0444, 31.2357)
        assertEquals(136.1, cairo, 1.5)

        // London -> ~119.0° (ESE)
        val london = QiblaMath.calculateQiblaBearing(51.5074, -0.1278)
        assertEquals(119.0, london, 1.5)

        // New York -> ~58.5° (ENE)
        val ny = QiblaMath.calculateQiblaBearing(40.7128, -74.0060)
        assertEquals(58.5, ny, 1.5)

        // Tokyo -> ~293.0° (WNW)
        val tokyo = QiblaMath.calculateQiblaBearing(35.6762, 139.6503)
        assertEquals(293.0, tokyo, 1.5)
    }

    @Test
    fun qiblaSmoothing_wrapsCleanlyAcrossNorthZero() {
        val smoothed = QiblaMath.smoothAzimuth(previousDeg = 359f, newDeg = 1f, alpha = 0.5f)
        assertEquals(0f, smoothed, 0.5f)
    }

    @Test
    fun magneticDeclination_wrapsWithin360() {
        assertEquals(4.5f, QiblaMath.applyMagneticDeclination(359.5f, 5.0f), 0.01f)
        assertEquals(357.0f, QiblaMath.applyMagneticDeclination(2.0f, -5.0f), 0.01f)
    }

    @Test
    fun appUpdater_blocksUntrustedAndHttpUrls() {
        assertTrue(AppUpdater.isTrustedHttpsUrl("https://github.com/nabelmuh525-design/dhikri-app/releases/download/v4.0.0/DHIKRI-v4.0.0.apk"))
        assertFalse(AppUpdater.isTrustedHttpsUrl("http://github.com/nabelmuh525-design/dhikri-app/releases/download/v4.0.0/DHIKRI-v4.0.0.apk"))
        assertFalse(AppUpdater.isTrustedHttpsUrl("https://evil.example.com/fake.apk"))
        assertTrue(AppUpdater.isNewerVersion("4.0.0", "3.8.4"))
        assertFalse(AppUpdater.isNewerVersion("3.8.4", "4.0.0"))
        assertFalse(AppUpdater.isNewerVersion("4.0.0", "4.0.0"))
    }
}
