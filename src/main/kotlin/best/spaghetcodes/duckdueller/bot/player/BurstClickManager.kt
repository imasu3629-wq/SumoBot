package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.core.Config
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity

object BurstClickManager {
    var lastAttackTick = 0

    fun shouldAttack(target: EntityPlayer?): Boolean {
        if (target == null) return false
        val mc = DuckDueller.mc
        val config = DuckDueller.config ?: return false
        
        if (mc.thePlayer.getDistanceToEntity(target) > config.maxDistanceAttack) {
            return false
        }
        
        if (target.hurtResistantTime <= 1) {
            return true
        }
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
