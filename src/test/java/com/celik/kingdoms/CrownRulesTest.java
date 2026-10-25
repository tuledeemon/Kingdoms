package com.celik.kingdoms;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CrownRulesTest {
    @Test void fiveDistinctVictimsGrantTwentyBonusHp(){var c=new ArtifactConfig();var life=new ArtifactRules.CrownLife();assertEquals(0,ArtifactRules.crownBonus(life,c));for(int i=1;i<=5;i++){life.victims.add(UUID.randomUUID());assertEquals(i*4,ArtifactRules.crownBonus(life,c));}life.victims.add(UUID.randomUUID());assertEquals(20,ArtifactRules.crownBonus(life,c));}
    @Test void repeatedVictimsDoNotIncreaseProgress(){var c=new ArtifactConfig();var life=new ArtifactRules.CrownLife();UUID id=UUID.randomUUID();life.victims.add(id);life.victims.add(id);assertEquals(4,ArtifactRules.crownBonus(life,c));}
    @Test void legacyStateLoadsAndPersonalProgressSurvivesReload(){var old=Store.JSON.fromJson("{}",ArtifactRules.Data.class);ArtifactRules.validate(old);assertTrue(old.crownLives.isEmpty());var life=new ArtifactRules.CrownLife();life.crown=UUID.randomUUID();life.victims.add(UUID.randomUUID());UUID owner=UUID.randomUUID();old.crownLives.put(owner,life);var loaded=Store.JSON.fromJson(Store.JSON.toJson(old),ArtifactRules.Data.class);ArtifactRules.validate(loaded);assertEquals(life.crown,loaded.crownLives.get(owner).crown);assertEquals(life.victims,loaded.crownLives.get(owner).victims);assertEquals(4,ArtifactRules.crownBonus(loaded.crownLives.get(owner),new ArtifactConfig()));}
    @Test void invalidHealthSettingsAreRejected(){var c=new ArtifactConfig();c.crownHealthPerUniqueKill=0;assertThrows(IllegalArgumentException.class,c::validate);c.crownHealthPerUniqueKill=4;c.crownMaxBonusHealth=-1;assertThrows(IllegalArgumentException.class,c::validate);}
}
