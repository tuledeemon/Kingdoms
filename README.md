# Kingdoms

A server-side **Fabric mod for Minecraft 26.2**. Build kingdoms, claim land, manage a shared treasury and fight timed wars. AirDrops and eight special artifacts add recurring objectives for kingdoms and solo players.

Players join with normal Minecraft clients. No client mod or resource pack is required.

**1.8.0:** Optional BlueMap kingdom territories, clickable information and core pins. Includes the 1.7.2 explosion-protection patch. See the [changelog](CHANGELOG.md).

## Player guide

Start with `/kingdom` in-game, or read **[How Kingdoms Works](https://github.com/tuledeemon/Kingdoms/wiki/How-Kingdoms-Works)**.

- [Guide home](https://github.com/tuledeemon/Kingdoms/wiki)
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
- [Placeholders](https://github.com/tuledeemon/Kingdoms/wiki/Placeholders)
- [BlueMap territories](https://github.com/tuledeemon/Kingdoms/wiki/BlueMap)
- [Build instructions](BUILDING.md)
- [Full technical reference](PROJECT_REFERENCE.md)
- [Example configuration](config.example.json)
- [Parachute model attribution](PARACHUTE_MODEL.md)

Version **1.7.1** passed **102 unit tests**, **545 dedicated-server checks**, and **546 checks with C2ME**. Version **1.8.0** compiles with the optional BlueMap integration and 1.7.2 explosion patch; the earlier test results do not verify these changes. Live client visuals and third-party mod compatibility still need testing.
