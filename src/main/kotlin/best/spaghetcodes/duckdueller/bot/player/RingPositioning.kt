package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.utils.WorldUtils
import best.spaghetcodes.duckdueller.utils.EntityUtils
import best.spaghetcodes.duckdueller.DuckDueller
import net.minecraft.entity.player.EntityPlayer

object RingPositioning {
    var lastStrafeDirection = false // false = right, true = left

    fun calculateSafeStrafeVector(player: EntityPlayer, target: EntityPlayer): IntArray {
        val movePriority = intArrayOf(0, 0) // [left, right]
        val config = DuckDueller.Companion.getConfig() ?: return movePriority

        val leftEdge = WorldUtils.distanceToLeftEdge(player)
        val rightEdge = WorldUtils.distanceToRightEdge(player)
        val airFront = WorldUtils.airInFront(player, config.edgeThreshold.toFloat())
        val airBack = WorldUtils.airInBack(player, config.edgeThreshold.toFloat())

        val intensity = config.strafeIntensity

        if (leftEdge < config.edgeThreshold) {
            movePriority[1] += intensity // 右へ
        }
        if (rightEdge < config.edgeThreshold) {
            movePriority[0] += intensity // 左へ
        }

        if (airFront) {
            if (leftEdge > rightEdge) {
                movePriority[0] += intensity * 2
            } else {
                movePriority[1] += intensity * 2
            }
        }
        return movePriority
    }

    fun shouldDiagonalStrafe(player: EntityPlayer, target: EntityPlayer): Boolean {
        val config = DuckDueller.Companion.getConfig() ?: return false
        if (!config.diagonalStrafeEnabled) return false

        val dist = EntityUtils.getDistanceNoY(player, target)
        if (dist in 3.5f..6.0f && player.isSprinting) {
            return true
        }
        return false
    }

    fun applyDiagonalStrafe(player: EntityPlayer, target: EntityPlayer) {
        if (EntityUtils.entityMovingLeft(player, target)) {
            Movement.startRight()
            lastStrafeDirection = false
        } else {
            Movement.startLeft()
            lastStrafeDirection = true
        }
    }
}
