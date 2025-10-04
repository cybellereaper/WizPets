package com.github.cybellereaper.wizpets.core.talent

import com.github.cybellereaper.wizpets.api.talent.PetTalent
import com.github.cybellereaper.wizpets.api.talent.PetTalentDescriptor
import com.github.cybellereaper.wizpets.api.talent.TalentFactory
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView
import kotlin.random.Random

class TalentRegistryImpl : TalentRegistryView {
    private val factories = linkedMapOf<String, TalentFactoryWrapper>()

    fun register(factory: TalentFactory, replace: Boolean = false) {
        val sample = factory.create()
        if (!replace && factories.containsKey(sample.id)) {
            throw IllegalArgumentException("Talent with id ${sample.id} already registered")
        }
        factories[sample.id] = TalentFactoryWrapper(sample, factory)
    }

    fun unregister(id: String) {
        factories.remove(id)
    }

    fun instantiate(ids: List<String>): List<PetTalent> = ids.mapNotNull { factories[it]?.create() }

    fun roll(random: Random, count: Int = 2): List<String> {
        if (factories.isEmpty()) return emptyList()
        val keys = factories.keys.toList()
        return List(count) { keys[random.nextInt(keys.size)] }
    }

    fun inherit(parentA: List<String>, parentB: List<String>, random: Random, count: Int = 2): List<String> {
        val pool = (parentA + parentB).distinct().toMutableList()
        if (pool.isEmpty()) {
            return roll(random, count)
        }
        pool.shuffle(random)
        val chosen = pool.take(count).toMutableList()
        while (chosen.size < count) {
            chosen += roll(random, 1)
        }
        return chosen
    }

    override fun iterator(): Iterator<PetTalentDescriptor> = factories.values.map { it.descriptor }.iterator()

    override fun get(id: String): PetTalentDescriptor? = factories[id]?.descriptor

    override val size: Int get() = factories.size

    private class TalentFactoryWrapper(sample: PetTalent, private val delegate: TalentFactory) : TalentFactory {
        val descriptor = PetTalentDescriptor(sample.id, sample.displayName, sample.description)
        override fun create(): PetTalent = delegate.create()
    }
}
