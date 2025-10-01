package com.github.cybellereaper.wizPets

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class PetStorage(private val plugin: JavaPlugin) {
    private val petKey = NamespacedKey(plugin, "pet")
    private val nameKey = NamespacedKey(plugin, "name")
    private val genKey = NamespacedKey(plugin, "generation")
    private val breedCountKey = NamespacedKey(plugin, "breed_count")
    private val talentsKey = NamespacedKey(plugin, "talents")
    private val mountKey = NamespacedKey(plugin, "mount_unlocked")
    private val flightKey = NamespacedKey(plugin, "flight_unlocked")
    private val behaviorKey = NamespacedKey(plugin, "behavior_script")

    private val evHealthKey = NamespacedKey(plugin, "ev_health")
    private val evAttackKey = NamespacedKey(plugin, "ev_attack")
    private val evDefenseKey = NamespacedKey(plugin, "ev_defense")
    private val evMagicKey = NamespacedKey(plugin, "ev_magic")

    private val ivHealthKey = NamespacedKey(plugin, "iv_health")
    private val ivAttackKey = NamespacedKey(plugin, "iv_attack")
    private val ivDefenseKey = NamespacedKey(plugin, "iv_defense")
    private val ivMagicKey = NamespacedKey(plugin, "iv_magic")

    fun load(player: Player): PetData? {
        val parent = player.persistentDataContainer
        val container = parent.get(petKey, PersistentDataType.TAG_CONTAINER) ?: return null
        val displayName = container.get(nameKey, PersistentDataType.STRING) ?: return null
        val generation = container.get(genKey, PersistentDataType.INTEGER) ?: 1
        val breedCount = container.get(breedCountKey, PersistentDataType.INTEGER) ?: 0
        val talentString = container.get(talentsKey, PersistentDataType.STRING) ?: ""
        val mountUnlocked = (container.get(mountKey, PersistentDataType.BYTE) ?: 0.toByte()).toInt() == 1
        val flightUnlocked = (container.get(flightKey, PersistentDataType.BYTE) ?: 0.toByte()).toInt() == 1
        val behaviorScript = container.get(behaviorKey, PersistentDataType.STRING) ?: "default"

        val evs = StatSet(
            health = container.get(evHealthKey, PersistentDataType.DOUBLE) ?: 0.0,
            attack = container.get(evAttackKey, PersistentDataType.DOUBLE) ?: 0.0,
            defense = container.get(evDefenseKey, PersistentDataType.DOUBLE) ?: 0.0,
            magic = container.get(evMagicKey, PersistentDataType.DOUBLE) ?: 0.0
        )
        val ivs = StatSet(
            health = container.get(ivHealthKey, PersistentDataType.DOUBLE) ?: 0.0,
            attack = container.get(ivAttackKey, PersistentDataType.DOUBLE) ?: 0.0,
            defense = container.get(ivDefenseKey, PersistentDataType.DOUBLE) ?: 0.0,
            magic = container.get(ivMagicKey, PersistentDataType.DOUBLE) ?: 0.0
        )

        val talentIds = if (talentString.isBlank()) emptyList() else talentString.split(';').filter { it.isNotBlank() }

        return PetData(
            displayName = displayName,
            evs = evs,
            ivs = ivs,
            talentIds = talentIds,
            generation = generation,
            breedCount = breedCount,
            mountUnlocked = mountUnlocked,
            flightUnlocked = flightUnlocked,
            behaviorScript = behaviorScript
        )
    }

    fun save(player: Player, data: PetData) {
        val parent = player.persistentDataContainer
        val ctx = parent.adapterContext
        val container = ctx.newPersistentDataContainer()

        container.set(nameKey, PersistentDataType.STRING, data.displayName)
        container.set(genKey, PersistentDataType.INTEGER, data.generation)
        container.set(breedCountKey, PersistentDataType.INTEGER, data.breedCount)
        container.set(talentsKey, PersistentDataType.STRING, data.talentIds.joinToString(";"))
        container.set(mountKey, PersistentDataType.BYTE, if (data.mountUnlocked) 1.toByte() else 0.toByte())
        container.set(flightKey, PersistentDataType.BYTE, if (data.flightUnlocked) 1.toByte() else 0.toByte())
        container.set(behaviorKey, PersistentDataType.STRING, data.behaviorScript)

        container.set(evHealthKey, PersistentDataType.DOUBLE, data.evs.health)
        container.set(evAttackKey, PersistentDataType.DOUBLE, data.evs.attack)
        container.set(evDefenseKey, PersistentDataType.DOUBLE, data.evs.defense)
        container.set(evMagicKey, PersistentDataType.DOUBLE, data.evs.magic)

        container.set(ivHealthKey, PersistentDataType.DOUBLE, data.ivs.health)
        container.set(ivAttackKey, PersistentDataType.DOUBLE, data.ivs.attack)
        container.set(ivDefenseKey, PersistentDataType.DOUBLE, data.ivs.defense)
        container.set(ivMagicKey, PersistentDataType.DOUBLE, data.ivs.magic)

        parent.set(petKey, PersistentDataType.TAG_CONTAINER, container)
    }

    fun clear(player: Player) {
        player.persistentDataContainer.remove(petKey)
    }
}
