# Artifact Administration

## Configuration

Edit `artifacts` in `config/kingdoms/config.json`, then `/kingdomadmin reload`. Probabilities are fractions (`0.001` = 0.10%). Generated identities keep their saved ability stats. Keep internal `abilityBalanceVersion` at `2`.

| Setting | Default / meaning |
|---|---|
| `enabled`, `announcements` | `true`; enable artifact abilities/rolls and crate announcements |
| `chanceByDay` | `{"1":0.001,"3":0.0015,"5":0.002,"7":0.003}`; per-crate artifact probability |
| `fixerChance`, `fixerCount` | `0.5`, `2`; optional fixer chance/quantity |
| `fixerItem` | `minecraft:echo_shard`; fixer appearance |
| `repairFraction`, `repairLevels` | `0.25`, `5`; fixer repair amount and XP price |
| `upgradeIngots` | `3`; Netherite upgrade price |
| `rebindOnTrade` | `true`; update the tracked holder on legitimate trades |
| `types.<type>.enabled`, `weight`, `minDay` | Enable a type, relative selection weight and unlock day |
| `name`, `baseItem`, `rarity` | Identity presentation; use a damageable base for tools and Heart of the Sea for Guardian Angel |
| `maxUses` | Native maximum durability; `0` uses the base item's default. Guardian Angel instead uses this as its charge capacity |
| `upgradedMaxUses` | Upgraded native maximum; `0` preserves the type's normal upgrade behavior |
| `netheriteUpgradeable` | Whether the menu offers a Netherite upgrade |
| `cooldownSeconds` | Optional ability cooldown; default `0` |
| `upgradedValue`, `upgradedDurationSeconds` | Upgraded reflection/healing fraction and timed-effect duration |
| `variants[].weight`, `chance` | Relative generated-stat variant weight and activation probability, where applicable; default chance `1` |
| `variants[].value` | Healing/reflection fraction |
| `variants[].durationSeconds` | Timed-effect seconds; supports fractions such as `1.5` |
| `variants[].amplifier` | Built-in Wind Burst level, default `2` for the spear |
| `variants[].rarity` | Generated rarity |

Type IDs: `thunder_axe`, `thorns_shield`, `frostbite_sword`, `multi_pickaxe`, `vampire_sword`, `void_scythe`, `guardian_angel`, `wind_burst_spear`.

## Admin commands

```text
/kingdomadmin artifact give thunder_axe
/kingdomadmin artifact give guardian_angel
/kingdomadmin artifact fixer
/kingdomadmin artifact inspect
/kingdomadmin artifact status
/kingdomadmin artifact retire <id>
/kingdomadmin artifact recover
/kingdomadmin artifact chance 7 0.3%
/kingdomadmin artifact set upgradeIngots 3
/kingdomadmin artifact type thunder_axe maxUses 512
/kingdomadmin artifact type thunder_axe upgradedMaxUses 768
/kingdomadmin artifact variant frostbite_sword 1 durationSeconds 1
```

`set` edits scalar global settings, `type` edits a type and `variant` edits a numbered variant starting at one. Collections can be edited in JSON. To guarantee an artifact crate during testing, temporarily set the current day's artifact chance to `100%`, spawn a normal test AirDrop, then restore the original probability.

## Existing-server migration

The old catalogue is replaced once when loading its configuration. Overall AirDrop artifact odds and fixer settings remain intact. Removed identities are retired and lose their abilities when encountered. Shared old types adopt the new mechanics, preserving their IDs, owners and upgrades; old remaining-use percentages become native durability on their item stacks. Current-version administrator changes survive reloads. Migration also covers records inside pending transactions. Kingdom/world/AirDrop files are not deleted.

## Artifact repair and Void Scythe update (1.10.2)

The default Void Scythe maximum is 1,000 native durability. Older 10-durability defaults and saved identities migrate once; existing authentic stacks keep their remaining durability percentage when encountered. Other custom values survive. The migration marker is `durabilityBalanceVersion`; leave it in place so later customizations are not reset.

One artifact can be repaired with an ordinary matching item in crafting, anvils or grindstones, regardless of input order. Its identity and ability remain. Two-artifact combinations produce no output, and consume neither input. Smithing artifacts remains blocked; use the dedicated Netherite upgrade menu. Vanilla crafting/grindstone enchantment removal still applies, except the spear's built-in Wind Burst is preserved.

Items that already lost their identity before the update cannot safely be matched back to a unique artifact record; an administrator must issue a replacement if needed.
