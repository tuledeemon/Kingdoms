# AirDrops — Kingdoms 1.7.0

Recurring supply crates create PvP objectives for kingdoms and solo players. This is part of the server-only Fabric mod for Minecraft 26.2. Players need no client mod or resource pack.

## Players: finding and winning a crate

Open **AirDrops** in `/kingdom`, or use `/airdrop`. Each entry shows its coordinates, crate variant, loot tier and status.

- Events normally begin every **3 hours plus a random 0–20 minutes**. At least **10 players** must be online: 10–19 produces one crate, 20+ produces two. If too few players are online or no safe sites are found, the server retries later without announcing a nonexistent event.
- Coordinates are announced when the event actually starts. A glowing barrel descends over **10 minutes**. It cannot be captured or opened during descent.
- After landing, control the area within **15 blocks horizontally and 3 blocks vertically**. Living survival/adventure players count. Spectators, creative players and flying players do not. Ground-only contribution is an optional admin setting, disabled by default.
- A kingdom needs **3 members** for **90 seconds**. An unaffiliated player needs **135 seconds** alone. Only **one side** owns capture progress. A stronger qualifying side must keep its advantage for **15 continuous seconds** before taking over from zero. Capture pauses during this takeover countdown; losing the advantage resets the challenge. Ties preserve the holder. Each eligible kingdom member contributes one control strength; an unaffiliated solo player contributes **three**. Four kingdom members can take over from a solo player; three cannot break the tie. Separate solo players remain separate sides and never combine their strength.
- Any rival side inside the radius slows progress by **1.3×**, regardless of rival count. A kingdom with 90 seconds of uninterrupted, fully contested capture takes 117 seconds. A solo player takes 175.5 seconds under continuous interference.
- Losing the required presence pauses progress. Restore it within **15 seconds** to continue. Otherwise progress resets. Disconnecting, dying, teleporting out and membership changes use the same eligibility rules. A membership change does not transfer progress to a new side.
- A nearby boss bar shows arrival time, your side's progress, interference and grace pauses. Chat, titles and sounds announce important changes.
- A configurable **5-block horizontal build-protection radius** stops placing, breaking and destructive environmental changes during descent/capture, including towers above the zone. Building remains allowed outside it. After capture, the chest and its support stay protected until cleanup. OPs obey event protection. Normal PvP and claim rules still apply.
- A **dust circle** shows the configured capture radius from descent until cleanup. It uses the current controlling kingdom's exact core color, black for solo control, and gold while neutral. The descending crate has twelve glowing vanilla display edges and a parachute built from vanilla block displays, including a rounded striped fabric canopy, lining, stitched seams and eight continuous suspension lines. The complete assembly sways gently, settles onto the landing coordinates and disappears on landing.
- Eligible players inside the capture area glow, including rivals; winners also glow while looting the chest. Glow ends when they leave the area/menu, die, disconnect or the event closes. Vanilla player outlines support sixteen colors, so kingdom outlines use the closest match to the core's RGB; solo outlines are black. Actual server scoreboard memberships and existing glowing tags/potion effects are preserved.

### Collecting rewards

On landing, a **physical chest** appears at the drop's coordinates. It stays sealed until capture finishes. After capture, **right-click the chest** to open its winners-only contents. The winning kingdom's membership roster is recorded at capture; a solo winner is the sole recipient. Members who join later do not gain access to that crate. Recorded winners retain access if they subsequently leave the kingdom.

Loot is **shared**, not copied for each member. Click or shift-click an item to collect its whole stack once; keep an empty inventory slot. Loot is scattered across random chest slots; those slots remain stable after collecting, reopening or restarting. The chest shows actual stack counts and additional pages for oversized loot pools. Stay within eight blocks of the chest to keep its contents open. `/airdrop` shows location and status only; it does not collect loot remotely. Enemies cannot open or break the chest. Explosions, fluids, pistons and hoppers cannot bypass its lock, including after capture finishes. Ordinary chests are unaffected.

**The deadline is 30 minutes after landing, not 30 minutes after capture.** At that deadline, the crate and all uncollected rewards disappear, including rewards left in the winners' chest. Collected items stay in the recipient's inventory. Nothing spills onto the ground. Restarting or reopening the menu does not reset the deadline or reroll loot.

Changing capture control always resets progress; the new side never inherits the former side's timer. If no stronger qualified side takes over when the holder loses quorum, the existing grace period pauses its timer and then resets it.

Loot transfers already reserved during a save failure are reconciled using the recipient's saved inventory receipt. This prevents an interrupted delivery from granting the same stack twice. An undelivered reservation also expires at the original deadline.

## Loot age and variants

Loot age begins when AirDrops are first enabled. It advances in elapsed real-world 24-hour periods, including nights, empty-server periods and server downtime. Minecraft day cycles have no effect.

