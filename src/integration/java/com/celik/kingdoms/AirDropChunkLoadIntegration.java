package com.celik.kingdoms;

import java.util.UUID;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Inject a persisted orphan before Kingdoms sees a real loading chunk. Test mod only. */
final class AirDropChunkLoadIntegration {
    private static ChunkPos armed;
    private static BlockPos fixture;
    private static boolean callbackReturned,notPublished;
    static void register(){
        Identifier early=Identifier.parse("kingdoms_test:orphan_fixture");
        ServerChunkEvents.CHUNK_LOAD.addPhaseOrdering(early,Event.DEFAULT_PHASE);
        ServerChunkEvents.CHUNK_LOAD.register(early,(level,chunk,generated)->{
            if(armed==null||!chunk.getPos().equals(armed))return;
            fixture=new BlockPos(armed.getMinBlockX()+8,100,armed.getMinBlockZ()+8);
            chunk.setBlockState(fixture,Blocks.CHEST.defaultBlockState(),0);
            ChestBlockEntity chest=new ChestBlockEntity(fixture,Blocks.CHEST.defaultBlockState());
            ((AirDropChestMarker)chest).kingdoms$airDropId(UUID.randomUUID());chest.setLevel(level);
            chunk.getBlockEntities().put(fixture,chest);
            notPublished=level.getChunkSource().getChunkNow(armed.x(),armed.z())==null;
            KingdomsMod.service.airDrops.chests.chunkLoaded(level,chunk);
            callbackReturned=true;
        });
    }
    static void run(AirDrops drops,ServerLevel level,BiConsumer<Boolean,String> check){
        armed=new ChunkPos(777,779);fixture=null;callbackReturned=false;notPublished=false;
        try{
            level.getChunk(armed.x(),armed.z());
            check.accept(callbackReturned,"real chunk-load callback with a persisted orphan returns without waiting on its own chunk");
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("c2me"))
                check.accept(notPublished,"C2ME regression exercises an orphan before the full chunk is published");
            check.accept(level.getBlockState(fixture).is(Blocks.CHEST),"orphan cleanup does not mutate terrain during the load callback");
            drops.chests.sync(drops.service.now());
            check.accept(level.getBlockState(fixture).isAir(),"deferred cleanup removes the orphan once the real chunk is fully available");
        }finally{armed=null;}
        ChunkPos absent=new ChunkPos(17000,17001);BlockPos pos=new BlockPos(absent.getMinBlockX()+8,100,absent.getMinBlockZ()+8);
        LevelChunk unpublished=new LevelChunk(level,absent);
        ChestBlockEntity stale=new ChestBlockEntity(pos,Blocks.CHEST.defaultBlockState());
        ((AirDropChestMarker)stale).kingdoms$airDropId(UUID.randomUUID());unpublished.getBlockEntities().put(pos,stale);
        drops.chests.chunkLoaded(level,unpublished);
        check.accept(!drops.chests.protects(level,pos)&&level.getChunkSource().getChunkNow(absent.x(),absent.z())==null,"marker protection queries never force an unavailable chunk to load");
        drops.chests.sync(drops.service.now());
        check.accept(level.getChunkSource().getChunkNow(absent.x(),absent.z())==null,"deferred cleanup does not load an unavailable chunk");
        drops.chests.chunkUnloaded(level,unpublished);
    }
}
