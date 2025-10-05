package com.github.cybellereaper.wizpets.core.di;

import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchModelEngine;
import com.github.cybellereaper.wizpets.api.persistence.PetPersistence;
import com.github.cybellereaper.wizpets.core.config.PluginConfig;
import com.github.cybellereaper.wizpets.core.model.blockbench.BlockbenchModelEngineImpl;
import com.github.cybellereaper.wizpets.core.persistence.PetStorage;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.random.RandomGenerator.SplittableGenerator;
import org.bukkit.plugin.java.JavaPlugin;

@Module
public final class WizPetsModule {
  private WizPetsModule() {}

  @Provides
  @Singleton
  static PluginConfig pluginConfig(JavaPlugin plugin) {
    return new PluginConfig(plugin.getConfig());
  }

  @Provides
  @Singleton
  static PetPersistence petPersistence(JavaPlugin plugin) {
    return new PetStorage(plugin);
  }

  @Provides
  @Singleton
  static BlockbenchModelEngine blockbenchModelEngine(JavaPlugin plugin) {
    BlockbenchModelEngineImpl engine = new BlockbenchModelEngineImpl();
    try (var stream = plugin.getResource("models/pets/default.json")) {
      if (stream != null) {
        engine.registerModel("wizpet_default", stream);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load default Blockbench model", e);
    }
    return engine;
  }

  @Provides
  @Singleton
  static TalentRegistryImpl talentRegistry() {
    return new TalentRegistryImpl();
  }

  @Provides
  @Singleton
  static SplittableGenerator randomGenerator() {
    return new SplittableRandom(System.nanoTime());
  }

  @Provides
  @Singleton
  static ExecutorService virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
