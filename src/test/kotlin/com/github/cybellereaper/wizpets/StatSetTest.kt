package com.github.cybellereaper.wizpets

import com.github.cybellereaper.wizpets.api.StatSet
import com.github.cybellereaper.wizpets.api.StatType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StatSetTest {
    @Test
    fun `breedWith mixes stats from parents`() {
        val parentA = StatSet(health = 40.0, attack = 10.0, defense = 12.0, magic = 8.0)
        val parentB = StatSet(health = 30.0, attack = 18.0, defense = 9.0, magic = 11.0)
        val child = parentA.breedWith(parentB, Random(42))

        StatType.entries.forEach { type ->
            val min = minOf(parentA[type], parentB[type]) - 3.0
            val max = maxOf(parentA[type], parentB[type]) + 3.0
            assertTrue(child[type] in min..max, "${type.name} should be within blended bounds")
        }
    }

    @Test
    fun `random generators produce varied results`() {
        val sampleA = StatSet.randomEV(Random(1))
        val sampleB = StatSet.randomEV(Random(2))
        assertNotEquals(sampleA, sampleB)
        StatType.entries.forEach { type ->
            assertTrue(sampleA[type] >= 0.0)
        }
    }
}
