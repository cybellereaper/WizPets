package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class MoveKind { PHYSICAL, MAGICAL, SUPPORT }

data class BattleMove(
    val id: String,
    val name: String,
    val description: String,
    val kind: MoveKind,
    val power: Double = 1.0,
    val supportAction: ((BattleParticipant) -> Unit)? = null
)

class BattleParticipant(val player: Player, val pet: Pet) {
    val maxHealth: Double = pet.getStat(StatType.HEALTH)
    var currentHealth: Double = maxHealth

    fun applyDamage(amount: Double) {
        currentHealth = max(0.0, currentHealth - amount)
    }

    fun heal(amount: Double) {
        currentHealth = min(maxHealth, currentHealth + amount)
    }

    fun isDefeated(): Boolean = currentHealth <= 0.01
}

class PetBattleManager(private val plugin: WizPets) : Listener {
    private val random = Random(System.currentTimeMillis())
    private val activeBattles = mutableSetOf<PetBattle>()

    fun startBattle(challenger: Player, opponent: Player) {
        val petA = plugin.petManager.getActivePet(challenger)
        val petB = plugin.petManager.getActivePet(opponent)
        if (petA == null) {
            challenger.sendMessage("§cYou need an active pet to start a battle.")
            return
        }
        if (petB == null) {
            challenger.sendMessage("§c${opponent.name} does not have an active pet to battle.")
            return
        }
        if (activeBattles.any { it.contains(challenger) || it.contains(opponent) }) {
            challenger.sendMessage("§cOne of you is already battling.")
            return
        }
        val battle = PetBattle(plugin, challenger, opponent, petA, petB, random) { finished ->
            activeBattles.remove(finished)
        }
        activeBattles += battle
        battle.begin()
    }

    fun shutdown() {
        activeBattles.toList().forEach { it.stop() }
        activeBattles.clear()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val battle = activeBattles.firstOrNull { it.contains(event.player) } ?: return
        battle.stop(withForfeit = true)
        activeBattles.remove(battle)
    }
}

