package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class PetManager(private val plugin: WizPets) : Listener {
    private val random = Random(System.currentTimeMillis())
    private val profileStore = ProfileStore(plugin)
    private val profiles = profileStore.loadProfiles()
    private val activePets = mutableMapOf<Player, Pet>()
    private val farmConfigSessions = mutableMapOf<UUID, FarmConfigSession>()

    fun capturePet(player: Player, speciesId: String, nickname: String?): CapturedPet? {
        val species = PetSpeciesRegistry.getSpecies(speciesId)
        if (species == null) {
            player.sendMessage("§cUnknown species '$speciesId'.")
            return null
        }

        val profile = getProfile(player.uniqueId)
        val resolvedNickname = resolveNickname(profile, nickname ?: species.displayName)
        val evs = StatSet.randomEV(random)
        val ivs = StatSet.randomIV(random)
        val level = plugin.config.getInt("starting-level", 5)
        val talents = TalentRegistry.rollTalents(random, species, count = 2)
        val captured = CapturedPet(
            id = UUID.randomUUID(),
            speciesId = species.id,
            nickname = resolvedNickname,
            level = level,
            experience = 0,
            evs = evs,
            ivs = ivs,
            talentIds = talents.toMutableList(),
            farmTargets = mutableSetOf()
        )
        profile.pets[captured.id] = captured
        if (profile.activePetId == null) {
            profile.activePetId = captured.id
            spawnActivePet(player)
        } else {
            saveProfiles()
        }
        player.sendMessage("§aYou befriended §b${resolvedNickname}§a the §d${species.displayName}§a!")
        return captured
    }

    fun spawnActivePet(player: Player) {
        val profile = getProfile(player.uniqueId)
        val captured = profile.getActivePet() ?: run {
            player.sendMessage("§7You do not have an active pet selected.")
            return
        }
        activePets.remove(player)?.remove()
        val species = PetSpeciesRegistry.getSpecies(captured.speciesId)
        if (species == null) {
            player.sendMessage("§c${captured.nickname} cannot be summoned because its species is missing.")
            return
        }
        val entity = spawnCompanionEntity(player.location.add(0.5, 0.0, 0.5), species)
        val pet = Pet(plugin, player, captured, species, entity, random)
        pet.spawn()
        activePets[player] = pet
        saveProfiles()
        player.sendMessage("§a${captured.nickname} appears at your side.")
    }

    fun dismissPet(player: Player) {
        activePets.remove(player)?.remove()
        player.sendMessage("§cYour pet returns to its dormitory.")
    }

    fun listPets(player: Player): List<CapturedPet> = getProfile(player.uniqueId).pets.values.sortedBy { it.nickname.lowercase(Locale.US) }

    fun selectPet(player: Player, nickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(nickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $nickname.")
            return false
        }
        profile.activePetId = captured.id
        spawnActivePet(player)
        return true
    }

    fun releasePet(player: Player, nickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(nickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $nickname.")
            return false
        }
        profile.pets.remove(captured.id)
        if (profile.activePetId == captured.id) {
            dismissPet(player)
            profile.activePetId = profile.pets.keys.firstOrNull()
            profile.activePetId?.let { spawnActivePet(player) }
        }
        saveProfiles()
        player.sendMessage("§7You released $nickname back into the Spiral.")
        return true
    }

    fun renamePet(player: Player, oldNickname: String, newNickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(oldNickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $oldNickname.")
            return false
        }
        val resolved = resolveNickname(profile, newNickname)
        captured.nickname = resolved
        if (profile.activePetId == captured.id) {
            activePets[player]?.refreshDisplayName()
        }
        saveProfiles()
        player.sendMessage("§aYour pet will now be known as §b$resolved§a.")
        return true
    }

    fun getActivePet(player: Player): Pet? = activePets[player]

    fun commandStay(player: Player) {
        val pet = getActivePet(player)
        if (pet == null) {
            player.sendMessage("§cYou do not have an active pet summoned.")
            return
        }
        val anchor = player.location.clone()
        pet.setMode(PetMode.STATIONARY, anchor)
        player.sendMessage("§e${pet.captured.nickname} will maintain this post.")
    }

    fun commandFollow(player: Player) {
        val pet = getActivePet(player)
        if (pet == null) {
            player.sendMessage("§cYou do not have an active pet summoned.")
            return
        }
        pet.setMode(PetMode.FOLLOW, null)
        player.sendMessage("§a${pet.captured.nickname} resumes following you.")
    }

    fun openCropConfigurator(player: Player) {
        val pet = getActivePet(player)
        val captured = pet?.captured
        if (pet == null || captured == null) {
            player.sendMessage("§cYou need your active companion summoned to configure farming.")
            return
        }
        val inventory = Bukkit.createInventory(player, InventoryType.ANVIL, Component.text("Pet Farming"))
        val anvil = inventory as AnvilInventory
        anvil.secondItem = guideBook(captured)
        player.openInventory(anvil)
        farmConfigSessions[player.uniqueId] = FarmConfigSession(captured.id)
        player.sendMessage("§7Place a crop in the left slot to toggle harvesting. Take the result to confirm.")
    }

    fun getProfile(uuid: UUID): TamerProfile = profiles.computeIfAbsent(uuid) { TamerProfile(mutableMapOf(), null) }

    fun removeAllPets() {
        activePets.values.forEach { it.remove() }
        activePets.clear()
        saveProfiles()
    }

    fun shutdown() {
        activePets.values.forEach { it.remove() }
        activePets.clear()
        profileStore.saveProfiles(profiles)
    }

    fun handleJoin(player: Player) {
        val profile = getProfile(player.uniqueId)
        if (plugin.config.getBoolean("autosummon", true) && profile.activePetId != null) {
            Bukkit.getScheduler().runTask(plugin) { spawnActivePet(player) }
        }
    }

    fun handleQuit(player: Player) {
        activePets.remove(player)?.remove()
    }

    fun adminGivePet(target: Player, speciesId: String, nickname: String?): CapturedPet? = capturePet(target, speciesId, nickname)

    fun reloadSpecies() {
        PetSpeciesRegistry.reload(plugin)
        activePets.values.forEach { it.remove() }
        activePets.keys.toList().forEach { spawnActivePet(it) }
    }

    private fun resolveNickname(profile: TamerProfile, candidate: String): String {
        val baseName = candidate.trim().ifEmpty { "Companion" }
        var name = baseName
        var index = 2
        while (profile.pets.values.any { it.nickname.equals(name, ignoreCase = true) }) {
            name = "$baseName $index"
            index++
        }
        return name
    }

    private fun spawnCompanionEntity(location: Location, species: PetSpecies): LivingEntity {
        val world = location.world ?: throw IllegalStateException("Cannot spawn pet without world")
        val entity = world.spawnEntity(location, species.entityType) as? LivingEntity
            ?: world.spawnEntity(location, EntityType.ARMOR_STAND) as LivingEntity
        entity.isSilent = true
        entity.isInvulnerable = true
        entity.customName = species.displayName
        entity.isCustomNameVisible = true
        entity.setAI(false)
        entity.setGravity(false)
        entity.equipment?.helmet = ItemStack(species.modelItem)
        if (entity.type == EntityType.ARMOR_STAND) {
            val stand = entity as org.bukkit.entity.ArmorStand
            stand.isSmall = true
            stand.isMarker = true
            stand.isVisible = false
        }
        return entity
    }

    private fun saveProfiles() {
        profileStore.saveProfiles(profiles)
    }

    private fun guideBook(captured: CapturedPet): ItemStack {
        val book = ItemStack(Material.BOOK)
        val meta = book.itemMeta
        meta.displayName(Component.text("Configure ${captured.nickname}"))
        val enabled = captured.farmTargets.mapNotNull { Material.matchMaterial(it) }
        val lore = mutableListOf<Component>()
        lore += Component.text("Place a crop item to toggle it.")
        if (enabled.isEmpty()) {
            lore += Component.text("No crops enabled.")
        } else {
            lore += Component.text("Enabled crops:")
            enabled.sortedBy { it.displayName() }.forEach { crop ->
                lore += Component.text("- ${crop.displayName()}")
            }
        }
        meta.lore(lore)
        book.itemMeta = meta
        return book
    }

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val player = event.viewers.filterIsInstance<Player>().firstOrNull() ?: return
        val session = farmConfigSessions[player.uniqueId] ?: return
        val captured = getProfile(player.uniqueId).pets[session.petId] ?: return
        val input = event.inventory.firstItem
        if (input == null || input.type.isAir) {
            event.result = null
            return
        }
        val material = input.type
        if (material !in FARMABLES) {
            event.result = ItemStack(Material.BARRIER).apply {
                val meta = itemMeta
                meta.displayName(Component.text("Not a supported crop"))
                itemMeta = meta
            }
            return
        }
        val enabled = captured.farmTargets.contains(material.name)
        event.result = ItemStack(if (enabled) Material.REDSTONE_TORCH else Material.EMERALD).apply {
            val meta = itemMeta
            val action = if (enabled) "Disable" else "Enable"
            meta.displayName(Component.text("$action ${material.displayName()}"))
            itemMeta = meta
        }
    }

    @EventHandler
    fun onAnvilClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = farmConfigSessions[player.uniqueId] ?: return
        if (event.view.topInventory.type != InventoryType.ANVIL) return
        if (event.slotType != InventoryType.SlotType.RESULT) return
        event.isCancelled = true
        val inventory = event.view.topInventory as AnvilInventory
        val input = inventory.firstItem ?: return
        val material = input.type
        if (material !in FARMABLES) {
            player.sendMessage("§cThat item cannot be automated.")
            return
        }
        val captured = getProfile(player.uniqueId).pets[session.petId] ?: return
        val enabled = captured.toggleFarmTarget(material)
        val verb = if (enabled) "enabled" else "disabled"
        val color = if (enabled) "§a" else "§c"
        player.sendMessage("$color${material.displayName()} farming $verb for ${captured.nickname}.")
        inventory.secondItem = guideBook(captured)
        inventory.result = null
        saveProfiles()
        Bukkit.getScheduler().runTask(plugin) { player.updateInventory() }
    }

    @EventHandler
    fun onAnvilClose(event: InventoryCloseEvent) {
        farmConfigSessions.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        handleJoin(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        handleQuit(event.player)
    }
}

