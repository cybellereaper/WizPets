# WizPets

WizPets is a Spigot/Paper plugin that gives every adventurer a magical companion. Pets heal, fight, fly, and even carry their owners thanks to an expanded persistent system backed by Bukkit's Persistent Data Containers (PDCs).

The codebase has been completely rewritten around a modular service architecture that exposes an official developer API. Other plugins can safely interact with pets, listen for lifecycle events, and register their own talents at runtime.

## Features

- **Summonable combat familiars** that orbit their owner, heal them, and attack nearby monsters.
- **Persistent progression** stored in PDCs, including EVs/IVs, generation, unlocked abilities, and talent selections – no YAML save files required.
- **Breeding mechanic** that lets two players combine their pets to produce a higher-generation companion with blended stats and inherited talents.
- **Mounting and aerial travel** so players can ride their pet and unlock flight assistance.
- **Automatic cleanup** of armor stands on player disconnects and server shutdown to prevent lingering entities.
- **Actionable debugging** via `/wizpet debug` to inspect saved data and detailed logging for key lifecycle events.
- **Modern Blockbench animations** powering armor stand poses, including idle and combat sequences driven by a reusable model engine.

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

## Developer API

WizPets registers an implementation of `com.github.cybellereaper.wizpets.api.WizPetsApi` with Bukkit's service manager during plugin enable. Retrieve it with:

```kotlin
val api = Bukkit.getServicesManager().load(WizPetsApi::class.java)
```

The API lets you:

- Inspect and persist pet data (`storedPet`, `activePet`, `persist`).
- Work with stored records directly through the `PetPersistence` API for clean PDC access,
  including helpers like `loadOrCreate`, `exists`, and atomic `compute` mutations for modern PDC
  tooling.
- Summon or dismiss pets with explicit reasons for traceability.
- Register and unregister custom talents at runtime with `registerTalent` / `unregisterTalent`.
- Subscribe to lifecycle callbacks through `addListener` to react to summons, dismissals, and saves.
- Drive custom models and animations through the Blockbench engine exposed via `blockbench()`, letting other plugins register new model files or create bespoke animators.

Custom talents implement `PetTalent` and can manipulate stats, react to ticks, or respond to attacks. Register a talent factory to make it available to players:

```kotlin
api.registerTalent(TalentFactory { MyCustomTalent() })
```

See `src/main/kotlin/com/github/cybellereaper/wizpets/api` for the full set of developer-facing types.

## Building

Run the Gradle build to compile the plugin JAR:

```bash
./gradlew build
```

The compiled artifact will appear under `build/libs/`.

## License

This project is released under the [BSD 3-Clause License](LICENSE).
