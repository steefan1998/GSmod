# GS Mod

Two separate NeoForge **26.1.2** mods, built from this repository. Each has its own jar and works without the other.

| Mod | Mod id | Jar | What it adds |
|---|---|---|---|
| **GS Mod: Tech** | `gsmod` | `GSMod-Tech/build/libs/GSMod-Tech-<version>.jar` | machines, galvanite and steel, upgrades, energy cell, compat recipes |
| **GS Mod: Daily Rewards** | `gsmod_daily` | `GSMod-DailyRewards/build/libs/GSMod-DailyRewards-<version>.jar` | daily login rewards, daily missions, the Daily screen |

Both go in the `mods` folder of the client **and** the server. With both installed, Tech reports machine use, production and generated FE to Daily Rewards, so the machine missions count.

## GS Mod: Tech

| | |
|---|---|
| Materials | **Galvanite** (ore + deepslate ore, raw, dust, ingot, block; generates between Y -48 and 48) and **Steel** (dust, ingot, block) |
| Machines | **Combustion Generator** (any furnace fuel → FE), **Crusher**, **Alloy Smelter** (two inputs) |
| Energy storage | **Energy Cell**: 1,000,000 FE, takes power on every side, outputs from its front, keeps its charge when broken |
| Upgrades | **Speed**, **Efficiency**, **Capacity**, **Yield**; each machine has 3 upgrade slots, up to 4 per slot |

All energy is NeoForge FE, so Mekanism cables, Ender IO conduits, Powah cables and similar connect directly. Hoppers and pipes can insert into inputs and pull from outputs.

### Compatibility recipes

Only loaded when the mod is installed (and can be switched off in the config):

- **Mekanism**: Enrichment Chamber/Crusher handle galvanite; the GS Crusher makes Mekanism dusts from osmium, tin, lead and uranium.
- **Ender IO**: SAG Mill and Alloy Smelter handle galvanite/steel; the GS Alloy Smelter makes conductive alloy, redstone alloy, pulsating alloy and soularium.

Machine recipes use `gsmod:crushing` and `gsmod:alloying` (see `CrushingRecipe` / `AlloyingRecipe` for the format); `processing_time` and `energy_per_tick` can be set per recipe.

### Configuration

`config/gsmod-common.toml` (or Mods → GS Mod: Tech → Config): energy use and production, capacities, upgrade strength and recipe groups.

## GS Mod: Daily Rewards

- `/daily` or `/daily claim` — claim today's reward
- `/daily status` — streak, next reward day and its tier, time until the reset
- `/daily missions` — today's missions in chat
- `/daily menu` — opens the Daily screen (players with the mod installed)
- `/daily reset <players>` — operators: clear daily data

Everything is checked on the server and stored with the player, so it works in multiplayer and on dedicated servers. A claim is saved before the items are handed out, so a reward can never be paid twice.

**Reset time:** one reset moment for the whole server, at `resetHour` in `timeZone` (default `UTC`, e.g. `Europe/Amsterdam`). Summer and winter time are handled automatically: the reset stays on the same local hour, so the day it changes is 23 or 25 hours long.

**Rewards** run in a 10-day cycle with random picks and random stack sizes. Day 5 is the **iron tier**, day 7 the **diamond tier** and day 10 the **netherite tier** (mostly netherite scrap or ancient debris; a netherite ingot is a rare roll). Each day also has a small chance for modded items when the mod is installed: GS Mod: Tech upgrades, Mekanism upgrades and tier installers (up to elite), Oritech machine addons, AE2 cards, Ender IO capacitors and Powah! parts.

On top of the day's reward:

- **Week bonus** every 7 days in a row (`weekBonusEvery`): experience plus two picks such as golden apples, a name tag, diamonds or, rarely, a Mending book or a Totem of Undying.
- **Comeback reward** when a streak of at least 3 days is lost (`comebackMinStreak`): the streak starts again at day 1, with food, torches and a useful pick to get going again.

**Missions:** each player gets 3 missions a day, picked by weight and the same all day. A mission can be limited to certain days of the reward cycle (`days`) or weekdays (`weekdays`), and a reward day can force missions (`missions`); by default day 5 always has "Mine 32 Iron Ore", day 10 "Produce 64 Steel Ingots", and weekends have "Play 60 minutes". Missions that need GS Mod: Tech, Mekanism, Ender IO, AE2, Apotheosis, LTX Industries, Powah! or Tinkers' Continuum only appear when that mod is installed.

