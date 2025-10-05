package com.github.cybellereaper.wizpets.api.talent;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.StatType;
import org.bukkit.entity.LivingEntity;

/**
 * Represents a behaviour module that can be attached to an active pet.
 */
public interface PetTalent {
    String getId();

    String getDisplayName();

    String getDescription();

    default void onSummon(ActivePet pet) {
    }

    default void tick(ActivePet pet) {
    }

    default void onAttack(ActivePet pet, LivingEntity target, double baseDamage) {
    }

    default void onDismiss(ActivePet pet) {
    }

    default double modifyStat(ActivePet pet, StatType stat, double current) {
        return current;
    }
}
