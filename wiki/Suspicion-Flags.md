# Suspicion flags and staff review

Kingdoms Combat 1.4.1 adds server-side observations for unusual clicking and repeated movement/farming routines. It requires no client mod. These are **review flags, not proof of cheating**: no check cancels actions, kicks, bans, freezes, teleports or changes player items.

## Open the review menu

Enable `/staffmode`, then open `/staff` and select **Most suspicious players**, or run:

```text
/staff flags
/staff flags PlayerName
/staff flags alerts
/staff flags clear PlayerName
```

`/staff flags` ranks players by recent observations. Each new flag contributes 10 review points, decreasing linearly to zero over `antiCheatRankingHours` (24 hours by default). More recent repeated observations rank higher; this score is a review priority, not a probability that someone cheated. Click a player's head to inspect check names, evidence, timestamps and coordinates. Offline players remain reviewable. The full history button also includes older retained flags.

Player lists, player detail headers, ranked entries and history cards use actual player profiles. Recorded profiles preserve known skins for offline review; no synchronous skin lookup runs in the menu.

`/staff flags alerts` toggles live alerts for your current session. Alerts require active staff mode. Logging out resets the toggle. Detection keeps running while alerts are muted. `/staff flags <player>` also works from console and prints recent evidence; console `/staff flags` prints the latest saved observations. Clearing a player's saved history additionally requires admin permission and is logged.

## What the checks observe

**Fast clicking:** repeated windows of main-hand swings while recently attempting to attack a living entity. Defaults require more than 24 clicks per second over three separate 128-click windows. Attack damage cooldowns do not erase attempted swings. Air swings without recent attack context, block mining, held-use actions, severe packet bursts and short timing windows are excluded.

**Click timing:** very consistent swing intervals across several independent windows, at a minimum click rate. Defaults require an interval standard deviation at most 1.2 ms, at least 4 CPS and three windows lasting at least eight seconds each. Input that mostly arrives on exact 50 ms boundaries is skipped because client tick scheduling can create that pattern normally. Arrival timestamps are recorded when packets are constructed, before server-tick batching.

**Slow attack timing:** a separate check for repeated attacks at 0.5–2 CPS, including exactly one hit per second. It requires three independent 64-interval windows with interval deviation no greater than 3 ms. At one hit per second this takes about three minutes of consistently repeated timing. Long breaks, mining, held-use actions, lag and ordinary timing variation interrupt or fail the evidence window. Tick-aligned input is eligible here because the check requires multiple long windows; it remains only an observation for human review.

**Four CPS is not a cheating cutoff.** It is the minimum speed for the separate faster timing check. Slow attack timing works independently and is enabled when upgrading an existing configuration.

**Repeated action loops:** substantially identical cycles of movement, turns, selected slot and attacks/block uses/successful mining. Defaults sample every ten server ticks and require five cycles of 24–120 samples each, matching at least 98% of samples. Each cycle also needs movement, several turns, varied states and performed actions. Standing idle, moving in a straight line or simply holding an input is insufficient. Teleports, dimension changes, riding, flying with an elytra and arena countdowns reset/exclude the relevant samples. Artifact-assisted mining excludes the nearby movement/mining sequence.

Timing observations reset after a server tick exceeding `antiCheatMaxTickMs`, followed by a grace period. Sample memory is bounded per online player. No ore-location scans or packet obfuscation are performed.

Human routines can resemble automation, and clients/network conditions can hide automation or create misleading patterns. Staff should review the actual behavior and other evidence before taking action. There is no automatic connection from flags to moderation punishments.

## Permissions

- Enter staff mode: `kingdomscombat.staffmode`, with the existing OP fallback.
- Review menus, history and live alerts: `kingdomscombat.staff.flags`. Authorized active staff are allowed by default; explicitly deny this node to hide access.
- Clear history: the flag permission plus `kingdomscombat.admin`/configured admin OP fallback.
- Exclude a player from observations: `kingdomscombat.anticheat.bypass` (default false).

Active staff, creative/spectator players, dead players and frozen review targets are excluded. Existing staff-mode off/default-group permission rules continue to apply.

## Configuration

Settings are in `config/kingdoms-combat/config.json`. Change scalar values with `/combatadmin set <key> <value>`, or edit the file and run `/combatadmin reload`. Reloading resets in-progress sample windows. For example:

```text
/combatadmin set antiCheatEnabled true
/combatadmin set antiCheatMaxCps 28
/combatadmin set antiCheatMacros false
/combatadmin set antiCheatRankingHours 48
```

| Setting | Default | Purpose |
| --- | --- | --- |
| `antiCheatEnabled` | `true` | Enable the observer |
| `antiCheatClicks` | `true` | Enable fast-click and click-timing checks |
| `antiCheatMacros` | `true` | Enable repeated movement/action-loop checks |
| `antiCheatLog` | `true` | Write evidence to the server log |
| `antiCheatAlertCooldownSeconds` | `120` | Minimum time between flags for the same player/check |
| `antiCheatHistoryLimit` | `500` | Maximum total saved observations |
| `antiCheatRetentionDays` | `14` | Saved history retention |
| `antiCheatClickSamples` | `128` | Intervals per independent clicking window |
| `antiCheatClickWindows` | `3` | Consecutive suspicious windows required |
| `antiCheatSlowTiming` | `true` | Enable independent slow-attack timing analysis |
| `antiCheatSlowSamples` | `64` | Intervals in each slow timing window |
| `antiCheatSlowWindows` | `3` | Consecutive slow timing windows required |
| `antiCheatSlowMinCps` | `0.5` | Slow timing minimum rate |
| `antiCheatSlowMaxCps` | `2` | Slow timing maximum rate |
| `antiCheatSlowStdMs` | `3` | Maximum slow interval deviation |
| `antiCheatMaxCps` | `24.0` | Fast-click threshold |
| `antiCheatRegularMinCps` | `4.0` | Minimum rate for timing regularity |
| `antiCheatRegularStdMs` | `1.2` | Maximum interval deviation for timing regularity |
| `antiCheatMacroSampleTicks` | `10` | Movement/action sampling interval |
| `antiCheatMacroMinPeriod` | `24` | Shortest cycle in samples |
| `antiCheatMacroMaxPeriod` | `120` | Longest cycle in samples |
| `antiCheatMacroRepeats` | `5` | Required repetitions |
| `antiCheatMacroMatch` | `0.98` | Required matching fraction |
| `antiCheatRankingHours` | `24` | Review-score decay/ranking window |
| `antiCheatMaxTickMs` | `120` | Server-tick duration that resets timing observations |
| `antiCheatLagGraceSeconds` | `5` | Pause after detected server lag |

Evidence is saved at `<world>/kingdoms-combat/flags.json`, periodically and at shutdown. Observation samples are temporary and restart empty. If history cannot be read, the original file is preserved and observations continue in memory; write failures retain pending evidence for retry. Detector failures are logged and allowed gameplay continues.
