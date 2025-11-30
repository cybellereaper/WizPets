package com.github.cybellereaper.noblepets.core.talent.defaults;

import com.github.cybellereaper.noblepets.api.ActivePet;
import com.github.cybellereaper.noblepets.api.talent.PetTalent;

public abstract class PeriodicTalent implements PetTalent {
  private final int intervalTicks;
  private int ticks;

  protected PeriodicTalent(int intervalTicks) {
    this.intervalTicks = intervalTicks;
  }

  @Override
  public final void tick(ActivePet pet) {
    ticks++;
    if (ticks >= intervalTicks) {
      ticks = 0;
      trigger(pet);
    }
  }

  protected abstract void trigger(ActivePet pet);
}
