package com.labix.navirom.ui.radial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.labix.navirom.ui.util.NaviromHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Polar coordinate representation of a point relative to a center.
 */
data class PolarPoint(
    val radius: Float,
    val angleRadians: Float,
    val angleDegrees: Float
)

/**
 * Concentric ring specification for multi-ring hit testing.
 */
data class RadialRingSpec(
    val innerRadius: Float,
    val outerRadius: Float,
    val id: String = ""
)

/**
 * Math utilities for polar-to-cartesian projection and rotational calculations.
 */
object RadialMath {

    fun toPolar(center: Offset, point: Offset): PolarPoint {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val radius = hypot(dx, dy)
        val radians = atan2(dy, dx)
        var degrees = Math.toDegrees(radians.toDouble()).toFloat()
        if (degrees < 0f) degrees += 360f
        return PolarPoint(
            radius = radius,
            angleRadians = radians,
            angleDegrees = degrees
        )
    }

    fun toCartesian(center: Offset, radius: Float, angleDegrees: Float): Offset {
        val rad = Math.toRadians(angleDegrees.toDouble())
        return Offset(
            x = center.x + (radius * cos(rad)).toFloat(),
            y = center.y + (radius * sin(rad)).toFloat()
        )
    }

    fun normalizeDegrees(angle: Float): Float {
        val normalized = angle % 360f
        return if (normalized < 0f) normalized + 360f else normalized
    }

