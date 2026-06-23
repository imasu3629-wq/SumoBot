package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.events.packet.PacketEvent
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs

/**
 * Optional outgoing-packet optimizer.
 *
 * Minecraft 1.8.9 sends a C03PacketPlayer (or one of its position/look subtypes)
 * every client tick to keep the server informed, even when nothing changed.
 * When enabled, this trims packets that carry no new information:
 *
 *  - Redundant look packets are dropped when the rotation hasn't meaningfully moved.
 *  - Idle duplicate position packets are dropped (optional, off by default since
 *    some anti-cheats dislike missing keep-alives).
 *  - Duplicate arm-swing animations within the same tick are dropped.
 *
 * All behaviour is gated behind the "Packets" config category so it can be toggled
 * from the in-game config screen.
 */
object PacketOptimizer {

    private var lastYaw = 0f
    private var lastPitch = 0f

    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    private var lastSwingTick = -1

    // Below this angular change (degrees) a rotation is considered "unchanged".
    private const val ROTATION_EPSILON = 0.05f
    private const val POSITION_EPSILON = 1.0E-4

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Outgoing) {
        val config = DuckDueller.config ?: return
        if (!config.packetOptimization) return
        // Only optimize while the bot is actually active so normal play is untouched.
        if (DuckDueller.bot?.toggled() != true) return

        when (val packet = event.getPacket()) {
            is C03PacketPlayer -> handlePlayerPacket(event, packet, config)
            is C0APacketAnimation -> handleAnimation(event, config)
        }
    }

    private fun handlePlayerPacket(
        event: PacketEvent.Outgoing,
        packet: C03PacketPlayer,
        config: best.spaghetcodes.duckdueller.core.Config,
    ) {
        if (DuckDueller.mc.thePlayer == null) return

        // Determine what kind of update this is from the concrete subtype.

        val isLook = packet is C03PacketPlayer.C05PacketPlayerLook || packet is C03PacketPlayer.C06PacketPlayerPosLook
        val isPos = packet is C03PacketPlayer.C04PacketPlayerPosition || packet is C03PacketPlayer.C06PacketPlayerPosLook

        // --- Redundant look removal (pure look packets only) ---
        if (config.packetRemoveRedundantRotations && isLook) {
            val yaw = packet.getYaw()
            val pitch = packet.getPitch()
            val unchanged = abs(yaw - lastYaw) < ROTATION_EPSILON && abs(pitch - lastPitch) < ROTATION_EPSILON
            lastYaw = yaw
            lastPitch = pitch

            // A pure look packet (no position payload) that doesn't change the angle is redundant.
            if (unchanged && !isPos) {
                event.isCanceled = true
                return
            }
        } else if (isLook) {
            lastYaw = packet.getYaw()
            lastPitch = packet.getPitch()
        }

        // --- Duplicate position removal (pure position packets only) ---
        if (config.packetRemoveDuplicatePosition && isPos && !isLook) {
            val x = packet.getPositionX()
            val y = packet.getPositionY()
            val z = packet.getPositionZ()

            val unchanged = abs(x - lastPosX) < POSITION_EPSILON &&
                    abs(y - lastPosY) < POSITION_EPSILON &&
                    abs(z - lastPosZ) < POSITION_EPSILON
            lastPosX = x
            lastPosY = y
            lastPosZ = z

            if (unchanged) {
                event.isCanceled = true
                return
            }
        }
    }

    private fun handleAnimation(event: PacketEvent.Outgoing, config: best.spaghetcodes.duckdueller.core.Config) {
        if (!config.packetBatchSwings) return
        val player = DuckDueller.mc.thePlayer ?: return
        val tick = player.ticksExisted
        if (tick == lastSwingTick) {
            // Already swung this tick - drop the duplicate.
            event.isCanceled = true
            return
        }
        lastSwingTick = tick
    }

    fun reset() {
        lastSwingTick = -1
    }
}
