# WizPets

WizPets is a Spigot/Paper plugin that gives every adventurer a magical companion. Pets heal, fight, fly, and even carry their owners thanks to an expanded persistent system backed by Bukkit's Persistent Data Containers (PDCs).

## Features

- **Summonable combat familiars** that orbit their owner, heal them, and attack nearby monsters.
- **Persistent progression** stored in PDCs, including EVs/IVs, generation, unlocked abilities, and talent selections – no YAML save files required.
- **Breeding mechanic** that lets two players combine their pets to produce a higher-generation companion with blended stats and inherited talents.
- **Mounting and aerial travel** so players can ride their pet and unlock flight assistance.
- **Automatic cleanup** of armor stands on player disconnects and server shutdown to prevent lingering entities.
- **Actionable debugging** via `/wizpet debug` to inspect saved data and detailed logging for key lifecycle events.
- **Kotlin-scripted behaviors and particle control** so animations, raycasts, AOE pulses, and pet decision making can be tweaked and hot-reloaded without recompiling.

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
| `/wizpet script list` | List the Kotlin behavior scripts currently loaded. |
| `/wizpet script set <name>` | Assign a loaded behavior script to your pet. |
| `/wizpet script reload` | Reload all scripts and particle definitions from `plugins/WizPets/scripts`. |

## Data Storage

Every player has their pet data serialized into their personal `PersistentDataContainer`. This includes:

- Display name
- EVs/IVs
- Talent identifiers
- Generation and breeding count
- Ability unlock flags (mounting, flight)
- Active behavior script identifier

Data is saved automatically whenever stats change, on logout, and during server shutdown. Orphaned armor stands tagged by the plugin are also cleaned up on disable.

## Building

Run the Gradle build to compile the plugin JAR:

```bash
./gradlew build
```

The compiled artifact will appear under `build/libs/`.

## Kotlin Scripting & Animations

WizPets loads Kotlin script files from `plugins/WizPets/scripts` on startup. Each `.kts` file can register:

- `petBehavior("name") { ... }` blocks that describe summon/tick/attack/dismiss logic with full access to the `PetScriptContext` helpers (`playSequence`, `playRaycast`, `playAreaEffect`, `healOwner`, etc.).
- `particleSequence`, `raycastAnimation`, and `areaEffect` builders that define reusable particle timelines, beam-style raycasts, and layered area pulses.

Edit or drop new scripts into that folder, then run `/wizpet script reload` in game to hot-reload behaviors and particle definitions without restarting the server. A default `default.kts` script is shipped as a reference implementation.

## License

This project is released under the [BSD 3-Clause License](LICENSE).
