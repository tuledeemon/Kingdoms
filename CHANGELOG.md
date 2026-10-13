# Changelog

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
