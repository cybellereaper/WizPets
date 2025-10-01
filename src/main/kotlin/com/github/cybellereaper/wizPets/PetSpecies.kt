package com.github.cybellereaper.wizPets

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

enum class Element(val displayName: String) {
    FIRE("Fire"),
    WATER("Water"),
    STORM("Storm"),
    NATURE("Nature"),
    ICE("Ice"),
    MYTH("Myth"),
    ARCANE("Arcane")
}

data class PetSpecies(
    val id: String,
    val displayName: String,
    val element: Element,
    val baseStats: StatSet,
    val entityType: EntityType,
    val modelItem: Material,
    val description: String,
    val talentPool: List<String>
)

object PetSpeciesRegistry {
    private val species = mutableMapOf<String, PetSpecies>()

    fun load(plugin: JavaPlugin) {
        val section = plugin.config.getConfigurationSection("species") ?: return
        species.clear()
        for (id in section.getKeys(false)) {
            val speciesSection = section.getConfigurationSection(id) ?: continue
            parseSpecies(id, speciesSection)?.let { species[id.lowercase()] = it }
        }
    }

    fun reload(plugin: JavaPlugin) {
        plugin.reloadConfig()
        load(plugin)
    }

    fun getSpecies(id: String): PetSpecies? = species[id.lowercase()]

    fun getAllSpecies(): Collection<PetSpecies> = species.values

    private fun parseSpecies(id: String, section: ConfigurationSection): PetSpecies? {
        val name = section.getString("name") ?: id.replaceFirstChar { it.uppercase() }
        val elementName = section.getString("element")?.uppercase() ?: "ARCANE"
        val element = runCatching { Element.valueOf(elementName) }.getOrDefault(Element.ARCANE)
        val entityTypeName = section.getString("entity-type", "ARMOR_STAND")?.uppercase()
        val entityType = entityTypeName?.let { runCatching { EntityType.valueOf(it) }.getOrNull() }
            ?: EntityType.ARMOR_STAND
        val modelMaterial = section.getString("model", "AMETHYST_SHARD")
        val material = Material.matchMaterial(modelMaterial ?: "") ?: Material.AMETHYST_SHARD
        val statsSection = section.getConfigurationSection("base-stats")
        val baseStats = StatSet(
            health = statsSection?.getDouble("health", 40.0) ?: 40.0,
            attack = statsSection?.getDouble("attack", 6.0) ?: 6.0,
            defense = statsSection?.getDouble("defense", 4.0) ?: 4.0,
            magic = statsSection?.getDouble("magic", 5.0) ?: 5.0,
            speed = statsSection?.getDouble("speed", 5.0) ?: 5.0
        )
        val description = section.getString("description") ?: "A mysterious companion."
        val talentPool = section.getStringList("talents").takeIf { it.isNotEmpty() } ?: listOf(
            "healing_aura",
            "guardian_shell",
            "arcane_burst"
        )
        return PetSpecies(
            id = id,
            displayName = name,
            element = element,
            baseStats = baseStats,
            entityType = entityType,
            modelItem = material,
            description = description,
            talentPool = talentPool
        )
    }
}
