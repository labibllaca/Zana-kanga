package com.labix

import androidx.compose.ui.geometry.Offset
import com.labix.navirom.ui.radial.RadialMath
import com.labix.navirom.ui.radial.RadialRingSpec
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RadialDialEngineTest {

    @Test
    fun testToPolarCardinalPoints() {
        val center = Offset(100f, 100f)

        // Point directly to the right (East -> 0 deg)
        val east = RadialMath.toPolar(center, Offset(200f, 100f))
        assertEquals(100f, east.radius, 0.01f)
        assertEquals(0f, east.angleDegrees, 0.01f)

        // Point directly below (South -> 90 deg in screen coords where Y goes down)
        val south = RadialMath.toPolar(center, Offset(100f, 200f))
        assertEquals(100f, south.radius, 0.01f)
        assertEquals(90f, south.angleDegrees, 0.01f)

        // Point directly to the left (West -> 180 deg)
        val west = RadialMath.toPolar(center, Offset(0f, 100f))
        assertEquals(100f, west.radius, 0.01f)
        assertEquals(180f, west.angleDegrees, 0.01f)

        // Point directly above (North -> 270 deg)
        val north = RadialMath.toPolar(center, Offset(100f, 0f))
        assertEquals(100f, north.radius, 0.01f)
        assertEquals(270f, north.angleDegrees, 0.01f)
    }

    @Test
    fun testToCartesianProjection() {
        val center = Offset(50f, 50f)
        val radius = 50f

        val pointEast = RadialMath.toCartesian(center, radius, 0f)
        assertEquals(100f, pointEast.x, 0.01f)
        assertEquals(50f, pointEast.y, 0.01f)

        val pointSouth = RadialMath.toCartesian(center, radius, 90f)
        assertEquals(50f, pointSouth.x, 0.01f)
        assertEquals(100f, pointSouth.y, 0.01f)
    }

    @Test
    fun testNormalizeDegrees() {
        assertEquals(45f, RadialMath.normalizeDegrees(45f), 0.001f)
        assertEquals(0f, RadialMath.normalizeDegrees(360f), 0.001f)
        assertEquals(350f, RadialMath.normalizeDegrees(-10f), 0.001f)
        assertEquals(90f, RadialMath.normalizeDegrees(450f), 0.001f)
    }

    @Test
    fun testAngularDistance() {
        // Small forward delta
        assertEquals(10f, RadialMath.angularDistance(0f, 10f), 0.01f)
        // Small backward delta
        assertEquals(-10f, RadialMath.angularDistance(10f, 0f), 0.01f)
        // Crossing the 0/360 boundary
        assertEquals(20f, RadialMath.angularDistance(350f, 10f), 0.01f)
        assertEquals(-20f, RadialMath.angularDistance(10f, 350f), 0.01f)
    }

    @Test
    fun testRingIndexAt() {
        val rings = listOf(
            RadialRingSpec(innerRadius = 0f, outerRadius = 50f, id = "core"),
            RadialRingSpec(innerRadius = 50f, outerRadius = 100f, id = "inner_lathe"),
            RadialRingSpec(innerRadius = 100f, outerRadius = 180f, id = "outer_bezel")
        )

        assertEquals(0, RadialMath.ringIndexAt(25f, rings))
        assertEquals(1, RadialMath.ringIndexAt(75f, rings))
        assertEquals(2, RadialMath.ringIndexAt(140f, rings))
        assertEquals(-1, RadialMath.ringIndexAt(200f, rings))
    }

    @Test
    fun testFindNearestDetent() {
        val detents = listOf(0f, 90f, 180f, 270f)

        assertEquals(0f, RadialMath.findNearestDetent(10f, detents) ?: -1f, 0.01f)
        assertEquals(0f, RadialMath.findNearestDetent(355f, detents) ?: -1f, 0.01f)
        assertEquals(90f, RadialMath.findNearestDetent(82f, detents) ?: -1f, 0.01f)
        assertEquals(180f, RadialMath.findNearestDetent(175f, detents) ?: -1f, 0.01f)
    }
}
