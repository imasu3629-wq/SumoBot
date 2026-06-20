package best.spaghetcodes.duckdueller.bot

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.utils.ChatUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.random.Random

object SessionScheduler {
    enum class State {
        PLAYING, RESTING, RECONNECTING, DISABLED
    }

    var currentState = State.DISABLED
    var sessionEndTime: Long = 0
    var restEndTime: Long = 0

    fun start() {
        val config = DuckDueller.Companion.getConfig() ?: return
        if (config.sessionEnabled) {
            currentState = State.PLAYING
            val playTime = generatePlayTime(config.sessionPlayMin, config.sessionPlayMax)
            sessionEndTime = System.currentTimeMillis() + playTime
            ChatUtils.info("Session started. Playing for ${playTime / 60000} minutes.")
        }
    }

    fun stop() {
        currentState = State.DISABLED
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        
        val config = DuckDueller.Companion.getConfig() ?: return
        if (!config.sessionEnabled) return

        val mc = DuckDueller.mc
        val now = System.currentTimeMillis()

        when (currentState) {
            State.PLAYING -> {
                if (now > sessionEndTime) {
                    if (mc.thePlayer != null) {
                        mc.thePlayer.sendChatMessage("/lobby")
                    }
                    val restTime = generateRestTime(config.sessionRestMin, config.sessionRestMax)
                    restEndTime = now + restTime + 3000 // 3 seconds delay for lobby
                    currentState = State.RESTING
                    ChatUtils.info("Session ended. Resting for ${restTime / 60000} minutes.")
                }
            }
            State.RESTING -> {
                if (now > restEndTime) {
                    currentState = State.RECONNECTING
                    ChatUtils.info("Rest time is over. Please reconnect.")
                }
            }
            State.RECONNECTING -> {
                // To be implemented: auto reconnect logic if needed
            }
            State.DISABLED -> {}
        }
    }

    private fun generatePlayTime(min: Int, max: Int): Long {
        return (Random.nextInt(min, max + 1) * 60 * 1000).toLong()
    }

    private fun generateRestTime(min: Int, max: Int): Long {
        return (Random.nextInt(min, max + 1) * 60 * 1000).toLong()
    }
}
