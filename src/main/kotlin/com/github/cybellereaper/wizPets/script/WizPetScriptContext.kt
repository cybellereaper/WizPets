package com.github.cybellereaper.wizPets.script

import com.github.cybellereaper.wizPets.pet.model.Element
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.model.StatLine
import com.github.cybellereaper.wizPets.pet.model.StatSheet
import com.github.cybellereaper.wizPets.talent.ArtisanTalent
import com.github.cybellereaper.wizPets.talent.GardenerTalent
import com.github.cybellereaper.wizPets.talent.GathererTalent
import com.github.cybellereaper.wizPets.talent.MedicTalent
import com.github.cybellereaper.wizPets.talent.SentinelTalent
import com.github.cybellereaper.wizPets.talent.Talent
import com.github.cybellereaper.wizPets.talent.TalentContext
import com.github.cybellereaper.wizPets.talent.TalentId
import com.github.cybellereaper.wizPets.talent.TalentRegistry
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger
import javax.script.Bindings
import javax.script.SimpleBindings
import kotlin.script.experimental.jsr223.KotlinJsr223JvmLocalScriptEngineFactory

/**
 * Loads Kotlin scripts from the plugin data directory and exposes a DSL for registering species and talents.
 */
class WizPetScriptRuntime(private val plugin: JavaPlugin) {

    data class RuntimeState(
        val species: Map<String, PetSpecies>,
        val talents: TalentRegistry,
    )

    private val logger: Logger = plugin.logger
    private val scriptsDirectory: File = File(plugin.dataFolder, "scripts")
    private val engineFactory = KotlinJsr223JvmLocalScriptEngineFactory()

    fun load(): RuntimeState {
        if (!scriptsDirectory.exists()) {
            scriptsDirectory.mkdirs()
        }
        ensureDefaultScript()

        val context = WizPetScriptContext(logger)
        val engine = engineFactory.scriptEngine

        scriptsDirectory.listFiles { file -> file.extension.equals("kts", ignoreCase = true) }
            ?.sortedBy(File::getName)
            ?.forEach { script ->
                val bindings: Bindings = SimpleBindings(mapOf("wizpets" to context))
                try {
                    engine.eval(script.readText(), bindings)
                    logger.info("Loaded script ${script.name}")
                } catch (ex: Exception) {
                    logger.severe("Failed to load script ${script.name}: ${ex.message}")
                    throw ex
                }
            }

        return context.build()
    }

    private fun ensureDefaultScript() {
        val hasScripts = scriptsDirectory.listFiles { file -> file.extension.equals("kts", true) }?.isNotEmpty() == true
        if (!hasScripts) {
            plugin.saveResource("scripts/default.wizpet.kts", false)
        }
    }
}

class WizPetScriptContext(private val logger: Logger) {
    private val speciesRegistry = linkedMapOf<String, PetSpecies>()
    private val talents = TalentRegistry.builder()

    init {
        registerBuiltinTalents()
    }

    fun species(id: String, block: SpeciesBuilder.() -> Unit) {
        val builder = SpeciesBuilder(id).apply(block)
        val built = builder.build()
        speciesRegistry[id] = built
        logger.fine("Registered species ${built.id}")
    }

    fun talent(id: String, handler: (TalentContext) -> Unit) {
        talent(TalentId(id), handler)
    }

    fun talent(id: TalentId, handler: (TalentContext) -> Unit) {
        talents.register(id, Talent(handler))
        logger.fine("Registered talent ${id.value}")
    }

    fun build(): WizPetScriptRuntime.RuntimeState = WizPetScriptRuntime.RuntimeState(
        species = speciesRegistry.toMap(),
        talents = talents.build(),
    )

    private fun registerBuiltinTalents() {
        talents.register(TalentId.SENTINEL, Talent(SentinelTalent::perform))
        talents.register(TalentId.MEDIC, Talent(MedicTalent::perform))
        talents.register(TalentId.GARDENER, Talent(GardenerTalent::perform))
        talents.register(TalentId.GATHERER, Talent(GathererTalent::perform))
        talents.register(TalentId.ARTISAN, Talent(ArtisanTalent::perform))
    }
}

class SpeciesBuilder(private val id: String) {
    var displayName: String = id.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    var element: Element = Element.NATURE
    var baseLevel: Int = 1
    private var baseStats: StatSheet = defaultStats()
    private var defaultTalents: List<TalentId> = listOf(TalentId.SENTINEL)

    fun stats(block: StatSheetBuilder.() -> Unit) {
        baseStats = StatSheetBuilder().apply(block).build()
    }

    fun defaultTalents(vararg talents: TalentId) {
        if (talents.isNotEmpty()) {
            defaultTalents = talents.toList()
        }
    }

    fun build(): PetSpecies = PetSpecies(
        id = id,
        displayName = displayName,
        element = element,
        statSheet = baseStats,
        baseLevel = baseLevel,
        defaultTalents = defaultTalents,
    )

    private fun defaultStats(): StatSheet = StatSheet(
        stamina = StatLine(base = 10, effort = 0, individual = 0),
        power = StatLine(base = 10, effort = 0, individual = 0),
        defense = StatLine(base = 10, effort = 0, individual = 0),
        focus = StatLine(base = 10, effort = 0, individual = 0),
    )
}

class StatSheetBuilder {
    var stamina: Int = 10
    var power: Int = 10
    var defense: Int = 10
    var focus: Int = 10

    fun build(): StatSheet = StatSheet(
        stamina = StatLine(base = stamina, effort = 0, individual = 0),
        power = StatLine(base = power, effort = 0, individual = 0),
        defense = StatLine(base = defense, effort = 0, individual = 0),
        focus = StatLine(base = focus, effort = 0, individual = 0),
    )
}
