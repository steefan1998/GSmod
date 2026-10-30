GS Mod: Daily Rewards - making your own daily rewards
=====================================================

Files in config/gsmod_daily/custom_rewards/ are used in EVERY world (and on your server).

  config/gsmod_daily/
      custom_rewards.toml     settings (on/off, MERGE or REPLACE)
      custom_rewards/         put your own .json files HERE
      uitleg/                 this explanation + examples (recreated when deleted)
          voorbeelden/        ready-made examples to copy

Quick start: copy uitleg/voorbeelden/day_01_simpel.json to custom_rewards/, edit it, run
/reload, and check it with /daily debug preview 1. File names may only use a-z, 0-9, _ and -.

MERGE (default): your day replaces the same day of the world; every other day stays. A day
higher than the world's last day makes the cycle longer.
REPLACE: only your files count, including the week and comeback bonus.

File format: "day" (or "bonus": "week" / "comeback"), "tier", "items" (always given),
"random" (picks by "weight", "min"/"max" for a random count), "rolls", "experience" (points),
"missions". Items take "components" for names, lore and enchantments, and
"neoforge:conditions" to only count while a mod is installed. See LEESMIJ.txt sections 3-5 and
the files in voorbeelden/.

Problems: look in logs/latest.log for lines with "gsmod_daily" or "Skipping".
