package com.celik.kingdoms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdminSettingsTest {
    @Test void percentagesUseRealProbabilityAndRejectNonFinite() {
        assertEquals(.001,AdminSettings.chance("0.1%"));
        assertEquals(.25,AdminSettings.chance("0.25"));
        assertThrows(IllegalArgumentException.class,()->AdminSettings.chance("101%"));
        assertThrows(IllegalArgumentException.class,()->AdminSettings.chance("NaN"));
    }
    @Test void cashFollowsRealDayBoundariesAndArtifactCratesStayCashFree() {
        var c=new AirDropConfig();c.validate();
        assertEquals(1000,c.money(1));assertEquals(1000,c.money(2));
        assertEquals(10000,c.money(3));assertEquals(50000,c.money(5));assertEquals(100000,c.money(7));
        assertFalse(c.artifactDropsHaveMoney);
        c.moneyByDay.put(7,0L);assertEquals(0,c.money(70));
        c.moneyByDay.put(7,-1L);assertThrows(IllegalArgumentException.class,c::validate);
    }
}
