# ᴋɪɴɢᴅᴏᴍѕ — Fabric 26.2

Kingdoms 1.8.1 reorganizes inventory menus with centered action rows, shared frames and 28-entry pages, and includes optional BlueMap territories with merged claim borders, clickable kingdom details and core pins. It includes the 1.7.2 explosion-protection patch and the Wind Burst Spear living-target restriction. The eight-artifact catalogue and joining-world freeze fix remain: AirDrop cleanup runs only after chunks become fully available. See [the fix notes](CHUNK_LOAD_FIX.md).

A server-only kingdom and claims mod. Players join with normal Minecraft 26.2 clients; no client mod, custom registry, or resource pack is required. Menus use vanilla chest inventories, mandatory small-caps text, confirmation pages, live counters, territory action bars, transition titles and sounds.

AirDrops add recurring kingdom/solo PvP objectives: a three-hour schedule plus random delay, ten-minute descent, eight age-scaled crate variants, shared winners-only loot chests, and no-spill expiry thirty minutes after landing. Drops accept uneven terrain and tree canopies. A configurable five-block radius protects building during descent/capture; the capture radius defaults to fifteen blocks with ±3 blocks of height tolerance. Only one side progresses, with a stronger group taking over from zero after 15 continuous seconds of superior strength; solo players have strength three. Capturers glow, a dust circle marks capture range, and glowing display edges and a rounded striped parachute with stitched fabric and eight continuous ropes surround the descending crate. The assembly sways gently and settles onto its exact landing position. Chest rewards are scattered in stable random slots. Ordinary crates include age-scaled cash from 1,000 to 100,000, credited once to the winning kingdom treasury or solo wallet. Use `/airdrop` or **AirDrops** in `/kingdom` for locations/status. After capture, right-click the physical chest at the drop site to collect loot; remote menus cannot dispense rewards. See [the AirDrop player/admin guide](AIRDROPS_GUIDE.md) for configuration and testing commands.

Eight persistent artifacts have guaranteed eligible triggers and short small-caps lore. Tools and shields use vanilla durability, repairs and compatible enchantments, including Mending and Unbreaking. Guardian Angel has 100 charges and grants three seconds of server-only cobweb immunity using the CustomGear boots approach. Wind Burst Spear attacks living players and mobs only; armor stands, mannequins and nonliving targets are ignored for jabs and charged attacks, including the Netherite upgrade. Use `/artifact` to upgrade for three Netherite Ingots. Artifact drops still contain one artifact and optional fixers, no cash and no quantity caps. Old types retire safely; shared types migrate to their new mechanics. See [the artifact guide](ARTIFACTS_GUIDE.md).

For a player-facing walkthrough of the menus, rules and commands, see [the player guide](PLAYER_GUIDE.md).

## Install

Put the release JAR in your **server** `mods` directory alongside:

- Fabric Loader **0.19.5+**, Minecraft **26.2**, Java **25**.
- Fabric API for 26.2 (tested with **0.160.0+26.2**).
- EconomyCraft Fabric **1.10.0+** for 26.2.
- EconomyCraft's dependencies: Architectury API and Placeholder API (tested with **21.1.9** and **3.1.0-beta.1+26.2**).

Start the server. Open `/kingdom` (alias `/k`). The server generates `config/kingdoms/config.json`. Kingdom data lives in `<world>/kingdoms/`; back up that entire directory together with EconomyCraft's data and the world.

Do not install the integration-test mod or use the test server's properties on a public server. Those are development fixtures only.

## Kingdoms and land

