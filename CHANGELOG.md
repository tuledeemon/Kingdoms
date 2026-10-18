# Changelog

## 1.10.1

- Added Glowing I to opponent beacon powers, with a default 300,000 treasury fee, normal 24-hour duration, active-war purchase eligibility and claimed-land-only application. It combines with other powers, including Mining Fatigue.
- Expanded the opponent menu to four effect buttons. Existing price maps receive Glowing through a one-time migration; customized prices remain, and later removal of its entry stays disabled.
- Vanilla tracking limits and team colors apply; outlines are visible to all viewers tracking the player. No underground/surface building restrictions were introduced.
- Updated code, configurations and guides for Fabric 26.2.

## 1.10.0

- Added `/kingdomadmin war` with persistent add/remove/list/clear commands for instant-war blocks, disallowed placements/entities, cancelled explosions and explosion block damage. Registry IDs have admin tab completion; mayor menus cannot change these rules.
- Added dedicated pearl teleport and lava-casting toggles. Pearl source/impact positions are checked. Water/lava cannot generate cobblestone, stone or obsidian when casting is banned; normal manual block placement is unaffected.
- Default policy is empty and scoped to active-war claims. Admins may extend the scope to active participants anywhere or explicitly apply restrictions globally. Instant mining still additionally requires active war and preserves break permissions, core capture and vanilla tool/drop rules.
- Added source-specific TNT, TNT-minecart, other entity, bed/respawn-anchor and wildcard explosion controls. Terrain-only bans preserve entity damage; cross-border blast targets are checked separately. Operators respect admin policies.
- Added the War Restrictions guide and updated example configurations and wiki.
- Built for Fabric 26.2, with local and dedicated-server verification. Live client visuals and third-party compatibility remain unverified.

## 1.9.0

- Added saved, mutually approved alliances with requests, expiry, removal and a home menu button. Default limit: five allies; allied friendly fire is disabled without granting automatic claim access.
- Added opt-in joint wars: both sides recruit their direct allies during preparation, with explicit mayor approval. Default limit: four kingdoms per side. Protection, cooldown, online majority, opposing alliances and other wars are checked again on approval.
- Coalition membership drives opposing claim access, core capture, feedback, map details, combat-addon war lookup, freezes and cooldowns. Same-side physical permissions remain required. Hostile beacons offer per-enemy selection.
- Added alliance and war-side placeholders, player commands and configurable limits/toggles. Admin endwar ends the whole coalition war and updates every participant's/member's cooldown.
- Preserved old pair-war records and custom menu positions; the default Alliances button uses slot 40. Added the alliance guide and updated the wiki.
- Built for Fabric 26.2; local rule tests and integration-source compilation passed. Live client/server visuals and third-party compatibility were not tested for this release.

## 1.8.3

- Moved the kingdom beacon to slot 32: War Protection at 30, an empty slot at 31, then the beacon. The original menu layout otherwise remains.
- Updates previous stock positions automatically and removes the retired home Help slot from configuration. Custom arrangements remain configurable.
- Built for Fabric 26.2.

## 1.8.2

- Restored the original inventory menu layouts, navigation and pagination.
- Removed AirDrops, Artifacts and Help shortcuts from the kingdom home menu for members and nonmembers. Their commands remain available.
- Stock 1.8.1 home positions migrate back to the original positions; custom arrangements are preserved.
- Built for Fabric 26.2; live client appearance has not been verified for this release.

## 1.8.1

- Reorganized the main menu into centered management, war/powers and activity rows, with shared framed inventory layouts and bottom navigation.
- Settings now use two rows of four flags, green/red toggles, separate identity/permission controls and an isolated disband action.
- Member, kingdom, captured-core, AirDrop, active-power and leaderboard lists use 28 interior slots per page, page indicators and protected navigation rows. Invalidated pages clamp safely, and detail views return to their parent lists.
- Stock menu positions migrate once; custom positions remain intact. Filler items can be disabled with `menuDecorations` or customized through `menuItems` frame/background/warning_frame keys.
- Built for Fabric 26.2. Live Minecraft client appearance has not been verified for this release.

## 1.8.0

- Added optional BlueMap integration: kingdom-colored territory fills, merged borders, holes and disconnected outposts, clickable kingdom information, and placed-core pins.
- Automatically refreshes saved kingdom data and restores markers after BlueMap reloads. Generates geometry off the server thread without loading world chunks.
- Added validated `blueMap` settings and `/kingdomadmin bluemap set <key> <value>`, with controls for appearance, map selection and public popup details.
- BlueMap remains optional and is not bundled. Includes the 1.7.2 explosion-protection patch.
- Compiled against BlueMap API 2.7.7; live BlueMap/server behavior has not been verified for this release.

## 1.7.2

- Moved explosion terrain protection from the calculated position list to each block's explosion callback. Minecraft's ray tracing and other mods' explosion position lists are left untouched.
- Unclaimed terrain uses vanilla explosion damage except for active AirDrop protection and marked cores. Protected blocks are checked before drops, chain-priming TNT or other block effects occur.
- Kept claim explosion flags, actor permissions, wartime rules, core protection and AirDrop chest protection. No gamerules or configuration migration are required.
- Built for Fabric 26.2. This patch has not been verified on the affected server; no unit tests or test-server runs were performed for this release.

## 1.7.1

- Wind Burst Spear attacks now require a living player or mob. Armor stands, mannequins and nonliving entities are ignored.
- The restriction applies to ordinary hits, piercing jabs and charged contact, in either hand and with the Netherite upgrade. Mob-held artifact spears follow the same rule.
- Ignored targets take no damage or knockback, cannot trigger Wind Burst, and consume no hit durability. Piercing attacks can still hit a living target behind an ignored prop.
- Living Ender Dragon hitboxes remain valid. Ordinary spears keep their vanilla targeting behavior, and kingdom/PvP protection still applies.
- Updated the player guide and artifact catalogue with the targeting rules.
