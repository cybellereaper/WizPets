package com.github.cybellereaper.noblepets.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.cybellereaper.noblepets.api.ActivePet;
import com.github.cybellereaper.noblepets.api.PetRecord;
import com.github.cybellereaper.noblepets.api.StatSet;
import com.github.cybellereaper.noblepets.api.StatType;
import com.github.cybellereaper.noblepets.api.SummonReason;
import com.github.cybellereaper.noblepets.api.NoblePetsApi;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

final class NoblePetCommandTest {
  private NoblePetsApi api;
  private Server server;
  private NoblePetCommand command;
  private Command bukkitCommand;

  @BeforeEach
  void setUp() {
    api = mock(NoblePetsApi.class);
    server = mock(Server.class);
    command = new NoblePetCommand(api, server);
    bukkitCommand = mock(Command.class);
  }

  @Test
  void summonCommandInvokesService() {
    Player player = playerWithPermission("noblepets.command.summon");

    boolean handled = command.onCommand(player, bukkitCommand, "noblepet", new String[] {"summon"});

    assertTrue(handled);
    verify(api).summon(player, SummonReason.MANUAL);
    verify(player).sendMessage("§aYour pet answers your call.");
  }

  @Test
  void permissionDeniedShortCircuitsExecution() {
    Player player = playerWithPermission("noblepets.command.summon", false);

    boolean handled = command.onCommand(player, bukkitCommand, "noblepet", new String[] {"summon"});

    assertTrue(handled);
    verify(api, never()).summon(player, SummonReason.MANUAL);
    verify(player).sendMessage("§cYou do not have permission to use this command.");
  }

  @Test
  void breedCommandResolvesPartner() {
    Player player = playerWithPermission("noblepets.command.breed");
    Player partner = mock(Player.class);
    when(partner.isOnline()).thenReturn(true);
    when(partner.getUniqueId()).thenReturn(UUID.randomUUID());
    when(partner.getName()).thenReturn("Partner");
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(server.getPlayerExact("Partner")).thenReturn(partner);

    boolean handled =
        command.onCommand(player, bukkitCommand, "noblepet", new String[] {"breed", "Partner"});

    assertTrue(handled);
    verify(api).breed(player, partner);
    verify(partner).sendMessage(any(Component.class));
    verify(player).sendMessage("§dYour pet egg hums with new potential.");
  }

  @Test
  void breedCommandRejectsMissingPartner() {
    Player player = playerWithPermission("noblepets.command.breed");

    boolean handled = command.onCommand(player, bukkitCommand, "noblepet", new String[] {"breed"});

    assertTrue(handled);
    verify(api, never()).breed(any(), any());
    verify(player).sendMessage("§cUsage: /noblepet breed <player>");
  }

  @Test
  void editCommandOpensEditor() {
    Player player = playerWithPermission("noblepets.command.edit");

    boolean handled = command.onCommand(player, bukkitCommand, "noblepet", new String[] {"edit"});

    assertTrue(handled);
    verify(api).openEditor(player);
  }

  @Test
  void editRenameDelegatesToApi() {
    Player player = playerWithPermission("noblepets.command.edit");
    when(api.renamePet(player, "Nova Prime")).thenReturn(true);

    boolean handled =
        command.onCommand(
            player,
            bukkitCommand,
            "noblepet",
            new String[] {"edit", "name", "Nova", "Prime"});

    assertTrue(handled);
    verify(api).renamePet(player, "Nova Prime");
  }

  @Test
  void editRerollDelegatesToApi() {
    Player player = playerWithPermission("noblepets.command.edit");
    when(api.rerollTalents(player)).thenReturn(true);

    boolean handled =
        command.onCommand(player, bukkitCommand, "noblepet", new String[] {"edit", "reroll"});

    assertTrue(handled);
    verify(api).rerollTalents(player);
  }

  @Test
  void editTalentValidatesUsage() {
    Player player = playerWithPermission("noblepets.command.edit");

    boolean handled =
        command.onCommand(player, bukkitCommand, "noblepet", new String[] {"edit", "talent"});

    assertTrue(handled);
    verify(player).sendMessage("§cUsage: /noblepet edit talent <slot>");
  }

  @Test
  void tabCompleteHonoursPermissions() {
    Player player = playerWithPermission("noblepets.command.summon");

    List<String> suggestions =
        command.onTabComplete(player, bukkitCommand, "noblepet", new String[] {});

    assertEquals(List.of("help", "summon"), suggestions);
  }

  @Test
  void breedTabCompleteSuggestsPlayers() {
    Player player = playerWithPermission("noblepets.command.breed");
    when(player.getName()).thenReturn("Invoker");
    Player partner = mock(Player.class);
    when(partner.getName()).thenReturn("Partner");
    doReturn(List.of(partner)).when(server).getOnlinePlayers();

    List<String> suggestions =
        command.onTabComplete(player, bukkitCommand, "noblepet", new String[] {"breed", "Pa"});

    assertEquals(List.of("Partner"), suggestions);
  }

  @Test
  void statsCommandPrintsBreakdown() {
    Player player = playerWithPermission("noblepets.command.stats");
    ActivePet pet = mock(ActivePet.class);
    when(pet.getRecord())
        .thenReturn(
            new PetRecord(
                "Nova",
                new StatSet(1, 1, 1, 1),
                new StatSet(1, 1, 1, 1),
                List.of(),
                1,
                0,
                false,
                false));
    when(pet.statBreakdown()).thenReturn(Map.of(StatType.ATTACK, 5.0, StatType.DEFENSE, 3.5));
    when(api.activePet(player)).thenReturn(pet);

    boolean handled = command.onCommand(player, bukkitCommand, "noblepet", new String[] {"stats"});

    assertTrue(handled);
    verify(player).sendMessage("§aNova Stats:");
    verify(player)
        .sendMessage(
            "§7- §b"
                + StatType.ATTACK.getDisplayName()
                + "§7: §f"
                + String.format(Locale.US, "%.1f", 5.0));
  }

  private Player playerWithPermission(String permission) {
    return playerWithPermission(permission, true);
  }

  private Player playerWithPermission(String permission, boolean granted) {
    Player player = mock(Player.class);
    when(player.hasPermission(anyString()))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> permission.equals(invocation.getArgument(0)) && granted);
    when(player.getName()).thenReturn("Player");
    return player;
  }
}
