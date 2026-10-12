# Artifacts — Kingdoms 1.7.1

Eight artifacts replace the previous catalogue. They work on a Fabric 26.2 server with ordinary clients. In-game names and short lore use small caps.

## Catalogue

| Artifact | Rarity / base item | Ability | Netherite upgrade |
|---|---|---|---|
| Thunder Axe | Legendary / Diamond Axe | Real lightning on every fully charged critical melee hit; 512 vanilla durability | 768 durability; thunderstorms consume no durability |
| Thorns Shield | Mythic / Shield | Reflects 100% of the damage actually blocked, using ordinary Thorns damage | Retains its appearance and reflection strength |
| Frostbite Sword | Arcane / Diamond Sword | Each successful hostile hit adds one second of real freezing | Adds 1.5 seconds per hit |
| Multi Pickaxe | Legendary / Diamond Pickaxe | Mines a 2×2 square viewed from the mined face; the original block is top-left | Mines a centered 3×3 square |
| Vampire Sword | Relic / Diamond Sword | Heals the wielder by 25% of actual damage dealt, including absorption; overkill does not count | Heals 50% |
| Void Scythe | Mythic / Diamond Hoe | Each successful hostile hit disables held-item actions for two seconds; 10 vanilla durability | Netherite appearance; same ability and durability |
| Guardian Angel | Legendary / Heart of the Sea | Right-click for three seconds without cobweb slowing; 100 charges | No Netherite upgrade |
| Wind Burst Spear | Relic / Diamond Spear | Wind Burst II against living targets only, with Minecraft's falling-hit activation rules | Netherite Spear with the same target rules and Wind Burst II |

All eligible triggers succeed by default; the former 50% activation rule is removed. Failed or protected hits do not trigger combat powers. Kingdom members, allies, pets and farm animals are excluded from hostile-hit powers. Kingdom protection still applies to lightning damage and terrain/fire changes.

Void disables attacking, mining, item use and entity/block interaction, and stops an already-raised shield or charging item. Movement and inventory access remain available. Previously fired projectiles are not held-item actions.

Wind Burst Spear attacks only living players and mobs. Armor stands, mannequins, boats, minecarts, item frames, paintings, crystals, dropped items, projectiles, displays and other nonliving entities are ignored. Jabs and charged attacks both follow this rule, including offhand attacks and the Netherite upgrade. Rejected targets take no damage or knockback, trigger no Wind Burst and consume no hit durability. A living mob behind an ignored prop can still be hit; normal kingdom and PvP protection applies. Ordinary spears retain Minecraft's normal targeting rules.

Multi Pickaxe runs Minecraft's normal player mining separately for each extra block. It respects claims, normal loot tables, tool requirements, Silk Touch, Fortune and durability. Bedrock, blocks with negative hardness and game-master blocks are skipped. Protected blocks generate no loot and consume no durability. Squares on horizontal faces follow the player's heading.

Guardian Angel uses the same approach as CustomGear's Webmaster Boots: nearby real cobwebs are shown as harmless glow lichen only to the affected player, while the server also suppresses their cobweb slowing. The world blocks remain intact, updates/chunk resends retain the mask, and expiry restores the real blocks. This is possible in server-only Fabric. Exact visuals and third-party movement/anticheat compatibility still need testing on your live modpack.

## Durability, repair and enchantments

Tools, weapons and shields use **Minecraft durability**, including the visible durability bar and normal breaking. Vanilla actions can cost more than one durability point. Mending, Unbreaking, enchanting tables, compatible anvil books, ordinary repair materials and grindstones work normally. Wind Burst II is the spear's built-in effect: grinding removes its other non-curse enchantments but preserves Wind Burst without granting repeatable XP for it.

Guardian Angel uses custom charges instead. Its final charge still grants the full effect. Holding right-click while the effect is active does not spend additional charges.

A genuine Artifact Fixer in the right slot of a real anvil restores 25% of maximum durability, or Guardian Angel charges, rounded up and capped at the maximum. The default cost is five experience levels. Keep an empty inventory slot for the saved output. Fixers cannot repair ordinary items.

## Upgrading

Hold an artifact and open **Artifacts** in `/kingdom`, or `/artifact`. **Upgrade to Netherite** consumes three Netherite Ingots by default. It preserves the unique identity, compatible enchantments and remaining durability percentage. Each artifact can upgrade once; Guardian Angel has no upgrade button. Shields retain their vanilla appearance.

