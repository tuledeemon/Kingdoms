package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Private waypoint packets: never enable the world gamerule or change another player's HUD. */
final class CrownLocator {
    private final Artifacts artifacts;
    private final KingdomService service;
    private final Map<UUID,View> views=new HashMap<>();
    private int ticks;
    private record View(ServerPlayer player,net.minecraft.server.level.ServerLevel level,Map<UUID,Marker> markers){}
    private record Marker(UUID id,int mode,BlockPos position,ChunkPos chunk,float angle,Waypoint.Icon icon){
        boolean same(Marker other){return other!=null&&mode==other.mode&&icon.style.equals(other.icon.style)&&icon.color.equals(other.icon.color)&&switch(mode){case 0->position.equals(other.position);case 1->chunk.equals(other.chunk);default->Math.abs(Math.atan2(Math.sin(angle-other.angle),Math.cos(angle-other.angle)))<=0.008726646F;};}
        ClientboundTrackedWaypointPacket packet(boolean add){return switch(mode){case 0->add?ClientboundTrackedWaypointPacket.addWaypointPosition(id,icon,position):ClientboundTrackedWaypointPacket.updateWaypointPosition(id,icon,position);case 1->add?ClientboundTrackedWaypointPacket.addWaypointChunk(id,icon,chunk):ClientboundTrackedWaypointPacket.updateWaypointChunk(id,icon,chunk);default->add?ClientboundTrackedWaypointPacket.addWaypointAzimuth(id,icon,angle):ClientboundTrackedWaypointPacket.updateWaypointAzimuth(id,icon,angle);};}
    }
    CrownLocator(Artifacts artifacts,KingdomService service){this.artifacts=artifacts;this.service=service;}
    boolean wearing(ServerPlayer player){var record=artifacts.usable(player.getItemBySlot(EquipmentSlot.HEAD));return player.isAlive()&&!player.isSpectator()&&record!=null&&record.type.equals("golden_crown");}
    void tick(){if(++ticks%5==0)refresh();}
    void refresh(){
        List<ServerPlayer> online=List.copyOf(service.server.getPlayerList().getPlayers());Set<UUID> present=new HashSet<>();
        for(ServerPlayer player:online){present.add(player.getUUID());
            if(!ready(player)||!wearing(player)||player.level().getGameRules().get(GameRules.LOCATOR_BAR)){clear(player.getUUID());continue;}
            View view=views.get(player.getUUID());if(view!=null&&(view.player!=player||view.level!=player.level())){clear(player.getUUID());view=null;}
            if(view==null){view=new View(player,player.level(),new HashMap<>());views.put(player.getUUID(),view);}
            Set<UUID> visible=new HashSet<>();
            for(ServerPlayer target:online){
                if(target==player||target.level()!=player.level()||!target.isAlive()||target.isSpectator()||target.isInvisible()||target.isDiscrete()||!target.isTransmittingWaypoint())continue;
                double receive=player.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE);if(receive<=0)receive=60_000_000;
                if(player.distanceTo(target)>=Math.min(receive,target.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE)))continue;
                UUID targetId=target.getUUID();visible.add(targetId);Marker next=marker(player,target),old=view.markers.get(targetId);
                if(!next.same(old)){player.connection.send(next.packet(old==null||old.mode!=next.mode||!old.icon.style.equals(next.icon.style)||!old.icon.color.equals(next.icon.color)));view.markers.put(targetId,next);}
            }
            for(var it=view.markers.entrySet().iterator();it.hasNext();){var entry=it.next();if(!visible.contains(entry.getKey())){player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(entry.getValue().id));it.remove();}}
        }
        for(UUID id:new HashSet<>(views.keySet()))if(!present.contains(id))clear(id);
    }
    private Marker marker(ServerPlayer receiver,ServerPlayer target){
        UUID id=markerId(target.getUUID());Waypoint.Icon icon=new Waypoint.Icon();icon.copyFrom(target.waypointIcon().cloneAndAssignStyle(target));
        if(WaypointTransmitter.isReallyFar(target,receiver)){var direction=receiver.position().subtract(target.position()).rotateClockwise90();return new Marker(id,2,null,null,(float)Mth.atan2(direction.z(),direction.x()),icon);}
        if(!WaypointTransmitter.isChunkVisible(target.chunkPosition(),receiver))return new Marker(id,1,null,target.chunkPosition(),0,icon);
        return new Marker(id,0,target.blockPosition().immutable(),null,0,icon);
    }
    static UUID markerId(UUID target){return UUID.nameUUIDFromBytes(("kingdoms:crown_locator:"+target).getBytes(StandardCharsets.UTF_8));}
    private static boolean ready(ServerPlayer player){return player.connection!=null&&player.connection.isAcceptingMessages();}
    private void clear(UUID id){View view=views.remove(id);if(view!=null&&ready(view.player))for(Marker marker:view.markers.values())view.player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(marker.id));}
    void leave(ServerPlayer player){
        if(!service.server.isSameThread()){service.server.execute(()->leave(player));return;}
        clear(player.getUUID());for(View view:views.values()){Marker marker=view.markers.remove(player.getUUID());if(marker!=null&&ready(view.player))view.player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(marker.id));}
    }
    void stop(){for(UUID id:new HashSet<>(views.keySet()))clear(id);}
}
