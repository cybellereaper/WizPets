package com.github.cybellereaper.wizPets

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptException
import kotlin.script.experimental.jsr223.KotlinJsr223JvmLocalScriptEngineFactory

class ScriptedPetBehavior(
    val name: String,
    private val onSummonHandler: (PetScriptContext) -> Unit,
    private val onTickHandler: (PetScriptContext) -> Unit,
    private val onAttackHandler: (PetScriptContext, LivingEntity, Double) -> Unit,
    private val onDismissHandler: (PetScriptContext) -> Unit,
    private val onRaycastHitHandler: (PetScriptContext, LivingEntity) -> Unit,
    private val onAreaHitHandler: (PetScriptContext, LivingEntity) -> Unit
) {
    fun onSummon(context: PetScriptContext) = onSummonHandler(context)
    fun onTick(context: PetScriptContext) = onTickHandler(context)
    fun onAttack(context: PetScriptContext, target: LivingEntity, damage: Double) = onAttackHandler(context, target, damage)
    fun onDismiss(context: PetScriptContext) = onDismissHandler(context)
    fun onRaycastHit(context: PetScriptContext, entity: LivingEntity) = onRaycastHitHandler(context, entity)
    fun onAreaHit(context: PetScriptContext, entity: LivingEntity) = onAreaHitHandler(context, entity)

    companion object {
        fun fallback(name: String): ScriptedPetBehavior = PetBehaviorBuilder(name).build()
    }
}

class PetScriptContext internal constructor(
    private val plugin: WizPets,
    val pet: Pet,
    private val particles: ParticleController
) {
    val owner: Player
        get() = pet.owner

    val location: Location
        get() = pet.location

    fun playSequence(name: String, origin: Location = location, viewer: Player? = null): ParticleHandle? {
        return particles.playSequence(name, origin, viewer)
    }

    fun playRaycast(
        name: String,
        origin: Location = location,
        direction: Vector = owner.location.direction,
        onStep: (Location) -> Unit = {},
        onEntityHit: (LivingEntity) -> Unit = {}
    ): ParticleHandle? {
        return particles.playRaycast(name, origin, direction, onStep) { entity ->
            onEntityHit(entity)
            pet.handleRaycastHit(entity)
        }
    }

    fun playAreaEffect(
        name: String,
        center: Location = location,
        filter: (LivingEntity) -> Boolean = { it != owner },
        onEntityHit: (LivingEntity) -> Unit = {}
    ): ParticleHandle? {
        return particles.playAreaEffect(name, center, filter) { entity ->
            onEntityHit(entity)
            pet.handleAreaHit(entity)
        }
    }

    fun healOwner(amount: Double) = pet.heal(amount)

    fun grantShield(hearts: Double) = pet.grantAbsorption(hearts)

    fun debug(message: String) = pet.debug(message)

    fun runLater(delayTicks: Long, block: () -> Unit): BukkitTask =
        plugin.server.scheduler.runTaskLater(plugin, Runnable(block), delayTicks)

    fun runRepeating(initialDelay: Long, periodTicks: Long, block: () -> Unit): BukkitTask =
        plugin.server.scheduler.runTaskTimer(plugin, Runnable(block), initialDelay, periodTicks)
}

class PetBehaviorBuilder(private val name: String) {
    private var onSummon: (PetScriptContext) -> Unit = {}
    private var onTick: (PetScriptContext) -> Unit = {}
    private var onAttack: (PetScriptContext, LivingEntity, Double) -> Unit = { _, _, _ -> }
    private var onDismiss: (PetScriptContext) -> Unit = {}
    private var onRaycastHit: (PetScriptContext, LivingEntity) -> Unit = { _, _ -> }
    private var onAreaHit: (PetScriptContext, LivingEntity) -> Unit = { _, _ -> }

    fun onSummon(block: PetScriptContext.() -> Unit) {
        onSummon = { context -> context.block() }
    }

    fun tick(block: PetScriptContext.() -> Unit) {
        onTick = { context -> context.block() }
    }

    fun onAttack(block: PetScriptContext.(LivingEntity, Double) -> Unit) {
        onAttack = { context, entity, damage -> context.block(entity, damage) }
    }

    fun onDismiss(block: PetScriptContext.() -> Unit) {
        onDismiss = { context -> context.block() }
    }

    fun onRaycastHit(block: PetScriptContext.(LivingEntity) -> Unit) {
        onRaycastHit = { context, entity -> context.block(entity) }
    }

    fun onAreaHit(block: PetScriptContext.(LivingEntity) -> Unit) {
        onAreaHit = { context, entity -> context.block(entity) }
    }

    internal fun build(): ScriptedPetBehavior = ScriptedPetBehavior(
        name,
        onSummon,
        onTick,
        onAttack,
        onDismiss,
        onRaycastHit,
        onAreaHit
    )
}

