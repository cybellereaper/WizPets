package com.github.cybellereaper.wizPets

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
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

data class PetEntityConfig(
    val entityType: EntityType,
    val invisible: Boolean,
    val silent: Boolean,
    val baby: Boolean,
    val followSpeed: Double,
    val followStartDistance: Double,
    val followStopDistance: Double,
    val leashDistance: Double,
    val mountAllowed: Boolean,
    val flightAllowed: Boolean
)

class ScriptedPetMove(
    val name: String,
    val cooldownTicks: Int,
    val range: Double,
    private val selectHandler: (PetScriptContext) -> LivingEntity?,
    private val executeHandler: (PetScriptContext, LivingEntity) -> Unit
) {
    fun selectTarget(context: PetScriptContext): LivingEntity? = selectHandler(context)
    fun execute(context: PetScriptContext, entity: LivingEntity) = executeHandler(context, entity)
}

class ScriptedPetDefinition(
    val id: String,
    val behavior: ScriptedPetBehavior,
    val displayNameProvider: (PetScriptContext) -> String,
    val entityConfig: PetEntityConfig,
    val baseStats: StatSet,
    val moves: List<ScriptedPetMove>
)

class PetScriptContext internal constructor(
    private val plugin: WizPets,
    val pet: Pet,
    private val particles: ParticleController
) {
    private val definition: ScriptedPetDefinition
        get() = pet.currentDefinition

    val owner: Player
        get() = pet.owner

    val location: Location
        get() = pet.location

    val entity: Mob
        get() = pet.mob

    fun definitionId(): String = definition.id

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

    fun setDisplayName(name: String) = pet.updateDisplayName(name)

    fun stat(stat: StatType): Double = pet.getStat(stat)

    fun stat(name: String): Double = StatType.entries.firstOrNull { it.name.equals(name, true) || it.displayName.equals(name, true) }
        ?.let { stat(it) } ?: 0.0

    fun attack(target: LivingEntity, damage: Double) = pet.performAttack(target, damage)

    fun nearestEnemy(radius: Double): LivingEntity? = pet.findNearestEnemy(radius)

    fun ownerTarget(range: Double): LivingEntity? = owner.getTargetEntity(range) as? LivingEntity

    fun directionTo(entity: LivingEntity): Vector = entity.location.toVector().subtract(location.toVector()).normalize()

    fun moveTo(location: Location, speed: Double = definition.entityConfig.followSpeed) {
        pet.navigateTo(location, speed)
    }

    fun stopNavigation() = pet.stopNavigation()

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
    private val definitions = ConcurrentHashMap<String, ScriptedPetDefinition>()
    private val scriptsDirectory: File = File(plugin.dataFolder, "scripts")

    val defaultDefinitionId: String = DEFAULT_DEFINITION

    val availableScripts: Set<String>
        get() = definitions.values.map { it.id }.toSet()

    fun findDefinition(name: String): ScriptedPetDefinition? = definitions[name.lowercase(Locale.ROOT)]

    fun definitionFor(name: String): ScriptedPetDefinition =
        findDefinition(name) ?: definitions[defaultDefinitionId] ?: fallbackDefinition()

    fun reloadScripts(): Int {
        ensureDefaultScript()
        definitions.clear()
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
        if (!definitions.containsKey(defaultDefinitionId)) {
            val fallback = fallbackDefinition()
            definitions[fallback.id.lowercase(Locale.ROOT)] = fallback
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

    private fun fallbackDefinition(): ScriptedPetDefinition {
        val behavior = ScriptedPetBehavior.fallback(DEFAULT_BEHAVIOR_NAME)
        val entityConfig = PetEntityConfig(
            entityType = EntityType.WOLF,
            invisible = true,
            silent = true,
            baby = false,
            followSpeed = 1.2,
            followStartDistance = 3.0,
            followStopDistance = 1.5,
            leashDistance = 32.0,
            mountAllowed = true,
            flightAllowed = false
        )
        return ScriptedPetDefinition(
            id = DEFAULT_DEFINITION,
            behavior = behavior,
            displayNameProvider = { ctx -> "${ctx.owner.name}'s Familiar" },
            entityConfig = entityConfig,
            baseStats = StatSet(health = 40.0, attack = 6.0, defense = 3.0, magic = 5.0),
            moves = emptyList()
        )
    }

    private fun evaluateScript(file: File) {
        val globals = JsePlatform.standardGlobals()
        val registrar = ScriptRegistrar(plugin, particleController, definitions)
        val petFunction = PetDefinitionFunction(plugin, registrar)
        val sequenceFunction = SequenceFunction(plugin, registrar)
        val raycastFunction = RaycastFunction(plugin, registrar)
        val areaFunction = AreaFunction(plugin, registrar)

        globals.set("pet", petFunction)
        globals.set("pet_definition", petFunction)
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
    }

    companion object {
        private const val DEFAULT_DEFINITION = "default"
        private const val DEFAULT_BEHAVIOR_NAME = "default"
    }
}

private class ScriptRegistrar(
    private val plugin: WizPets,
    private val particleController: ParticleController,
    private val definitions: MutableMap<String, ScriptedPetDefinition>
) {
    fun registerPet(table: LuaTable) {
        val idValue = table.get("id").takeIf { it.isstring() } ?: table.get("name").takeIf { it.isstring() }
        if (idValue.isnil()) {
            throw LuaError("Pet definition requires an 'id' field")
        }
        val id = idValue.checkjstring()
        val key = id.lowercase(Locale.ROOT)
        val behavior = buildBehaviorFromTable(plugin, id, table.get("behavior"))
        val entityConfig = parseEntityConfig(plugin, table.get("entity"))
        val baseStats = parseStats(table.get("stats"))
        val moves = parseMoves(plugin, table.get("moves"))
        val displayNameProvider = parseDisplayNameProvider(plugin, id, table.get("displayName"))

        val definition = ScriptedPetDefinition(
            id = id,
            behavior = behavior,
            displayNameProvider = displayNameProvider,
            entityConfig = entityConfig,
            baseStats = baseStats,
            moves = moves
        )
        definitions[key] = definition
        plugin.logger.log(Level.FINE, "Registered pet definition {0}", id)
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

private class PetDefinitionFunction(
    private val plugin: WizPets,
    private val registrar: ScriptRegistrar
) : OneArgFunction() {
    override fun call(configArg: LuaValue): LuaValue {
        val table = configArg.checktable()
        registrar.registerPet(table)
        return LuaValue.NIL
    }
}

private fun buildBehaviorFromTable(
    plugin: WizPets,
    name: String,
    value: LuaValue
): ScriptedPetBehavior {
    if (!value.istable()) {
        return ScriptedPetBehavior.fallback(name)
    }
    val builder = PetBehaviorBuilder(name)
    val table = value.checktable()

    table.get("onSummon").takeIf { it.isfunction() }?.let { fn ->
        builder.onSummon { context -> invokeBehaviorSafely(plugin, fn, context, "onSummon", name) }
    }
    (table.get("tick").takeIf { it.isfunction() } ?: table.get("onTick").takeIf { it.isfunction() })?.let { fn ->
        builder.tick { context -> invokeBehaviorSafely(plugin, fn, context, "tick", name) }
    }
    table.get("onAttack").takeIf { it.isfunction() }?.let { fn ->
        builder.onAttack { context, entity, damage ->
            invokeBehaviorSafely(plugin, fn, context, "onAttack", name, CoerceJavaToLua.coerce(entity), LuaValue.valueOf(damage))
        }
    }
    table.get("onDismiss").takeIf { it.isfunction() }?.let { fn ->
        builder.onDismiss { context -> invokeBehaviorSafely(plugin, fn, context, "onDismiss", name) }
    }
    table.get("onRaycastHit").takeIf { it.isfunction() }?.let { fn ->
        builder.onRaycastHit { context, entity ->
            invokeBehaviorSafely(plugin, fn, context, "onRaycastHit", name, CoerceJavaToLua.coerce(entity))
        }
    }
    table.get("onAreaHit").takeIf { it.isfunction() }?.let { fn ->
        builder.onAreaHit { context, entity ->
            invokeBehaviorSafely(plugin, fn, context, "onAreaHit", name, CoerceJavaToLua.coerce(entity))
        }
    }
    return builder.build()
}

private fun invokeBehaviorSafely(
    plugin: WizPets,
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

private fun parseDisplayNameProvider(plugin: WizPets, id: String, value: LuaValue): (PetScriptContext) -> String {
    if (!value.isfunction()) {
        return { ctx -> "${ctx.owner.name}'s Familiar" }
    }
    val function = value.checkfunction()
    return { context ->
        try {
            val result = function.invoke(
                LuaValue.varargsOf(arrayOf(CoerceJavaToLua.coerce(context)))
            ).arg1()
            if (result.isstring()) result.checkjstring() else "${context.owner.name}'s Familiar"
        } catch (error: LuaError) {
            plugin.logger.log(Level.SEVERE, "displayName function failed for pet {0}: {1}", arrayOf(id, error.message))
            "${context.owner.name}'s Familiar"
        }
    }
}

private fun parseEntityConfig(plugin: WizPets, value: LuaValue): PetEntityConfig {
    val table = value.takeIf { it.istable() }?.checktable()
    val typeName = table?.get("type")?.optjstring("WOLF") ?: "WOLF"
    val type = runCatching { EntityType.valueOf(typeName.uppercase(Locale.ROOT)) }.getOrNull()
    val entityType = if (type != null && Mob::class.java.isAssignableFrom(type.entityClass)) {
        type
    } else {
        plugin.logger.log(Level.WARNING, "Unsupported entity type {0} for pet, defaulting to WOLF", typeName)
        EntityType.WOLF
    }
    val invisible = table?.get("invisible")?.optboolean(true) ?: true
    val silent = table?.get("silent")?.optboolean(true) ?: true
    val baby = table?.get("baby")?.optboolean(false) ?: false
    val followSpeed = table?.get("followSpeed")?.optdouble(1.25) ?: table?.get("speed")?.optdouble(1.25) ?: 1.25
    val followStart = table?.get("followStart")?.optdouble(3.0) ?: table?.get("followDistance")?.optdouble(3.0) ?: 3.0
    val followStop = table?.get("followStop")?.optdouble(1.5) ?: 1.5
    val leash = table?.get("leashDistance")?.optdouble(32.0) ?: 32.0
    val mountAllowed = table?.get("mount")?.optboolean(true) ?: true
    val flightAllowed = table?.get("flight")?.optboolean(false) ?: false
    return PetEntityConfig(
        entityType = entityType,
        invisible = invisible,
        silent = silent,
        baby = baby,
        followSpeed = followSpeed,
        followStartDistance = followStart,
        followStopDistance = followStop,
        leashDistance = leash,
        mountAllowed = mountAllowed,
        flightAllowed = flightAllowed
    )
}

private fun parseStats(value: LuaValue): StatSet {
    if (!value.istable()) {
        return StatSet(health = 40.0, attack = 6.0, defense = 3.0, magic = 5.0)
    }
    val table = value.checktable()
    return StatSet(
        health = table.get("health").optdouble(40.0),
        attack = table.get("attack").optdouble(6.0),
        defense = table.get("defense").optdouble(3.0),
        magic = table.get("magic").optdouble(5.0)
    )
}

private fun parseMoves(plugin: WizPets, value: LuaValue): List<ScriptedPetMove> {
    if (!value.istable()) return emptyList()
    val table = value.checktable()
    val moves = mutableListOf<ScriptedPetMove>()
    var key = LuaValue.NIL
    while (true) {
        key = table.next(key)
        if (key.isnil()) break
        val entry = table.get(key)
        if (!entry.istable()) continue
        val moveTable = entry.checktable()
        val name = moveTable.get("name").optjstring("Unnamed Move")
        val cooldown = moveTable.get("cooldown").optint(40).coerceAtLeast(1)
        val range = moveTable.get("range").optdouble(6.0)
        val selectFunction = moveTable.get("select")
        val executeFunction = moveTable.get("execute")
        if (!executeFunction.isfunction()) {
            plugin.logger.log(Level.WARNING, "Move {0} missing execute function", name)
            continue
        }
        val selectHandler: (PetScriptContext) -> LivingEntity? = if (selectFunction.isfunction()) {
            { context ->
                try {
                    val result = selectFunction.invoke(
                        LuaValue.varargsOf(arrayOf(CoerceJavaToLua.coerce(context)))
                    ).arg1()
                    if (result.isnil()) {
                        null
                    } else {
                        try {
                            result.touserdata(LivingEntity::class.java) as? LivingEntity ?: result.touserdata() as? LivingEntity
                        } catch (_: LuaError) {
                            null
                        }
                    }
                } catch (error: LuaError) {
                    plugin.logger.log(Level.SEVERE, "select function failed for move {0}: {1}", arrayOf(name, error.message))
                    null
                }
            }
        } else {
            { context -> context.nearestEnemy(range) }
        }
        val executeHandler: (PetScriptContext, LivingEntity) -> Unit = { context, target ->
            try {
                executeFunction.invoke(
                    LuaValue.varargsOf(
                        arrayOf(
                            CoerceJavaToLua.coerce(context),
                            CoerceJavaToLua.coerce(target)
                        )
                    )
                )
            } catch (error: LuaError) {
                plugin.logger.log(Level.SEVERE, "execute function failed for move {0}: {1}", arrayOf(name, error.message))
            }
        }
        moves += ScriptedPetMove(name, cooldown, range, selectHandler, executeHandler)
    }
    return moves
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
