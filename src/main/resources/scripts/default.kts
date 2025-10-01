import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.util.Vector

particleSequence("summon_glimmer") {
    repeats(2)
    frame(
        delayTicks = 0,
        particle = Particle.END_ROD,
        offset = Vector(0.0, 1.0, 0.0),
        count = 6,
        speed = 0.02,
        spread = Vector(0.15, 0.25, 0.15)
    )
    frame(
        delayTicks = 6,
        particle = Particle.CRIT_MAGIC,
        offset = Vector(0.0, 1.2, 0.0),
        count = 12,
        speed = 0.03,
        spread = Vector(0.3, 0.3, 0.3)
    )
}

raycastAnimation("arcane_bolt") {
    particle = Particle.END_ROD
    step = 0.45
    maxDistance = 18.0
    periodTicks = 1L
    count = 3
    spread = Vector(0.02, 0.02, 0.02)
    speed = 0.001
    hitRadius = 0.6
}

areaEffect("soothing_field") {
    particle = Particle.SPELL_INSTANT
    radius = 3.5
    layers = 2
    pointsPerLayer = 36
    layerSpacing = 0.4
    durationTicks = 60L
    intervalTicks = 5L
    count = 4
    spread = Vector(0.05, 0.1, 0.05)
    affectEntities = false
}

petBehavior("default") {
    onSummon {
        debug("Summoned using default behavior script.")
        playSequence("summon_glimmer", location.clone().add(0.0, 0.25, 0.0))
    }

    tick {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: owner.health
        if (owner.health < maxHealth - 4.0) {
            playAreaEffect("soothing_field", location.clone())
            healOwner(0.4)
        }
    }

    onAttack { target, damage ->
        playRaycast("arcane_bolt", location.clone().add(0.0, 0.8, 0.0), owner.location.direction)
        debug("Engaged ${target.name} for ${"%.1f".format(damage)} damage")
    }

    onRaycastHit { entity ->
        debug("Arcane bolt clipped ${entity.name}")
    }
}
