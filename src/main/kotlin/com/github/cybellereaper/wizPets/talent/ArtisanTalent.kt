package com.github.cybellereaper.wizPets.talent

import org.bukkit.Material
import org.bukkit.block.Furnace

internal object ArtisanTalent {
    fun perform(context: TalentContext) {
        val origin = context.armorStand.location
        val world = origin.world
        val radius = 5
        for (x in -radius..radius) {
            for (y in -1..1) {
                for (z in -radius..radius) {
                    val block = world.getBlockAt(origin.blockX + x, origin.blockY + y, origin.blockZ + z)
                    val state = block.state
                    if (state is Furnace) {
                        state.cookTime = (state.cookTime + 20).coerceAtMost(state.cookTimeTotal)
                        state.update()
                        return
                    }
                    if (block.type == Material.ANVIL) {
                        val owner = context.owner
                        owner.giveExp(2)
                        return
                    }
                }
            }
        }
    }
}

