package com.github.cybellereaper.wizPets.pet.service

import com.github.cybellereaper.wizPets.pet.model.PetInstance
import com.github.cybellereaper.wizPets.pet.model.PetSnapshot
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.model.StatInvestment
import com.github.cybellereaper.wizPets.pet.model.StatInvestments
import com.github.cybellereaper.wizPets.talent.TalentId
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class PetRepository(private val directory: File) {
    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    private val speciesIndex = mutableMapOf<String, PetSpecies>()

    fun refreshSpeciesIndex(newIndex: Map<String, PetSpecies>) {
        speciesIndex.clear()
        speciesIndex.putAll(newIndex)
    }

    fun load(speciesIndex: Map<String, PetSpecies>): Map<UUID, MutableList<PetInstance>> {
        refreshSpeciesIndex(speciesIndex)
        return directory.listFiles { file -> file.extension == "yml" }
            ?.mapNotNull(::loadFile)
            ?.toMap()
            ?: emptyMap()
    }

    fun save(ownerId: UUID, pets: List<PetInstance>) {
        val file = File(directory, "$ownerId.yml")
        val yaml = YamlConfiguration()
        yaml.set("owner", ownerId.toString())
        yaml.set("pets", pets.map(::serializeSnapshot))
        yaml.save(file)
    }

    private fun loadFile(file: File): Pair<UUID, MutableList<PetInstance>>? {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val ownerId = yaml.getString("owner")?.let(UUID::fromString) ?: return null
        val list = yaml.getMapList("pets").mapNotNull { section ->
            @Suppress("UNCHECKED_CAST")
            val typed = section as? Map<String, Any> ?: return@mapNotNull null
            deserializeSnapshot(typed)?.toInstance(ownerId)
        }
        return ownerId to list.toMutableList()
    }

    private fun deserializeSnapshot(section: Map<String, Any>): PetSnapshot? {
        val speciesId = section["species"] as? String ?: return null
        if (!speciesIndex.containsKey(speciesId)) return null
        val id = (section["id"] as? String)?.let(UUID::fromString) ?: return null
        val nickname = section["name"] as? String ?: speciesId
        val level = (section["level"] as? Int) ?: 1
        val statsSection = section["stats"] as? Map<*, *> ?: emptyMap<String, Any>()
        val investments = StatInvestments(
            stamina = statsSection.toInvestment("stamina"),
            power = statsSection.toInvestment("power"),
            defense = statsSection.toInvestment("defense"),
            focus = statsSection.toInvestment("focus"),
        )
        val talents = (section["talents"] as? List<*>)
            ?.mapNotNull { (it as? String)?.let { name -> runCatching { TalentId.valueOf(name) }.getOrNull() } }
            ?: emptyList()
        return PetSnapshot(id, nickname, speciesId, level, investments, talents)
    }

    private fun Map<*, *>.toInvestment(key: String): StatInvestment {
        val entry = this[key] as? Map<*, *> ?: emptyMap<String, Any>()
        val effort = (entry["ev"] as? Int) ?: 0
        val individual = (entry["iv"] as? Int) ?: 0
        return StatInvestment(effort, individual)
    }

    private fun serializeSnapshot(pet: PetInstance): Map<String, Any> {
        val snapshot = pet.snapshot()
        return mapOf(
            "id" to snapshot.id.toString(),
            "name" to snapshot.nickname,
            "species" to snapshot.speciesId,
            "level" to snapshot.level,
            "stats" to mapOf(
                "stamina" to snapshot.investments.stamina.toMap(),
                "power" to snapshot.investments.power.toMap(),
                "defense" to snapshot.investments.defense.toMap(),
                "focus" to snapshot.investments.focus.toMap(),
            ),
            "talents" to snapshot.talents.map(TalentId::name),
        )
    }

    private fun StatInvestment.toMap(): Map<String, Any> = mapOf(
        "ev" to effort,
        "iv" to individual,
    )

    private fun PetSnapshot.toInstance(ownerId: UUID): PetInstance {
        val species = requireNotNull(speciesIndex[speciesId]) { "Unknown species $speciesId" }
        return PetInstance(
            id = id,
            ownerId = ownerId,
            nickname = nickname,
            species = species,
            level = level,
            investments = investments,
            talents = if (talents.isEmpty()) species.defaultTalents else talents,
        )
    }
}

