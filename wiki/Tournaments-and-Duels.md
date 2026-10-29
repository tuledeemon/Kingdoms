# Tournaments and 1v1s

Kingdoms Combat supports hosted 1v1 duels and single-elimination 1v1 or 2v2 tournaments. One arena runs one match at a time. Open `/tournament`, or `/event` while a tournament is active, for sign-ups, brackets, your Elo and the leaderboard. Menus, dialogs, titles and boss bars use the same small-caps style as Kingdoms.

## Set up the arena

Arena setup requires `kingdomscombat.admin` or the configured OP level. Recognized staff must enable `/staffmode` to regain those permissions. Tournament hosting uses `kingdomscombat.eventhost`.

Use `/eventhost arena` for the setup menu, or these commands:

```text
/eventhost arena pos1
/eventhost arena pos2
/eventhost arena claim
/eventhost arena spawn 1
/eventhost arena spawn 2
/eventhost arena setlocation
/eventhost arena kit
/eventhost arena gate add
/eventhost arena gate remove
```

1. Select opposite corners using your current block positions, then claim the cuboid. The selection automatically includes the floor block below the lower corner and two blocks of headroom above the higher corner. Existing arena selections receive this padding once on load. Include the walls and spectator areas you want protected. The arena must be in wilderness; kingdom creation, expansion and outposts cannot overlap its chunks afterward.
2. Stand at the first spawn facing the opponent and set spawn 1. Do the same for spawn 2. Each spawn needs two clear blocks above solid ground. Doubles place the teammate 1.2 blocks to the side, so leave enough space.
3. For kit mode, equip the exact kit in your inventory, armor and offhand, then save it. The template is copied without consuming staff items. Tracked artifacts, core tokens, fixers and storage items cannot be copied into kits.
4. Look at each gate block within six blocks and add it. Gates must be breakable, solid blocks without inventories or fluids. Repeat for all gate blocks. They remain closed during preparation, disappear without item drops when the countdown ends, and return after the match.

Setup is locked while a tournament is active or inventories still need recovery. `/eventhost arena unclaim` releases the arena when idle.

**Banners never grant a shield.** If the selected loadout has no shield, no item is added or replaced. Existing shields are decorated in their original inventory/main-hand/offhand slots; enchantments, durability, custom names and all other non-pattern properties are preserved.

## Choose the gear mode

Kit mode remains the default. Hosts can choose personal equipment instead:

```text
/eventhost tournament open 1v1 "Own gear cup" own
/eventhost tournament open 2v2 "Own gear doubles" own
/eventhost tournament gear own
/eventhost tournament gear kit
```

The optional final `own` or `kit` argument selects that tournament's mode. Without it, the saved default applies. `tournament gear` changes an event while applications are open; when no event is active, it saves the default for future tournaments and `/eventhost duel`. The host menu has the same gear switch. Once the tournament starts, the mode is locked. Own-gear mode needs the arena and spawns, but does not need an admin kit.

Special equipment is supported in both modes. Kit mode safely stores original equipment and its persistent wearer state while the admin kit is used; own-gear mode keeps it usable. Original equipment and wearer state return after the match or restart recovery. Normal equipment restrictions still apply during player interactions.

Personal equipment is taken from each fighter's inventory at the start of their match, including armor and offhand. The complete original inventory is saved first. Original components, durability and consumable counts return afterward, so arena supplies and wear do not permanently consume personal equipment. Artifacts can be used during own-gear fights. Their saved identities, ability charges and cooldown state are restored with the original items, even if an artifact breaks during the match. Artifact fixers and kingdom core tokens remain safely in the backup and return afterward. Banners decorate existing shields in either loadout without adding a shield.

Every match clears status effects, fire, freezing and absorption; fills health, air, hunger and saturation; and resets hunger exhaustion during preparation and again immediately before gates open. Original health, hunger and effects are restored after the match along with the inventory. Ordinary equipment bonuses may apply again through their normal mechanics.

## Public arena teleport

Stand on safe spectator ground and run `/eventhost arena setlocation`, or use **Set public teleport spot** in the admin menu. During a tournament, `/eventhost setlocation` updates this same arena point and requires administrator permission.

