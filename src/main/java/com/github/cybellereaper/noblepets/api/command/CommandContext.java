package com.github.cybellereaper.noblepets.api.command;

import com.github.cybellereaper.noblepets.api.NoblePetsApi;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Shared context provided to each command action including the executing player, raw arguments, and
 * supporting services.
 */
public record CommandContext(Player player, List<String> arguments, NoblePetsApi api, Server server) {

  public CommandContext {
    arguments = List.copyOf(arguments);
  }

  /** Returns the argument at the given index or {@link Optional#empty()} if missing. */
  public Optional<String> argument(int index) {
    return index >= 0 && index < arguments.size()
        ? Optional.of(arguments.get(index))
        : Optional.empty();
  }

  /** Emits a stream of the currently online players. */
  public Stream<Player> onlinePlayers() {
    Collection<? extends Player> players = server.getOnlinePlayers();
    return players.stream().map(Player.class::cast);
  }

  /** Attempts to resolve an online player by their exact name. */
  public Optional<Player> findPlayer(String name) {
    return Optional.ofNullable(server.getPlayerExact(name));
  }

  /** Sends a legacy-formatted message response to the invoking player. */
  public void reply(String message) {
    player.sendMessage(message);
  }

  /** Sends an Adventure component response to the invoking player. */
  public void reply(Component component) {
    player.sendMessage(component);
  }

  /** Checks whether the invoking player holds the provided permission node. */
  public boolean hasPermission(String permission) {
    return player.hasPermission(permission);
  }

  /** Normalises a string to lower-case for key comparisons. */
  public static String lowerKey(String raw) {
    return raw.toLowerCase(Locale.ROOT);
  }
}
