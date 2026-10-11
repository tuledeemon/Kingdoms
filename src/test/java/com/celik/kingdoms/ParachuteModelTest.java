package com.celik.kingdoms;
import org.junit.jupiter.api.Test;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.junit.jupiter.api.Assertions.*;

class ParachuteModelTest {
    private static Vector3f ropeStart(ParachuteModel.Part p){return new Vector3f((float)p.offset()[0],(float)p.offset()[1],(float)p.offset()[2]).add(new Quaternionf(p.rotation()[0],p.rotation()[1],p.rotation()[2],p.rotation()[3]).transform(new Vector3f(p.scale()[0]/2,0,p.scale()[2]/2)));}
    private static Vector3f ropeEnd(ParachuteModel.Part p){return ropeStart(p).add(new Quaternionf(p.rotation()[0],p.rotation()[1],p.rotation()[2],p.rotation()[3]).transform(new Vector3f(0,p.scale()[1],0)));}
    @Test void convertedCanopyHasBoundedGeometryAndContinuousRopes(){
        var parts=ParachuteModel.parts();assertTrue(parts.size()<=256);assertEquals(8,parts.stream().filter(p->p.role().equals("rope")).count());
        for(var p:parts)if(p.role().equals("rope")){assertEquals(1.02,ropeStart(p).y,.00001);assertEquals(5,ropeEnd(p).y,.00001);assertEquals(2.7,Math.hypot(ropeEnd(p).x-.5,ropeEnd(p).z-.5),.00001);assertTrue(p.scale()[1]>4);}
    }
    @Test void canopyColorsAndDetailRemainSeparate(){
        var cfg=new AirDropConfig();var parts=ParachuteModel.parts();assertTrue(parts.stream().anyMatch(p->p.role().equals("seam")));assertTrue(parts.stream().anyMatch(p->p.role().equals("lining")));
        assertEquals("minecraft:orange_wool",ParachuteModel.block(parts.stream().filter(p->p.role().equals("primary")).findFirst().orElseThrow(),cfg));
        cfg.parachutePrimaryBlock="minecraft:red_wool";assertEquals("minecraft:red_wool",ParachuteModel.block(parts.stream().filter(p->p.role().equals("primary")).findFirst().orElseThrow(),cfg));
    }
    @Test void scalingKeepsEveryRopeAttachedToTheOriginalCrate(){
        for(var p:ParachuteModel.parts())if(p.role().equals("rope"))for(float factor:new float[]{.25f,1.5f,3}){
            var scaled=ParachuteModel.scaled(p,factor);assertTrue(ropeStart(p).distance(ropeStart(scaled))<.00002);
            Vector3f expected=ropeEnd(p);expected.set(.5f+(expected.x-.5f)*factor,1+(expected.y-1)*factor,.5f+(expected.z-.5f)*factor);assertTrue(expected.distance(ropeEnd(scaled))<.00003);
        }
    }
    @Test void unsafeGeometrySettingsRejected(){
        var c=new AirDropConfig();c.parachuteScale=Float.NaN;assertThrows(IllegalArgumentException.class,c::validate);
        c=new AirDropConfig();c.parachuteScale=3.1f;assertThrows(IllegalArgumentException.class,c::validate);
        c=new AirDropConfig();c.parachuteSway=-.1f;assertThrows(IllegalArgumentException.class,c::validate);
    }
}
