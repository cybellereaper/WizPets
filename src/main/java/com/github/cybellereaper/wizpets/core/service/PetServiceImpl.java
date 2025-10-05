package com.github.cybellereaper.wizpets.core.service;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.api.SummonReason;
import com.github.cybellereaper.wizpets.api.WizPetsApi;
import com.github.cybellereaper.wizpets.api.persistence.PetPersistence;
import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener;
import com.github.cybellereaper.wizpets.core.config.PluginConfig;
import com.github.cybellereaper.wizpets.core.pet.ActivePetImpl;
import com.github.cybellereaper.wizpets.core.service.BreedingEngine.BreedOutcome;
import com.github.cybellereaper.wizpets.core.service.PetTalentResolver.ResolvedTalents;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import com.github.cybellereaper.wizpets.core.talent.defaults.ArcaneBurstTalent;
import com.github.cybellereaper.wizpets.core.talent.defaults.GuardianShellTalent;
import com.github.cybellereaper.wizpets.core.talent.defaults.HealingAuraTalent;
import io.vavr.control.Option;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator.SplittableGenerator;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.lambda.Seq;

@Singleton
public final class PetServiceImpl implements WizPetsApi, Listener, AutoCloseable {
  private final JavaPlugin plugin;
  private final PluginConfig config;
  private final PetPersistence storage;
  private final TalentRegistryImpl registry;
  private final PetTalentResolver talentResolver;
  private final BreedingEngine breedingEngine;
  private final Set<PetLifecycleListener> listeners = new CopyOnWriteArraySet<>();
  private final Map<UUID, ActivePetImpl> activePets = new ConcurrentHashMap<>();
  private final SplittableGenerator random;
  private final ExecutorService executor;

  @Inject
  public PetServiceImpl(
      @NonNull JavaPlugin plugin,
      @NonNull PluginConfig config,
      @NonNull PetPersistence storage,
      @NonNull TalentRegistryImpl registry,
      @NonNull PetTalentResolver talentResolver,
      @NonNull BreedingEngine breedingEngine,
      @NonNull SplittableGenerator random,
      @NonNull ExecutorService executor) {
    this.plugin = plugin;
    this.config = config;
    this.storage = storage;
    this.registry = registry;
    this.talentResolver = talentResolver;
    this.breedingEngine = breedingEngine;
    this.random = random;
    this.executor = executor;
    registerDefaults();
    plugin
        .getServer()
        .getServicesManager()
        .register(WizPetsApi.class, this, plugin, ServicePriority.Normal);
  }

  public JavaPlugin getPlugin() {
    return plugin;
  }

  public PluginConfig getConfigValues() {
    return config;
  }

  @Override
  public PetPersistence persistence() {
    return storage;
  }

  @Override
  public ActivePet activePet(Player player) {
    return activePets.get(player.getUniqueId());
  }

  @Override
  public PetRecord storedPet(Player player) {
    return storage.load(player).orElse(null);
  }

  @Override
  public void summon(Player player, SummonReason reason) {
    Option.of(activePets.remove(player.getUniqueId()))
        .peek(pet -> storage.save(player, pet.toRecord()))
        .peek(pet -> pet.remove(true));

    PetRecord baseline =
        Option.ofOptional(storage.load(player)).getOrElse(() -> createNewRecord(player));
    ResolvedTalents resolved = talentResolver.resolve(baseline);
    ActivePetImpl pet = new ActivePetImpl(this, player, resolved.record(), resolved.talents());
    pet.spawn();
    activePets.put(player.getUniqueId(), pet);
    storage.save(player, pet.toRecord());
    listeners.forEach(listener -> listener.onSummoned(player, pet, reason));
    plugin.getLogger().fine(() -> "Summoned pet for " + player.getName() + " via " + reason);
  }

  @Override
  public boolean dismiss(Player player, DismissReason reason) {
    return dismiss(player, reason, reason == DismissReason.PLUGIN_DISABLE);
  }

  public void dismiss(Player player, boolean internal) {
    dismiss(player, internal ? DismissReason.PLAYER_QUIT : DismissReason.MANUAL, internal);
  }

  private boolean dismiss(Player player, DismissReason reason, boolean persistFlight) {
    ActivePetImpl pet = activePets.remove(player.getUniqueId());
    if (pet == null) {
      return false;
    }
    PetRecord record = pet.toRecord();
    storage.save(player, record);
    listeners.forEach(listener -> listener.onDismissed(player, record, reason));
    pet.remove(persistFlight);
    plugin.getLogger().fine(() -> "Dismissed pet for " + player.getName() + " via " + reason);
    return true;
  }

  @Override
  public void persist(Player player) {
    ActivePetImpl pet = activePets.get(player.getUniqueId());
    if (pet == null) {
      return;
    }
    PetRecord record = pet.toRecord();
    storage.save(player, record);
    listeners.forEach(listener -> listener.onPersist(player, record));
  }

  @Override
  public TalentRegistryView talents() {
    return registry;
  }

  @Override
  public void registerTalent(TalentFactory factory, boolean replace) {
    registry.register(factory, replace);
    refreshActivePets();
  }

  @Override
  public void unregisterTalent(String id) {
    registry.unregister(id);
    refreshActivePets();
  }

