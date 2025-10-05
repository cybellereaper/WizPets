package com.github.cybellereaper.wizpets.core.service;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator.SplittableGenerator;
import org.bukkit.entity.Player;

/**
 * Encapsulates the deterministic logic used to breed pets into new generations while preserving
 * unlock progress and distributing stat growth.
 */
@Singleton
public final class BreedingEngine {
  private final TalentRegistryImpl registry;
  private final SplittableGenerator random;

  @Inject
  public BreedingEngine(TalentRegistryImpl registry, SplittableGenerator random) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.random = Objects.requireNonNull(random, "random");
  }

  public BreedOutcome breed(Player owner, PetRecord self, PetRecord partner) {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(self, "self");
    Objects.requireNonNull(partner, "partner");
    int generation = Math.max(self.generation(), partner.generation()) + 1;
    SplittableGenerator branch = random.split();
    StatSet childEvs = self.evs().breedWith(partner.evs(), branch.split());
    StatSet childIvs = self.ivs().breedWith(partner.ivs(), branch.split());
    List<String> childTalents = registry.inherit(self.talentIds(), partner.talentIds(), branch);
    PetRecord childRecord =
        new PetRecord(
            MessageFormat.format("{0}'s Hatchling", owner.getName()),
            childEvs,
            childIvs,
            childTalents,
            generation,
            self.breedCount() + 1,
            self.mountUnlocked() || partner.mountUnlocked(),
            self.flightUnlocked() || partner.flightUnlocked());

    PetRecord updatedPartner = partner.withBreedCount(partner.breedCount() + 1);
    return new BreedOutcome(childRecord, updatedPartner);
  }

  public record BreedOutcome(PetRecord childRecord, PetRecord updatedPartnerRecord) {
    public BreedOutcome {
      Objects.requireNonNull(childRecord, "childRecord");
      Objects.requireNonNull(updatedPartnerRecord, "updatedPartnerRecord");
    }
  }
}
