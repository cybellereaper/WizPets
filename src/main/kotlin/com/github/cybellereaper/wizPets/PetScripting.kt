package com.github.cybellereaper.wizPets

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

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
        onEntityHit: (LivingEntity) -> Unit = {},
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
        onEntityHit: (LivingEntity) -> Unit = {},
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
    private val behaviors = ConcurrentHashMap<String, ScriptedPetBehavior>()
    private val scriptsDirectory: File = File(plugin.dataFolder, "scripts")

    val defaultBehaviorName: String = DEFAULT_BEHAVIOR

    val availableScripts: Set<String>
        get() = behaviors.values.map { it.name }.toSet()

    fun findBehavior(name: String): ScriptedPetBehavior? = behaviors[name.lowercase(Locale.ROOT)]

    fun behaviorFor(name: String): ScriptedPetBehavior =
        findBehavior(name) ?: behaviors[defaultBehaviorName] ?: ScriptedPetBehavior.fallback(defaultBehaviorName)

    fun reloadScripts(): Int {
        ensureDefaultScript()
        behaviors.clear()
        particleController.clear()
        val files = scriptsDirectory.listFiles { file -> file.isFile && file.extension.equals("lua", true) } ?: return 0
        var loaded = 0
        files.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { file ->
            try {
                evaluateScript(file)
                loaded++
            } catch (ex: LuaError) {
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
        val defaultScript = File(scriptsDirectory, "default.lua")
        if (!defaultScript.exists()) {
            plugin.saveResource("scripts/default.lua", false)
        }
    }

    private fun evaluateScript(file: File) {
        val globals = JsePlatform.standardGlobals()
        val registrar = ScriptRegistrar(plugin, particleController)
        val behaviorFunction = BehaviorFunction(plugin, registrar)
        val sequenceFunction = SequenceFunction(plugin, registrar)
        val raycastFunction = RaycastFunction(plugin, registrar)
        val areaFunction = AreaFunction(plugin, registrar)

        globals.set("petBehavior", behaviorFunction)
        globals.set("pet_behavior", behaviorFunction)
        globals.set("particleSequence", sequenceFunction)
        globals.set("particle_sequence", sequenceFunction)
        globals.set("raycastAnimation", raycastFunction)
        globals.set("raycast_animation", raycastFunction)
        globals.set("areaEffect", areaFunction)
        globals.set("area_effect", areaFunction)
        globals.set("logger", CoerceJavaToLua.coerce(plugin.logger))

        file.reader().use { reader ->
            val chunk = globals.load(reader, file.name)
            chunk.call()
        }

        registrar.behaviors.forEach { behavior ->
            behaviors[behavior.name.lowercase(Locale.ROOT)] = behavior
            plugin.logger.log(Level.FINE, "Registered pet behavior script {0}", behavior.name)
        }
    }

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

private class BehaviorFunction(
    private val plugin: WizPets,
    private val registrar: ScriptRegistrar
) : TwoArgFunction() {
    override fun call(nameArg: LuaValue, configArg: LuaValue): LuaValue {
        val name = nameArg.checkjstring("Behavior name is required")
        val table = configArg.checktable()

        registrar.registerBehavior(name) {
            table.get("onSummon").takeIf { it.isfunction() }?.let { fn ->
                onSummon { context -> invokeSafely(fn, context, "onSummon", name) }
            }
            (table.get("tick").takeIf { it.isfunction() } ?: table.get("onTick").takeIf { it.isfunction() })?.let { fn ->
                tick { context -> invokeSafely(fn, context, "tick", name) }
            }
            table.get("onAttack").takeIf { it.isfunction() }?.let { fn ->
                onAttack { context, entity, damage ->
                    invokeSafely(
                        fn,
                        context,
                        "onAttack",
                        name,
                        CoerceJavaToLua.coerce(entity),
                        LuaValue.valueOf(damage)
                    )
                }
            }
            table.get("onDismiss").takeIf { it.isfunction() }?.let { fn ->
                onDismiss { context -> invokeSafely(fn, context, "onDismiss", name) }
            }
            table.get("onRaycastHit").takeIf { it.isfunction() }?.let { fn ->
                onRaycastHit { context, entity ->
                    invokeSafely(
                        fn,
                        context,
                        "onRaycastHit",
                        name,
                        CoerceJavaToLua.coerce(entity)
                    )
                }
            }
            table.get("onAreaHit").takeIf { it.isfunction() }?.let { fn ->
                onAreaHit { context, entity ->
                    invokeSafely(
                        fn,
                        context,
                        "onAreaHit",
                        name,
                        CoerceJavaToLua.coerce(entity)
                    )
                }
            }
        }
        return LuaValue.NIL
    }

    private fun invokeSafely(
        function: LuaValue,
        context: PetScriptContext,
        hook: String,
        behavior: String,
        vararg extra: LuaValue
    ) {
        val args = arrayOf(CoerceJavaToLua.coerce(context)) + extra
        try {
            function.invoke(LuaValue.varargsOf(args))
        } catch (error: LuaError) {
            plugin.logger.log(
                Level.SEVERE,
                "Lua error in {0} for behavior {1}: {2}",
                arrayOf(hook, behavior, error.message)
            )
        }
    }
}

private class SequenceFunction(
    private val plugin: WizPets,
    private val registrar: ScriptRegistrar
) : TwoArgFunction() {
    override fun call(nameArg: LuaValue, configArg: LuaValue): LuaValue {
        val name = nameArg.checkjstring("Sequence name is required")
        val table = configArg.checktable()

        registrar.registerSequence(name) {
            table.get("repeats").takeIf { it.isnumber() }?.let { repeats(it.toint()) }
            table.get("loopDelay").takeIf { it.isnumber() }?.let { loopDelay(it.todouble().toLong()) }

            val framesTable = table.get("frames")
            if (framesTable.isnil() || !framesTable.istable()) {
                plugin.logger.log(Level.WARNING, "Sequence {0} missing frames table", name)
                return@registerSequence
            }

            val frames = framesTable.checktable()
            var index = LuaValue.NIL
            while (true) {
                index = frames.next(index)
                if (index.isnil()) break
                val frameValue = frames.get(index)
                if (!frameValue.istable()) continue
                val frame = frameValue.checktable()
                val particle = parseParticle(frame.get("particle"))
                if (particle == null) {
                    plugin.logger.log(Level.WARNING, "Sequence {0} frame missing valid particle", name)
                    continue
                }
                val delay = frame.get("delayTicks").optdouble(0.0).toLong()
                val count = frame.get("count").optint(1)
                val speed = frame.get("speed").optdouble(0.0)
                val offset = parseVector(frame.get("offset"))
                val spread = parseVector(frame.get("spread"))
                frame(delayTicks = delay, particle = particle, offset = offset, count = count, speed = speed, spread = spread)
            }
        }
        return LuaValue.NIL
    }
}

private class RaycastFunction(
    private val plugin: WizPets,
    private val registrar: ScriptRegistrar
) : TwoArgFunction() {
    override fun call(nameArg: LuaValue, configArg: LuaValue): LuaValue {
        val name = nameArg.checkjstring("Raycast name is required")
        val table = configArg.checktable()

        registrar.registerRaycast(name) {
            parseParticle(table.get("particle"))?.let { particle = it }
            table.get("step").takeIf { it.isnumber() }?.let { step = it.todouble() }
            table.get("maxDistance").takeIf { it.isnumber() }?.let { maxDistance = it.todouble() }
            table.get("periodTicks").takeIf { it.isnumber() }?.let { periodTicks = it.todouble().toLong() }
            table.get("count").takeIf { it.isnumber() }?.let { count = it.toint() }
            table.get("spread").takeIf { it.istable() }?.let { spread = parseVector(it) }
            table.get("speed").takeIf { it.isnumber() }?.let { speed = it.todouble() }
            table.get("hitRadius").takeIf { it.isnumber() }?.let { hitRadius = it.todouble() }
        }
        return LuaValue.NIL
    }
}

private class AreaFunction(
    private val plugin: WizPets,
    private val registrar: ScriptRegistrar
) : TwoArgFunction() {
    override fun call(nameArg: LuaValue, configArg: LuaValue): LuaValue {
        val name = nameArg.checkjstring("Area effect name is required")
        val table = configArg.checktable()

        registrar.registerArea(name) {
            parseParticle(table.get("particle"))?.let { particle = it }
            table.get("radius").takeIf { it.isnumber() }?.let { radius = it.todouble() }
            table.get("pointsPerLayer").takeIf { it.isnumber() }?.let { pointsPerLayer = it.toint() }
            table.get("layers").takeIf { it.isnumber() }?.let { layers = it.toint() }
            table.get("layerSpacing").takeIf { it.isnumber() }?.let { layerSpacing = it.todouble() }
            table.get("durationTicks").takeIf { it.isnumber() }?.let { durationTicks = it.todouble().toLong() }
            table.get("intervalTicks").takeIf { it.isnumber() }?.let { intervalTicks = it.todouble().toLong() }
            table.get("count").takeIf { it.isnumber() }?.let { count = it.toint() }
            table.get("spread").takeIf { it.istable() }?.let { spread = parseVector(it) }
            table.get("speed").takeIf { it.isnumber() }?.let { speed = it.todouble() }
            table.get("affectEntities").takeIf { it.isboolean() }?.let { affectEntities = it.toboolean() }
        }
        return LuaValue.NIL
    }
}

private fun parseParticle(value: LuaValue): Particle? {
    if (!value.isstring()) return null
    return try {
        Particle.valueOf(value.checkjstring().uppercase(Locale.ROOT))
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun parseVector(value: LuaValue): Vector {
    if (!value.istable()) return Vector()
    val table = value.checktable()
    return if (table.length() >= 3) {
        Vector(
            table.get(1).optdouble(0.0),
            table.get(2).optdouble(0.0),
            table.get(3).optdouble(0.0)
        )
    } else {
        Vector(
            table.get("x").optdouble(0.0),
            table.get("y").optdouble(0.0),
            table.get("z").optdouble(0.0)
        )
    }
}
