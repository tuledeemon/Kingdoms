# Combat and Teleports

This is an optional **Kingdoms Combat** addon. These rules apply only when the server has installed it. Values below describe its default settings; staff can change them.

## Checking your status

Use `/combat` to open a status menu. It shows your timers, opponents, whether teleport commands are available, and whether logging out would kill you.

The action bar normally shows a small-caps line such as **ᴄᴏᴍʙᴀᴛ › 20ꜱ**, appended to the normal Kingdoms territory display. Active war and nearby AirDrops use the same countdown style, with their own labels and colors. When several restrictions apply, the displayed number is the longest remaining timer.

## PvP combat

A successful player hit tags both the attacker and victim for **20 real-time seconds**. Player-owned projectile hits count too. Fully blocked shield hits also count by default. Self-damage, mob attacks and claim-protected/failed hits do not start a PvP combat tag.

**Logging out while combat tagged kills you.** Disconnects, timeouts and kicks also count. The death follows Minecraft's inventory/experience rules, including the server's `keepInventory` setting. Normal server shutdowns do not punish players by default.

Each opponent has a separate timer. If you kill the only opponent keeping you tagged, combat ends. Killing one opponent while another fight remains clears only the dead opponent's timer. Staff can change this to clear the killer's whole combat tag on any opponent kill.

## Staff mode

Staff can use **`/staffmode`** to toggle their combat-log exemption. It requires **`kingdomscombat.staffmode`**, with the configured `adminLevel` (OP level 2 by default) as the fallback when no permission is assigned. An explicit LuckPerms denial overrides that fallback. Permissions are checked again while the exemption is used.

Enabling it clears your existing PvP tag. Hits involving a player in staff mode do not create PvP tags, and logging out in staff mode does not cause an addon logout death. It does not provide damage immunity or change Kingdoms claim protection. War and AirDrop teleport restrictions still apply.

Run `/staffmode` again to return to normal combat rules. The toggle resets on disconnect or server restart. A logout penalty already recorded before entering staff mode still recovers normally; use the existing admin pardon command to intentionally forgive one.

The **`%kingdomscombat:staffmode%`** placeholder returns `true` while authorized staff mode is enabled, otherwise `false`.

## Movement items

While you already have an active timer, these successful actions refresh **your** applicable timers:

- Throwing an ender pearl.
- Using a wind charge.
- Receiving your spear's actual **Lunge enchantment impulse**.

The use itself does not refresh an opponent's timer. A normal PvP hit still tags both sides. Failed/cooldown-blocked item uses do not count, and movement items used while safe do not create a fresh combat tag.

## Hosted events

An event host can announce a server-wide event with a duration:

```text
/eventhost setlocation
/eventhost description Fight in teams. Last team standing wins.
/eventhost "PvP Tournament" 10m
```

`setlocation` saves your current position, dimension and facing direction. You can use it before starting an event or to update its location while it runs. If no location was previously set, starting an event in-game uses the host's position. A console host must have a location set by a player before players can join.

The event **starts immediately and lasts ten minutes** in this example. Everyone receives a small-caps title/subtitle, chat announcement and sound, with a boss bar showing the name and remaining time. Players who join the server later receive the active boss bar too. The bar disappears when the timer ends or a host stops the event.

Players use **`/event`** to open a Minecraft dialog showing the staff-written description, host, total duration and remaining time, with a **Teleport to event** button. You can read the event information while combat tagged; the button is blocked during PvP combat, active-war restrictions, and AirDrop restrictions, including their remaining cooldown tails. These checks also run when a destination finishes loading, so getting tagged while waiting cannot bypass protection. Staff mode only removes the PvP restriction; war/AirDrop restrictions still apply. This built-in check applies independently of the command blacklist. Buttons from an old or ended event cannot teleport you to a different event.

Hosts set or update the description with `/eventhost description <text>`, before or during the event. The default limit is 2048 characters, configurable up to 4096. Text uses small caps in-game.

```text
/eventhost                   # Show current event status
/eventhost "Building Contest" 1h30m
/eventhost stop              # End the current event early
/event                      # Read details and choose whether to teleport
```

Use quotes for names with spaces. Durations accept seconds without a suffix, or `s`, `m`, `h`, and `d`; examples include `30s`, `10m`, `1.5m` and `1h30m`. The default maximum is one day, configurable up to seven days. Only one hosted event runs at once. This announcement system does not automatically create arenas, change PvP rules or manage event rewards.

Events and their selected locations persist in `<world>/kingdoms-combat/event.json`. Their real-time countdown continues through downtime. A host logging out does not stop an event.

### LuckPerms permissions

Install the matching Fabric LuckPerms build and its permission API. Permissions use the official [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api/blob/master/USAGE.md); the addon does not bundle a permission manager.

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

Event hosts do not need OP, staff-mode access or combat-admin access when their own node is granted. Without a permission provider, staff/event/admin commands fall back to `adminLevel`. Explicit `false` assignments override OP fallback. Permission node names are configurable.

