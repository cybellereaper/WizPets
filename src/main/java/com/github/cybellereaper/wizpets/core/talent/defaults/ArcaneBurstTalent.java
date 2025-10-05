package com.github.cybellereaper.wizpets.core.talent.defaults;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public final class ArcaneBurstTalent implements PetTalent {
  private int cooldown;

  @Override
  public String getId() {
    return "arcane_burst";
  }

  @Override
  public String getDisplayName() {
    return "Arcane Burst";
  }

  @Override
  public String getDescription() {
    return "Your pet's strikes occasionally unleash extra arcane damage.";
  }

  @Override
  public void tick(ActivePet pet) {
    if (cooldown > 0) {
      cooldown--;
    }
  }

  @Override
  public void onAttack(ActivePet pet, LivingEntity target, double baseDamage) {
    if (cooldown > 0) {
      return;
    }
    double bonus = pet.statValue(StatType.MAGIC) * 0.6;
    target.damage(bonus, pet.getOwner());
    target
        .getWorld()
        .spawnParticle(
            Particle.END_ROD, target.getLocation().add(0.0, 1.0, 0.0), 15, 0.2, 0.4, 0.2, 0.0);
    pet.getOwner()
        .sendActionBar(
            Component.text(
                "§5Arcane Burst deals "
                    + String.format(Locale.US, "%.1f", bonus)
                    + " bonus damage!"));
    cooldown = 60;
  }
}