## Finding artifacts

Every new AirDrop independently rolls for an artifact before ordinary loot. The crate variant does not multiply these odds. Success produces one artifact, no ordinary loot or cash, and a 50% chance of exactly two optional fixers. Capture, winners-only chest and 30-minute expiry rules remain unchanged. There are no quantity caps.

| Real-world loot age | Artifact chance per crate |
|---|---:|
| Days 1–2 | 0.10% |
| Days 3–4 | 0.15% |
| Days 5–6 | 0.20% |
| Day 7 onward | 0.30% |

## Configuration

Edit `artifacts` in `config/kingdoms/config.json`, then `/kingdomadmin reload`. Probabilities are fractions (`0.001` = 0.10%). Generated identities keep their saved ability stats. Keep internal `abilityBalanceVersion` at `2`.

| Setting | Default / meaning |
|---|---|
| `enabled`, `announcements` | `true`; enable artifact abilities/rolls and crate announcements |
| `chanceByDay` | `{"1":0.001,"3":0.0015,"5":0.002,"7":0.003}`; per-crate artifact probability |
| `fixerChance`, `fixerCount` | `0.5`, `2`; optional fixer chance/quantity |
| `fixerItem` | `minecraft:echo_shard`; fixer appearance |
| `repairFraction`, `repairLevels` | `0.25`, `5`; fixer repair amount and XP price |
| `upgradeIngots` | `3`; Netherite upgrade price |
| `rebindOnTrade` | `true`; update the tracked holder on legitimate trades |
| `types.<type>.enabled`, `weight`, `minDay` | Enable a type, relative selection weight and unlock day |
| `name`, `baseItem`, `rarity` | Identity presentation; use a damageable base for tools and Heart of the Sea for Guardian Angel |
| `maxUses` | Native maximum durability; `0` uses the base item's default. Guardian Angel instead uses this as its charge capacity |
| `upgradedMaxUses` | Upgraded native maximum; `0` preserves the type's normal upgrade behavior |
| `netheriteUpgradeable` | Whether the menu offers a Netherite upgrade |
| `cooldownSeconds` | Optional ability cooldown; default `0` |
| `upgradedValue`, `upgradedDurationSeconds` | Upgraded reflection/healing fraction and timed-effect duration |
| `variants[].weight`, `chance` | Relative generated-stat variant weight and activation probability, where applicable; default chance `1` |
| `variants[].value` | Healing/reflection fraction |
| `variants[].durationSeconds` | Timed-effect seconds; supports fractions such as `1.5` |
| `variants[].amplifier` | Built-in Wind Burst level, default `2` for the spear |
| `variants[].rarity` | Generated rarity |

Type IDs: `thunder_axe`, `thorns_shield`, `frostbite_sword`, `multi_pickaxe`, `vampire_sword`, `void_scythe`, `guardian_angel`, `wind_burst_spear`.

## Admin commands

```text
/kingdomadmin artifact give thunder_axe
/kingdomadmin artifact give guardian_angel
/kingdomadmin artifact fixer
/kingdomadmin artifact inspect
/kingdomadmin artifact status
/kingdomadmin artifact retire <id>
/kingdomadmin artifact recover
/kingdomadmin artifact chance 7 0.3%
/kingdomadmin artifact set upgradeIngots 3
/kingdomadmin artifact type thunder_axe maxUses 512
/kingdomadmin artifact type thunder_axe upgradedMaxUses 768
/kingdomadmin artifact variant frostbite_sword 1 durationSeconds 1
```

`set` edits scalar global settings, `type` edits a type and `variant` edits a numbered variant starting at one. Collections can be edited in JSON. To guarantee an artifact crate during testing, temporarily set the current day's artifact chance to `100%`, spawn a normal test AirDrop, then restore the original probability.

## Existing-server migration

The old catalogue is replaced once when loading its configuration. Overall AirDrop artifact odds and fixer settings remain intact. Removed identities are retired and lose their abilities when encountered. Shared old types adopt the new mechanics, preserving their IDs, owners and upgrades; old remaining-use percentages become native durability on their item stacks. Current-version administrator changes survive reloads. Migration also covers records inside pending transactions. Kingdom/world/AirDrop files are not deleted.
