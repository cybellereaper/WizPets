# WizPets

WizPets is a Spigot/Paper plugin that gives every adventurer a magical companion. Pets heal, fight, fly, and even carry their owners thanks to an expanded persistent system backed by Bukkit's Persistent Data Containers (PDCs).

## Features

- **Summonable combat familiars** that orbit their owner, heal them, and attack nearby monsters.
- **Persistent progression** stored in PDCs, including EVs/IVs, generation, unlocked abilities, and talent selections – no YAML save files required.
- **Breeding mechanic** that lets two players combine their pets to produce a higher-generation companion with blended stats and inherited talents.
- **Mounting and aerial travel** so players can ride their pet and unlock flight assistance.
- **Automatic cleanup** of armor stands on player disconnects and server shutdown to prevent lingering entities.
- **Actionable debugging** via `/wizpet debug` to inspect saved data and detailed logging for key lifecycle events.

## Commands

| Command | Description |
| --- | --- |
| `/wizpet summon` | Summon or respawn your active pet. |
| `/wizpet dismiss` | Dismiss your current pet. |
| `/wizpet stats` | View the calculated stats for your active pet. |
| `/wizpet talents` | List your pet's talents. |
| `/wizpet mount` | Mount and ride your pet. |
| `/wizpet dismount` | Leave your pet's saddle. |
| `/wizpet fly` | Enable assisted flight (unlocks the ability permanently). |
| `/wizpet land` | Disable assisted flight and land safely. |
| `/wizpet breed <player>` | Breed your pet with another player's pet to create a new generation. |
| `/wizpet debug` | View stored pet data for diagnostics. |

## Data Storage

Every player has their pet data serialized into their personal `PersistentDataContainer`. This includes:

- Display name
- EVs/IVs
- Talent identifiers
- Generation and breeding count
- Ability unlock flags (mounting, flight)

Data is saved automatically whenever stats change, on logout, and during server shutdown. Orphaned armor stands tagged by the plugin are also cleaned up on disable.

## Building

Run the Gradle build to compile the plugin JAR:

```bash
./gradlew build
```

The compiled artifact will appear under `build/libs/`.

## License

This project is released under the [BSD 3-Clause License](LICENSE).
