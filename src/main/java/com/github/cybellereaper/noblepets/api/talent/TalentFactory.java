package com.github.cybellereaper.noblepets.api.talent;

@FunctionalInterface
public interface TalentFactory {
  PetTalent create();
}
