package com.github.cybellereaper.wizpets.core.talent.defaults;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.StatType;
import org.bukkit.Particle;

public final class HealingAuraTalent extends PeriodicTalent {
  public HealingAuraTalent() {
    super(100);
  }

  @Override
  public String getId() {
    return "healing_aura";
  }

  @Override
  public String getDisplayName() {
    return "Healing Aura";
  }

  @Override
  public String getDescription() {
    return "Every few seconds your pet mends your wounds.";
  }

  @Override
  protected void trigger(ActivePet pet) {
    double healAmount = 1.5 + pet.statValue(StatType.MAGIC) * 0.08;
    pet.heal(healAmount);
    pet.getOwner()
        .getWorld()
        .spawnParticle(
            Particle.HEART, pet.getOwner().getLocation().add(0.0, 1.0, 0.0), 5, 0.3, 0.4, 0.3, 0.0);
  }
}
