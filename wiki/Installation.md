# Installation


Put the release JAR in your **server** `mods` directory alongside:

- Fabric Loader **0.19.5+**, Minecraft **26.2**, Java **25**.
- Fabric API for 26.2 (tested with **0.160.0+26.2**).
- EconomyCraft Fabric **1.10.0+** for 26.2.
- EconomyCraft's dependencies: Architectury API and Placeholder API (tested with **21.1.9** and **3.1.0-beta.1+26.2**).

Start the server. Open `/kingdom` (alias `/k`). The server generates `config/kingdoms/config.json`. Kingdom data lives in `<world>/kingdoms/`; back up that entire directory together with EconomyCraft's data and the world.

Do not install the integration-test mod or use the test server's properties on a public server. Those are development fixtures only.


## Building from source

See the repository's `BUILDING.md`. Kingdoms is a Fabric server mod; it does not run on Paper or Folia. The world border and Discord account linking belong to the server setup, not this mod.

## Verification

The 1.7.1 release passed 102 unit tests, 545 dedicated-server checks, and 546 checks with C2ME. A connected Minecraft client and live anticheat compatibility have not been tested. The 1.7.2 explosion patch was compiled only; no new unit or test-server checks were run.
