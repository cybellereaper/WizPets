package com.github.cybellereaper.wizpets.core.command;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.SummonReason;
import com.github.cybellereaper.wizpets.api.WizPetsApi;
import com.github.cybellereaper.wizpets.api.command.CommandAction;
import com.github.cybellereaper.wizpets.api.command.CommandContext;
import com.github.cybellereaper.wizpets.api.command.RequiresPermission;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

/** Modernised command dispatcher backed by functional command actions and annotations. */
public final class WizPetCommand implements TabExecutor {
  private static final String PERMISSION_PREFIX = "wizpets.command.";

  private final WizPetsApi api;
  private final Server server;
  private final List<CommandRegistration> registrations;
  private final Map<String, CommandRegistration> registry;

  public WizPetCommand(WizPetsApi api, Server server) {
    this.api = api;
    this.server = server;
    this.registrations = buildRegistrations();
    this.registry = indexRegistrations(registrations);
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can manage pets.");
      return true;
    }

    CommandRegistration registration = lookup(args);
    if (registration == null) {
      player.sendMessage("§cUnknown subcommand. Try /" + label + " help.");
      return true;
    }

    if (!hasPermission(player, registration.action())) {
      player.sendMessage("§cYou do not have permission to use this command.");
      return true;
    }

    CommandContext context = new CommandContext(player, tailArguments(args), api, server);
    return registration.action().execute(context);
  }

  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      String[] args) {
    if (!(sender instanceof Player player)) {
      return List.of();
    }

    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0];
      return Seq.seq(registrations)
          .filter(reg -> hasPermission(player, reg.action()))
          .map(CommandRegistration::name)
          .filter(option -> startsWith(option, prefix))
          .toList();
    }

    CommandRegistration registration = lookup(args);
    if (registration == null || !hasPermission(player, registration.action())) {
      return List.of();
    }

    CommandContext context = new CommandContext(player, tailArguments(args), api, server);
    return registration.action().tabComplete(context);
  }

  private CommandRegistration lookup(String[] args) {
    String key = args.length == 0 ? "help" : CommandContext.lowerKey(args[0]);
    return registry.get(key);
  }

  private static boolean startsWith(String option, String prefix) {
    return option.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  private static List<String> tailArguments(String[] args) {
    if (args.length <= 1) {
      return List.of();
    }
    return List.copyOf(Arrays.asList(args).subList(1, args.length));
  }

  private boolean hasPermission(Player player, CommandAction action) {
    RequiresPermission permission = action.getClass().getAnnotation(RequiresPermission.class);
    return permission == null || player.hasPermission(permission.value());
  }

  private List<CommandRegistration> buildRegistrations() {
    List<CommandRegistration> commands = new ArrayList<>();
    HelpAction help = new HelpAction(commands);
    commands.add(new CommandRegistration("help", "Show command help", help));
    commands.add(
        new CommandRegistration("summon", "Summon or respawn your pet", new SummonAction()));
    commands.add(new CommandRegistration("dismiss", "Dismiss your pet", new DismissAction()));
    commands.add(new CommandRegistration("stats", "View pet statistics", new StatsAction()));
    commands.add(new CommandRegistration("talents", "View pet talents", new TalentsAction()));
    commands.add(new CommandRegistration("mount", "Ride your pet", new MountAction()));
    commands.add(new CommandRegistration("dismount", "Stop riding your pet", new DismountAction()));
    commands.add(new CommandRegistration("fly", "Enable flight with your pet", new FlyAction()));
    commands.add(new CommandRegistration("land", "Land and disable pet flight", new LandAction()));
    commands.add(
        new CommandRegistration(
            "breed", "Breed your pet with another player's", new BreedAction()));
    commands.add(new CommandRegistration("debug", "Show stored pet data", new DebugAction()));
    return ImmutableList.copyOf(commands);
  }

  private Map<String, CommandRegistration> indexRegistrations(List<CommandRegistration> commands) {
    Map<String, CommandRegistration> map = new LinkedHashMap<>();
    for (CommandRegistration registration : commands) {
      map.put(registration.name(), registration);
    }
    map.put("?", commands.getFirst());
    return Map.copyOf(map);
  }

  private record CommandRegistration(String name, String description, CommandAction action) {
    CommandRegistration {
      name = CommandContext.lowerKey(name);
    }
  }

  private interface PetCommandAction extends CommandAction {}

  private final class HelpAction implements PetCommandAction {
    private final List<CommandRegistration> commands;

    HelpAction(List<CommandRegistration> commands) {
      this.commands = commands;
    }

    @Override
    public boolean execute(CommandContext context) {
      context.reply("§7==== §dWizPets Commands §7====");
      Seq.seq(commands)
          .filter(reg -> reg.action() == this || hasPermission(context.player(), reg.action()))
          .map(this::formatLine)
          .forEach(context::reply);
      return true;
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
      return Seq.seq(commands)
          .filter(reg -> reg.action() == this || hasPermission(context.player(), reg.action()))
          .map(CommandRegistration::name)
          .toList();
    }

    private String formatLine(CommandRegistration registration) {
      return "§e/wizpet " + registration.name() + " §7- " + registration.description();
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "summon")
  private final class SummonAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      api.summon(context.player(), SummonReason.MANUAL);
      context.reply("§aYour pet answers your call.");
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "dismiss")
  private final class DismissAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      if (api.dismiss(context.player(), DismissReason.MANUAL)) {
        context.reply("§cYour pet returns to its dormitory.");
      } else {
        context.reply("§eYou do not have an active pet.");
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "stats")
  private final class StatsAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      return Optional.ofNullable(api.activePet(context.player()))
          .map(pet -> displayStats(context, pet))
          .orElseGet(
              () -> {
                context.reply("§eYou do not have an active pet.");
                return true;
              });
    }

    private boolean displayStats(CommandContext context, ActivePet pet) {
      context.reply("§a" + pet.getRecord().displayName() + " Stats:");
      pet.statBreakdown()
          .forEach(
              (type, value) ->
                  context.reply(
                      "§7- §b"
                          + type.getDisplayName()
                          + "§7: §f"
                          + String.format(Locale.US, "%.1f", value)));
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "talents")
  private final class TalentsAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      return Optional.ofNullable(api.activePet(context.player()))
          .map(pet -> displayTalents(context, pet))
          .orElseGet(
              () -> {
                context.reply("§eYou do not have an active pet.");
                return true;
              });
    }

    private boolean displayTalents(CommandContext context, ActivePet pet) {
      context.reply("§d" + pet.getRecord().displayName() + " Talents:");
      for (PetTalent talent : pet.getTalents()) {
        context.reply("§7- §d" + talent.getDisplayName() + "§7: " + talent.getDescription());
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "mount")
  private final class MountAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      if (api.mount(context.player())) {
        context.reply("§bYou climb onto your pet.");
      } else {
        context.reply("§eYou are already mounted or have no pet.");
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "dismount")
  private final class DismountAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      if (api.dismount(context.player())) {
        context.reply("§7You hop off your pet.");
      } else {
        context.reply("§eYou are not currently riding your pet.");
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "fly")
  private final class FlyAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      if (api.enableFlight(context.player())) {
        context.reply("§3Wings of mana lift you skyward.");
      } else {
        context.reply("§eYou are already soaring or have no pet.");
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "land")
  private final class LandAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      if (api.disableFlight(context.player())) {
        context.reply("§7You drift back to the ground.");
      } else {
        context.reply("§eYou are not currently flying with your pet.");
      }
      return true;
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "breed")
  private final class BreedAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      Optional<String> target = context.argument(0);
      if (target.isEmpty()) {
        context.reply("§cUsage: /wizpet breed <player>");
        return true;
      }

      Optional<Player> partner = context.findPlayer(target.get()).filter(Player::isOnline);
      if (partner.isEmpty()) {
        context.reply("§cThe specified partner is not online.");
        return true;
      }

      if (partner.get().getUniqueId().equals(context.player().getUniqueId())) {
        context.reply("§cYou must choose another player to breed with.");
        return true;
      }

      return breed(context, partner.get());
    }

    private boolean breed(CommandContext context, Player partner) {
      api.breed(context.player(), partner);
      context.reply("§dYour pet egg hums with new potential.");
      partner.sendMessage(
          Component.text("§dYour pet assisted " + context.player().getName() + " in breeding."));
      return true;
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
      String prefix = context.argument(0).orElse("");
      return Seq.seq(context.onlinePlayers())
          .map(Player::getName)
          .filter(name -> !name.equalsIgnoreCase(context.player().getName()))
          .filter(name -> startsWith(name, prefix))
          .sorted(String.CASE_INSENSITIVE_ORDER)
          .toList();
    }
  }

  @RequiresPermission(PERMISSION_PREFIX + "debug")
  private final class DebugAction implements PetCommandAction {
    @Override
    public boolean execute(CommandContext context) {
      api.debugLines(context.player()).forEach(context::reply);
      return true;
    }
  }
}
