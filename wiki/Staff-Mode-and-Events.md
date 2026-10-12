# Staff Mode and Events

These features belong to the optional **Kingdoms Combat addon**. This page covers staff exemptions, event hosting, the `/event` dialog, permissions and settings. For PvP timers and teleport restrictions, see [Combat and Teleports](https://github.com/tuledeemon/Kingdoms/wiki/Combat-and-Teleports).

## Staff mode

Staff can use **`/staffmode`** to toggle their combat-log exemption. It requires **`kingdomscombat.staffmode`**, with the configured `adminLevel` (OP level 2 by default) as the fallback when no permission is assigned. An explicit LuckPerms denial overrides that fallback. Permissions are checked again while the exemption is used.

Enabling it clears your existing PvP tag. Hits involving a player in staff mode do not create PvP tags, and logging out in staff mode does not cause an addon logout death. It does not provide damage immunity or change Kingdoms claim protection. War and AirDrop teleport restrictions still apply.

Run `/staffmode` again to return to normal combat rules. The toggle resets on disconnect or server restart. A logout penalty already recorded before entering staff mode still recovers normally; use the existing admin pardon command to intentionally forgive one.

The **`%kingdomscombat:staffmode%`** placeholder returns `true` while authorized staff mode is enabled, otherwise `false`.

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

## Placeholders

Use Text Placeholder API with the `kingdomscombat` namespace.

| Placeholder | Returns |
| --- | --- |
| `%kingdomscombat:staffmode%` | Whether authorized staff mode is enabled |
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
