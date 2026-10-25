# Artifacts — Kingdoms 1.11.0

Fourteen artifacts are available: thirteen can appear in AirDrops, and the Golden Crown is staff-command-only. They work on a Fabric 26.2 server with ordinary clients. In-game names and short lore use small caps.

## Catalogue

| Artifact | Rarity / base item | Ability | Netherite upgrade |
|---|---|---|---|
| Thunder Axe | Legendary / Diamond Axe | Real lightning on every fully charged critical melee hit; 512 vanilla durability | 768 durability; thunderstorms consume no durability |
| Thorns Shield | Mythic / Shield | Reflects 100% of the damage actually blocked, using ordinary Thorns damage | Retains its appearance and reflection strength |
| Frostbite Sword | Arcane / Diamond Sword | Each successful hostile hit adds one second of real freezing | Adds 1.5 seconds per hit |
| Multi Pickaxe | Legendary / Diamond Pickaxe | Mines a 2×2 square viewed from the mined face; the original block is top-left | Mines a centered 3×3 square |
| Vampire Sword | Relic / Diamond Sword | Heals the wielder by 25% of actual damage dealt, including absorption; overkill does not count | Heals 50% |
| Void Scythe | Mythic / Diamond Hoe | Each successful hostile hit disables held-item actions for two seconds; 1,000 vanilla durability | Netherite appearance; same ability and durability |
| Guardian Angel | Legendary / Heart of the Sea | Right-click for three seconds without cobweb slowing; 100 charges | No Netherite upgrade |
| Wind Burst Spear | Relic / Diamond Spear | Wind Burst II against living targets only, with Minecraft's falling-hit activation rules | Netherite Spear with the same target rules and Wind Burst II |
| Runner's Boots | Legendary / Diamond Boots | Speed I while worn; stronger potions take priority | Netherite Boots, same effect |
| King's Chestplate | Legendary / Diamond Chestplate | Regeneration I while worn; stronger potions take priority | Netherite Chestplate, same effect |
| Archer's Bow | Legendary / Bow | Fires two arrows per shot using one ammo; bonus arrow cannot be collected | No Netherite upgrade |
| Advanced Axe | Legendary / Diamond Axe | Instantly fells connected tree logs, with normal drops/tool wear | Netherite Axe, same ability |
| Advanced Pickaxe | Legendary / Diamond Pickaxe | Instantly mines connected matching ore veins, including deepslate equivalents | Netherite Pickaxe, same ability |
| Golden Crown | Mythic / Diamond Helmet with gold appearance | Strength I while worn; staff-command-only event reward | Netherite Helmet stats for three ingots; keeps gold appearance |

All eligible triggers succeed by default; the former 50% activation rule is removed. Failed or protected hits do not trigger combat powers. Kingdom members, allies, pets and farm animals are excluded from hostile-hit powers. Kingdom protection still applies to lightning damage and terrain/fire changes.

Void disables attacking (including spear stabs/contact), mining, item use and entity/block interaction, and stops an already-raised shield or charging item. Fully shield-blocked Scythe hits also trigger it. Native cooldown overlays make the lock visible on held items; switching held items applies the remaining cooldown without shortening a longer existing cooldown. Movement and inventory access remain available. Previously fired projectiles are not held-item actions.

Wind Burst Spear attacks only living players and mobs. Armor stands, mannequins, boats, minecarts, item frames, paintings, crystals, dropped items, projectiles, displays and other nonliving entities are ignored. Jabs and charged attacks both follow this rule, including offhand attacks and the Netherite upgrade. Rejected targets take no damage or knockback, trigger no Wind Burst and consume no hit durability. A living mob behind an ignored prop can still be hit; normal kingdom and PvP protection applies. Ordinary spears retain Minecraft's normal targeting rules.

Multi Pickaxe runs Minecraft's normal player mining separately for each extra block. It respects claims, normal loot tables, tool requirements, Silk Touch, Fortune and durability. Bedrock, blocks with negative hardness and game-master blocks are skipped. Protected blocks generate no loot and consume no durability. Squares on horizontal faces follow the player's heading.

Guardian Angel uses the same approach as CustomGear's Webmaster Boots: nearby real cobwebs are shown as harmless glow lichen only to the affected player, while the server also suppresses their cobweb slowing. The world blocks remain intact, updates/chunk resends retain the mask, and expiry restores the real blocks. This is possible in server-only Fabric. Exact visuals and third-party movement/anticheat compatibility still need testing on your live modpack.

## Worn armor, bow and advanced tools

Runner's Boots and King's Chestplate grant the actual Level I effects only in their armor slots. Golden Crown grants Strength I in the helmet slot. Stronger potions take priority without being shortened, and removing armor does not remove legitimate potion effects. The chestplate heals at Minecraft's normal Regeneration I cadence.

Golden Crown uses Minecraft's existing gold-helmet item model and worn texture, with Diamond Helmet stats, durability and compatible helmet enchantments. Its Netherite upgrade keeps the gold appearance, enchantments and wear percentage, and gains native Netherite Helmet stats. No resource pack or client mod is needed. It never appears in AirDrops: staff issue it in-game with `/kingdomadmin artifact give golden_crown` and award it for their events. Staff using the Combat addon must enable staff mode to regain their admin permissions.