## Wars

War preparation adds no automatic teleport restriction. Existing PvP combat or AirDrop restrictions can still apply during preparation.

Once the war becomes active, participating kingdom members receive a continuously refreshed **20-second war timer**, wherever they are. When the war ends, that timer counts down. Logging out with only a war timer does not kill you by default; it still blocks the configured commands.

Staff can also restrict visitors standing inside wartime territory by enabling `restrictWarTerritoryVisitors`.

## AirDrops

Being within **100 blocks horizontally** of an incoming or capturable AirDrop refreshes an AirDrop timer to **20 seconds**. The default radius covers the full height of that column, so climbing above the drop does not enable teleport commands.

Leaving the area starts the countdown. A claim by **any** winning side, cancellation, or expiry also stops refreshing it. The remaining tail then expires; looting a claimed chest does not start a new AirDrop timer.

Logging out with only an AirDrop timer does not kill you by default. Staff can change the radius, use spherical distance, apply a height limit, change the countdown, disable the rule, or enable logout death for this timer.

## Teleport commands

By default the following command roots are blocked while **any** timer is active:

```text
home phome publichome warp spawn
tp teleport tpo tphere tpoffline tpall
tpa tpahere tpaall tpaccept tpyes
rtp back hub lobby server
```

Arguments do not bypass a root rule: blocking `home` also blocks `/home base`. Namespaced versions and Brigadier redirect aliases are checked. Administrators can add command paths such as `myplugin travel` and wildcard rules such as `example:*`.

Commands such as `/sethome`, `/homelist`, `/tpdecline` and `/combat` remain available unless staff add them to the list. This addon does not disable vanilla movement, portals or the listed movement items.

With HuskHomes installed, its teleport/warmup/back/random callbacks also check the **player being teleported**. This catches requests accepted by another player, menu clicks, and a teleport started before the combat or area restriction appeared.

`huskHomesTeleportGuard` controls that extra execution guard independently. To deliberately allow a HuskHomes teleport command while tagged, staff must both remove its command rule and disable the broader HuskHomes guard.

## Placeholders

Use the installed **Text Placeholder API**. The namespace is `kingdomscombat`.

| Placeholder | Returns |
| --- | --- |
| `%kingdomscombat:tag%` | Styled small-caps countdown component; empty when safe |
| `%kingdomscombat:tagged%` | `true` if any restriction is active |
| `%kingdomscombat:combat_tagged%` | `true` for actual PvP/admin combat tags |
| `%kingdomscombat:remaining%` | Longest remaining timer in whole seconds, rounded up |
| `%kingdomscombat:remaining_formatted%` | Longest timer as `0:20` |
| `%kingdomscombat:combat_remaining%` | Combat seconds |
| `%kingdomscombat:war_remaining%` | War seconds |
| `%kingdomscombat:airdrop_remaining%` | AirDrop seconds |
| `%kingdomscombat:reasons%` | Small-caps active reason labels |
| `%kingdomscombat:opponent_count%` | Number of active opponent timers |
| `%kingdomscombat:opponents%` | Sorted small-caps online names, or UUIDs for offline opponents |
| `%kingdomscombat:can_teleport%` | Whether restrictions permit teleporting |
| `%kingdomscombat:logout_punished%` | Whether the current timers carry a logout penalty |
| `%kingdomscombat:staffmode%` | Whether authorized staff mode is enabled |
| `%kingdomscombat:event_active%` | Whether a hosted event is running |
| `%kingdomscombat:event_name%` | Small-caps hosted event name; empty when inactive |
| `%kingdomscombat:event_remaining%` | Event seconds remaining, rounded up |
| `%kingdomscombat:at_war%` | Current active-war exposure, without counting a leftover tail |
| `%kingdomscombat:airdrop_nearby%` | Current active AirDrop exposure, without counting a leftover tail |

The component can be placed in a tab list, scoreboard, hologram or other compatible formatter. Component-aware systems retain its colors; plain-text systems retain its text. No chat prefix is inserted automatically.

To hide the built-in combat action bar and use a custom display:

```json
"actionBar": false
```

The timers, command restrictions, logout penalties and placeholders continue working. `titles`, `chatNotifications` and `sounds` are separate display settings.

## Administrator commands and menus

Use `/combatadmin` for the menu. Duration, radius and text fields use Minecraft input dialogs; command rules can be added or removed there.

```text
/combatadmin reload
/combatadmin status
/combatadmin set durationSeconds 20
/combatadmin set warLockSeconds 20
/combatadmin set airDropLockSeconds 20
/combatadmin set airDropRadius 100
/combatadmin set actionBar false
/combatadmin commands list
/combatadmin commands add home
/combatadmin commands add myplugin travel
/combatadmin commands remove home
/combatadmin tag <online-player> [seconds]
/combatadmin clear <online-player-or-UUID>
/combatadmin pardon <online-player-or-UUID>
```

