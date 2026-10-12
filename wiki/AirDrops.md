# AirDrops

Recurring supply crates create PvP objectives for kingdoms and solo players. This is part of the server-only Fabric mod for Minecraft 26.2. Players need no client mod or resource pack.

## Players: finding and winning a crate

Open **AirDrops** in `/kingdom`, or use `/airdrop`. Each entry shows its coordinates, crate variant, loot tier and status.

- Events normally begin every **3 hours plus a random 0–20 minutes**. At least **10 players** must be online: 10–19 produces one crate, 20+ produces two. If too few players are online or no safe sites are found, the server retries later without announcing a nonexistent event.
- Coordinates are announced when the event actually starts. A glowing barrel descends over **10 minutes**. It cannot be captured or opened during descent.
- After landing, control the area within **15 blocks horizontally and 3 blocks vertically**. Living survival/adventure players count. Spectators, creative players and flying players do not. Ground-only contribution is an optional admin setting, disabled by default.
- A kingdom needs **3 members** for **90 seconds**. An unaffiliated player needs **135 seconds** alone. Only **one side** owns capture progress. A stronger qualifying side must keep its advantage for **15 continuous seconds** before taking over from zero. Capture pauses during this takeover countdown; losing the advantage resets the challenge. Ties preserve the holder. Each eligible kingdom member contributes one control strength; an unaffiliated solo player contributes **three**. Four kingdom members can take over from a solo player; three cannot break the tie. Separate solo players remain separate sides and never combine their strength.
- Any rival side inside the radius slows progress by **1.3×**, regardless of rival count. A kingdom with 90 seconds of uninterrupted, fully contested capture takes 117 seconds. A solo player takes 175.5 seconds under continuous interference.
- Losing the required presence pauses progress. Restore it within **15 seconds** to continue. Otherwise progress resets. Disconnecting, dying, teleporting out and membership changes use the same eligibility rules. A membership change does not transfer progress to a new side.
- A nearby boss bar shows arrival time, your side's progress, interference and grace pauses. Chat, titles and sounds announce important changes.
- A configurable **5-block horizontal build-protection radius** stops placing, breaking and destructive environmental changes during descent/capture, including towers above the zone. Building remains allowed outside it. After capture, the chest and its support stay protected until cleanup. OPs obey event protection. Normal PvP and claim rules still apply.
- A **dust circle** shows the configured capture radius from descent until cleanup. It uses the current controlling kingdom's exact core color, black for solo control, and gold while neutral. The descending crate has twelve glowing vanilla display edges and a parachute built from vanilla block displays, including a rounded striped fabric canopy, lining, stitched seams and eight continuous suspension lines. The complete assembly sways gently, settles onto the landing coordinates and disappears on landing.
- Eligible players inside the capture area glow, including rivals; winners also glow while looting the chest. Glow ends when they leave the area/menu, die, disconnect or the event closes. Vanilla player outlines support sixteen colors, so kingdom outlines use the closest match to the core's RGB; solo outlines are black. Actual server scoreboard memberships and existing glowing tags/potion effects are preserved.

### Collecting rewards

On landing, a **physical chest** appears at the drop's coordinates. It stays sealed until capture finishes. After capture, **right-click the chest** to open its winners-only contents. The winning kingdom's membership roster is recorded at capture; a solo winner is the sole recipient. Members who join later do not gain access to that crate. Recorded winners retain access if they subsequently leave the kingdom.

Loot is **shared**, not copied for each member. Click or shift-click an item to collect its whole stack once; keep an empty inventory slot. Loot is scattered across random chest slots; those slots remain stable after collecting, reopening or restarting. The chest shows actual stack counts and additional pages for oversized loot pools. Stay within eight blocks of the chest to keep its contents open. `/airdrop` shows location and status only; it does not collect loot remotely. Enemies cannot open or break the chest. Explosions, fluids, pistons and hoppers cannot bypass its lock, including after capture finishes. Ordinary chests are unaffected.

**The deadline is 30 minutes after landing, not 30 minutes after capture.** At that deadline, the crate and all uncollected rewards disappear, including rewards left in the winners' chest. Collected items stay in the recipient's inventory. Nothing spills onto the ground. Restarting or reopening the menu does not reset the deadline or reroll loot.

