package com.github.cybellereaper.wizpets;

import com.github.cybellereaper.wizpets.core.command.WizPetCommand;
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WizPetsPlugin extends JavaPlugin {
    private PetServiceImpl service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        service = new PetServiceImpl(this);
        WizPetCommand command = new WizPetCommand(service);
        PluginCommand pluginCommand = getCommand("wizpet");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(service, this);
        service.restoreOnlinePlayers();
        getLogger().info(() -> "WizPets enabled with developer API exposing " + service.talents().size() + " talents.");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
            service.unregister();
            service = null;
        }
    }

    public PetServiceImpl getService() {
        return service;
    }
}
