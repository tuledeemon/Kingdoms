# Kingdom Administration

Defaults can be changed by server staff. Player guides describe stock Kingdoms settings, so always check the live menus for your server's prices and limits.

## Commands


Tab completion exposes only `/kingdom` for player commands. Subcommands and their arguments remain usable when typed or invoked by menus/dialogs, but are not suggested; the `/k` alias is also hidden from suggestions. Authorized administrators keep the full `/kingdomadmin` command tree and argument suggestions.

| Command | Action |
| --- | --- |
| `/kingdom` or `/k` | Main menu |
| `/kingdom create <name>` | Confirm creation in the current chunk |
| `/kingdom icon` | Use the held item as the saved kingdom icon; mayors can also set it from Kingdom Settings |
| `/kingdom leaderboard` | Treasury ranking with saved item icons and town-center coordinates/dimension in each entry's tooltip |
| `/kingdom beacon` | Open the Kingdom Beacon purchase and active-power UI |
| `/kingdom core` | View the town center, change its block, and inspect tribute |
| `/kingdom claim` | Buy the current chunk |
| `/kingdom outpost` | Confirm purchase of a disconnected outpost anchor |
| `/kingdom unclaim` | Confirm release of the current chunk |
| `/kingdom invite <online-player>` | Invite a member |
| `/kingdom join` | Accept the latest invitation |
| `/kingdom leave` | Confirm leaving |
| `/kingdom kick <name-or-UUID>` | Confirm removal |
| `/kingdom mayor <name-or-UUID> <true/false>` | Confirm mayor appointment/removal |
| `/kingdom grant <name-or-UUID> <permission> <true/false>` | Change a personal grant |
| `/kingdom flag <flag> <true/false>` | Change a land flag |
| `/kingdom treasury` | Treasury menu |
| `/kingdom deposit <amount>` | Deposit into your kingdom; accepts money aliases such as `1k`, `1.5m`, and `2 billion` |
| `/kingdom withdraw <amount>` | Confirm withdrawal; accepts the same money aliases |
| `/kingdom war <kingdom-name>` | Review and confirm declaration |
| `/kingdom alliance` | Open alliances and requests |
| `/kingdom alliance request <kingdom>` | Mayor: request an alliance |
| `/kingdom alliance accept <kingdom>` | Mayor: review accepting an alliance |
| `/kingdom alliance decline <kingdom>` | Mayor: decline a request |
| `/kingdom alliance cancel <kingdom>` | Mayor: withdraw an outgoing request |
| `/kingdom alliance remove <kingdom>` | Mayor: review ending an alliance |
| `/kingdom warinvite <kingdom>` | Mayor: invite an ally during preparation |
| `/kingdom warjoin` | Review incoming joint-war invitations |
| `/kingdom protect` | Confirm purchase of protection |
| `/kingdom acknowledge` | Acknowledge the current debt warning |
| `/kingdom disband` | Confirm disbanding and forfeiting the bank |
| `/kingdomadmin war` | View server war restrictions |
| `/kingdomadmin war set <key> <value>` | Set enabled, scope, blockPearlTeleport or preventLavaCasting |
| `/kingdomadmin war <block/placement/entity/explosion/blockdamage> <add/remove> <id>` | Edit a restriction list; registry IDs have tab completion |
| `/kingdomadmin war <list> <list/clear>` | Show or empty a restriction list |
| `/kingdomadmin reload` | Validate and load config changes |
| `/kingdomadmin beaconprice <effect> <amount>` | Set a Level I beacon price; accepts money aliases |
| `/kingdomadmin set <key> <value>` | Set a scalar config field and save it |
| `/kingdomadmin cap "Kingdom Name" claims <number>` | Override this kingdom's claim cap; 0 restores global default |
| `/kingdomadmin cap "Kingdom Name" members <number>` | Override member cap; 0 restores global default |
| `/kingdomadmin endwar <kingdom-name>` | End the entire shared war early; every participant and their members enter cooldown |
| `/kingdomadmin clearcooldown <kingdom-name>` | Clear this kingdom's war cooldown, active war protection (including starter protection), and its current members' saved war cooldowns; end preparation/wartime first |

