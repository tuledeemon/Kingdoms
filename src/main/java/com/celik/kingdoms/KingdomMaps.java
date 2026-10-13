package com.celik.kingdoms;

import net.fabricmc.loader.api.FabricLoader;

/** Keep every BlueMap API type behind an optional, separately loaded implementation. */
public interface KingdomMaps {
    KingdomMaps NONE = new KingdomMaps(){public void tick(){}public void stop(){}};
    void tick();
    void stop();
    static KingdomMaps start(KingdomService service){
        if(!FabricLoader.getInstance().isModLoaded("bluemap"))return NONE;
        try{return new BlueMapIntegration(service);}
        catch(RuntimeException|LinkageError failure){KingdomsMod.LOG.error("BlueMap integration unavailable; Kingdoms will continue without map markers",failure);return NONE;}
    }
}
