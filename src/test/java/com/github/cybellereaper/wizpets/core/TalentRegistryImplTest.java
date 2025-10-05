package com.github.cybellereaper.wizpets.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TalentRegistryImplTest {
  private static class DummyTalent implements PetTalent {
    @Override
    public String getId() {
      return "dummy";
    }

    @Override
    public String getDisplayName() {
      return "Dummy";
    }

    @Override
    public String getDescription() {
      return "Does nothing";
    }

    @Override
    public double modifyStat(ActivePet pet, StatType stat, double current) {
      return current + 1;
    }
  }

  private static final class AlternateDummyTalent extends DummyTalent {
    @Override
    public String getDisplayName() {
      return "Alternate Dummy";
    }
  }

  @Test
  void registerAndInstantiateTalents() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    List<PetTalent> talents = registry.instantiate(List.of("dummy"));
    assertEquals(1, talents.size());
    assertEquals("Dummy", talents.get(0).getDisplayName());
    assertThrows(UnsupportedOperationException.class, () -> talents.add(new DummyTalent()));
  }

  @Test
  void registeringDuplicateWithoutReplaceFails() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    assertThrows(
        IllegalArgumentException.class, () -> registry.register((TalentFactory) DummyTalent::new));
  }

  @Test
  void registeringWithReplaceOverridesDescriptor() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    registry.register((TalentFactory) AlternateDummyTalent::new, true);
    assertEquals("Alternate Dummy", registry.get("dummy").getDisplayName());
  }

  @Test
  void rollReturnsIdsFromRegistry() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    List<String> roll = registry.roll(new Random(0), 3);
    assertEquals(3, roll.size());
    assertTrue(roll.stream().allMatch(id -> id.equals("dummy")));
    assertThrows(UnsupportedOperationException.class, () -> roll.add("other"));
  }

  @Test
  void inheritFallsBackToRegistryWhenParentsEmpty() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    List<String> inherited = registry.inherit(List.of(), List.of(), new Random(1), 2);
    assertEquals(2, inherited.size());
    assertTrue(inherited.stream().allMatch(id -> id.equals("dummy")));
  }

  @Test
  void inheritPrefersUniqueTalentsFromParents() {
    TalentRegistryImpl registry = new TalentRegistryImpl();
    registry.register((TalentFactory) DummyTalent::new);
    registry.register(
        () ->
            new PetTalent() {
              @Override
              public String getId() {
                return "other";
              }

              @Override
              public String getDisplayName() {
                return "Other";
              }

              @Override
              public String getDescription() {
                return "Other talent";
              }
            });
    List<String> inherited = registry.inherit(List.of("dummy"), List.of("other"), new Random(5), 2);
    assertTrue(inherited.contains("dummy"));
    assertTrue(inherited.contains("other"));
  }
}