Players open `/event` and click **Teleport to arena**. This moves them to the configured viewing point without granting fighter permissions. Combat, war, nearby AirDrop locks and staff freezes still apply, and active fighters cannot use it to leave their match. Queued arrivals recheck the event, destination and restrictions after chunk loading.

## Host a direct 1v1

```text
/eventhost duel PlayerOne PlayerTwo
```

Both players must be online, alive in Survival, out of staff mode, unfrozen and clear of combat/war/AirDrop restrictions. Their original inventory is saved before they receive the kit or their backed-up personal loadout, according to the saved gear mode. A held banner is optional for staff-picked fighters; if present, it customizes shields already in their kit.

The default countdown is ten seconds. Announcement titles/chat respect `eventTitles` and `eventChat`; sounds respect the global sound settings. Fighters cannot move, fight or break gates during it. At zero, the gates open and the fight begins. Titles, sounds, chat and a boss bar identify the match.

## Tournament sign-ups

```text
/eventhost tournament open 1v1 "Saturday duels"
/eventhost tournament open 2v2 "Doubles cup"
```

For **1v1**, choose Apply in `/tournament`, or use `/tournament apply`. **A banner is optional.** Holding one copies its color and patterns onto shields already in your kit without consuming the banner. With an empty hand or another item, your selected loadout is unchanged.

For **2v2**, the captain runs `/tournament invite Teammate` and the teammate uses `/tournament accept`. Both must be online, eligible and unregistered when accepting. **Neither player needs a banner.** Each may optionally hold one when the team application is accepted to customize their existing kit shields; otherwise their selected loadout is unchanged.

Use the Applicants button or `/tournament entries` to view application order. Staff can remove an entry with `/eventhost tournament remove PlayerName` while applications are open.

Players can withdraw while applications are open with `/tournament leave`. Withdrawing removes the complete team. Using it while fighting forfeits the player's participation.

Staff can enter fighters manually:

```text
/eventhost tournament add PlayerOne
/eventhost tournament add Captain Teammate
```

When entries close, the **earliest power-of-two number of complete entries** is kept. For example, 6 solo entrants becomes 4, and 7 doubles teams becomes 4 teams. The latest excess entries are excluded and notified. There are no byes. At least two complete entries are required.

## Brackets and hosting

Set any opening-round pairings manually, using registered captain names:

```text
/eventhost tournament bracket CaptainOne CaptainTwo
/eventhost tournament start
```

Starting closes applications, excludes late excess entries and auto-pairs entrants not already in manual pairings. A manual pairing involving an excluded late entry is removed; its retained opponent returns to the automatic pool. Winners advance through a fixed single-elimination bracket. Waiting matches run sequentially, with a fifteen-second interval by default.

```text
/eventhost tournament
/eventhost tournament next
/eventhost tournament winner CaptainName
/eventhost tournament stop
```

The hosting menu provides dialogs for choosing fighters and manual opening pairs, plus start, next, stop and current-match winner buttons. `next` resumes a paused bracket or replays a voided match. `winner` adjudicates the current match and updates Elo. `stop` cancels the unfinished match without an Elo result and restores original inventories. `/eventhost stop` also stops the active tournament.

## Arena permissions and combat

Only the **current match's active, non-eliminated fighters** can place/break blocks or damage their opponents. Registration alone grants no permissions. Waiting entrants and spectators cannot build, interfere with PvP or access arena containers. Teammates cannot damage one another. Staff/admins can manage the area, but cannot damage an official fighter to change the result.

The configured cuboid remains protected between events. Unrelated environmental terrain changes and explosion block drops are blocked there. Water and lava can flow within the arena; neither can flow across its boundary. Their sources, spread and resulting terrain changes are restored after the match. Set `arenaFluidFlow` to false to disable this flow. Fighter changes to ordinary blocks are tracked and restored between matches. Block entities and storage blocks cannot be created or destroyed by fighters. The configured change cap bounds the reset work.

Teleport commands and other unapproved commands are blocked during matches. Combat mobility can teleport a fighting player within the arena, but cannot teleport them out. Leaving its bounds forfeits participation. Opponents cannot teleport into an active arena from elsewhere.

