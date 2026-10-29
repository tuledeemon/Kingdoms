# Kingdoms

A server-side **Fabric mod for Minecraft 26.2**. Build kingdoms, claim land, manage a shared treasury and fight timed wars. AirDrops and special artifacts add recurring objectives for kingdoms and solo players.

Players join with normal Minecraft clients. No client mod or resource pack is required.

**1.12.2:** Removed player-head inventory cleanup. All player heads remain untouched.

**1.12.1:** Lightweight inventory-only cleanup removes `_Umut` player-head items once per second.

**1.11.2:** Towny-style liquid and machine boundaries: no Fluids flag, normal outward flow/exports into wilderness, bucket placement/collection tied to Place/Break permission, and protected piston retraction. Adds the global `preventFluidGriefing` setting and selective dispenser checks.

**1.11.1:** Removed piston/automation flags and claim-border redstone filtering. Pistons now validate the whole move before changing blocks, and accepted retraction/animation changes finish normally in claimed land. Existing saves migrate automatically.

**1.11.0:** Added Runner's Boots, King's Chestplate, Archer's Bow, Advanced Axe and Advanced Pickaxe. Armor effects respect stronger potions, and connected harvesting respects protection and native loot.

**1.10.2:** Covered cores move to the top of their column after five minutes, with mayor warnings and a stone fallback above fluids/unbreakable blocks. Artifact repairs keep their abilities in crafting, anvils and grindstones, in either input order. Void Scythe now has 1,000 durability, with existing default scythes migrated while preserving wear.

**1.10.1:** Added Glowing as a purchasable enemy beacon effect for spotting opponents through blocks, including underground. Includes the administrator war rules: selected blocks become instant to mine, with separate block-placement/entity bans, pearl teleport restrictions, explosion detonation/block-damage lists and lava-casting prevention. Configure everything through `/kingdomadmin war`. Includes alliances and joint wars. See the [changelog](CHANGELOG.md).

## Player guide

Start with `/kingdom` in-game, or read **[How Kingdoms Works](https://github.com/tuledeemon/Kingdoms/wiki/How-Kingdoms-Works)**.

- [Guide home](https://github.com/tuledeemon/Kingdoms/wiki)
- [Alliances and joint wars](https://github.com/tuledeemon/Kingdoms/wiki/Alliances)
- [AirDrops](https://github.com/tuledeemon/Kingdoms/wiki/AirDrops)
- [Artifacts](https://github.com/tuledeemon/Kingdoms/wiki/Artifacts)
- [Commands](https://github.com/tuledeemon/Kingdoms/wiki/Commands)
- [Combat and teleports](https://github.com/tuledeemon/Kingdoms/wiki/Combat-and-Teleports) — optional [Kingdoms Combat addon](https://github.com/tuledeemon/KingdomsCombat).
- [Staff mode and events](https://github.com/tuledeemon/Kingdoms/wiki/Staff-Mode-and-Events)

The documentation source is also available in [wiki/](wiki/Home.md). Prices, timers and limits in the guide are defaults; server owners can change them.

## Install

Download Kingdoms from [Releases](https://github.com/tuledeemon/Kingdoms/releases) and put it in your server's `mods` folder with:

- Minecraft **26.2**, Java **25** and Fabric Loader **0.19.5+**.
- Fabric API for 26.2 (tested with **0.160.0+26.2**).
- EconomyCraft **1.10.0+** for 26.2.
- Architectury API and Text Placeholder API (tested with **21.1.9** and **3.1.0-beta.1+26.2**).

Start the server and use `/kingdom`. Configuration is generated at `config/kingdoms/config.json`; saved data lives in `<world>/kingdoms/`.

Kingdoms runs on **Fabric**, not Paper or Folia. Server borders, addresses and Discord account linking are managed separately by the server owner.

## Server owners and developers

- [Installation](https://github.com/tuledeemon/Kingdoms/wiki/Installation)
- [Administration](https://github.com/tuledeemon/Kingdoms/wiki/Administration)
- [War restrictions](https://github.com/tuledeemon/Kingdoms/wiki/War-Restrictions)
- [Placeholders](https://github.com/tuledeemon/Kingdoms/wiki/Placeholders)
- [BlueMap territories](https://github.com/tuledeemon/Kingdoms/wiki/BlueMap)
- [Build instructions](BUILDING.md)
- [Full technical reference](PROJECT_REFERENCE.md)
- [Example configuration](config.example.json)
- [Parachute model attribution](PARACHUTE_MODEL.md)

Version **1.7.1** passed **102 unit tests**, **545 dedicated-server checks**, and **546 checks with C2ME**. Version **1.10.1** builds and passes local rule tests and dedicated-server checks, including actual instant-mining, entity rejection, pearl impact, explosions, wilderness behavior and lava/water formation. Live client visuals and compatibility with third-party server mods remain unverified.

## Tournament addon

[KingdomsCombat 1.5.2](https://github.com/tuledeemon/KingdomsCombat/releases/tag/v1.5.2) adds hosted duels, 1v1/2v2 tournaments, saved kits or personal gear with artifact recovery, gates, banner shields, shared Elo, optional voice moderation, playtime Discord roles and `/baltop`. [Setup and commands](https://github.com/tuledeemon/Kingdoms/wiki/Tournaments-and-Duels).
