package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

/** Vanilla entity types and packets only. Visual entities never save; JSON is the authoritative crate. */
public final class AirDropVisuals {
    public static final class Crate extends Display.BlockDisplay {
        public final UUID drop;
        Crate(ServerLevel level,UUID drop){super(EntityTypes.BLOCK_DISPLAY,level);this.drop=drop;}
        @Override public boolean shouldBeSaved(){return false;}
    }
    public static final class HitBox extends Interaction {
        public final UUID drop;
        HitBox(ServerLevel level,UUID drop){super(EntityTypes.INTERACTION,level);this.drop=drop;}
        @Override public boolean shouldBeSaved(){return false;}
    }
    final ServerLevel level;
    final Crate crate;
    final HitBox hit;
    final float sway;
    record Rod(Crate entity,Vec3 offset){}
    final List<Rod> outline=new ArrayList<>(12);
    final List<Rod> parachute=new ArrayList<>();
    AirDropVisuals(ServerLevel level,Drop drop,AirDropConfig config){
        this.level=level;sway=config.parachute?config.parachuteSway:0;crate=new Crate(level,drop.id);hit=new HitBox(level,drop.id);
        CompoundTag display=new CompoundTag(),block=new CompoundTag(),brightness=new CompoundTag();block.putString("Name",config.crateBlock);display.put("block_state",block);display.putBoolean("Glowing",true);display.putInt("glow_color_override",0xFFD34D);display.putFloat("view_range",8);display.putBoolean("NoGravity",true);display.putBoolean("Invulnerable",true);brightness.putInt("block",15);brightness.putInt("sky",15);display.put("brightness",brightness);display.putInt("teleport_duration",Math.min(59,config.pollTicks));
        crate.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),display));
        CompoundTag hitData=new CompoundTag();hitData.putFloat("width",1.4f);hitData.putFloat("height",1.4f);hitData.putBoolean("response",true);hitData.putBoolean("Invulnerable",true);hit.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),hitData));
        if(config.descentOutline)for(CoreHighlight.Edge edge:CoreHighlight.edges(BlockPos.ZERO)){
            Vec3 length=edge.end().subtract(edge.start());float width=.025f;CompoundTag rodData=display.copy(),rodBlock=new CompoundTag(),transform=new CompoundTag();rodBlock.putString("Name","minecraft:white_concrete");rodData.put("block_state",rodBlock);
            transform.put("translation",floats(0,0,0));transform.put("left_rotation",floats(0,0,0,1));transform.put("right_rotation",floats(0,0,0,1));transform.put("scale",floats(length.x==0?width:(float)length.x+width,length.y==0?width:(float)length.y+width,length.z==0?width:(float)length.z+width));rodData.put("transformation",transform);rodData.putFloat("shadow_radius",0);rodData.putFloat("shadow_strength",0);
            Crate rod=new Crate(level,drop.id);rod.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),rodData));outline.add(new Rod(rod,edge.start().subtract(width/2,width/2,width/2)));
        }
        if(config.parachute)for(ParachuteModel.Part raw:ParachuteModel.parts())parachute.add(part(drop,display,ParachuteModel.scaled(raw,config.parachuteScale),ParachuteModel.block(raw,config)));
        move(drop,System.currentTimeMillis());
        if(!level.addFreshEntity(crate)||!level.addFreshEntity(hit)||outline.stream().anyMatch(r->!level.addFreshEntity(r.entity))||parachute.stream().anyMatch(r->!level.addFreshEntity(r.entity))){clear();throw new IllegalStateException("AirDrop visual entity spawn was denied");}
    }
    private Rod part(Drop drop,CompoundTag display,ParachuteModel.Part part,String block){CompoundTag tag=display.copy(),state=new CompoundTag(),transform=new CompoundTag();state.putString("Name",block);tag.put("block_state",state);tag.putBoolean("Glowing",false);tag.putFloat("shadow_radius",0);tag.putFloat("shadow_strength",0);transform.put("translation",floats(0,0,0));transform.put("left_rotation",floats(part.rotation()));transform.put("right_rotation",floats(0,0,0,1));transform.put("scale",floats(part.scale()));tag.put("transformation",transform);Crate e=new Crate(level,drop.id);e.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));return new Rod(e,new Vec3(part.offset()[0],part.offset()[1],part.offset()[2]));}
    private static ListTag floats(float... values){ListTag list=new ListTag();for(float value:values)list.add(FloatTag.valueOf(value));return list;}
    void move(Drop drop,long now){double y=drop.phase==Phase.DESCENDING?drop.height(now):drop.y;double amplitude=drop.phase==Phase.DESCENDING?sway*Math.min(1,Math.max(0,(y-drop.y)/3)):0;double phase=(now-drop.createdAt)/1000d+Math.floorMod(drop.id.hashCode(),628)/100d;double x=drop.x+Math.sin(phase*.55)*amplitude,z=drop.z+Math.cos(phase*.39)*amplitude*.65;crate.setPos(x,y,z);hit.setPos(x+.5,y,z+.5);for(Rod rod:outline)rod.entity.setPos(x+rod.offset.x,y+rod.offset.y,z+rod.offset.z);for(Rod rod:parachute)rod.entity.setPos(x+rod.offset.x,y+rod.offset.y,z+rod.offset.z);}
    boolean removed(){return crate.isRemoved()||hit.isRemoved()||outline.stream().anyMatch(r->r.entity.isRemoved())||parachute.stream().anyMatch(r->r.entity.isRemoved());}
    void clear(){crate.discard();hit.discard();outline.forEach(r->r.entity.discard());parachute.forEach(r->r.entity.discard());}
    static List<Vec3> ringPoints(Drop drop,int count){List<Vec3> points=new ArrayList<>(count);for(int i=0;i<count;i++){double angle=i*Math.PI*2/count;points.add(new Vec3(drop.x+.5+Math.cos(angle)*drop.captureRadius,drop.y+.12,drop.z+.5+Math.sin(angle)*drop.captureRadius));}return points;}
    static void fx(AirDrops drops,ServerLevel level,Drop drop,long now){fx(drops,level,drop,now,drops.nearby(drop));}
    static void fx(AirDrops drops,ServerLevel level,Drop drop,long now,List<ServerPlayer> players){DustParticleOptions dust=new DustParticleOptions(drops.visualColor(drop),drops.config().dustSize);for(Vec3 point:ringPoints(drop,drops.config().ringPoints))for(ServerPlayer player:players)if(player.connection!=null&&player.level()==level)level.sendParticles(player,dust,true,false,point.x,point.y,point.z,0,0,0,0,0);double y=drop.phase==Phase.DESCENDING?drop.height(now):drop.y;level.sendParticles(ParticleTypes.END_ROD,drop.x+.5,y+1.1,drop.z+.5,3,.5,.5,.5,.01);if(drop.phase==Phase.CAPTURABLE)for(int i=1;i<=6;i++)level.sendParticles(ParticleTypes.END_ROD,drop.x+.5,drop.y+i*5,drop.z+.5,1,0,0,0,0);}
}
