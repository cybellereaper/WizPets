package com.github.cybellereaper.noblepets.core.service;

import com.github.cybellereaper.noblepets.api.PetRecord;
import com.github.cybellereaper.noblepets.api.talent.PetTalent;
import com.github.cybellereaper.noblepets.core.talent.TalentRegistryImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.random.RandomGenerator.SplittableGenerator;

/** Resolves persisted pet records into live talent instances. */
@Singleton
public final class PetTalentResolver {
  private final TalentRegistryImpl registry;
  private final SplittableGenerator random;
  private final ExecutorService executor;

  @Inject
  public PetTalentResolver(
      TalentRegistryImpl registry, SplittableGenerator random, ExecutorService executor) {
    this.registry = registry;
    this.random = random;
    this.executor = executor;
  }

  public ResolvedTalents resolve(PetRecord source) {
    PetRecord sanitized = ensureTalentIds(source);
    List<PetTalent> talents =
        CompletableFuture.supplyAsync(() -> registry.instantiate(sanitized.talentIds()), executor)
            .join();
    return new ResolvedTalents(sanitized, List.copyOf(talents));
  }

  private PetRecord ensureTalentIds(PetRecord record) {
    List<PetTalent> preview = registry.instantiate(record.talentIds());
    if (preview.size() != record.talentIds().size() || preview.isEmpty()) {
      SplittableGenerator branch = random.split();
      return record.withTalentIds(registry.roll(branch));
    }
    return record;
  }

  public record ResolvedTalents(PetRecord record, List<PetTalent> talents) {}
}
