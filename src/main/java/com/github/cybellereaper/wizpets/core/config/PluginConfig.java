package com.github.cybellereaper.wizpets.core.config;

import lombok.NonNull;
import org.bukkit.configuration.file.FileConfiguration;

public record PluginConfig(@NonNull FileConfiguration configuration) {
  public boolean isAutoSummon() {
    return configuration.getBoolean("autosummon", true);
  }

  public String getMountUnlockMessage() {
    return configuration.getString(
        "messages.mountUnlock", "§bYour bond deepens, allowing you to ride your pet!");
  }

  public String getFlightUnlockMessage() {
    return configuration.getString("messages.flightUnlock", "§3Your pet lifts you into the skies!");
  }

  public String getDefaultPetName() {
    return configuration.getString("defaults.displayName", "Wisp Familiar");
  }

  public String getDefaultModelId() {
    return configuration.getString("defaults.modelId", "wizpet_default");
  }
}
