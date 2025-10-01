package com.github.cybellereaper.wizPets

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

data class ParticleFrame(
    val delayTicks: Long,
    val particle: Particle,
    val offset: Vector = Vector(),
    val count: Int = 1,
    val speed: Double = 0.0,
    val spread: Vector = Vector()
)

class ParticleSequence(
    val name: String,
    private val frames: List<ParticleFrame>,
    private val repeats: Int = 1,
    private val loopDelayTicks: Long = 0L
) {
    fun play(plugin: JavaPlugin, origin: Location, viewer: Player? = null): List<BukkitTask> {
        if (frames.isEmpty()) return emptyList()
        val world = origin.world ?: return emptyList()
        val tasks = mutableListOf<BukkitTask>()
        var accumulatedDelay = 0L
        repeat(repeats.coerceAtLeast(1)) {
            for (frame in frames) {
                accumulatedDelay += frame.delayTicks.coerceAtLeast(0L)
                val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val target = origin.clone().add(frame.offset)
                    if (viewer != null) {
                        viewer.spawnParticle(
                            frame.particle,
                            target,
                            frame.count,
                            frame.spread.x,
                            frame.spread.y,
                            frame.spread.z,
                            frame.speed
                        )
                    } else {
                        world.spawnParticle(
                            frame.particle,
                            target,
                            frame.count,
                            frame.spread.x,
                            frame.spread.y,
                            frame.spread.z,
                            frame.speed
                        )
                    }
                }, accumulatedDelay)
                tasks.add(task)
            }
            accumulatedDelay += loopDelayTicks.coerceAtLeast(0L)
        }
        return tasks
    }
}

data class RaycastAnimation(
    val name: String,
    val particle: Particle,
    val step: Double,
    val maxDistance: Double,
    val periodTicks: Long,
    val count: Int,
    val spread: Vector,
    val speed: Double,
    val hitRadius: Double
) {
    fun play(
        plugin: JavaPlugin,
        origin: Location,
        direction: Vector,
        onStep: (Location) -> Unit = {},
        onEntityHit: (LivingEntity) -> Unit = {}
    ): BukkitTask? {
        val world = origin.world ?: return null
        if (direction.lengthSquared() == 0.0) return null
        val normalized = direction.clone().normalize()
        val visited = mutableSetOf<LivingEntity>()
        val runnable = object : BukkitRunnable() {
            private var travelled = 0.0
            override fun run() {
                if (travelled > maxDistance) {
                    cancel()
                    return
                }
                val point = origin.clone().add(normalized.clone().multiply(travelled))
                world.spawnParticle(
                    particle,
                    point,
                    count,
                    spread.x,
                    spread.y,
                    spread.z,
                    speed
                )
                onStep(point)
                world.getNearbyEntities(point, hitRadius, hitRadius, hitRadius)
                    .filterIsInstance<LivingEntity>()
                    .filter { it.isValid }
                    .forEach {
                        if (visited.add(it)) {
                            onEntityHit(it)
                        }
                    }
                travelled += step
            }
        }
        return runnable.runTaskTimer(plugin, 0L, periodTicks.coerceAtLeast(1L))
    }
}

data class AreaParticleEffect(
    val name: String,
    val particle: Particle,
    val radius: Double,
    val pointsPerLayer: Int,
    val layers: Int,
    val layerSpacing: Double,
    val durationTicks: Long,
    val intervalTicks: Long,
    val count: Int,
    val spread: Vector,
    val speed: Double,
    val affectEntities: Boolean
) {
    fun play(
        plugin: JavaPlugin,
        center: Location,
        filter: (LivingEntity) -> Boolean = { true },
        onEntityHit: (LivingEntity) -> Unit = {}
    ): BukkitTask? {
        val world = center.world ?: return null
        val seen = mutableSetOf<LivingEntity>()
        val runnable = object : BukkitRunnable() {
            private var elapsed = 0L
            override fun run() {
                if (elapsed > durationTicks) {
                    cancel()
                    return
                }
                val base = center.clone()
                repeat(layers.coerceAtLeast(1)) { layer ->
                    val y = layer * layerSpacing
                    repeat(pointsPerLayer.coerceAtLeast(4)) { index ->
                        val angle = (2.0 * Math.PI * index) / pointsPerLayer
                        val x = cos(angle) * radius
                        val z = sin(angle) * radius
                        val target = base.clone().add(x, y, z)
                        world.spawnParticle(
                            particle,
                            target,
                            count,
                            spread.x,
                            spread.y,
                            spread.z,
                            speed
                        )
                    }
                }
                if (affectEntities) {
                    val nearby = world.getNearbyEntities(center, radius, layers * layerSpacing + 1.0, radius)
                        .filterIsInstance<LivingEntity>()
                        .filter { it.isValid && filter(it) }
                    nearby.forEach {
                        if (seen.add(it)) {
                            onEntityHit(it)
                        }
                    }
                }
                elapsed += intervalTicks
            }
        }
        return runnable.runTaskTimer(plugin, 0L, intervalTicks.coerceAtLeast(1L))
    }
}

