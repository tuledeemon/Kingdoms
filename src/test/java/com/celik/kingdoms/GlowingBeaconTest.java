package com.celik.kingdoms;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.celik.kingdoms.Rules.*;

class GlowingBeaconTest {
    @Test void glowingIsHostileAndSupportsOnlyLevelOne(){Config config=new Config();assertFalse(BeaconPower.GLOWING.beneficial());assertEquals(300000,KingdomBeacon.cost(config,BeaconPower.GLOWING,1));assertThrows(IllegalArgumentException.class,()->KingdomBeacon.cost(config,BeaconPower.GLOWING,2));assertEquals(4,Arrays.stream(BeaconPower.values()).filter(p->!p.beneficial()).count());}
    @Test void existingPricesMigrateOnceWithoutOverwritingCustomValues(){Config config=Store.JSON.fromJson("{\"beaconEffectFees\":{\"speed\":123}}",Config.class);config.validate();assertEquals(123,config.beaconEffectFees.get("speed"));assertEquals(300000,config.beaconEffectFees.get("glowing"));assertEquals(1,config.beaconEffectVersion);config.beaconEffectFees.remove("glowing");config.validate();assertFalse(config.beaconEffectFees.containsKey("glowing"));assertThrows(IllegalArgumentException.class,()->KingdomBeacon.cost(config,BeaconPower.GLOWING,1));}
    @Test void migrationPreservesAnAlreadyConfiguredGlowingPrice(){Config config=Store.JSON.fromJson("{\"beaconEffectFees\":{\"glowing\":700000}}",Config.class);config.validate();assertEquals(700000,config.beaconEffectFees.get("glowing"));Config saved=Store.JSON.fromJson(Store.JSON.toJson(config),Config.class);saved.validate();assertEquals(1,saved.beaconEffectVersion);assertEquals(700000,saved.beaconEffectFees.get("glowing"));}
}
