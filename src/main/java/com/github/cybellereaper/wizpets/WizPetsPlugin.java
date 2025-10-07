package com.github.cybellereaper.wizpets;

import com.github.cybellereaper.wizpets.core.command.WizPetCommand;
import com.github.cybellereaper.wizpets.core.di.DaggerWizPetsComponent;
import com.github.cybellereaper.wizpets.core.di.WizPetsComponent;
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import com.github.cybellereaper.wizpets.nova.bridge.NovaPaperBridge;
import com.github.cybellereaper.wizpets.nova.runtime.NovaRuntime;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class WizPetsPlugin extends JavaPlugin {
  private WizPetsComponent component;
  private PetServiceImpl service;
  private NovaRuntime novaRuntime;
  private NovaPaperBridge novaPaperBridge;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    component = DaggerWizPetsComponent.factory().create(this);
    service = component.petService();
    WizPetCommand command = new WizPetCommand(service, getServer());
    PluginCommand pluginCommand = getCommand("wizpet");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(command);
      pluginCommand.setTabCompleter(command);
    }
    getServer().getPluginManager().registerEvents(service, this);
    service.restoreOnlinePlayers();
    getLogger()
        .info(
            () ->
                "WizPets enabled with developer API exposing "
                    + service.talents().size()
                    + " talents.");

    novaRuntime = NovaRuntime.createDefault();
    novaPaperBridge = new NovaPaperBridge(this, getServer().getPluginManager());
    getServer()
        .getServicesManager()
        .register(NovaRuntime.class, novaRuntime, this, ServicePriority.Normal);
  }

  @Override
  public void onDisable() {
    if (service != null) {
      service.shutdown();
      service.unregister();
      service.close();
      service = null;
    }
    if (novaRuntime != null) {
      novaRuntime.close();
      getServer().getServicesManager().unregister(NovaRuntime.class, novaRuntime);
      novaRuntime = null;
    }
    novaPaperBridge = null;
    component = null;
  }

  public PetServiceImpl getService() {
    return service;
  }

  public NovaRuntime getNovaRuntime() {
    return novaRuntime;
  }

  public NovaPaperBridge getNovaPaperBridge() {
    return novaPaperBridge;
  }
}
