# Economy and playtime utilities

These server-side features are included in Kingdoms Combat for Fabric 26.2. The dynamic sell market requires 1.6.0 or newer. Keep Kingdoms, EconomyCraft 1.10.0 and the addon's existing dependencies installed. No client mod is needed.

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

## Dynamic sell market

Run `/sell`, click or shift-click inventory stacks into the top three rows, then close the menu to sell them. Clicking a deposited stack takes it back. The footer shows the estimated total; the chest button returns items and cancels, and the door button closes and sells. Cursor dragging, number-key swaps and creative cloning are intentionally blocked; the menu moves entire stacks directly so every transfer can be saved safely. Normal inventory clicking works without holding an item on the cursor.

Items with a custom name, including an anvil name or custom item-name component, cannot be sold. Kingdoms artifacts, fixers and core tokens are rejected as well. Containers with contents are never sold; EconomyCraft's item resolution and damaged-item restrictions still apply. Unrecognized or disabled prices are not accepted. Removing a price before checkout retains the deposited items for retrieval.

The included price list uses the supplied 1,695 starting prices with **all eleven leaf types reduced from $3 to $1 each**. Other starting prices remain unchanged. The file is copied into `config/kingdoms-combat/sell-prices.json` only when it does not already exist. Your EconomyCraft buy-price file is not overwritten.

Supply pressure is separate for each price key and shared by all players. Each item sold advances pressure; each item in a batch uses its own next marginal price, so splitting a batch into many sales does not increase its total payout at the same time. Prices recover with real elapsed time, including time while the server is offline. Pressure is calculated lazily, without a repeating scan of every priced item.

| Setting in `sell-settings.json` | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Enable the custom market |
| `blockInCombat` | `true` | Prevent opening while PvP combat tagged |
| `defaults.floorMultiplier` | `0.25` | Minimum price multiplier, from 0 exclusive to 1 inclusive |
| `defaults.volumeForFloor` | `16384` | Net items sold before reaching the floor, per item key |
| `defaults.recoveryPerHour` | `4096` | Items of pressure removed per real hour; zero disables recovery |
| `items` | `{}` | Per-price-key policy overrides |

Whole-unit prices round up with an absolute minimum of $1. Leaves therefore remain at $1 even when their supply pressure rises. Default pressure reaches the floor after 256 stacks of the same item and recovers completely from the floor in four hours with no further sales. Quotes are estimates until closing; another player's sale or a price edit can change the checkout total.

The administrator commands use `kingdomscombat.admin` and the existing staff-mode permission rules:

```text
/combatadmin market
/combatadmin market set volumeForFloor 16384
/combatadmin market set recoveryPerHour 4096
/combatadmin market set floorMultiplier 0.25
/combatadmin market set blockInCombat true
/combatadmin market price minecraft:oak_leaves 1
/combatadmin market price minecraft:diamond 420
/combatadmin market reset minecraft:diamond
/combatadmin market reload
```

`price <key> 0` disables an existing price key. `reset <key>` clears only that item's pressure. Reload reads both market settings and prices without deleting saved deposits, pressure or pending payments. Use `/combatadmin market set enabled false` to stop selling; re-enable it to retrieve any unsold deposits that remain saved.

Edit `config/kingdoms-combat/sell-settings.json` for per-item policies, then reload. For example:

```json
{
  "enabled": true,
  "blockInCombat": true,
  "defaults": { "floorMultiplier": 0.25, "volumeForFloor": 16384, "recoveryPerHour": 4096 },
  "items": {
    "minecraft:diamond": { "floorMultiplier": 0.5, "volumeForFloor": 1024, "recoveryPerHour": 128 }
  }
}
```

The public permission is `kingdomscombat.sell`, allowed by default. Arena participants, frozen players and spectators cannot use sell storage. Taking damage cancels the open menu and returns its deposits before damage is processed. Server shutdown also cancels open menus. Inventory overflow or a failed checkout remains saved for retrieval with `/sell`; reconnecting does not automatically sell it. Retrieve saved unsold deposits before entering an arena.

Selling pays EconomyCraft player wallets through Kingdoms' existing receipt-backed cash journal, including applicable core tribute. The receipt makes a queued payment safe to retry after a restart. A failed payment remains queued rather than issuing the items again. Inputs and player inventory transfers use verified player saves; failed saves retain a recovery journal. Keep the world's `kingdoms-combat/sell-market.json`, `sell-transaction.json` if present, player data and Kingdoms data together in backups. Do not delete these files to reset prices.

EconomyCraft's sell menu and shop sell actions redirect into this market while it is enabled. Buying, auctions and player-to-player orders keep their normal behavior. This market uses its own supply curve and does not apply EconomyCraft's wealth multiplier, daily-sell limit or automatic order fulfillment. Original base-price buy/sell differences remain as supplied; server owners should review those separately when balancing purchases against sales.
