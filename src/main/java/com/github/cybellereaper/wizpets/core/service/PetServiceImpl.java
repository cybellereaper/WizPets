package com.github.cybellereaper.wizpets.core.service;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.api.SummonReason;
import com.github.cybellereaper.wizpets.api.WizPetsApi;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener;
import com.github.cybellereaper.wizpets.core.config.PluginConfig;
import com.github.cybellereaper.wizpets.core.persistence.PetStorage;
import com.github.cybellereaper.wizpets.core.pet.ActivePetImpl;
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl;
import com.github.cybellereaper.wizpets.core.talent.defaults.ArcaneBurstTalent;
import com.github.cybellereaper.wizpets.core.talent.defaults.GuardianShellTalent;
import com.github.cybellereaper.wizpets.core.talent.defaults.HealingAuraTalent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PetServiceImpl implements WizPetsApi, Listener {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final PetStorage storage;
    private final TalentRegistryImpl registry;
    private final Set<PetLifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final Map<UUID, ActivePetImpl> activePets = new ConcurrentHashMap<>();
    private final Random random = new Random(System.currentTimeMillis());

    public PetServiceImpl(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = new PluginConfig(plugin.getConfig());
        this.storage = new PetStorage(plugin);
        this.registry = new TalentRegistryImpl();
        registerDefaults();
        plugin.getServer().getServicesManager().register(WizPetsApi.class, this, plugin, ServicePriority.Normal);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public PluginConfig getConfigValues() {
        return config;
    }

    @Override
    public ActivePet activePet(Player player) {
        return activePets.get(player.getUniqueId());
    }

    @Override
    public PetRecord storedPet(Player player) {
        return storage.load(player);
    }

    @Override
    public ActivePet summon(Player player, SummonReason reason) {
        ActivePetImpl existing = activePets.remove(player.getUniqueId());
        if (existing != null) {
            storage.save(player, existing.toRecord());
            existing.remove(true);
        }
        PetRecord loaded = storage.load(player);
        if (loaded == null) {
            loaded = createNewRecord(player);
        }
        ResolvedTalents resolved = resolveTalents(loaded);
        ActivePetImpl pet = new ActivePetImpl(this, player, resolved.record(), resolved.talents());
        pet.spawn();
        activePets.put(player.getUniqueId(), pet);
        storage.save(player, pet.toRecord());
        listeners.forEach(listener -> listener.onSummoned(player, pet, reason));
        plugin.getLogger().fine(() -> "Summoned pet for " + player.getName() + " via " + reason);
        return pet;
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
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (config.isAutoSummon()) {
                summon(player, SummonReason.RESTORE);
            }
        });
    }

    public void shutdown() {
        List<ActivePetImpl> snapshot = new ArrayList<>(activePets.values());
        snapshot.forEach(pet -> {
            Player owner = pet.getOwner();
            storage.save(owner, pet.toRecord());
            pet.remove(false);
            listeners.forEach(listener -> listener.onDismissed(owner, pet.toRecord(), DismissReason.PLUGIN_DISABLE));
        });
        activePets.clear();
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(WizPetsApi.class, this);
    }

    public void breed(Player player, Player partner) {
        PetRecord playerRecord = storage.load(player);
        if (playerRecord == null) {
            playerRecord = createNewRecord(player);
        }
        PetRecord partnerRecord = storage.load(partner);
        if (partnerRecord == null) {
            return;
        }

        int generation = Math.max(playerRecord.getGeneration(), partnerRecord.getGeneration()) + 1;
        PetRecord childRecord = new PetRecord(
            MessageFormat.format("{0}'s Hatchling", player.getName()),
            playerRecord.getEvs().breedWith(partnerRecord.getEvs(), random),
            playerRecord.getIvs().breedWith(partnerRecord.getIvs(), random),
            registry.inherit(playerRecord.getTalentIds(), partnerRecord.getTalentIds(), random),
            generation,
            playerRecord.getBreedCount() + 1,
            playerRecord.isMountUnlocked() || partnerRecord.isMountUnlocked(),
            playerRecord.isFlightUnlocked() || partnerRecord.isFlightUnlocked()
        );

        storage.save(player, childRecord);
        listeners.forEach(listener -> listener.onPersist(player, childRecord));
        summon(player, SummonReason.BREEDING_REFRESH);

        PetRecord updatedPartner = partnerRecord.withBreedCount(partnerRecord.getBreedCount() + 1);
        storage.save(partner, updatedPartner);
        listeners.forEach(listener -> listener.onPersist(partner, updatedPartner));
        ActivePetImpl partnerPet = activePets.get(partner.getUniqueId());
        if (partnerPet != null) {
            ResolvedTalents resolved = resolveTalents(updatedPartner);
            storage.save(partner, resolved.record());
            partnerPet.update(resolved.record(), resolved.talents());
        }
    }

    public List<String> debugLines(Player player) {
        PetRecord record = storage.load(player);
        if (record == null) {
            return List.of("§cNo stored pet data found.");
        }
        String stats = buildStatsSummary(record);
        return List.of(
            "§7==== §dPet Debug §7====",
            "§7Name: §f" + record.getDisplayName(),
            "§7Generation: §f" + record.getGeneration() + "  §7Breeds: §f" + record.getBreedCount(),
            "§7Talents: §f" + (record.getTalentIds().isEmpty() ? "None" : String.join(", ", record.getTalentIds())),
            "§7Mount unlocked: §f" + record.isMountUnlocked(),
            "§7Flight unlocked: §f" + record.isFlightUnlocked(),
            "§7Base Stats: §f" + stats
        );
    }

    private String buildStatsSummary(PetRecord record) {
        StringBuilder builder = new StringBuilder();
        StatType[] types = StatType.values();
        for (int i = 0; i < types.length; i++) {
            StatType type = types[i];
            double value = record.getEvs().value(type) / 4.0 + record.getIvs().value(type);
            builder.append(type.getDisplayName())
                .append(':')
                .append(' ')
                .append(String.format(Locale.US, "%.2f", value));
            if (i < types.length - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private PetRecord createNewRecord(Player player) {
        PetRecord record = new PetRecord(
            config.getDefaultPetName().replace("{player}", player.getName()),
            StatSet.randomEV(random),
            StatSet.randomIV(random),
            registry.roll(random),
            1,
            0,
            false,
            false
        );
        storage.save(player, record);
        return record;
    }

    private void refreshActivePets() {
        activePets.values().forEach(pet -> {
            Player owner = pet.getOwner();
            PetRecord record = storage.load(owner);
            if (record == null) {
                record = pet.toRecord();
            }
            ResolvedTalents resolved = resolveTalents(record);
            storage.save(owner, resolved.record());
            pet.update(resolved.record(), resolved.talents());
        });
    }

    private void registerDefaults() {
        registry.register(HealingAuraTalent::new);
        registry.register(GuardianShellTalent::new);
        registry.register(ArcaneBurstTalent::new);
    }

    private ResolvedTalents resolveTalents(PetRecord record) {
        PetRecord currentRecord = record;
        List<PetTalent> talents = registry.instantiate(currentRecord.getTalentIds());
        if (talents.size() != currentRecord.getTalentIds().size() || talents.isEmpty()) {
            List<String> ids = registry.roll(random);
            currentRecord = currentRecord.withTalentIds(ids);
            talents = registry.instantiate(ids);
        }
        return new ResolvedTalents(currentRecord, talents);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (config.isAutoSummon()) {
            Bukkit.getScheduler().runTask(plugin, () -> summon(event.getPlayer(), SummonReason.AUTO_SUMMON));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dismiss(event.getPlayer(), DismissReason.PLAYER_QUIT, false);
    }

    private record ResolvedTalents(PetRecord record, List<PetTalent> talents) {
    }
}