  @Override
  public void addListener(PetLifecycleListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(PetLifecycleListener listener) {
    listeners.remove(listener);
  }

  @Override
  public boolean mount(Player player) {
    ActivePetImpl pet = activePets.get(player.getUniqueId());
    if (pet == null) {
      return false;
    }
    boolean mounted = pet.mount();
    if (mounted) {
      persist(player);
    }
    return mounted;
  }

  @Override
  public boolean dismount(Player player) {
    ActivePetImpl pet = activePets.get(player.getUniqueId());
    if (pet == null) {
      return false;
    }
    boolean result = pet.dismount();
    if (result) {
      persist(player);
    }
    return result;
  }

  @Override
  public boolean enableFlight(Player player) {
    ActivePetImpl pet = activePets.get(player.getUniqueId());
    if (pet == null) {
      return false;
    }
    boolean started = pet.startFlying();
    if (started) {
      persist(player);
    }
    return started;
  }

  @Override
  public boolean disableFlight(Player player) {
    ActivePetImpl pet = activePets.get(player.getUniqueId());
    if (pet == null) {
      return false;
    }
    boolean stopped = pet.stopFlying();
    if (stopped) {
      persist(player);
    }
    return stopped;
  }

  public void restoreOnlinePlayers() {
    Seq.seq(Bukkit.getOnlinePlayers())
        .filter(player -> config.isAutoSummon())
        .forEach(player -> summon(player, SummonReason.RESTORE));
  }

  public void shutdown() {
    List<ActivePetImpl> snapshot = new ArrayList<>(activePets.values());
    snapshot.forEach(
        pet -> {
          Player owner = pet.getOwner();
          PetRecord record = pet.toRecord();
          storage.save(owner, record);
          pet.remove(false);
          listeners.forEach(
              listener -> listener.onDismissed(owner, record, DismissReason.PLUGIN_DISABLE));
        });
    activePets.clear();
  }

  public void unregister() {
    plugin.getServer().getServicesManager().unregister(WizPetsApi.class, this);
  }

  @Override
  public void breed(Player player, Player partner) {
    PetRecord playerRecord =
        Option.ofOptional(storage.load(player)).getOrElse(() -> createNewRecord(player));
    PetRecord partnerRecord = storage.load(partner).orElse(null);
    if (partnerRecord == null) {
      return;
    }

    BreedOutcome outcome = breedingEngine.breed(player, playerRecord, partnerRecord);

    storage.save(player, outcome.childRecord());
    listeners.forEach(listener -> listener.onPersist(player, outcome.childRecord()));
    summon(player, SummonReason.BREEDING_REFRESH);

    storage.save(partner, outcome.updatedPartnerRecord());
    listeners.forEach(listener -> listener.onPersist(partner, outcome.updatedPartnerRecord()));
    Option.of(activePets.get(partner.getUniqueId()))
        .peek(
            pet -> {
              PetRecord partnerBaseline =
                  storage.load(partner).orElse(outcome.updatedPartnerRecord());
              ResolvedTalents resolved = talentResolver.resolve(partnerBaseline);
              storage.save(partner, resolved.record());
              pet.update(resolved.record(), resolved.talents());
            });
  }

  @Override
  public List<String> debugLines(Player player) {
    return storage
        .load(player)
        .map(
            record -> {
              String stats = buildStatsSummary(record);
              return List.of(
                  "§7==== §dPet Debug §7====",
                  "§7Name: §f" + record.displayName(),
                  "§7Generation: §f" + record.generation() + "  §7Breeds: §f" + record.breedCount(),
                  "§7Talents: §f"
                      + (record.talentIds().isEmpty()
                          ? "None"
                          : String.join(", ", record.talentIds())),
                  "§7Mount unlocked: §f" + record.mountUnlocked(),
                  "§7Flight unlocked: §f" + record.flightUnlocked(),
                  "§7Base Stats: §f" + stats);
            })
        .orElseGet(() -> List.of("§cNo stored pet data found."));
  }

  private String buildStatsSummary(PetRecord record) {
    return Seq.of(StatType.values())
        .map(
            type ->
                type.getDisplayName()
                    + ": "
                    + String.format(
                        Locale.US,
                        "%.2f",
                        record.evs().value(type) / 4.0 + record.ivs().value(type)))
        .toString(", ");
  }

  private PetRecord createNewRecord(Player player) {
    SplittableGenerator branch = random.split();
    PetRecord record =
        new PetRecord(
            config.getDefaultPetName().replace("{player}", player.getName()),
            StatSet.randomEV(branch.split()),
            StatSet.randomIV(branch.split()),
            registry.roll(branch),
            1,
            0,
            false,
            false);
    storage.save(player, record);
    return record;
  }

  private void refreshActivePets() {
    activePets
        .values()
        .forEach(
            pet -> {
              Player owner = pet.getOwner();
              PetRecord baseline = storage.load(owner).orElse(pet.toRecord());
              ResolvedTalents resolved = talentResolver.resolve(baseline);
              storage.save(owner, resolved.record());
              pet.update(resolved.record(), resolved.talents());
            });
  }

  private void registerDefaults() {
    registry.register(HealingAuraTalent::new);
    registry.register(GuardianShellTalent::new);
    registry.register(ArcaneBurstTalent::new);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (config.isAutoSummon()) {
      Bukkit.getScheduler()
          .runTask(plugin, () -> summon(event.getPlayer(), SummonReason.AUTO_SUMMON));
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    dismiss(event.getPlayer(), DismissReason.PLAYER_QUIT, false);
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }
}
