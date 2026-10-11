package com.celik.kingdoms;

import net.minecraft.network.syncher.SynchedEntityData;

/** Refresh the ordinary glow metadata without changing a saved tag or potion effect. */
public interface AirDropGlowEntity {
    SynchedEntityData.DataValue<Byte> kingdoms$refreshAirDropGlow();
}