- `/kingdom create <name>` shows a confirmation. The founding mayor pays the creation fee from their EconomyCraft balance. Their current 16×16 chunk becomes the first full-height claim. Creation is rejected within 16 chunks of any existing claim, with the nearest kingdom and distance shown. By default, creation and every later claim are also blocked inside the 16-chunk square radius around overworld world spawn.
- Expansion uses the shared treasury. By default it must touch a claimed chunk, remains in the original dimension, and cannot exceed the OP-configured claim cap.
- `/kingdom outpost` claims a disconnected anchor chunk. By default an outpost costs 150,000 from the shared treasury and consumes six claim slots. Chunks expanded from that anchor use the normal one slot each. Outpost price, weight, and per-kingdom cap are configurable.
- Release only edge chunks when adjacency is enabled. Releasing the last chunk requires disbanding. Releasing land has no refund.
- Membership is invite-only. One player can belong to one kingdom. Mayors can appoint additional mayors; a kingdom must retain at least one.
- Every mayor has management access, including treasury withdrawals, appointments and disbanding. Appoint only people you trust.
- Personal permission grants can also apply to nonmembers for building, breaking, interacting, containers, entities and PvP. Treasury access requires kingdom membership.
- Grants are additive: switching a personal grant off removes that grant; member/public defaults can still grant that action. Disable a default to make access individually assigned.
- Unclaiming/disbanding is confirmed in a menu. Voluntary disbanding forfeits treasury funds; withdraw them first if desired.

## Wars

A declaration starts a **3-minute preparation countdown** by default. Claims stay fully protected until that countdown ends; the default vulnerable wartime is **15 minutes**. Both kingdoms receive chat, action-bar, title and sound milestones at 3 minutes, 1 minute, 30 seconds, 10 seconds and the final 5 seconds. Only a mayor can declare war. At declaration the defender needs **strictly more than half** its members online, including **at least one mayor**. For example: 6 of 10, 3 of 5, or the only player in a one-member kingdom.

- Each kingdom can participate in one preparation/war at a time. Membership, claims, mayors, flags and protection purchases freeze as soon as preparation begins.
- Once preparation ends, members of either kingdom have unrestricted physical access to both kingdoms' land. Ordinary visitors gain no war access.
- All land flags, including explosions, fire, fluids, mobs and PvP, are overridden for the war. Administrative rules about TNT/lava casting remain manually enforced.
- Treasury permissions remain enforced. War never grants withdrawal privileges.
- The war lasts its full duration even if players/mayors disconnect.
- Membership, mayor appointments, permission/flag edits and claim changes are locked during war. Treasury deposits and authorized withdrawals remain available.
- New kingdoms receive 24 hours of automatic war protection by default. Protected kingdoms cannot declare war or be declared upon. Protection cannot be purchased during war or stacked while active.
- Both kingdoms receive a **two-hour cooldown after the scheduled end**. They cannot declare or receive another declaration during that cooldown. Participants retain their cooldown if they recreate a kingdom or join another kingdom.
- Wars, protection and cooldowns use real timestamps and survive restarts. Time continues while the server is offline. Destruction during war is permanent; this mod does not roll back the terrain afterward.
- Automatic disbanding for debt is deferred until an active war ends.

## Town centers, cores and tribute

Every new kingdom starts with a core at the topmost breakable block in the center of its founding chunk. Existing kingdoms receive a core in a mainland chunk on startup; if that center column has no breakable block, place one there first. Dust traces all twelve edges of the core block alongside a thin, glowing wireframe. Each kingdom gets a bright random color saved with its kingdom record; dust and glow share that color, including when its core is moved or captured. Existing saves receive a stable color based on their random kingdom UUID. Air, fire, fluid and unbreakable blocks cannot be chosen. Waterlogged solid blocks remain eligible.

The glow uses vanilla block-display packets and requires no client mod. It outlines a cube even when the chosen block is a chest or another partial model. Frames do not create real world entities, change blocks, collide, drop items or enter chunk saves. Glow is enabled only during **active wartime** by default, following the kingdom whose claim currently holds the core. Preparation and peace do not show glow. During war, its frame is five times thicker and is sent to every player in the same dimension without the mod's nearby-distance cutoff or a loaded-server-chunk requirement. No chunks are force-loaded. Frame packets are removed when the core is carried, moved, disbanded, no longer at war or in another dimension. Respawning and changing dimensions reset the display. Dust keeps its 48-block nearby range and refreshes twice per second. Colors are random and are not guaranteed unique.

Wartime frames use the maximum finite display viewing range and are sent throughout the same dimension without a nearby distance cutoff. Vanilla clients still require the display's terrain section to be compiled and visible, so this cannot guarantee a world-space glow at arbitrary distances. The glow disappears after war. Disabling `coreGlowOnlyDuringWar` permits nearby peace-time glow. Core highlighting adds no action-bar messages.

