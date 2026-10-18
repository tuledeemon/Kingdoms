# Admin War Restrictions

Administrators can control wartime blocks, entities, pearls, explosions and lava casting through **`/kingdomadmin war`**. Mayors cannot change these rules or override them with kingdom flags. No client mod is required.

**Defaults are empty lists, with pearl and lava-casting bans off.** Adding this update does not automatically ban obsidian, TNT or any other item.

## Quick examples

```text
/kingdomadmin war block add minecraft:obsidian
/kingdomadmin war set blockPearlTeleport true
/kingdomadmin war explosion add minecraft:tnt
/kingdomadmin war explosion add minecraft:tnt_minecart
/kingdomadmin war set preventLavaCasting true
```

These make obsidian instant to mine during active war, prevent pearl teleporting, stop TNT/TNT-minecart detonations and prevent lava/water block formation within the configured scope.

To allow explosions but stop their block damage, use the separate **blockdamage** list:

```text
/kingdomadmin war explosion remove minecraft:tnt
/kingdomadmin war explosion remove minecraft:tnt_minecart
/kingdomadmin war blockdamage add minecraft:tnt
/kingdomadmin war blockdamage add minecraft:tnt_minecart
```

That permits explosion damage and knockback against entities under the existing PvP/claim rules, while preserving terrain inside the restricted area.

## Where rules apply

```text
/kingdomadmin war set scope war_claims
```

The default **`war_claims`** scope applies only inside claimed chunks whose kingdom is currently in the **active, vulnerable phase of war**. Preparation and peaceful territory use their usual rules. All participating coalition kingdoms are covered. Wilderness keeps ordinary Minecraft behavior.

Other admin options:

| Scope | Behavior |
| --- | --- |
| `war_claims` | Active-war claimed chunks only |
| `war` | Active-war claimed chunks, plus actions attributed to active-war participants anywhere |
| `global` | Explicit server-wide restrictions, including peace and wilderness |

**Instant mining always additionally requires an active-war context**, even with `global` selected. It is available only where the player already has permission to break. Outsiders receive no access, core capture still uses its existing rules, and unbreakable blocks remain unbreakable. Vanilla tools, drops, enchantments and durability still apply.

Pearls check the thrower's position and the impact location: throwing from outside a war claim cannot bypass a restricted destination. Denied throws from a restricted position preserve the pearl; an already flying pearl that reaches a restricted location is discarded without teleporting its owner.

Explosion bans are also checked against affected blocks and entities. A TNT blast outside a restricted border cannot damage protected targets inside it. A **blockdamage** ban protects only terrain; it does not grant explosion immunity to players or mobs.

Operators follow configured restrictions too. Use the admin commands to change the policy.

## Block lists

```text
/kingdomadmin war block add obsidian
/kingdomadmin war block remove obsidian
/kingdomadmin war block list
/kingdomadmin war block clear
```

The **block** list makes matching blocks instant to mine during active war. Existing blocks stay in place until someone mines them. Adding a block does not prohibit placing it.

To forbid placement separately:

```text
/kingdomadmin war placement add obsidian
/kingdomadmin war placement remove obsidian
```

Placement bans reject changes that introduce that block in a restricted location, including piston/environmental placement through normal server block changes. Existing blocks remain. Changing a property of the same block is allowed.

Commands accept vanilla and registered modded block IDs. Omit `minecraft:` for vanilla IDs. Air cannot be listed; the instant-mining command also rejects unbreakable blocks such as bedrock.

## Entity bans

```text
/kingdomadmin war entity add tnt
/kingdomadmin war entity add tnt_minecart
/kingdomadmin war entity add end_crystal
/kingdomadmin war entity add creeper
/kingdomadmin war entity remove creeper
```

Matching entities cannot spawn in restricted situations. Existing entities are discarded when they tick in a restricted situation, including entities entering from outside. This applies to actual registered entity types, not their item/block names. Banning `minecraft:tnt` as an entity bans primed TNT, not the unlit TNT block; use **placement** to ban the block.

**Players cannot be banned as an entity type.** There is no entity wildcard. Mob approval and mayor permissions do not bypass an admin entity ban.

Entity-spawning items may still be consumed by vanilla mechanics when their resulting entity is rejected. For pearls, the dedicated teleport restriction also prevents throws from restricted positions.

## Explosions

Both lists support registered entity IDs, including:

| Source ID | Explosion source |
| --- | --- |
| `minecraft:tnt` | Primed TNT |
| `minecraft:tnt_minecart` | TNT minecarts |
| `minecraft:creeper` | Creepers |
| `minecraft:end_crystal` | End crystals |
| `minecraft:fireball` | Large fireballs |
| `minecraft:wither` | Wither explosions |
| `minecraft:wither_skull` | Wither skulls |
| `minecraft:wind_charge` | Player wind charges |
| `minecraft:breeze_wind_charge` | Breeze wind charges |
| `minecraft:bed` | Vanilla bed explosions during player interaction, any bed color |
| `minecraft:respawn_anchor` | Vanilla respawn-anchor explosions during player interaction |
| `minecraft:other` | Unattributed explosions without an entity or recognized interaction |
| `*` or `all` | Every source, including modded sources |

The **explosion** list stops detonation in the restricted situation, including normal explosion packets, entity damage, block damage and explosion fire. Vanilla explosives may still be consumed/disappear when their attempted explosion is cancelled.

The **blockdamage** list allows the explosion while blocking terrain damage, chain priming and explosion-created fire in restricted blocks. It preserves the original per-block protection checks and does not replace Minecraft's blast calculations.

```text
/kingdomadmin war explosion add all
/kingdomadmin war explosion clear
/kingdomadmin war blockdamage add all
/kingdomadmin war blockdamage list
```

If a source appears in both lists, the detonation ban takes precedence. A separate entity ban also prevents that entity from existing in the restricted situation.

## Lava casting

```text
/kingdomadmin war set preventLavaCasting true
```

Prevents vanilla water/lava contact from forming **cobblestone, stone or obsidian**, including downward-flowing lava. Existing generated blocks remain. Players may still place ordinary stone/cobblestone unless those blocks are in the placement list. Other fluid protections continue to apply. This does not block basalt generation from blue ice and soul soil.

## Full command reference

| Command | Action |
| --- | --- |
| `/kingdomadmin war` | Show scope, toggles and all lists |
| `/kingdomadmin war set enabled <true/false>` | Toggle this entire rule system |
| `/kingdomadmin war set scope <war_claims/war/global>` | Change where rules apply |
| `/kingdomadmin war set blockPearlTeleport <true/false>` | Toggle the pearl teleport ban |
| `/kingdomadmin war set preventLavaCasting <true/false>` | Toggle the fluid-formation ban |
| `/kingdomadmin war <list> add <id>` | Add a block, entity or explosion source |
| `/kingdomadmin war <list> remove <id>` | Remove an entry |
| `/kingdomadmin war <list> list [page]` | View entries, twenty per page |
| `/kingdomadmin war <list> clear` | Empty one list |

Lists are **`block`**, **`placement`**, **`entity`**, **`explosion`**, and **`blockdamage`**. Admin commands retain tab completion for settings and registered identifiers.

Every successful command saves immediately and applies immediately, including during an existing war. Invalid command values are rejected without changing the running configuration. Settings survive restarts in **`config/kingdoms/config.json` → `warRestrictions`**. Existing servers keep their other settings. You may also edit the configuration and use `/kingdomadmin reload`.
