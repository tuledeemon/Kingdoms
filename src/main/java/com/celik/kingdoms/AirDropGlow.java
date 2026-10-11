package com.celik.kingdoms;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.*;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

/** Ephemeral glow and client-only team colors; real scoreboard memberships are never changed. */
public final class AirDropGlow {
    private final AirDrops drops;
    private final Map<UUID,ServerPlayer> capture=new HashMap<>(),looting=new HashMap<>(),active=new HashMap<>();
    private record Viewer(ServerPlayer player,Set<UUID> shown){}
    private final Map<UUID,Viewer> viewers=new HashMap<>();
    AirDropGlow(AirDrops drops){this.drops=drops;}
    public boolean glowing(Entity entity){if(!drops.config().captureGlow||!(entity instanceof ServerPlayer player)||active.get(player.getUUID())!=player)return false;boolean collecting=looting.containsKey(player.getUUID())&&player.containerMenu instanceof AirDropChestMenu menu&&drops.chests.canUse(player,AirDrops.find(drops.data,menu.drop));return collecting||capture.containsKey(player.getUUID())&&drops.data.drops.stream().anyMatch(d->d.phase==Phase.CAPTURABLE&&drops.service.now()<d.expiresAt&&drops.eligible(player,d));}
    int rgb(ServerPlayer player){var k=drops.service.own(player.getUUID());return k==null?0:k.highlightColor;}
    static TeamColor nearest(int rgb){TeamColor result=TeamColor.BLACK;long best=Long.MAX_VALUE;for(TeamColor color:TeamColor.values()){int c=color.rgb(),r=(rgb>>16&255)-(c>>16&255),g=(rgb>>8&255)-(c>>8&255),b=(rgb&255)-(c&255);long distance=r*r+g*g+b*b;if(distance<best){best=distance;result=color;}}return result;}
    TeamColor color(ServerPlayer player){return nearest(rgb(player));}
    private String name(UUID id){return "kdg_"+id.toString().replace("-","").substring(0,12);}
    private PlayerTeam team(ServerPlayer player){PlayerTeam original=drops.service.server.getScoreboard().getPlayersTeam(player.getScoreboardName());PlayerTeam clone=new PlayerTeam(new Scoreboard(),name(player.getUUID()));if(original!=null){clone.setDisplayName(original.getDisplayName());clone.setPlayerPrefix(original.getPlayerPrefix());clone.setPlayerSuffix(original.getPlayerSuffix());clone.setNameTagVisibility(original.getNameTagVisibility());clone.setDeathMessageVisibility(original.getDeathMessageVisibility());clone.setCollisionRule(original.getCollisionRule());clone.unpackOptions(original.packOptions());}else clone.setDisplayName(Ui.text("AirDrop glow"));clone.setColor(Optional.of(color(player)));clone.getPlayers().add(player.getScoreboardName());return clone;}
    private void metadata(ServerPlayer player){var flags=((AirDropGlowEntity)player).kingdoms$refreshAirDropGlow();if(player.connection!=null)player.connection.send(new ClientboundSetEntityDataPacket(player.getId(),List.of(flags)));}
    void refresh(long now,List<ServerPlayer> testPlayers){capture.clear();if(drops.config().captureGlow)for(Drop d:drops.data.drops)if(d.phase==Phase.CAPTURABLE&&now<d.expiresAt)for(ServerPlayer player:testPlayers==null?drops.nearby(d):testPlayers)if(drops.eligible(player,d))capture.put(player.getUUID(),player);looting.entrySet().removeIf(e->!drops.config().captureGlow||!(e.getValue().containerMenu instanceof AirDropChestMenu menu)||!drops.chests.canUse(e.getValue(),AirDrops.find(drops.data,menu.drop)));sync(testPlayers==null?drops.service.server.getPlayerList().getPlayers():testPlayers);}
    void looting(ServerPlayer player,boolean open){if(open&&drops.config().captureGlow)looting.put(player.getUUID(),player);else looting.remove(player.getUUID());sync(drops.service.server.getPlayerList().getPlayers());}
    private void restore(Viewer viewer,UUID id,ServerPlayer former){PlayerTeam dummy=new PlayerTeam(new Scoreboard(),name(id));viewer.player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummy));if(former!=null){PlayerTeam original=drops.service.server.getScoreboard().getPlayersTeam(former.getScoreboardName());if(original!=null)viewer.player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(original,former.getScoreboardName(),ClientboundSetPlayerTeamPacket.Action.ADD));}viewer.shown.remove(id);}
    private void sync(Collection<ServerPlayer> observers){Map<UUID,ServerPlayer> next=new HashMap<>(capture);next.putAll(looting);Map<UUID,ServerPlayer> former=new HashMap<>(active);active.clear();active.putAll(next);for(var entry:former.entrySet())if(next.get(entry.getKey())!=entry.getValue())metadata(entry.getValue());for(var entry:next.entrySet())if(former.get(entry.getKey())!=entry.getValue())metadata(entry.getValue());
        Map<UUID,ServerPlayer> present=new HashMap<>();for(ServerPlayer observer:observers)if(observer.connection!=null)present.put(observer.getUUID(),observer);for(ServerPlayer observer:next.values())if(observer.connection!=null)present.put(observer.getUUID(),observer);
        for(var entry:present.entrySet()){Viewer previous=viewers.get(entry.getKey());if(previous==null||previous.player!=entry.getValue()){previous=new Viewer(entry.getValue(),new HashSet<>());viewers.put(entry.getKey(),previous);}Viewer viewer=previous;for(UUID id:new HashSet<>(viewer.shown))if(!next.containsKey(id))restore(viewer,id,former.get(id));for(ServerPlayer player:next.values()){PlayerTeam clone=team(player);boolean create=viewer.shown.add(player.getUUID());viewer.player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(clone,create));if(!create)viewer.player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(clone,player.getScoreboardName(),ClientboundSetPlayerTeamPacket.Action.ADD));}}
        for(var entry:new ArrayList<>(viewers.entrySet()))if(!present.containsKey(entry.getKey())){Viewer viewer=entry.getValue();if(viewer.player.connection!=null)for(UUID id:new HashSet<>(viewer.shown))restore(viewer,id,former.get(id));viewers.remove(entry.getKey());}
    }
    void leave(UUID id){capture.remove(id);looting.remove(id);sync(drops.service.server.getPlayerList().getPlayers());viewers.remove(id);}
    void clear(){capture.clear();looting.clear();sync(drops.service.server.getPlayerList().getPlayers());}
}