Archer's Bow uses native draw strength, arrow types, enchantments and damage rules. A shot consumes one ammunition item unless Infinity/creative rules prevent consumption; the extra arrow is uncollectable. Each projectile uses native bow durability, normally two durability per shot. Ordinary bows are unchanged.

Advanced tools mine the original block instantly and break connected matching logs/ores through normal player mining. Tree leaves decay normally. By default, trees need nearby leaves; the limits are 256 logs or 64 ore blocks, including the original, within 16 blocks of the start. Scans use only loaded chunks, respect protected claims, cores and AirDrops, and stop when the tool breaks. Silk Touch, Fortune, XP and tool costs follow vanilla rules. Placed logs/ores are excluded from chaining by default; placement tracking begins with this update, so it cannot identify constructions placed before installation. Administrators can adjust these rules.

## Durability, repair and enchantments

Armor, tools, weapons and shields use **Minecraft durability**, including the visible durability bar and normal breaking. Vanilla actions can cost more than one durability point. Mending, Unbreaking, enchanting tables, compatible anvil books, ordinary repair materials and grindstones work normally. Wind Burst II is the spear's built-in effect: grinding removes its other non-curse enchantments but preserves Wind Burst without granting repeatable XP for it.

Combining an artifact with an ordinary matching item preserves its ability and identity in a crafting grid, anvil or grindstone, in either input order. Ordinary repair costs and enchantment-removal rules still apply; the spear always retains its built-in Wind Burst. Combining **two artifacts** is blocked in all three places so neither artifact is consumed or loses its power.

Void Scythe now defaults to **1,000 durability**, including its Netherite upgrade. On the first update from older versions, the old default of 10 is changed to 1,000 in configuration and saved artifact identities. Existing item stacks retain their wear percentage: an old scythe with 3 of 10 durability remaining becomes 300 of 1,000. Other custom durability values are preserved; later administrator changes remain configurable.

Guardian Angel uses custom charges instead. Its final charge still grants the full effect. Holding right-click while the effect is active does not spend additional charges.

A genuine Artifact Fixer in the right slot of a real anvil restores 25% of maximum durability, or Guardian Angel charges, rounded up and capped at the maximum. The default cost is five experience levels. Keep an empty inventory slot for the saved output. Fixers cannot repair ordinary items.

## Upgrading

Hold an artifact and open **Artifacts** in `/kingdom`, or `/artifact`. **Upgrade to Netherite** consumes three Netherite Ingots by default. It preserves the unique identity, compatible enchantments and remaining durability percentage. Each artifact can upgrade once; Guardian Angel and Archer's Bow have no upgrade button. Shields retain their vanilla appearance.

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
| `advancedTreeMaxBlocks`, `advancedOreMaxBlocks` | `256`, `64`; maximum blocks per harvest, including the original; 1..4096 |
| `advancedChainRadius` | `16`; maximum axis distance from the first block; 1..64 |
| `advancedTreeRequireLeaves` | `true`; require nearby leaves before felling connected logs |
| `advancedProtectPlacedBlocks` | `true`; exclude tracked player-placed logs/ores from instant mining and chaining |
| `types.<type>.airDropEligible` | `true` for ordinary artifacts; `false` and locked out for Golden Crown |
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

Type IDs: `thunder_axe`, `thorns_shield`, `frostbite_sword`, `multi_pickaxe`, `vampire_sword`, `void_scythe`, `guardian_angel`, `wind_burst_spear`, `runners_boots`, `kings_chestplate`, `archers_bow`, `advanced_axe`, `advanced_pickaxe`, `golden_crown`.

## Admin commands

```text
/kingdomadmin artifact give thunder_axe
/kingdomadmin artifact give guardian_angel
/kingdomadmin artifact give golden_crown
/kingdomadmin artifact give runners_boots
/kingdomadmin artifact give advanced_pickaxe
/kingdomadmin artifact set advancedOreMaxBlocks 64
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

## Expanded catalogue (1.11.0)

Six types are added without replacing the previous eight or changing overall artifact/fixer chances. `expandedCatalogVersion` records the one-time addition; keep it at `1`. Existing type customizations and identities remain intact. The Golden Crown is excluded from automatic artifact rolls even if it is the only enabled type; its `airDropEligible` setting must stay false. Staff command issuance remains available. Runner's Boots, King's Chestplate, Archer's Bow, Advanced Axe and Advanced Pickaxe are enabled for AirDrops by default, each with Legendary rarity and weight 35. The crown has Mythic rarity. Archer's Bow has no Netherite upgrade; native diamond armor/tools and the crown can use the existing three-ingot upgrade menu.

Void Scythe (1.11.0) now handles fully blocked shield hits and all ordinary/native spear item-action paths. Existing valid Scythes receive the behavior change without reissuing. If an old item already lost its authenticated identity before the repair fix, staff must issue a replacement with `/kingdomadmin artifact give void_scythe`; names/lore alone are not proof of identity.