Lethal damage is handled as an arena defeat: the player stays alive, no kit items or XP drop, and their original items, armor, offhand, health, hunger, effects, experience and location are restored. In doubles, the remaining teammate continues fighting; an eliminated player has no fighter permissions. Dropping arena items, collecting outside items, equipping props and opening storage (including the private event chest) are blocked. Temporary projectiles expire when their match ends, including those in unloaded chunks; arrows cannot be collected outside the arena.

**Disconnecting forfeits the entire current side.** The remaining side advances. Arena participants are exempt from the ordinary combat-log death while their original inventory is being recovered. The original inventory is restored before logout when possible, otherwise on reconnect. A saved receipt prevents restoration from duplicating old items.

The default fight limit is ten minutes. A timeout voids the unfinished match, restores inventories and pauses the bracket for staff to replay it; it does not award arbitrary Elo. Server shutdown/restart also voids an interrupted match, preserves the bracket, restores items and leaves the tournament paused for `/eventhost tournament next`.

## Elo

There is **one shared individual rating** for duels and doubles. Default starting Elo is 1,000, K is 32 and the rating scale is 400. Equal-rated sides exchange 16 points. Upsets award more and expected wins award less. Doubles use each team's average Elo, applying the same result change to both teammates. Ratings never fall below zero. Only resolved matches count; applications, exclusions, voids and restarts grant no points.

Use `/tournament elo` for the leaderboard. Placeholders:

```text
%kingdomscombat:elo%
%kingdomscombat:arena_fighting%
%kingdomscombat:tournament_name%
```

## Voice chat and staff mutes

The optional [Simple Voice Chat integration](https://dev.modrepo.de/minecraft/voicechat/api/getting_started) targets Fabric 26.2 Simple Voice Chat 2.6.22. It is installed separately. Current-match players, admins/staff and permitted event hosts can use voice inside the arena. Waiting entrants and spectators cannot speak, but can hear staff/hosts through normal voice-chat distance and group routing. They cannot hear fighters or other spectators. Set `arenaSpectatorsHearStaff` to false to also block staff audio for spectators. Current players can talk during the countdown. Staff mutes also block microphone/group speech server-wide, while allowing muted players to listen outside restricted spectator areas. Expiry and `/staff unmute` restore speech.

No extra client mod is needed for the tournament features. Voice audio naturally requires the server's existing voice-chat client support. A graphical client and live microphone connection were not used in the automated verification.

## Configuration

Settings live in `config/kingdoms-combat/config.json`. Scalar values can be changed with `/combatadmin set <key> <value>`; list values are edited in JSON, then `/combatadmin reload`.

| Setting | Default |
|---|---|
| `arenaEnabled` | `true`; disabling stops an active tournament safely |
| `arenaCountdownSeconds` | `10` |
| `arenaBetweenMatchesSeconds` | `15` |
| `arenaMaxFightSeconds` | `600` |
| `arenaAutoNext` | `true`; disable to have staff start each match |
| `arenaBuildAllowed` | `true`; only active current fighters qualify |
| `arenaMaxTeams` | `64` complete entries |
| `arenaMaxGates` | `512` blocks |
| `arenaMaxChangedBlocks` | `4096` unique changed positions per match |
| `arenaMaxRegionVolume` | `4000000` blocks |
| `arenaMaxRegionChunks` | `256` intersecting chunks |
| `arenaEloStart`, `arenaEloK`, `arenaEloScale` | `1000`, `32`, `400` |
| `arenaVoiceRestricted` | `true` |
| `arenaSpectatorsHearStaff` | `true`; spectators hear staff/hosts but cannot speak |
| `arenaFluidFlow` | `true`; permit water/lava flow within arena bounds |
| `staffMuteVoice` | `true` |
| `arenaAllowedCommands` | `event`, `tournament`, `combat`, `msg`, `tell`, `reply`, `r` |

The arena, kit, saved gear-mode default, event gear mode, gates, ordered entries, brackets, shared Elo and pending inventory backups are saved at `<world>/kingdoms-combat/arena.json`. Do not remove that file while an inventory restore is pending. Chunk tickets are temporary, limited to the configured arena, and released after matches.
