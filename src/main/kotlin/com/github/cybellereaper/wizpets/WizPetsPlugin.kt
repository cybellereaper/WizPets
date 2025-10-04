package com.github.cybellereaper.wizpets

import com.github.cybellereaper.wizpets.core.command.WizPetCommand
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl
import org.bukkit.plugin.java.JavaPlugin

class WizPetsPlugin : JavaPlugin() {
    lateinit var service: PetServiceImpl
        private set

    override fun onEnable() {
        saveDefaultConfig()
        service = PetServiceImpl(this)
        val command = WizPetCommand(service)
        getCommand("wizpet")?.apply {
            setExecutor(command)
            tabCompleter = command
        }
        server.pluginManager.registerEvents(service, this)
        service.restoreOnlinePlayers()
        logger.info("WizPets enabled with developer API exposing ${service.talents().size} talents.")
    }

    override fun onDisable() {
        if (::service.isInitialized) {
            service.shutdown()
            service.unregister()
        }
    }
}
