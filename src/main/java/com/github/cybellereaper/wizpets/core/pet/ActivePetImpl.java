package com.github.cybellereaper.wizpets.core.pet;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchModelInstance;
import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.core.model.blockbench.ArmorStandPoseTarget;
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class ActivePetImpl implements ActivePet {
  private static final double ANIMATION_TICK_SECONDS = 0.5D;
  private static final double FOLLOW_RADIUS = 1.6D;
  private static final double FOLLOW_HEIGHT_GROUND = 0.9D;
  private static final double FOLLOW_HEIGHT_AIR = 1.5D;
  private static final double FOLLOW_SMOOTHING = 0.45D;
  private static final double ORBIT_INCREMENT = Math.toRadians(12);

  private final PetServiceImpl service;
  private final Player owner;
  private final StatSet baseStats = new StatSet(40.0, 6.0, 3.0, 5.0);
  private PetRecord currentRecord;
  private List<PetTalent> currentTalents;
  private ArmorStand armorStand;
  private BlockbenchModelInstance modelInstance;
  private BukkitTask tickTask;
  private int attackCooldown;
  private boolean mounted;
  private boolean flying;
  private double orbitAngle;

  public ActivePetImpl(
      PetServiceImpl service,
      Player owner,
      PetRecord initialRecord,
      List<PetTalent> initialTalents) {
    this.service = Objects.requireNonNull(service, "service");
    this.owner = Objects.requireNonNull(owner, "owner");
    this.currentRecord = Objects.requireNonNull(initialRecord, "initialRecord");
    this.currentTalents = List.copyOf(initialTalents);
  }

  @Override
  public Player getOwner() {
    return owner;
  }

  @Override
  public PetRecord getRecord() {
    return currentRecord;
  }

  @Override
  public List<PetTalent> getTalents() {
    return currentTalents;
  }

  @Override
  public boolean isMounted() {
    return mounted;
  }

  @Override
  public boolean isFlying() {
    return flying;
  }

  public void spawn() {
    spawn(owner.getLocation().add(0.5, 0.0, 0.5));
  }

  public void spawn(Location location) {
    Location spawnLocation = location.clone();
    World world = spawnLocation.getWorld();
    if (world == null) {
      return;
    }
    ArmorStand stand = (ArmorStand) world.spawnEntity(spawnLocation, EntityType.ARMOR_STAND);
    stand.setGravity(false);
    stand.setVisible(false);
    stand.setMarker(false);
    stand.setSmall(true);
    stand.getEquipment();
    stand.getEquipment().setHelmet(new ItemStack(Material.END_ROD));
    stand.customName(Component.text(currentRecord.displayName()));
    stand.setCustomNameVisible(true);
    this.armorStand = stand;

    initializeModel(stand);

    tickTask = Bukkit.getScheduler().runTaskTimer(service.getPlugin(), this::tick, 10L, 10L);
    currentTalents.forEach(talent -> talent.onSummon(this));
    if (currentRecord.flightUnlocked()) {
      owner.setAllowFlight(true);
    }
  }

  public void update(PetRecord record, List<PetTalent> talents) {
    this.currentRecord = Objects.requireNonNull(record, "record");
    this.currentTalents = List.copyOf(talents);
    if (armorStand != null) {
      armorStand.customName(Component.text(record.displayName()));
      armorStand.setCustomNameVisible(true);
    }
    if (modelInstance != null) {
      ensureIdleAnimation();
    }
  }

  public void dismountIfNecessary() {
    Entity vehicle = owner.getVehicle();
    if (vehicle != null && vehicle.equals(armorStand)) {
      owner.leaveVehicle();
    }
    mounted = false;
  }

  public void remove(boolean persistFlight) {
    if (tickTask != null) {
      tickTask.cancel();
      tickTask = null;
    }
    currentTalents.forEach(talent -> talent.onDismiss(this));
    dismountIfNecessary();
    if (!persistFlight
        && owner.getGameMode() != GameMode.CREATIVE
        && owner.getGameMode() != GameMode.SPECTATOR) {
      owner.setAllowFlight(currentRecord.flightUnlocked());
      if (!currentRecord.flightUnlocked()) {
        owner.setFlying(false);
      }
    }
    if (armorStand != null) {
      if (modelInstance != null) {
        modelInstance.destroy();
        modelInstance = null;
      }
      armorStand.remove();
      armorStand = null;
    }
  }

  public boolean mount() {
    ArmorStand stand = armorStand;
    if (stand == null) {
      return false;
    }
    if (!currentRecord.mountUnlocked()) {
      currentRecord = currentRecord.withMountUnlocked(true);
      owner.sendMessage(service.getConfigValues().getMountUnlockMessage());
    }
    if (owner.getVehicle() == stand) {
      return false;
    }
    boolean added = stand.addPassenger(owner);
    if (added) {
      mounted = true;
    }
    return added;
  }

  public boolean dismount() {
    if (owner.getVehicle() != armorStand) {
      return false;
    }
    owner.leaveVehicle();
    mounted = false;
    return true;
  }

  public boolean startFlying() {
    if (!currentRecord.flightUnlocked()) {
      currentRecord = currentRecord.withFlightUnlocked(true);
      owner.sendMessage(service.getConfigValues().getFlightUnlockMessage());
    }
    if (owner.getAllowFlight() && owner.isFlying()) {
      flying = true;
      return false;
    }
    owner.setAllowFlight(true);
    owner.setFlying(true);
    flying = true;
    return true;
  }

  public boolean stopFlying() {
    if (!flying) {
      return false;
    }
    flying = false;
    if (owner.getGameMode() != GameMode.CREATIVE
        && owner.getGameMode() != GameMode.SPECTATOR
        && !currentRecord.flightUnlocked()) {
      owner.setFlying(false);
      owner.setAllowFlight(false);
    }
    return true;
  }

  @Override
  public void heal(double amount) {
    double maxHealth = owner.getMaxHealth();
    owner.setHealth(Math.min(owner.getHealth() + amount, maxHealth));
  }

  @Override
  public void grantAbsorption(double hearts) {
    owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(), hearts));
  }

  @Override
  public double statValue(StatType type) {
    double base = baseStats.value(type);
    double iv = currentRecord.ivs().value(type);
    double evContribution = currentRecord.evs().value(type) / 4.0;
    double result = base + iv + evContribution;
    for (PetTalent talent : currentTalents) {
      result = talent.modifyStat(this, type, result);
    }
    return result;
  }

  public PetRecord toRecord() {
    return currentRecord;
  }

  private void tick() {
    ArmorStand stand = armorStand;
    if (stand == null) {
      return;
    }
    if (!owner.isOnline() || owner.isDead()) {
      service.dismiss(owner, true);
      return;
    }

    Location targetLocation = mounted ? owner.getLocation() : computeFollowLocation(stand);
    stand.teleport(targetLocation);
    currentTalents.forEach(talent -> talent.tick(this));

    BlockbenchModelInstance animator = modelInstance;
    if (animator != null) {
      animator.tick(ANIMATION_TICK_SECONDS);
    }

    handleHealing();
    handleCombat(stand);
    if (flying) {
      owner.setFallDistance(0f);
    }
  }

  private Location computeFollowLocation(ArmorStand stand) {
    Location ownerLocation = owner.getLocation();
    Location desired = ownerLocation.clone();
    orbitAngle = (orbitAngle + ORBIT_INCREMENT) % (Math.PI * 2);
    double height = (owner.isFlying() || flying) ? FOLLOW_HEIGHT_AIR : FOLLOW_HEIGHT_GROUND;
    desired.add(Math.cos(orbitAngle) * FOLLOW_RADIUS, height, Math.sin(orbitAngle) * FOLLOW_RADIUS);

    Location current = stand.getLocation();
    if (current.getWorld() == null || !current.getWorld().equals(desired.getWorld())) {
      desired.setYaw(ownerLocation.getYaw());
      desired.setPitch(0f);
      return desired;
    }
    Vector delta = desired.toVector().subtract(current.toVector());
    if (delta.lengthSquared() > 9.0) {
      // Prevent wild teleports if the owner moved far away.
      current = desired.clone();
    } else {
      Vector step = delta.multiply(FOLLOW_SMOOTHING);
      current.add(step);
    }
    current.setYaw(ownerLocation.getYaw());
    current.setPitch(0f);
    return current;
  }

  private void handleHealing() {
    double maxHealth = owner.getMaxHealth();
    if (owner.getHealth() < maxHealth) {
      double healAmount = Math.max(0.5, statValue(StatType.MAGIC) * 0.05);
      heal(healAmount);
    }
  }

  private void handleCombat(ArmorStand stand) {
    if (attackCooldown > 0) {
      attackCooldown--;
      return;
    }
    World world = stand.getWorld();
    double closestDistance = Double.MAX_VALUE;
    Monster target = null;
    for (Entity entity : world.getNearbyEntities(stand.getLocation(), 8.0, 6.0, 8.0)) {
      if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
        double distance = monster.getLocation().distanceSquared(stand.getLocation());
        if (distance < closestDistance) {
          closestDistance = distance;
          target = monster;
        }
      }
    }
    if (target != null) {
      double finalDamage = statValue(StatType.ATTACK) * 0.75 + statValue(StatType.MAGIC) * 0.25;
      dealDamage(target, finalDamage);
      Monster finalTarget = target;
      currentTalents.forEach(talent -> talent.onAttack(this, finalTarget, finalDamage));
      playOnceIfSupported("attack");
      attackCooldown = Math.max(2, (int) (6 - statValue(StatType.MAGIC) / 4));
    }
  }

  private void dealDamage(LivingEntity target, double amount) {
    target.damage(amount, owner);
    owner.sendActionBar(
        Component.text(
            "§d"
                + currentRecord.displayName()
                + " hits "
                + target.getName()
                + " for "
                + String.format(Locale.US, "%.1f", amount)));
  }

  private void initializeModel(ArmorStand stand) {
    String modelId = service.getConfigValues().getDefaultModelId();
    if (!service.blockbench().hasModel(modelId)) {
      service
          .getPlugin()
          .getLogger()
          .warning(() -> "No Blockbench model registered with id '" + modelId + "'");
      return;
    }
    BlockbenchModelInstance instance =
        service.blockbench().createInstance(modelId, new ArmorStandPoseTarget(stand));
    this.modelInstance = instance;
    ensureIdleAnimation();
  }

  private void ensureIdleAnimation() {
    if (modelInstance == null) {
      return;
    }
    if (service.blockbench().animations(modelInstance.modelId()).contains("idle")) {
      modelInstance.playLoop("idle");
      modelInstance.tick(0.0);
    }
  }

  private void playOnceIfSupported(String animation) {
    BlockbenchModelInstance animator = modelInstance;
    if (animator == null) {
      return;
    }
    if (service.blockbench().animations(animator.modelId()).contains(animation)) {
      animator.playOnce(animation);
    }
  }
}
