# WizPets

WizPets is a Paper/Spigot plugin that gives every player a loyal magical companion. Pets orbit their owner, heal, fight, fly, and even act as living mounts, all while their data is stored safely inside Bukkit's Persistent Data Containers (PDCs). The project has been rebuilt around a modular service architecture, exposing an official API so other plugins can extend every part of the experience.

## Table of Contents
- [Features](#features)
- [Commands](#commands)
- [Developer API](#developer-api)
- [Data Model](#data-model)
- [Building](#building)
- [License](#license)

## Features
- **Summonable familiars** that follow their owner, heal allies, and attack nearby monsters.
- **Persistent progression** saved to per-player PDCs—no YAML files or manual serialization required.
- **Breeding system** for combining two pets into a higher-generation companion with blended stats and inherited talents.
- **Mounting and flight** so players can ride their pet and unlock aerial assistance.
- **Automated cleanup** that removes tagged armor stands on disconnect and shutdown.
- **Debug utilities** such as `/wizpet debug` and rich logging to inspect pet lifecycle events.
- **Modern Blockbench animations** for all idle and combat poses, powered by a reusable model engine.

## Commands
| Command | Description |
| --- | --- |
| `/wizpet summon` | Summon or respawn the active pet. |
| `/wizpet dismiss` | Send the current pet away. |
| `/wizpet stats` | Display calculated stats for the active pet. |
| `/wizpet talents` | List unlocked talents. |
| `/wizpet mount` | Mount and ride the pet. |
| `/wizpet dismount` | Dismount from the pet. |
| `/wizpet fly` | Toggle assisted flight once unlocked. |
| `/wizpet land` | Disable flight and land safely. |
| `/wizpet breed <player>` | Breed with another player's active pet. |
| `/wizpet debug` | Inspect stored pet data for diagnostics. |

## Developer API
WizPets registers `com.github.cybellereaper.wizpets.api.WizPetsApi` with Bukkit's service manager during plugin enable. Retrieve it with:

```kotlin
val api = Bukkit.getServicesManager().load(WizPetsApi::class.java)
```

The API allows you to:
- Inspect or persist pet data via `storedPet`, `activePet`, and `persist`.
- Use the `PetPersistence` helpers (`loadOrCreate`, `exists`, `compute`, etc.) for safe atomic access to PDC storage.
- Summon or dismiss pets with explicit reasons for traceability.
- Register and unregister custom talents through `registerTalent` / `unregisterTalent`.
- Subscribe to lifecycle callbacks with `addListener` to react to summons, dismissals, or saves.
- Drive Blockbench models and animations via `blockbench()` so other plugins can register new model files or animators.

Custom talents implement `PetTalent`. Register a factory to expose a new talent:

```kotlin
api.registerTalent(TalentFactory { MyCustomTalent() })
```

Refer to `src/main/kotlin/com/github/cybellereaper/wizpets/api` for the full set of developer-facing types and utilities.

## Data Model
Each player's pet information is serialized into their personal `PersistentDataContainer`, including:
- Display name
- EVs/IVs and calculated stats
- Talent identifiers and unlock state
- Generation, breeding count, and ability flags (mounting, flight)

Data is saved whenever stats change, on logout, and during server shutdown. Any armor stands tagged by the plugin are cleaned up automatically when the server disables the plugin.

## Building
Compile the plugin using Gradle:

```bash
./gradlew build
```

The resulting JAR will be placed under `build/libs/`.

## License
Released under the [BSD 3-Clause License](LICENSE).
