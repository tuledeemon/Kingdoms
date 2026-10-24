# Verification

The final source was compiled with Java 25 and tested on an isolated Minecraft 26.2 Fabric server using Loader 0.19.5, Fabric API 0.160.0+26.2 and the real EconomyCraft 1.10.0 API.

**Result for 1.7.1: 545 dedicated-server checks without C2ME, 546 with C2ME 0.4.1-beta.1.0+26.2, and 102 JUnit tests passed.**

## Covered cores, artifact combining and durability — 1.10.2

**122 unit tests and 634 dedicated-server checks passed**, with C2ME 0.4.1-beta.1.0+26.2, Fabric Loader 0.19.5, Fabric API 0.160.0+26.2 and EconomyCraft 1.10.0. No graphical client or production server was used.

- Actual crafting-table, anvil and grindstone output clicks preserve the artifact's authenticated record and consume the expected ingredients, with either input order. Repaired Vampire Swords still heal through real damage events.
- Two-artifact repair combinations return no result and consume neither input; ordinary-item repairs remain ordinary. Existing compatible enchantment, fixer, smithing restrictions and built-in Wind Burst/grindstone XP checks pass.
- Void Scythe survives 999 native durability damage and breaks at 1,000. Old 10-durability item stacks preserve their damage fraction when refreshed. Configuration/record migrations preserve IDs, nonces, drop odds, retired status and later customizations.
- Real terrain checks cover transparent roofs, five-minute timing, timer persistence, resetting when uncovered, same-column relocation without terrain duplication, stone above water/bedrock, build-height failure and retry, carried-core exemption, disabling enforcement, and captured-core tribute preservation. The test fixture first used the wrong height ceiling; correcting it to the version's inclusive maximum verified the actual build-height boundary. Coverage uses loaded chunk heightmaps and never requests chunk generation.
- Full claim, war, alliance, beacon, core, AirDrop and artifact regression checks completed and the server stopped normally.

## Wind Burst Spear target regression

- Diamond and Netherite artifact spears reject armor stands, mannequins, minecarts, chest minecarts, boats, chest boats, item frames, glow item frames, paintings, dropped items, falling blocks, End Crystals, interaction entities, block displays, arrows and fireballs.
- Real ordinary attacks and main/offhand stabs cannot damage, knock back or trigger Wind Burst on those targets, and do not consume hit durability.
- A real piercing jab skips an armor stand and damages the living mob behind it. Charged contact excludes the armor stand before contact/cooldown bookkeeping; a mob holding the artifact also cannot stab it.
- Player and mob targets remain eligible, native jabs still deal damage, living dragon hitboxes remain eligible, and ordinary Diamond Spears retain vanilla prop targeting.

## Chunk-loading freeze regression

- The provided crash report identifies AirDrop orphan cleanup calling a blocking world lookup inside the chunk-load event while C2ME is upgrading the same chunk. The callback now queues provided chunk references without loading or mutating terrain. Normal polling drains only already available chunks; unload and shutdown discard queued references.
- A fixture inserts a persisted orphan in a real load callback before Kingdoms runs. The callback returns, performs no cleanup mutations during loading, and normal polling removes the orphan after availability. With the exact C2ME version from the report, the check verifies the full chunk has not yet been published during the callback.
- Marker protection/access lookups and deferred cleanup do not force an unpublished synthetic chunk to load. Existing ordinary chests retain inventory; expired/orphan markers clean up without loot spills.
- Both isolated servers finish the full gameplay regression suite. A live client join on the production modpack was not performed. See [reproduction and installation notes](CHUNK_LOAD_FIX.md).

## AirDrop checks

