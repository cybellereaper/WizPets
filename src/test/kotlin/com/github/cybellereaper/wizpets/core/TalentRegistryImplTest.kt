package com.github.cybellereaper.wizpets.core

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import com.github.cybellereaper.wizpets.api.talent.TalentFactory
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class DummyTalent : PetTalent {
    override val id: String = "dummy"
    override val displayName: String = "Dummy"
    override val description: String = "Does nothing"
    override fun modifyStat(pet: ActivePet, stat: StatType, current: Double): Double = current + 1
}

class TalentRegistryImplTest {
    @Test
    fun `register and instantiate talents`() {
        val registry = TalentRegistryImpl()
        registry.register(TalentFactory { DummyTalent() })
        val talents = registry.instantiate(listOf("dummy"))
        assertEquals(1, talents.size)
        assertEquals("Dummy", talents.first().displayName)
    }

    @Test
    fun `registering duplicate without replace fails`() {
        val registry = TalentRegistryImpl()
        registry.register(TalentFactory { DummyTalent() })
        assertFailsWith<IllegalArgumentException> {
            registry.register(TalentFactory { DummyTalent() })
        }
    }

    @Test
    fun `roll returns ids from registry`() {
        val registry = TalentRegistryImpl()
        registry.register(TalentFactory { DummyTalent() })
        val roll = registry.roll(Random(0), 3)
        assertTrue(roll.all { it == "dummy" })
    }

    @Test
    fun `inherit falls back to registry when parents empty`() {
        val registry = TalentRegistryImpl()
        registry.register(TalentFactory { DummyTalent() })
        val inherited = registry.inherit(emptyList(), emptyList(), Random(1), count = 2)
        assertEquals(2, inherited.size)
        assertTrue(inherited.all { it == "dummy" })
    }
}
