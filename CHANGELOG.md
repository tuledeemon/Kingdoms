# Changelog

## 1.12.1

- Golden Crown now uses red Unique rarity; existing crowns and configuration update automatically.
- Remove `_Umut` player-head items from online player inventories and the cursor once per second. No world scans; placed heads, drops and container contents stay unchanged.

## 1.12.0

- Replace Golden Crown locator packets with wearer-owned, per-life health progression: +4 HP per unique player kill, capped at +20 HP by default. Heal newly gained capacity; do not count repeat/self kills. Use native player kill attribution, persist unique victims immediately and reset on death.
- Keep the crown bound in the helmet slot while alive, including creative inventory packets, armor clicks, shift/drop/hotbar swaps, right-click replacement and direct native equipment/inventory removal. Native death drops remain allowed.
- Make both crown tiers natively Unbreakable, including existing crowns when refreshed, and add persistent glowing while worn. Keep gold appearance, Diamond/Netherite armor stats, compatible enchantments, upgrades and staff-only issuance.
- Save a dedicated native max-health modifier so reconnecting preserves both maximum and current health. Clear that modifier and saved progress on death. Keep progress on the wearer rather than transferring it with the item.
- Remove the locator implementation and packet tests; keep legitimate Strength potions unchanged. Add configurable `artifacts.crownHealthPerUniqueKill` and `artifacts.crownMaxBonusHealth`, with legacy-save defaults.
- Add unique-kill, repeat/cap, survival/creative inventory lock, infinite durability, native save reload and death-reset coverage; update the guide.

## 1.11.3

- Replace Golden Crown's worn Strength effect with a wearer-only locator bar. Keep Diamond/Netherite stats, golden appearance, compatible enchantments, upgrades and staff-only event issuance.
- Send native waypoint packets directly while the world locator gamerule is off; do not toggle the world rule or change receive/transmit attributes. Leave globally enabled locator behavior intact without duplicate markers. PacketEvents remains optional.
- Use separate marker IDs so cleanup cannot remove ordinary vanilla waypoints. Respect dimension, death, spectator, sneaking, invisibility and transmission range; send direction-only markers for distant targets and replay tracks for marker-type/icon changes.
- Clear private markers on unequip, logout, respawn, spectator mode, configuration disable and shutdown. Rebuild sessions after player/world changes and avoid sending play packets during protocol reconfiguration. Preserve legitimate Strength potions.
- Refresh existing crown lore automatically and document the new effect. Add native packet encode/decode and recipient/cleanup checks.

## 1.11.2

- Remove the `FLUIDS` kingdom flag and migrate its legacy settings without changing other kingdom flags. Pouring buckets requires Place permission; collecting liquid requires Break permission. Cauldron bucket interactions use the corresponding permission.
- Use Towny-style ownership rules for liquid flow: allow same-owner and wilderness flow, including claim-to-wilderness flow; block wilderness-to-claim and different-owner flow at peace. Flow does not borrow the original placer's permissions. Existing active-war behavior and special core/AirDrop restrictions remain.
- Add global `preventFluidGriefing` (default true) for flow and dispenser-bucket boundaries. Retire `protectWildernessFromClaimMachines`; normal machine exports into wilderness are allowed.
- Permit outward piston extension into wilderness while retaining ownership checks on retraction. Keep complete-event validation and accepted animation completion from 1.11.1.
- Check the selected dispenser item before consumption: forbidden buckets stay in the dispenser, while ordinary projectile dispensing is not blocked by a blanket border rule. Dropper/hopper inventory protection remains.
- Update guides, examples, migration checks and native fluid/bucket/dispenser/piston regression coverage.

## 1.11.1

- Remove `PISTONS` and `AUTOMATION` from kingdom flags, settings, command values and new-kingdom defaults. Load legacy flags safely in configuration, kingdom saves and transaction snapshots without resetting other settings.
- Remove the claim-border redstone signal filter. Observers and other power sources work across chunk/ownership borders.
- Preflight complete piston events before their first state change, retaining ownership, core, AirDrop and war-placement protection. Scope accepted positions through base retraction, block movement and native animation completion so claim mutation checks cannot leave invalid moving-piston data on an ordinary piston.
- Keep same-kingdom machines enabled without toggles and retain hopper/dispenser ownership checks and locked reward-chest protection.
- Add real observer/sticky-piston cycles across chunk borders and claim/wilderness borders, plus legacy-save migration checks. Update the guides and example configuration.

## 1.11.0

- Add Legendary Runner's Boots (Speed I), King's Chestplate (Regeneration I), Archer's Bow (two arrows), Advanced Axe (connected trees) and Advanced Pickaxe (connected ores) to the uncapped artifact AirDrop pool. Preserve overall artifact/fixer chances and existing type settings during the one-time catalogue expansion.
- Add a Mythic, staff-command-only Golden Crown. It uses vanilla gold-helmet item/worn assets with native Diamond Helmet stats, compatible enchantments and durability; the existing three-ingot upgrade gives Netherite Helmet stats while preserving its gold appearance.
- Worn effects preserve stronger/longer potions, clean up owned hidden effects, and use native regeneration cadence. Archer's Bow retains native ammo/enchantment rules, makes its bonus projectile uncollectable and uses native per-projectile wear.
- Advanced harvesting is bounded/configurable, checks loaded chunks only, follows ordinary player mining for each block, respects claims/cores/AirDrops and stops on tool break. Track direct block placement to avoid treating player constructions as trees/veins; natural growth is not marked as placement.
- Fix Void Scythe on shield-blocked hits and on successful hits without health loss. Block spear stabs/contact and direct native item-start paths during the lock, add visible native cooldowns to held/swapped items without shortening longer cooldowns, and verify expiry with actual attacks.
- Expand runtime and migration tests and update player/admin guides and example configuration. No resource pack or client mod is required.

## 1.10.2

- Require placed cores, including captured cores, to have sky access. After five continuously covered minutes, move the marker to the highest suitable block in the same column. Glass, leaves and liquids count as cover. Create stone above an unsuitable or reserved top block; warn and retry if build height prevents this.
- Notify owner/holder mayors when covered, moved or blocked, and remind online mayors on joining. Save coverage timers across restarts, reset them when uncovered/moved/carried, expose configurable enforcement and timeout, and show coverage in the core menu. Check only already-loaded chunks.
- Preserve authenticated artifact identities and abilities in crafting-grid repairs, anvils and grindstones, including artifacts in the right/second input. Keep ordinary enchantment-removal rules and the spear's built-in Wind Burst.
- Block merging two artifacts so neither identity or ability is discarded. Keep normal materials, compatible books and Artifact Fixers working.
- Raise Void Scythe's default native durability from 10 to 1,000. Migrate old default configuration and saved identities once, including pending transactions; preserve other custom values and native item wear percentages.
- Exercise native recipe assembly and actual crafting-table, anvil and grindstone output clicks in both orders, with real Vampire ability activation after repair.

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
