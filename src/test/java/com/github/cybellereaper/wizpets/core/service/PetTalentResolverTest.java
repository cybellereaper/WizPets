package com.github.cybellereaper.wizpets.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import com.github.cybellereaper.wizpets.core.talent.defaults.HealingAuraTalent;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetTalentResolverTest {
  private TalentRegistryImpl registry;
  private ExecutorService executor;
  private PetTalentResolver resolver;

  @BeforeEach
  void setUp() {
    registry = new TalentRegistryImpl();
    registry.register(HealingAuraTalent::new);
    registry.register(
        () ->
            new com.github.cybellereaper.wizpets.api.talent.PetTalent() {
              @Override
              public String getId() {
                return "custom";
              }

              @Override
              public String getDisplayName() {
                return "Custom";
              }

              @Override
              public String getDescription() {
                return "Custom talent";
              }
            });
    executor = Executors.newSingleThreadExecutor();
    resolver = new PetTalentResolver(registry, new SplittableRandom(7), executor);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void resolveKeepsValidTalentIds() {
    PetRecord record =
        new PetRecord(
            "Companion",
            new StatSet(10, 10, 10, 10),
            new StatSet(5, 5, 5, 5),
            List.of("healing_aura"),
            1,
            0,
            false,
            false);

    PetTalentResolver.ResolvedTalents resolved = resolver.resolve(record);
    assertEquals(record, resolved.record());
    assertEquals(1, resolved.talents().size());
  }

  @Test
  void resolveRepairsMissingTalentIds() {
    PetRecord record =
        new PetRecord(
            "Companion",
            new StatSet(10, 10, 10, 10),
            new StatSet(5, 5, 5, 5),
            List.of("unknown"),
            1,
            0,
            false,
            false);

    PetTalentResolver.ResolvedTalents resolved = resolver.resolve(record);
    assertNotEquals(record.talentIds(), resolved.record().talentIds());
    assertEquals(resolved.record().talentIds().size(), resolved.talents().size());
    Set<String> registered =
        registry.instantiate(List.of("healing_aura", "custom")).stream()
            .map(com.github.cybellereaper.wizpets.api.talent.PetTalent::getId)
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(resolved.record().talentIds().stream().allMatch(registered::contains));
  }
}
