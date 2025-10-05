package com.github.cybellereaper.wizpets.api.talent;

/**
 * Read-only view of the registered talent factories.
 */
public interface TalentRegistryView extends Iterable<PetTalentDescriptor> {
    PetTalentDescriptor get(String id);

    int size();
}
