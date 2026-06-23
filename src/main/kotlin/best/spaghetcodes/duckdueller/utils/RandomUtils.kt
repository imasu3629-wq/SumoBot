package best.spaghetcodes.duckdueller.utils

import java.util.*
import java.util.concurrent.ThreadLocalRandom

object RandomUtils {

    /**
     * Get a random integer in a certain range
     * @param min
     * @param max
     * @return int
     */
    fun randomIntInRange(min: Int, max: Int): Int {
        return ThreadLocalRandom.current().nextInt(min, max + 1)
    }

    /**
     * Get a random double in a certain range
     * @param min
     * @param max
     * @return double
     */
    fun randomDoubleInRange(min: Double, max: Double): Double {
        val r = Random()
        return min + (max - min) * r.nextDouble()
    }

    /**
     * Get a random boolean value
     * @return bool
     */
    fun randomBool(): Boolean {
        val r = Random()
        return r.nextBoolean()
    }

    /**
     * Returns true with the given probability (0.0 - 1.0).
     */
    fun chance(probability: Double): Boolean {
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        return ThreadLocalRandom.current().nextDouble() < probability
    }

    /**
     * Get a gaussian (normal) distributed value, clamped to [min, max].
     * This feels much more human than a flat uniform distribution because
     * values cluster around the mean with occasional outliers.
     *
     * @param mean center of the distribution
     * @param stdDev standard deviation (spread)
     * @param min hard lower bound
     * @param max hard upper bound
     */
    fun randomGaussian(mean: Double, stdDev: Double, min: Double, max: Double): Double {
        val value = mean + ThreadLocalRandom.current().nextGaussian() * stdDev
        return value.coerceIn(min, max)
    }

    /**
     * Gaussian distributed integer, clamped to [min, max].
     * Mean defaults to the middle of the range, spread to ~1/4 of the range.
     */
    fun randomGaussianIntInRange(min: Int, max: Int): Int {
        if (min >= max) return min
        val mean = (min + max) / 2.0
        val stdDev = (max - min) / 4.0
        return Math.round(randomGaussian(mean, stdDev, min.toDouble(), max.toDouble())).toInt()
    }

}