class Pet(
    private val plugin: WizPets,
    val owner: Player,
    val captured: CapturedPet,
    val species: PetSpecies,
    private val entity: LivingEntity,
    private val random: Random
) {
    private var tickTask: BukkitTask? = null
    private var attackCooldown = 0
    private val talents: List<Talent> = TalentRegistry.instantiate(captured.talentIds, random)
    private var mode: PetMode = PetMode.FOLLOW
    private var anchor: Location? = null
    private val farmAgent = FarmAgent(plugin)

    val displayName: String
        get() = "${owner.name}'s ${captured.nickname}"

    val element: Element
        get() = species.element

    var extraCollectionRange: Double = 0.0

    fun spawn() {
        entity.customName = displayName
        entity.isCustomNameVisible = true
        entity.equipment?.helmet = ItemStack(species.modelItem)
        talents.forEach { it.onSummon(this) }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L, 20L)
    }

    fun refreshDisplayName() {
        entity.customName = displayName
    }

    fun getStat(stat: StatType): Double {
        val base = species.baseStats[stat]
        val iv = captured.ivs[stat]
        val evContribution = captured.evs[stat] / 32.0
        val levelBonus = captured.level * when (stat) {
            StatType.HEALTH -> 0.9
            StatType.ATTACK -> 0.6
            StatType.DEFENSE -> 0.55
            StatType.MAGIC -> 0.65
            StatType.SPEED -> 0.7
        }
        return base + iv + evContribution + levelBonus
    }

    fun getStatBreakdown(): Map<String, Double> = StatType.entries.associate { it.displayName to getStat(it) }

    fun hasTalent(id: String): Boolean = captured.talentIds.any { it.equals(id, ignoreCase = true) }

    private fun tick() {
        if (!owner.isOnline || owner.isDead) {
            remove()
            return
        }

        val targetLocation = when (mode) {
            PetMode.FOLLOW -> owner.location.clone().add(0.5, 0.0, 0.5)
            PetMode.STATIONARY -> anchor ?: owner.location.clone()
        }
        if (mode == PetMode.STATIONARY) {
            if (entity.location.distanceSquared(targetLocation) > 4.0) {
                entity.teleport(targetLocation)
            }
        } else {
            entity.teleport(targetLocation)
        }
        talents.forEach { it.tick(this) }
        if (attackCooldown > 0) {
            attackCooldown--
        }
        handleHealing()
        handleCombat()
        handleCollection()
        farmAgent.tick(this)
    }

    private fun handleHealing() {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        val healAmount = max(0.0, getStat(StatType.MAGIC) * 0.03)
        val newHealth = min(maxHealth, owner.health + healAmount)
        owner.health = newHealth
    }

    private fun handleCombat() {
        val world = entity.world
        val range = 10.0
        val nearby = world.getNearbyEntities(entity.location, range, range, range)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
        val target = nearby.minByOrNull { it.location.distanceSquared(entity.location) }
        if (target != null && attackCooldown <= 0) {
            val physical = getStat(StatType.ATTACK)
            val magical = getStat(StatType.MAGIC)
            val damage = physical * 0.6 + magical * 0.5
            target.damage(damage, owner)
            owner.sendActionBar(Component.text("§d${captured.nickname} strikes ${target.name} for ${"%.1f".format(damage)}"))
            talents.forEach { it.onAttack(this, target, damage) }
            val speedBonus = getStat(StatType.SPEED)
            attackCooldown = max(2, (6 - speedBonus / 5).toInt())
        }
    }

    private fun handleCollection() {
        val center = when (mode) {
            PetMode.FOLLOW -> entity.location
            PetMode.STATIONARY -> anchor ?: entity.location
        }
        val range = 2.5 + extraCollectionRange
        val items = entity.world.getNearbyEntities(center, range, range, range)
            .filterIsInstance<Item>()
        for (item in items) {
            if (item.isDead || item.itemStack.type.isAir) continue
            val leftovers = owner.inventory.addItem(item.itemStack)
            if (leftovers.isEmpty()) {
                item.remove()
            } else {
                item.itemStack = leftovers.values.first()
            }
        }
    }

    fun heal(amount: Double) {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        owner.health = min(owner.health + amount, maxHealth)
    }

    fun grantAbsorption(hearts: Double) {
        owner.absorptionAmount = max(owner.absorptionAmount, hearts * 2)
    }

    fun remove() {
        tickTask?.cancel()
        tickTask = null
        talents.forEach { it.onDismiss(this) }
        entity.remove()
    }

    fun setMode(newMode: PetMode, anchorLocation: Location?) {
        mode = newMode
        anchor = anchorLocation?.clone() ?: if (newMode == PetMode.STATIONARY) entity.location.clone() else null
    }

    fun getMode(): PetMode = mode

    fun getAnchor(): Location? = anchor?.clone()
}