| Real-world age | Tier |
|---|---|
| Days 1–2 | Early game: food, iron, arrows, basic supplies and equipment |
| Days 3–4 | Nether unlocked: diamonds, brewing supplies, eligible Nether materials and moderate enchantments |
| Days 5–6 | Advanced: stronger equipment, larger resources and rare premium rewards |
| Day 7+ | Endgame: eligible End resources and rare endgame rewards |

Nether-exclusive loot is locked until day 3; End-exclusive loot is locked until day 7. These are loot unlocks, not restrictions on travelling to dimensions. Early resources remain in later pools. Equipment, enchantments, potions, quantities and chances are configurable.

| Variant | Spawn weight / chance | Emphasis |
|---|---:|---|
| Standard supply drop | 6000 / 60% | Mixed survival supplies |
| Resource cache | 1200 / 12% | Basic materials, valuable resources and diamonds |
| Combat cache | 1000 / 10% | Equipment and eligible potions |
| Enchantment cache | 700 / 7% | Enchanting supplies and books |
| Armory cache | 500 / 5% | Equipment and tools |
| Elite cache | 400 / 4% | Better mixed categories |
| Legendary cache | 180 / 1.8% | Mixed valuable categories; 2× rare-roll multiplier |
| Mythic cache | 20 / 0.2% | Mixed valuable categories; 3× rare-roll multiplier |

Every crate variant uses the current age tier: a day-one Mythic crate has early-game loot, while an endgame Mythic crate has stronger pools, enchantments, quantities and rare chances. A rare variant does **not** guarantee a rare reward.

An independent artifact roll happens **before** normal loot, for every variant. Its default chances are 0.10%, 0.15%, 0.20% and 0.30% at days 1, 3, 5 and 7. Success replaces normal loot with one genuine ability artifact and optionally two fixers. These special artifact drops contain **no cash**. Artifact abilities are generated from their own configured variants, rather than scaling with crate phase. See [Artifacts](ARTIFACTS_GUIDE.md).

### Cash rewards

Ordinary crates include one cash reward: **1,000** on days 1–2, **10,000** on days 3–4, **50,000** on days 5–6 and **100,000** on day 7 onward. Clicking it deposits money into the **winning kingdom’s treasury**, or the **solo winner’s wallet**. It is shared and paid once; it requires no inventory space. Existing core tribute applies. If the winning kingdom disbands, its treasury reward cannot be collected; it expires with the crate. Artifact-only drops contain no cash.

### Default rare-roll probabilities

Each check is made once per eligible crate, never independently for every slot. If several checks succeed, one successful reward is chosen; the rest are discarded. Consequently the final appearance rate for an individual item can be below its check probability. A selected reward may contain a configured quantity/bundle.

| Reward | Days 1–2 | Days 3–4 | Days 5–6 | Day 7+ |
|---|---:|---:|---:|---:|
| Golden apple | 0.5% | 3% | 8% | 15% |
| Enchanted golden apple | 0% | 0.05% | 0.25% | 1% |
| Totem | 0% | 0.1% | 0.5% | 2% |
| Trident | 0% | 0.1% | 0.5% | 2% |
| Netherite upgrade template | 0% | 0.2% | 1% | 3% |
| Netherite ingot | 0% | 0.1% | 0.5% | 2% |
| Elytra | 0% | 0% | 0% | 0.5% |
| Beacon | 0% | 0% | 0.1% | 0.5% |
| Nether star | 0% | 0% | 0% | 0.25% |

## Admin testing commands

These require the configured Kingdoms OP level. Testing changes the next scheduled event time, and creates real, persistent rewards.

```text
/kingdomadmin airdrop start
/kingdomadmin airdrop spawn
/kingdomadmin airdrop spawn mythic
/kingdomadmin airdrop spawn mythic landed
/kingdomadmin airdrop spawnat 1250 -430 combat
/kingdomadmin airdrop spawnat 1250 -430 combat landed
/kingdomadmin airdrop land
/kingdomadmin airdrop cancel
/kingdomadmin airdrop status
/kingdomadmin airdrop age 7
/kingdomadmin airdrop recover
```

- **start:** random safe-location search; bypasses the minimum online count. It requires AirDrops enabled and no uncaptured active drops.
- **spawn:** creates a random variant at your X/Z, using a safe ground height. Add a variant key to select it, and `landed` to skip descent.
- **spawnat:** same behavior at explicit X/Z in the configured AirDrop dimension; works from the console. `random` is a valid variant key.
- **land:** immediately lands every descending crate. Its 30-minute deadline then starts at that landing time.
- **cancel:** stops active drops and pending searches, removing their protection. Already-won loot retains its original deadline.
- **status:** shows loot day, active-drop count and the next scheduling check.
- **age:** changes future loot progression without changing existing loot. Day 1 resets the age epoch to now.
- **recover:** reloads the AirDrop save and retries pending deliveries for online recipients after a storage problem has been fixed.

