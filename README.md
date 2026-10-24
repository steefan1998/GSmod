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

Machine recipes use `gsmod:crushing` and `gsmod:alloying` (see `CrushingRecipe` / `AlloyingRecipe` for the format); `processing_time` and `energy_per_tick` can be set per recipe.

## Daily rewards and missions

- `/daily` or `/daily claim` — claim today's reward
- `/daily status` — streak, next reward day and its tier, time until the reset
- `/daily missions` — today's missions in chat
- `/daily menu` — opens the Daily screen (players with GS Mod installed)
- `/daily reset <players>` — operators: clear daily data

The day resets at `resetHourUtc` (UTC), the same moment for every player. Everything is checked on the server and stored with the player, so it works in multiplayer and on dedicated servers. A claim is saved before the items are handed out, so a reward can never be paid twice.

**Rewards** run in a 10-day cycle with random picks and random stack sizes. Day 5 is the **iron tier**, day 7 the **diamond tier** and day 10 the **netherite tier** (mostly netherite scrap or ancient debris; a netherite ingot is a rare roll). Each day also has a small chance for modded items when the mod is installed: Mekanism upgrades and tier installers (up to elite), Oritech machine addons, AE2 cards, Ender IO capacitors and Powah! parts.

**Missions:** each player gets 3 missions a day, picked by weight and the same all day. A mission can be limited to certain days of the reward cycle (`days`) or weekdays (`weekdays`), and a reward day can force missions (`missions`); by default day 5 always has "Mine 32 Iron Ore", day 10 "Produce 64 Steel Ingots", and weekends have "Play 60 minutes". Missions for Mekanism, Ender IO, AE2, Apotheosis, LTX Industries, Powah! and Tinkers' Continuum only appear when that mod is installed.

With [Inventory Buttons](https://github.com/steefan1998/InventoryButtons) installed, the inventory gets two buttons: claim reward and open the Daily screen.

### Per world

Every world gets its own editable copy the first time it starts:

```
<world>/gsmod/daily_rewards/day_01.json ... day_10.json
<world>/gsmod/daily_missions/*.json   (compat/<mod>/ for mod-specific missions)
<world>/gsmod/README.txt
<world>/serverconfig/gsmod-server.toml
```

Edit, add or delete files there; changes apply with `/reload` or `/daily debug reload`. Delete a folder to get the defaults back. `gsmod-server.toml` holds the world's daily settings (rewards on/off, reset hour, streak grace days, missions on/off, missions per day) and applies on the next world start. Put a `gsmod-server.toml` in `defaultconfigs/` to use it for new worlds.

Reward day:

```json
{ "day": 5,
  "tier": "iron",
  "items": [ { "id": "minecraft:iron_ingot", "count": 4 } ],
  "random": [
    { "weight": 4, "item": { "id": "minecraft:iron_ingot" }, "min": 4, "max": 8 },
    { "weight": 1, "item": { "id": "mekanism:upgrade_speed" },
      "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ] }
  ],
  "rolls": 2,
  "experience": 50,
  "missions": [ "gsmod:mine_iron_ore" ] }
```

Mission (objectives: `mine_block`, `craft_item`, `produce_item` (GS machine output), `use_machine`, `generate_energy` (GS generators), `play_time` (seconds)):

```json
{ "objective": "mine_block", "target": "#c:ores/iron", "amount": 32, "weight": 10,
  "days": [ 1, 2, 3, 4 ], "weekdays": [ "saturday", "sunday" ],
  "title": "Mine 32 Iron Ore",
  "rewards": [ { "id": "minecraft:iron_ingot", "count": 8 } ], "experience": 60,
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ] }
```

Mission ids are `gsmod:<path in daily_missions without .json>`, e.g. `gsmod:compat/mekanism/mine_osmium_ore`.

### Debug commands (operators)

| Command | |
|---|---|
| `/daily debug info [players]` | day, streak, cycle day, tier, reset time and today's missions |
| `/daily debug setday <players> <day>` | the next claim gives that reward day; missions are re-rolled for it |
| `/daily debug unclaim <players>` | allow claiming again today, keeping the streak |
| `/daily debug preview <day>` | chances of every item on that day, plus an example roll |
| `/daily debug give <players> <day>` | hand out a reward day without changing streaks |
| `/daily debug missions list` | all loaded missions with their days/weekdays |
| `/daily debug missions reroll <players>` | new missions for today |
| `/daily debug missions complete <players> [n]` | complete mission n (or all) and pay the reward |
| `/daily debug missions progress <players> <n> <amount>` | add progress to mission n |
| `/daily debug reload` | re-read this world's `gsmod` folder |

## Languages

English, Nederlands, Deutsch, Français, Español, Italiano, Português (Brasil), Polski, Русский and 简体中文.

## Configuration

`config/gsmod-common.toml` (or Mods → GS Mod → Config): energy use and production, capacities, upgrade strength and recipe groups. Daily reward and mission settings are per world (see above).

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runGameTestServer` runs the GameTests. `dev_runtime_mods` in `gradle.properties` lists extra mod jars to load in dev runs.