data class CapturedPet(
    val id: UUID,
    val speciesId: String,
    var nickname: String,
    var level: Int,
    var experience: Int,
    val evs: StatSet,
    val ivs: StatSet,
    val talentIds: MutableList<String>,
    val farmTargets: MutableSet<String>
) {
    fun toggleFarmTarget(material: Material): Boolean {
        val key = material.name
        return if (farmTargets.remove(key)) {
            false
        } else {
            farmTargets.add(key)
            true
        }
    }
}

data class TamerProfile(
    val pets: MutableMap<UUID, CapturedPet>,
    var activePetId: UUID?,
) {
    fun getActivePet(): CapturedPet? = activePetId?.let { pets[it] }

    fun findByNickname(nickname: String): CapturedPet? = pets.values.firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }
}

private class ProfileStore(private val plugin: WizPets) {
    private val file: File = File(plugin.dataFolder, "profiles.yml")

    fun loadProfiles(): MutableMap<UUID, TamerProfile> {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val profilesSection = config.getConfigurationSection("profiles") ?: return mutableMapOf()
        val map = mutableMapOf<UUID, TamerProfile>()
        for (uuidKey in profilesSection.getKeys(false)) {
            val uuid = runCatching { UUID.fromString(uuidKey) }.getOrNull() ?: continue
            val profileSection = profilesSection.getConfigurationSection(uuidKey) ?: continue
            val petsSection = profileSection.getConfigurationSection("pets")
            val pets = mutableMapOf<UUID, CapturedPet>()
            petsSection?.getKeys(false)?.forEach { petKey ->
                val petId = runCatching { UUID.fromString(petKey) }.getOrNull() ?: return@forEach
                val petSection = petsSection.getConfigurationSection(petKey) ?: return@forEach
                val speciesId = petSection.getString("species") ?: return@forEach
                val nickname = petSection.getString("nickname") ?: speciesId
                val level = petSection.getInt("level", 5)
                val experience = petSection.getInt("experience", 0)
                val evs = petSection.parseStatSet("evs") ?: StatSet.randomEV(Random(System.nanoTime()))
                val ivs = petSection.parseStatSet("ivs") ?: StatSet.randomIV(Random(System.nanoTime()))
                val talents = petSection.getStringList("talents").map { it.lowercase(Locale.US) }.toMutableList()
                val farmTargets = petSection.getStringList("farm-crops").map { it.uppercase(Locale.US) }.toMutableSet()
                pets[petId] = CapturedPet(petId, speciesId, nickname, level, experience, evs, ivs, talents, farmTargets)
            }
            val activePet = profileSection.getString("active")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            map[uuid] = TamerProfile(pets, activePet)
        }
        return map
    }

