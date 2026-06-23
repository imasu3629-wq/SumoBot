package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.core.Config
import best.spaghetcodes.duckdueller.utils.RandomUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity

object BurstClickManager {
    var lastAttackTick = 0

    // Human reaction can't perfectly catch the exact tick the hurt timer expires.
    // A tiny random extra-delay is rolled each time the window opens so the bot
    // doesn't hit with frame-perfect consistency. Kept very small to stay strong.
    private var pendingReactionTicks = -1

    fun shouldAttack(target: EntityPlayer?): Boolean {
        if (target == null) return false
        val mc = DuckDueller.mc
        val config = DuckDueller.config ?: return false
        
        if (mc.thePlayer.getDistanceToEntity(target) > config.maxDistanceAttack) {
            pendingReactionTicks = -1
            return false
        }
        
        if (target.hurtResistantTime <= 1) {
            // Only humanize when the humanizer is on; otherwise behave exactly as before.
            if (config.humanizedClicks) {
                if (pendingReactionTicks < 0) {
                    // 0-2 tick (~0-100ms) jitter so the burst isn't frame-perfect.
                    pendingReactionTicks = RandomUtils.randomIntInRange(0, 2)
                }
                if (pendingReactionTicks > 0) {
                    pendingReactionTicks--
                    return false
                }
                pendingReactionTicks = -1
                return true
            }
            return true
        }
        // hurt timer is up again - reset for the next window
        pendingReactionTicks = -1
        return false
    }


    fun executeAttack(target: EntityPlayer?) {
        if (target == null) return
        val mc = DuckDueller.mc
        
        mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
        mc.thePlayer.swingItem()
        
        lastAttackTick = mc.thePlayer.ticksExisted
    }

    fun onTick(target: EntityPlayer?) {
        val config = DuckDueller.config ?: return
        if (!config.burstClickEnabled) return
        
        if (shouldAttack(target)) {
            executeAttack(target)
        }
    }

    fun reset() {
        lastAttackTick = 0
    }
}
