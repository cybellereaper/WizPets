package com.github.cybellereaper.wizPets

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

class WizPets : JavaPlugin() {
    lateinit var petManager: PetManager
        private set

    override fun onEnable() {
        saveDefaultConfig()
        PetSpeciesRegistry.load(this)
        petManager = PetManager(this)
        val petCommand = PetCommand(this, petManager)
        getCommand("wizpet")?.setExecutor(petCommand)
        getCommand("wizpet")?.tabCompleter = petCommand
        server.pluginManager.registerEvents(petManager, this)
        server.pluginManager.registerEvents(petManager.battleManager, this)
        logger.info("WizPets enabled with ${Bukkit.getOnlinePlayers().size} players online")
        Bukkit.getOnlinePlayers().forEach { petManager.handleJoin(it) }
    }

    override fun onDisable() {
        petManager.shutdown()
    }
}

private class PetCommand(private val plugin: WizPets, private val petManager: PetManager) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        return when (args[0].lowercase(Locale.US)) {
            "list" -> handleList(sender)
            "capture" -> handleCapture(sender, args)
            "summon" -> handleSummon(sender, args)
            "dismiss" -> handleDismiss(sender)
            "select" -> handleSelect(sender, args)
            "stay" -> handleStay(sender)
            "follow" -> handleFollow(sender)
            "info" -> handleInfo(sender, args)
            "stats" -> handleInfo(sender, args)
            "talents" -> handleTalents(sender, args)
            "release" -> handleRelease(sender, args)
            "rename" -> handleRename(sender, args)
            "crops" -> handleCrops(sender)
            "incubate" -> handleIncubate(sender, args)
            "eggs" -> handleEggs(sender)
            "hatch" -> handleHatch(sender)
            "battle" -> handleBattle(sender, args)
            "admin" -> handleAdmin(sender, args)
            else -> {
                sendHelp(sender)
                true
            }
        }
    }

    private fun handleList(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        val pets = petManager.listPets(player)
        if (pets.isEmpty()) {
            player.sendMessage("§7You have not befriended any companions yet.")
            return true
        }
        player.sendMessage("§dYour Companions:")
        pets.forEach { captured ->
            val species = PetSpeciesRegistry.getSpecies(captured.speciesId)
            val activeMarker = if (petManager.getProfile(player.uniqueId).activePetId == captured.id) "§a★" else "§7☆"
            player.sendMessage("$activeMarker §b${captured.nickname}§7 - ${species?.displayName ?: captured.speciesId} (§eLvl ${captured.level}§7)")
        }
        return true
    }

    private fun handleCapture(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 2) {
            player.sendMessage("§cUsage: /wizpet capture <species> [nickname]")
            return true
        }
        val speciesId = args[1]
        val nickname = args.drop(2).joinToString(" ").takeIf { it.isNotBlank() }
        petManager.capturePet(player, speciesId, nickname)
        return true
    }

    private fun handleSummon(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size >= 2) {
            val nickname = args.drop(1).joinToString(" ")
            if (!petManager.selectPet(player, nickname)) {
                return true
            }
        } else {
            petManager.spawnActivePet(player)
        }
        return true
    }

    private fun handleDismiss(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.dismissPet(player)
        return true
    }

    private fun handleStay(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.commandStay(player)
        return true
    }

    private fun handleFollow(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.commandFollow(player)
        return true
    }

    private fun handleIncubate(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 3) {
            player.sendMessage("§cUsage: /wizpet incubate <pet one> <pet two> [count]")
            return true
        }
        val count = args.getOrNull(3)?.toIntOrNull() ?: 1
        petManager.beginIncubation(player, args[1], args[2], count)
        return true
    }

    private fun handleEggs(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.listEggs(player)
        return true
    }

    private fun handleHatch(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.hatchReadyEggs(player)
        return true
    }

    private fun handleBattle(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 2) {
            player.sendMessage("§cUsage: /wizpet battle <player>")
            return true
        }
        val target = plugin.server.getPlayerExact(args[1])
        if (target == null) {
            player.sendMessage("§c${args[1]} is not online.")
            return true
        }
        if (target.uniqueId == player.uniqueId) {
            player.sendMessage("§cYou cannot battle yourself.")
            return true
        }
        petManager.battleManager.startBattle(player, target)
        return true
    }

    private fun handleSelect(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 2) {
            player.sendMessage("§cUsage: /wizpet select <nickname>")
            return true
        }
        val nickname = args.drop(1).joinToString(" ")
        petManager.selectPet(player, nickname)
        return true
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        val target = if (args.size >= 2) {
            val nickname = args.drop(1).joinToString(" ")
            petManager.getProfile(player.uniqueId).findByNickname(nickname)
        } else {
            petManager.getProfile(player.uniqueId).getActivePet()
        }
        if (target == null) {
            player.sendMessage("§cNo matching pet found.")
            return true
        }
        val species = PetSpeciesRegistry.getSpecies(target.speciesId)
        player.sendMessage("§d${target.nickname} §7- ${species?.displayName ?: target.speciesId} (${species?.element?.displayName ?: "Unknown"})")
        player.sendMessage("§7Level: §e${target.level} §7• Experience: §e${target.experience}")
        val activePet = petManager.getActivePet(player)
        val breakdown = activePet?.getStatBreakdown() ?: species?.baseStats?.let { base ->
            StatType.entries.associate { it.displayName to (base[it] + target.ivs[it] + target.evs[it] / 32.0) }
        }
        breakdown?.forEach { (name, value) ->
            player.sendMessage("§7- §b$name§7: §f${"%.1f".format(value)}")
        }
        player.sendMessage("§7IVs: §f${formatStatSet(target.ivs)}")
        player.sendMessage("§7EVs: §f${formatStatSet(target.evs)}")
        return true
    }

    private fun handleTalents(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        val target = if (args.size >= 2) {
            val nickname = args.drop(1).joinToString(" ")
            petManager.getProfile(player.uniqueId).findByNickname(nickname)
        } else {
            petManager.getProfile(player.uniqueId).getActivePet()
        }
        if (target == null) {
            player.sendMessage("§cNo matching pet found.")
            return true
        }
        player.sendMessage("§d${target.nickname}'s Talents:")
        target.talentIds.forEach { id ->
            val talent = TalentRegistry.createTalent(id)
            if (talent != null) {
                player.sendMessage("§7- §d${talent.displayName}§7: ${talent.description}")
            } else {
                player.sendMessage("§7- §cUnknown talent: $id")
            }
        }
        return true
    }

    private fun handleRelease(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 2) {
            player.sendMessage("§cUsage: /wizpet release <nickname>")
            return true
        }
        val nickname = args.drop(1).joinToString(" ")
        petManager.releasePet(player, nickname)
        return true
    }

    private fun handleRename(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender.ensurePlayer() ?: return true
        if (args.size < 3) {
            player.sendMessage("§cUsage: /wizpet rename <old nickname> <new nickname>")
            return true
        }
        val oldName = args[1]
        val newName = args.drop(2).joinToString(" ")
        petManager.renamePet(player, oldName, newName)
        return true
    }

    private fun handleCrops(sender: CommandSender): Boolean {
        val player = sender.ensurePlayer() ?: return true
        petManager.openCropConfigurator(player)
        return true
    }

    private fun handleAdmin(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("wizpet.admin")) {
            sender.sendMessage("§cYou do not have permission to use admin controls.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /wizpet admin <give|reload> ...")
            return true
        }
        return when (args[1].lowercase(Locale.US)) {
            "give" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /wizpet admin give <player> <species> [nickname]")
                    true
                } else {
                    val target = plugin.server.getPlayer(args[2])
                    if (target == null) {
                        sender.sendMessage("§cThat player is not online.")
                        true
                    } else {
                        val nickname = if (args.size > 4) args.drop(4).joinToString(" ") else null
                        val result = petManager.adminGivePet(target, args[3], nickname)
                        if (result != null) {
                            sender.sendMessage("§aGave ${target.name} a ${result.nickname}.")
                        }
                        true
                    }
                }
            }

            "reload" -> {
                petManager.reloadSpecies()
                sender.sendMessage("§aReloaded species definitions.")
                true
            }

            else -> {
                sender.sendMessage("§cUnknown admin subcommand.")
                true
            }
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§dWizPets Commands:")
        sender.sendMessage("§e/wizpet list §7- View your pet collection")
        sender.sendMessage("§e/wizpet capture <species> [nickname] §7- Befriend a new pet")
        sender.sendMessage("§e/wizpet summon [nickname] §7- Summon your active or chosen pet")
        sender.sendMessage("§e/wizpet stay §7- Instruct your active pet to hold position")
        sender.sendMessage("§e/wizpet follow §7- Resume following you")
        sender.sendMessage("§e/wizpet crops §7- Configure farming targets via an anvil")
        sender.sendMessage("§e/wizpet incubate <pet one> <pet two> [count] §7- Start incubating eggs between two pets")
        sender.sendMessage("§e/wizpet eggs §7- Review incubating eggs")
        sender.sendMessage("§e/wizpet hatch §7- Hatch all ready eggs together")
        sender.sendMessage("§e/wizpet battle <player> §7- Challenge another tamer to a pet battle")
        sender.sendMessage("§e/wizpet talents [nickname] §7- Inspect pet talents")
        sender.sendMessage("§e/wizpet release <nickname> §7- Release a pet")
        sender.sendMessage("§e/wizpet admin ... §7- Administrative controls")
    }

    private fun CommandSender.ensurePlayer(): Player? {
        if (this !is Player) {
            sendMessage("Only players can use this command.")
            return null
        }
        return this
    }

    private fun formatStatSet(stats: StatSet): String = listOf(
        "HP" to stats.health,
        "ATK" to stats.attack,
        "DEF" to stats.defense,
        "MAG" to stats.magic,
        "SPD" to stats.speed
    ).joinToString(", ") { "${it.first}:${"%.0f".format(it.second)}" }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (args.isEmpty()) return mutableListOf()
        return when (args.size) {
            1 -> listOf(
                "list",
                "capture",
                "summon",
                "dismiss",
                "select",
                "stay",
                "follow",
                "info",
                "stats",
                "talents",
                "release",
                "rename",
                "crops",
                "incubate",
                "eggs",
                "hatch",
                "battle",
                "admin"
            )
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()

            2 -> when (args[0].lowercase(Locale.US)) {
                "capture" -> PetSpeciesRegistry.getAllSpecies().map { it.id }.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
                "select", "info", "stats", "talents", "release", "rename", "incubate" -> {
                    val player = sender as? Player ?: return mutableListOf()
                    petManager.listPets(player).map { it.nickname }.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
                }
                "battle" -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
                "admin" -> listOf("give", "reload").filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
                else -> mutableListOf()
            }

            3 -> when {
                args[0].equals("admin", true) && args[1].equals("give", true) ->
                    plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }.toMutableList()

                args[0].equals("incubate", true) -> {
                    val player = sender as? Player ?: return mutableListOf()
                    petManager.listPets(player).map { it.nickname }.filter { it.startsWith(args[2], ignoreCase = true) }.toMutableList()
                }

                else -> mutableListOf()
            }

            4 -> if (args[0].equals("admin", true) && args[1].equals("give", true)) {
                PetSpeciesRegistry.getAllSpecies().map { it.id }.filter { it.startsWith(args[3], ignoreCase = true) }.toMutableList()
            } else {
                mutableListOf()
            }

            else -> mutableListOf()
        }
    }
}