Open **Settings → Town center → Change block**, then right-click an existing block inside your own kingdom. Only mayors can change the location, and selection is unavailable during war preparation or wartime, or while the core is captive/in transit. `/kingdom core` opens the same core information menu.

To move a captured core your kingdom holds, open **Settings → Town center → Captured kingdoms**, choose the kingdom, then **Change block** and right-click an existing breakable block in your own claims. The captured-core list has pages for larger collections. Only mayors may move secured cores, and both kingdoms must be outside preparation/wartime. Moving the marker leaves both blocks intact and retains the original kingdom, captor, highlight color and tribute, including fractional tax credits. Selection expires after two minutes and rechecks ownership and war state on the final click.

During active wartime, opposing members can break the host kingdom's marked core blocks. Ordinary block drops still follow Minecraft's break operation, and the player receives a unique, glowing core item. Carry that item home and **right-click an existing block in your own claimed land** to secure the core. The item is a designation token; it does not place a new physical block. Securing another kingdom's core starts tribute; merely breaking it does not. You may hold multiple kingdoms' cores, and taking your own core away does not free the other captive kingdoms.

Each current member of a captive kingdom pays **20% of every positive EconomyCraft balance change** to the capturing kingdom's shared treasury. This includes selling, rewards, incoming payments, positive balance-setting adjustments, and treasury withdrawals. Spending/debits are not income. Kingdom withdrawals are taxed inside their own recovery transaction, and tax debits are excluded from the event listener to prevent recursion. Fractional tax amounts carry forward per member so splitting money into tiny credits does not avoid tax. This applies wherever the member earns the money, including wilderness. Other mods must use EconomyCraft's balance system to participate.

To regain independence, recover your kingdom's core from its captor during a war and right-click an existing block in your own kingdom with the recovered item. Tribute continues until the item is secured at home. An unplaced core returns to its previous territory after **10 real-time minutes**, including time offline. Returning a captive core to its former host does not grant independence. If its former block is gone, that block type is restored empty so the core remains recoverable; container contents are not restored. The saved unique ID changes after return or securing, invalidating any old/copy items.

Core items can be carried, dropped and picked up, but cannot be put into storage. The restrictions follow NationMace's approach: ordinary clicks, shift-clicks, hotbar/offhand swaps, dragging, portable bundles, hopper transfers and dropped-item collection are blocked; shelf interactions that could siphon a hotbar core and entity storage interactions are also refused. Workstation input slots are refused for core items as well.

A kingdom cannot buy war protection while it holds a foreign core, including a foreign core in transit. Core blocks resist unattended deletion by explosions, fluids, fire or pistons; player capture is the route for removing them. A chunk containing or reserving a core cannot be unclaimed. Disbanding a captor releases its captive kingdoms and generates their new home cores. Voluntary disbanding remains allowed.

Members leave or are kicked immediately and stop paying tribute immediately. They cannot rejoin that same kingdom for **one hour**; normal war membership freezes still apply. This cooldown persists through restarts and does not prohibit joining a different kingdom.

## Treasury and upkeep

Default upkeep is `upkeepBase + memberCount × upkeepPerMember` per real-time cycle. Mayors count as members. The default cycle is 24 hours.

Anyone with `DEPOSIT` can add their own EconomyCraft money. `WITHDRAW` must be granted by a mayor; mayors already have it. The bank starts at zero. There are fixed menu presets plus commands for arbitrary positive integer amounts.

Unpaid upkeep becomes debt. Claims remain protected during the configurable grace period (default 72 hours). The warning page automatically reopens for mayors until **one mayor acknowledges that incident**. That acknowledgment dismisses it for every mayor, including after reconnecting. Acknowledgment does not pay the bill or extend the deadline. Paying the entire debt clears the incident; a later missed payment creates a new warning.

If the grace period expires without enough treasury funds to pay the debt, the kingdom disbands and all its claims become wilderness. Missed offline cycles are billed on startup using the current member count/rate. Keep the treasury funded before extended server downtime.

Money transfers use the official EconomyCraft API on the server thread, with mutation-source labels and a write-ahead recovery record. Ambiguous interrupted transfers lock changes and require an administrator to reconcile `transaction.json`; never delete that file blindly. Invalid claim data stops startup instead of silently opening the land.

