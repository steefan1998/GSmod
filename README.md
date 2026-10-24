# GS Mod

Tech mod for NeoForge **26.1.2**: machines, two new materials, upgrades, energy storage, daily rewards and daily missions.

## Content

| | |
|---|---|
| Materials | **Galvanite** (ore + deepslate ore, raw, dust, ingot, block; generates between Y -48 and 48) and **Steel** (dust, ingot, block) |
| Machines | **Combustion Generator** (any furnace fuel → FE), **Crusher**, **Alloy Smelter** (two inputs) |
| Energy storage | **Energy Cell**: 1,000,000 FE, takes power on every side, outputs from its front, keeps its charge when broken |
| Upgrades | **Speed**, **Efficiency**, **Capacity**, **Yield**; each machine has 3 upgrade slots, up to 4 per slot |

All energy is NeoForge FE, so Mekanism cables, Ender IO conduits, Powah cables and similar connect directly. Hoppers and pipes can insert into inputs and pull from outputs.

## Compatibility recipes

Only loaded when the mod is installed (and can be switched off in the config):

- **Mekanism**: Enrichment Chamber/Crusher handle galvanite; the GS Crusher makes Mekanism dusts from osmium, tin, lead and uranium.
- **Ender IO**: SAG Mill and Alloy Smelter handle galvanite/steel; the GS Alloy Smelter makes conductive alloy, redstone alloy, pulsating alloy and soularium.

## Daily rewards and missions

- `/daily` or `/daily claim` — claim today's reward
- `/daily status` — streak and time until the next reward
- `/daily missions` — today's missions in chat
- `/daily menu` — opens the Daily screen (players with GS Mod installed)
- `/daily reset <players>` — operators: clear daily data

The day resets at `resetHourUtc` (UTC), the same moment for every player. Everything is checked on the server and stored with the player, so it works in multiplayer and on dedicated servers. A claim is saved before the items are handed out, so a reward can never be paid twice.

Each player gets 3 random missions a day (weighted, the same all day). Missions for Mekanism, Ender IO, AE2, Apotheosis, LTX Industries, Powah! and Tinkers' Continuum only appear when that mod is installed.

With [Inventory Buttons](../InventoryButtons) installed, the inventory gets two buttons: claim reward and open the Daily screen.

### Datapacks

Reward tables: `data/<namespace>/daily_rewards/<name>.json`. The highest `day` is the cycle length.

```json
{ "day": 4,
  "items": [ { "id": "gsmod:steel_ingot", "count": 6 } ],
  "random": [ { "weight": 2, "item": { "id": "minecraft:emerald", "count": 2 } } ],
  "rolls": 1,
  "experience": 50 }
```

Missions: `data/<namespace>/daily_missions/<name>.json`. Objectives: `mine_block`, `craft_item`, `produce_item` (GS machine output), `use_machine`, `generate_energy` (GS generators), `play_time` (seconds).

```json
{ "objective": "mine_block", "target": "#c:ores/iron", "amount": 32, "weight": 10,
  "title": "Mine 32 Iron Ore",
  "rewards": [ { "id": "minecraft:iron_ingot", "count": 8 } ], "experience": 60,
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ] }
```

Machine recipes use `gsmod:crushing` and `gsmod:alloying` (see `CrushingRecipe` / `AlloyingRecipe` for the format); `processing_time` and `energy_per_tick` can be set per recipe.

## Configuration

`config/gsmod-common.toml` (or Mods → GS Mod → Config): energy use and production, capacities, upgrade strength, recipe groups, and daily reward/mission settings.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runGameTestServer` runs the GameTests. `dev_runtime_mods` in `gradle.properties` lists extra mod jars to load in dev runs.
