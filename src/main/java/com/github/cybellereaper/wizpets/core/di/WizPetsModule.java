package com.github.cybellereaper.wizpets.core.di;

import com.github.cybellereaper.wizpets.api.persistence.PetPersistence;
import com.github.cybellereaper.wizpets.core.config.PluginConfig;
import com.github.cybellereaper.wizpets.core.persistence.PetStorage;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
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