## Kingdom Beacon

The main Kingdom menu includes a vanilla **Beacon** button. It opens a server-side purchase UI; no client mod or custom item registry is required. Only a mayor can spend the shared treasury on beacon powers.

Member powers are **Speed, Haste, Resistance, Jump Boost, Strength, and Regeneration**. Each is available at Level I or Level II. Level II costs four times the configured Level I price by default. A kingdom can run several different powers simultaneously. The same active power cannot be repurchased at the same or a lower level; Level I can be upgraded to Level II.

Opponent powers are **Slowness, Blindness, and Mining Fatigue I**. They can only be purchased against the kingdom's active enemy after war preparation ends and claims become vulnerable. A hostile power targets all members of that enemy kingdom and continues for its purchased duration even if wartime ends. All positive and hostile powers function only while the affected player is inside any claimed chunk; they stop in wilderness and return upon entering claimed land. Purchases default to 24 real-time hours, persist through restarts, apply immediately to eligible online members, and are applied when affected members join. Disbanding either involved kingdom removes its related powers.

The UI shows the treasury balance, exact cost, target, duration, active powers, incoming hostile powers, and remaining time. Purchases produce chat notices, titles, sounds, and particles when `beaconFx` is enabled.

## Mobs

With `MOBS` disabled, unwanted mobs cannot spawn in claims and unapproved entrants disappear at the next entity movement/tick check, including passengers. **There is no quarantine or grace window.**

Villagers, farm animals, pets and configured exempt entity types are allowed. Before bringing another mob into a kingdom, a mayor **sneak-right-clicks that individual mob outside the claim**. Its UUID is allowed across the kingdom and saved across restarts; approved mobs are protected from ordinary distance despawning. Approval does not bypass combat, fire or explosion settings. Enable `MOBS` to allow all mobs. The war override allows mobs while the war is active.

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
| `/kingdom protect` | Confirm purchase of protection |
| `/kingdom acknowledge` | Acknowledge the current debt warning |
| `/kingdom disband` | Confirm disbanding and forfeiting the bank |
| `/kingdomadmin reload` | Validate and load config changes |
| `/kingdomadmin beaconprice <effect> <amount>` | Set a Level I beacon price; accepts money aliases |
| `/kingdomadmin set <key> <value>` | Set a scalar config field and save it |
| `/kingdomadmin cap "Kingdom Name" claims <number>` | Override this kingdom's claim cap; 0 restores global default |
| `/kingdomadmin cap "Kingdom Name" members <number>` | Override member cap; 0 restores global default |
| `/kingdomadmin endwar <kingdom-name>` | End war early; both kingdoms enter cooldown |
| `/kingdomadmin clearcooldown <kingdom-name>` | Clear this kingdom's war cooldown, active war protection (including starter protection), and its current members' saved war cooldowns; end preparation/wartime first |

Permissions: `PLACE`, `BREAK`, `INTERACT`, `CONTAINERS`, `ENTITIES`, `PVP`, `DEPOSIT`, `WITHDRAW`.

Flags: `EXPLOSIONS`, `FIRE`, `FLUIDS`, `MOBS`, `MOB_GRIEFING`, `PISTONS`, `AUTOMATION`, `PVP`. At peace, disabled flags apply even to mayors and players with building permissions. `AUTOMATION` is enabled by default for new kingdoms; the other flags default to off. Automation covers hoppers, droppers, dispensers and similar inventory machines. Pistons remain controlled by `PISTONS`, and redstone signals cannot bypass claim boundaries.

Operators at the configured `opLevel` bypass ordinary claim permissions for placing, breaking, interacting, containers, entities and PvP. They still obey kingdom flags, including explosions, fire, fluids, mobs, mob griefing, pistons, Automation and PvP. Operator status does not grant treasury access.

## Placeholders

These use Text Placeholder API syntax, for example `%kingdoms:kingdom_styled%`. Rank-based leaderboard placeholders take a one-based argument, for example `%kingdoms:leaderboard_entry/1%`.