class PetBattle(
    private val plugin: WizPets,
    private val playerA: Player,
    private val playerB: Player,
    private val petA: Pet,
    private val petB: Pet,
    private val random: Random,
    private val onFinish: (PetBattle) -> Unit
) {
    private val participantA = BattleParticipant(playerA, petA)
    private val participantB = BattleParticipant(playerB, petB)
    private var task: BukkitTask? = null
    private var round = 1

    fun begin() {
        announce("§dA pet battle between §b${playerA.name}§d and §b${playerB.name}§d begins!")
        playerA.playSound(playerA.location, org.bukkit.Sound.MUSIC_DISC_FAR, 0.6f, 1.2f)
        playerB.playSound(playerB.location, org.bukkit.Sound.MUSIC_DISC_FAR, 0.6f, 1.2f)
        task = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 40L, 40L)
    }

    fun stop(withForfeit: Boolean = false) {
        task?.cancel()
        task = null
        if (withForfeit) {
            val quitter = if (!playerA.isOnline) playerA else playerB
            val winner = if (quitter == playerA) playerB else playerA
            announce("§c${quitter.name} forfeited the battle! ${winner.name}'s pet is victorious.")
        }
        onFinish(this)
    }

    fun contains(player: Player): Boolean = player.uniqueId == playerA.uniqueId || player.uniqueId == playerB.uniqueId

    private fun tick() {
        if (!playerA.isOnline || !playerB.isOnline) {
            stop(withForfeit = true)
            return
        }
        if (participantA.isDefeated() || participantB.isDefeated()) {
            finishBattle()
            return
        }
        val moveA = petA.randomBattleMove(random)
        val moveB = petB.randomBattleMove(random)
        val speedA = petA.getStat(StatType.SPEED)
        val speedB = petB.getStat(StatType.SPEED)
        announce("§7-- Round $round --")
        round++
        val first = if (speedA > speedB) {
            listOf({ executeMove(participantA, participantB, moveA) }, { executeMove(participantB, participantA, moveB) })
        } else if (speedB > speedA) {
            listOf({ executeMove(participantB, participantA, moveB) }, { executeMove(participantA, participantB, moveA) })
        } else if (random.nextBoolean()) {
            listOf({ executeMove(participantA, participantB, moveA) }, { executeMove(participantB, participantA, moveB) })
        } else {
            listOf({ executeMove(participantB, participantA, moveB) }, { executeMove(participantA, participantB, moveA) })
        }
        for (action in first) {
            if (participantA.isDefeated() || participantB.isDefeated()) break
            action.invoke()
        }
        if (participantA.isDefeated() || participantB.isDefeated()) {
            finishBattle()
        } else {
            sendStatus()
        }
    }

    private fun executeMove(attacker: BattleParticipant, defender: BattleParticipant, move: BattleMove) {
        when (move.kind) {
            MoveKind.SUPPORT -> {
                move.supportAction?.invoke(attacker)
                announce("§a${attacker.player.name}'s ${attacker.pet.captured.nickname} uses ${move.name}!")
            }
            else -> {
                val damage = calculateDamage(attacker.pet, defender.pet, move)
                defender.applyDamage(damage)
                announce(
                    "§d${attacker.pet.captured.nickname} uses ${move.name} for ${"%.1f".format(damage)} damage!"
                )
            }
        }
    }

    private fun calculateDamage(attacker: Pet, defender: Pet, move: BattleMove): Double {
        val offense = when (move.kind) {
            MoveKind.PHYSICAL -> attacker.getStat(StatType.ATTACK)
            MoveKind.MAGICAL -> attacker.getStat(StatType.MAGIC)
            MoveKind.SUPPORT -> 0.0
        }
        val defense = when (move.kind) {
            MoveKind.PHYSICAL -> defender.getStat(StatType.DEFENSE)
            MoveKind.MAGICAL -> defender.getStat(StatType.MAGIC)
            MoveKind.SUPPORT -> 0.0
        }
        val base = max(1.0, offense * move.power - defense * 0.35)
        val elementBonus = if (attacker.element == defender.element) 1.0 else attacker.element.matchupAgainst(defender.element)
        return base * elementBonus
    }

    private fun finishBattle() {
        task?.cancel()
        task = null
        val winner = if (participantA.isDefeated()) participantB else participantA
        val loser = if (participantA.isDefeated()) participantA else participantB
        announce("§b${winner.player.name}'s ${winner.pet.captured.nickname} wins the battle!")
        val experience = plugin.config.getInt("battle-experience", 45)
        winner.pet.captured.gainExperience(experience)
        winner.player.sendMessage("§aYour pet gained $experience experience from the battle!")
        loser.player.sendMessage("§cYour pet will grow stronger from this defeat.")
        plugin.petManager.saveProfiles()
        onFinish(this)
    }

    private fun sendStatus() {
        playerA.sendActionBar(
            Component.text("§d${participantA.pet.captured.nickname}: ${"%.0f".format(participantA.currentHealth)} / ${"%.0f".format(participantA.maxHealth)}")
        )
        playerB.sendActionBar(
            Component.text("§d${participantB.pet.captured.nickname}: ${"%.0f".format(participantB.currentHealth)} / ${"%.0f".format(participantB.maxHealth)}")
        )
    }

    private fun announce(message: String) {
        playerA.sendMessage(message)
        playerB.sendMessage(message)
    }
}

private fun Element.matchupAgainst(defender: Element): Double = when (this) {
    Element.FIRE -> when (defender) {
        Element.ICE, Element.NATURE -> 1.25
        Element.WATER -> 0.75
        else -> 1.0
    }
    Element.WATER -> when (defender) {
        Element.FIRE, Element.STORM -> 1.25
        Element.NATURE -> 0.75
        else -> 1.0
    }
    Element.STORM -> when (defender) {
        Element.WATER, Element.MYTH -> 1.25
        Element.ICE -> 0.75
        else -> 1.0
    }
    Element.NATURE -> when (defender) {
        Element.WATER, Element.MYTH -> 1.25
        Element.FIRE -> 0.75
        else -> 1.0
    }
    Element.ICE -> when (defender) {
        Element.STORM, Element.FIRE -> 1.25
        Element.MYTH -> 0.75
        else -> 1.0
    }
    Element.MYTH -> when (defender) {
        Element.ARCANE -> 1.25
        Element.NATURE -> 0.75
        else -> 1.0
    }
    Element.ARCANE -> when (defender) {
        Element.MYTH -> 0.75
        else -> 1.0
    }
}
