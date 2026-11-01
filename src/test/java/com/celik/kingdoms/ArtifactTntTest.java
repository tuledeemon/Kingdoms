package com.celik.kingdoms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArtifactTntTest {
    @Test void damageMultipliesThenCaps(){assertEquals(12,ArtifactTnt.damage(6,2,20));assertEquals(20,ArtifactTnt.damage(30,2,20));assertEquals(0,ArtifactTnt.damage(30,0,20));assertEquals(0,ArtifactTnt.damage(30,2,0));assertEquals(2,ArtifactTnt.damage(4,.5,20));}
    @Test void rejectsUnboundedOrNegativeSettings(){var c=new ArtifactConfig();c.instantTntDamageMultiplier=Double.NaN;assertThrows(IllegalArgumentException.class,c::validate);c.instantTntDamageMultiplier=1;c.instantTntMaxDamage=-1;assertThrows(IllegalArgumentException.class,c::validate);c.instantTntMaxDamage=20;c.instantFlintCooldownSeconds=-1;assertThrows(IllegalArgumentException.class,c::validate);}
    @Test void migrationAddsFlintAndKeepsCustomHoe(){var c=new ArtifactConfig();c.types.get("replant_hoe").enabled=false;var json=com.google.gson.JsonParser.parseString(Store.JSON.toJson(c)).getAsJsonObject();json.remove("instantFlintCatalogVersion");json.getAsJsonObject("types").remove("instant_flint");var loaded=Store.JSON.fromJson(json,ArtifactConfig.class);loaded.validate();assertFalse(loaded.types.get("replant_hoe").enabled);assertEquals("minecraft:flint_and_steel",loaded.types.get("instant_flint").baseItem);assertFalse(loaded.types.get("instant_flint").netheriteUpgradeable);}
}
