package com.celik.kingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Attributes spreading hazards to their initiator, including across scheduled ticks and restarts. */
public final class Provenance {
 public static class Entry {public UUID owner;public String block;Entry(UUID owner,String block){this.owner=owner;this.block=block;}}
 public static class Data {public Map<String,Entry> blocks=new HashMap<>();public Map<UUID,UUID> entities=new HashMap<>();}
 private static Data data=new Data();private static boolean dirty;
 static String key(Level l,BlockPos p){return KingdomService.dimension(l)+"|"+p.getX()+"|"+p.getY()+"|"+p.getZ();}
 public static void load(Store store){data=store.read("hazard-owners.json",Data.class,new Data());if(data.blocks==null||data.entities==null)throw new IllegalStateException("Invalid hazard ownership data");dirty=false;}
 public static UUID blockOwner(Level l,BlockPos p){Entry e=data.blocks.get(key(l,p));if(e==null)return null;return Objects.equals(e.block,BuiltInRegistries.BLOCK.getKey(l.getBlockState(p).getBlock()).toString())?e.owner:null;}
 public static void changed(Level l,BlockPos p,BlockState state){if(Protection.service(l)==null)return;String key=key(l,p);Protection.Context ctx=Protection.current();UUID owner=ctx==null?null:ctx.actor();
  boolean hazard=!state.getFluidState().isEmpty()||state.is(BlockTags.FIRE)||state.is(Blocks.TNT);
  if(hazard&&owner!=null){if(data.blocks.size()>=KingdomsMod.service.config.maxTrackedHazards&&!data.blocks.containsKey(key))return;data.blocks.put(key,new Entry(owner,BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));dirty=true;}
  else if(data.blocks.remove(key)!=null)dirty=true;
 }
 public static UUID entityOwner(UUID entity){return data.entities.get(entity);}
 public static void entity(UUID entity,UUID owner){if(owner!=null&&data.entities.size()<KingdomsMod.service.config.maxTrackedHazards){data.entities.put(entity,owner);dirty=true;}}
 public static void removed(UUID entity){if(data.entities.remove(entity)!=null)dirty=true;}
 public static void flush(Store store){if(dirty){store.write("hazard-owners.json",data);dirty=false;}}
 public static void reset(){data=new Data();dirty=false;}
}