Changing capture control always resets progress; the new side never inherits the former side's timer. If no stronger qualified side takes over when the holder loses quorum, the existing grace period pauses its timer and then resets it.

Loot transfers already reserved during a save failure are reconciled using the recipient's saved inventory receipt. This prevents an interrupted delivery from granting the same stack twice. An undelivered reservation also expires at the original deadline.

## Loot age and variants

Loot age begins when AirDrops are first enabled. It advances in elapsed real-world 24-hour periods, including nights, empty-server periods and server downtime. Minecraft day cycles have no effect.

| Real-world age | Tier |
|---|---|
| Days 1–2 | Early game: food, iron, arrows, basic supplies and equipment |
| Days 3–4 | Nether unlocked: diamonds, brewing supplies, eligible Nether materials and moderate enchantments |
| Days 5–6 | Advanced: stronger equipment, larger resources and rare premium rewards |
| Day 7+ | Endgame: eligible End resources and rare endgame rewards |

Nether-exclusive loot is locked until day 3; End-exclusive loot is locked until day 7. These are loot unlocks, not restrictions on travelling to dimensions. Early resources remain in later pools. Equipment, enchantments, potions, quantities and chances are configurable.

| Variant | Spawn weight / chance | Emphasis |
|---|---:|---|
| Standard supply drop | 6000 / 60% | Mixed survival supplies |
| Resource cache | 1200 / 12% | Basic materials, valuable resources and diamonds |
| Combat cache | 1000 / 10% | Equipment and eligible potions |
| Enchantment cache | 700 / 7% | Enchanting supplies and books |
| Armory cache | 500 / 5% | Equipment and tools |
| Elite cache | 400 / 4% | Better mixed categories |
| Legendary cache | 180 / 1.8% | Mixed valuable categories; 2× rare-roll multiplier |
| Mythic cache | 20 / 0.2% | Mixed valuable categories; 3× rare-roll multiplier |

Every crate variant uses the current age tier: a day-one Mythic crate has early-game loot, while an endgame Mythic crate has stronger pools, enchantments, quantities and rare chances. A rare variant does **not** guarantee a rare reward.

An independent artifact roll happens **before** normal loot, for every variant. Its default chances are 0.10%, 0.15%, 0.20% and 0.30% at days 1, 3, 5 and 7. Success replaces normal loot with one genuine ability artifact and optionally two fixers. These special artifact drops contain **no cash**. Artifact abilities are generated from their own configured variants, rather than scaling with crate phase. See [Artifacts](Artifacts).

### Cash rewards

Ordinary crates include one cash reward: **1,000** on days 1–2, **10,000** on days 3–4, **50,000** on days 5–6 and **100,000** on day 7 onward. Clicking it deposits money into the **winning kingdom’s treasury**, or the **solo winner’s wallet**. It is shared and paid once; it requires no inventory space. Existing core tribute applies. If the winning kingdom disbands, its treasury reward cannot be collected; it expires with the crate. Artifact-only drops contain no cash.

### Default rare-roll probabilities

Each check is made once per eligible crate, never independently for every slot. If several checks succeed, one successful reward is chosen; the rest are discarded. Consequently the final appearance rate for an individual item can be below its check probability. A selected reward may contain a configured quantity/bundle.

| Reward | Days 1–2 | Days 3–4 | Days 5–6 | Day 7+ |
|---|---:|---:|---:|---:|
| Golden apple | 0.5% | 3% | 8% | 15% |
| Enchanted golden apple | 0% | 0.05% | 0.25% | 1% |
| Totem | 0% | 0.1% | 0.5% | 2% |
| Trident | 0% | 0.1% | 0.5% | 2% |
| Netherite upgrade template | 0% | 0.2% | 1% | 3% |
| Netherite ingot | 0% | 0.1% | 0.5% | 2% |
| Elytra | 0% | 0% | 0% | 0.5% |
| Beacon | 0% | 0% | 0.1% | 0.5% |
| Nether star | 0% | 0% | 0% | 0.25% |


## More information

[Admin commands and configuration](AirDrop-Administration).