    /**
     * Calculates the shortest delta from [fromAngle] to [toAngle] in range [-180..180].
     */
    fun angularDistance(fromAngle: Float, toAngle: Float): Float {
        var diff = (toAngle - fromAngle) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    /**
     * Hit-tests radial distance against concentric ring boundaries.
     * Returns the index of the matching ring, or -1 if outside.
     */
    fun ringIndexAt(distance: Float, rings: List<RadialRingSpec>): Int {
        rings.forEachIndexed { index, ring ->
            if (distance in ring.innerRadius..ring.outerRadius) {
                return index
            }
        }
        return -1
    }

    /**
     * Finds the nearest detent angle to [currentAngle] from a list of [detents].
     */
    fun findNearestDetent(currentAngle: Float, detents: List<Float>): Float? {
        if (detents.isEmpty()) return null
        val norm = normalizeDegrees(currentAngle)
        return detents.minByOrNull { detent ->
            abs(angularDistance(norm, normalizeDegrees(detent)))
        }
    }
}

/**
 * Reactive state holding rotation, physics momentum, and detent snapping for a radial dial.
 */
class RadialDialState(
    initialAngle: Float = 0f,
    val stepSizeDegrees: Float = 0f,
    val isBounded: Boolean = false,
    val minAngle: Float = -180f,
    val maxAngle: Float = 180f
) {
    val animatableAngle = Animatable(initialAngle)
    val angle: Float get() = animatableAngle.value

    var isDragging by mutableStateOf(false)
        private set

    var angularVelocity by mutableFloatStateOf(0f)
        private set

    private var flingJob: Job? = null
    private var lastHapticDetentIndex = (initialAngle / if (stepSizeDegrees > 0f) stepSizeDegrees else 1f).toInt()

    fun setDraggingState(dragging: Boolean) {
        isDragging = dragging
        if (dragging) {
            flingJob?.cancel()
            flingJob = null
        }
    }

    fun updateAngularVelocity(velocity: Float) {
        angularVelocity = velocity
    }

    suspend fun snapTo(targetAngle: Float) {
        animatableAngle.snapTo(clampIfBounded(targetAngle))
    }

    suspend fun rotateBy(deltaDegrees: Float, haptics: NaviromHaptics? = null) {
        val newTarget = clampIfBounded(animatableAngle.value + deltaDegrees)
        animatableAngle.snapTo(newTarget)

        if (stepSizeDegrees > 0f && haptics != null) {
            val currentDetentIndex = (animatableAngle.value / stepSizeDegrees).toInt()
            if (currentDetentIndex != lastHapticDetentIndex) {
                haptics.tick()
                lastHapticDetentIndex = currentDetentIndex
            }
        }
    }

    fun flingWithDecay(
        scope: CoroutineScope,
        initialVelocity: Float,
        detents: List<Float> = emptyList(),
        haptics: NaviromHaptics? = null,
        onSettled: ((Float) -> Unit)? = null
    ) {
        flingJob?.cancel()
        flingJob = scope.launch {
            var currentVelocity = initialVelocity.coerceIn(-1200f, 1200f)
            val friction = 0.94f
            val minVelocity = 12f

            while (isActive && abs(currentVelocity) > minVelocity) {
                delay(16)
                val dt = 0.016f
                val delta = currentVelocity * dt
                rotateBy(delta, haptics)
                currentVelocity *= friction
            }

            // Snap to nearest detent if requested
            if (detents.isNotEmpty()) {
                val nearest = RadialMath.findNearestDetent(animatableAngle.value, detents)
                if (nearest != null) {
                    val shortestDist = RadialMath.angularDistance(animatableAngle.value, nearest)
                    haptics?.click()
                    animatableAngle.animateTo(
                        targetValue = animatableAngle.value + shortestDist,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            } else if (stepSizeDegrees > 0f) {
                val current = animatableAngle.value
                val snapped = round(current / stepSizeDegrees) * stepSizeDegrees
                haptics?.click()
                animatableAngle.animateTo(
                    targetValue = clampIfBounded(snapped),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            onSettled?.invoke(animatableAngle.value)
        }
    }

    private fun clampIfBounded(target: Float): Float {
        return if (isBounded) {
            target.coerceIn(minAngle, maxAngle)
        } else {
            target
        }
    }
}

@Composable
fun rememberRadialDialState(
    initialAngle: Float = 0f,
    stepSizeDegrees: Float = 0f,
    isBounded: Boolean = false,
    minAngle: Float = -180f,
    maxAngle: Float = 180f
): RadialDialState {
    return remember(initialAngle, stepSizeDegrees, isBounded, minAngle, maxAngle) {
        RadialDialState(
            initialAngle = initialAngle,
            stepSizeDegrees = stepSizeDegrees,
            isBounded = isBounded,
            minAngle = minAngle,
            maxAngle = maxAngle
        )
    }
}

/**
 * Modifier detecting angular pointer dragging around a center point with velocity calculation.
 */
fun Modifier.radialRotatable(
    state: RadialDialState,
    scope: CoroutineScope,
    centerProvider: () -> Offset,
    haptics: NaviromHaptics? = null,
    detents: List<Float> = emptyList(),
    onDragStart: (() -> Unit)? = null,
    onDragEnd: ((Float) -> Unit)? = null
): Modifier = this.pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val center = centerProvider()
        if (center == Offset.Zero) return@awaitEachGesture

        var lastAngle = RadialMath.toPolar(center, down.position).angleDegrees
        var lastTime = down.uptimeMillis
        var computedVelocity = 0f

        state.setDraggingState(true)
        onDragStart?.invoke()

        try {
            while (true) {
                val event = awaitPointerEvent()
                val change: PointerInputChange? = event.changes.firstOrNull { it.pressed }
                if (change == null) break

                val currentTime = change.uptimeMillis
                val currentPolar = RadialMath.toPolar(center, change.position)
                val currentAngle = currentPolar.angleDegrees
                val dtMs = (currentTime - lastTime).coerceAtLeast(1)

                val deltaAngle = RadialMath.angularDistance(lastAngle, currentAngle)
                computedVelocity = (deltaAngle / (dtMs / 1000f))

                if (abs(deltaAngle) > 0.05f) {
                    scope.launch {
                        state.rotateBy(deltaAngle, haptics)
                    }
                    change.consume()
                }

                lastAngle = currentAngle
                lastTime = currentTime
            }
        } finally {
            state.setDraggingState(false)
            state.updateAngularVelocity(computedVelocity)
            state.flingWithDecay(
                scope = scope,
                initialVelocity = computedVelocity,
                detents = detents,
                haptics = haptics,
                onSettled = onDragEnd
            )
        }
    }
}

/**
 * Common Astrolabe aesthetic drawing primitives for dials, star charts, and engraved bezels.
 */
object AstrolabeTheme {
    val DeepSpaceMidnight = Color(0xFF090D16)
    val StarCyan = Color(0xFF7FE5D9)
    val AstrolabeGold = Color(0xFFFFD166)
    val EngravedSlate = Color(0xFF2A344A)
    val ParchmentRim = Color(0xFFD4C5B0)
    val CelestialGrid = Color(0x337FE5D9)
    val ActivePointerGlow = Color(0xFFFFB703)
}

/**
 * Canvas drawing utility for drawing astrolabe degree ticks, bezel rings, and coordinate crosshairs.
 */
fun DrawScope.drawAstrolabeBezel(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    bezelColor: Color = AstrolabeTheme.EngravedSlate,
    tickColor: Color = AstrolabeTheme.ParchmentRim
) {
    // Outer and Inner Concentric Ring Borders
    drawCircle(
        color = bezelColor,
        radius = outerRadius,
        center = center,
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = bezelColor.copy(alpha = 0.5f),
        radius = innerRadius,
        center = center,
        style = Stroke(
            width = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    )

    // Fine Azimuth Ticks around the perimeter
    val tickCount = 72 // 5-degree increments
    for (i in 0 until tickCount) {
        val angleDeg = i * (360f / tickCount)
        val isMajor = (i % 6 == 0) // Every 30 degrees (Zodiac / Cardinal)
        val isMedium = (i % 2 == 0)

        val tickLength = if (isMajor) 14f else if (isMedium) 9f else 5f
        val tickStroke = if (isMajor) 2.2f else if (isMedium) 1.2f else 0.8f
        val color = if (isMajor) AstrolabeTheme.AstrolabeGold else tickColor.copy(alpha = if (isMedium) 0.8f else 0.45f)

        val start = RadialMath.toCartesian(center, outerRadius - tickLength, angleDeg)
        val end = RadialMath.toCartesian(center, outerRadius, angleDeg)
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Celestial starfield renderer with constellation coordinate grid and pulsating starlight.
 */
fun DrawScope.drawCelestialCore(
    center: Offset,
    radius: Float,
    pulseEnergy: Float = 1.0f,
    rotationDeg: Float = 0f,
    isPlaying: Boolean = true
) {
    // Midnight circular aperture
    drawCircle(
        color = AstrolabeTheme.DeepSpaceMidnight,
        radius = radius,
        center = center
    )

    // Cardinal coordinate crosshairs & equatorial celestial ring
    drawCircle(
        color = AstrolabeTheme.CelestialGrid,
        radius = radius * 0.65f,
        center = center,
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f))
    )

    // Concentric celestial coordinates
    drawCircle(
        color = AstrolabeTheme.CelestialGrid.copy(alpha = 0.25f),
        radius = radius * 0.35f,
        center = center,
        style = Stroke(width = 0.8f)
    )

    // Axis lines (North-South, East-West)
    val cardinalAngles = floatArrayOf(0f, 90f, 180f, 270f)
    for (ang in cardinalAngles) {
        val radAng = ang + rotationDeg
        val p1 = RadialMath.toCartesian(center, radius * 0.15f, radAng)
        val p2 = RadialMath.toCartesian(center, radius * 0.95f, radAng)
        drawLine(
            color = AstrolabeTheme.CelestialGrid.copy(alpha = 0.35f),
            start = p1,
            end = p2,
            strokeWidth = 1f
        )
    }

    // Procedural Stars and Constellations
    val starPositions = listOf(
        Pair(0.25f, 45f),
        Pair(0.48f, 72f),
        Pair(0.55f, 120f),
        Pair(0.38f, 160f),
        Pair(0.70f, 215f),
        Pair(0.82f, 240f),
        Pair(0.42f, 290f),
        Pair(0.68f, 330f),
        Pair(0.85f, 25f),
        Pair(0.30f, 220f)
    )

    val points = starPositions.map { (distFactor, angle) ->
        RadialMath.toCartesian(center, radius * distFactor, angle + rotationDeg)
    }

    // Connect constellation lines between stars
    val constellationLinks = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(4, 5), Pair(6, 7), Pair(0, 8)
    )
    constellationLinks.forEach { (i1, i2) ->
        if (i1 < points.size && i2 < points.size) {
            drawLine(
                color = AstrolabeTheme.StarCyan.copy(alpha = 0.35f * pulseEnergy),
                start = points[i1],
                end = points[i2],
                strokeWidth = 1f
            )
        }
    }

    // Draw glowing stars
    points.forEachIndexed { index, pt ->
        val isMajorStar = index % 3 == 0
        val baseR = if (isMajorStar) 3.5f else 2.0f
        val animatedR = baseR * (if (isPlaying) pulseEnergy else 1.0f)
        val color = if (isMajorStar) AstrolabeTheme.AstrolabeGold else AstrolabeTheme.StarCyan

        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = animatedR * 2.2f,
            center = pt
        )
        drawCircle(
            color = color,
            radius = animatedR,
            center = pt
        )
    }
}