    fun saveProfiles(profiles: Map<UUID, TamerProfile>) {
        val config = YamlConfiguration()
        val root = config.createSection("profiles")
        for ((uuid, profile) in profiles) {
            val profileSection = root.createSection(uuid.toString())
            profile.activePetId?.let { profileSection.set("active", it.toString()) }
            val petsSection = profileSection.createSection("pets")
            for ((petId, pet) in profile.pets) {
                val petSection = petsSection.createSection(petId.toString())
                petSection.set("species", pet.speciesId)
                petSection.set("nickname", pet.nickname)
                petSection.set("level", pet.level)
                petSection.set("experience", pet.experience)
                petSection.set("evs", pet.evs.toMap())
                petSection.set("ivs", pet.ivs.toMap())
                petSection.set("talents", pet.talentIds)
                petSection.set("farm-crops", pet.farmTargets.toList())
            }
        }
        file.parentFile.mkdirs()
        config.save(file)
    }
}

private fun ConfigurationSection.parseStatSet(path: String): StatSet? {
    val section = getConfigurationSection(path) ?: return null
    return StatSet(
        health = section.getDouble("health", 0.0),
        attack = section.getDouble("attack", 0.0),
        defense = section.getDouble("defense", 0.0),
        magic = section.getDouble("magic", 0.0),
        speed = section.getDouble("speed", 0.0)
    )
}