class PetScriptManager(
    private val plugin: WizPets,
    private val particleController: ParticleController
) {
    private val factory = KotlinJsr223JvmLocalScriptEngineFactory()
    private val behaviors = ConcurrentHashMap<String, ScriptedPetBehavior>()
    private val scriptsDirectory: File = File(plugin.dataFolder, "scripts")

    val defaultBehaviorName: String = DEFAULT_BEHAVIOR

    val availableScripts: Set<String>
        get() = behaviors.values.map { it.name }.toSet()

    fun findBehavior(name: String): ScriptedPetBehavior? = behaviors[name.lowercase()]

    fun behaviorFor(name: String): ScriptedPetBehavior =
        findBehavior(name) ?: behaviors[defaultBehaviorName] ?: ScriptedPetBehavior.fallback(defaultBehaviorName)

    fun reloadScripts(): Int {
        ensureDefaultScript()
        behaviors.clear()
        particleController.clear()
        val files = scriptsDirectory.listFiles { file -> file.isFile && file.extension.equals("kts", true) } ?: return 0
        var loaded = 0
        files.sortedBy { it.name.lowercase() }.forEach { file ->
            try {
                evaluateScript(file)
                loaded++
            } catch (ex: ScriptException) {
                plugin.logger.log(Level.SEVERE, "Failed to evaluate pet script {0}: {1}", arrayOf(file.name, ex.message))
            } catch (ex: Exception) {
                plugin.logger.log(Level.SEVERE, "Unexpected error loading pet script {0}", file.name)
                plugin.logger.log(Level.FINE, "Stack trace", ex)
            }
        }
        if (!behaviors.containsKey(defaultBehaviorName)) {
            behaviors[defaultBehaviorName] = ScriptedPetBehavior.fallback(defaultBehaviorName)
        }
        return loaded
    }

    private fun ensureDefaultScript() {
        if (!scriptsDirectory.exists()) {
            scriptsDirectory.mkdirs()
        }
        val defaultScript = File(scriptsDirectory, "default.kts")
        if (!defaultScript.exists()) {
            plugin.saveResource("scripts/default.kts", false)
        }
    }

    private fun evaluateScript(file: File) {
        val engine = newEngine()
        val registrar = ScriptRegistrar(plugin, particleController)
        val context = engine.context
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
        context.getBindings(ScriptContext.ENGINE_SCOPE).apply {
            put("petBehavior", BehaviorRegistrar(registrar))
            put("particleSequence", SequenceRegistrar(registrar))
            put("raycastAnimation", RaycastRegistrar(registrar))
            put("areaEffect", AreaRegistrar(registrar))
            put("logger", plugin.logger)
        }
        file.reader().use { reader ->
            engine.eval(reader)
        }
        registrar.behaviors.forEach { behavior ->
            behaviors[behavior.name.lowercase()] = behavior
            plugin.logger.log(Level.FINE, "Registered pet behavior script {0}", behavior.name)
        }
    }

    private fun newEngine(): ScriptEngine = factory.scriptEngine

    companion object {
        private const val DEFAULT_BEHAVIOR = "default"
    }
}

private class ScriptRegistrar(
    private val plugin: JavaPlugin,
    private val particleController: ParticleController
) {
    val behaviors = mutableListOf<ScriptedPetBehavior>()

    fun registerBehavior(name: String, builder: PetBehaviorBuilder.() -> Unit) {
        val behavior = PetBehaviorBuilder(name).apply(builder).build()
        behaviors += behavior
    }

    fun registerSequence(name: String, builder: ParticleSequenceBuilder.() -> Unit) {
        val sequence = ParticleSequenceBuilder(name).apply(builder).build()
        particleController.registerSequence(sequence)
        plugin.logger.log(Level.FINE, "Registered particle sequence {0}", name)
    }

    fun registerRaycast(name: String, builder: RaycastAnimationBuilder.() -> Unit) {
        val raycast = RaycastAnimationBuilder(name).apply(builder).build()
        particleController.registerRaycast(raycast)
        plugin.logger.log(Level.FINE, "Registered raycast animation {0}", name)
    }

    fun registerArea(name: String, builder: AreaEffectBuilder.() -> Unit) {
        val area = AreaEffectBuilder(name).apply(builder).build()
        particleController.registerAreaEffect(area)
        plugin.logger.log(Level.FINE, "Registered area particle effect {0}", name)
    }
}

private class BehaviorRegistrar(private val registrar: ScriptRegistrar) {
    operator fun invoke(name: String, builder: PetBehaviorBuilder.() -> Unit) {
        registrar.registerBehavior(name, builder)
    }
}

private class SequenceRegistrar(private val registrar: ScriptRegistrar) {
    operator fun invoke(name: String, builder: ParticleSequenceBuilder.() -> Unit) {
        registrar.registerSequence(name, builder)
    }
}

private class RaycastRegistrar(private val registrar: ScriptRegistrar) {
    operator fun invoke(name: String, builder: RaycastAnimationBuilder.() -> Unit) {
        registrar.registerRaycast(name, builder)
    }
}

private class AreaRegistrar(private val registrar: ScriptRegistrar) {
    operator fun invoke(name: String, builder: AreaEffectBuilder.() -> Unit) {
        registrar.registerArea(name, builder)
    }
}
