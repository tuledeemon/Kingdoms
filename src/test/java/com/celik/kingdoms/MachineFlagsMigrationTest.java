package com.celik.kingdoms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;

class MachineFlagsMigrationTest {
    @Test void oldConfigDropsOnlyRemovedFlags(){
        Config config=Store.JSON.fromJson("{\"defaultFlags\":[\"AUTOMATION\",\"FIRE\",\"PISTONS\",\"PVP\"],\"creationFee\":12345}",Config.class);
        config.validate();assertEquals(Set.of(Rules.Flag.FIRE,Rules.Flag.PVP),config.defaultFlags);assertEquals(12345,config.creationFee);
        assertFalse(Store.JSON.toJson(config).contains("AUTOMATION"));assertFalse(Store.JSON.toJson(config).contains("PISTONS"));
    }
    @Test void oldKingdomAndNestedJournalKeepRemainingFlags(){
        Rules.Kingdom kingdom=Store.JSON.fromJson("{\"name\":\"Existing town\",\"treasury\":98765,\"flags\":[\"PISTONS\",\"EXPLOSIONS\",\"AUTOMATION\"]}",Rules.Kingdom.class);
        assertEquals(Set.of(Rules.Flag.EXPLOSIONS),kingdom.flags);assertEquals(98765,kingdom.treasury);
        Rules.State state=new Rules.State();state.kingdoms.put(kingdom.id,kingdom);
        var json=Store.JSON.toJson(state).replace("\"EXPLOSIONS\"","\"EXPLOSIONS\",\"AUTOMATION\",\"PISTONS\"");
        Rules.State restored=Store.JSON.fromJson(json,Rules.State.class);
        assertEquals(kingdom.flags,restored.kingdoms.get(kingdom.id).flags);assertEquals(kingdom.name,restored.kingdoms.get(kingdom.id).name);
    }
}
