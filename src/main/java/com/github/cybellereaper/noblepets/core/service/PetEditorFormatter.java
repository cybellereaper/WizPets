package com.github.cybellereaper.noblepets.core.service;

import com.github.cybellereaper.noblepets.api.PetRecord;
import com.github.cybellereaper.noblepets.api.StatType;
import com.github.cybellereaper.noblepets.api.talent.PetTalentDescriptor;
import com.github.cybellereaper.noblepets.api.talent.TalentRegistryView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Utility for rendering in-game editor summaries. */
final class PetEditorFormatter {
  private PetEditorFormatter() {}

  static List<String> summaryLines(PetRecord record, TalentRegistryView registry) {
    List<String> lines = new ArrayList<>();
    lines.add("§7==== §dNoblePet Editor §7====");
    lines.add("§7Name: §f" + record.displayName());
    lines.add(
        "§7Generation: §f"
            + record.generation()
            + "  §7Breeds: §f"
            + record.breedCount());
    lines.add(
        "§7Mount unlocked: §f"
            + record.mountUnlocked()
            + "  §7Flight unlocked: §f"
            + record.flightUnlocked());
    lines.add("§7Talents:");
    if (record.talentIds().isEmpty()) {
      lines.add("§8  (None)");
    } else {
      int index = 1;
      for (String talentId : record.talentIds()) {
        lines.add("§7  " + index++ + ". §d" + describeTalent(talentId, registry));
      }
    }
    lines.add("§7Stats:");
    for (StatType type : StatType.values()) {
      double combined = record.evs().value(type) / 4.0 + record.ivs().value(type);
      lines.add(
          "§7  - §b"
              + type.getDisplayName()
              + "§7: §f"
              + String.format(Locale.US, "%.2f", combined));
    }
    lines.add("§8Use §d/noblepet edit name <name>§8 to rename your pet.");
    lines.add("§8Use §d/noblepet edit reroll§8 to reroll all talents.");
    lines.add("§8Use §d/noblepet edit talent <slot>§8 to reroll a single talent.");
    return List.copyOf(lines);
  }

  private static String describeTalent(String talentId, TalentRegistryView registry) {
    PetTalentDescriptor descriptor = registry.get(talentId);
    if (descriptor == null) {
      return talentId + " §8(Unknown)";
    }
    return descriptor.getDisplayName() + "§8 - §7" + descriptor.getDescription();
  }
}
