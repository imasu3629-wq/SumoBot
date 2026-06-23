package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.utils.EntityUtils
import best.spaghetcodes.duckdueller.utils.RandomUtils
import best.spaghetcodes.duckdueller.utils.TimeUtils
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs

object Mouse {

    private var leftAC = false
    var rClickDown = false

    private var tracking = false

    private var _usingProjectile = false
    private var _usingPotion = false
    private var _runningAway = false

    private var leftClickDur = 0

    private var lastLeftClick = 0L

    private var runningRotations: FloatArray? = null

    private var splashAim = 0.0

    // Reaction delay: timestamp (ms) before which aim should not yet react to a freshly acquired target.
    private var reactionReadyAt = 0L

    // Humanized clicking state
    private var nextClickDelay = 0L
    private var doubleClickPending = false


    fun leftClick() {
        if (DuckDueller.bot?.toggled() == true && DuckDueller.mc.thePlayer != null && !DuckDueller.mc.thePlayer.isUsingItem) {
            DuckDueller.mc.thePlayer.swingItem()
            KeyBinding.setKeyBindState(DuckDueller.mc.gameSettings.keyBindAttack.keyCode, true)
            if (DuckDueller.mc.objectMouseOver != null && DuckDueller.mc.objectMouseOver.entityHit != null) {
                DuckDueller.mc.playerController.attackEntity(DuckDueller.mc.thePlayer, DuckDueller.mc.objectMouseOver.entityHit)
            }
        }
    }

    fun rClick(duration: Int) {
        if (DuckDueller.bot?.toggled() == true) {
            if (!rClickDown) {
                rClickDown()
                TimeUtils.setTimeout(this::rClickUp, duration)
            }
        }
    }

    fun startLeftAC() {
        if (DuckDueller.bot?.toggled() == true) {
            leftAC = true
        }
    }

    fun stopLeftAC() {
        // no need to check for toggled state here
        leftAC = false
    }

    fun startTracking() {
        if (!tracking) {
            // Only impose a reaction delay when we first START tracking a target.
            // Once locked on, aim stays accurate - this just mimics human latency on acquisition.
            val config = DuckDueller.config
            if (config?.reactionDelayEnabled == true) {
                val min = config.reactionDelayMin.coerceAtMost(config.reactionDelayMax)
                val max = config.reactionDelayMax.coerceAtLeast(config.reactionDelayMin)
                reactionReadyAt = System.currentTimeMillis() + RandomUtils.randomIntInRange(min, max)
            } else {
                reactionReadyAt = 0L
            }
        }
        tracking = true
    }

    fun stopTracking() {
        tracking = false
        best.spaghetcodes.duckdueller.bot.player.NaturalAim.reset()
    }


    fun setUsingProjectile(proj: Boolean) {
        _usingProjectile = proj
    }

    fun isUsingProjectile(): Boolean {
        return _usingProjectile
    }

    fun setUsingPotion(potion: Boolean) {
        _usingPotion = potion
        if (!_usingPotion) {
            splashAim = 0.0
        }
    }

    fun isUsingPotion(): Boolean {
        return _usingPotion
    }

    fun setRunningAway(runningAway: Boolean) {
        _runningAway = runningAway
        runningRotations = null // make sure to clear this, otherwise running away gets buggy asf
    }

    fun isRunningAway(): Boolean {
        return _runningAway
    }

    private fun leftACFunc() {
        if (DuckDueller.bot?.toggled() == true && leftAC) {
            if (!DuckDueller.mc.thePlayer.isUsingItem) {
                val config = DuckDueller.config
                val minCPS = config?.minCPS ?: 10
                val maxCPS = config?.maxCPS ?: 14
                val humanized = config?.humanizedClicks ?: true

                val now = System.currentTimeMillis()

                if (!humanized) {
                    // Original uniform behaviour
                    if (now >= lastLeftClick + (1000 / RandomUtils.randomIntInRange(minCPS, maxCPS))) {
                        leftClick()
                        lastLeftClick = now
                    }
                    return
                }

                // Humanized: gaussian inter-click interval so the rhythm clusters
                // around a comfortable CPS with natural variation, plus rare misses
                // and rare double-clicks. Net click rate stays in the configured band.
                if (now >= lastLeftClick + nextClickDelay) {
                    val missChance = (config?.clickMissChance ?: 0) / 100.0

                    if (doubleClickPending) {
                        // Quick follow-up click of a human "double tap"
                        leftClick()
                        doubleClickPending = false
                        lastLeftClick = now
                        nextClickDelay = RandomUtils.randomGaussianIntInRange(
                            1000 / maxCPS,
                            1000 / minCPS
                        ).toLong()
                        return
                    }

                    if (RandomUtils.chance(missChance)) {
                        // Drop this click (human inconsistency) but don't stall too long.
                        lastLeftClick = now
                        nextClickDelay = RandomUtils.randomGaussianIntInRange(
                            1000 / maxCPS,
                            1000 / minCPS
                        ).toLong()
                        return
                    }

                    leftClick()
                    lastLeftClick = now

                    // Occasionally queue a fast second click (burst), otherwise normal gaussian gap.
                    if (RandomUtils.chance(0.08)) {
                        doubleClickPending = true
                        nextClickDelay = RandomUtils.randomIntInRange(40, 70).toLong()
                    } else {
                        nextClickDelay = RandomUtils.randomGaussianIntInRange(
                            1000 / maxCPS,
                            1000 / minCPS
                        ).toLong()
                    }
                }
            }
        }
    }


