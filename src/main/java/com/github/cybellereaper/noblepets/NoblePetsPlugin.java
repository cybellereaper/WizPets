package com.github.cybellereaper.noblepets;

import com.github.cybellereaper.noblepets.core.command.NoblePetCommand;
import com.github.cybellereaper.noblepets.core.di.DaggerNoblePetsComponent;
import com.github.cybellereaper.noblepets.core.di.NoblePetsComponent;
import com.github.cybellereaper.noblepets.core.service.PetServiceImpl;
import com.github.cybellereaper.noblepets.nova.bridge.NovaPaperBridge;
import com.github.cybellereaper.noblepets.nova.runtime.NovaRuntime;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NoblePetsPlugin extends JavaPlugin {
  private NoblePetsComponent component;
  private PetServiceImpl service;
  private NovaRuntime novaRuntime;
  private NovaPaperBridge novaPaperBridge;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    component = DaggerNoblePetsComponent.factory().create(this);
    service = component.petService();
    NoblePetCommand command = new NoblePetCommand(service, getServer());
    PluginCommand pluginCommand = getCommand("noblepet");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(command);
      pluginCommand.setTabCompleter(command);
    }
    getServer().getPluginManager().registerEvents(service, this);
    service.restoreOnlinePlayers();
    getLogger()
        .info(
            () ->
                "NoblePets enabled with developer API exposing "
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
