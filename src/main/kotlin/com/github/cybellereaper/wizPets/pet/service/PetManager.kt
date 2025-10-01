package com.github.cybellereaper.wizPets.pet.service

import com.github.cybellereaper.wizPets.pet.model.Element
import com.github.cybellereaper.wizPets.pet.model.PetInstance
import com.github.cybellereaper.wizPets.pet.model.PetSpecies
import com.github.cybellereaper.wizPets.pet.model.StatInvestment
import com.github.cybellereaper.wizPets.pet.model.StatInvestments
import com.github.cybellereaper.wizPets.talent.TalentRegistry
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.random.Random

class PetManager(
    talentRegistry: TalentRegistry,
    private val repository: PetRepository,
    private val speciesCatalog: () -> Map<String, PetSpecies>,
) : Listener {

    private val armorStandPool = ArmorStandPool()
    private val petsByOwner: MutableMap<UUID, MutableList<PetInstance>> = ConcurrentHashMap()
    private val summonedPet: MutableMap<UUID, PetInstance> = ConcurrentHashMap()
    @Volatile
    private var talentRegistry: TalentRegistry = talentRegistry

    fun load() {
        val catalog = speciesCatalog()
        repository.refreshSpeciesIndex(catalog)
        petsByOwner.clear()
        petsByOwner.putAll(repository.load(catalog))
    }

    fun save(ownerId: UUID) {
        petsByOwner[ownerId]?.let { repository.save(ownerId, it) }
    }

    fun saveAll() {
        petsByOwner.forEach { (ownerId, pets) -> repository.save(ownerId, pets) }
    }

    fun getPets(ownerId: UUID): List<PetInstance> = petsByOwner[ownerId]?.toList() ?: emptyList()

    fun grant(owner: Player, speciesId: String, nickname: String? = null) {
        val species = speciesCatalog()[speciesId] ?: return
        val instance = PetInstance(
            id = UUID.randomUUID(),
            ownerId = owner.uniqueId,
            nickname = nickname ?: species.displayName,
            species = species,
            level = species.baseLevel,
            investments = defaultInvestments(),
            talents = species.defaultTalents,
        )
        petsByOwner.computeIfAbsent(owner.uniqueId) { mutableListOf() }.add(instance)
        owner.sendMessage("§aYou received a ${species.displayName}!")
        save(owner.uniqueId)
    }

    fun capture(owner: Player, target: LivingEntity) {
        val catalog = speciesCatalog()
        val species = catalog.values.randomOrNull() ?: return
        target.remove()
        val instance = PetInstance(
            id = UUID.randomUUID(),
            ownerId = owner.uniqueId,
            nickname = species.displayName,
            species = species,
            level = max(1, target.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue?.toInt() ?: species.baseLevel),
            investments = rollInvestments(species.element),
            talents = species.defaultTalents,
        )
        petsByOwner.computeIfAbsent(owner.uniqueId) { mutableListOf() }.add(instance)
        owner.addPotionEffect(PotionEffect(PotionEffectType.LUCK, 20 * 60, 1))
        owner.sendMessage("§bCaptured ${species.displayName} with exotic traits!")
        save(owner.uniqueId)
    }

    fun summon(owner: Player, petId: UUID) {
        val instance = petsByOwner[owner.uniqueId]?.firstOrNull { it.id == petId } ?: return
        val location = owner.location.clone().add(1.0, 0.0, 0.0)
        instance.summon(owner, armorStandPool, location)
        summonedPet[owner.uniqueId]?.dismiss(armorStandPool)
        summonedPet[owner.uniqueId] = instance
        owner.sendMessage("§e${instance.nickname} is now deployed.")
    }

    fun dismiss(owner: Player) {
        summonedPet.remove(owner.uniqueId)?.dismiss(armorStandPool)
    }

    fun tick() {
        val iterator = summonedPet.iterator()
        while (iterator.hasNext()) {
            val (ownerId, pet) = iterator.next()
            val owner = Bukkit.getPlayer(ownerId)
            if (owner == null || !owner.isOnline) {
                pet.dismiss(armorStandPool)
                iterator.remove()
                continue
            }
            pet.tick(owner, talentRegistry)
        }
    }

    fun updateTalents(registry: TalentRegistry) {
        talentRegistry = registry
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!petsByOwner.containsKey(player.uniqueId)) {
            petsByOwner[player.uniqueId] = mutableListOf()
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        dismiss(player)
        save(player.uniqueId)
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val owner = (event.damager as? Player) ?: return
        val pet = summonedPet[owner.uniqueId] ?: return
        val target = event.entity as? Monster ?: return

        val prePetDamage = event.damage
        val stats = pet.investments.toSheet(pet.species.statSheet).atLevel(pet.level)
        val elementMultiplier = ELEMENT_WEAKNESSES[pet.species.element]?.getOrDefault(elementOf(target), 1.0) ?: 1.0
        val petDamage = stats.attack * elementMultiplier

        event.damage = prePetDamage + petDamage
        target.noDamageTicks = 0
        if (pet.species.element == Element.FLAME) {
            target.fireTicks = max(target.fireTicks, 60)
        }

        if (pet.damage(prePetDamage)) {
            owner.sendMessage("§c${pet.nickname} needs to recover before fighting again.")
            dismiss(owner)
        }
    }

    private fun defaultInvestments(): StatInvestments = StatInvestments(
        stamina = StatInvestment(0, Random.nextInt(16, 31)),
        power = StatInvestment(0, Random.nextInt(16, 31)),
        defense = StatInvestment(0, Random.nextInt(16, 31)),
        focus = StatInvestment(0, Random.nextInt(16, 31)),
    )

    private fun rollInvestments(element: Element): StatInvestments = when (element) {
        Element.FLAME -> StatInvestments(
            stamina = StatInvestment(32, Random.nextInt(20, 31)),
            power = StatInvestment(96, Random.nextInt(24, 31)),
            defense = StatInvestment(16, Random.nextInt(16, 31)),
            focus = StatInvestment(24, Random.nextInt(16, 31)),
        )
        Element.FROST -> StatInvestments(
            stamina = StatInvestment(64, Random.nextInt(20, 31)),
            power = StatInvestment(24, Random.nextInt(16, 31)),
            defense = StatInvestment(96, Random.nextInt(24, 31)),
            focus = StatInvestment(32, Random.nextInt(20, 31)),
        )
        Element.STORM -> StatInvestments(
            stamina = StatInvestment(40, Random.nextInt(20, 31)),
            power = StatInvestment(72, Random.nextInt(20, 31)),
            defense = StatInvestment(24, Random.nextInt(16, 31)),
            focus = StatInvestment(40, Random.nextInt(20, 31)),
        )
        Element.TERRA -> StatInvestments(
            stamina = StatInvestment(96, Random.nextInt(24, 31)),
            power = StatInvestment(32, Random.nextInt(16, 31)),
            defense = StatInvestment(72, Random.nextInt(20, 31)),
            focus = StatInvestment(20, Random.nextInt(16, 31)),
        )
        Element.AQUA -> StatInvestments(
            stamina = StatInvestment(56, Random.nextInt(20, 31)),
            power = StatInvestment(32, Random.nextInt(16, 31)),
            defense = StatInvestment(48, Random.nextInt(20, 31)),
            focus = StatInvestment(64, Random.nextInt(20, 31)),
        )
        Element.ARCANE -> StatInvestments(
            stamina = StatInvestment(32, Random.nextInt(16, 31)),
            power = StatInvestment(48, Random.nextInt(20, 31)),
            defense = StatInvestment(24, Random.nextInt(16, 31)),
            focus = StatInvestment(96, Random.nextInt(24, 31)),
        )
        Element.NATURE -> StatInvestments(
            stamina = StatInvestment(72, Random.nextInt(20, 31)),
            power = StatInvestment(40, Random.nextInt(16, 31)),
            defense = StatInvestment(40, Random.nextInt(16, 31)),
            focus = StatInvestment(48, Random.nextInt(20, 31)),
        )
    }

    private fun elementOf(target: LivingEntity): Element = when {
        target.fireTicks > 0 -> Element.FLAME
        target.isInWater -> Element.AQUA
        target.world.hasStorm() -> Element.STORM
        else -> Element.TERRA
    }

    companion object {
        private val ELEMENT_WEAKNESSES = mapOf(
            Element.FLAME to mapOf(Element.NATURE to 1.4, Element.AQUA to 0.7),
            Element.FROST to mapOf(Element.FLAME to 0.6, Element.STORM to 1.2),
            Element.STORM to mapOf(Element.AQUA to 1.3, Element.TERRA to 0.8),
            Element.TERRA to mapOf(Element.STORM to 1.2, Element.ARCANE to 0.9),
            Element.AQUA to mapOf(Element.FLAME to 1.5, Element.NATURE to 0.7),
            Element.ARCANE to mapOf(Element.TERRA to 1.4, Element.STORM to 0.8),
            Element.NATURE to mapOf(Element.TERRA to 1.2, Element.FLAME to 0.6),
        )
    }
}

private fun <T> Collection<T>.randomOrNull(): T? = if (isEmpty()) null else elementAt(Random.nextInt(size))

