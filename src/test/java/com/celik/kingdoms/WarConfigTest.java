package com.celik.kingdoms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class WarConfigTest {
    @Test void oldConfigurationsGetInertRestrictions(){Config config=Store.JSON.fromJson("{\"schemaVersion\":1}",Config.class);config.validate();assertTrue(config.warRestrictions.enabled);assertEquals("war_claims",config.warRestrictions.scope);assertFalse(config.warRestrictions.blockPearlTeleport);assertFalse(config.warRestrictions.preventLavaCasting);for(String list:List.of("block","placement","entity","explosion","blockdamage"))assertTrue(WarAdmin.entries(config.warRestrictions,list).isEmpty());}
    @Test void listsRemainIndependentAndPersist(){WarConfig config=new WarConfig();config.instantBreakBlocks.add("minecraft:obsidian");config.blockedExplosions.add("minecraft:tnt");config.blockedExplosionBlockDamage.add("minecraft:tnt_minecart");WarConfig copy=Store.JSON.fromJson(Store.JSON.toJson(config),WarConfig.class);copy.validate();assertEquals(config.instantBreakBlocks,copy.instantBreakBlocks);assertFalse(copy.blockedPlacementBlocks.contains("minecraft:obsidian"));assertFalse(WarConfig.matches(copy.blockedExplosions,"minecraft:tnt_minecart"));assertTrue(WarConfig.matches(copy.blockedExplosionBlockDamage,"minecraft:tnt_minecart"));}
    @Test void invalidScopesIdentifiersAndPlayerBansFail(){WarConfig config=new WarConfig();config.scope="anything";assertThrows(IllegalArgumentException.class,config::validate);config.scope="war";config.blockedEntities.add("minecraft:player");assertThrows(IllegalArgumentException.class,config::validate);config.blockedEntities.clear();config.instantBreakBlocks.add("obsidian");assertThrows(IllegalArgumentException.class,config::validate);}
    @Test void explosiveWildcardDoesNotExtendToOtherLists(){WarConfig config=new WarConfig();config.blockedExplosions.add("*");config.validate();assertTrue(WarConfig.matches(config.blockedExplosions,"modded:explosive"));config.blockedEntities.add("*");assertThrows(IllegalArgumentException.class,config::validate);}
}
