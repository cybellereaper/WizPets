package com.github.cybellereaper.wizPets.talent

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import java.util.EnumMap

/**
 * Enumeration of built-in talents inspired by Palworld roles.
 */
enum class TalentId {
    SENTINEL,
    MEDIC,
    GARDENER,
    GATHERER,
    ARTISAN,
}

/**
 * Runtime talent behaviour implementation.
 */
fun interface Talent {
    fun tick(context: TalentContext)
}

/**
 * All data required to run a talent tick.
 */
data class TalentContext(
    val armorStand: ArmorStand,
    val owner: LivingEntity,
    val level: Int,
    val carriedItems: MutableList<ItemStack>,
)

/**
 * Simple registry that maps talent identifiers to their functional implementations.
 */
class TalentRegistry(private val implementations: EnumMap<TalentId, Talent>) {
    fun get(id: TalentId): Talent = implementations[id]
        ?: error("Missing talent implementation for $id")

    companion object {
        fun default(): TalentRegistry {
            val map = EnumMap<TalentId, Talent>(TalentId::class.java)
            map[TalentId.SENTINEL] = Talent { context ->
                SentinelTalent.perform(context)
            }
            map[TalentId.MEDIC] = Talent { context ->
                MedicTalent.perform(context)
            }
            map[TalentId.GARDENER] = Talent { context ->
                GardenerTalent.perform(context)
            }
            map[TalentId.GATHERER] = Talent { context ->
                GathererTalent.perform(context)
            }
            map[TalentId.ARTISAN] = Talent { context ->
                ArtisanTalent.perform(context)
            }
            return TalentRegistry(map)
        }
    }
}

