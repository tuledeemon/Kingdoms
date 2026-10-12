# Placeholders


These use Text Placeholder API syntax, for example `%kingdoms:kingdom_styled%`. Rank-based leaderboard placeholders take a one-based argument, for example `%kingdoms:leaderboard_entry/1%`.

| Placeholder | Value |
| --- | --- |
| `player_kingdom`, `kingdom_name`, `kingdom_display_name` | Player kingdom identity |
| `kingdom_styled` | Item object icon + space + small-caps kingdom display name; `ɴᴏɴᴇ` without a kingdom |
| `kingdom_icon` | Real Minecraft 26.2 atlas `object` text component for the saved item icon |
| `kingdom_icon_material` | Saved item identifier |
| `kingdom_treasury`, `kingdom_treasury_raw` | Formatted/raw treasury balance |
| `kingdom_rank`, `kingdom_member_count`, `kingdom_claim_count` | Small-caps role, member count, and actual claimed chunk count |
| `kingdom_claim_slots`, `kingdom_outpost_count` | Weighted claim-slot use and outpost count |
| `kingdom_at_war` | Whether claims are currently vulnerable |
| `war_preparation_remaining`, `wartime_remaining`, `enemy_kingdom` | Current conflict state |
| `territory_kingdom`, `territory_icon` | Claim at the player's position |
| `inside_own_kingdom`, `inside_enemy_territory`, `in_wilderness` | Position booleans |
| `leaderboard_position/<rank>` | Requested one-based position |
| `leaderboard_name/<rank>` | Ranked identity |
| `leaderboard_icon/<rank>`, `leaderboard_icon_material/<rank>` | Ranked object icon/material |
| `leaderboard_treasury/<rank>`, `leaderboard_treasury_raw/<rank>` | Ranked balance |
| `leaderboard_entry/<rank>` | Combined `#1 [object icon] small-caps name — balance` component |

New kingdoms use paper as their default icon. Mayors can hold an item and select **Kingdom icon** in the settings menu to change it.

`%kingdoms:kingdom_styled%` preserves the real object icon in component-aware formatting systems. Systems that convert components to plain text cannot display the actual sprite. It is never added as a chat prefix automatically.

Inline icons resolve through a sprite mapping generated from the exact Minecraft 26.2 client assets. Model aliases and multi-state items use a verified representative texture, including clock/compass frames, crossbow standby, shared stick/apple textures and tipped-arrow base. These are static icons; item-model state, layered tinting and enchantment glint are not rendered by atlas object text. Shields use the vanilla shield GUI silhouette. Unknown modded item assets use paper for the inline sprite while retaining the selected item in hover data and the inventory leaderboard.

To regenerate the mapping for this version, run `tools/generate-icon-sprites.ps1 -ClientJar <path-to-26.2-client.jar>`. This ships only sprite identifiers, without Minecraft textures, resource packs or client-side code.

Kingdom tags have been removed, including `/kingdom tag`, `kingdom_tag`, `territory_tag`, `leaderboard_tag`, `maxTagLength` and `leaderboardShowTag`. Old tag fields in saved data/config are ignored; other kingdom data is preserved. Replace retired tag placeholders in server formatting with `%kingdoms:kingdom_styled%` or a name placeholder.

`KingdomIcons.stack(Kingdom)` is the Java-side `ItemStack` bridge for integrations that render item objects directly. The placeholder itself returns Minecraft's inline atlas object component, with the selected item attached as hover data. It is never inserted into chat automatically.
