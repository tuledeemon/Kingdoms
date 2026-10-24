package com.celik.kingdoms;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import static com.celik.kingdoms.Rules.*;

final class CoreSkyIntegration {
    static void run(KingdomService s,ServerLevel level,UUID owner,UUID holder,BiConsumer<Boolean,String> check){
        State original=s.store.copy(s.state);boolean enabled=s.config.coreSkyAccessRequired;int timeout=s.config.coreCoveredTimeoutSeconds;
        Core start=s.state.kingdoms.get(owner).core;BlockPos base=KingdomCores.position(start),roof=base.above(6);
        Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        for(int y=base.getY()+1;y<=level.getMaxY();y++){BlockPos pos=new BlockPos(base.getX(),y,base.getZ());blocks.put(pos,level.getBlockState(pos));}
        try{
            s.config.coreSkyAccessRequired=true;s.config.coreCoveredTimeoutSeconds=300;
            for(BlockPos pos:blocks.keySet())put(s,level,owner,pos,Blocks.AIR.defaultBlockState());
            long now=s.now();checkSky(s,owner,now);
            check.accept(core(s,owner).coveredSince==0,"exposed core has no coverage timer");
            put(s,level,owner,roof,Blocks.GLASS.defaultBlockState());checkSky(s,owner,now);
            check.accept(core(s,owner).coveredSince==now,"transparent roof starts continuous core coverage timer");
            State saved=s.store.read("kingdoms.json",State.class,new State());
            check.accept(saved.kingdoms.get(owner).core.coveredSince==now,"core coverage timer persists for restart recovery");
            checkSky(s,owner,now+299999);
            check.accept(KingdomCores.position(core(s,owner)).equals(base),"covered core stays in place before five minutes");
            put(s,level,owner,roof,Blocks.AIR.defaultBlockState());checkSky(s,owner,now+299999);
            check.accept(core(s,owner).coveredSince==0,"uncovering a core resets the continuous timer");
            put(s,level,owner,roof,Blocks.GLASS.defaultBlockState());checkSky(s,owner,now+300000);UUID nonce=core(s,owner).nonce;
            checkSky(s,owner,now+600000);
            check.accept(KingdomCores.position(core(s,owner)).equals(roof)&&!nonce.equals(core(s,owner).nonce)&&core(s,owner).coveredSince==0,"five minutes of coverage relocates core to top of same column and refreshes nonce");
            check.accept(level.getBlockState(base).is(Blocks.STONE)&&level.getBlockState(roof).is(Blocks.GLASS),"relocation changes designation without moving or duplicating original terrain");
            s.commit(s.store.copy(original));put(s,level,owner,roof,Blocks.BEDROCK.defaultBlockState());checkSky(s,owner,now);checkSky(s,owner,now+300000);
            check.accept(KingdomCores.position(core(s,owner)).equals(roof.above())&&level.getBlockState(roof.above()).is(Blocks.STONE),"unbreakable roof creates a breakable stone core above it");
            s.commit(s.store.copy(original));s.edit(owner,k->k.flags.add(Flag.FLUIDS));put(s,level,owner,roof.above(),Blocks.AIR.defaultBlockState());put(s,level,owner,roof,Blocks.WATER.defaultBlockState());checkSky(s,owner,now);checkSky(s,owner,now+300000);
            check.accept(KingdomCores.position(core(s,owner)).equals(roof.above())&&level.getBlockState(roof.above()).is(Blocks.STONE),"fluid roof cannot evade relocation and receives stone above it");
            s.commit(s.store.copy(original));put(s,level,owner,roof.above(),Blocks.AIR.defaultBlockState());put(s,level,owner,roof,Blocks.AIR.defaultBlockState());
            BlockPos ceiling=new BlockPos(base.getX(),level.getMaxY(),base.getZ());put(s,level,owner,ceiling,Blocks.BEDROCK.defaultBlockState());
            checkSky(s,owner,now);checkSky(s,owner,now+300000);checkSky(s,owner,now+301000);
            check.accept(core(s,owner).skyMoveBlocked&&KingdomCores.position(core(s,owner)).equals(base),"build-height obstruction keeps core valid and records one blocked-move warning");
            put(s,level,owner,ceiling,Blocks.AIR.defaultBlockState());put(s,level,owner,roof,Blocks.STONE.defaultBlockState());checkSky(s,owner,now+302000);
            check.accept(KingdomCores.position(core(s,owner)).equals(roof)&&!core(s,owner).skyMoveBlocked,"blocked relocation retries automatically once space becomes available");
            s.commit(s.store.copy(original));s.edit(owner,k->{k.core.carried=true;k.core.carriedByKingdom=holder;k.core.takenAt=now;});checkSky(s,owner,now+600000);
            check.accept(core(s,owner).carried&&core(s,owner).coveredSince==0,"carried tokens are exempt from sky access checks");
            s.commit(s.store.copy(original));checkSky(s,owner,now);s.config.coreSkyAccessRequired=false;checkSky(s,owner,now+600000);
            check.accept(core(s,owner).coveredSince==0&&KingdomCores.position(core(s,owner)).equals(base),"disabling sky enforcement clears coverage without relocation");
            s.config.coreSkyAccessRequired=true;s.commit(s.store.copy(original));
            Core other=core(s,holder);BlockPos foreign=KingdomCores.position(other).east(3),foreignRoof=foreign.above(5);
            BlockState oldBase=level.getBlockState(foreign),oldRoof=level.getBlockState(foreignRoof);
            try{
                put(s,level,holder,foreign,Blocks.STONE.defaultBlockState());put(s,level,holder,foreignRoof,Blocks.STONE.defaultBlockState());
                s.edit(owner,k->{k.core.x=foreign.getX();k.core.y=foreign.getY();k.core.z=foreign.getZ();k.core.tributeTo=holder;k.core.taxRemainders.put(UUID.randomUUID(),17);});
                checkSky(s,owner,now);checkSky(s,owner,now+300000);
                check.accept(KingdomCores.position(core(s,owner)).equals(foreignRoof)&&holder.equals(core(s,owner).tributeTo)&&core(s,owner).taxRemainders.containsValue(17),"captured core relocates within holder territory without changing tribute");
            }finally{s.commit(s.store.copy(original));put(s,level,holder,foreign,oldBase);put(s,level,holder,foreignRoof,oldRoof);}
        }finally{
            s.commit(s.store.copy(original));for(var entry:blocks.entrySet())put(s,level,owner,entry.getKey(),entry.getValue());
            s.config.coreSkyAccessRequired=enabled;s.config.coreCoveredTimeoutSeconds=timeout;
        }
    }
    private static Core core(KingdomService s,UUID id){return s.state.kingdoms.get(id).core;}
    private static void checkSky(KingdomService s,UUID id,long now){s.cores.checkSky(s.state.kingdoms.get(id),now);}
    private static void put(KingdomService s,ServerLevel level,UUID owner,BlockPos pos,BlockState block){try(var scope=Protection.scope(level,pos,s.state.kingdoms.get(owner).mayors.iterator().next(),null,Protection.Cause.PLAYER)){level.setBlockAndUpdate(pos,block);}}
    private CoreSkyIntegration(){}
}
