package com.celik.kingdoms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedAirDropBuffTest {
    @Test void advancedHasMoreRollsDiamondsAndLevelFourGear(){
        var c=new AirDropConfig();c.validate();var t=c.tier(5);assertEquals(10,t.minRolls);assertEquals(14,t.maxRolls);
        var diamonds=t.categories.stream().filter(x->x.key.equals("diamonds")).findFirst().orElseThrow();assertEquals(.60,diamonds.chance);assertEquals(8,diamonds.entries.getFirst().minCount);assertEquals(16,diamonds.entries.getFirst().maxCount);
        var combat=t.categories.stream().filter(x->x.key.equals("combat")).findFirst().orElseThrow();assertEquals(.70,combat.chance);assertTrue(combat.entries.stream().filter(e->!e.enchantments.isEmpty()).allMatch(e->e.enchantments.values().stream().allMatch(level->level==4)));
    }
    @Test void rareBuffStopsAtEndgame(){var c=new AirDropConfig();var totem=c.rareRewards.stream().filter(r->r.key.equals("totem")).findFirst().orElseThrow();assertEquals(.001,totem.chance(4));assertEquals(.01,totem.chance(5));assertEquals(.01,totem.chance(6));assertEquals(.02,totem.chance(7));}
    @Test void applyingPresetIsIdempotentAndPreservesOtherSettings(){
        var c=new AirDropConfig();c.endUnlockDay=30;c.netherUnlockDay=4;c.moneyByDay.put(5,76543L);c.variants.getFirst().weight=1234;c.lootTiers.getLast().minRolls=11;
        String early=Store.JSON.toJson(c.lootTiers.getFirst()),late=Store.JSON.toJson(c.lootTiers.getLast());AdvancedAirDropBuff.apply(c);c.validate();String once=Store.JSON.toJson(c);AdvancedAirDropBuff.apply(c);assertEquals(once,Store.JSON.toJson(c));
        assertEquals(30,c.endUnlockDay);assertEquals(4,c.netherUnlockDay);assertEquals(76543L,c.money(5));assertEquals(1234,c.variants.getFirst().weight);assertEquals(early,Store.JSON.toJson(c.lootTiers.getFirst()));assertEquals(late,Store.JSON.toJson(c.lootTiers.getLast()));
    }
    @Test void customTierDatesAndImplicitLaterRareChancesSurvive(){
        var c=new AirDropConfig();c.lootTiers.get(2).fromDay=11;c.lootTiers.get(3).fromDay=17;var rare=c.rareRewards.getFirst();rare.chanceByDay.clear();rare.chanceByDay.put(1,.003);AdvancedAirDropBuff.apply(c);assertEquals(11,c.lootTiers.get(2).fromDay);assertEquals(.003,rare.chance(10));assertEquals(.12,rare.chance(11));assertEquals(.003,rare.chance(17));
    }
    @Test void missingAdvancedTierIsRejected(){var c=new AirDropConfig();c.lootTiers.get(2).name="custom";assertThrows(IllegalArgumentException.class,()->AdvancedAirDropBuff.apply(c));}
}
