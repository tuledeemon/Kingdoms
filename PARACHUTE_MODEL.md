# AirDrop parachute — Kingdoms 1.6.3

The descending AirDrop now has a round, sixteen-section canopy with eight broad alternating orange/ivory fabric gores, a shallow lining, stitched seams, a top vent, a reinforced rim and eight continuous suspension lines. The complete crate assembly sways gently and settles onto its exact landing position. Display position interpolation keeps the descent smooth.

The parachute uses **192 unsaved vanilla block displays**, replacing the previous 81-part stepped square and segmented ropes. The same bounded geometry is used at every size. Displays disappear on landing, cancellation and server shutdown, and are rebuilt from event state after restart. Nothing is downloaded by the running server or its clients. No resource pack or additional mod is required.

## Source and conversion

- [CC0 AirDrop crate with parachute](https://3dassets.dev/assets/wipe-day-survival-island-airdrop-crate-ef2e0e2f): the original GLB supplies the canopy profile and eight rigging directions. Its CC0 1.0 license permits modification and distribution. The source is preserved in `research/parachute/airdrop-source.glb`; the bundled model records its SHA-256 and source page.
- [SansCraftBDE converter](https://github.com/SansCraft-Network/sanscraft-bde#3d-model-converters): the researched converter supports GLB/OBJ-to-block-display conversion through voxelization and greedy merging. It is a Paper plugin and is **not installed or bundled** in Kingdoms. Its MIT-licensed source and license were retained as research references.
- Kingdoms' offline `tools/convert-parachute.py` converts this GLB into fitted, rotated fabric panels, avoiding a voxel staircase. It handles normalized quantized vertices and node transforms, separates canopy/rigging components, rounds the canopy into sixteen sections, recolors it with vanilla wool, reshapes its lining, and exports the compact display geometry. The source barrel is replaced by Kingdoms' existing crate.
- The generated `src/main/resources/data/kingdoms/airdrop/parachute.json` is bundled inside the mod. The server validates its part count, dimensions and rotations before use. Each part contains its material role, offset, scale and quaternion rotation.

To regenerate the geometry and preview, use Python with `numpy` and `Pillow`:

```text
python tools/convert-parachute.py
```

`research/parachute/parachute-preview.png` is a geometry render. Minecraft applies the configured vanilla block textures; this preview is not an in-game screenshot.

## Settings

These fields are inside `airDrops` in `config/kingdoms/config.json`. Existing admin settings commands validate and save them.

| Field | Default | Purpose |
|---|---|---|
| `parachute` | `true` | Enable the canopy, rigging and sway |
| `parachuteScale` | `1` | Size multiplier, 0.25–3; default canopy is about 5.4 blocks wide |
| `parachuteSway` | `0.12` | Maximum sideways sway in blocks, 0–1; zero disables sway |
| `parachutePrimaryBlock` | `minecraft:orange_wool` | First fabric color |
| `parachuteSecondaryBlock` | `minecraft:white_wool` | Second fabric color and lining |
| `parachuteSeamBlock` | `minecraft:light_gray_wool` | Stitched canopy seams |
| `parachuteTrimBlock` | `minecraft:brown_wool` | Rim, vent border and attachment knots |
| `parachuteRopeBlock` | `minecraft:white_wool` | Suspension lines |

```text
/kingdomadmin airdrop set parachuteScale 1.25
/kingdomadmin airdrop set parachuteSway 0.12
/kingdomadmin airdrop set parachutePrimaryBlock minecraft:red_wool
/kingdomadmin airdrop set parachuteSecondaryBlock minecraft:white_wool
/kingdomadmin airdrop spawn standard
```

Use a full cube block for the fabric/ropes to preserve the shape. Unknown block identifiers and air are rejected. Scale, materials and sway apply when visuals are created; to inspect changes, spawn a new descending test crate. The crate remains one block wide and rope roots remain attached when the canopy is resized. The display count stays constant, although large canopies can increase client rendering work.

## Verification

The 1.6.3 regression runs pass 431 dedicated-server checks without C2ME, 432 with C2ME, and 101 unit tests. New checks validate native display rotations/materials, continuous rope endpoints, attached ropes at the smallest/largest supported scales, shared bounded sway, exact landing coordinates, invalid material rejection, disabled parachutes and complete cleanup. The generated geometry preview was inspected. A connected Minecraft client's rendered textures and animation have not been visually reviewed.
