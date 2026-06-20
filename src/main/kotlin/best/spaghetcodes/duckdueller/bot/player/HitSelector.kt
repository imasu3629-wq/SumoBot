package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import net.minecraft.entity.player.EntityPlayer
import kotlin.random.Random

object HitSelector {
    var hitSelectActive = false
    var selectEndTick = 0
    var currentTick = 0

    enum class SelectMode(val delay: Int) {
        LIGHT(Random.nextInt(1, 3)),
        NORMAL(Random.nextInt(3, 5)),
        LONG(Random.nextInt(5, 7))
    }

    var selectMode: SelectMode = SelectMode.NORMAL

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    fun onLivingHurt(event: net.minecraftforge.event.entity.living.LivingHurtEvent) {
        if (event.entityLiving == DuckDueller.mc.thePlayer) {
            onDamageTaken(DuckDueller.bot?.opponent())
        }
    }

    fun onDamageTaken(target: EntityPlayer?) {
        val config = DuckDueller.config ?: return
        if (!config.hitSelectEnabled) return

        val player = DuckDueller.mc.thePlayer ?: return

        // 自分が相手より高い位置にいる（浮かされている）場合のみHit Select発動
        if (target != null && player.posY > target.posY + 0.1) {
            hitSelectActive = true
            
            val modes = SelectMode.values()
            selectMode = modes[Random.nextInt(modes.size)]
            
            selectEndTick = currentTick + selectMode.delay
        }
    }

    fun shouldBlockAttack(): Boolean {
        return hitSelectActive
    }

    fun onTick() {
        currentTick++
        if (hitSelectActive && currentTick >= selectEndTick) {
            hitSelectActive = false
        }
    }

    fun reset() {
        hitSelectActive = false
        currentTick = 0
        selectEndTick = 0
    }
}