private fun StatSet.toMap(): Map<String, Double> = mapOf(
    "health" to health,
    "attack" to attack,
    "defense" to defense,
    "magic" to magic,
    "speed" to speed
)

private data class FarmConfigSession(val petId: UUID)

enum class PetMode {
    FOLLOW,
    STATIONARY
}

private class FarmAgent(private val plugin: WizPets) {
    private var interval = 0

    fun tick(pet: Pet) {
        if (pet.getMode() != PetMode.STATIONARY) return
        if (!pet.hasTalent("agrarian_directive")) return
        if (pet.captured.farmTargets.isEmpty()) return
        interval = (interval + 1) % 20
        if (interval != 0) return
        val anchor = pet.getAnchor() ?: return
        val crops = pet.captured.farmTargets.mapNotNull { Material.matchMaterial(it) }.toSet()
        if (crops.isEmpty()) return
        val world = anchor.world
        val radius = 6
        val baseY = anchor.blockY
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                for (yOffset in -1..1) {
                    val crop = world.getBlockAt(anchor.blockX + x, baseY + yOffset, anchor.blockZ + z)
                    if (crop.type !in crops) continue
                    val data = crop.blockData as? Ageable ?: continue
                    if (data.age < data.maximumAge) continue
                    harvestCrop(pet, crop)
                    return
                }
            }
        }
    }

    private fun harvestCrop(pet: Pet, crop: Block) {
        val type = crop.type
        crop.breakNaturally()
        Bukkit.getScheduler().runTask(plugin) {
            if (consumeSeed(pet.owner, type)) {
                crop.type = type
                val replanted = crop.blockData as? Ageable
                if (replanted != null) {
                    replanted.age = 0
                    crop.blockData = replanted
                }
            }
            crop.world.spawnParticle(Particle.VILLAGER_HAPPY, crop.location.add(0.5, 0.5, 0.5), 10, 0.4, 0.4, 0.4, 0.0)
        }
    }

    private fun consumeSeed(owner: Player, crop: Material): Boolean {
        val seed = SEED_LOOKUP[crop] ?: return true
        val slot = owner.inventory.first(seed)
        if (slot < 0) return false
        val stack = owner.inventory.getItem(slot)
        if (stack != null) {
            stack.amount -= 1
            if (stack.amount <= 0) {
                owner.inventory.clear(slot)
            } else {
                owner.inventory.setItem(slot, stack)
            }
            return true
        }
        return false
    }
}

private val FARMABLES = setOf(
    Material.WHEAT,
    Material.CARROTS,
    Material.POTATOES,
    Material.BEETROOTS,
    Material.NETHER_WART,
    Material.SWEET_BERRY_BUSH,
    Material.COCOA
)

private val SEED_LOOKUP = mapOf(
    Material.WHEAT to Material.WHEAT_SEEDS,
    Material.CARROTS to Material.CARROT,
    Material.POTATOES to Material.POTATO,
    Material.BEETROOTS to Material.BEETROOT_SEEDS,
    Material.NETHER_WART to Material.NETHER_WART,
    Material.SWEET_BERRY_BUSH to Material.SWEET_BERRIES,
    Material.COCOA to Material.COCOA_BEANS
)

private fun Material.displayName(): String = name.lowercase(Locale.US)
    .split('_')
    .joinToString(" ") { it.replaceFirstChar { ch -> ch.titlecase(Locale.US) } }
