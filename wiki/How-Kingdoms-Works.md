# How Kingdoms Works

Build a kingdom, protect your land, manage a shared treasury, buy powers, and fight other kingdoms for their cores.

**Start with `/kingdom`.** Everything important is available through its menus. No client-side mod or resource pack is needed.

This guide covers Kingdoms 1.10.1 for Minecraft 26.2. Prices, timers, limits, permissions and effects below describe the **default settings**. Your server's admins can change them; check the menu for current costs and status.


## Contents

- [Getting started](#getting-started)
- [Menus and territory](#menus-and-territory)
- [Claims and outposts](#claims-and-outposts)
- [Members, mayors and permissions](#members-mayors-and-permissions)
- [Land flags](#land-flags)
- [Treasury and upkeep](#treasury-and-upkeep)
- [Wars](#wars)
- [Kingdom beacon powers](#kingdom-beacon-powers)
- [Town centers and cores](#town-centers-and-cores)
- [Mobs and animals](#mobs-and-animals)
- [Identity and leaderboard](#identity-and-leaderboard)
- [Command reference](#command-reference)
- [AirDrops](#airdrops)
- [Artifacts and AirDrop cash](#artifacts-and-airdrop-cash)

## Getting started

**Join a kingdom:** ask a mayor to invite you, then select **Accept invitation** or use `/kingdom join`. You can belong to only one kingdom at a time. A newer invitation replaces your previous invitation.

**Create a kingdom:** stand in the chunk you want to claim, open `/kingdom`, select **Create your kingdom**, enter a name in the dialog, and confirm. You become its first mayor.

- Creation costs **100,000 from your personal balance**. The treasury starts empty.
- Your current chunk becomes the first claim. A chunk is a 16×16 block area, protected through the world's full height.
- Creation is blocked within **16 chunks of another kingdom's claimed land**. The error tells you the nearest kingdom and how much farther to move.
- Creating or expanding claims is also blocked within the default **16-chunk square radius around overworld spawn**.
- Your founding chunk needs a breakable block at its center for the town core. If creation reports that none exists, place a suitable block there and try again.
- Names must be unique, with 3–24 letters, numbers, spaces, underscores or hyphens.
- New kingdoms receive **24 hours of war protection**. A protected kingdom cannot attack or be declared upon.

After founding, **deposit money into the treasury** so your kingdom can expand and pay upkeep. Creation is announced to the server.

## Menus and territory

Open `/kingdom` to see your kingdom's members, treasury, claim-slot use, war status and cooldown. Left-click the menu buttons; shift-clicks and item transfers do not operate them. Text-entry options use Minecraft dialogs. AirDrops, Artifacts and Help are accessed through `/airdrop`, `/artifact` and `/kingdom help`; their shortcuts are not shown in the home menu. The Kingdom beacon sits two slots to the right of War protection, with one empty slot between them.

| Button | What you do there |
| --- | --- |
| Claims | Claim, buy an outpost, inspect or release the current chunk |
| Treasury | Deposit, withdraw, check upkeep and debt |
| Members | Invite players, manage permissions and appoint mayors |
| Settings | Set land flags, member/public permissions, icon and town center |
| War | Declare war, view both sides and recruit allies during preparation |
| Alliances | Request/accept alliances and review joint-war invitations |
| War protection | Check protection and buy more when eligible |
| Kingdom beacon | Buy powers and inspect active/incoming effects |
| Treasury leaderboard | Compare kingdom balances and view center locations |

The action bar shows whose territory you are standing in. Entering and leaving claims produces titles and sounds by default; unclaimed land appears as **Wilderness**. Rejoining inside a claim immediately identifies that territory. War participants also see their enemy and preparation/wartime timer.

If your server uses BlueMap, enable the **ᴋɪɴɢᴅᴏᴍꜱ** map layer to see colored territories, outposts and core pins. Click a region for kingdom information. Map colors match the kingdom's core color; admin settings control the details and pins. See [BlueMap territories](BlueMap).

Tab completion intentionally shows only `/kingdom` for player commands. The subcommands below still work when typed. `/k` also opens the menu, but is hidden from suggestions.

## Claims and outposts

Mayors claim the chunk they are standing in through **Claims → Claim this chunk**. Ordinary expansion costs **25,000 from the treasury** and must share an edge with an existing claim; touching only a corner is not enough. Claims stay in the kingdom's original dimension by default.

For a separate settlement, choose **Claims → Claim outpost**. An outpost:

- Can be disconnected from your mainland.
- Costs **150,000 from the treasury**.
- Occupies one actual chunk but uses **six claim slots**.
- Can support normal expansion around it; each ordinary expansion uses one slot.

The default limits are **64 claim slots, four outposts and 20 members**. Your founding chunk uses one claim slot. For example, a founding chunk plus one outpost uses seven slots, despite owning only two chunks.

To release land, stand in it and choose **Unclaim this chunk**. There is no refund. You cannot release your last chunk, split a region into an unsupported disconnected region, or release a chunk containing a placed core or an unplaced core's return location. Remove an outpost's surrounding expansion chunks before removing its anchor if the remaining region would have no valid anchor.

Claims and outposts cannot be changed during war preparation or wartime.

## Members, mayors and permissions

Mayors invite players, remove members, manage land and money, declare wars, and appoint other mayors. **Every mayor has full management access**, including withdrawals and disbanding. Choose people you trust. A kingdom needs at least one mayor; its last mayor must appoint another mayor or disband before leaving.

New members can normally place and break blocks, interact, use containers/entities and deposit. Withdrawals require a separate grant. Visitors have no default permissions.

Use **Members → Select player** for personal grants, or **Settings → Member defaults / Public permissions** for general access. Personal grants can also give visitors physical access without making them members.

| Permission | Allows |
| --- | --- |
| `place` | Placing blocks and related placement actions |
| `break` | Breaking blocks |
| `interact` | Using blocks such as doors and switches |
| `containers` | Accessing storage |
| `entities` | Interacting with entities |
| `pvp` | Player combat, when the land's PvP flag also allows it |
| `deposit` | Depositing personal money into your kingdom |
| `withdraw` | Taking money from your kingdom's treasury |

**Grants add access.** Turning a personal grant off does not remove access still supplied by member or public defaults. Treasury access always requires membership.

Leaving or being kicked takes effect immediately. You must then wait **one hour before rejoining that same kingdom**. Joining a different kingdom is allowed. Membership changes, mayor appointments and permission edits are frozen during preparation and wartime.

## Land flags

Permissions decide **who** may act. Flags decide **which kinds of activity** the kingdom allows. Change them through **Settings**.

| Flag | Controls |
| --- | --- |
| Explosions | Explosion damage |
| Fire | Ignition, fire spread and fire damage |
| Fluids | Water/lava placement and flow |
| Mobs | Whether unapproved mobs may remain in claims |
| Mob griefing | Mob-caused damage and environmental changes |
| Pistons | Piston movement within the kingdom |
| Automation | Hoppers, droppers, dispensers and similar inventory machines |
| PvP | Player-versus-player combat |

**Automation starts enabled; the other flags start disabled.** Pistons have their own flag. Machines, redstone and flowing materials do not grant permission to affect neighboring protected claims across a border.

Kingdom explosion flags apply to claimed land. TNT and other explosions use normal Minecraft block damage in wilderness, subject to vanilla gamerules and other server mods. Marked cores and active AirDrop protection still protect their specific blocks or areas, including in wilderness. A blast crossing a claim border is checked for each affected block.

Disabled flags apply even to mayors and players with building permissions. For example, being allowed to place blocks does not let you pour lava when Fluids is off. Operators can bypass ordinary physical claim permissions, but still obey the land flags. Flags cannot be changed during preparation or wartime.

## Treasury and upkeep

The treasury is your kingdom's shared bank. It pays for expansion, outposts, war protection, beacon powers and upkeep. Use **Treasury** for preset amounts or a custom-amount dialog.

You can also type:

- `/kingdom deposit 10k`
- `/kingdom withdraw 1.5k`

Money accepts amounts such as `1k`, `10m`, `1.5m`, `2 billion`, commas and underscores. `k`, `m`, `b` and `t` mean thousand, million, billion and trillion. The final amount must be a positive whole number: `1.5k` is 1,500, but `1.5` is not a valid whole-currency transfer.

Default upkeep is **1,000 per member every 24 hours**, including mayors. Ten members therefore cost 10,000 per day. The treasury page shows your bill, balance, debt and next billing time.

If the treasury cannot pay, the bill becomes debt and a **72-hour grace period** begins. Claims remain protected under their usual rules during that grace period. Mayors receive an overdue warning that reopens until **one mayor acknowledges it for everyone**.

**Acknowledging does not pay the debt or extend the deadline.** Deposit enough to cover the entire outstanding debt. A later missed payment creates a new warning.

If unpaid debt remains when the grace period ends, the kingdom disbands and its claims become wilderness. Debt disbanding waits until an ongoing war ends. Offline time still counts, and missed upkeep cycles are charged when the server returns.

Voluntary disbanding also releases every claim and **forfeits the treasury**. Withdraw funds first if you intend to keep them. Disbanding is unavailable during preparation or wartime.

## Wars

Only a mayor can declare war. Use **War → Select kingdom → Confirm**, or `/kingdom war Kingdom Name`.

A declaration is allowed only when:

- Neither kingdom is already preparing for or fighting a war.
- The kingdoms are not allied with each other.
- Neither kingdom has active war protection or a war cooldown.
- **Strictly more than half of the defending kingdom's members are online**, including at least one defending mayor. A kingdom with ten members needs six online; one with five needs three.

### Preparation: 3 minutes

Claims **remain protected** throughout preparation. Both sides receive announcements, titles, sounds and countdown milestones at 3 minutes, 1 minute, 30 seconds, 10 seconds and the final five seconds.

Membership, membership invitations, claims, mayors, permissions, flags, icons and core relocation are locked. Protection cannot be purchased. Treasury deposits and permitted withdrawals remain available.

### Active war: 15 minutes

Once the countdown ends, **members of all participating kingdoms can build, break, interact and fight throughout every opposing kingdom's claims**. Both sides can attack and defend. Ordinary visitors receive no war access.

Land flags are overridden for wartime, including fire, fluids, explosions, mobs and PvP. Server rules still apply: staff may prohibit TNT, lava casting or other griefing methods. Marked cores retain their special capture rules; explosions do not substitute for breaking and taking a core.

War does **not** grant access to the enemy treasury or bypass withdrawal permissions. Logging out does not stop the war, even if mayors disconnect. Destruction is permanent; there is no automatic terrain restoration afterward.

### Server-controlled war rules

Staff can make selected blocks, such as obsidian, instant to mine during active war. You still need permission to break there, tools and drops follow vanilla rules, and core capture rules still apply. Staff may separately ban placing blocks, particular entities, pearl teleporting, explosions, explosion block damage or lava/water formation. Mayors cannot override these policies.

By default these lists are empty and restrictions are scoped to active-war claims. Your server may configure a wider scope. Check with staff for its rules. [Admin command guide](War-Restrictions).

### End of war

Announcements make it clear when ordinary claim protection returns. Every participating kingdom enters a **two-hour war cooldown**, preventing declarations in either direction. Members retain their saved war cooldown when they recreate a kingdom or join another one. War and cooldown timers continue through server downtime.

### Alliances and joint wars

Open **Alliances** to request a relationship with another kingdom. A mayor on each side must agree. Defaults allow **five allies**, with requests expiring after **60 minutes**. Allies keep their own land and treasury. They gain no automatic claim access, and PvP between allies is disabled by default.

When declaring war, **Declare and invite allies** starts preparation and sends invitations to your eligible allies. Their mayors must explicitly approve in **Alliances → War invitations**. Defenders can recruit allies during preparation too. Each side can contain **four kingdoms** by default. Invited kingdoms cannot have protection, a cooldown, another war or an alliance with an opposing participant; more than half their members must be online by default.

All participants share the original start/end times, and every participant freezes membership and claim changes. Approval is final; a kingdom cannot withdraw or switch sides. Claims become vulnerable only between opposing sides. Allies on your side still need physical claim permissions. Everyone receives the same war countdown, ending notifications and cooldown. AirDrop capture groups stay separate. [Read the full alliance and joint-war guide](Alliances).

### Buying protection

**War protection** costs **1,000,000 from the treasury for 24 hours**. A mayor may buy it only outside preparation/wartime, with no protection already active. It cannot be stacked.

Protection blocks both attacking and receiving declarations. **A kingdom holding another kingdom's core cannot buy protection**, including while carrying a foreign core before securing it. Staff can end wars or clear cooldowns and protection when necessary.

## Kingdom beacon powers

Open **Kingdom beacon** or `/kingdom beacon`. This is a menu-based kingdom power system; you do not need to build a vanilla beacon pyramid. Only mayors may buy powers, using treasury money.

**Member powers:** Speed, Haste, Resistance, Jump Boost, Strength and Regeneration. Each supports Level I or II. **Level II costs four times Level I** by default.

**Glowing** reveals affected enemy members through blocks while they stand in claimed land, including underground. Vanilla entity tracking limits apply, and the outline is visible to everyone who can track the player. It can run alongside Mining Fatigue. The usual hostile-power duration is 24 hours; buying it requires active war, and it persists for its purchased duration even after the war ends. It stops in wilderness.

**Opponent powers:** Slowness, Blindness, Mining Fatigue and Glowing, all Level I. These can be bought only during active wartime. In joint wars, choose one kingdom on the opposing side; each purchase targets that kingdom's members at the normal price.

| Power | Default Level I price |
| --- | ---: |
| Speed | 100,000 |
| Haste | 125,000 |
| Resistance | 150,000 |
| Jump Boost | 100,000 |
| Strength | 200,000 |
| Regeneration | 250,000 |
| Slowness | 300,000 |
| Blindness | 400,000 |
| Mining Fatigue | 350,000 |
| Glowing | 300,000 |

Each purchase lasts **24 real-time hours**. Several different powers may run together. You cannot rebuy an active power at the same or a lower level; upgrading Level I to II buys a new duration at the displayed Level II price.

**Powers work only inside claimed chunks.** They follow the affected members and work in any kingdom's claimed land, including enemy territory. They stop in wilderness and return in claims while the purchase remains active. Time continues while you are outside claims or offline.

Hostile powers can continue after the war ends until their purchase expires. Disbanding either involved kingdom removes its related powers. Use **Active powers** to see your bonuses, outgoing attacks, incoming effects and time remaining.

## Town centers and cores

Your kingdom's core marks an existing block. Initially it is at the topmost suitable block in the center of your founding chunk. Each kingdom has a saved random dust/glow color.

Dust outlines the core nearby. During **active war**, its glow becomes thicker and uses the maximum display viewing range. Minecraft's terrain rendering still limits how far you can actually see it. Preparation does not activate the glow, and there is no core-direction action-bar compass.

### Keep your core open to the sky

If any block covers your core for **five continuous minutes**, its marker moves to the top of the **same X/Z column**. Glass, leaves and liquids count as cover. Mayors of the owning and holding kingdoms receive a warning when coverage starts and another message when it moves. Removing the roof before the deadline resets the timer.

If the highest block is fluid, fire, unbreakable or reserved for another core, a plain stone block is created above it for the core. At build height, mayors are warned and relocation retries until there is room. The old block stays in place. This also applies to captured cores and does not change tribute. Carried core items are exempt. Timers survive restarts; unloaded columns are checked when loaded again. Administrators can change the timeout or disable this rule.

### Move your own core

Go to **Settings → Town center → Change block**, then right-click an existing breakable block in your own claims within two minutes.

Only mayors may do this, outside preparation/wartime, while their own core is secured at home. Air, fire, fluids and unbreakable blocks cannot be selected, and two cores cannot reserve the same block. The selected block stays as it is; it receives the core marker.

### Capture an enemy core

1. During active war, find and break a core in the opposing kingdom's territory.
2. Take the glowing core item back to your own claimed land.
3. **Right-click an existing breakable block with the item** to secure it. You need placement permission there.

The item selects a block; it does not place a new block. **Tribute starts when the core is secured, not merely when it is broken.** You can hold several kingdoms' cores at once.

Core items cannot be stored in chests, shulkers, bundles, hoppers, workstations or entity storage. Carry them in your player inventory. They may be dropped and picked up, but stale or copied items cannot secure the same core again.

An unplaced core automatically returns to its previous location after **10 real-time minutes**, including offline time. If the former block is gone, its block type may be restored, but container contents are not recovered. The old core item then expires.

### Tribute and independence

While another kingdom holds your core, its treasury receives **20% of every incoming balance increase earned by your current members**, wherever they earn it. This includes sales, rewards, incoming player payments, positive balance adjustments and treasury withdrawals. Spending is not income.

For example, a taxed withdrawal of 1,000 gives the member 800 and sends 200 to the captor. Small credits accumulate fractional tax, so splitting payments does not avoid tribute. Leaving or being kicked ends that member's tribute immediately, with the normal one-hour same-kingdom rejoin restriction.

To regain independence, **recover your core during a war and secure it on a block inside your own claims**. Tribute continues until you secure it at home. An automatic return to the captor does not free you. Recovering your own core also does not release any other kingdoms whose cores you hold.

### Move a captured core you hold

Go to **Settings → Town center → Captured kingdoms → Select kingdom → Change block**, then right-click a suitable block in your own claims.

Only the holder's mayors may move a secured captured core, and both kingdoms must be outside preparation/wartime. Its original owner, tribute and color stay the same. The list has pages if you hold many cores.

You cannot unclaim a chunk containing or reserving a core. If a captor disbands, its captive kingdoms are released and receive new home cores.

## Mobs and animals

With Mobs disabled, unapproved mobs cannot spawn in claims, and unapproved mobs entering the kingdom disappear when checked. **There is no holding area or grace period.**

Villagers, farm animals, pets and server-approved exempt types are allowed. To keep another individual mob, a mayor must **sneak-right-click it outside the claim before bringing it in**. Approval stays with that mob for the kingdom and survives restarts.

Approving a mob does not bypass combat, fire, explosion or mob-griefing rules. Enabling Mobs allows unapproved mobs; active war also overrides that flag. Mobs entering during war may disappear afterward if they are not approved or exempt.

## Identity and leaderboard

Your kingdom has a name and item icon. New kingdoms use **paper** as their icon.

- To change the icon, hold a **non-block item** and select **Settings → Kingdom icon**, or type `/kingdom icon`. Block items are not supported. Icon changes are unavailable during preparation and wartime.
- Open **Treasury leaderboard** or `/kingdom leaderboard` to compare kingdoms by shared treasury balance. Entries show position, icon, identity, money, members and claims by default.
- Hover an entry to see that kingdom's current core coordinates and dimension, including a captured core's location. Carried cores show their last placed location. Staff may hide these fields.

The icon is not automatically added to chat prefixes. Your server may choose to show kingdom names, roles, icons, balances and war information in chat, tab lists or scoreboards. The styled kingdom placeholder combines the item icon with the kingdom name in small caps.

Items with multiple appearances, such as clocks and compasses, use a fixed representative picture in inline text. Shields use a shield silhouette. The inventory leaderboard and item hover still show the selected item.

## Command reference

Replace the example arguments with your own values; do not type the `< >` brackets. Commands that create, remove, withdraw, protect or declare war may open a confirmation menu.

| Command | Action |
| --- | --- |
| `/kingdom` or `/k` | Open the main menu |
| `/kingdom help` | Open help |
| `/kingdom create <name>` | Create a kingdom in your current chunk |
| `/kingdom invite <online-player>` | Mayor: invite a player |
| `/kingdom join` | Accept your latest invitation |
| `/kingdom leave` | Leave your kingdom |
| `/kingdom kick <player-or-UUID>` | Mayor: remove a member |
| `/kingdom mayor <player-or-UUID> <true/false>` | Mayor: appoint/remove another mayor |
| `/kingdom grant <player-or-UUID> <permission> <true/false>` | Mayor: change a personal grant |
| `/kingdom flag <flag> <true/false>` | Mayor: change a land flag |
| `/kingdom claim` | Mayor: buy the current chunk |
| `/kingdom outpost` | Mayor: buy a disconnected outpost |
| `/kingdom unclaim` | Mayor: release the current chunk |
| `/kingdom treasury` | Open the treasury |
| `/kingdom deposit <amount>` | Deposit your money |
| `/kingdom withdraw <amount>` | Withdraw with permission |
| `/kingdom war <kingdom-name>` | Mayor: declare war |
| `/kingdom alliance` | Open alliances and requests |
| `/kingdom alliance request <kingdom>` | Mayor: request an alliance |
| `/kingdom alliance accept <kingdom>` | Mayor: review accepting an alliance |
| `/kingdom alliance decline <kingdom>` | Mayor: decline a request |
| `/kingdom alliance cancel <kingdom>` | Mayor: withdraw an outgoing request |
| `/kingdom alliance remove <kingdom>` | Mayor: review ending an alliance |
| `/kingdom warinvite <kingdom>` | Mayor: invite an ally during preparation |
| `/kingdom warjoin` | Review incoming joint-war invitations |
| `/kingdom protect` | Mayor: buy war protection |
| `/kingdom beacon` | Open powers and active effects |
| `/kingdom core` | Open home/captured core management |
| `/kingdom icon` | Mayor: set your held non-block item as the icon |
| `/kingdom leaderboard` | Open treasury rankings and core locations |
| `/kingdom acknowledge` | Mayor: acknowledge the current debt warning |
| `/kingdom disband` | Mayor: disband and forfeit the treasury |

Flag command names: `explosions`, `fire`, `fluids`, `mobs`, `mob_griefing`, `pvp`.

Pistons, observers, redstone, hoppers, droppers and dispensers have no kingdom toggle. They work normally within your kingdom, including across claimed chunk borders. Redstone signals also cross ownership borders; protected block movement, inventory transfers, cores and AirDrop chests still obey their protection rules.

Examples: `/kingdom grant Alex withdraw true`, `/kingdom flag fluids true`, `/kingdom deposit 1.5m`.

**Keep the treasury funded, check the war timer, approve mobs before crossing claims, and secure stolen cores before their return timer expires.**


## AirDrops

Use `/airdrop`. Events require 10 online players and normally occur every three hours plus a random 0–20-minute delay. Announced crates descend for ten minutes before capture begins.

Stay within 15 blocks horizontally and within ±3 blocks of the landing height: three kingdom members capture in 90 seconds, while an unaffiliated solo player needs 135 seconds. Only one side progresses. A larger eligible group takes over after keeping its advantage for 15 continuous seconds, starting from zero; ties preserve control. Solo players count as three kingdom members, so a kingdom needs four to take over from a solo. Rival presence slows it by 1.3×, without stacking. Losing the required presence pauses capture for up to 15 seconds; failing to restore it resets progress. Nearby boss bars show arrival, progress, contested capture and grace pauses.

A configurable **5-block horizontal radius** prevents placing, breaking and destructive terrain changes during descent/capture. You can build beyond that radius. After capture, only the chest and its support remain protected until the drop disappears. Normal PvP and kingdom claim rules still apply. A dust ring marks the capture boundary. Eligible capturers and winners looting the chest glow: kingdom players use the closest vanilla glow color to their core, and solo players use black. The dust uses the exact controlling kingdom color.

When a drop lands, it becomes a physical chest. After capture, right-click that chest to open its winners-only contents. Every member on the winning roster at capture can collect; later joiners cannot. Each stack is shared and available once. Click or shift-click a stack to collect it, and keep an empty inventory slot. You must stay near the chest; `/airdrop` only shows information and coordinates. Enemies cannot open or break it, and hoppers cannot take its loot.

**All uncollected loot disappears thirty minutes after landing, even if your side has captured it. Nothing spills onto the ground.** Collecting late does not restart this deadline, and restarting the server does not reroll rewards.

Loot tiers advance using real-world days since first enable: days 1–2, 3–4, 5–6 and 7+. Eight weighted crate variants specialize in different reward categories. Nether-exclusive loot cannot appear before day 3; End-exclusive loot cannot appear before day 7. Legendary/Mythic crates do not guarantee rare items.

See [the full AirDrop guide](AirDrops) for details, loot probabilities and admin commands.


## Artifacts and AirDrop cash

Ordinary AirDrops include one shared cash reward: 1,000 on days 1–2, 10,000 on days 3–4, 50,000 on days 5–6 and 100,000 from day 7. Collect it from the winners-only chest to pay the winning kingdom treasury or your solo wallet. Core tribute applies.

A rare independent artifact roll replaces the usual loot with one artifact and sometimes two fixers. Those special drops have no cash. The thirteen AirDrop types include Thunder Axe, Thorns Shield, Frostbite Sword, Multi Pickaxe, Vampire Sword, Void Scythe, Guardian Angel, Wind Burst Spear, Runner's Boots, King's Chestplate, Archer's Bow, Advanced Axe and Advanced Pickaxe. Golden Crown is a separate staff-command-only event reward. Tools and shields use normal Minecraft durability and compatible enchantments, including Mending and Unbreaking. Guardian Angel instead has 100 right-click charges for three seconds of cobweb immunity.

Ordinary tools accept normal repairs. A genuine fixer in a vanilla anvil restores 25% of durability or Guardian Angel charges for five levels by default. Hold your artifact and open **Artifacts** in `/kingdom` or `/artifact` to upgrade once for three Netherite Ingots; Guardian Angel has no Netherite upgrade. See [the full catalogue](Artifacts) for each ability and upgrade. Artifact quantities remain unlimited, and every eligible trigger succeeds by default.

The Wind Burst Spear hits living players and mobs only. Armor stands, mannequins and nonliving entities such as boats, minecarts and item frames are ignored, including when using its Netherite upgrade.
