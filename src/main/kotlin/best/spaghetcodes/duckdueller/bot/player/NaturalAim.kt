package best.spaghetcodes.duckdueller.bot.player

import kotlin.math.floor
import kotlin.math.pow

object NaturalAim {
    var noiseTime = 0.0

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
    }
}