| Placeholder | Value |
| --- | --- |
| `player_kingdom`, `kingdom_name`, `kingdom_display_name` | Player kingdom identity |
| `kingdom_styled` | Item object icon + space + small-caps kingdom display name; `ɴᴏɴᴇ` without a kingdom |
| `kingdom_icon` | Real Minecraft 26.2 atlas `object` text component for the saved item icon |
| `kingdom_icon_material` | Saved item identifier |
| `kingdom_treasury`, `kingdom_treasury_raw` | Formatted/raw treasury balance |
| `kingdom_rank`, `kingdom_member_count`, `kingdom_claim_count` | Small-caps role, member count, and actual claimed chunk count |
| `kingdom_claim_slots`, `kingdom_outpost_count` | Weighted claim-slot use and outpost count |
| `kingdom_at_war` | Whether claims are currently vulnerable |
| `war_preparation_remaining`, `wartime_remaining`, `enemy_kingdom` | Current conflict state |
| `territory_kingdom`, `territory_icon` | Claim at the player's position |
| `inside_own_kingdom`, `inside_enemy_territory`, `in_wilderness` | Position booleans |
| `leaderboard_position/<rank>` | Requested one-based position |
| `leaderboard_name/<rank>` | Ranked identity |
| `leaderboard_icon/<rank>`, `leaderboard_icon_material/<rank>` | Ranked object icon/material |
| `leaderboard_treasury/<rank>`, `leaderboard_treasury_raw/<rank>` | Ranked balance |
| `leaderboard_entry/<rank>` | Combined `#1 [object icon] small-caps name — balance` component |

New kingdoms use paper as their default icon. Mayors can hold an item and select **Kingdom icon** in the settings menu to change it.

`%kingdoms:kingdom_styled%` preserves the real object icon in component-aware formatting systems. Systems that convert components to plain text cannot display the actual sprite. It is never added as a chat prefix automatically.

Inline icons resolve through a sprite mapping generated from the exact Minecraft 26.2 client assets. Model aliases and multi-state items use a verified representative texture, including clock/compass frames, crossbow standby, shared stick/apple textures and tipped-arrow base. These are static icons; item-model state, layered tinting and enchantment glint are not rendered by atlas object text. Shields use the vanilla shield GUI silhouette. Unknown modded item assets use paper for the inline sprite while retaining the selected item in hover data and the inventory leaderboard.

To regenerate the mapping for this version, run `tools/generate-icon-sprites.ps1 -ClientJar <path-to-26.2-client.jar>`. This ships only sprite identifiers, without Minecraft textures, resource packs or client-side code.

Kingdom tags have been removed, including `/kingdom tag`, `kingdom_tag`, `territory_tag`, `leaderboard_tag`, `maxTagLength` and `leaderboardShowTag`. Old tag fields in saved data/config are ignored; other kingdom data is preserved. Replace retired tag placeholders in server formatting with `%kingdoms:kingdom_styled%` or a name placeholder.

