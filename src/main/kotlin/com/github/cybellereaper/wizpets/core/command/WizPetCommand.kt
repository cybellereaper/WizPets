package com.github.cybellereaper.wizpets.core.command

import com.github.cybellereaper.wizpets.core.service.PetServiceImpl
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class WizPetCommand(private val service: PetServiceImpl) : TabExecutor {
    private val helpLines = listOf(
        "§e/wizpet summon §7- Summon or respawn your pet",
        "§e/wizpet dismiss §7- Dismiss your pet",
        "§e/wizpet stats §7- View pet statistics",
        "§e/wizpet talents §7- View pet talents",
        "§e/wizpet mount §7- Ride your pet",
        "§e/wizpet dismount §7- Stop riding your pet",
        "§e/wizpet fly §7- Take to the skies with your pet",
        "§e/wizpet land §7- Land safely and end flight",
        "§e/wizpet breed <player> §7- Breed your pet with another player's",
        "§e/wizpet debug §7- Show stored pet data"
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can manage pets.")
            return true
        }

        if (args.isEmpty()) {
            helpLines.forEach(sender::sendMessage)
            return true
        }

        when (args[0].lowercase()) {
            "summon" -> {
                service.summon(sender, com.github.cybellereaper.wizpets.api.SummonReason.MANUAL)
                sender.sendMessage("§aYour pet answers your call.")
            }

            "dismiss" -> {
                if (service.dismiss(sender, com.github.cybellereaper.wizpets.api.DismissReason.MANUAL)) {
                    sender.sendMessage("§cYour pet returns to its dormitory.")
                } else {
                    sender.sendMessage("§eYou do not have an active pet.")
                }
            }

            "stats" -> {
                val pet = service.activePet(sender)
                if (pet == null) {
                    sender.sendMessage("§eYou do not have an active pet.")
                } else {
                    sender.sendMessage("§a${pet.record.displayName} Stats:")
                    pet.statBreakdown().forEach { (type, value) ->
                        sender.sendMessage("§7- §b${type.displayName}§7: §f${"%.1f".format(value)}")
                    }
                }
            }

            "talents" -> {
                val pet = service.activePet(sender)
                if (pet == null) {
                    sender.sendMessage("§eYou do not have an active pet.")
                } else {
                    sender.sendMessage("§d${pet.record.displayName} Talents:")
                    pet.talents.forEach { talent ->
                        sender.sendMessage("§7- §d${talent.displayName}§7: ${talent.description}")
                    }
                }
            }

            "mount" -> {
                if (!service.mount(sender)) {
                    sender.sendMessage("§eYou are already mounted or have no pet.")
                } else {
                    sender.sendMessage("§bYou climb onto your pet.")
                }
            }

            "dismount" -> {
                if (!service.dismount(sender)) {
                    sender.sendMessage("§eYou are not currently riding your pet.")
                } else {
                    sender.sendMessage("§7You hop off your pet.")
                }
            }

            "fly" -> {
                if (!service.enableFlight(sender)) {
                    sender.sendMessage("§eYou are already soaring or have no pet.")
                } else {
                    sender.sendMessage("§3Wings of mana lift you skyward.")
                }
            }

            "land" -> {
                if (!service.disableFlight(sender)) {
                    sender.sendMessage("§eYou are not currently flying with your pet.")
                } else {
                    sender.sendMessage("§7You drift back to the ground.")
                }
            }

            "breed" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /wizpet breed <player>")
                } else {
                    val partner = Bukkit.getPlayerExact(args[1])
                    if (partner == null || !partner.isOnline) {
                        sender.sendMessage("§cThe specified partner is not online.")
                    } else if (partner == sender) {
                        sender.sendMessage("§cYou must choose another player to breed with.")
                    } else {
                        service.breed(sender, partner)
                        sender.sendMessage("§dYour pet egg hums with new potential.")
                        partner.sendMessage(Component.text("§dYour pet assisted ${sender.name} in breeding."))
                    }
                }
            }

            "debug" -> {
                service.debugLines(sender).forEach(sender::sendMessage)
            }

            else -> return false
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return listOf(
                "summon",
                "dismiss",
                "stats",
                "talents",
                "mount",
                "dismount",
                "fly",
                "land",
                "breed",
                "debug"
            ).filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }

        if (args.size == 2 && args[0].equals("breed", ignoreCase = true)) {
            return Bukkit.getOnlinePlayers()
                .filter { it.name.startsWith(args[1], ignoreCase = true) }
                .map { it.name }
                .toMutableList()
        }

        return mutableListOf()
    }
}
