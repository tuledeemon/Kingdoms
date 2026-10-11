package com.celik.kingdoms;
import java.util.UUID;
/** Identifies an event chest independently of its loaded chunk or event history. */
public interface AirDropChestMarker {
    UUID kingdoms$airDropId();
    void kingdoms$airDropId(UUID id);
}
