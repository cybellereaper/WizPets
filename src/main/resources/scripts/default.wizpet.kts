import com.github.cybellereaper.wizPets.pet.model.Element
import com.github.cybellereaper.wizPets.talent.TalentContext
import com.github.cybellereaper.wizPets.talent.TalentId

// Register a simple radiant pulse talent implemented entirely in script.
wizpets.talent("radiant_pulse") { context: TalentContext ->
    val nearby = context.armorStand.location.getNearbyLivingEntities(6.0)
    nearby.filter { it.uniqueId != context.owner.uniqueId }
        .forEach { entity ->
            entity.damage((context.level * 0.3) + 1.0, context.owner)
        }
}

wizpets.species("ember_fox") {
    displayName = "Ember Fox"
    element = Element.FLAME
    baseLevel = 5
    stats {
        stamina = 12
        power = 15
        defense = 8
        focus = 10
    }
    defaultTalents(TalentId.SENTINEL, TalentId.GATHERER)
}

wizpets.species("aqua_sprite") {
    displayName = "Aqua Sprite"
    element = Element.AQUA
    baseLevel = 3
    stats {
        stamina = 10
        power = 9
        defense = 11
        focus = 16
    }
    defaultTalents(TalentId.MEDIC, TalentId.GARDENER)
}

wizpets.species("radiant_magus") {
    displayName = "Radiant Magus"
    element = Element.ARCANE
    baseLevel = 12
    stats {
        stamina = 14
        power = 18
        defense = 12
        focus = 20
    }
    defaultTalents(TalentId.ARTISAN, TalentId("radiant_pulse"))
}
