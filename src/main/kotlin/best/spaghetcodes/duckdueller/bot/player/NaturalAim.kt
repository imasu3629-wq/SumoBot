package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.utils.RandomUtils
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sign

object NaturalAim {
    var noiseTime = 0.0

    // Internal "hand" velocity so rotation has momentum like a real mouse movement.
    private var yawVelocity = 0.0
    private var pitchVelocity = 0.0

    // Per-flick noise parameters, re-rolled occasionally so the jitter isn't perfectly periodic.
    private var noiseFreq = 0.05
    private var noiseReroll = 0

    /**
     * Original easing-based smoothing (kept for compatibility / fallback).
     */
    fun smooth(currentYaw: Float, currentPitch: Float, targetYaw: Float, targetPitch: Float, smoothFactor: Double, noiseScale: Double): FloatArray {
        val deltaYaw = wrapAngleTo180(targetYaw - currentYaw)
        val deltaPitch = wrapAngleTo180(targetPitch - currentPitch)

        // Easing
        val easedYaw = deltaYaw * easeOutCubic(smoothFactor)
        val easedPitch = deltaPitch * easeOutCubic(smoothFactor)

        // Perlin noise
        val noiseY = perlinNoise1D(noiseTime) * noiseScale
        val noiseP = perlinNoise1D(noiseTime + 100.0) * noiseScale * 0.5

        noiseTime += 0.05

        return floatArrayOf(
            (currentYaw + easedYaw + noiseY).toFloat(),
            (currentPitch + easedPitch + noiseP).toFloat()
        )
    }

    /**
     * Momentum-based humanized aim. Simulates the way a human hand accelerates
     * toward a target and decelerates as it arrives, with a small chance to
     * slightly overshoot and correct. Returns the next absolute {yaw, pitch}.
     *
     * @param smoothFactor 0.1 (very smooth/slow) .. 1.0 (snappy). Maps to acceleration.
     * @param noiseScale base amount of hand jitter
     * @param overshootChance probability (0-1) of overshooting on a large flick
     */
    fun humanize(
        currentYaw: Float,
        currentPitch: Float,
        targetYaw: Float,
        targetPitch: Float,
        smoothFactor: Double,
        noiseScale: Double,
        overshootChance: Double = 0.0,
    ): FloatArray {
        val deltaYaw = wrapAngleTo180(targetYaw - currentYaw).toDouble()
        val deltaPitch = wrapAngleTo180(targetPitch - currentPitch).toDouble()

        // Acceleration / responsiveness derived from smoothFactor.
        // Higher smoothFactor -> reaches the target faster.
        val accel = 0.18 + smoothFactor * 0.42 // ~0.18 .. 0.6
        // Damping prevents the velocity from running away and creates the decel curve.
        val damping = 0.55

        // Occasionally overshoot a large movement, then the correction happens naturally next ticks.
        var aimDeltaYaw = deltaYaw
        if (overshootChance > 0.0 && abs(deltaYaw) > 25.0 && RandomUtils.chance(overshootChance)) {
            aimDeltaYaw += sign(deltaYaw) * RandomUtils.randomDoubleInRange(2.0, 6.0)
        }

        // Spring-like integration: velocity moves toward the remaining delta, then is damped.
        yawVelocity = yawVelocity * damping + aimDeltaYaw * accel
        pitchVelocity = pitchVelocity * damping + deltaPitch * accel

        // Don't let momentum carry us insanely far in a single tick.
        val maxStep = 70.0
        yawVelocity = yawVelocity.coerceIn(-maxStep, maxStep)
        pitchVelocity = pitchVelocity.coerceIn(-maxStep, maxStep)

        // Humanized jitter using Perlin noise with a wandering frequency.
        if (noiseReroll-- <= 0) {
            noiseFreq = RandomUtils.randomDoubleInRange(0.035, 0.08)
            noiseReroll = RandomUtils.randomIntInRange(15, 40)
        }
        // Jitter scales down as we settle on the target so resting aim is steady.
        val settle = (abs(deltaYaw) / 30.0).coerceIn(0.2, 1.0)
        val noiseY = perlinNoise1D(noiseTime) * noiseScale * settle
        val noiseP = perlinNoise1D(noiseTime + 100.0) * noiseScale * 0.5 * settle
        noiseTime += noiseFreq

        return floatArrayOf(
            (currentYaw + yawVelocity + noiseY).toFloat(),
            (currentPitch + pitchVelocity + noiseP).toFloat()
        )
    }

    private fun wrapAngleTo180(angle: Float): Float {
        var result = angle % 360.0f
        if (result >= 180.0f) {
            result -= 360.0f
        }
        if (result < -180.0f) {
            result += 360.0f
        }
        return result
    }

    private fun easeOutCubic(t: Double): Double {
        val t1 = 1.0 - t
        return 1.0 - t1 * t1 * t1
    }

    private fun perlinNoise1D(x: Double): Double {
        val xi = floor(x).toInt()
        val xf = x - xi
        val fade = 6 * xf.pow(5) - 15 * xf.pow(4) + 10 * xf.pow(3)
        return lerp(fade, grad(xi, xf), grad(xi + 1, xf - 1))
    }

    private fun lerp(t: Double, a: Double, b: Double): Double {
        return a + t * (b - a)
    }

    private fun grad(hash: Int, x: Double): Double {
        val h = hash and 15
        val grad = 1.0 + (h and 7)
        return if (h and 8 != 0) -grad * x else grad * x
    }

    fun reset() {
        noiseTime = 0.0
        yawVelocity = 0.0
        pitchVelocity = 0.0
    }
}
