package com.github.cybellereaper.noblepets.api;

import com.github.cybellereaper.noblepets.api.talent.PetTalent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;

/** Read-only handle for an active pet instance with a small command surface for talents. */
public interface ActivePet {
  Player getOwner();

  PetRecord getRecord();

  List<PetTalent> getTalents();

  boolean isMounted();

  boolean isFlying();

  double statValue(StatType type);

  default Map<StatType, Double> statBreakdown() {
    Map<StatType, Double> breakdown = new EnumMap<>(StatType.class);
    for (StatType type : StatType.values()) {
      breakdown.put(type, statValue(type));
    }
    return Map.copyOf(breakdown);
  }

  void heal(double amount);

  void grantAbsorption(double hearts);
}
