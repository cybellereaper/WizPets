package com.github.cybellereaper.wizpets.core.talent.defaults

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.talent.PetTalent

abstract class PeriodicTalent(private val intervalTicks: Int) : PetTalent {
    private var ticks = 0

    final override fun tick(pet: ActivePet) {
        ticks++
        if (ticks >= intervalTicks) {
            ticks = 0
            trigger(pet)
        }
    }

    protected abstract fun trigger(pet: ActivePet)
}
