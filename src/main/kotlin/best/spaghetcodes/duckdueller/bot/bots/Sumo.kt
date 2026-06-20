package best.spaghetcodes.duckdueller.bot.bots

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.bot.StateManager
import best.spaghetcodes.duckdueller.bot.player.Combat
import best.spaghetcodes.duckdueller.bot.player.LobbyMovement
import best.spaghetcodes.duckdueller.bot.player.Mouse
import best.spaghetcodes.duckdueller.bot.player.Movement
import best.spaghetcodes.duckdueller.utils.*
import kotlin.math.abs

class Sumo : BotBase("/play duels_sumo_duel") {

    override fun getName(): String {
        return "Sumo"
    }

    init {
        setStatKeys(
            mapOf(
                "wins" to "player.stats.Duels.sumo_duel_wins",
                "losses" to "player.stats.Duels.sumo_duel_losses",
                "ws" to "player.stats.Duels.current_sumo_winstreak",
            )
        )
    }

    private var tapping = false
    private var opponentOffEdge = false
    private var tap50 = false

    override fun onJoinGame() {
        if (DuckDueller.config?.lobbyMovement == true) {
            LobbyMovement.sumo()
        }
    }

    override fun beforeStart() {
        LobbyMovement.stop()
    }

    override fun beforeLeave() {
        LobbyMovement.stop()
    }

    override fun onGameStart() {
        LobbyMovement.stop()
        Movement.startSprinting()
        Movement.startForward()
    }

    override fun onGameEnd() {
        TimeUtils.setTimeout(fun () {
            Movement.clearAll()
            Mouse.stopLeftAC()
            Combat.stopRandomStrafe()
        }, RandomUtils.randomIntInRange(100, 300))
    }

    override fun onAttack() {
        if (!tapping) {
            tapping = true
            val dur = if (tap50) 50 else 100
            val config = DuckDueller.config
            if (config != null && best.spaghetcodes.duckdueller.utils.RandomUtils.randomIntInRange(0, 100) <= config.wTapFrequency) {
                Combat.wTap(dur)
            }
            tap50 = !tap50
            TimeUtils.setTimeout(fun () {
                tapping = false
            }, dur)
        }
    }

    override fun onFoundOpponent() {
        Mouse.startTracking()
    }

    fun leftEdge(distance: Float): Boolean {
        return (WorldUtils.airOnLeft(mc.thePlayer, distance))
    }

    fun rightEdge(distance: Float): Boolean {
        return (WorldUtils.airOnRight(mc.thePlayer, distance))
    }

    fun nearEdge(distance: Float): Boolean { // doesnt check front
        return (rightEdge(distance) || leftEdge(distance) || WorldUtils.airInBack(mc.thePlayer, distance))
    }

    fun opponentNearEdge(distance: Float): Boolean {
        return (WorldUtils.airInBack(opponent()!!, distance) || WorldUtils.airOnLeft(opponent()!!, distance) || WorldUtils.airOnRight(
            opponent()!!, distance))
    }

    override fun onTick() {
        opponentOffEdge = opponent() != null && mc.thePlayer != null &&
                (WorldUtils.entityOffEdge(opponent()!!) || opponentOffEdge && EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!) > 6)
        
        val config = DuckDueller.config

        if (!opponentOffEdge && mc.thePlayer != null && opponent() != null) {
            if (!mc.thePlayer.isSprinting) {
                Movement.startSprinting()
            }

            Mouse.startTracking()
            val distance = EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!)

            // Burst Click and Hit Select integration
            best.spaghetcodes.duckdueller.bot.player.HitSelector.onTick()
            if (config != null && config.burstClickEnabled) {
                Mouse.stopLeftAC() // Disable normal auto clicker
                best.spaghetcodes.duckdueller.bot.player.BurstClickManager.onTick(opponent())
            } else {
                if (distance > (config?.maxDistanceAttack ?: 5)) {
                    Mouse.stopLeftAC()
                } else {
                    if (!best.spaghetcodes.duckdueller.bot.player.HitSelector.shouldBlockAttack()) {
                        Mouse.startLeftAC()
                    } else {
                        Mouse.stopLeftAC()
                    }
                }
            }

            // Ring Positioning
            val safeStrafe = best.spaghetcodes.duckdueller.bot.player.RingPositioning.calculateSafeStrafeVector(mc.thePlayer, opponent()!!)
            if (safeStrafe[0] > 0 || safeStrafe[1] > 0) {
                Combat.stopRandomStrafe()
                if (safeStrafe[0] > safeStrafe[1]) {
                    Movement.stopRight()
                    Movement.startLeft()
                } else {
                    Movement.stopLeft()
                    Movement.startRight()
                }
            } else if (best.spaghetcodes.duckdueller.bot.player.RingPositioning.shouldDiagonalStrafe(mc.thePlayer, opponent()!!)) {
                Combat.stopRandomStrafe()
                best.spaghetcodes.duckdueller.bot.player.RingPositioning.applyDiagonalStrafe(mc.thePlayer, opponent()!!)
            } else {
                if (distance > 3) {
                    Combat.startRandomStrafe(900, 1400)
                } else {
                    Combat.stopRandomStrafe()
                    Movement.clearLeftRight()
                }
            }

            if (distance < 1.2) {
                Movement.stopForward()
            } else {
                if (!tapping) {
                    Movement.startForward()
                }
            }

            // don't walk off an edge
            if (WorldUtils.airInFront(mc.thePlayer, 2f) && mc.thePlayer.onGround) {
                Movement.startSneaking()
            } else {
                Movement.stopSneaking()
            }
            if (WorldUtils.airInBack(mc.thePlayer, 2.5f) && mc.thePlayer.onGround) {
                Movement.startForward()
                Movement.clearLeftRight()
            }
        } else {
            if (opponentOffEdge && StateManager.state == StateManager.States.PLAYING) {
                Movement.clearAll()
                Mouse.stopLeftAC()
                Combat.stopRandomStrafe()
                Mouse.stopTracking()
            }
        }
    }

}