Permissions: `PLACE`, `BREAK`, `INTERACT`, `CONTAINERS`, `ENTITIES`, `PVP`, `DEPOSIT`, `WITHDRAW`.

Flags: `EXPLOSIONS`, `FIRE`, `MOBS`, `MOB_GRIEFING`, `PVP`. At peace, disabled flags apply even to mayors and players with building permissions. New kingdoms start with these flags off. Liquids and machines have no separate toggle: they work normally within a kingdom, including across its chunk borders. Redstone signals can cross claim borders. Flow, dispenser buckets and inventory exports may leave a claim into wilderness. Incoming wilderness flow and transfers into another kingdom remain blocked at peace. Piston extension may enter wilderness; retraction retains ownership checks. Pouring uses Place permission and collecting uses Break permission. The global `preventFluidGriefing` setting controls liquid boundary checks, while cores and AirDrop protection remain enforced. A forbidden piston event is rejected before its first state change, and accepted moving blocks finish their animations normally. The removed `PISTONS`, `AUTOMATION` and `FLUIDS` entries are ignored when loading existing configs, kingdom saves and transaction snapshots.

Operators at the configured `opLevel` bypass ordinary claim permissions for placing, breaking, interacting, containers, entities and PvP. They still obey kingdom flags, including explosions, fire, mobs, mob griefing and PvP. Operator status does not grant treasury access.


## Configuration


All amounts resolve to whole EconomyCraft currency units. Money inputs accept case-insensitive `k`, `m`, `b`, and `t` suffixes, decimals such as `1.5m`, and names such as `thousand`, `million`, `billion`, and `trillion`. Commas, underscores, and an optional `$` prefix are accepted. Money-valued `/kingdomadmin set` fields use the same parser. Use `/kingdomadmin set` for scalar fields. Edit arrays/maps/sets in the JSON file and run `/kingdomadmin reload`. Invalid reloads leave the previous configuration active.

| Setting | Default | Meaning |
| --- | --- | --- |
| `creationFee` | 100000 | Founder's personal creation cost |
| `expansionFee` | 25000 | Treasury cost per additional chunk |
| `outpostFee` | 150000 | Treasury cost per disconnected outpost anchor |
| `protectionFee` | 1000000 | Treasury price of war protection |
| `beaconEffectFees` | Per-effect defaults | Level I prices; removing a key disables that effect |
| `beaconDurationMinutes` | 1440 | Real-time duration of each purchased power |
| `beaconLevel2Multiplier` | 4 | Positive Level II price multiplier |
| `beaconRefreshTicks` | 80 | How frequently active powers are reapplied |
| `coreTaxPercent` | 20 | Tribute percentage charged on incoming credits |
| `coreReturnMinutes` | 10 | Real-time timeout for unplaced core items |
| `coreSkyAccessRequired` | true | Relocate continuously covered placed cores within the same column, including captured cores; notify owner/holder mayors |
| `coreCoveredTimeoutSeconds` | 300 | Continuous coverage before relocation; 1..604800 seconds; removing the cover resets the timer |
| `coreParticles` | true | Dust along all twelve core-block edges |
| `coreGlow` | true | Glowing frame using vanilla display packets |
| `coreGlowOnlyDuringWar` | true | Restrict glow to active war, excluding preparation |
| `coreWarOutlineThicknessMilli` | 60 | Wartime frame thickness; 2..100 thousandths of a block |
| `coreHighlightRange` | 48 | Nearby dust and optional peace-time glow range; 4..128; does not limit wartime glow |
| `coreParticleIntervalTicks` | 10 | Dust refresh interval; 5..100 ticks |
| `coreDustSizePercent` | 45 | Dust size as a percentage of normal; 5..200 |
| `coreOutlineThicknessMilli` | 12 | Frame thickness in thousandths of a block; 2..100 |

