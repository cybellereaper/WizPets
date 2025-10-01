package com.github.cybellereaper.wizPets

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

class WildPetSpawner(private val plugin: WizPets) {
    private val random = Random(System.currentTimeMillis())
    private val spawned = mutableSetOf<LivingEntity>()
    private var task: BukkitTask? = null

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L * 45, 20L * 45)
    }

    private fun tick() {
        if (PetSpeciesRegistry.getAllSpecies().isEmpty()) return
        for (player in Bukkit.getOnlinePlayers()) {
            if (random.nextDouble() > 0.35) continue
            val species = PetSpeciesRegistry.getAllSpecies().random(random)
            val offset = Location(
                player.world,
                player.location.x + random.nextInt(-12, 13),
                player.location.y,
                player.location.z + random.nextInt(-12, 13)
            )
            val spawnLocation = offset.world?.getHighestBlockAt(offset).location.add(0.5, 1.0, 0.5)
            val entity = spawnLocation.world?.spawnEntity(spawnLocation, species.entityType) as? LivingEntity ?: continue
            entity.customName = "Wild ${species.displayName}"
            entity.isCustomNameVisible = true
            entity.setMetadata("wizpets:wild", FixedMetadataValue(plugin, species.id))
            entity.health = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: entity.health
            entity.setAI(true)
            spawned += entity
            Bukkit.getScheduler().runTaskLater(plugin, {
                if (!entity.isDead) {
                    entity.removeMetadata("wizpets:wild", plugin)
                    entity.remove()
                }
                spawned.remove(entity)
            }, 20L * 60)
        }
    }

    fun shutdown() {
        task?.cancel()
        task = null
        spawned.forEach {
            it.removeMetadata("wizpets:wild", plugin)
            it.remove()
        }
        spawned.clear()
    }
}
