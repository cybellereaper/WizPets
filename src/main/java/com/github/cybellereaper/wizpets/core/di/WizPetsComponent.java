package com.github.cybellereaper.wizpets.core.di;

import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@Component(modules = WizPetsModule.class)
public interface WizPetsComponent {
  PetServiceImpl petService();

  @Component.Factory
  interface Factory {
    WizPetsComponent create(@BindsInstance JavaPlugin plugin);
  }
}
