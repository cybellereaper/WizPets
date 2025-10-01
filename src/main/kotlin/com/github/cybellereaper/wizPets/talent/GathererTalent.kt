package com.github.cybellereaper.wizPets.talent

import org.bukkit.entity.Item

internal object GathererTalent {
    fun perform(context: TalentContext) {
        val origin = context.armorStand.location
        val radius = 6.0
        val items = origin.world.getNearbyEntities(origin, radius, radius, radius)
            .filterIsInstance<Item>()
            .filter { !it.isDead }
            .take(5)

        for (entity in items) {
            context.carriedItems += entity.itemStack.clone()
            entity.remove()
        }
    }
}