With [Inventory Buttons](https://github.com/steefan1998/InventoryButtons) installed, the inventory gets two buttons: claim reward and open the Daily screen.

### Per world

Every world gets its own editable copy the first time it starts:

```
<world>/gsmod_daily/daily_rewards/day_01.json ... day_10.json
<world>/gsmod_daily/daily_rewards/bonus_week.json, bonus_comeback.json
<world>/gsmod_daily/daily_missions/*.json   (compat/<mod>/ for mod-specific missions)
<world>/gsmod_daily/README.txt
<world>/serverconfig/gsmod_daily-server.toml
```

Edit, add or delete files there; changes apply with `/reload` or `/daily debug reload`. Delete a folder to get the defaults back. `gsmod_daily-server.toml` holds the world's settings (rewards on/off, time zone and reset hour, streak grace days, week bonus, comeback reward, missions on/off, missions per day) and applies on the next world start. Put a `gsmod_daily-server.toml` in `defaultconfigs/` to use it for new worlds.

### Own rewards for every world

`config/gsmod_daily/custom_rewards/` holds your own reward tables (same format as below) and applies to every world. `config/gsmod_daily/custom_rewards.toml` sets `mode`: `MERGE` (default; a custom day or bonus replaces that day of the world, a higher day makes the cycle longer) or `REPLACE` (only the custom files count). `config/gsmod_daily/uitleg/` explains it all (Dutch `LEESMIJ.txt`, English `README_EN.txt`) with example files in `voorbeelden/`; it is recreated when deleted. Changes apply with `/reload` or `/daily debug reload`.

Reward day (a bonus table uses `"bonus": "week"` or `"bonus": "comeback"` instead of `"day"`):

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
  "missions": [ "gsmod_daily:mine_iron_ore" ] }
```

Mission (objectives: `mine_block`, `craft_item`, `play_time` (seconds), and `produce_item`, `use_machine`, `generate_energy`, which mods such as GS Mod: Tech report):

```json
{ "objective": "mine_block", "target": "#c:ores/iron", "amount": 32, "weight": 10,
  "days": [ 1, 2, 3, 4 ], "weekdays": [ "saturday", "sunday" ],
  "title": "Mine 32 Iron Ore",
  "rewards": [ { "id": "minecraft:iron_ingot", "count": 8 } ], "experience": 60,
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ] }
```

Mission ids are `gsmod_daily:<path in daily_missions without .json>`, e.g. `gsmod_daily:compat/mekanism/mine_osmium_ore`. For `use_machine` the target is the machine's block id, e.g. `gsmod:crusher`.

### Debug commands (operators)

| Command | |
|---|---|
| `/daily debug info [players]` | day, streak, cycle day, tier, reset time and zone, today's missions |
| `/daily debug setday <players> <day>` | the next claim gives that reward day; missions are re-rolled for it |
| `/daily debug unclaim <players>` | allow claiming again today, keeping the streak |
| `/daily debug preview <day\|week\|comeback>` | chances of every item on that day or bonus, plus an example roll |
| `/daily debug give <players> <day>` | hand out a reward day without changing streaks |
| `/daily debug missions list` | all loaded missions with their days/weekdays |
| `/daily debug missions reroll <players>` | new missions for today |
| `/daily debug missions complete <players> [n]` | complete mission n (or all) and pay the reward |
| `/daily debug missions progress <players> <n> <amount>` | add progress to mission n |
| `/daily debug reload` | re-read this world's `gsmod_daily` folder |

### For other mods

`nl.gsmod.daily.api.DailyMissionsApi` lets any mod feed the machine missions (`machineUsed`, `itemProduced`, `energyGenerated`). Only call it when `ModList.get().isLoaded("gsmod_daily")`; see `GSMod-Tech/src/main/java/nl/gsmod/compat/DailyHooks.java` for an example that keeps Daily Rewards optional.

## GS Mod: Stages

Mod id `gsmod_stages`, jar `GSMod-Stages/build/libs/GSMod-Stages-<version>.jar`. End-game mods stay locked until the Ender Dragon has been defeated in the world (worlds where it was already defeated open right away). After that every player unlocks the stage **once**, with the **★ Unlock end-game** button under the Daily screen (needs GS Mod: Daily Rewards) or `/stages unlock`; it stays unlocked for that player.

While locked, items and blocks from `lockedMods` (default: `projecte`, `projectexpansion`, `stellaris`, `draconicevolution`, `avaritia`, `extendedcrafting`) and `lockedItems` cannot be used, placed or opened, crafted locked items are taken away again, and with JEI installed they are hidden until the player unlocks. Creative and spectator players are not locked (`creativeBypass`).

- `/stages` — stage status
- `/stages unlock` — unlock it for yourself, once the dragon is defeated
- `/stages world open|close` — operators: open or close the stage for the world
- `/stages player <players> unlock|lock` — operators: change it per player

Settings in `<world>/serverconfig/gsmod_stages-server.toml`: `lockedMods`, `lockedItems` and `allowedItems` (item ids or `#tags`), `requirePlayerUnlock`, `creativeBypass`, `announce`.

## Languages

Both mods: English, Nederlands, Deutsch, Français, Español, Italiano, Português (Brasil), Polski, Русский and 简体中文.

## Building

```
./gradlew build
```

Builds both jars. Other useful tasks:

- `./gradlew :GSMod-Tech:runGameTestServer` runs the Tech GameTests (Daily Rewards is loaded too, so its tests run with Tech present)
- `./gradlew :GSMod-DailyRewards:runGameTestServer` runs the Daily Rewards GameTests on their own
- `./gradlew :GSMod-Tech:runDevCheck` starts a test client that loads `GSMod-Tech/run-devcheck/saves/world`, places the machines, opens the screens, presses the daily buttons and takes screenshots

`dev_runtime_mods` in `gradle.properties` lists extra mod jars (relative to this folder) to load in dev runs.
