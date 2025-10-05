package com.github.cybellereaper.wizpets.core.command;

import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.SummonReason;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jooq.lambda.Seq;

public final class WizPetCommand implements TabExecutor {
  private final PetServiceImpl service;
  private final List<String> helpLines =
      ImmutableList.of(
          "§e/wizpet summon §7- Summon or respawn your pet",
          "§e/wizpet dismiss §7- Dismiss your pet",
          "§e/wizpet stats §7- View pet statistics",
          "§e/wizpet talents §7- View pet talents",
          "§e/wizpet mount §7- Ride your pet",
          "§e/wizpet dismount §7- Stop riding your pet",
          "§e/wizpet fly §7- Take to the skies with your pet",
          "§e/wizpet land §7- Land safely and end flight",
          "§e/wizpet breed <player> §7- Breed your pet with another player's",
          "§e/wizpet debug §7- Show stored pet data");

  public WizPetCommand(PetServiceImpl service) {
    this.service = service;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can manage pets.");
      return true;
    }

    PetCommandAction action = parseAction(args);
    if (action == null) {
      return false;
    }

    return switch (action) {
      case HelpAction help -> {
        help.execute(player);
        yield true;
      }
      case SummonAction summon -> {
        summon.execute(player);
        yield true;
      }
      case DismissAction dismiss -> {
        dismiss.execute(player);
        yield true;
      }
      case StatsAction stats -> {
        stats.execute(player);
        yield true;
      }
      case TalentsAction talents -> {
        talents.execute(player);
        yield true;
      }
      case MountAction mount -> {
        mount.execute(player);
        yield true;
      }
      case DismountAction dismount -> {
        dismount.execute(player);
        yield true;
      }
      case FlyAction fly -> {
        fly.execute(player);
        yield true;
      }
      case LandAction land -> {
        land.execute(player);
        yield true;
      }
      case BreedAction breed -> {
        breed.execute(player, args);
        yield true;
      }
      case DebugAction debug -> {
        debug.execute(player);
        yield true;
      }
    };
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      return Seq.of(
              "summon",
              "dismiss",
              "stats",
              "talents",
              "mount",
              "dismount",
              "fly",
              "land",
              "breed",
              "debug")
          .filter(option -> option.regionMatches(true, 0, args[0], 0, args[0].length()))
          .toList();
    }

    if (args.length == 2 && "breed".equalsIgnoreCase(args[0])) {
      Predicate<String> filter = name -> name.regionMatches(true, 0, args[1], 0, args[1].length());
      return Seq.seq(Bukkit.getOnlinePlayers()).map(Player::getName).filter(filter).toList();
    }

    return List.of();
  }

  private PetCommandAction parseAction(String[] args) {
    if (args.length == 0) {
      return new HelpAction();
    }
    return switch (args[0].toLowerCase(Locale.ROOT)) {
      case "summon" -> new SummonAction();
      case "dismiss" -> new DismissAction();
      case "stats" -> new StatsAction();
      case "talents" -> new TalentsAction();
      case "mount" -> new MountAction();
      case "dismount" -> new DismountAction();
      case "fly" -> new FlyAction();
      case "land" -> new LandAction();
      case "breed" -> new BreedAction();
      case "debug" -> new DebugAction();
      default -> null;
    };
  }

  private sealed interface PetCommandAction
      permits HelpAction,
          SummonAction,
          DismissAction,
          StatsAction,
          TalentsAction,
          MountAction,
          DismountAction,
          FlyAction,
          LandAction,
          BreedAction,
          DebugAction {}

  private final class HelpAction implements PetCommandAction {
    void execute(Player player) {
      helpLines.forEach(player::sendMessage);
    }
  }

  private final class SummonAction implements PetCommandAction {
    void execute(Player player) {
      service.summon(player, SummonReason.MANUAL);
      player.sendMessage("§aYour pet answers your call.");
    }
  }

  private final class DismissAction implements PetCommandAction {
    void execute(Player player) {
      if (service.dismiss(player, DismissReason.MANUAL)) {
        player.sendMessage("§cYour pet returns to its dormitory.");
      } else {
        player.sendMessage("§eYou do not have an active pet.");
      }
    }
  }

  private final class StatsAction implements PetCommandAction {
    void execute(Player player) {
      Optional.ofNullable(service.activePet(player))
          .ifPresentOrElse(
              pet -> {
                player.sendMessage("§a" + pet.getRecord().displayName() + " Stats:");
                pet.statBreakdown()
                    .forEach(
                        (type, value) ->
                            player.sendMessage(
                                "§7- §b"
                                    + type.getDisplayName()
                                    + "§7: §f"
                                    + String.format(Locale.US, "%.1f", value)));
              },
              () -> player.sendMessage("§eYou do not have an active pet."));
    }
  }

  private final class TalentsAction implements PetCommandAction {
    void execute(Player player) {
      Optional.ofNullable(service.activePet(player))
          .ifPresentOrElse(
              pet -> {
                player.sendMessage("§d" + pet.getRecord().displayName() + " Talents:");
                for (PetTalent talent : pet.getTalents()) {
                  player.sendMessage(
                      "§7- §d" + talent.getDisplayName() + "§7: " + talent.getDescription());
                }
              },
              () -> player.sendMessage("§eYou do not have an active pet."));
    }
  }

  private final class MountAction implements PetCommandAction {
    void execute(Player player) {
      if (!service.mount(player)) {
        player.sendMessage("§eYou are already mounted or have no pet.");
      } else {
        player.sendMessage("§bYou climb onto your pet.");
      }
    }
  }

  private final class DismountAction implements PetCommandAction {
    void execute(Player player) {
      if (!service.dismount(player)) {
        player.sendMessage("§eYou are not currently riding your pet.");
      } else {
        player.sendMessage("§7You hop off your pet.");
      }
    }
  }

  private final class FlyAction implements PetCommandAction {
    void execute(Player player) {
      if (!service.enableFlight(player)) {
        player.sendMessage("§eYou are already soaring or have no pet.");
      } else {
        player.sendMessage("§3Wings of mana lift you skyward.");
      }
    }
  }

  private final class LandAction implements PetCommandAction {
    void execute(Player player) {
      if (!service.disableFlight(player)) {
        player.sendMessage("§eYou are not currently flying with your pet.");
      } else {
        player.sendMessage("§7You drift back to the ground.");
      }
    }
  }

  private final class BreedAction implements PetCommandAction {
    void execute(Player player, String[] args) {
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
        partner.sendMessage(
            Component.text("§dYour pet assisted " + player.getName() + " in breeding."));
      }
    }
  }

  private final class DebugAction implements PetCommandAction {
    void execute(Player player) {
      service.debugLines(player).forEach(player::sendMessage);
    }
  }
}
