package com.github.cybellereaper.wizpets.core.config

import org.bukkit.configuration.file.FileConfiguration

class PluginConfig(private val config: FileConfiguration) {
    val autoSummon: Boolean get() = config.getBoolean("autosummon", true)
    val mountUnlockMessage: String get() = config.getString("messages.mountUnlock") ?: "§bYour bond deepens, allowing you to ride your pet!"
    val flightUnlockMessage: String get() = config.getString("messages.flightUnlock") ?: "§3Your pet lifts you into the skies!"
    val defaultPetName: String get() = config.getString("defaults.displayName") ?: "Wisp Familiar"
}
