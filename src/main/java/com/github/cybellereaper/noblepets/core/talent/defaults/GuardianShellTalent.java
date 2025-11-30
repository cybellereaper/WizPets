package com.github.cybellereaper.noblepets.core.talent.defaults;

import com.github.cybellereaper.noblepets.api.ActivePet;
import com.github.cybellereaper.noblepets.api.StatType;
import org.bukkit.Particle;

public final class GuardianShellTalent extends PeriodicTalent {
  public GuardianShellTalent() {
    super(240);
  }

  @Override
  public String getId() {
    return "guardian_shell";
  }

  @Override
  public String getDisplayName() {
    return "Guardian Shell";
  }

  @Override
  public String getDescription() {
    return "Periodically grants absorption hearts to protect you.";
  }

  @Override
  protected void trigger(ActivePet pet) {
    double absorption = 2.0 + pet.statValue(StatType.DEFENSE) * 0.05;
    pet.grantAbsorption(absorption);
    pet.getOwner()
        .getWorld()
        .spawnParticle(
            Particle.INSTANT_EFFECT, pet.getOwner().getLocation(), 20, 0.5, 0.5, 0.5, 0.0);
  }
}
