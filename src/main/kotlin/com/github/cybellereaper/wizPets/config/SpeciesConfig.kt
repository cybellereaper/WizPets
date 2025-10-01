package com.github.cybellereaper.wizPets.config

import com.github.cybellereaper.wizPets.pet.model.Element
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.model.StatLine
import com.github.cybellereaper.wizPets.pet.model.StatSheet
import com.github.cybellereaper.wizPets.talent.TalentId
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

class SpeciesConfig(private val plugin: JavaPlugin) {

    fun load(): Map<String, PetSpecies> {
        val section = plugin.config.getConfigurationSection("species") ?: return emptyMap()
        return section.getKeys(false).associateWith { key ->
            parseSpecies(key, section.getConfigurationSection(key)!!)
        }
    }

    private fun parseSpecies(id: String, section: ConfigurationSection): PetSpecies {
        val displayName = section.getString("name") ?: id
        val element = section.getString("element")?.let { Element.valueOf(it.uppercase()) } ?: Element.NATURE
        val baseLevel = section.getInt("level", 1)
        val baseStats = parseStatSheet(section.getConfigurationSection("stats"))
        val talents = section.getStringList("talents")
            .mapNotNull { runCatching { TalentId.valueOf(it.uppercase()) }.getOrNull() }
        return PetSpecies(
            id = id,
            displayName = displayName,
            element = element,
            statSheet = baseStats,
            baseLevel = baseLevel,
            defaultTalents = if (talents.isEmpty()) DEFAULT_TALENTS else talents,
        )
    }

    private fun parseStatSheet(section: ConfigurationSection?): StatSheet {
        val stamina = section?.getInt("stamina", 10) ?: 10
        val power = section?.getInt("power", 10) ?: 10
        val defense = section?.getInt("defense", 10) ?: 10
        val focus = section?.getInt("focus", 10) ?: 10
        return StatSheet(
            stamina = StatLine(stamina, 0, 0),
            power = StatLine(power, 0, 0),
            defense = StatLine(defense, 0, 0),
            focus = StatLine(focus, 0, 0),
        )
    }

    companion object {
        private val DEFAULT_TALENTS = listOf(TalentId.SENTINEL, TalentId.GATHERER)
    }
}