class ParticleHandle internal constructor(private val tasks: List<BukkitTask>) {
    fun cancel() {
        tasks.forEach { it.cancel() }
    }
}

class ParticleController(private val plugin: JavaPlugin) {
    private val sequences = ConcurrentHashMap<String, ParticleSequence>()
    private val raycasts = ConcurrentHashMap<String, RaycastAnimation>()
    private val areas = ConcurrentHashMap<String, AreaParticleEffect>()

    fun registerSequence(sequence: ParticleSequence) {
        sequences[sequence.name.lowercase()] = sequence
    }

    fun registerRaycast(animation: RaycastAnimation) {
        raycasts[animation.name.lowercase()] = animation
    }

    fun registerAreaEffect(effect: AreaParticleEffect) {
        areas[effect.name.lowercase()] = effect
    }

    fun clear() {
        sequences.clear()
        raycasts.clear()
        areas.clear()
    }

    fun sequenceNames(): Set<String> = sequences.keys
    fun raycastNames(): Set<String> = raycasts.keys
    fun areaNames(): Set<String> = areas.keys

    fun playSequence(name: String, origin: Location, viewer: Player? = null): ParticleHandle? {
        val sequence = sequences[name.lowercase()] ?: return null
        val tasks = sequence.play(plugin, origin, viewer)
        return if (tasks.isEmpty()) null else ParticleHandle(tasks)
    }

    fun playRaycast(
        name: String,
        origin: Location,
        direction: Vector,
        onStep: (Location) -> Unit = {},
        onEntityHit: (LivingEntity) -> Unit = {}
    ): ParticleHandle? {
        val animation = raycasts[name.lowercase()] ?: return null
        val task = animation.play(plugin, origin, direction, onStep, onEntityHit) ?: return null
        return ParticleHandle(listOf(task))
    }

    fun playAreaEffect(
        name: String,
        center: Location,
        filter: (LivingEntity) -> Boolean = { true },
        onEntityHit: (LivingEntity) -> Unit = {}
    ): ParticleHandle? {
        val effect = areas[name.lowercase()] ?: return null
        val task = effect.play(plugin, center, filter, onEntityHit) ?: return null
        return ParticleHandle(listOf(task))
    }
}

class ParticleSequenceBuilder(private val name: String) {
    private val frames = mutableListOf<ParticleFrame>()
    private var repeats = 1
    private var loopDelay = 0L

    fun frame(
        delayTicks: Long = 0L,
        particle: Particle,
        offset: Vector = Vector(),
        count: Int = 1,
        speed: Double = 0.0,
        spread: Vector = Vector()
    ) {
        frames += ParticleFrame(delayTicks, particle, offset, count, speed, spread)
    }

    fun repeats(times: Int) {
        repeats = times
    }

    fun loopDelay(ticks: Long) {
        loopDelay = ticks
    }

    internal fun build(): ParticleSequence = ParticleSequence(name, frames.toList(), repeats, loopDelay)
}

class RaycastAnimationBuilder(private val name: String) {
    var particle: Particle = Particle.END_ROD
    var step: Double = 0.5
    var maxDistance: Double = 16.0
    var periodTicks: Long = 1L
    var count: Int = 1
    var spread: Vector = Vector()
    var speed: Double = 0.0
    var hitRadius: Double = 0.5

    internal fun build(): RaycastAnimation = RaycastAnimation(
        name,
        particle,
        step,
        maxDistance,
        periodTicks,
        count,
        spread,
        speed,
        hitRadius
    )
}

class AreaEffectBuilder(private val name: String) {
    var particle: Particle = Particle.WITCH
    var radius: Double = 3.0
    var pointsPerLayer: Int = 24
    var layers: Int = 1
    var layerSpacing: Double = 0.5
    var durationTicks: Long = 40L
    var intervalTicks: Long = 5L
    var count: Int = 2
    var spread: Vector = Vector()
    var speed: Double = 0.0
    var affectEntities: Boolean = true

    internal fun build(): AreaParticleEffect = AreaParticleEffect(
        name,
        particle,
        radius,
        pointsPerLayer,
        layers,
        layerSpacing,
        durationTicks,
        intervalTicks,
        count,
        spread,
        speed,
        affectEntities
    )
}
