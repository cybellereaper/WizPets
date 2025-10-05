package com.github.cybellereaper.wizpets.api;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a pet stored in persistent data.
 */
public record PetRecord(
    String displayName,
    StatSet evs,
    StatSet ivs,
    List<String> talentIds,
    int generation,
    int breedCount,
    boolean mountUnlocked,
    boolean flightUnlocked
) {
    public PetRecord {
        displayName = Objects.requireNonNull(displayName, "displayName");
        evs = Objects.requireNonNull(evs, "evs");
        ivs = Objects.requireNonNull(ivs, "ivs");
        talentIds = List.copyOf(talentIds);
    }

    public PetRecord withDisplayName(String name) {
        return new PetRecord(Objects.requireNonNull(name, "name"), evs, ivs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withTalentIds(List<String> ids) {
        return new PetRecord(displayName, evs, ivs, Objects.requireNonNull(ids, "ids"), generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withBreedCount(int count) {
        return new PetRecord(displayName, evs, ivs, talentIds, generation, count, mountUnlocked, flightUnlocked);
    }

    public PetRecord withMountUnlocked(boolean unlocked) {
        return new PetRecord(displayName, evs, ivs, talentIds, generation, breedCount, unlocked, flightUnlocked);
    }

    public PetRecord withFlightUnlocked(boolean unlocked) {
        return new PetRecord(displayName, evs, ivs, talentIds, generation, breedCount, mountUnlocked, unlocked);
    }

    public PetRecord withEvs(StatSet newEvs) {
        return new PetRecord(displayName, Objects.requireNonNull(newEvs, "newEvs"), ivs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withIvs(StatSet newIvs) {
        return new PetRecord(displayName, evs, Objects.requireNonNull(newIvs, "newIvs"), talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withGeneration(int newGeneration) {
        return new PetRecord(displayName, evs, ivs, talentIds, newGeneration, breedCount, mountUnlocked, flightUnlocked);
    }
}