Spawn tests still obey terrain safety, world borders, claimed land, protected spawn, exclusion regions, crate spacing and the configured active-drop cap. Use open wilderness sufficiently far from spawn. Variant keys are `standard`, `resource`, `combat`, `enchantment`, `armory`, `elite`, `legendary`, `mythic`.

## Settings commands

Values can also be edited without changing the JSON file:

```text
/kingdomadmin airdrop set captureRadius 15
/kingdomadmin airdrop set protectionRadius 5
/kingdomadmin airdrop set takeoverSeconds 15
/kingdomadmin airdrop set parachute true
/kingdomadmin airdrop money 1 1k
/kingdomadmin airdrop money 7 100k
/kingdomadmin airdrop threshold 20 2
```

`set <key> <value>` supports all scalar AirDrop settings and suggests their names. `money <day> <amount>` saves the reward from that real-world day onward; zero disables cash for that period. `threshold <players> <drops>` saves a player-count threshold. Money accepts aliases such as `1k`, `1.5m` and `100k`. All changes are validated and saved. Invalid settings leave both running and saved settings unchanged. Complex loot pools and exclusions remain editable in JSON.

## Configuration

Edit the **`airDrops` object** in `config/kingdoms/config.json`, then run `/kingdomadmin reload`. Invalid loot definitions, identifiers, timers and unsafe limits are rejected. Reloaded timers and radii apply to newly spawned crates; existing crates keep their saved timing/radius snapshots. Feedback and eligibility options apply immediately.

| Setting | Default | Purpose |
|---|---:|---|
| enabled | true | Schedule new events; disable does not cancel existing crates |
| intervalMinutes / jitterMinutes | 180 / 20 | Interval plus uniformly random delay |
| minimumPlayers / playerThresholds | 10 / {10:1,20:2} | Online eligibility and crate counts |
| retrySeconds / maxDrops | 60 / 6 | Retry interval and active-crate cap |
| descentSeconds / descentHeight | 600 / 120 | Descent duration and height above terrain |
| kingdomMembers / captureSeconds | 3 / 90 | Kingdom quorum and normal capture time |
| soloMultiplier / interferenceMultiplier | 1.5 / 1.3 | Solo duration and rival slowdown |
| graceSeconds | 15 | Paused recovery window |
| captureRadius / captureVerticalRadius | 15 / 3 | Capture cylinder |
| protectionRadius / nearbyRadius | 5 / 96 | Horizontal build protection and nearby-player search range; protection 0 reserves only landing/support |
| soloControlStrength / largerGroupTakeover | 3 / true | Solo takeover strength; allow stronger qualifying groups to challenge control |
| captureGlow / descentOutline / parachute | true / true / true | Temporary colored player glow, descending display wireframe and parachute |
| parachuteScale / parachuteSway | 1 / 0.12 | Canopy size multiplier and sideways sway; ropes remain attached when resized |
| parachutePrimaryBlock / parachuteSecondaryBlock | orange_wool / white_wool | Vanilla fabric blocks |
| parachuteSeamBlock / parachuteTrimBlock / parachuteRopeBlock | light_gray_wool / brown_wool / white_wool | Seams, rim/vent/knots and continuous suspension lines |
| takeoverSeconds / scatterLoot | 15 / true | Continuous advantage before takeover; stable random chest slots |
| moneyByDay | {1:1000,3:10000,5:50000,7:100000} | Cash per ordinary crate, by real-world day |
| artifactDropsHaveMoney | false | Keep artifact-only crates free of cash by default |
| particleIntervalTicks / ringPoints / dustSize | 10 / 96 / 1.5 | Dust-ring refresh interval, points and particle size |
| captureExpiryMinutes | 30 | Time after landing until all remaining loot disappears |
| minSpawnDistance / maxSpawnDistance | 320 / 5000 | Random-search distances from world spawn |
| minDropSeparation / maxDropSeparation | 500 / 3000 | Minimum separation and maximum distance from the event's first crate |
| claimBufferBlocks | 0 | Extra claim/exclusion clearance beyond capture radius |
| minLandingY / maxLandingY | -64 / 319 | Allowed surface heights, also constrained to the dimension's build range |
| candidateAttempts / attemptsPerTick | 512 / 1 | Search budget; candidates are processed once per second |
| allowChunkGeneration / requireGround | true / false | Terrain loading and grounded capture eligibility |
| dimension / crateBlock | overworld / barrel | Dimension and descending display block identifiers; the landed block is always a chest |
| particles / sounds / titles / bossBars | true | Feedback toggles |
| pollTicks / checkpointSeconds | 20 / 5 | Presence polling and progress saving |
| historyDays / menuSlot / maxLootStacks | 7 / 38 / 108 | Closed history retention, home button and loot-size limit |
| netherUnlockDay / endUnlockDay | 3 / 7 | Earliest eligible Nether/End loot |
| specialPool / specialPoolChance | empty / 0.05 | Optional named holiday/event pool |

