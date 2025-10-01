package com.github.cybellereaper.wizPets.talent

import kotlinx.serialization.Serializable
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Identifier for a companion talent. Script authors can create arbitrary names while
 * built-in helpers expose a stable set of defaults.
 */
@JvmInline
@Serializable
value class TalentId(val value: String) {
    init {
        require(value.isNotBlank()) { "Talent identifier cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        val SENTINEL = TalentId("sentinel")
        val MEDIC = TalentId("medic")
        val GARDENER = TalentId("gardener")
        val GATHERER = TalentId("gatherer")
        val ARTISAN = TalentId("artisan")
    }
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
 * Registry that maps talent identifiers to their functional implementations. Supports hot-swapping
 * at runtime when scripts are reloaded.
 */
class TalentRegistry private constructor(
    private val implementations: Map<TalentId, Talent>,
) {

    fun get(id: TalentId): Talent = implementations[id.normalized()]
        ?: error("Missing talent implementation for $id")

    class Builder {
        private val map = ConcurrentHashMap<TalentId, Talent>()

        fun register(id: TalentId, talent: Talent): Builder = apply {
            map[id.normalized()] = talent
        }

        fun build(): TalentRegistry = TalentRegistry(map.toMap())
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}

private fun TalentId.normalized(): TalentId = TalentId(value.lowercase(Locale.ROOT))
