package com.github.cybellereaper.wizpets.core.command;

import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.SummonReason;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class WizPetCommand implements TabExecutor {
    private final PetServiceImpl service;
    private final List<String> helpLines = List.of(
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
    );

    public WizPetCommand(PetServiceImpl service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can manage pets.");
            return true;
        }

        if (args.length == 0) {
            helpLines.forEach(player::sendMessage);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "summon" -> {
                service.summon(player, SummonReason.MANUAL);
                player.sendMessage("§aYour pet answers your call.");
            }
            case "dismiss" -> {
                if (service.dismiss(player, DismissReason.MANUAL)) {
                    player.sendMessage("§cYour pet returns to its dormitory.");
                } else {
                    player.sendMessage("§eYou do not have an active pet.");
                }
            }
            case "stats" -> {
                var pet = service.activePet(player);
                if (pet == null) {
                    player.sendMessage("§eYou do not have an active pet.");
                } else {
                    player.sendMessage("§a" + pet.getRecord().displayName() + " Stats:");
                    pet.statBreakdown().forEach((type, value) ->
                        player.sendMessage("§7- §b" + type.getDisplayName() + "§7: §f" + String.format(Locale.US, "%.1f", value))
                    );
                }
            }
            case "talents" -> {
                var pet = service.activePet(player);
                if (pet == null) {
                    player.sendMessage("§eYou do not have an active pet.");
                } else {
                    player.sendMessage("§d" + pet.getRecord().displayName() + " Talents:");
                    for (PetTalent talent : pet.getTalents()) {
                        player.sendMessage("§7- §d" + talent.getDisplayName() + "§7: " + talent.getDescription());
                    }
                }
            }
            case "mount" -> {
                if (!service.mount(player)) {
                    player.sendMessage("§eYou are already mounted or have no pet.");
                } else {
                    player.sendMessage("§bYou climb onto your pet.");
                }
            }
            case "dismount" -> {
                if (!service.dismount(player)) {
                    player.sendMessage("§eYou are not currently riding your pet.");
                } else {
                    player.sendMessage("§7You hop off your pet.");
                }
            }
            case "fly" -> {
                if (!service.enableFlight(player)) {
                    player.sendMessage("§eYou are already soaring or have no pet.");
                } else {
                    player.sendMessage("§3Wings of mana lift you skyward.");
                }
            }
            case "land" -> {
                if (!service.disableFlight(player)) {
                    player.sendMessage("§eYou are not currently flying with your pet.");
                } else {
                    player.sendMessage("§7You drift back to the ground.");
                }
            }
            case "breed" -> handleBreed(player, args);
            case "debug" -> service.debugLines(player).forEach(player::sendMessage);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleBreed(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /wizpet breed <player>");
            return;
        }
        Player partner = Bukkit.getPlayerExact(args[1]);
        if (partner == null || !partner.isOnline()) {
            player.sendMessage("§cThe specified partner is not online.");
        } else if (partner.equals(player)) {
            player.sendMessage("§cYou must choose another player to breed with.");
        } else {
            service.breed(player, partner);
            player.sendMessage("§dYour pet egg hums with new potential.");
            partner.sendMessage(Component.text("§dYour pet assisted " + player.getName() + " in breeding."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (String option : Arrays.asList("summon", "dismiss", "stats", "talents", "mount", "dismount", "fly", "land", "breed", "debug")) {
                if (option.regionMatches(true, 0, args[0], 0, args[0].length())) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && "breed".equalsIgnoreCase(args[0])) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.regionMatches(true, 0, args[1], 0, args[1].length()))
                .forEach(players::add);
            return players;
        }

        return List.of();
    }
}