Set these with `/kingdomadmin set coreSkyAccessRequired true` and `/kingdomadmin set coreCoveredTimeoutSeconds 300`. Sky enforcement uses the highest non-air block, so transparent blocks, leaves and fluids count as roofs. The core marker moves without removing its original block or changing tribute. If the highest block is unsuitable or reserved, stone is created above it; build-height failures warn once and retry automatically. Timers are saved and checked once per second in already-loaded chunks only. Offline mayors receive a reminder on joining if the core is still covered.
| `kingdomRejoinCooldownMinutes` | 60 | Wait after leaving/kicking before rejoining the same kingdom; 0 disables |
| `beaconEnabled`, `beaconFx` | true | Master power application and purchase-FX toggles |
| `beaconPurchaseSound`, `beaconHostileSound` | Vanilla beacon/guardian sounds | Purchase notification sounds |
| `upkeepPerMember`, `upkeepBase` | 1000, 0 | Upkeep formula |
| `upkeepIntervalMinutes` | 1440 | Billing interval |
| `gracePeriodMinutes` | 4320 | Time from first unpaid bill to disbanding |
| `maxMembers`, `maxClaims` | 20, 64 | Global caps; claim cap includes the founding chunk |
| `outpostClaimWeight`, `maxOutposts` | 6, 4 | Claim slots consumed per outpost and maximum outpost anchors per kingdom |
| `kingdomCreationBufferChunks` | 16 | Minimum blocked radius around every existing claim |
| `spawnClaimProtectionChunks` | 16 | No-claim square radius around overworld world spawn; 0 disables it |
| `alliancesEnabled` | true | Allow new alliance requests and acceptance |
| `maxAllies` | 5 | Direct allies per kingdom; 0..1000 |
| `allianceRequestMinutes` | 60 | New request lifetime; 1..10080 real-time minutes |
| `alliedFriendlyFire` | false | Allow ordinary PvP rules between allies and different kingdoms on the same war side |
| `jointWarsEnabled` | true | Allow war invitations and joining; existing participants remain |
| `maxKingdomsPerWarSide` | 4 | Includes the original kingdom; 1..1000 |
| `jointWarRequiresOnlineMajority` | true | Require a strict online majority when joining a war; mayor approval is always required |
| `warPreparationSeconds` | 180 | Protected countdown before vulnerability |
| `warCountdownMilestonesSeconds` | 180, 60, 30, 10, 5..1 | Persisted countdown announcements |
| `warMinutes` | 15 | Vulnerable wartime duration |
| `warCooldownMinutes` | 120 | Cooldown after a new war |
| `protectionMinutes` | 1440 | Duration of a new protection purchase |
| `newKingdomProtectionMinutes` | 1440 | Automatic war protection granted when a kingdom is created; 0 disables it |
| `adjacentExpansion` | true | Require connected claims per dimension |
| `allowMultipleDimensions` | false | Allow another dimension's first claim without adjacency |
| `allowedDimensions` | All three vanilla dimensions | Dimensions where players may create/expand claims |
| `defaultFlags` | Empty | Flags enabled for newly created kingdoms |
| `defaultMemberPermissions` | Building, breaking, interacting, containers, entities, deposit | Initial member defaults |
| `defaultPublicPermissions` | Empty | Initial visitor permissions |
| `allowPublicPermissions` | true | Allow mayors to edit public permissions |
| `allowTreasuryWithdrawals` | true | Enable withdrawals |
| `allowedMobTypes` | Villager | Extra mob-type exemptions; animals and pets are already allowed |
| `smallCaps` | true | Small-cap UI headings |
| `title` | kingdoms | Main menu title |
| `primaryColor`, `secondaryColor`, `errorColor` | gold, aqua, red | Vanilla formatting colors |
| `menuSlots` | Generated defaults | Original main-menu button positions, 0–53; keep them unique. War protection uses slot 30 and the beacon uses 32. Alliances uses 40 (an unused slot is chosen for custom layouts). The retired `help` position is ignored |
| `menuLayoutVersion` | 3 | Internal migration marker; previous stock layouts restore and move the beacon to 32, while custom positions are preserved |
| `menuItems` | Generated defaults | Main-menu item identifiers |
| `messages` | Empty | Text overrides keyed by the original text; also applies to button labels/lore |
| `treasuryPresets` | 1000, 10000, 100000 | Three deposit/withdraw menu amounts |
| `overdueWarning`, `acknowledgeLabel` | Generated defaults | Debt-warning wording and acknowledgment button |
| `warningReopenTicks`, `menuRefreshTicks`, `territoryUpdateTicks` | 20, 20, 20 | Warning, menu and HUD refresh intervals |
| `territoryActionBar`, `territoryTitles`, `territorySounds` | true | Territory feedback channels |
| `warActionBar`, `warTitles`, `warSounds` | true | War feedback channels |
| `territoryEnterSound`, `territoryLeaveSound`, `warCountdownSound`, `warStartSound`, `warEndSound`, `kingdomCreatedSound` | Vanilla sound IDs | Configurable sound effects |
| `leaderboardSize` | 10 | Number of ranked kingdoms |
| `leaderboardShowPosition/Icon/Name/Treasury` | true | Leaderboard field toggles |
| `leaderboardShowCenter` | true | Show each kingdom's current town-center coordinates and dimension; carried cores show their last placed location |
| `maxNameLength` | 24 | Maximum kingdom-name length |
| `opLevel` | 2 | Required operator level for admin commands |
| `logActions` | true | Log treasury/claim/war actions |
| `preventFluidGriefing` | true | Apply ownership boundaries to liquid flow and dispenser buckets; outward flow into wilderness is allowed. Core, AirDrop and direct-player permissions remain enforced when disabled. |
| `maxTrackedHazards` | 250000 | Ownership-map capacity; unknown effects are handled conservatively |

