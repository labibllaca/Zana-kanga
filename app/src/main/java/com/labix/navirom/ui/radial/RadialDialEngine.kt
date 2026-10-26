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
 * Configured in stark high-contrast monochrome (pure white background, pure black lines, borders, and accents).
 */
object AstrolabeTheme {
    val DeepSpaceMidnight = Color(0xFFFFFFFF) // Pure white background
    val StarCyan = Color(0xFF000000) // Stark black
    val AstrolabeGold = Color(0xFF000000) // Stark black
    val EngravedSlate = Color(0xFFFFFFFF) // Pure white surface
    val ParchmentRim = Color(0xFF000000) // Stark black text and icons
    val CelestialGrid = Color(0x33000000) // Hairline black grid
    val ActivePointerGlow = Color(0xFF000000) // Stark black pointer

    // Explicit monochrome colors
    val BackgroundWhite = Color(0xFFFFFFFF)
    val TextBlack = Color(0xFF000000)
    val TextDarkGray = Color(0xFF333333)
    val PureBlack = Color(0xFF000000)
    val BorderBlack = Color(0xFF000000)
    val ThickArcBlack = Color(0xFF000000)
}

/**
 * Editorial Concentric Music Wheel inspired by architectural planisphere calendars.
 * Pure white background, pure black borders and tick marks, zero text inside the wheel rings.
 *
 * Ring Hierarchy:
 * 1. Most Inner Circle: Track playback ring with fine vinyl grooves & progress arc.
 * 2. Middle Circle: Album ring with bold black curved divider arcs & album track division ticks.
 * 3. Outer Circle: Available Lists ring with prominent segmented thick black arcs & fine azimuth ticks.
 */
