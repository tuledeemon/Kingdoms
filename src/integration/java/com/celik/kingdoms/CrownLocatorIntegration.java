package com.celik.kingdoms;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.*;
import java.util.*;
import java.util.function.BiConsumer;

final class CrownLocatorIntegration {
    static void run(KingdomService s,ServerPlayer player,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        var level=player.level();boolean rule=level.getGameRules().get(GameRules.LOCATOR_BAR);var oldHandler=player.connection;ItemStack head=player.getItemBySlot(EquipmentSlot.HEAD);var position=player.position();var mode=player.gameMode.getGameModeForPlayer();
        ServerPlayer target=create(s,"CrownTarget",player),observer=create(s,"CrownObserver",player);target.setPos(player.getX()+20,player.getY(),player.getZ());observer.setPos(player.getX()+30,player.getY(),player.getZ());
        var captured=new RecordingConnection();new ServerGamePacketListenerImpl(s.server,captured,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));
        boolean registered=s.server.getPlayerList().getPlayers().contains(player);if(!registered)s.server.getPlayerList().getPlayers().add(player);
        s.server.getPlayerList().getPlayers().add(target);s.server.getPlayerList().getPlayers().add(observer);
        var client=new ClientView();var observerClient=new RecordingConnection();new ServerGamePacketListenerImpl(s.server,observerClient,observer,CommonListenerCookie.createInitial(observer.getGameProfile(),false));
        ItemStack crown=s.artifacts.create(ArtifactIntegration.issue(s.artifacts,cfg,"golden_crown"));
        try{
            level.getGameRules().set(GameRules.LOCATOR_BAR,false,s.server);captured.packets.clear();observerClient.packets.clear();
            player.setItemSlot(EquipmentSlot.HEAD,crown);s.artifacts.armor.tick(player);s.artifacts.locator.refresh();apply(s,captured,client);
            UUID marker=CrownLocator.markerId(target.getUUID());
            check.accept(client.waypoints.containsKey(marker)&&!marker.equals(target.getUUID()),"crown grants private native waypoint packets while global locator rule stays off");
            check.accept(!level.getGameRules().get(GameRules.LOCATOR_BAR)&&observerClient.waypoints()==0,"equipping a crown does not enable the bar for a non-wearer or change the gamerule");
            check.accept(!player.hasEffect(MobEffects.STRENGTH)&&crown.getOrDefault(net.minecraft.core.component.DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().stream().anyMatch(t->t.getString().contains(Ui.caps("locator bar"))),"crown grants no Strength and its refreshed lore describes the locator bar");
            target.setPos(player.getX()+600,player.getY(),player.getZ());s.artifacts.locator.refresh();apply(s,captured,client);
            check.accept(client.waypoints.get(marker).getClass().getSimpleName().contains("Azimuth"),"distant players use native direction-only waypoints");
            target.setInvisible(true);s.artifacts.locator.refresh();apply(s,captured,client);check.accept(!client.waypoints.containsKey(marker),"invisible targets are removed from crown waypoints");
            target.setInvisible(false);target.setShiftKeyDown(true);s.artifacts.locator.refresh();apply(s,captured,client);check.accept(!client.waypoints.containsKey(marker),"sneaking retains vanilla locator hiding");
            target.setShiftKeyDown(false);s.artifacts.locator.refresh();apply(s,captured,client);check.accept(client.waypoints.containsKey(marker),"visible targets return without re-equipping");
            target.waypointIcon().color=Optional.of(0x123456);s.artifacts.locator.refresh();apply(s,captured,client);
            check.accept((client.waypoints.get(marker).icon().color.orElseThrow()&0xFFFFFF)==0x123456,"native waypoint color changes update existing crown markers");
            try{
                var waiting=ServerGamePacketListenerImpl.class.getDeclaredField("waitingForSwitchToConfig");waiting.setAccessible(true);waiting.setBoolean(player.connection,true);s.artifacts.locator.refresh();
                check.accept(captured.waypoints()==0,"no play waypoint packets are sent during protocol reconfiguration");
                waiting.setBoolean(player.connection,false);client.waypoints.clear();s.artifacts.locator.refresh();apply(s,captured,client);
                check.accept(client.waypoints.containsKey(marker),"returning to play mode rebuilds the crown's private waypoints");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,900,1));s.artifacts.armor.tick(player);
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);s.artifacts.locator.refresh();apply(s,captured,client);
            check.accept(client.waypoints.isEmpty()&&player.getEffect(MobEffects.STRENGTH).getDuration()==900,"unequip clears only crown markers and preserves legitimate Strength potions");
            player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.GOLDEN_HELMET));s.artifacts.locator.refresh();apply(s,captured,client);check.accept(client.waypoints.isEmpty(),"ordinary gold helmets do not grant crown waypoints");
            player.setItemSlot(EquipmentSlot.HEAD,crown);s.artifacts.locator.refresh();apply(s,captured,client);
            cfg.types.get("golden_crown").enabled=false;s.artifacts.locator.refresh();apply(s,captured,client);check.accept(client.waypoints.isEmpty(),"disabling the crown removes existing private markers");cfg.types.get("golden_crown").enabled=true;
            s.artifacts.locator.refresh();apply(s,captured,client);s.artifacts.locator.leave(target);apply(s,captured,client);check.accept(!client.waypoints.containsKey(marker),"target logout removes its marker from current wearers immediately");
            s.artifacts.locator.refresh();apply(s,captured,client);player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);s.artifacts.locator.refresh();apply(s,captured,client);check.accept(client.waypoints.isEmpty(),"spectators cannot activate a worn crown");player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            s.artifacts.locator.refresh();apply(s,captured,client);level.getGameRules().set(GameRules.LOCATOR_BAR,true,s.server);s.artifacts.locator.refresh();apply(s,captured,client);check.accept(client.waypoints.isEmpty()&&level.getGameRules().get(GameRules.LOCATOR_BAR),"global-on mode removes private duplicates and leaves normal locator behavior intact");
            level.getGameRules().set(GameRules.LOCATOR_BAR,false,s.server);s.artifacts.locator.refresh();apply(s,captured,client);s.artifacts.locator.stop();apply(s,captured,client);check.accept(client.waypoints.isEmpty()&&!level.getGameRules().get(GameRules.LOCATOR_BAR),"shutdown clears private markers without changing the world's locator rule");
        }finally{
            s.artifacts.locator.stop();cfg.types.get("golden_crown").enabled=true;player.setItemSlot(EquipmentSlot.HEAD,head);player.setPos(position);player.gameMode.changeGameModeForPlayer(mode);player.connection=oldHandler;level.getGameRules().set(GameRules.LOCATOR_BAR,rule,s.server);
            s.server.getPlayerList().getPlayers().remove(target);s.server.getPlayerList().getPlayers().remove(observer);if(!registered)s.server.getPlayerList().getPlayers().remove(player);
        }
    }
    private static ServerPlayer create(KingdomService s,String name,ServerPlayer reference){var p=new ServerPlayer(s.server,reference.level(),new GameProfile(UUID.randomUUID(),name),ClientInformation.createDefault());p.setHealth(20);new ServerGamePacketListenerImpl(s.server,new RecordingConnection(),p,CommonListenerCookie.createInitial(p.getGameProfile(),false));return p;}
    private static void apply(KingdomService s,RecordingConnection packets,ClientView client){
        for(Packet<?> p:packets.packets)if(p instanceof ClientboundTrackedWaypointPacket waypoint){var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),s.server.registryAccess());try{ClientboundTrackedWaypointPacket.STREAM_CODEC.encode(buffer,waypoint);ClientboundTrackedWaypointPacket.STREAM_CODEC.decode(buffer).apply(client);}finally{buffer.release();}}
        packets.packets.clear();
    }
    private static final class ClientView implements TrackedWaypointManager {
        final Map<UUID,TrackedWaypoint> waypoints=new HashMap<>();
        public void trackWaypoint(TrackedWaypoint waypoint){waypoints.put(waypoint.id().left().orElseThrow(),waypoint);}
        public void updateWaypoint(TrackedWaypoint waypoint){UUID id=waypoint.id().left().orElseThrow();if(!waypoints.containsKey(id))throw new AssertionError("Updated a waypoint before tracking");waypoints.get(id).update(waypoint);}
        public void untrackWaypoint(TrackedWaypoint waypoint){waypoints.remove(waypoint.id().left().orElseThrow());}
    }
    private static final class RecordingConnection extends net.minecraft.network.Connection {
        final List<Packet<?>> packets=new ArrayList<>();RecordingConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnected(){return true;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        int waypoints(){return (int)packets.stream().filter(p->p instanceof ClientboundTrackedWaypointPacket).count();}
    }
    private CrownLocatorIntegration(){}
}
