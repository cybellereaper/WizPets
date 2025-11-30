package com.github.cybellereaper.noblepets.core.service;

import com.github.cybellereaper.noblepets.api.PetRecord;
import com.github.cybellereaper.noblepets.api.StatSet;
import com.github.cybellereaper.noblepets.core.talent.TalentRegistryImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.text.MessageFormat;
import java.util.List;
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
    this.registry = registry;
    this.random = random;
  }

  public BreedOutcome breed(Player owner, PetRecord self, PetRecord partner) {
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

  public record BreedOutcome(PetRecord childRecord, PetRecord updatedPartnerRecord) {}
}
