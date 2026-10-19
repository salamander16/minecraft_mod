# Blood Moon (Fabric, Minecraft 26.3)

Every **20th night** a Blood Moon rises. It is a **server-side mod**: players join with a
completely vanilla client, nothing to install on their side.

| | |
|---|---|
| Minecraft | **26.3** (latest Java release, Sept 2026) |
| Loader | Fabric Loader 0.19.5+ and Fabric API |
| Java | 25 |
| Side | Server (also works in singleplayer) |

## What happens during a Blood Moon

* **Red night** – sky, fog, clouds and night lighting turn red (done with a data-driven timeline, so
  vanilla clients render it with no resource pack).
* **Double monster spawns** – every naturally spawning monster gets a twin (configurable multiplier,
  capped for performance). Twins vanish at dawn.
* **Tougher mobs** – +50% health and a much larger detection range.
* **Overpowered loot** – extra XP plus bonus drops: iron, gold, emeralds, diamonds, ender pearls,
  golden apples, netherite scrap, and very rarely enchanted golden apples, totems and netherite ingots.
* **Creepers** are fast (Speed II).
* **Spiders & cave spiders** are invisible.
* **Skeletons (skeleton, stray, bogged)**: ~5% carry a glowing **explosive bow**; their arrows explode on impact.
* **Zombies (zombie, husk, drowned, zombie villager)** wear enchanted diamond/netherite armour, sometimes carry
  a Sharpness V netherite sword, and **break through entrances**: wooden doors, fence gates, fences and
  trapdoors.
* **Drowned** carry tridents with **Channeling** (and a thunderstorm is started so channeling actually strikes lightning).

## Install

1. Install Fabric Loader for Minecraft 26.3 and put **Fabric API** in `mods/`.
2. Drop `bloodmoon-1.0.0.jar` in `mods/`.
3. Start the server. A config file appears at `config/bloodmoon.json`.

## Commands (permission level: gamemaster / op)

| Command | |
|---|---|
| `/bloodmoon` or `/bloodmoon status` | Shows when the next blood moon is |
| `/bloodmoon start` | Starts one now (lasts until dawn) |
| `/bloodmoon stop` | Ends it / skips tonight's |
| `/bloodmoon reload` | Reloads `config/bloodmoon.json` |

Tip for testing: run `/bloodmoon start` – everything except the red sky starts immediately. The red sky follows
the real clock, so to see it use `/time` to jump to the 20th night.

## Config (`config/bloodmoon.json`)

| Option | Default | Meaning |
|---|---|---|
| `intervalNights` | 20 | Blood moon every N nights (see note on the red sky below) |
| `spawnMultiplier` | 2.0 | 2.0 = double, 3.0 = triple |
| `maxExtraMobs` | 150 | Cap of bonus mobs alive at once |
| `healthMultiplier` / `followRange` | 1.5 / 48 | Mob toughness / detection range |
| `thunderstorm` | true | Start a thunderstorm (needed for Channeling lightning) |
| `creeperSpeedAmplifier` | 1 | 0 = Speed I, 1 = Speed II |
| `invisibleSpiders` | true | |
| `explosiveSkeletonChance` | 0.05 | Share of skeletons with explosive bows |
| `explosiveArrowPower` | 2.0 | Explosion size (TNT = 4) |
| `explosionsBreakBlocks` | true | Obeys the `mobGriefing` gamerule |
| `zombieArmor`, `zombieWeaponChance` | true, 0.25 | |
| `breakEntrances`, `breakDoors`, `breakIronDoors`, `breakFenceGates`, `breakFences`, `breakTrapdoors` | true/false | What zombies may smash |
| `breakTicks` | 100 | Ticks to break one block (20 = 1 s) |
| `bonusLoot`, `bonusLootChance`, `bonusXpMultiplier` | true, 0.6, 2.0 | |

## Building from source

Requires **JDK 25**.

```
./gradlew build
```

The jar is in `build/libs/` (use `bloodmoon-1.0.0.jar`, not the `-sources` one). No JDK 25 locally? Push to GitHub –
the included workflow (`.github/workflows/build.yml`) builds the jar for you and attaches it as an artifact.

## Publishing on Modrinth

1. modrinth.com → **Create a project** → type *Mod*. Paste `docs/MODRINTH_PAGE.md` as the description.
2. Environment: **Server: required**, **Client: optional** (or unsupported). Loader: **Fabric**. Game version: **26.3**.
3. Upload `bloodmoon-1.0.0.jar` as a version, add dependency **Fabric API (required)**.

Before releasing, edit `fabric.mod.json` (authors, links) and `LICENSE`.

## Known limitations

* **The moon texture itself stays white.** A server-only mod cannot swap the moon sprite for vanilla clients.
  The *sky, fog, clouds and night lighting* turn red instead. (A matching optional resource pack is possible.)
* **The red sky is fixed to a 20-night cycle** (`data/bloodmoon/timeline/blood_moon.json`, `period_ticks: 480000`).
  If you change `intervalNights`, gameplay follows the config but the sky does not – edit/override that file
  with a datapack too (period = interval × 24000, night = last day of the period).
* Sleeping through the night skips the blood moon like any other night.
* Wither skeletons use swords, so they do not get explosive bows.

## How "night 20" is counted

The overworld clock is read each half-second. Night number = day index + 1, so the first blood moon is the end of
the 20th day (clock tick 456000 + 13000).
