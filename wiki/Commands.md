# Player Commands


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

Flag command names: `explosions`, `fire`, `mobs`, `mob_griefing`, `pvp`.

Pistons, observers, redstone, hoppers, droppers and dispensers have no kingdom toggle. They work normally within the same kingdom and across its chunk borders. Redstone signals cross ownership borders. Pistons may extend into wilderness; retraction checks ownership before changing blocks and cannot pull across a protected border. Liquid flow may leave a kingdom for wilderness, but incoming wilderness flow and flow between different kingdoms are blocked at peace. Pouring a bucket requires Place permission; collecting liquid requires Break permission. Cores, AirDrop protection and administrator war restrictions still apply.

Examples: `/kingdom grant Alex withdraw true`, `/kingdom flag fire true`, `/kingdom deposit 1.5m`.

**Keep the treasury funded, check the war timer, approve mobs before crossing claims, and secure stolen cores before their return timer expires.**
