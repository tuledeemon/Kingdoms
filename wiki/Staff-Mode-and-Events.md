# Staff Mode and Events

These features belong to the optional **Kingdoms Combat addon**. This page covers staff permissions, moderation tools, event hosting, the `/event` dialog, permissions and settings. For PvP timers and teleport restrictions, see [Combat and Teleports](https://github.com/tuledeemon/Kingdoms/wiki/Combat-and-Teleports).

## Staff mode

Staff use **`/staffmode`** to enable their staff permissions, moderation tools and combat-log exemption. It requires **`kingdomscombat.staffmode`**, with the configured `adminLevel` (OP level 2 by default) as the fallback when no permission is assigned. An explicit LuckPerms denial overrides that fallback. Permissions are checked again while the exemption is used.

Enabling it clears all your combat, war and AirDrop timers. Hits involving a player in staff mode do not create PvP tags, and logging out in staff mode does not cause an addon logout death. It does not provide damage immunity or change Kingdoms claim protection. Staff mode also bypasses war and AirDrop teleport restrictions, including their cooldown tails, HuskHomes teleports and the `/event` teleport button.

Run `/staffmode` again to return to normal combat rules and default-group permissions. Turning it off closes the moderation menu, removes creative/spectator mode and disables creative flight. Enabling staff mode itself does not change your game mode. The toggle resets on disconnect or server restart. A logout penalty already recorded before entering staff mode still recovers normally; use the existing admin pardon command to intentionally forgive one.

The **`%kingdomscombat:staffmode%`** placeholder returns `true` while authorized staff mode is enabled, otherwise `false`.

## Permissions while staff mode is off

By default, staff accounts use **only the LuckPerms `default` group's effective permissions** while staff mode is off. Personal grants, staff groups, donor permissions attached to that staff account, wildcards and OP command levels do not carry over. `/staffmode` checks the real staff permission separately, so authorized staff can still turn it on.

Turning staff mode on restores the account's original effective permissions. Assigned groups, permission nodes, prefixes and role-sync data are never deleted or rewritten. Default-group inheritance, denials and context rules continue to apply. Ordinary players keep their normal rank permissions.

An account counts as staff if it is OP or has `kingdomscombat.staffmode`, `kingdomscombat.admin` or `kingdomscombat.staff.tools`. Event-host access alone does not make an account staff. Once recognized, an account stays subject to the off-mode limit for that online session, including after permission revocation.

**Requires LuckPerms Fabric 5.5.85+ for Minecraft 26.2, installed separately.** This release uses its permission-cache integration; it does not bundle LuckPerms. Mods that authorize actions through unrelated custom rank systems are outside LuckPerms permission checks.

## Suspicion review

Open **Most suspicious players** in `/staff`, or use `/staff flags`, to review a ranked list of recent clicking and repeated movement/farming flags. Click a player’s head for the evidence. Staff lists and detail headers now use each player’s own head. These checks only notify staff and record observations; they do not punish or block gameplay. See [Suspicion flags and staff review](https://github.com/tuledeemon/Kingdoms/wiki/Suspicion-Flags) for checks, settings and review commands.

## Moderation tools

1. Enable **`/staffmode`**.
2. Open **`/staff`** and choose an online player.
3. Choose an action. Warnings, punishments and notes use Minecraft input dialogs for their reason and duration.

All player-operated tools recheck active staff mode and the individual permission at execution. An old menu or dialog cannot act after staff mode is disabled. The addon adds no vanish, teleport, bring, inventory-inspection or ender-chest-inspection tools.

| Command | Purpose |
| --- | --- |
| `/staff` | Open the online-player moderation menu |
| `/staff freeze <player>` | Freeze a player for review |
| `/staff unfreeze <player>` | End the review; also works offline |
| `/staff warn <player> <reason>` | Send and record a warning |
| `/staff mute <player> <duration> <reason>` | Temporary or permanent chat mute |
| `/staff unmute <player>` | Remove a mute |
| `/staff kick <player> <reason>` | Disconnect a player with a reason |
| `/staff ban <player> <duration> <reason>` | Add a native Minecraft ban |
| `/staff unban <player>` | Remove the native ban |
| `/staff notes <player>` | Read action history and notes |
| `/staff note <player> <reason>` | Add a staff-only note |
| `/staff chat <message>` or `/sc <message>` | Message staff who currently have staff mode enabled |

Punishment durations accept `30m`, `1h30m`, `7d`, decimal amounts such as `1.5h`, or `permanent`. Names and UUIDs are accepted. Offline names must have been seen by the addon; a UUID is also accepted. New offline punishments require cached target permissions so immunity can be checked; otherwise apply them while the player is online or use the server console. Reversals and notes remain available offline. Console moderation does not require an in-game mode and can override target immunity.

### Freeze and chat rules

A frozen player cannot move, fight, take damage, use items, change inventory contents, break/use blocks or teleport. They can still chat unless muted. Freezing clears their combat tag so review logout cannot cause a combat-log death. The frozen location and review survive reconnects and restarts. Logging out is recorded and announced to active staff; it does not automatically ban the player. Staff can end an offline review with `unfreeze`.

Mutes block normal chat plus the configured `mutedCommands` list, including namespaces and Brigadier redirect aliases. Add any custom messaging commands provided by other mods to this list. A mute prevents chat delivery without breaking signed-message processing. Mute and ban deadlines use real time and continue through downtime.

Kicks and bans do not pardon existing combat penalties: a target currently in normal PvP combat follows the existing logout-death setting. Freeze first when conducting a protected review.

### Tool permissions and immunity

Every tool requires active staff mode. Tools are allowed to an authorized active staff member by default; explicitly set a node to `false` to disable that tool for a role. Grant `kingdomscombat.staffmode` only to trusted staff. The `staff.tools` classification node alone does not authorize entering staff mode.

Tool nodes use the `kingdomscombat.staff.` prefix and these suffixes: `menu`, `freeze`, `warn`, `mute`, `kick`, `ban`, `notes`, `chat`, `flags`. `freeze` covers unfreeze, `mute` covers unmute and `ban` covers unban. `notes` covers both reading and adding notes.

An explicit `kingdomscombat.staff.immune=true` protects a player from new punishments. Active staff are protected too; staff cannot punish themselves. Reversing a punishment and adding a note do not require the target to lose immunity.

```text
/lp group moderator permission set kingdomscombat.staffmode true
/lp group moderator permission set kingdomscombat.staff.ban false
/lp group administrator permission set kingdomscombat.staff.immune true
```

Review state, mutes, known names and bounded per-player history live in `<world>/kingdoms-combat/moderation.json`. A separate append-only audit file, `staff-audit.jsonl`, records the actor, target UUID, action, reason and timestamp. Vanilla bans also persist in the server's normal ban file. Back up these files with the world.

## Hosted events

An event host can announce a server-wide event with a duration:

```text
/eventhost setlocation
/eventhost description Fight in teams. Last team standing wins.
/eventhost "PvP Tournament" 10m
```

`setlocation` saves your current position, dimension and facing direction. You can use it before starting an event or to update its location while it runs. If no location was previously set, starting an event in-game uses the host's position. A console host must have a location set by a player before players can join.

The event **starts immediately and lasts ten minutes** in this example. Everyone receives a small-caps title/subtitle, chat announcement and sound, with a boss bar showing the name and remaining time. Players who join the server later receive the active boss bar too. The bar disappears when the timer ends or a host stops the event.

Players use **`/event`** to open a Minecraft dialog showing the staff-written description, host, total duration and remaining time, with a **Teleport to event** button. You can read the event information while combat tagged; the button is blocked during PvP combat, active-war restrictions, and AirDrop restrictions, including their remaining cooldown tails. These checks also run when a destination finishes loading, so getting tagged while waiting cannot bypass protection. Authorized staff with staff mode enabled bypass all these teleport restrictions. This built-in check applies independently of the command blacklist. Buttons from an old or ended event cannot teleport you to a different event.

Hosts set or update the description with `/eventhost description <text>`, before or during the event. The default limit is 2048 characters, configurable up to 4096. Text uses small caps in-game.

```text
/eventhost                   # Show current event status
/eventhost "Building Contest" 1h30m
/eventhost stop              # End the current event early
/event                      # Read details and choose whether to teleport
```

Use quotes for names with spaces. Durations accept seconds without a suffix, or `s`, `m`, `h`, and `d`; examples include `30s`, `10m`, `1.5m` and `1h30m`. The default maximum is one day, configurable up to seven days. Only one hosted event runs at once. Events do not automatically create arenas or change PvP rules. The optional input chest below handles turn-in rewards.

Events and their selected locations persist in `<world>/kingdoms-combat/event.json`. Their real-time countdown continues through downtime. A host logging out does not stop an event.

### Event input chest and rewards

Configure the chest **before starting** the event. Place an ordinary chest and look at it within six blocks, then open **`/eventhost chest`** for the setup menu. Select the chest, add held stacks as requirements/rewards, set cash through the input dialog, and toggle the two claim rules. The main `/event` dialog shows the chest's coordinates, requirements and rewards.

The same settings are available through commands:

```text
/eventhost chest set                  # Select the chest you are looking at
/eventhost chest required             # Add the exact stack in your main hand as a requirement
/eventhost chest reward               # Add the exact held stack as an item reward
/eventhost chest money 10k            # Cash per claim; 0 disables cash
/eventhost chest onceperplayer true   # false allows repeat turn-ins
/eventhost chest endafterclaim true   # First successful claim ends the event
/eventhost chest clearrequired
/eventhost chest clearrewards         # Clear item rewards and cash
/eventhost chest disable              # No input chest for future events
/eventhost "Collection challenge" 30m
```

Held stacks are **templates**, not consumed from the host. Rewards are generated by the authorized host's configuration. Up to nine required stacks and eight reward stacks are supported, with cash in addition. Components such as enchantments, names and potion contents must match. Unique Kingdoms artifacts, cores and fixers cannot be templates. Normal event-host permissions apply, including staff-mode permission restrictions.

Players right-click the selected chest. **Top row:** their private deposits. **Middle row:** requirements. **Bottom row:** item rewards and the emerald Claim button, which shows the cash amount. Click or shift-click a stack in your inventory to deposit it; click deposited stacks to withdraw them. Only matching requirements are accepted. Dragging, dropping from the menu and extracting previews are blocked. Cash goes to the claimant's wallet; normal kingdom tribute still applies.

Click **Claim** once the requirements are present. Only the required quantities are consumed. If anything is missing or the reward will not fit, nothing is consumed. `onceperplayer` defaults to **true**; `endafterclaim` defaults to **false**. The settings are independent: allow repeats while the event runs, or end it after its first successful claim. Two players cannot win a first-claim event simultaneously.

Closing the menu or ending an event does not delete deposits. Use **`/event chest`** to reopen them; after their event ends, old deposits can be recovered anywhere. During an active event, turn-ins require being within eight blocks of the physical chest. If an old event still holds your items, recover those first before using a new event's chest. Physical chest contents and hoppers are separate from each player's private input menu.

Plans, private inputs, completed claims and pending cash are saved in `<world>/kingdoms-combat/event-chests.json`. A pending `event-chest-transaction.json` is a recovery journal: keep it with the world backup rather than deleting it. Verified player-save receipts prevent item duplication or repeated cash delivery after an interrupted save. Full wallets leave cash pending for automatic retry. Chest setup is copied when the event starts; stop the event before editing it.

### LuckPerms permissions

Install LuckPerms Fabric **5.5.85+** for 26.2; it includes the matching Fabric Permissions API. Permissions use the official [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api/blob/master/USAGE.md); the addon does not bundle a permission manager.

| Node | Access |
| --- | --- |
| `kingdomscombat.staffmode` | Toggle staff mode |
| `kingdomscombat.eventhost` | Start/stop events and set their location |
| `kingdomscombat.admin` | Combat admin commands and menus |
| `kingdomscombat.bypass` | Existing explicit combat/war/AirDrop teleport exemption |

For example:

```text
/lp group staff permission set kingdomscombat.staffmode true
/lp group eventhost permission set kingdomscombat.eventhost true
```

Event hosts do not need OP, staff-mode access or combat-admin access when their own node is granted. Protected commands use `adminLevel` as the OP fallback when their node is unassigned; this release requires LuckPerms. Explicit `false` assignments override OP fallback. Staff accounts must enable staff mode before using their restored event/admin grants. Permission node names are configurable.

## Placeholders

Use Text Placeholder API with the `kingdomscombat` namespace.

| Placeholder | Returns |
| --- | --- |
| `%kingdomscombat:staffmode%` | Whether authorized staff mode is enabled |
| `%kingdomscombat:frozen%` | Whether the player is frozen for a review |
| `%kingdomscombat:muted%` | Whether a current mute is active |
| `%kingdomscombat:event_active%` | Whether a hosted event is running |
| `%kingdomscombat:event_name%` | Small-caps hosted event name; empty when inactive |
| `%kingdomscombat:event_remaining%` | Event seconds remaining, rounded up |

## Configuration

Settings are shared with the combat addon in `config/kingdoms-combat/config.json`. Use `/combatadmin reload` after editing, or `/combatadmin set <key> <value>`. Configuration access requires `kingdomscombat.admin`, independently of event-host access.

### Permission settings

| Setting | Default / meaning |
| --- | --- |
| `staffModePermission` | `kingdomscombat.staffmode` |
| `eventHostPermission` | `kingdomscombat.eventhost` |
| `adminLevel` | `2`; OP fallback when a protected command node is unassigned |

### Moderation settings

| Setting | Default / meaning |
| --- | --- |
| `staffDefaultPermissionsWhenOff` | `true`; limit recognized staff to the default group when off |
| `staffDefaultGroup` | `default`; source group for off-mode permissions |
| `staffToolPermissionPrefix` | `kingdomscombat.staff.`; tool and immunity node prefix |
| `staffActionBar` | `true`; show active staff-mode status once per second |
| `staffReasonMaxLength` | `512`; reason/note limit, 1–2048 characters |
| `staffNotesLimit` | `100`; retained history entries per player, 1–1000; audit file remains complete |
| `staffTeleportTimeoutSeconds` | `15`; asynchronous frozen-position recovery timeout, 1–120 seconds; adds no teleport tool |
| `mutedCommands` | `me`, `say`, `tell`, `msg`, `w`, `teammsg`, `tm`, `whisper`, `message`, `reply`, `r`; edit the JSON list for custom chat commands |

Moderation and permission restrictions operate independently of the combat `enabled` toggle. A config reload applies to permission checks immediately.

### Hosted event settings

| Setting | Default / meaning |
| --- | --- |
| `eventMaxSeconds` | `86400`; maximum duration, configurable from 1–604800 seconds |
| `eventTeleportTimeoutSeconds` | `15`; destination-loading timeout, 1–120 seconds |
| `eventDescriptionMaxLength` | `2048`; description limit, 1–4096 characters |
| `eventBossBarColor` | `purple`; one of pink, blue, red, green, yellow, purple or white |
| `eventColor` | `light_purple`; Minecraft text color |
| `eventBarTemplate` | `event • {name} • {time}` |
| `eventTitles`, `eventSounds`, `eventChat` | `true`; announcement channels |
| `eventStartMessage`, `eventEndMessage`, `eventStopMessage` | Custom announcement text, supporting `{name}`, `{host}` and `{time}` |

Event sounds also respect the global `sounds`/`soundVolume` settings and use `tagSound`/`endSound`. Event hosting is independent of the combat system's `enabled` toggle.

## Tournaments and voice moderation

See [Tournaments and Duels](TOURNAMENTS_GUIDE.md) for arena setup, kits, gates, applications, manual brackets and Elo. Staff mutes also silence Simple Voice Chat microphone/group speech when the optional integration is installed; `staffMuteVoice` controls it. Arena spectator voice restrictions are separate and configurable.