Landing checks require just one dry usable surface column with room for the chest and headroom. Hills, uneven surroundings, tree canopies and replaceable plants are accepted; there is no slope or nine-point flatness test. Water/lava, dangerous support blocks, storage blocks and void are rejected at the landing spot. Only the candidate column needs loading, rather than a grid of surrounding chunks. The default search budget is 512 candidates.

`maxSlope` is obsolete and ignored. `protectionRadius` is independent from `captureRadius` and can be smaller or larger. On first upgrade, the previous defaults of 25-block protection and 6-block height tolerance become 5 and 3, including old event snapshots. `settingsVersion` records that upgrade so future explicit settings remain configurable. Older independent capture timers keep only the most advanced side, then use the new takeover rules. Loot IDs, rewards and original deadlines are preserved.

The spawn-protection square also uses Kingdoms' `spawnClaimProtectionChunks`. `excludedRegions` contains rectangles with `dimension`, `minX`, `maxX`, `minZ`, `maxZ`. `excludedBiomes` is a list of biome identifiers. Protection from another mod is not automatically discovered; list its protected regions here.

`lootTiers` contains `fromDay`, `name`, `minRolls`, `maxRolls`, common `entries`, and `categories`. Each category has `key`, per-crate `chance`, and weighted `entries`. Each tier needs common fallback items available from its starting day. `maxRolls` must allow all categories; rare rewards are an additional check. Stack splitting can create more menu entries than the roll count.

An entry supports `item` (a vanilla item-command definition, including components), `weight`, `minCount`, `maxCount`, `minDay`, `chance`, `enchantments`, `potion`, `name`, `lore`, and `glint`. Quantities must be 1–64; unstackable quantities split into separate stacks. Enchantment levels are 1–255. Names and lore are displayed in small caps.

`variants` controls weights, category chance multipliers and rare-roll multipliers. Multipliers are restricted to 0–5. `rareRewards` contains item entries, `chanceByDay` probabilities and optional allowed `variants`. Probabilities use fractions: `0.001` means 0.1%. Registered rare item identifiers are excluded from ordinary/special pools so they cannot bypass their per-crate checks. `itemUnlockDays` adds explicit unlock days, including for modded items.

`specialPools` maps names to ordinary loot-entry lists. Selecting `specialPool` makes that eligible pool available to weighted selection after its configured check; it does not guarantee a special item. Loot generation still enforces the progression locks and rare-item exclusions.

`startSound`, `landingSound`, `captureSound` accept sound identifiers. `messages` overrides announcement keys `start`, `landed`, `claimed`, `expired`, `cancelled`; `{detail}` inserts the relevant coordinates or winner. Kingdoms' general message map also applies to menu labels/messages. All in-game event text uses small caps.

## Persistence and recovery

Back up `<world>/kingdoms/airdrops.json` **together with the world's `playerdata`**, the rest of Kingdoms data and EconomyCraft data. These files must come from the same backup point.

Loot is generated and saved before any event announcement. Each reward has a unique ID. Transfers reserve that ID first, then save the inventory and a receipt together in vanilla player data, verify the receipt on disk, and finally mark the reward collected. Recovery reconciles those records rather than rerolling or blindly giving rewards again. Cash uses a durable receipt committed with kingdom balances; solo-wallet credit uses Kingdoms’ existing economy transaction journal. A reserved cash reward whose balance commit succeeded is finalized without another payment, even after its deadline.

Descent and expiry use saved wall-clock deadlines. Capture only progresses while eligible players are present; downtime is never credited. Grace periods expire while the server is offline. Pending takeover challenges restart their continuous-presence countdown after restart. A progress checkpoint may lose a few seconds after a crash, according to `checkpointSeconds`.

Descending crate visuals are unsaved vanilla display/interaction entities rebuilt from the authoritative save. Landed chests have persistent event markers and are rebuilt from the saved event when needed. Their backing block inventory stays empty: the protected chest screen reads the saved reward ledger, so hoppers and other ordinary inventory access cannot extract copies. Expiry/cancellation removes only marked event chests, displays, tickets and boss bars; nearby ordinary chests are preserved. Loot never spills into dropped-item entities. Storage failures lock event/reward changes until recovery; do not delete reward journals or independently restore old player inventories to resolve them.


See [the parachute model and conversion notes](PARACHUTE_MODEL.md) for the source, preview and appearance settings.