Changing global default permissions/flags affects newly created kingdoms; existing kingdoms keep their mayor-selected settings. Changing war/protection durations affects new wars/purchases, not existing expiry timestamps. Lowering a cap blocks growth; it does not evict members or unclaim land.

Examples:

```text
/kingdomadmin set maxClaims 128
/kingdomadmin set maxMembers 30
/kingdomadmin set expansionFee 50000
/kingdomadmin set upkeepPerMember 2500
/kingdomadmin set beaconDurationMinutes 720
/kingdomadmin beaconprice mining_fatigue 750k
/kingdomadmin set gracePeriodMinutes 2880
/kingdomadmin set spawnClaimProtectionChunks 24
/kingdomadmin set newKingdomProtectionMinutes 2880
/kingdomadmin cap "Red Kingdom" claims 256
/kingdomadmin clearcooldown Red Kingdom
```

### War restrictions

Settings live in `warRestrictions`. All are editable without restarting using `/kingdomadmin war`.

| Setting | Default | Effect |
| --- | --- | --- |
| `enabled` | true | Enable the policy system |
| `scope` | war_claims | `war_claims`, `war` (also active participants), or `global` |
| `blockPearlTeleport` | false | Block pearl throws/teleports in the restricted scope, including entry/exit |
| `preventLavaCasting` | false | Prevent vanilla water/lava cobblestone, stone and obsidian formation |
| `instantBreakBlocks` | empty | Selected blocks become instant to mine during active war; permissions and vanilla drops remain |
| `blockedPlacementBlocks` | empty | Prevent introducing these block types into restricted locations |
| `blockedEntities` | empty | Reject spawn and discard existing matching entities when ticked in restricted situations; players excluded |
| `blockedExplosions` | empty | Cancel detonations by source entity or special `minecraft:bed`, `minecraft:respawn_anchor`, `minecraft:other`; `*` matches all |
| `blockedExplosionBlockDamage` | empty | Allow explosions but prevent terrain damage/chain priming/fire by source; also supports `*` |

Policies are saved immediately and can change during a war. Empty defaults preserve existing gameplay. Administrator restrictions override mayor flags and operator physical-permission bypass. [War restriction command guide](War-Restrictions).

### BlueMap territories

Install BlueMap's Fabric version for Minecraft 26.2 to enable the automatic `kingdoms:territories` layer. The optional API integration does not bundle BlueMap or load Minecraft chunks. Connected claims merge, unclaimed holes stay clear, and outposts remain separate. Markers refresh every ten seconds and are rebuilt after BlueMap reloads.

Settings live in the `blueMap` configuration object. Use `/kingdomadmin bluemap set <key> <value>` for scalar settings or edit `mapIds` and reload. Controls include the layer title, visibility, core pins, mayor/member/treasury/war details, border width, opacity, overlay height, viewing distance and update interval. See the [complete settings and installation guide](BlueMap).
