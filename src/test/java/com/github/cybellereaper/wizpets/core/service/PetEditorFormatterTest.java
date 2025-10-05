package com.github.cybellereaper.wizpets.core.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.talent.PetTalentDescriptor;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PetEditorFormatterTest {

  @Test
  void summaryIncludesFormattedTalentsAndInstructions() {
    TalentRegistryView registry = mock(TalentRegistryView.class);
    when(registry.get("healing"))
        .thenReturn(new PetTalentDescriptor("healing", "Healing", "Restores health"));
    PetRecord record =
        new PetRecord(
            "Nova",
            new StatSet(4, 2, 3, 5),
            new StatSet(1, 1, 1, 1),
            List.of("healing"),
            2,
            1,
            true,
            false);

    List<String> lines = PetEditorFormatter.summaryLines(record, registry);

    assertTrue(lines.stream().anyMatch(line -> line.contains("Nova")));
    assertTrue(lines.stream().anyMatch(line -> line.contains("Healing")));
    assertTrue(lines.getLast().contains("talent <slot>"));
  }
}
