package com.github.cybellereaper.wizpets.core.persistence

import com.github.cybellereaper.wizpets.api.PetRecord
import com.github.cybellereaper.wizpets.api.StatSet
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class PetStorage(plugin: JavaPlugin) {
    private val rootKey = NamespacedKey(plugin, "pet")

    private val nameKey = NamespacedKey(plugin, "name")
    private val generationKey = NamespacedKey(plugin, "generation")
    private val breedKey = NamespacedKey(plugin, "breed_count")
    private val talentsKey = NamespacedKey(plugin, "talents")
    private val mountKey = NamespacedKey(plugin, "mount_unlocked")
    private val flightKey = NamespacedKey(plugin, "flight_unlocked")

    private val evKeys = mapOf(
        "health" to NamespacedKey(plugin, "ev_health"),
        "attack" to NamespacedKey(plugin, "ev_attack"),
        "defense" to NamespacedKey(plugin, "ev_defense"),
        "magic" to NamespacedKey(plugin, "ev_magic")
    )
    private val ivKeys = mapOf(
        "health" to NamespacedKey(plugin, "iv_health"),
        "attack" to NamespacedKey(plugin, "iv_attack"),
        "defense" to NamespacedKey(plugin, "iv_defense"),
        "magic" to NamespacedKey(plugin, "iv_magic")
    )

    fun load(player: Player): PetRecord? {
        val parent = player.persistentDataContainer
        val container = parent.get(rootKey, PersistentDataType.TAG_CONTAINER) ?: return null
        val name = container.get(nameKey, PersistentDataType.STRING) ?: return null
        val generation = container.get(generationKey, PersistentDataType.INTEGER) ?: 1
        val breedCount = container.get(breedKey, PersistentDataType.INTEGER) ?: 0
        val talents = container.get(talentsKey, PersistentDataType.STRING)?.split(';')?.filter { it.isNotBlank() } ?: emptyList()
        val mountUnlocked = container.get(mountKey, PersistentDataType.BYTE)?.toInt() == 1
        val flightUnlocked = container.get(flightKey, PersistentDataType.BYTE)?.toInt() == 1

        val evs = StatSet(
            health = container.get(evKeys.getValue("health"), PersistentDataType.DOUBLE) ?: 0.0,
            attack = container.get(evKeys.getValue("attack"), PersistentDataType.DOUBLE) ?: 0.0,
            defense = container.get(evKeys.getValue("defense"), PersistentDataType.DOUBLE) ?: 0.0,
            magic = container.get(evKeys.getValue("magic"), PersistentDataType.DOUBLE) ?: 0.0
        )
        val ivs = StatSet(
            health = container.get(ivKeys.getValue("health"), PersistentDataType.DOUBLE) ?: 0.0,
            attack = container.get(ivKeys.getValue("attack"), PersistentDataType.DOUBLE) ?: 0.0,
            defense = container.get(ivKeys.getValue("defense"), PersistentDataType.DOUBLE) ?: 0.0,
            magic = container.get(ivKeys.getValue("magic"), PersistentDataType.DOUBLE) ?: 0.0
        )

        return PetRecord(
            displayName = name,
            evs = evs,
            ivs = ivs,
            talentIds = talents,
            generation = generation,
            breedCount = breedCount,
            mountUnlocked = mountUnlocked,
            flightUnlocked = flightUnlocked
        )
    }

    fun save(player: Player, record: PetRecord) {
        val parent = player.persistentDataContainer
        val ctx = parent.adapterContext
        val container = ctx.newPersistentDataContainer()

        container.set(nameKey, PersistentDataType.STRING, record.displayName)
        container.set(generationKey, PersistentDataType.INTEGER, record.generation)
        container.set(breedKey, PersistentDataType.INTEGER, record.breedCount)
        container.set(talentsKey, PersistentDataType.STRING, record.talentIds.joinToString(";"))
        container.set(mountKey, PersistentDataType.BYTE, record.mountUnlocked.asByte())
        container.set(flightKey, PersistentDataType.BYTE, record.flightUnlocked.asByte())

        container.set(evKeys.getValue("health"), PersistentDataType.DOUBLE, record.evs.health)
        container.set(evKeys.getValue("attack"), PersistentDataType.DOUBLE, record.evs.attack)
        container.set(evKeys.getValue("defense"), PersistentDataType.DOUBLE, record.evs.defense)
        container.set(evKeys.getValue("magic"), PersistentDataType.DOUBLE, record.evs.magic)

        container.set(ivKeys.getValue("health"), PersistentDataType.DOUBLE, record.ivs.health)
        container.set(ivKeys.getValue("attack"), PersistentDataType.DOUBLE, record.ivs.attack)
        container.set(ivKeys.getValue("defense"), PersistentDataType.DOUBLE, record.ivs.defense)
        container.set(ivKeys.getValue("magic"), PersistentDataType.DOUBLE, record.ivs.magic)

        parent.set(rootKey, PersistentDataType.TAG_CONTAINER, container)
    }

    fun clear(player: Player) {
        player.persistentDataContainer.remove(rootKey)
    }

    private fun Boolean?.asByte(): Byte = if (this == true) 1 else 0
}