- All default vanilla loot templates parse with the actual Minecraft 26.2 item/potion/enchantment registries. 960 generated crates across four tiers and eight variants enforce progression locks, unique reward IDs, legal stack sizes and at most one selected rare reward type.
- Default timing, real-world day boundaries, online thresholds, variants and rare tables match the approved replacement loot specification. Unit tests cover exclusive progress, stronger-group takeovers, solo strength three, ties preserving control, kingdom/solo durations, nonstacking contest penalties, grace deadlines and legacy timer/default migration.
- Landing uses just the candidate surface column. Steep surrounding terrain, forest canopy and soft vegetation are accepted; claims, spawn and its protected square corners, exclusions and wet landing surfaces remain rejected. An actual random spawn search finds a site and starts an event. Newly searched chunks load before height sampling.
- Two crates descend independently for ten minutes. A vanilla display/interaction pair and twelve glowing display edges follow the controlled trajectory without persisting duplicate entities. All edges clean up; the outline can be disabled. The converted parachute uses 192 bounded displays with a round striped fabric canopy, lining, seams, rim/vent borders, knots and eight continuous ropes. Actual saved native metadata preserves its rotations, scales and wool materials. The complete assembly shares bounded sway and settles precisely onto its landing coordinates. Every panel and rope is removed during cleanup; disabling parachutes also disables sway. Unit checks validate rope endpoints and unchanged crate attachments at scales 0.25, 1.5 and 3. Invalid/air material IDs are rejected before spawning. The offline geometry preview was inspected.
- The default five-block build-protection radius includes the exact boundary and blocks towers above it. Actual nearby placement/breaking and environmental terrain changes outside it remain allowed. The radius can differ from capture range; zero reserves only landing/support. OPs obey protection. New claims cannot absorb a live objective. Explosion terrain filtering stays separate from combat damage.
- Real kingdom membership drives quorum and exclusive control. Four actual kingdom members can challenge solo strength three with zero inherited progress; unit tests verify a continuous 15-second takeover delay, interrupted challenges, pauses and tie handling; three retain a tie; a solo takes over from two members. Membership changes cannot transfer solo progress. Exact ±3-block height boundaries are tested. Boss-bar packets remain vanilla.
- Capturers and chest looters receive native glow. Client team packets use the closest vanilla color to the core RGB, or black for solo players; actual scoreboard memberships are preserved. Cleanup removes transient glow and preserves pre-existing tags and actual glowing potion effects. Disabling glow works, and transient glow does not persist in player saves.
- A configurable dust ring matches capture radius and exact core RGB, and emits a full ring of vanilla particle packets. Solo control switches its color to black.
- Capture winners record an immutable authorized roster. Another crate remains independent. Original loot and schedule survive reload; offline time is not credited and expired grace resets progress.
- Each landed drop creates a physical chest. Its persistent marker survives actual block-entity save/load. Pre-capture and enemy access are denied; winners must remain within eight blocks. `/airdrop` supplies information without remote loot collection.
- Vanilla chest menus show actual stack counts and collect through the durable reward ledger. Unauthorized access, full inventories, repeated clicks, hotbar swaps, clone/throw inputs and expired menus cannot consume or duplicate rewards.
- Captured chests and their supports reject breaking, explosions, hopper transfer, pistons and fluids until cleanup. Expiry/cancellation removes marked chests without spilling loot; ordinary chests are preserved and orphan markers clean up on chunk load.
- Actual vanilla player saves contain both inventory and delivery receipts. A reconstructed crash after the player save commits a reserved reward without giving it twice. A pre-delivery reservation is fulfilled once on recovery.
- Thirty-minute expiry applies to uncollected winners' loot as well as uncaptured crates. Collected stacks remain delivered; no ItemEntity spill is created. Cancellation preserves already-earned rewards with their original deadline.
- Admin commands spawn selected variants at explicit safe coordinates, optionally already landed; a separate command lands descending crates immediately. Unknown variants are rejected and manual starts bypass the online minimum.

## Artifact and cash checks

- All eight replacement artifact templates parse against Minecraft 26.2 with short lore and authentic identities. Guaranteed default triggers, phase drop odds, uncapped issuance, type settings, one-time legacy catalogue/identity migration, retired identities and nested transaction records are covered by the rules suite.
- A forced artifact roll replaces ordinary rewards with one genuine artifact and optional two individually tracked fixers. Failed rolls fall back to normal loot. An actual artifact crate includes no cash by default.
- Combat checks exercise actual-damage Vampire healing at 25/50%, overkill clamping, real Frostbite freezing, Void held-item locks and cancellation of raised shields, fully blocked Thorns reflection, and real Thunder lightning. A fully charged vanilla critical attack triggers Thunder; non-critical attacks do not. Lightning retains player attribution.
- Multi Pickaxe runs actual player mining for the 2×2 and centered 3×3 squares, checks every face orientation, skips bedrock and stops at another kingdom's protected claim boundary. Only successfully mined blocks spend durability. Normal mining owns loot and enchantment handling; the code adds no custom or unobtainable drops.
- Guardian Angel grants immunity through actual game-mode right-click use, spends one charge, does not drain while held, and permits its last charge. Real outbound block/chunk packets retain harmless per-player web masks, server cobweb slowing is cancelled, and expiry restores the real blocks. Existing web slowdown clears on activation. This is server-side packet/physics verification, not a graphical-client movement or anticheat review.
- Vanilla durability damage survives refresh and item serialization. Void Scythe breaks and retires its identity at ten durability. Thunder upgrades from 512 to 768 durability, preserves wear percentage and enchantments, and retains free storm durability during ability cooldowns. Diamond Spear upgrades to Netherite with native Wind Burst II. Grinding preserves its built-in enchantment without awarding repeatable XP.
- A real vanilla anvil previews and completes native durability fixer repairs with configured XP, consumes one fixer and saves its output. Normal diamond repair and compatible Mending, Unbreaking, Efficiency and Silk Touch books are accepted without losing identity. Guardian charge repair previews do not mutate live records. Guardian has no Netherite upgrade.
- Physically duplicated artifacts lose their abilities. Charges and retired identities survive disk reload; old removed types retire without invalidating the save. Actual admin gives remain uncapped.
- AirDrop/artifact scalar, threshold, day/chance, per-type and variant commands validate and save settings. Money aliases and percentage input work; invalid values leave the running configuration unchanged. Saved configuration is read back from disk.
- Kingdom cash credits a shared treasury once across different collectors. Solo cash pays its wallet once through the economy journal. A captured kingdom pays twenty-percent tribute on the reward.
- The actual winners-only chest renders cash in its deterministic random slot; full-inventory collection does not issue a money token. Reopening retains collected holes and other reward positions. A reconstructed crash after the treasury credit completes the reserved reward without paying twice; the receipt is present on disk.

