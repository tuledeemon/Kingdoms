# Artifacts

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
| Golden Crown | Mythic / Diamond Helmet with gold appearance | Private locator bar while worn; staff-command-only event reward | Netherite Helmet stats for three ingots; keeps gold appearance |

All eligible triggers succeed by default; the former 50% activation rule is removed. Failed or protected hits do not trigger combat powers. Kingdom members, allies, pets and farm animals are excluded from hostile-hit powers. Kingdom protection still applies to lightning damage and terrain/fire changes.

Void disables attacking (including spear stabs/contact), mining, item use and entity/block interaction, and stops an already-raised shield or charging item. Fully shield-blocked Scythe hits also trigger it. Native cooldown overlays make the lock visible on held items; switching held items applies the remaining cooldown without shortening a longer existing cooldown. Movement and inventory access remain available. Previously fired projectiles are not held-item actions.

Wind Burst Spear attacks only living players and mobs. Armor stands, mannequins, boats, minecarts, item frames, paintings, crystals, dropped items, projectiles, displays and other nonliving entities are ignored. Jabs and charged attacks both follow this rule, including offhand attacks and the Netherite upgrade. Rejected targets take no damage or knockback, trigger no Wind Burst and consume no hit durability. A living mob behind an ignored prop can still be hit; normal kingdom and PvP protection applies. Ordinary spears retain Minecraft's normal targeting rules.

Multi Pickaxe runs Minecraft's normal player mining separately for each extra block. It respects claims, normal loot tables, tool requirements, Silk Touch, Fortune and durability. Bedrock, blocks with negative hardness and game-master blocks are skipped. Protected blocks generate no loot and consume no durability. Squares on horizontal faces follow the player's heading.

Guardian Angel uses the same approach as CustomGear's Webmaster Boots: nearby real cobwebs are shown as harmless glow lichen only to the affected player, while the server also suppresses their cobweb slowing. The world blocks remain intact, updates/chunk resends retain the mask, and expiry restores the real blocks. This is possible in server-only Fabric. Exact visuals and third-party movement/anticheat compatibility still need testing on your live modpack.

## Worn armor, bow and advanced tools

Runner's Boots and King's Chestplate grant the actual Level I effects only in their armor slots. Golden Crown grants a private locator bar in the helmet slot. Stronger potions take priority without being shortened, and removing armor does not remove legitimate potion effects. The chestplate heals at Minecraft's normal Regeneration I cadence.

Golden Crown enables a private locator bar for its wearer while the normal server locator bar is off. It shows visible, living players in the same dimension, respects sneaking, invisibility and waypoint transmission hiding, and uses direction-only markers for distant players. Removing the crown, dying, entering spectator mode or disabling the artifact clears its markers; logout and dimension changes also clean up. It does not grant Strength or remove legitimate Strength potions. The global locator gamerule and other players' HUDs stay unchanged; if the normal bar is already enabled globally, its normal behavior remains. This uses vanilla waypoint packets and requires no client mod or PacketEvents dependency. Vanilla experience and riding displays can temporarily take priority over the locator bar.

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


## More information

[Configuration and admin commands](Artifact-Administration).
