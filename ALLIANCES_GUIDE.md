# Alliances and Joint Wars

Open **`/kingdom` → Alliances** to manage relationships with other kingdoms. Every kingdom keeps its own members, claims, core, treasury and AirDrop capture progress.

## Forming an alliance

1. A mayor chooses **Request alliance**, then selects another kingdom.
2. A mayor of the other kingdom opens **Alliance requests** and accepts or declines.
3. Accepted alliances are saved across restarts. By default, a kingdom can have **five allies**.

Requests expire after **60 real-time minutes**. The sending kingdom can withdraw a request. Either kingdom's mayor can end an established alliance, provided neither kingdom is preparing for or fighting a war. Disbanding a kingdom removes its alliances and outstanding requests.

Alliances are direct: your ally's allies do not automatically become yours. Allies cannot declare war on each other. By default, allied players cannot damage each other, including in wilderness; administrators can enable allied friendly fire. Alliances grant **no automatic access** to blocks, containers, entities or treasuries. Mayors can use the existing permission menu to grant physical access to particular allied players.

## Declaring a joint war

Open **War**, select a target, and choose:

- **Declare war:** begin preparation for your kingdom. You can invite allies afterward.
- **Declare and invite allies:** begin preparation and send invitations to eligible allies. Each invited kingdom's mayor must approve joining; nobody joins automatically.

The normal declaration rules still apply: the target needs strictly more than half its members online and at least one online mayor. Neither primary kingdom may have protection, a cooldown or another war.

## Joining and defending together

During the default **three-minute preparation**, a mayor on either side can use **War → Recruit an ally**, or **Alliances → Recruit ally**. Defenders can recruit their allies too. Any participating kingdom can recruit its own direct allies to its side.

An invited mayor opens **Alliances → War invitations** and reviews the opposing kingdoms, remaining preparation time and consequences before approving. By default, joining requires strictly more than half of the invited kingdom's members online. Its mayor must personally approve. Joining is blocked by protection, cooldown, another war, a full side, or an alliance with any kingdom on the opposing side.

Each side supports **four kingdoms**, including its original kingdom, by default. Invites reserve no places; eligibility and space are checked again when accepted. Invitations expire when preparation ends. Kingdoms already on either side cannot switch sides or join another war.

**All participants share the original countdown and end time.** Joining does not restart preparation or extend the war. A late approval may leave only a short preparation period; the review menu shows the remaining time.

## During the war

Preparation keeps claims protected. At the shared start time, members of every participating kingdom can attack, break, place and interact in **every opposing kingdom's claims**. The default active phase lasts **15 minutes**.

Members of the same side gain no automatic building or storage access to each other. Existing grants and public physical permissions still apply between participating kingdoms on the same side. By default, friendly fire is also disabled between different kingdoms fighting on the same side, even if they are not directly allied. Ordinary outsiders receive no war access.

Membership, claims, mayors, permission/flag edits, core relocation and protection purchases freeze for every participant. Alliances cannot be added or removed while either affected kingdom is in preparation or wartime. Once accepted, a kingdom cannot withdraw from the war. Logouts do not end it.

Core capture works against any opposing kingdom's host claims. Hostile beacon purchases let you choose a kingdom on the opposing side; each purchase affects **one enemy kingdom**, at the normal price. Treasuries and AirDrop capture groups remain separate.

All participants receive war feedback, and their territory/core status and combat-addon restrictions follow their participation. At war end, ordinary claim protection returns and every participating kingdom receives the default **two-hour cooldown**. Saved member cooldowns apply to all participants as well.

## Commands

Most players can use the menus. These commands are also available, with no `/kingdom` subcommand tab completion:

| Command | Action |
| --- | --- |
| `/kingdom alliance` | Open Alliances |
| `/kingdom alliance request <kingdom>` | Send a request |
| `/kingdom alliance accept <kingdom>` | Review and confirm acceptance |
| `/kingdom alliance decline <kingdom>` | Decline an incoming request |
| `/kingdom alliance cancel <kingdom>` | Withdraw your outgoing request |
| `/kingdom alliance remove <kingdom>` | Review and confirm ending an alliance |
| `/kingdom warinvite <kingdom>` | Invite an ally during preparation |
| `/kingdom warjoin` | Review incoming war invitations |

Names containing spaces work as the final command argument. All relationship changes and war approvals require a mayor.

## Configuration

Use `/kingdomadmin set <setting> <value>`, or edit `config/kingdoms/config.json` and run `/kingdomadmin reload`.

| Setting | Default | Meaning |
| --- | --- | --- |
| `alliancesEnabled` | `true` | Allow new alliance requests and acceptance; existing alliances remain |
| `maxAllies` | `5` | Maximum allies per kingdom, 0–1000; lowering this does not remove existing allies |
| `allianceRequestMinutes` | `60` | Lifetime of new requests, 1–10080 real-time minutes |
| `alliedFriendlyFire` | `false` | Allow ordinary PvP rules between allies and different kingdoms on the same war side |
| `jointWarsEnabled` | `true` | Allow sending/accepting war invitations; existing participants stay in their war |
| `maxKingdomsPerWarSide` | `4` | Maximum kingdoms per side, including its original kingdom, 1–1000; lowering this does not expel participants |
| `jointWarRequiresOnlineMajority` | `true` | Require more than half of the joining kingdom online; mayor approval is always required |
| `menuSlots.alliances` | `40` | Home button position; an unused position is selected when migrating custom layouts |
| `menuItems.alliances` | `minecraft:iron_chain` | Home button item |

The existing preparation, war duration, cooldown, titles, sounds and action-bar settings also apply to joint wars. Existing wars keep their saved timers when settings change. Pending requests keep their saved expiry; caps and eligibility use the current configuration when accepted. Restart downtime counts toward preparation, war, cooldown and request expiry.

`/kingdomadmin endwar <kingdom>` ends the **whole shared war** containing that participant and applies cooldown to every participant and their members. `/kingdomadmin clearcooldown <kingdom>` remains specific to that kingdom and also clears its protection; end its war first.

## Placeholders

Requires the separate, compatible Fabric Placeholder API; it is not bundled.

| Placeholder | Result |
| --- | --- |
| `%kingdoms:allied_kingdoms%` | Direct ally names, or `ɴᴏɴᴇ` |
| `%kingdoms:alliance_count%` | Number of direct allies |
| `%kingdoms:war_allies%` | Other kingdoms on your current war side, or `ɴᴏɴᴇ` |
| `%kingdoms:enemy_kingdoms%` | All opposing kingdoms in the current war, or empty outside a war |
| `%kingdoms:enemy_kingdom%` | Same opposing-side list; preserves the existing placeholder name |
| `%kingdoms:war_side%` | `ᴀᴛᴛᴀᴄᴋɪɴɢ`, `ᴅᴇꜰᴇɴᴅɪɴɢ` or `ɴᴏɴᴇ` |
| `%kingdoms:inside_allied_territory%` | Whether you stand in a direct ally's claims |

The existing war preparation, remaining wartime and enemy-territory placeholders now work for every participant. New name/status values use small caps. Numeric and boolean values remain numbers and `true`/`false` for formatting integrations.

## Updating an existing server

Old two-kingdom wars load with the same kingdoms, timings, countdown state and cooldowns. No alliance memberships are invented. Existing custom menu positions remain; the new Alliances button occupies an unused slot. Back up your configuration and kingdom data before replacing the mod. No client mod is required.
