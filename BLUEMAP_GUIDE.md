# BlueMap — Kingdom Territories

Kingdoms 1.8.0 automatically adds a **ᴋɪɴɢᴅᴏᴍꜱ** layer when BlueMap is installed and its API is ready. Players can toggle the layer in BlueMap's map menu.

## What players see

- Colored territory fills and clear borders, matching each kingdom's core color.
- Adjacent claimed chunks merged into regions, with disconnected outposts shown separately. Unclaimed holes stay uncolored.
- Click a region to see its kingdom's name, mayors, member/online counts, treasury, claim/outpost counts and current war status.
- Core pins at the actual saved center locations, including captured cores. Carried cores have no pin until placed again.
- Small-caps labels and formatted information panels.

The map follows actual saved claims in each dimension. Kingdoms does not claim or load chunks for the overlay. Claiming, unclaiming, disbanding, moving cores, changing members and war transitions appear at the next refresh, normally within ten seconds. BlueMap's web update interval can add a short delay.

## Install

1. Install the **Fabric build of BlueMap compatible with Minecraft 26.2** alongside Kingdoms 1.8.0. BlueMap is optional; Kingdoms still works without it.
2. Complete [BlueMap's setup](https://bluemap.blue/wiki/getting-started/Installation.html), including the resource-download agreement and any web-port allocation your host requires.
3. Start the server and let BlueMap initialize its maps. Open its web viewer and enable **ᴋɪɴɢᴅᴏᴍꜱ** if the layer is hidden.

No Towny, separate territory addon, client mod, resource pack or manual marker file is needed. Kingdoms uses BlueMap API 2.7.7 and does not bundle BlueMap or its API. BlueMap's own rendering and web-server configuration remains separate.

On startup, the log reports `BlueMap kingdom territory integration registered`. If BlueMap is not ready yet, Kingdoms waits for it. Markers are rebuilt after BlueMap reloads and removed when the integration is disabled or the server stops. Other marker layers are preserved.

## Admin commands

```text
/kingdomadmin bluemap set enabled true
/kingdomadmin bluemap set showCoreMarkers false
/kingdomadmin bluemap set showTreasury false
/kingdomadmin bluemap set updateIntervalSeconds 5
/kingdomadmin bluemap set fillOpacityPercent 25
/kingdomadmin bluemap set lineWidth 4
```

These commands use the normal Kingdoms administrator permission level. Changes apply automatically. For collections, edit `config/kingdoms/config.json` and run `/kingdomadmin reload`.

## Configuration

All settings below live inside the root **`blueMap`** object in `config/kingdoms/config.json`. Existing configurations receive the defaults on load.

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Enable automatic kingdom markers when BlueMap is installed |
| `updateIntervalSeconds` | `10` | Refresh period; 1–3600 seconds |
| `layerLabel` | `kingdoms` | Layer title, rendered in small caps |
| `defaultHidden` | `false` | Initially hide the layer; players can toggle it |
| `showCoreMarkers` | `true` | Show placed core locations as pins |
| `showMayors` | `true` | Include mayor names in click details |
| `showMemberCount` | `true` | Include total members and currently online members |
| `showMemberNames` | `false` | Include the complete member roster |
| `showTreasury` | `true` | Include the kingdom's shared treasury balance |
| `showWarStatus` | `true` | Show peace, preparation or active war, enemy and remaining time |
| `depthTest` | `false` | If false, borders/fills remain visible through rendered terrain |
| `lineWidth` | `3` | Border width in pixels; 0–20 |
| `borderOpacityPercent` | `90` | Border opacity; 0–100 |
| `fillOpacityPercent` | `18` | Territory fill opacity; 0–100 |
| `markerHeight` | `64` | Flat overlay height; -2048–2048; does not change actual claims |
| `maxDistance` | `100000000` | Maximum marker visibility distance in BlueMap; positive integer |
| `mapIds` | `[]` | Empty means all matching maps; otherwise list exact BlueMap map IDs |

For example, `"mapIds": ["overworld"]` limits the layer to that configured map. Map IDs are BlueMap configuration identifiers, not Minecraft dimension names. World lookup uses the actual Fabric dimension, so changing a map's display name does not mix territories between worlds.

The layer ID is `kingdoms:territories`; reserve that ID for this integration. Popup text is escaped before rendering as HTML. Shape generation runs on a separate worker and reuses unchanged claim geometry.

## Verification status

This release compiles against the official BlueMap API. Live BlueMap web-viewer behavior has not been verified on your server. Existing 1.7.1 test results do not verify this integration or the 1.7.2 explosion patch.
