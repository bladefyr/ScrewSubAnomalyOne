# ScrewYourMobs

Codebase for the Minecraft Mod "ScrewYourMobs". Link: https://modrinth.com/mod/screwyourmobs

## Rule Files

Rules are JSON files stored in one of two folders:

| Location | Scope |
|---|---|
| `config/sym/rules/` | Global, applies to every world |
| `<world>/serverconfig/sym_rules/` | Per-world, applies only to that world |

Rules with the same filename in both locations: the per-world version wins.

### Format

{
  "entities": [
    "minecraft:creeper",
    "#minecraft:raiders"
  ],
  "dimensions": [
    "minecraft:overworld"
  ]
}

- `entities` — list of entity IDs (`namespace:id`) and/or entity tags (`#namespace:tag`). At least one required.
- `dimensions` — list of dimension IDs (`namespace:dimension`). Empty or omitted means "all dimensions."

Filenames starting with `_` are ignored by the loader, useful for templates or disabled rules.

It is recommended to just use the '/sym' or '/symrules' commands instead.