    private fun rClickDown() {
        if (DuckDueller.bot?.toggled() == true) {
            rClickDown = true
            KeyBinding.setKeyBindState(DuckDueller.mc.gameSettings.keyBindUseItem.keyCode, true)
        }
    }

    fun rClickUp() {
        if (DuckDueller.bot?.toggled() == true) {
            rClickDown = false
            KeyBinding.setKeyBindState(DuckDueller.mc.gameSettings.keyBindUseItem.keyCode, false)
        }
    }

    @SubscribeEvent
    fun onTick(ev: TickEvent.ClientTickEvent) {
        if (DuckDueller.mc.thePlayer != null && DuckDueller.bot?.toggled() == true) {
            if (leftAC) {
                leftACFunc()
            }

            if (leftClickDur > 0) {
                leftClickDur--
            } else {
                KeyBinding.setKeyBindState(DuckDueller.mc.gameSettings.keyBindAttack.keyCode, false)
            }
        }
        if (DuckDueller.mc.thePlayer != null && DuckDueller.bot?.toggled() == true && tracking && DuckDueller.bot?.opponent() != null) {
            // Reaction delay: hold off on reacting to a freshly acquired target for a human-like moment.
            // This does not lower accuracy once we start tracking, it just delays the initial reaction.
            if (System.currentTimeMillis() < reactionReadyAt) {
                return
            }

            if (_runningAway) {
                _usingProjectile = false
            }
            var rotations = EntityUtils.getRotations(DuckDueller.mc.thePlayer, DuckDueller.bot?.opponent(), false)


            if (rotations != null) {
                if (_runningAway) {
                    if (runningRotations == null) {
                        runningRotations = rotations
                        runningRotations!![0] += 180 + RandomUtils.randomDoubleInRange(-5.0, 5.0).toFloat()
                    }
                    rotations = runningRotations!!
                }

                if (_usingPotion) {
                    if (splashAim == 0.0) {
                        splashAim = RandomUtils.randomDoubleInRange(80.0, 90.0)
                    }
                    rotations[1] = splashAim.toFloat()
                }

                val config = DuckDueller.config
                val smoothFactor = config?.aimSmoothFactor?.toDouble() ?: 0.6
                val lookRand = (config?.lookRand ?: 0.3f).toDouble()
                val humanizedAim = config?.humanizedAim ?: true

                val smoothed = if (humanizedAim) {
                    val overshoot = (config?.aimOvershootChance ?: 0) / 100.0
                    best.spaghetcodes.duckdueller.bot.player.NaturalAim.humanize(
                        DuckDueller.mc.thePlayer.rotationYaw,
                        DuckDueller.mc.thePlayer.rotationPitch,
                        rotations[0],
                        rotations[1],
                        smoothFactor,
                        lookRand * 10.0,
                        overshoot
                    )
                } else {
                    best.spaghetcodes.duckdueller.bot.player.NaturalAim.smooth(
                        DuckDueller.mc.thePlayer.rotationYaw,
                        DuckDueller.mc.thePlayer.rotationPitch,
                        rotations[0],
                        rotations[1],
                        smoothFactor,
                        lookRand * 10.0
                    )
                }


                var dyaw = smoothed[0] - DuckDueller.mc.thePlayer.rotationYaw
                var dpitch = smoothed[1] - DuckDueller.mc.thePlayer.rotationPitch

                val factor = when (EntityUtils.getDistanceNoY(DuckDueller.mc.thePlayer, DuckDueller.bot?.opponent()!!)) {
                    in 0f..10f -> 1.0f
                    in 10f..20f -> 0.6f
                    in 20f..30f -> 0.4f
                    else -> 0.2f
                }

                val maxRotH = (DuckDueller.config?.lookSpeedHorizontal ?: 10).toFloat() * factor
                val maxRotV = (DuckDueller.config?.lookSpeedVertical ?: 5).toFloat() * factor

                if (abs(dyaw) > maxRotH) {
                    dyaw = if (dyaw > 0) {
                        maxRotH
                    } else {
                        -maxRotH
                    }
                }

                if (abs(dpitch) > maxRotV) {
                    dpitch = if (dpitch > 0) {
                        maxRotV
                    } else {
                        -maxRotV
                    }
                }

                DuckDueller.mc.thePlayer.rotationYaw += dyaw
                DuckDueller.mc.thePlayer.rotationPitch += dpitch
            }
        }
    }

}