## Existing Kingdoms regression checks

- Highlight geometry covers all twelve cube edges with 92 unique dust positions. Display metadata contains twelve thin, correctly scaled glowing rods in the saved kingdom color, with vanilla spawn/metadata packets and no world entities or block changes.
- Kingdom colors persist through storage round trips, old saves receive a stable bright color, and invalid colors and highlight range settings are rejected.
- War glow is absent at peace, becomes eligible during active war and stops after expiry. Unit checks cover preparation, captured cores following the host's war, and carried cores.
- Wartime display metadata uses a 60-thousandth-block frame and the maximum finite float viewing range. Core highlighting adds no action-bar directions or distances.
- Core theft is rejected at peace and during preparation, then succeeds through a real block break during active wartime. Securing a token marks an existing home block and starts captivity; stale copies cannot capture twice.
- Real wartime explosions preserve marked cores without creating duplicate drops; a real piston cannot move them. Core gravity and TNT entity conversion are rejected, and unbreakable blocks cannot trap a core's reserved return location.
- Core storage rejects ordinary clicks, shift clicks, hotbar/offhand swaps, dragging, bundles and hopper insertion/collection.
- Real EconomyCraft sell credits, positive balance setting, incoming payments and treasury withdrawals route exactly 20% to the captor, with fractional credits accumulating correctly and withdrawals taxed once.
- Core holders cannot buy war protection. Timed-out recovery returns a core to its former captor without independence, invalidates its old token, and restores the previous block when needed. Returning a recovered token home grants independence.
- The mayor's selection flow designates the right-clicked existing block; an unbreakable founding center is rejected.
- Captured-kingdom menu clicks open a core-specific Change block option. Only the holding kingdom's mayors may relocate secured cores within their own claims; preparation/war, occupied core positions, air and unbreakable blocks are rejected. The original owner, tribute recipient, tax remainder, kingdom color and both existing blocks survive relocation. Changes persist, and stale selections or wars beginning mid-selection cannot move the core.
- Leaving a captured kingdom immediately ends membership and tribute; kicking is immediate, rejoining the same kingdom is blocked until the persisted cooldown expires.
- Real EconomyCraft deposits and withdrawals; missing/granted withdrawal permission; withdrawal permissions remain enforced during war.
- Money inputs accept exact compact and written aliases such as `1k`, `1.5m`, `2 million` and `1bil`; invalid, fractional-result and overflowing values are rejected.
- Kingdom Beacon Speed II and Haste apply together to members with Level II priced at exactly four times Level I; both stop in wilderness and return in claimed land.
- Opponent powers are rejected during war preparation; Mining Fatigue applies to active enemy members in claimed land during wartime and stops in wilderness.
- Beacon purchases persist in kingdom state, validate on load, use treasury funds, and clean up with kingdom disbanding.
- Chest-menu shift clicks, hotbar swaps, ordinary button clicks and extraction prevention.
- Outsider block placement/destruction, mayor building, and disabled fire/fluid settings applying to mayors.
- Operators bypass ordinary claim permissions while disabled fire, fluid and explosion flags still apply.
- Normal remote expansion is rejected; a purchased outpost can anchor disconnected land and consumes six claim slots by default.
- Creation, ordinary claims and outposts share the configured overworld spawn no-claim radius; an outpost inside the default radius is rejected with a spawn-specific message.
- Block-destruction rejection before item drops; disabled fire rejection before TNT priming.
- A real protected explosion, then a real enemy explosion destroying blocks during war.
- Actual fluid spreading and piston movement across a border, including enabled local fluid/piston flags.
- Hopper/dispenser boundary decisions, border-chest access, and cross-border chest automation decisions.
- Bucket emptying with a disabled flag and with an unauthorized player even when fluids are enabled.
- Unapproved mob spawns, farm animals, claimed-animal damage, harmful potions, approved mobs and immediate removal of unapproved entrants.
- Protected war preparation, active reciprocal war access, neutral-player rejection, end-of-war protection restoration, and cooldown retained after kingdom recreation.
- The admin cooldown command rejects non-admin sources, unknown kingdoms, preparation and active wars. Console use clears the named kingdom's war cooldown/protection and its current members' persisted war cooldowns. Other kingdoms, their members, their protection and rejoin restrictions remain unchanged; a cleared member cooldown stays cleared when recreating a kingdom.
- Actual command-tree packets expose only the player `/kingdom` root and hide `/k`, while admin trees retain subcommands and argument nodes. Server suggestion packets hide player subcommands/arguments and retain admin and vanilla suggestions. Manually typed player commands still execute and open menus; non-admin trees omit admin commands.
- The generated defaults include the 16-chunk creation buffer and Automation enabled for new kingdoms.
- The generated defaults include a 16-chunk overworld spawn no-claim radius and 24 hours of automatic war protection for new kingdoms.
- Fabric loaded Placeholder API 3.1.0-beta.1 and the server-only mod successfully with the icon object-component integration present.
- Registered PlaceholderAPI checks verify the styled kingdom name, real paper/diamond atlas objects, item hover data, live identity changes, small-caps `ɴᴏɴᴇ` without membership/player context, removal of tag commands/placeholders, and the combined leaderboard component without tags.
- All 482 selectable vanilla non-block items are checked for a mapped atlas object and item hover data. Registered kingdom, territory, ranked-icon, styled and combined-leaderboard placeholders are exercised with multi-frame/model-alias items. Shield GUI silhouette and unknown-client-asset paper fallback are checked separately.
- Unpaid upkeep, warning creation, one mayor acknowledging for all mayors, debt repayment and grace-expiry disbanding.
- Recovery of an interrupted completed debit exactly once and rejection of ambiguous transaction recovery.

