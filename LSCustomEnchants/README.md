# LSCustomEnchants (Paper 1.21.11)

55 custom enchants stored on items (PDC) with lore lines. No datapacks / NMS.

## Build
JDK 21 + Maven:  mvn package   ->  target/LSCustomEnchants-1.0.0.jar  -> server /plugins

## Commands (permission ce.admin)
/ce list
/ce enchant <name> [level]     apply to held item
/ce remove <name>
/ce book <name> [level] [player]   enchant book; combine with an item in an anvil
/ce reload                     reloads config.yml (disabled list)

## Notes
- Vein Miner and Timber activate while SNEAKING.
- Effects are interpretations of each enchant name - tune the numbers in the *Listener classes.
- Hoe enchants (e.g. Replant) are intentionally not included.
- Disable any enchant via config.yml `disabled:`.
