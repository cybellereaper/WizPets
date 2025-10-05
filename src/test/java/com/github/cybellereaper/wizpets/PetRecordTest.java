package com.github.cybellereaper.wizpets;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetRecordTest {
    private final StatSet evs = new StatSet(1, 2, 3, 4);
    private final StatSet ivs = new StatSet(1, 2, 3, 4);

    @Test
    void withDisplayNameReturnsNewRecord() {
        PetRecord record = baseRecord();
        PetRecord renamed = record.withDisplayName("New Name");
        assertEquals("New Name", renamed.getDisplayName());
        assertEquals("Test", record.getDisplayName());
        assertNotSame(record, renamed);
    }

    @Test
    void withMountAndFlightUnlocksIndependently() {
        PetRecord record = baseRecord();
        PetRecord mountUnlocked = record.withMountUnlocked(true);
        PetRecord flightUnlocked = mountUnlocked.withFlightUnlocked(true);
        assertFalse(record.isMountUnlocked());
        assertFalse(record.isFlightUnlocked());
        assertTrue(mountUnlocked.isMountUnlocked());
        assertTrue(flightUnlocked.isFlightUnlocked());
    }

    private PetRecord baseRecord() {
        return new PetRecord("Test", evs, ivs, List.of("a"), 1, 0, false, false);
    }
}
