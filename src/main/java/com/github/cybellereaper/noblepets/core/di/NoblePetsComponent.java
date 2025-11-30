package com.github.cybellereaper.noblepets.core.di;

import com.github.cybellereaper.noblepets.core.service.PetServiceImpl;
import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@Component(modules = NoblePetsModule.class)
public interface NoblePetsComponent {
  PetServiceImpl petService();

  @Component.Factory
  interface Factory {
    NoblePetsComponent create(@BindsInstance JavaPlugin plugin);
  }
}