`KingdomIcons.stack(Kingdom)` is the Java-side `ItemStack` bridge for integrations that render item objects directly. The placeholder itself returns Minecraft's inline atlas object component, with the selected item attached as hover data. It is never inserted into chat automatically.
## Customization

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
| `coreParticles` | true | Dust along all twelve core-block edges |
| `coreGlow` | true | Glowing frame using vanilla display packets |
| `coreGlowOnlyDuringWar` | true | Restrict glow to active war, excluding preparation |
| `coreWarOutlineThicknessMilli` | 60 | Wartime frame thickness; 2..100 thousandths of a block |
| `coreHighlightRange` | 48 | Nearby dust and optional peace-time glow range; 4..128; does not limit wartime glow |
| `coreParticleIntervalTicks` | 10 | Dust refresh interval; 5..100 ticks |
| `coreDustSizePercent` | 45 | Dust size as a percentage of normal; 5..200 |
| `coreOutlineThicknessMilli` | 12 | Frame thickness in thousandths of a block; 2..100 |
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
| `warPreparationSeconds` | 180 | Protected countdown before vulnerability |
| `warCountdownMilestonesSeconds` | 180, 60, 30, 10, 5..1 | Persisted countdown announcements |
| `warMinutes` | 15 | Vulnerable wartime duration |
| `warCooldownMinutes` | 120 | Cooldown after a new war |
| `protectionMinutes` | 1440 | Duration of a new protection purchase |
| `newKingdomProtectionMinutes` | 1440 | Automatic war protection granted when a kingdom is created; 0 disables it |
| `adjacentExpansion` | true | Require connected claims per dimension |
| `allowMultipleDimensions` | false | Allow another dimension's first claim without adjacency |
| `allowedDimensions` | All three vanilla dimensions | Dimensions where players may create/expand claims |
| `defaultFlags` | AUTOMATION | Flags enabled for newly created kingdoms |
| `defaultMemberPermissions` | Building, breaking, interacting, containers, entities, deposit | Initial member defaults |
| `defaultPublicPermissions` | Empty | Initial visitor permissions |
| `allowPublicPermissions` | true | Allow mayors to edit public permissions |
| `allowTreasuryWithdrawals` | true | Enable withdrawals |
| `allowedMobTypes` | Villager | Extra mob-type exemptions; animals and pets are already allowed |
| `smallCaps` | true | Small-cap UI headings |
| `title` | kingdoms | Main menu title |
| `primaryColor`, `secondaryColor`, `errorColor` | gold, aqua, red | Vanilla formatting colors |
| `menuSlots` | Generated defaults | Main-menu button positions, 0–53; keep them unique. Stock rows: claims/treasury/members 20/22/24; war/protection/beacon 29/31/33; AirDrops/artifacts/settings 38/40/42; help/close/leaderboard 45/49/51 |
| `menuDecorations` | true | Fill unused menu slots with a frame and background. Customize `menuItems.frame`, `menuItems.background` and `menuItems.warning_frame` with vanilla item IDs |
| `menuLayoutVersion` | 1 after validation | Internal one-time layout migration marker; old stock positions migrate, customized positions are preserved |
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
| `protectWildernessFromClaimMachines` | true | Also prevent peaceful claim machinery from affecting wilderness across a border |
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

### BlueMap territories

Install BlueMap's Fabric version for Minecraft 26.2 to enable the automatic `kingdoms:territories` layer. The optional API integration does not bundle BlueMap or load Minecraft chunks. Connected claims merge, unclaimed holes stay clear, and outposts remain separate. Markers refresh every ten seconds and are rebuilt after BlueMap reloads.

Settings live in the `blueMap` configuration object. Use `/kingdomadmin bluemap set <key> <value>` for scalar settings or edit `mapIds` and reload. Controls include the layer title, visibility, core pins, mayor/member/treasury/war details, border width, opacity, overlay height, viewing distance and update interval. See the [complete settings and installation guide](BLUEMAP_GUIDE.md).

## Protection implementation and limits

Protection checks direct actions and destination block mutations, as well as scheduled fluid/fire/block ticks, entity mutations, explosions before block drops, damage, harmful potion effects, piston movement plans, dispensers/droppers, hopper transfers, double-chest access and redstone across borders. Hazard ownership is tracked so authorized wartime effects can cross borders while neutral-player effects remain denied. Ownerless cross-border effects are rejected conservatively; mobs/environment cannot always reveal who caused them.

This is **not a guarantee against every possible mod interaction**. A server mod that writes chunk storage directly or supplies its own inventory/damage mechanics may need an integration hook. Test your exact server modpack before putting valuable land at risk. The supplied verification report records which cases were actually exercised. No live multiplayer visual review or arbitrary-mod compatibility audit is implied.

## Build and verify

Build with Java 25: `gradlew build`. Public dependency JARs are in local `libs/` during development and are not bundled into the release. Fetch the matching versions listed above if building a source-only copy.

`gradlew test runIntegration` runs the rules suite and a disposable localhost test server. `runIntegration` needs a development `run-test/server.properties` and accepted `run-test/eula.txt`; it shuts down automatically and fails the Gradle task if its result file does not report success. The test source set is excluded from the release JAR.

On this Windows machine, verification uses an ASCII-only temporary checkout/cache because Java/Gradle worker paths containing `Ç` failed to load classes. Source changes are made in this project and copied into that isolated test checkout. The release JAR is copied back only after verification.


The parachute comes from a converted CC0 model, uses vanilla wool textures and has configurable size, colors and sway. See [the model/conversion notes](PARACHUTE_MODEL.md).
