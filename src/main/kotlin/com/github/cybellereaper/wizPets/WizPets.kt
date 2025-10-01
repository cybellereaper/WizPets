package com.github.cybellereaper.wizPets

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class WizPets : JavaPlugin() {
    lateinit var particleController: ParticleController
        private set
    lateinit var scriptManager: PetScriptManager
        private set
    lateinit var petManager: PetManager
        private set

    override fun onEnable() {
        saveDefaultConfig()
        particleController = ParticleController(this)
        scriptManager = PetScriptManager(this, particleController)
        val scriptsLoaded = scriptManager.reloadScripts()
        logger.info("Loaded $scriptsLoaded pet definitions")
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
            sender.sendMessage("§e/wizpet summon §7- Summon or respawn your pet")
            sender.sendMessage("§e/wizpet dismiss §7- Dismiss your pet")
            sender.sendMessage("§e/wizpet stats §7- View pet statistics")
            sender.sendMessage("§e/wizpet talents §7- View pet talents")
            sender.sendMessage("§e/wizpet mount §7- Ride your pet")
            sender.sendMessage("§e/wizpet dismount §7- Stop riding your pet")
            sender.sendMessage("§e/wizpet fly §7- Take to the skies with your pet")
            sender.sendMessage("§e/wizpet land §7- Land safely and end flight")
            sender.sendMessage("§e/wizpet breed <player> §7- Breed your pet with another player's")
            sender.sendMessage("§e/wizpet debug §7- Show stored pet data")
            sender.sendMessage("§e/wizpet script list §7- Show available pet scripts")
            sender.sendMessage("§e/wizpet script set <name> §7- Apply a pet script to your companion")
            sender.sendMessage("§e/wizpet script reload §7- Reload Lua pet scripts from disk")
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

            "mount" -> {
                petManager.mountPet(sender)
                true
            }

            "dismount" -> {
                petManager.dismountPet(sender)
                true
            }

            "fly" -> {
                petManager.enableFlight(sender)
                true
            }

            "land" -> {
                petManager.disableFlight(sender)
                true
            }

            "breed" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /wizpet breed <player>")
                } else {
                    petManager.breedPets(sender, args[1])
                }
                true
            }

            "debug" -> {
                petManager.showDebug(sender)
                true
            }

            "script" -> {
                if (args.size == 1) {
                    sender.sendMessage("§cUsage: /wizpet script <list|set <name>|reload>")
                } else {
                    when (args[1].lowercase()) {
                        "list" -> {
                            val scripts = petManager.availableScripts()
                            if (scripts.isEmpty()) {
                                sender.sendMessage("§eNo scripts are currently loaded.")
                            } else {
                                sender.sendMessage("§aLoaded pet scripts: §f${scripts.joinToString(", ")}")
                            }
                        }

                        "reload" -> petManager.reloadScripts(sender)

                        "set" -> {
                            if (args.size < 3) {
                                sender.sendMessage("§cUsage: /wizpet script set <name>")
                            } else {
                                petManager.assignScript(sender, args[2])
                            }
                        }

                        else -> petManager.assignScript(sender, args[1])
                    }
                }
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
            return mutableListOf(
                "summon",
                "dismiss",
                "stats",
                "talents",
                "mount",
                "dismount",
                "fly",
                "land",
                "breed",
                "debug",
                "script"
            )
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
        }

        if (args.size == 2 && args[0].equals("breed", ignoreCase = true)) {
            return Bukkit.getOnlinePlayers()
                .filter { it.name.startsWith(args[1], ignoreCase = true) }
                .map { it.name }
                .toMutableList()
        }

        if (args.size == 2 && args[0].equals("script", ignoreCase = true)) {
            return mutableListOf("list", "reload", "set")
                .filter { it.startsWith(args[1], ignoreCase = true) }
                .toMutableList()
        }

        if (args.size == 3 && args[0].equals("script", ignoreCase = true) && args[1].equals("set", ignoreCase = true)) {
            return petManager.availableScripts()
                .filter { it.startsWith(args[2], ignoreCase = true) }
                .toMutableList()
        }

        return mutableListOf()
    }
}
