package com.celik.kingdoms;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import static com.celik.kingdoms.Rules.*;

/** Empty lists are inert. Every decision uses current server time and current administrator policy. */
public final class WarRestrictions {
    public record Blast(String source,UUID actor){}
    private static final ThreadLocal<Blast> BLAST=new ThreadLocal<>();
    private static final ThreadLocal<String> INTERACTION=new ThreadLocal<>();
    private static final ThreadLocal<Integer> CASTING=ThreadLocal.withInitial(()->0);
    public static final class Scope implements AutoCloseable {
        private final Runnable restore;
        private Scope(Runnable restore){this.restore=restore;}
        public void close(){restore.run();}
    }
    public static boolean activeWar(KingdomService s,Level level,BlockPos pos,UUID actor){
        Kingdom land=s.land(level,pos),own=actor==null?null:s.own(actor);long now=s.now();
        return land!=null&&Rules.war(s.state,land.id,now)!=null||own!=null&&Rules.war(s.state,own.id,now)!=null;
    }
    public static boolean restricted(Level level,BlockPos pos,UUID actor){
        KingdomService s=Protection.service(level);if(s==null||!s.config.warRestrictions.enabled)return false;
        return switch(s.config.warRestrictions.scope){
            case "global"->true;
            case "war"->activeWar(s,level,pos,actor);
            default->{Kingdom land=s.land(level,pos);yield land!=null&&Rules.war(s.state,land.id,s.now())!=null;}
        };
    }
    public static boolean instantBreak(ServerPlayer player,Level level,BlockPos pos,BlockState state){
        KingdomService s=Protection.service(level);
        return s!=null&&!s.config.warRestrictions.instantBreakBlocks.isEmpty()&&restricted(level,pos,player.getUUID())&&activeWar(s,level,pos,player.getUUID())
            &&s.config.warRestrictions.instantBreakBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
            &&state.getDestroySpeed(level,pos)>=0&&Protection.permitted(level,pos,player.getUUID(),Permission.BREAK)&&!s.artifacts.preventsMining(player);
    }
    public static boolean placement(Level level,BlockPos pos,BlockState before,BlockState next){
        KingdomService s=Protection.service(level);if(s==null||s.config.warRestrictions.blockedPlacementBlocks.isEmpty()||before.getBlock()==next.getBlock()||!s.config.warRestrictions.blockedPlacementBlocks.contains(BuiltInRegistries.BLOCK.getKey(next.getBlock()).toString()))return true;
        Protection.Context c=Protection.current();return !restricted(level,pos,c==null?null:c.actor());
    }
    public static boolean entityAllowed(Entity entity){
        if(entity instanceof Player)return true;KingdomService s=Protection.service(entity.level());
        return s==null||s.config.warRestrictions.blockedEntities.isEmpty()||!s.config.warRestrictions.blockedEntities.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())||!restricted(entity.level(),entity.blockPosition(),Protection.owner(entity));
    }
    public static void inspect(Entity entity){if(!entityAllowed(entity))entity.discard();}
    public static boolean pearlBlocked(Entity owner,Level destination,BlockPos pos){
        KingdomService s=Protection.service(destination);if(s==null||!s.config.warRestrictions.blockPearlTeleport)return false;UUID actor=Protection.owner(owner);
        return restricted(destination,pos,actor)||owner!=null&&restricted(owner.level(),owner.blockPosition(),actor);
    }
    public static boolean preventPearlThrow(ServerPlayer player){
        return pearlBlocked(player,player.level(),player.blockPosition())||KingdomsMod.service!=null&&KingdomsMod.service.config.warRestrictions.blockedEntities.contains("minecraft:ender_pearl")&&restricted(player.level(),player.blockPosition(),player.getUUID());
    }
    public static Scope interaction(Level level,BlockPos pos){
        String previous=INTERACTION.get();Block block=level.getBlockState(pos).getBlock();
        String key=block instanceof BedBlock?"minecraft:bed":block instanceof RespawnAnchorBlock?"minecraft:respawn_anchor":null;
        if(key==null)INTERACTION.remove();else INTERACTION.set(key);
        return new Scope(()->{if(previous==null)INTERACTION.remove();else INTERACTION.set(previous);});
    }
    public static String explosionKey(Entity source){return source!=null?BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString():INTERACTION.get()==null?"minecraft:other":INTERACTION.get();}
    public static boolean preventDetonation(Level level,BlockPos origin,Entity source){
        KingdomService s=Protection.service(level);if(s==null)return false;UUID actor=Protection.owner(source);Protection.Context c=Protection.current();if(actor==null&&c!=null)actor=c.actor();
        return WarConfig.matches(s.config.warRestrictions.blockedExplosions,explosionKey(source))&&restricted(level,origin,actor);
    }
    public static Scope blast(Entity source){
        Blast previous=BLAST.get();UUID actor=Protection.owner(source);Protection.Context c=Protection.current();if(actor==null&&c!=null)actor=c.actor();BLAST.set(new Blast(explosionKey(source),actor));
        return new Scope(()->{if(previous==null)BLAST.remove();else BLAST.set(previous);});
    }
    public static boolean preventBlast(Level level,BlockPos origin,BlockPos target,boolean terrain){
        KingdomService s=Protection.service(level);Blast blast=BLAST.get();if(s==null||blast==null)return false;WarConfig config=s.config.warRestrictions;
        if(!WarConfig.matches(config.blockedExplosions,blast.source)&&!(terrain&&WarConfig.matches(config.blockedExplosionBlockDamage,blast.source)))return false;
        return restricted(level,origin,blast.actor)||restricted(level,target,blast.actor);
    }
    public static Scope casting(){int previous=CASTING.get();CASTING.set(previous+1);return new Scope(()->{if(previous==0)CASTING.remove();else CASTING.set(previous);});}
    public static boolean preventFormation(Level level,BlockPos pos,BlockState old,BlockState next){
        KingdomService s=Protection.service(level);if(s==null||!s.config.warRestrictions.preventLavaCasting||CASTING.get()==0)return false;
        if(!(old.getFluidState().is(FluidTags.LAVA)||old.getFluidState().is(FluidTags.WATER))||!(next.is(Blocks.COBBLESTONE)||next.is(Blocks.STONE)||next.is(Blocks.OBSIDIAN)))return false;
        Protection.Context c=Protection.current();return restricted(level,pos,c==null?null:c.actor());
    }
    public static void reset(){BLAST.remove();INTERACTION.remove();CASTING.remove();}
    private WarRestrictions(){}
}
