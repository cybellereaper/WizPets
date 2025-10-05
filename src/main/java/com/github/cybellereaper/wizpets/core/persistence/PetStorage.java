package com.github.cybellereaper.wizpets.core.persistence;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PetStorage {
    private final NamespacedKey rootKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey generationKey;
    private final NamespacedKey breedKey;
    private final NamespacedKey talentsKey;
    private final NamespacedKey mountKey;
    private final NamespacedKey flightKey;
    private final NamespacedKey evHealthKey;
    private final NamespacedKey evAttackKey;
    private final NamespacedKey evDefenseKey;
    private final NamespacedKey evMagicKey;
    private final NamespacedKey ivHealthKey;
    private final NamespacedKey ivAttackKey;
    private final NamespacedKey ivDefenseKey;
    private final NamespacedKey ivMagicKey;

    public PetStorage(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        rootKey = new NamespacedKey(plugin, "pet");
        nameKey = new NamespacedKey(plugin, "name");
        generationKey = new NamespacedKey(plugin, "generation");
        breedKey = new NamespacedKey(plugin, "breed_count");
        talentsKey = new NamespacedKey(plugin, "talents");
        mountKey = new NamespacedKey(plugin, "mount_unlocked");
        flightKey = new NamespacedKey(plugin, "flight_unlocked");
        evHealthKey = new NamespacedKey(plugin, "ev_health");
        evAttackKey = new NamespacedKey(plugin, "ev_attack");
        evDefenseKey = new NamespacedKey(plugin, "ev_defense");
        evMagicKey = new NamespacedKey(plugin, "ev_magic");
        ivHealthKey = new NamespacedKey(plugin, "iv_health");
        ivAttackKey = new NamespacedKey(plugin, "iv_attack");
        ivDefenseKey = new NamespacedKey(plugin, "iv_defense");
        ivMagicKey = new NamespacedKey(plugin, "iv_magic");
    }

    public PetRecord load(Player player) {
        PersistentDataContainer parent = player.getPersistentDataContainer();
        PersistentDataContainer container = parent.get(rootKey, PersistentDataType.TAG_CONTAINER);
        if (container == null) {
            return null;
        }

        String name = container.get(nameKey, PersistentDataType.STRING);
        if (name == null || name.isEmpty()) {
            return null;
        }
        Integer generation = container.get(generationKey, PersistentDataType.INTEGER);
        Integer breedCount = container.get(breedKey, PersistentDataType.INTEGER);
        String talentRaw = container.get(talentsKey, PersistentDataType.STRING);
        List<String> talents;
        if (talentRaw == null || talentRaw.isEmpty()) {
            talents = Collections.emptyList();
        } else {
            talents = new ArrayList<>(Arrays.stream(talentRaw.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        }
        Byte mountUnlockedRaw = container.get(mountKey, PersistentDataType.BYTE);
        Byte flightUnlockedRaw = container.get(flightKey, PersistentDataType.BYTE);

        StatSet evs = new StatSet(
            getDouble(container, evHealthKey),
            getDouble(container, evAttackKey),
            getDouble(container, evDefenseKey),
            getDouble(container, evMagicKey)
        );

        StatSet ivs = new StatSet(
            getDouble(container, ivHealthKey),
            getDouble(container, ivAttackKey),
            getDouble(container, ivDefenseKey),
            getDouble(container, ivMagicKey)
        );

        return new PetRecord(
            name,
            evs,
            ivs,
            talents,
            generation != null ? generation : 1,
            breedCount != null ? breedCount : 0,
            mountUnlockedRaw != null && mountUnlockedRaw == (byte) 1,
            flightUnlockedRaw != null && flightUnlockedRaw == (byte) 1
        );
    }

    public void save(Player player, PetRecord record) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(record, "record");

        PersistentDataContainer parent = player.getPersistentDataContainer();
        PersistentDataContainer container = parent.getAdapterContext().newPersistentDataContainer();

        container.set(nameKey, PersistentDataType.STRING, record.getDisplayName());
        container.set(generationKey, PersistentDataType.INTEGER, record.getGeneration());
        container.set(breedKey, PersistentDataType.INTEGER, record.getBreedCount());
        container.set(talentsKey, PersistentDataType.STRING, String.join(";", record.getTalentIds()));
        container.set(mountKey, PersistentDataType.BYTE, asByte(record.isMountUnlocked()));
        container.set(flightKey, PersistentDataType.BYTE, asByte(record.isFlightUnlocked()));

        container.set(evHealthKey, PersistentDataType.DOUBLE, record.getEvs().getHealth());
        container.set(evAttackKey, PersistentDataType.DOUBLE, record.getEvs().getAttack());
        container.set(evDefenseKey, PersistentDataType.DOUBLE, record.getEvs().getDefense());
        container.set(evMagicKey, PersistentDataType.DOUBLE, record.getEvs().getMagic());

        container.set(ivHealthKey, PersistentDataType.DOUBLE, record.getIvs().getHealth());
        container.set(ivAttackKey, PersistentDataType.DOUBLE, record.getIvs().getAttack());
        container.set(ivDefenseKey, PersistentDataType.DOUBLE, record.getIvs().getDefense());
        container.set(ivMagicKey, PersistentDataType.DOUBLE, record.getIvs().getMagic());

        parent.set(rootKey, PersistentDataType.TAG_CONTAINER, container);
    }

    public void clear(Player player) {
        player.getPersistentDataContainer().remove(rootKey);
    }

    private static double getDouble(PersistentDataContainer container, NamespacedKey key) {
        Double value = container.get(key, PersistentDataType.DOUBLE);
        return value != null ? value : 0.0;
    }

    private static byte asByte(boolean value) {
        return (byte) (value ? 1 : 0);
    }
}
