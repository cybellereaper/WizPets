package com.github.cybellereaper.noblepets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.noblepets.api.PetRecord;
import com.github.cybellereaper.noblepets.api.StatSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class PetRecordTest {
  private final StatSet evs = new StatSet(1, 2, 3, 4);
  private final StatSet ivs = new StatSet(1, 2, 3, 4);

  @Test
  void withDisplayNameReturnsNewRecord() {
    PetRecord record = baseRecord();
    PetRecord renamed = record.withDisplayName("New Name");
    assertEquals("New Name", renamed.displayName());
    assertEquals("Test", record.displayName());
    assertNotSame(record, renamed);
  }

  @Test
  void withMountAndFlightUnlocksIndependently() {
    PetRecord record = baseRecord();
    PetRecord mountUnlocked = record.withMountUnlocked(true);
    PetRecord flightUnlocked = mountUnlocked.withFlightUnlocked(true);
    assertFalse(record.mountUnlocked());
    assertFalse(record.flightUnlocked());
    assertTrue(mountUnlocked.mountUnlocked());
    assertTrue(flightUnlocked.flightUnlocked());
  }

  @Test
  void talentListIsDefensivelyCopied() {
    PetRecord record = baseRecord();
    assertThrows(UnsupportedOperationException.class, () -> record.talentIds().add("new"));
  }

  private PetRecord baseRecord() {
    return new PetRecord("Test", evs, ivs, List.of("a"), 1, 0, false, false);
  }
}
