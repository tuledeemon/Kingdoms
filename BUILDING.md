# Building Kingdoms

Kingdoms targets **Minecraft 26.2**, **Java 25** and Fabric. Use the included Gradle wrapper (9.4.1).

## Dependencies

Create `libs/` and put these separately distributed dependency JARs in it:

| Dependency | Tested version | Required filename for compilation |
| --- | --- | --- |
| EconomyCraft | 1.10.0 for 26.2 | `economycraft-fabric-1.10.0_26.2.jar` |
| Text Placeholder API | 3.1.0-beta.1+26.2 | `placeholder-api-3.1.0-beta.1+26.2.jar` |
| Architectury API | 21.1.9 | Any `.jar` filename; needed at runtime |

Fabric Loader and Fabric API are fetched by Gradle. External dependency JARs are not included in this repository or bundled into Kingdoms.

BlueMap API **2.7.7** is fetched from the official `repo.bluecolored.de` Maven repository as a compile-only dependency. BlueMap itself is optional at runtime; neither it nor the API is bundled.

## Compile

```text
./gradlew build
```

On Windows, use `gradlew.bat`. The built mod and source JAR appear in `build/libs/`.

For Windows Java socket failures, create `C:\Temp` and set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Temp` before running Gradle. Preserve Unicode paths and quote them; do not rename Windows accounts.

## Verification

```text
./gradlew test
./gradlew runIntegration
```

`runIntegration` starts a disposable localhost Minecraft server on port 25691, generates its test-only configuration and accepted EULA file, then shuts down after the checks. Read [Minecraft's EULA](https://www.minecraft.net/eula) before running it. Never copy its server properties or integration mod into a public server.

To repeat the C2ME checks, add the exact compatible C2ME JAR to `run-test/mods/` before the integration run. The tested version was `0.4.1-beta.1.0+26.2`.

## Updating the guide

Edit `PLAYER_GUIDE.md`, `AIRDROPS_GUIDE.md`, `ARTIFACTS_GUIDE.md` or `PROJECT_REFERENCE.md`, then run:

```text
python tools/sync-wiki.py
```

Generated wiki pages live in `wiki/`. The GitHub wiki is a separate repository; push these pages there when publishing documentation changes.

The generated icon mapping and parachute geometry are already included, so normal builds do not need Minecraft client assets or the original 3D model. Their offline regeneration tools are optional; see `PARACHUTE_MODEL.md` and `PROJECT_REFERENCE.md`.
