package com.github.cybellereaper.wizPets

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class WizPets : JavaPlugin() {
    lateinit var petManager: PetManager
        private set

    override fun onEnable() {
        saveDefaultConfig()
        petManager = PetManager(this)
        val petCommand = PetCommand(petManager)
        getCommand("wizpet")?.setExecutor(petCommand)
        getCommand("wizpet")?.tabCompleter = petCommand
        server.pluginManager.registerEvents(petManager, this)
        logger.info("WizPets enabled with ${Bukkit.getOnlinePlayers().size} players online")
        Bukkit.getOnlinePlayers().forEach { petManager.restorePet(it) }
    }

    override fun onDisable() {
        petManager.removeAllPets()
    }
}

private class PetCommand(private val petManager: PetManager) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can manage pets.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§e/wizpet summon §7- Summon your pet")
            sender.sendMessage("§e/wizpet dismiss §7- Dismiss your pet")
            sender.sendMessage("§e/wizpet stats §7- View pet statistics")
            sender.sendMessage("§e/wizpet talents §7- View pet talents")
            return true
        }

        return when (args[0].lowercase()) {
            "summon" -> {
                petManager.spawnOrRespawnPet(sender)
                true
            }

            "dismiss" -> {
                petManager.dismissPet(sender)
                true
            }

            "stats" -> {
                petManager.getPet(sender)?.let { pet ->
                    sender.sendMessage("§a${pet.displayName} Stats:")
                    pet.getStatBreakdown().forEach { (name, value) ->
                        sender.sendMessage("§7- §b$name§7: §f${"%.1f".format(value)}")
                    }
                } ?: sender.sendMessage("You do not have an active pet.")
                true
            }

            "talents" -> {
                petManager.getPet(sender)?.let { pet ->
                    sender.sendMessage("§d${pet.displayName} Talents:")
                    pet.talents.forEach { talent ->
                        sender.sendMessage("§7- §d${talent.displayName}§7: ${talent.description}")
                    }
                } ?: sender.sendMessage("You do not have an active pet.")
                true
            }

            else -> false
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf("summon", "dismiss", "stats", "talents")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
        }

        return mutableListOf()
    }
}