fun DrawScope.drawEditorialConcentricWheel(
    center: Offset,
    outerRadius: Float,
    progressFraction: Float,
    trackCount: Int = 12,
    currentTrackIndex: Int = 0,
    listCount: Int = 5,
    activeListIndex: Int = 0,
    rotationDeg: Float = 0f
) {
    val outerR = outerRadius * 0.76f
    val middleR = outerR * 0.70f
    val innerR = outerR * 0.42f
    val coreR = outerR * 0.22f

    // Background: Pure White Canvas
    drawCircle(
        color = Color.White,
        radius = outerRadius,
        center = center
    )

    // ==========================================
    // 4. AROUND THE WHEEL: STEPPED BOLD ARCS, SHELF RAYS & CONSTELLATION
    // (Directly matching zoom-kal.jpg's outer radial geometry)
    // ==========================================
    val arc1R = outerR * 1.08f
    val arc2R = outerR * 1.18f
    val arc3R = outerR * 1.28f

    // Arc segment 1 (e.g. September sector bracket)
    drawArc(
        color = Color.Black,
        startAngle = rotationDeg - 85f,
        sweepAngle = 72f,
        useCenter = false,
        topLeft = Offset(center.x - arc1R, center.y - arc1R),
        size = androidx.compose.ui.geometry.Size(arc1R * 2f, arc1R * 2f),
        style = Stroke(width = 5.0f, cap = StrokeCap.Square)
    )
    // Step line from Arc 1 to Arc 2
    val step1Angle = rotationDeg - 13f
    val step1Start = RadialMath.toCartesian(center, arc1R, step1Angle)
    val step1End = RadialMath.toCartesian(center, arc2R, step1Angle)
    drawLine(Color.Black, step1Start, step1End, strokeWidth = 4.0f)

    // Arc segment 2 (e.g. October sector bracket)
    drawArc(
        color = Color.Black,
        startAngle = rotationDeg - 13f,
        sweepAngle = 76f,
        useCenter = false,
        topLeft = Offset(center.x - arc2R, center.y - arc2R),
        size = androidx.compose.ui.geometry.Size(arc2R * 2f, arc2R * 2f),
        style = Stroke(width = 5.0f, cap = StrokeCap.Square)
    )
    // Step line from Arc 2 to Arc 3
    val step2Angle = rotationDeg + 63f
    val step2Start = RadialMath.toCartesian(center, arc2R, step2Angle)
    val step2End = RadialMath.toCartesian(center, arc3R, step2Angle)
    drawLine(Color.Black, step2Start, step2End, strokeWidth = 4.0f)

    // Arc segment 3 (e.g. November sector bracket)
    drawArc(
        color = Color.Black,
        startAngle = rotationDeg + 63f,
        sweepAngle = 68f,
        useCenter = false,
        topLeft = Offset(center.x - arc3R, center.y - arc3R),
        size = androidx.compose.ui.geometry.Size(arc3R * 2f, arc3R * 2f),
        style = Stroke(width = 5.0f, cap = StrokeCap.Square)
    )

    // Outer radial shelf rays extending outward with tick marks
    val shelfAngles = floatArrayOf(-85f, -48f, -13f, 25f, 63f, 98f, 131f)
    for (ang in shelfAngles) {
        val rayAngle = ang + rotationDeg
        val rStart = outerR + 2f
        val rEnd = outerR * 1.28f
        val pStart = RadialMath.toCartesian(center, rStart, rayAngle)
        val pEnd = RadialMath.toCartesian(center, rEnd, rayAngle)
        drawLine(
            color = Color.Black,
            start = pStart,
            end = pEnd,
            strokeWidth = 1.6f,
            cap = StrokeCap.Square
        )
        // Perpendicular shelf ticks
        for (step in 1..2) {
            val tickR = rStart + (rEnd - rStart) * (step / 3f)
            val tickMid = RadialMath.toCartesian(center, tickR, rayAngle)
            val tickT1 = RadialMath.toCartesian(tickMid, 5f, rayAngle + 90f)
            val tickT2 = RadialMath.toCartesian(tickMid, 5f, rayAngle - 90f)
            drawLine(Color.Black, tickT1, tickT2, strokeWidth = 1.0f)
        }
    }

    // Outer constellation diagram (matching top right of zoom-kal.jpg)
    val constCenter = RadialMath.toCartesian(center, outerR * 1.20f, -55f + rotationDeg)
    val starOffsets = listOf(
        Offset(-18f, -14f), Offset(-4f, -22f), Offset(12f, -10f),
        Offset(24f, 2f), Offset(10f, 18f), Offset(-8f, 12f)
    )
    for (i in 0 until starOffsets.size - 1) {
        drawLine(
            color = Color.Black.copy(alpha = 0.75f),
            start = constCenter + starOffsets[i],
            end = constCenter + starOffsets[i + 1],
            strokeWidth = 1.1f
        )
    }
    for (so in starOffsets) {
        drawCircle(
            color = Color.Black,
            radius = 2.8f,
            center = constCenter + so
        )
    }

    // ==========================================
    // 3. OUTER CIRCLE: AVAILABLE LISTS
    // ==========================================
    // Outer perimeter borderline
    drawCircle(
        color = Color.Black,
        radius = outerR,
        center = center,
        style = Stroke(width = 2.0f)
    )

    // Fine Azimuth Ticks around the outer perimeter (72 steps, major/medium/minor)
    val perimeterTickCount = 72
    for (i in 0 until perimeterTickCount) {
        val angleDeg = i * (360f / perimeterTickCount) + rotationDeg
        val isMajor = (i % 6 == 0)
        val isMedium = (i % 2 == 0)

        val tickLength = if (isMajor) 14f else if (isMedium) 9f else 5f
        val tickStroke = if (isMajor) 2.0f else if (isMedium) 1.2f else 0.8f

        val start = RadialMath.toCartesian(center, outerR - tickLength, angleDeg)
        val end = RadialMath.toCartesian(center, outerR, angleDeg)
        drawLine(
            color = Color.Black,
            start = start,
            end = end,
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
    }

    // Segmented Thick Curved Black Arcs for Available Lists (matching zoom-kal bold arcs)
    val safeListCount = listCount.coerceAtLeast(3)
    val listStepDeg = 360f / safeListCount
    for (l in 0 until safeListCount) {
        val startAng = l * listStepDeg + rotationDeg + 8f
        val sweepAng = listStepDeg - 16f
        val isCurrentList = (l == (activeListIndex % safeListCount))
        val arcStrokeWidth = if (isCurrentList) 6.0f else 4.0f

        drawArc(
            color = Color.Black,
            startAngle = startAng,
            sweepAngle = sweepAng,
            useCenter = false,
            topLeft = Offset(center.x - middleR - 8f, center.y - middleR - 8f),
            size = androidx.compose.ui.geometry.Size((middleR + 8f) * 2f, (middleR + 8f) * 2f),
            style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Square)
        )

        // Radial sector divider ticks at boundary
        val divAngle = l * listStepDeg + rotationDeg
        val divP1 = RadialMath.toCartesian(center, middleR - 4f, divAngle)
        val divP2 = RadialMath.toCartesian(center, outerR - 16f, divAngle)
        drawLine(
            color = Color.Black,
            start = divP1,
            end = divP2,
            strokeWidth = 2.0f,
            cap = StrokeCap.Square
        )
    }

    // ==========================================
    // 2. MIDDLE CIRCLE: ALBUM
    // ==========================================
    // Middle ring boundary circle
    drawCircle(
        color = Color.Black,
        radius = middleR,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Secondary dashed concentric guide line
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = (middleR + innerR) / 2f,
        center = center,
        style = Stroke(
            width = 1.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )
    )

    // Album track radial dividers and ticks
    val safeTrackCount = trackCount.coerceIn(4, 32)
    val trackStepDeg = 360f / safeTrackCount
    for (t in 0 until safeTrackCount) {
        val tickAngle = t * trackStepDeg + rotationDeg
        val isCurrentTrack = (t == (currentTrackIndex % safeTrackCount))

        val tickInner = innerR + 10f
        val tickOuter = if (isCurrentTrack) middleR - 4f else middleR - 14f
        val tickWidth = if (isCurrentTrack) 3.0f else 1.2f

        val pStart = RadialMath.toCartesian(center, tickInner, tickAngle)
        val pEnd = RadialMath.toCartesian(center, tickOuter, tickAngle)
        drawLine(
            color = Color.Black,
            start = pStart,
            end = pEnd,
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )

        // Bold black arc segment for active track in the album
        if (isCurrentTrack) {
            drawArc(
                color = Color.Black,
                startAngle = tickAngle - (trackStepDeg * 0.4f),
                sweepAngle = trackStepDeg * 0.8f,
                useCenter = false,
                topLeft = Offset(center.x - middleR + 4f, center.y - middleR + 4f),
                size = androidx.compose.ui.geometry.Size((middleR - 4f) * 2f, (middleR - 4f) * 2f),
                style = Stroke(width = 5.0f, cap = StrokeCap.Square)
            )
        }
    }

    // Signature thick curved black arc (inspired by the prominent black curved divider in the photo)
    drawArc(
        color = Color.Black,
        startAngle = rotationDeg - 75f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(center.x - middleR, center.y - middleR),
        size = androidx.compose.ui.geometry.Size(middleR * 2f, middleR * 2f),
        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
    )

    // ==========================================
    // 1. MOST INNER CIRCLE: TRACK
    // ==========================================
    // Inner boundary ring
    drawCircle(
        color = Color.Black,
        radius = innerR,
        center = center,
        style = Stroke(width = 1.8f)
    )

    // Clustered fine concentric vinyl grooves (4 precision hairline circles as in zoom-kal)
    val grooveSpacing = (innerR - coreR) / 5f
    for (g in 1..4) {
        val r = coreR + (g * grooveSpacing)
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = r,
            center = center,
            style = Stroke(width = 0.8f)
        )
    }

    // Fine track ticks around the inner ring
    val innerTickCount = 48
    for (i in 0 until innerTickCount) {
        val ang = i * (360f / innerTickCount)
        val isMajor = (i % 4 == 0)
        val tickLen = if (isMajor) 7f else 3.5f
        val p1 = RadialMath.toCartesian(center, innerR - tickLen, ang)
        val p2 = RadialMath.toCartesian(center, innerR, ang)
        drawLine(
            color = Color.Black.copy(alpha = if (isMajor) 0.8f else 0.4f),
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 1.2f else 0.7f
        )
    }

    // Track Playback Progress Arc (Bold black line with bead indicator)
    val activeSweep = (progressFraction.coerceIn(0f, 1f)) * 360f
    val progressRadius = (innerR + coreR) / 2f

    // Inactive progress background path
    drawCircle(
        color = Color.Black.copy(alpha = 0.12f),
        radius = progressRadius,
        center = center,
        style = Stroke(width = 2.0f)
    )

    // Active progress arc
    if (activeSweep > 0.5f) {
        drawArc(
            color = Color.Black,
            startAngle = -90f,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = Offset(center.x - progressRadius, center.y - progressRadius),
            size = androidx.compose.ui.geometry.Size(progressRadius * 2f, progressRadius * 2f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Progress indicator black bead/pip
        val beadCenter = RadialMath.toCartesian(center, progressRadius, -90f + activeSweep)
        drawCircle(
            color = Color.Black,
            radius = 5.5f,
            center = beadCenter
        )
    }

    // 12 o'clock Pointer Needle across the wheel
    val needleTop = RadialMath.toCartesian(center, outerR + 4f, -90f)
    val needleBottom = RadialMath.toCartesian(center, middleR - 2f, -90f)
    drawLine(
        color = Color.Black,
        start = needleTop,
        end = needleBottom,
        strokeWidth = 3.0f,
        cap = StrokeCap.Round
    )

    // ==========================================
    // 0. CENTER CELESTIAL STARRY NIGHT DISK
    // ==========================================
    drawCircle(
        color = Color.Black,
        radius = coreR,
        center = center
    )
    // Twinkling white stars & constellation lines inside the core
    val starCoords = listOf(
        Pair(-0.45f, -0.3f), Pair(-0.2f, -0.6f), Pair(0.1f, -0.4f),
        Pair(0.4f, -0.2f), Pair(0.25f, 0.2f), Pair(-0.1f, 0.45f),
        Pair(-0.4f, 0.2f), Pair(0.5f, 0.35f), Pair(-0.25f, 0.05f),
        Pair(0.0f, -0.1f), Pair(0.3f, -0.45f), Pair(-0.55f, -0.1f)
    )
    val c1 = Offset(center.x + coreR * -0.45f, center.y + coreR * -0.3f)
    val c2 = Offset(center.x + coreR * -0.2f, center.y + coreR * -0.6f)
    val c3 = Offset(center.x + coreR * 0.1f, center.y + coreR * -0.4f)
    val c4 = Offset(center.x + coreR * 0.4f, center.y + coreR * -0.2f)
    val c5 = Offset(center.x + coreR * 0.25f, center.y + coreR * 0.2f)
    drawLine(Color.White.copy(alpha = 0.5f), c1, c2, strokeWidth = 0.8f)
    drawLine(Color.White.copy(alpha = 0.5f), c2, c3, strokeWidth = 0.8f)
    drawLine(Color.White.copy(alpha = 0.5f), c3, c4, strokeWidth = 0.8f)
    drawLine(Color.White.copy(alpha = 0.5f), c4, c5, strokeWidth = 0.8f)
    for ((sx, sy) in starCoords) {
        drawCircle(
            color = Color.White,
            radius = 1.2f,
            center = Offset(center.x + coreR * sx, center.y + coreR * sy)
        )
    }

    // Core boundary ring enclosing the center album/vinyl hub
    drawCircle(
        color = Color.Black,
        radius = coreR + 2f,
        center = center,
        style = Stroke(width = 2.0f)
    )
}

/**
 * Canvas drawing utility for legacy compatibility with degree ticks and bezel rings.
 */
fun DrawScope.drawAstrolabeBezel(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    bezelColor: Color = Color.Black,
    tickColor: Color = Color.Black
) {
    drawCircle(
        color = bezelColor,
        radius = outerRadius,
        center = center,
        style = Stroke(width = 2.0f)
    )
    drawCircle(
        color = bezelColor.copy(alpha = 0.4f),
        radius = innerRadius,
        center = center,
        style = Stroke(
            width = 1.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )
    )

    val tickCount = 72
    for (i in 0 until tickCount) {
        val angleDeg = i * (360f / tickCount)
        val isMajor = (i % 6 == 0)
        val isMedium = (i % 2 == 0)

        val tickLength = if (isMajor) 12f else if (isMedium) 7f else 4f
        val tickStroke = if (isMajor) 2.0f else if (isMedium) 1.0f else 0.8f

        val start = RadialMath.toCartesian(center, outerRadius - tickLength, angleDeg)
        val end = RadialMath.toCartesian(center, outerRadius, angleDeg)
        drawLine(
            color = tickColor,
            start = start,
            end = end,
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Celestial core renderer in monochrome white & black.
 */
fun DrawScope.drawCelestialCore(
    center: Offset,
    radius: Float,
    pulseEnergy: Float = 1.0f,
    rotationDeg: Float = 0f,
    isPlaying: Boolean = true
) {
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.Black,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = radius * 0.65f,
        center = center,
        style = Stroke(width = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f))
    )
}

/**
 * Editorial left half vertical wheel renderer matching zoom-kal.jpg geometry.
 * Sits vertically along the left edge of the screen, sweeping from -90° (top) to +90° (bottom).
 */
fun DrawScope.drawEditorialLeftHalfWheel(
    center: Offset,
    outerRadius: Float,
    rotationDeg: Float = 0f,
    listCount: Int = 12,
    activeListIndex: Int = 0
) {
    val outerR = outerRadius * 0.96f
    val middleR = outerR * 0.72f
    val innerR = outerR * 0.46f
    val coreR = outerR * 0.22f

    // Background semicircle
    drawArc(
        color = Color.White,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - outerR, center.y - outerR),
        size = androidx.compose.ui.geometry.Size(outerR * 2f, outerR * 2f)
    )

    // 1. Concentric Semicircular Border Rings
    drawArc(
        color = Color.Black,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - outerR, center.y - outerR),
        size = androidx.compose.ui.geometry.Size(outerR * 2f, outerR * 2f),
        style = Stroke(width = 2.0f)
    )

    drawArc(
        color = Color.Black,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - middleR, center.y - middleR),
        size = androidx.compose.ui.geometry.Size(middleR * 2f, middleR * 2f),
        style = Stroke(width = 1.5f)
    )

    drawArc(
        color = Color.Black.copy(alpha = 0.35f),
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - (middleR + innerR) / 2f, center.y - (middleR + innerR) / 2f),
        size = androidx.compose.ui.geometry.Size(middleR + innerR, middleR + innerR),
        style = Stroke(
            width = 1.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )
    )

    drawArc(
        color = Color.Black,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - innerR, center.y - innerR),
        size = androidx.compose.ui.geometry.Size(innerR * 2f, innerR * 2f),
        style = Stroke(width = 1.5f)
    )

    // 2. Vinyl Micro-Grooves in the inner ring
    val grooveCount = 4
    val spacing = (innerR - coreR) / (grooveCount + 1)
    for (g in 1..grooveCount) {
        val r = coreR + (g * spacing)
        drawArc(
            color = Color.Black.copy(alpha = 0.22f),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = Stroke(width = 0.8f)
        )
    }

    // 3. Azimuth Ticks around the outer semicircle
    val tickSteps = 36
    for (i in 0..tickSteps) {
        val rawAng = -90f + i * (180f / tickSteps)
        val rotatedAng = rawAng + (rotationDeg % (180f / tickSteps))
        if (rotatedAng in -89f..89f) {
            val isMajor = (i % 6 == 0)
            val isMed = (i % 2 == 0)
            val tickLen = if (isMajor) 14f else if (isMed) 8f else 4f
            val strokeW = if (isMajor) 2.0f else if (isMed) 1.2f else 0.8f

            val pStart = RadialMath.toCartesian(center, outerR - tickLen, rotatedAng)
            val pEnd = RadialMath.toCartesian(center, outerR, rotatedAng)
            drawLine(
                color = Color.Black,
                start = pStart,
                end = pEnd,
                strokeWidth = strokeW,
                cap = StrokeCap.Square
            )
        }
    }

    // 4. Segmented bold arcs for lists
    val safeCount = listCount.coerceIn(3, 24)
    val stepDeg = 180f / safeCount
    for (l in 0 until safeCount) {
        val startA = -90f + l * stepDeg + (rotationDeg % 180f)
        val clampedStart = startA.coerceIn(-90f, 90f)
        val clampedEnd = (startA + stepDeg - 4f).coerceIn(-90f, 90f)
        val sweep = (clampedEnd - clampedStart).coerceAtLeast(0f)
        if (sweep > 2f) {
            val isSelected = (l == (activeListIndex % safeCount))
            val arcW = if (isSelected) 5.5f else 3.5f
            drawArc(
                color = Color.Black,
                startAngle = clampedStart,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - middleR + 2f, center.y - middleR + 2f),
                size = androidx.compose.ui.geometry.Size((middleR - 2f) * 2f, (middleR - 2f) * 2f),
                style = Stroke(width = arcW, cap = StrokeCap.Square)
            )
        }
    }

    // 5. Hub Semicircle at Left Edge
    drawArc(
        color = Color.White,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - coreR, center.y - coreR),
        size = androidx.compose.ui.geometry.Size(coreR * 2f, coreR * 2f)
    )
    drawArc(
        color = Color.Black,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - coreR, center.y - coreR),
        size = androidx.compose.ui.geometry.Size(coreR * 2f, coreR * 2f),
        style = Stroke(width = 2.0f)
    )
    drawCircle(
        color = Color.Black,
        radius = 4f,
        center = center
    )

    // 6. Active Pointer / Cursor Needle pointing directly horizontally into the active list at 0° (equator)
    val pointerP1 = RadialMath.toCartesian(center, innerR, 0f)
    val pointerP2 = RadialMath.toCartesian(center, outerR + 10f, 0f)
    drawLine(
        color = Color.Black,
        start = pointerP1,
        end = pointerP2,
        strokeWidth = 3.0f,
        cap = StrokeCap.Square
    )
    // Triangular arrow head pointing right at active item
    val arrowTip = RadialMath.toCartesian(center, outerR + 16f, 0f)
    val arrowTop = RadialMath.toCartesian(center, outerR + 8f, -6f)
    val arrowBottom = RadialMath.toCartesian(center, outerR + 8f, 6f)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(arrowTip.x, arrowTip.y)
        lineTo(arrowTop.x, arrowTop.y)
        lineTo(arrowBottom.x, arrowBottom.y)
        close()
    }
    drawPath(path, Color.Black)
}
