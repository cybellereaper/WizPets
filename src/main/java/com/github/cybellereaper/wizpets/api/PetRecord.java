package com.github.cybellereaper.wizpets.api;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a pet stored in persistent data.
 */
public final class PetRecord {
    private final String displayName;
    private final StatSet evs;
    private final StatSet ivs;
    private final List<String> talentIds;
    private final int generation;
    private final int breedCount;
    private final boolean mountUnlocked;
    private final boolean flightUnlocked;

    public PetRecord(
        String displayName,
        StatSet evs,
        StatSet ivs,
        List<String> talentIds,
        int generation,
        int breedCount,
        boolean mountUnlocked,
        boolean flightUnlocked
    ) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.evs = Objects.requireNonNull(evs, "evs");
        this.ivs = Objects.requireNonNull(ivs, "ivs");
        this.talentIds = List.copyOf(talentIds);
        this.generation = generation;
        this.breedCount = breedCount;
        this.mountUnlocked = mountUnlocked;
        this.flightUnlocked = flightUnlocked;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StatSet getEvs() {
        return evs;
    }

    public StatSet getIvs() {
        return ivs;
    }

    public List<String> getTalentIds() {
        return talentIds;
    }

    public int getGeneration() {
        return generation;
    }

    public int getBreedCount() {
        return breedCount;
    }

    public boolean isMountUnlocked() {
        return mountUnlocked;
    }

    public boolean isFlightUnlocked() {
        return flightUnlocked;
    }

    public PetRecord withDisplayName(String name) {
        return new PetRecord(name, evs, ivs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withTalentIds(List<String> ids) {
        return new PetRecord(displayName, evs, ivs, ids, generation, breedCount, mountUnlocked, flightUnlocked);
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
        return new PetRecord(displayName, newEvs, ivs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withIvs(StatSet newIvs) {
        return new PetRecord(displayName, evs, newIvs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    public PetRecord withGeneration(int newGeneration) {
        return new PetRecord(displayName, evs, ivs, talentIds, newGeneration, breedCount, mountUnlocked, flightUnlocked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PetRecord that)) {
            return false;
        }
        return generation == that.generation
            && breedCount == that.breedCount
            && mountUnlocked == that.mountUnlocked
            && flightUnlocked == that.flightUnlocked
            && displayName.equals(that.displayName)
            && evs.equals(that.evs)
            && ivs.equals(that.ivs)
            && talentIds.equals(that.talentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, evs, ivs, talentIds, generation, breedCount, mountUnlocked, flightUnlocked);
    }

    @Override
    public String toString() {
        return "PetRecord{"
            + "displayName='" + displayName + '\''
            + ", evs=" + evs
            + ", ivs=" + ivs
            + ", talentIds=" + talentIds
            + ", generation=" + generation
            + ", breedCount=" + breedCount
            + ", mountUnlocked=" + mountUnlocked
            + ", flightUnlocked=" + flightUnlocked
            + '}';
    }
}
