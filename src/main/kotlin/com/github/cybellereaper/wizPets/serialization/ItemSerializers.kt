package com.github.cybellereaper.wizPets.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ItemStack", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemStack) {
        val yaml = YamlConfiguration()
        yaml.set("item", value)
        encoder.encodeString(yaml.saveToString())
    }

    override fun deserialize(decoder: Decoder): ItemStack {
        val yaml = YamlConfiguration()
        yaml.loadFromString(decoder.decodeString())
        return yaml.getItemStack("item") ?: ItemStack(Material.AIR)
    }
}

object ItemMetaSerializer : KSerializer<ItemMeta> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ItemMeta", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemMeta) {
        val yaml = YamlConfiguration()
        yaml.set("meta", value.serialize())
        encoder.encodeString(yaml.saveToString())
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): ItemMeta {
        val yaml = YamlConfiguration()
        yaml.loadFromString(decoder.decodeString())
        val raw = yaml.get("meta")
        val map = when (raw) {
            is Map<*, *> -> raw as Map<String, Any?>
            else -> emptyMap()
        }
        val sanitized = mutableMapOf<String, Any>()
        map.forEach { (key, value) ->
            if (value != null) {
                sanitized[key] = value
            }
        }
        return Bukkit.getItemFactory().deserializeItemMeta(sanitized)
    }
}
