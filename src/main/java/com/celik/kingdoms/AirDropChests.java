package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

/** Physical chests stay empty: the durable reward ledger serves their winners-only contents. */
public final class AirDropChests {
    final AirDrops drops;
    private String mutationKey;
    private boolean installing;
    private record Viewer(ServerPlayer player,UUID drop){}
    private final Map<UUID,Viewer> viewers=new HashMap<>();
    private record ChunkSite(ServerLevel level,int x,int z){}
    private final Map<ChunkSite,LevelChunk> pendingCleanup=new LinkedHashMap<>();
    AirDropChests(AirDrops drops){this.drops=drops;}
    static BlockPos pos(Drop d){return new BlockPos(d.x,d.y,d.z);}
    private static UUID marker(Level level,BlockPos pos){
        if(!(level instanceof ServerLevel server))return null;
        LevelChunk chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK) instanceof AirDropChestMarker marker?marker.kingdoms$airDropId():null;
    }
    public boolean protects(Level level,BlockPos pos){return marker(level,pos)!=null||marker(level,pos.above())!=null;}
    public boolean internal(Level level,BlockPos pos,BlockState next){return mutationKey!=null&&mutationKey.equals(KingdomService.dimension(level)+"|"+pos.asLong())&&(installing?next.is(Blocks.CHEST):next.isAir());}
    private void change(ServerLevel level,BlockPos pos,BlockState next){String previous=mutationKey;boolean wasInstalling=installing;mutationKey=KingdomService.dimension(level)+"|"+pos.asLong();installing=next.is(Blocks.CHEST);try{if(!level.setBlockAndUpdate(pos,next))throw new IllegalStateException("AirDrop chest mutation was denied");}finally{mutationKey=previous;installing=wasInstalling;}}
    private boolean located(Drop d,Level level,BlockPos pos){return d!=null&&d.dimension.equals(KingdomService.dimension(level))&&pos.equals(pos(d));}
    public boolean canUse(ServerPlayer player,Drop d){return d!=null&&d.phase==Phase.CLAIMED&&drops.service.now()<d.expiresAt&&d.recipients.contains(player.getUUID())&&d.dimension.equals(KingdomService.dimension(player.level()))&&player.distanceToSqr(d.x+.5,d.y+.5,d.z+.5)<=64&&d.id.equals(marker(player.level(),pos(d)));}
    public boolean use(ServerPlayer player,BlockPos pos){UUID id=marker(player.level(),pos);if(id==null)return false;Drop d=AirDrops.find(drops.data,id);if(!located(d,player.level(),pos)||d.phase==Phase.EXPIRED||d.phase==Phase.CANCELLED||drops.service.now()>=d.expiresAt){Ui.tell(player,"This AirDrop has expired.",true);removeOrphan(player.level(),pos,id);return true;}if(d.phase!=Phase.CLAIMED){Ui.tell(player,"Capture this AirDrop before opening its chest.",true);return true;}Ui.run(player,()->{drops.service.require(canUse(player,d),"Only this AirDrop's winners can open the chest. Stand close to it.");AirDropChestMenu.open(player,d.id);});return true;}
    void sync(long now){
        drainCleanup();
        for(Drop d:drops.data.drops){ServerLevel level=drops.level(d.dimension);if(level==null)continue;BlockPos pos=pos(d);
            boolean needed=(d.phase==Phase.CAPTURABLE||d.phase==Phase.CLAIMED)&&now<d.expiresAt&&d.loot.stream().anyMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING);
            if(needed){level.getChunkAt(pos);UUID marker=marker(level,pos);if(d.id.equals(marker))continue;if(marker!=null)throw new IllegalStateException("AirDrop chest location belongs to another event");if(!AirDrops.landingSpace(level,pos))throw new IllegalStateException("AirDrop chest location is occupied; existing terrain was preserved");change(level,pos,Blocks.CHEST.defaultBlockState());if(!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest))throw new IllegalStateException("AirDrop chest block entity is missing");((AirDropChestMarker)chest).kingdoms$airDropId(d.id);}
            else if(level.hasChunkAt(pos)&&d.id.equals(marker(level,pos)))change(level,pos,Blocks.AIR.defaultBlockState());
        }
        for(Viewer v:new ArrayList<>(viewers.values())){Drop d=AirDrops.find(drops.data,v.drop);boolean ours=v.player.containerMenu instanceof AirDropChestMenu menu&&menu.drop.equals(v.drop);if(!canUse(v.player,d)||!ours){if(ours)v.player.closeContainer();closed(v.player,v.drop);}}
    }
    /** C2ME fires CHUNK_LOAD before publishing its full-chunk future. Never query/mutate the world here. */
    void chunkLoaded(ServerLevel level,LevelChunk chunk){
        if(chunk.getBlockEntities().values().stream().anyMatch(e->e instanceof AirDropChestMarker marker&&marker.kingdoms$airDropId()!=null))
            pendingCleanup.put(new ChunkSite(level,chunk.getPos().x(),chunk.getPos().z()),chunk);
    }
    void chunkUnloaded(ServerLevel level,LevelChunk chunk){pendingCleanup.remove(new ChunkSite(level,chunk.getPos().x(),chunk.getPos().z()),chunk);}
    private void drainCleanup(){
        // Terrain updates can load adjacent chunks and queue more work; do not iterate the live map.
        for(var entry:new ArrayList<>(pendingCleanup.entrySet())){
            ChunkSite site=entry.getKey();
            LevelChunk ready=site.level.getChunkSource().getChunkNow(site.x,site.z);
            if(ready==null)continue;
            pendingCleanup.remove(site,entry.getValue());
            if(ready!=entry.getValue())continue;
            // Re-read live markers: block entities may have changed since the notification.
            for(BlockEntity entity:new ArrayList<>(ready.getBlockEntities().values()))
                if(entity instanceof AirDropChestMarker marker&&marker.kingdoms$airDropId()!=null)
                    removeOrphan(site.level,entity.getBlockPos(),marker.kingdoms$airDropId());
        }
    }
    private void removeOrphan(ServerLevel level,BlockPos pos,UUID id){Drop d=AirDrops.find(drops.data,id);if(!located(d,level,pos)||d.phase==Phase.EXPIRED||d.phase==Phase.CANCELLED||drops.service.now()>=d.expiresAt||d.phase==Phase.CLAIMED&&d.loot.stream().noneMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING))if(id.equals(marker(level,pos)))change(level,pos,Blocks.AIR.defaultBlockState());}
    private void lid(Drop d,boolean open){ServerLevel level=drops.level(d.dimension);if(level==null||!d.id.equals(marker(level,pos(d))))return;int count=(int)viewers.values().stream().filter(v->v.drop.equals(d.id)).count();level.blockEvent(pos(d),Blocks.CHEST,1,count);if(drops.config().sounds)level.playSound(null,pos(d),open?SoundEvents.CHEST_OPEN:SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.5f,1);}
    void opened(ServerPlayer player,UUID id){viewers.put(player.getUUID(),new Viewer(player,id));drops.glows.looting(player,true);lid(drops.drop(id),true);}
    void closed(ServerPlayer player,UUID id){Viewer v=viewers.get(player.getUUID());if(v==null||!v.drop.equals(id))return;viewers.remove(player.getUUID());drops.glows.looting(player,false);Drop d=AirDrops.find(drops.data,id);if(d!=null)lid(d,false);}
    void leave(UUID id){Viewer v=viewers.get(id);if(v!=null)closed(v.player,v.drop);}
    void stop(){pendingCleanup.clear();for(Viewer v:new ArrayList<>(viewers.values())){v.player.closeContainer();closed(v.player,v.drop);}}
}
