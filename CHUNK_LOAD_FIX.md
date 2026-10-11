# Joining-world freeze — Kingdoms 1.6.3

## Reproduction and evidence

The supplied 1.6.1 crash report shows the main server thread upgrading overworld chunk `[25, 30]` under C2ME `0.4.1-beta.1.0+26.2`. During the chunk-load event, Kingdoms called `AirDropChests.chunkLoaded → removeOrphan → marker → Level.getBlockEntity → ServerChunkCache.getChunkBlocking`. The server then waited for the same full chunk whose load callback had not returned. The watchdog terminated it after 60 seconds. The previous log records authentication completing before the stall, explaining the client remaining on Joining world.

## Fix

The chunk-load callback only inspects the provided chunk's block-entity map and queues marked chunks. It does not request chunks or change world blocks. Normal AirDrop polling checks whether each queued chunk is already fully available using the nonblocking `getChunkNow` lookup, then re-reads live markers and removes orphan chests. Unloads discard queued references; replacement chunks are checked by object identity. Shutdown clears the queue. Marker lookups used by protection and access checks also use already available chunks instead of forcing loads.

Legitimate event chests, ordinary chest inventories, saved loot and current Kingdoms settings are preserved. Deleting `airdrops.json` is unnecessary and can turn remaining marked chests into orphans while losing event/reward data.

## Validation

The original 1.6.3 fix passed 432 checks with the exact C2ME version in the report and 101 JUnit tests. The current 1.7.0 release retains this fix and passes 440 C2ME checks, 439 ordinary Fabric checks and 102 JUnit tests. The C2ME regression confirms the full chunk is unpublished when the queued callback runs.

The server regression injects an orphan chest in the actual chunk-load event before Kingdoms runs. It checks that the callback returns, leaves terrain untouched during loading, and removes the chest after full availability. With C2ME enabled, it also verifies that the callback ran before the full chunk was published. An unpublished synthetic chunk verifies that access/protection and cleanup do not request missing chunks. Existing chest, capture, artifact, cash and protection checks run alongside the regression.

C2ME test dependency: official Modrinth version `hfq4dPFH`, SHA-512 `21076491de36c5d680cafa494116b98a134a3d7e3cbcb77a59942bab0ceacf6c9386a80b1e4d870e501aafab27b89c0b5d7a09f1392a4d4ab43a87f9d3524841`. C2ME is used only in the isolated compatibility run and is not bundled in Kingdoms.

## Installation

Stop the server, replace the previous Kingdoms JAR with `kingdoms-1.7.0-mc26.2.jar`, then start it again. Keep all world and Kingdoms data files. A real player join on the complete production modpack still needs confirmation after replacement; this workspace does not have access to that running server.
