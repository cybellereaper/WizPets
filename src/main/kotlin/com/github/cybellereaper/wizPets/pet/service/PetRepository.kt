package com.github.cybellereaper.wizPets.pet.service

import com.github.cybellereaper.wizPets.pet.model.PetInstance
import com.github.cybellereaper.wizPets.pet.model.PetSnapshot
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.model.StatInvestment
import com.github.cybellereaper.wizPets.pet.model.StatInvestments
import com.github.cybellereaper.wizPets.serialization.UUIDSerializer
import com.github.cybellereaper.wizPets.talent.TalentId
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun refreshSpeciesIndex(newIndex: Map<String, PetSpecies>) {
        speciesIndex.clear()
        speciesIndex.putAll(newIndex)
    }

    fun load(speciesIndex: Map<String, PetSpecies>): Map<UUID, MutableList<PetInstance>> {
        refreshSpeciesIndex(speciesIndex)
        val result = mutableMapOf<UUID, MutableList<PetInstance>>()

        directory.listFiles { file -> file.extension.equals("json", true) }
            ?.mapNotNull(::loadJson)
            ?.forEach { (owner, pets) -> result[owner] = pets }

        directory.listFiles { file -> file.extension.equals("yml", true) }
            ?.mapNotNull(::loadLegacyYaml)
            ?.forEach { (owner, pets) -> result.putIfAbsent(owner, pets) }

        return result
    }

    fun save(ownerId: UUID, pets: List<PetInstance>) {
        val file = File(directory, "$ownerId.json")
        val record = PetStoreFile(ownerId = ownerId, pets = pets.map(PetInstance::snapshot))
        file.writeText(json.encodeToString(record))
    }

    private fun loadJson(file: File): Pair<UUID, MutableList<PetInstance>>? = runCatching {
        val record = json.decodeFromString<PetStoreFile>(file.readText())
        val pets = record.pets.mapNotNull { snapshot ->
            snapshot.toInstance(record.owner, speciesIndex)
        }
        record.owner to pets.toMutableList()
    }.getOrNull()

    private fun loadLegacyYaml(file: File): Pair<UUID, MutableList<PetInstance>>? {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val ownerId = yaml.getString("owner")?.let(UUID::fromString) ?: return null
        val list = yaml.getMapList("pets").mapNotNull { section ->
            @Suppress("UNCHECKED_CAST")
            val typed = section as? Map<String, Any> ?: return@mapNotNull null
            deserializeSnapshot(typed)?.toInstance(ownerId, speciesIndex)
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
            ?.mapNotNull { (it as? String)?.let(::TalentId) }
            ?: emptyList()
        return PetSnapshot(id, nickname, speciesId, level, investments, talents)
    }

    private fun Map<*, *>.toInvestment(key: String): StatInvestment {
        val entry = this[key] as? Map<*, *> ?: emptyMap<String, Any>()
        val effort = (entry["ev"] as? Int) ?: 0
        val individual = (entry["iv"] as? Int) ?: 0
        return StatInvestment(effort, individual)
    }

    private fun PetSnapshot.toInstance(ownerId: UUID, speciesIndex: Map<String, PetSpecies>): PetInstance? {
        val species = speciesIndex[speciesId] ?: return null
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

    @Serializable
    private data class PetStoreFile(
        @Serializable(with = UUIDSerializer::class)
        val owner: UUID,
        val pets: List<PetSnapshot>,
    )
}