`clear` clears current tags and opponent links. If the player remains in a restricted war or AirDrop area, its timer is refreshed again. `pardon` also removes a pending logout-death record.

All scalar settings below support `/combatadmin set <key> <value>`. Settings validate before being saved; an invalid reload keeps the existing configuration. Command lists can also be edited in JSON. The default admin level is **2**.

## Configuration reference

Edit `config/kingdoms-combat/config.json`, then `/combatadmin reload`.

### Timers and automatic restrictions

| Setting | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Enable new tags, command restrictions and displays; already journaled logout penalties still recover |
| `durationSeconds` | `20` | PvP and manual tag duration |
| `warLockSeconds` | `20` | War timer and its tail |
| `airDropLockSeconds` | `20` | AirDrop timer and its tail |
| `warRestrictions` | `true` | Refresh during active war |
| `airDropRestrictions` | `true` | Refresh near active incoming/capturable drops |
| `restrictWarTerritoryVisitors` | `false` | Also restrict visitors inside wartime claims |
| `airDropRadius` | `100` | Distance from the landing site's center |
| `horizontalAirDropRadius` | `true` | Use horizontal distance; `false` selects a sphere |
| `airDropVerticalTolerance` | `-1` | Optional maximum Y difference; `-1` ignores height |
| `pollTicks` | `5` | Regular exposure/expiry checks; command checks also refresh immediately |
| `saveIntervalSeconds` | `5` | Flush changing area timers; combat hits, movement refreshes and logout journals save immediately |

Durations accept **1–86400 seconds**. Radius accepts **1–10000 blocks**. Polling accepts **1–20 ticks**.

### Combat and logout behavior

| Setting | Default |
| --- | --- |
| `tagAttacker`, `tagVictim` | `true` |
| `tagBlockedHits` | `true` |
| `tagCreativePlayers`, `tagSpectators` | `false` |
| `killEndsCombat` | `true` |
| `killClearsAllOpponents` | `false` |
| `resetOnEnderPearl`, `resetOnWindCharge`, `resetOnSpearLunge` | `true` |
| `killOnCombatLogout` | `true` |
| `killOnWarLogout`, `killOnAirDropLogout` | `false` |
| `punishDuringShutdown` | `false` |

### Commands and exemptions

| Setting | Default / meaning |
| --- | --- |
| `blockedCommands` | The roots listed above; accepts roots, command paths and wildcard patterns |
| `huskHomesTeleportGuard` | `true`; cancel restricted HuskHomes teleports and warmups independently of the command list |
| `operatorBypass` | `false`; OP status alone does not bypass these rules |
| `respectBypassPermission` | `true`; check Fabric Permissions API when available |
| `bypassPermission` | `kingdomscombat.bypass`; explicit permission exempts its holder |
| `adminLevel` | `2`; OP fallback for protected commands when their permission node is unassigned |
| `staffModePermission` | `kingdomscombat.staffmode` |
| `eventHostPermission` | `kingdomscombat.eventhost` |
| `adminPermission` | `kingdomscombat.admin` |

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

### Display and text

| Setting | Default / meaning |
| --- | --- |
| `actionBar` | `true`; show the built-in countdown |
| `appendToKingdomsActionBar` | `true`; preserve territory/war information and append the tag. `false` shows combat status alone while tagged |
| `actionBarIntervalTicks` | `20`; standalone HUD refresh interval, 1–100 ticks |
| `chatNotifications`, `titles`, `sounds` | `true`; first-tag/transition feedback |
| `broadcastLogoutDeaths` | `false`; optional extra small-caps announcement |
| `prefix` | `combat`; custom chat header, empty string removes it |
| `combatColor`, `warColor`, `airDropColor`, `errorColor` | `red`, `gold`, `aqua`, `red`; Minecraft formatting color names |
| `actionBarTemplate` | `{reasons} › {seconds}s` |
| `separator` | ` | `; between Kingdoms HUD and countdown |
| `tagSound`, `endSound` | Vanilla note-block `pling` and `chime`; feedback is sent privately to the affected player |
| `soundVolume` | `0.6`; 0–4 |

The message fields `blockedMessage`, `taggedMessage`, `safeMessage`, `warMessage`, `airDropMessage` and `logoutMessage` are also configurable. Text is converted to small caps in-game. Templates support `{seconds}`, `{time}`, `{reasons}`, `{player}` and `{opponents}` where applicable. `logoutMessage` uses `{player}` for its optional broadcast.

## Persistent state

Tags use real timestamps, so their timers keep counting through downtime. Normal shutdown preserves current tags and skips logout deaths by default. A logout penalty is journaled before the death, and its receipt is saved with the player's Minecraft data. On a later join, a saved receipt prevents repeating the same penalty; an interrupted penalty is recovered even during login immunity.

Back up `<world>/kingdoms-combat/` with the world and player data. Do not delete pending penalties to repair ordinary configuration errors; staff can use `/combatadmin pardon` when intentionally forgiving one.
