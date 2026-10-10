![Stylized creeper with a red, circled strikethrough above it, indicating it's demise. Minecraft-font text next to it reading "ScrewYourMobs!"](https://cdn.modrinth.com/data/cached_images/79a16fae7717b10b79cacb2b0ee0073707fec0be.png)

Previously known as "Screw_SubAnomaly1".

Automatically despawns entities the moment they spawn! Based on JSON rules you write yourself.

Originally built to remove TheBrokenScript's SubAnomaly1, now a general-purpose entity filter!

## Features

**Rule-based removal.** Each rule is a JSON file listing entities and the dimensions it applies to. Entities are discarded the instant they join the world.

**Entities or tags.** List specific entities (`minecraft:creeper`), entity tags (`#minecraft:raiders`), or both in the same rule.

**Per-dimension or global.** Leave `dimensions` empty and the rule applies everywhere. Fill it in and it only fires in those dimensions.

**Drop-in rule files.** Rules live in `config/sym/rules/` (global) or `<world>/serverconfig/sym_rules/` (per-world). Edit the JSON, run `/sym reload`, done. No restart.

**In-game rule editor.** `/sym`, `/symrules` open a GUI. Add, remove, rename, enable, disable, and validate rules without touching the filesystem.

**Live validation.** On load, entity IDs, tags, and dimensions are checked against the registry. Unknown entries get flagged with a "did you mean…?" suggestion based on edit distance (Levenshtein). Missing mod namespaces are reported separately.

**Keep list.** Exempt specific entities from removal entirely, regardless of what any rule says. Also skips named mobs and tamed animals during bulk purge.

**Server-side sync.** On multiplayer, rules are synced from server to client so the editor shows the real state. Edits are permission-gated (op level 2) and round-trip through the server.

**Debug channels.** Optional logging for removals, rule errors, validation, and reloads. Output goes to console, chat, and/or `config/sym/debug.log`, each toggled independently.

![Image showing the in-game rules editor](https://cdn.modrinth.com/data/cached_images/bdb4fbae1fe64d0e0ddec09420835d763fca6082.jpeg)

## Commands

All require op level 2.

| Command | What it does |
|---|---|
| `/sym` | Open the rule editor (singleplayer / LAN only) |
| `/symrules` | Client-side alias for the editor (used for dedicated servers) |
| `/sym list` | List loaded rules |
| `/sym reload` | Re-read rule files and re-sync to clients |
| `/sym stats` | Rule and entity counts |
| `/sym validate` | Show validation warnings |
| `/sym debug` | Show or set debug channel state |

## Requirements

- **NeoForge** for Minecraft 1.21.1, at least version 21.1.100

## Example rule (also available in `config/sym/rules/_example.json`)

```json
{
  "_comment": "Copy this file and rename it. Delete the _comment fields if you want to.",
  "_comment2": "Empty 'dimensions' list means the rule applies in ALL dimensions.",
  "_comment3": "Tags like '#minecraft:raiders' are also supported.",
  "entities": [
    "minecraft:creeper",
    "minecraft:skeleton",
    "#minecraft:raiders"
  ],
  "dimensions": [
    "minecraft:overworld"
  ]
}

```

An empty `dimensions` list means the rule applies in every dimension. Rename the file to start with `_` to disable a rule.

# Download now!: https://modrinth.com/mod/screwyourmobs
