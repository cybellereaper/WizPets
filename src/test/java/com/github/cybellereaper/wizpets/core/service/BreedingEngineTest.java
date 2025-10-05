package com.github.cybellereaper.wizpets.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import com.github.cybellereaper.wizpets.core.talent.defaults.ArcaneBurstTalent;
import com.github.cybellereaper.wizpets.core.talent.defaults.GuardianShellTalent;
import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BreedingEngineTest {
  private static final String PLAYER_NAME = "Celeste";
  private Player player;

  @BeforeEach
  void setUp() {
    player = Mockito.mock(Player.class);
    Mockito.when(player.getName()).thenReturn(PLAYER_NAME);
  }

  @Test
  void breedCombinesParentAttributesAndUnlocks() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register(GuardianShellTalent::new);
    registry.register(ArcaneBurstTalent::new);

    BreedingEngine engine = new BreedingEngine(registry, new SplittableRandom(42));

    PetRecord parentA =
        new PetRecord(
            "A",
            new StatSet(20, 15, 12, 18),
            new StatSet(12, 10, 8, 6),
            List.of("guardian_shell"),
            2,
            4,
            true,
            false);
    PetRecord parentB =
        new PetRecord(
            "B",
            new StatSet(18, 10, 16, 11),
            new StatSet(14, 9, 7, 5),
            List.of("arcane_burst"),
            3,
            1,
            false,
            true);

    BreedingEngine.BreedOutcome outcome = engine.breed(player, parentA, parentB);

    PetRecord child = outcome.childRecord();
    assertEquals(4, child.generation());
    assertEquals(5, child.breedCount());
    assertTrue(child.mountUnlocked());
    assertTrue(child.flightUnlocked());
    assertFalse(child.talentIds().isEmpty());
    assertTrue(child.displayName().contains(PLAYER_NAME));

    PetRecord updatedPartner = outcome.updatedPartnerRecord();
    assertEquals(parentB.breedCount() + 1, updatedPartner.breedCount());
  }
}
