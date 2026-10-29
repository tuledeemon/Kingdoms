# Playtime Discord roles and balance leaderboard

These server-side features are included in Kingdoms Combat 1.5.0 for Fabric 26.2. Keep Kingdoms, EconomyCraft 1.10.0 and the addon's existing dependencies installed. No client mod is needed.

## Discord role after playtime

Keep **Discord Integration 3.2.1** installed with Minecraft/Discord account linking configured. The addon reuses that connection; it does not need another bot token. Run this as console or an authorized administrator in staff mode:

```text
/combatadmin playtimerole 4 <role_id>
```

This atomically enables the reward, sets four hours and saves the role ID. Use the numeric Discord role ID, not its name. The threshold and role are configurable. To stop future grants:

```text
/combatadmin set playtimeRoleEnabled false
```

The bot needs **Manage Roles**, and its highest role must be above the reward role. Managed roles and `@everyone` cannot be used. The adapter adds only the selected role and preserves all other roles. These requirements follow [JDA's role-assignment API](https://docs.jda.wiki/net/dv8tion/jda/api/entities/Guild.html#addRoleToMember(net.dv8tion.jda.api.entities.UserSnowflake,net.dv8tion.jda.api.entities.Role)).

Eligibility uses Minecraft's saved `PLAY_TIME` statistic: 20 ticks count as one second. Existing recorded playtime counts, including AFK time included by Minecraft. Players need not start earning time again after installing the feature. Offline players are checked on their next login. Eligible online players enter a bounded background queue; Discord calls never run on the Minecraft server thread. Unlinked accounts wait until linking succeeds; unavailable Discord connections and failed role grants are retried. Already-present roles do not cause duplicate grants. Role checks are repeated periodically, so a missing earned role can be restored. This feature grants roles and does not remove an earned role after relinking or a playtime/configuration change.

| Setting | Default | Meaning |
| --- | --- | --- |
| `playtimeRoleEnabled` | `false` | Enable configured role grants; setup command enables it |
| `playtimeRoleId` | empty | Discord reward role ID |
| `playtimeRoleHours` | `4` | Required recorded playtime, including fractional hours |
| `playtimeRoleCheckSeconds` | `300` | Time before rechecking a completed/unlinked account |
| `playtimeRoleRetrySeconds` | `900` | Retry delay after unavailable Discord or a failed request |
| `playtimeRoleQueueLimit` | `20` | Maximum queued/in-flight players |

Changes use `/combatadmin set <key> <value>`, or edit `config/kingdoms-combat/config.json` then run `/combatadmin reload`. Queued work for superseded settings is skipped. A request already sent to Discord can finish.

## Player wallet leaderboard

```text
/baltop
/baltop 2
```

The addon replaces EconomyCraft's `/baltop` with a small-caps chest menu. It ranks player wallets, including offline accounts stored by EconomyCraft, with player heads, names, positions, balances, previous/next pages and your own balance/rank. This is separate from the kingdom treasury leaderboard.

The board uses a shared cached balance snapshot and sorts it on a background worker. Repeated opens and refresh clicks share that cache. It reads EconomyCraft's in-memory account data, without scanning player files or changing balances. Names use EconomyCraft's cached/asynchronous name resolution. Unknown names fall back to a known server name or UUID. Your displayed wallet balance is current; rank/order use the cached snapshot. Ties use UUID order for stable positions.

Players may use the public command by default. Deny `kingdomscombat.baltop` with LuckPerms to restrict it. Console `/baltop` prints the ten richest stored wallets. The replacement takes over the root command after registration; other EconomyCraft commands are preserved.

| Setting | Default | Meaning |
| --- | --- | --- |
| `baltopCacheSeconds` | `30` | Shared snapshot refresh interval |
| `baltopMaxEntries` | `10000` | Maximum ranked/displayed accounts |
| `baltopPageSize` | `28` | Entries per page, maximum 28 |
