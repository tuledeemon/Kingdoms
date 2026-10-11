# AirDrop Administration

These commands require administrator access. Players should start with [AirDrops](AirDrops).

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


See [the parachute model and conversion notes](https://github.com/tuledeemon/Kingdoms/blob/main/PARACHUTE_MODEL.md) for the source, preview and appearance settings.
