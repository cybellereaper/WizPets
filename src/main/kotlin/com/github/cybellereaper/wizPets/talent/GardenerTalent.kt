package com.github.cybellereaper.wizPets.talent

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.inventory.ItemStack

internal object GardenerTalent {
    private val targetBlocks = setOf(Material.FARMLAND, Material.SOUL_SOIL, Material.MOSS_BLOCK)

    fun perform(context: TalentContext) {
        val origin = context.armorStand.location
        val world = origin.world
        val radius = 4
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val block = world.getBlockAt(origin.blockX + x, origin.blockY - 1, origin.blockZ + z)
                if (!targetBlocks.contains(block.type)) continue
                val crop = block.getRelative(0, 1, 0)
                if (tryHarvest(crop, context) || tryPlant(crop, context.carriedItems)) {
                    world.spawnParticle(Particle.VILLAGER_HAPPY, crop.location.add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.0)
                    return
                }
            }
        }
    }

    private fun tryHarvest(block: Block, context: TalentContext): Boolean {
        val data = block.blockData
        if (data !is Ageable || data.age < data.maximumAge) return false
        block.breakNaturally()
        context.owner.giveExp(1)
        return true
    }

    private fun tryPlant(block: Block, carriedItems: MutableList<ItemStack>): Boolean {
        if (!block.isEmpty) return false
        val seeds = carriedItems.firstOrNull { it.type.name.endsWith("_SEEDS") && it.amount > 0 } ?: return false
        block.type = seeds.type.correspondingCrop()
        seeds.amount -= 1
        return true
    }

    private fun Material.correspondingCrop(): Material = when (this) {
        Material.BEETROOT_SEEDS -> Material.BEETROOTS
        Material.MELON_SEEDS -> Material.MELON_STEM
        Material.PUMPKIN_SEEDS -> Material.PUMPKIN_STEM
        Material.TORCHFLOWER_SEEDS -> Material.TORCHFLOWER
        Material.NETHER_WART -> Material.NETHER_WART
        else -> Material.WHEAT
    }
}

