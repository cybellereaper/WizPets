package com.github.cybellereaper.wizPets

import com.github.cybellereaper.wizPets.config.SpeciesConfig
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.service.PetManager
import com.github.cybellereaper.wizPets.pet.service.PetRepository
import com.github.cybellereaper.wizPets.talent.TalentRegistry
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.Locale
import java.util.UUID

class WizPets : JavaPlugin(), TabExecutor {

    private lateinit var speciesConfig: SpeciesConfig
    private lateinit var petRepository: PetRepository
    private lateinit var petManager: PetManager
    private lateinit var talentRegistry: TalentRegistry

    private var tickTask: BukkitTask? = null
    private var speciesIndex: Map<String, PetSpecies> = emptyMap()

    override fun onEnable() {
        saveDefaultConfig()
        speciesConfig = SpeciesConfig(this)
        petRepository = PetRepository(File(dataFolder, "pets"))
        talentRegistry = TalentRegistry.default()
        speciesIndex = speciesConfig.load()
        petManager = PetManager(talentRegistry, petRepository) { speciesIndex }
        petManager.load()

        server.pluginManager.registerEvents(petManager, this)
        getCommand("wizpet")?.setExecutor(this)
        getCommand("wizpet")?.tabCompleter = this

        tickTask = server.scheduler.runTaskTimer(this, Runnable { petManager.tick() }, 20L, 20L)
        logger.info("WizPets enabled with ${speciesIndex.size} species")
    }

    override fun onDisable() {
        tickTask?.cancel()
        petManager.saveAll()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can run WizPets commands.")
            return true
        }
        if (args.isEmpty()) {
            return showHelp(sender)
        }
        return when (args[0].lowercase(Locale.getDefault())) {
            "list" -> {
                listPets(sender)
                true
            }
            "summon", "deploy" -> {
                summonPet(sender, args.getOrNull(1))
                true
            }
            "dismiss", "recall" -> {
                petManager.dismiss(sender)
                sender.sendMessage("§7All companions returned to storage.")
                true
            }
            "capture" -> {
                captureTarget(sender)
                true
            }
            "admin" -> handleAdmin(sender, args.drop(1))
            else -> showHelp(sender)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        if (args.isEmpty()) return mutableListOf("list", "summon", "dismiss", "capture", "admin")
        return when (args.size) {
            1 -> listOf("list", "summon", "dismiss", "capture", "admin")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
            2 -> if (args[0].equals("summon", true) || args[0].equals("deploy", true)) {
                petManager.getPets((sender as? Player)?.uniqueId ?: return mutableListOf())
                    .mapIndexed { index, pet -> "${index + 1}:${pet.nickname}" }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()
            } else if (args[0].equals("admin", true)) {
                mutableListOf("give", "reload")
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()
            } else {
                mutableListOf()
            }
            3 -> if (args[0].equals("admin", true) && args[1].equals("give", true)) {
                Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.startsWith(args[2], ignoreCase = true) }
                    .toMutableList()
            } else {
                mutableListOf()
            }
            4 -> if (args[0].equals("admin", true) && args[1].equals("give", true)) {
                speciesIndex.keys
                    .filter { it.startsWith(args[3], ignoreCase = true) }
                    .toMutableList()
            } else {
                mutableListOf()
            }
            else -> mutableListOf()
        }
    }

    private fun showHelp(player: Player): Boolean {
        player.sendMessage("§6WizPets commands:")
        player.sendMessage("§e/wizpet list §7- show stored companions")
        player.sendMessage("§e/wizpet summon <slot|uuid> §7- deploy a companion")
        player.sendMessage("§e/wizpet dismiss §7- recall active companion")
        player.sendMessage("§e/wizpet capture §7- convert the targeted creature into a companion")
        if (player.hasPermission("wizpet.admin")) {
            player.sendMessage("§c/wizpet admin give <player> <species> [name]")
            player.sendMessage("§c/wizpet admin reload")
        }
        return true
    }

    private fun listPets(player: Player) {
        val pets = petManager.getPets(player.uniqueId)
        if (pets.isEmpty()) {
            player.sendMessage("§7You do not own any companions yet.")
            return
        }
        player.sendMessage("§aYour companions:")
        pets.forEachIndexed { index, pet ->
            val stats = pet.investments.toSheet(pet.species.statSheet).atLevel(pet.level)
            player.sendMessage("§f${index + 1}. §b${pet.nickname} §7(${pet.species.displayName}) §8- Lv.${pet.level} HP:${stats.maxHealth}")
        }
    }

    private fun summonPet(player: Player, identifier: String?) {
        if (identifier == null) {
            player.sendMessage("§cSpecify the pet slot number or UUID to summon.")
            return
        }
        val pets = petManager.getPets(player.uniqueId)
        val pet = identifier.toIntOrNull()?.let { index ->
            pets.getOrNull(index - 1)
        } ?: runCatching { UUID.fromString(identifier) }.getOrNull()?.let { id ->
            pets.firstOrNull { it.id == id }
        }

        if (pet == null) {
            player.sendMessage("§cNo companion matches that identifier.")
            return
        }
        petManager.summon(player, pet.id)
    }

    private fun captureTarget(player: Player) {
        val target = findCaptureTarget(player)
        if (target == null) {
            player.sendMessage("§cNo capturable creature in sight.")
            return
        }
        petManager.capture(player, target)
    }

    private fun findCaptureTarget(player: Player): LivingEntity? {
        val eye = player.eyeLocation
        val result = player.world.rayTraceEntities(
            eye,
            eye.direction,
            12.0,
            0.3,
        ) { entity ->
            entity is LivingEntity && entity != player && entity.isValid && !entity.isDead
        }

        return result?.hitEntity as? LivingEntity
    }

    private fun handleAdmin(sender: Player, args: List<String>): Boolean {
        if (!sender.hasPermission("wizpet.admin")) {
            sender.sendMessage("§cYou do not have permission for admin commands.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /wizpet admin <give|reload>")
            return true
        }
        return when (args[0].lowercase(Locale.getDefault())) {
            "give" -> {
                val targetName = args.getOrNull(1)
                val speciesId = args.getOrNull(2)
                if (targetName == null || speciesId == null) {
                    sender.sendMessage("§cUsage: /wizpet admin give <player> <species> [name]")
                    true
                } else {
                    val target = Bukkit.getPlayerExact(targetName)
                    if (target == null) {
                        sender.sendMessage("§cThat player is not online.")
                        true
                    } else {
                        petManager.grant(target, speciesId, args.getOrNull(3))
                        sender.sendMessage("§aGranted $speciesId to ${target.name}.")
                        true
                    }
                }
            }
            "reload" -> {
                petManager.saveAll()
                reloadConfig()
                speciesIndex = speciesConfig.load()
                petManager.load()
                sender.sendMessage("§aWizPets configuration reloaded.")
                true
            }
            else -> {
                sender.sendMessage("§cUnknown admin subcommand.")
                true
            }
        }
    }
}