## Rules suite

Strict-majority thresholds, mayor eligibility, protection/cooldown on both sides, protected preparation versus active vulnerability, second-war rejection during preparation, war permissions and expiry, treasury permissions during war, disabled flags applying to mayors, individual grants, identity defaults, legacy tag-field compatibility, Automation defaults, spawn-radius defaults and chunk-distance boundaries, starter-protection defaults, config validation, persistence round-trip/overlap detection, warning incidents, connected claim shapes, outpost topology, weighted claim slots, beacon pricing, persisted beacon-effect validation, fractional tribute arithmetic without overflow, and core location/captor validation.

## Release checks

- Leaderboard entries include town-center X/Y/Z and dimension by default. Cores in transit label those coordinates as the last placed location; kingdoms without a center show "Not set". This tooltip change was compiled but not visually reviewed in a graphical client.
- Workspace source hashes matched the tested temporary checkout before packaging.
- Release metadata targets only the server environment and Minecraft 26.2.
- Integration/JUnit test classes are absent from the release JAR.
- `tools/generate-icon-sprites.ps1` resolves model branches and inherited texture references against the exact 26.2 client JAR. Every emitted item sprite has an existing texture in the items atlas; the shield sprite is checked in the GUI atlas. The release bundles the identifier mapping, without image assets or client code.
- `verification/icon-sprites-audit.json` records checks for all 608 generated mappings, the exact client JAR hash and the mapping hash.
- `verification/release-manifest.json` records the release SHA-256 and runtime versions.
- Full server output and JUnit XML are in `verification/`.

## Scope

Dust/glow geometry and display metadata were verified on the dedicated server. Their appearance in a connected graphical client, and live frame cleanup on player movement/respawning, were not visually reviewed. Broadcasting glow without a distance cutoff does not bypass the vanilla client's requirement for compiled/visible terrain sections. No chunks are force-loaded for highlighting.

Artifact identities authenticate abilities in offline inventories and storage without quantity caps; inspect/retire can invalidate deliberately removed items. Native item stacks own tool durability. These checks do not constitute an exhaustive exploit audit. Some mechanisms were checked through the shared protection decision, while others were exercised through actual Minecraft operations as identified above. The UI and Guardian Angel effect were not reviewed through a connected graphical client or your exact anticheat/modpack. Test the actual server modpack before relying on the claims for valuable terrain. Ownerless cross-border effects are handled conservatively because Minecraft does not always identify the player who initiated them.

The test server bound to localhost and shut down after the assertions. Its world, players, settings and integration mod are not part of the release.
