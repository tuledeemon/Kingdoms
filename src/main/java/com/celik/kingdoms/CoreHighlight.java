package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static com.celik.kingdoms.Rules.*;

/** Client-visible wireframes only: never inserted into the world, saved, collided with or lootable. */
public final class CoreHighlight {
    private final KingdomService service;
    private final Map<UUID,Frame> frames=new HashMap<>();
    private int ticks;
    public CoreHighlight(KingdomService service){this.service=service;}
    record Edge(Vec3 start,Vec3 end) {}
    static List<Edge> edges(BlockPos pos){
        List<Edge> result=new ArrayList<>(12);
        double lo=-.015,hi=1.015;
        for(int axis=0;axis<3;axis++)for(int a=0;a<2;a++)for(int b=0;b<2;b++){
            double[] start={0,0,0},end={0,0,0};
            start[axis]=lo;end[axis]=hi;
            start[(axis+1)%3]=end[(axis+1)%3]=a==0?lo:hi;
            start[(axis+2)%3]=end[(axis+2)%3]=b==0?lo:hi;
            result.add(new Edge(new Vec3(pos.getX()+start[0],pos.getY()+start[1],pos.getZ()+start[2]),new Vec3(pos.getX()+end[0],pos.getY()+end[1],pos.getZ()+end[2])));
        }
        return result;
    }
    static List<Vec3> dustPoints(BlockPos pos){
        Set<Vec3> points=new LinkedHashSet<>();
        for(Edge edge:edges(pos))for(int step=0;step<=8;step++)points.add(step==0?edge.start:step==8?edge.end:edge.start.lerp(edge.end,step/8.0));
        return List.copyOf(points);
    }
    static final class Frame {
        final ServerLevel level;
        final BlockPos pos;
        final int color,thickness;
        final boolean worldwide;
        final List<Display.BlockDisplay> rods=new ArrayList<>(12);
        final Map<UUID,ServerPlayer> viewers=new HashMap<>();
        final List<Vec3> points;
        Frame(ServerLevel level,BlockPos pos,int color,int thickness){
            this(level,pos,color,thickness,false);
        }
        Frame(ServerLevel level,BlockPos pos,int color,int thickness,boolean worldwide){
            this.level=level;this.pos=pos;this.color=color;this.thickness=thickness;this.worldwide=worldwide;points=dustPoints(pos);
            float width=thickness/1000f;
            for(Edge edge:edges(pos)){
                Vec3 length=edge.end.subtract(edge.start);
                CompoundTag data=new CompoundTag(),block=new CompoundTag(),transform=new CompoundTag(),brightness=new CompoundTag();
                block.putString("Name","minecraft:white_concrete");data.put("block_state",block);
                transform.put("translation",floats(0,0,0));transform.put("left_rotation",floats(0,0,0,1));transform.put("right_rotation",floats(0,0,0,1));
                transform.put("scale",floats(length.x==0?width:(float)length.x+width,length.y==0?width:(float)length.y+width,length.z==0?width:(float)length.z+width));
                data.put("transformation",transform);brightness.putInt("block",15);brightness.putInt("sky",15);data.put("brightness",brightness);
                data.putBoolean("Glowing",true);data.putInt("glow_color_override",color);data.putFloat("view_range",worldwide?Float.MAX_VALUE:2f);
                data.putFloat("shadow_radius",0);data.putFloat("shadow_strength",0);data.putBoolean("NoGravity",true);
                Display.BlockDisplay rod=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level);
                rod.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
                rod.setPos(edge.start.x-width/2,edge.start.y-width/2,edge.start.z-width/2);
                rods.add(rod);
            }
        }
        private static ListTag floats(float... values){ListTag list=new ListTag();for(float value:values)list.add(FloatTag.valueOf(value));return list;}
        List<Packet<?>> spawnPackets(){
            List<Packet<?>> packets=new ArrayList<>(24);
            for(var rod:rods){
                packets.add(new ClientboundAddEntityPacket(rod.getId(),rod.getUUID(),rod.getX(),rod.getY(),rod.getZ(),0,0,EntityTypes.BLOCK_DISPLAY,0,Vec3.ZERO,0));
                packets.add(new ClientboundSetEntityDataPacket(rod.getId(),Objects.requireNonNull(rod.getEntityData().getNonDefaultValues())));
            }
            return packets;
        }
        void show(ServerPlayer player){for(Packet<?> packet:spawnPackets())player.connection.send(packet);viewers.put(player.getUUID(),player);}
        void hide(ServerPlayer player){if(player.connection!=null)player.connection.send(new ClientboundRemoveEntitiesPacket(rods.stream().mapToInt(Entity::getId).toArray()));viewers.remove(player.getUUID());}
        void clear(){for(var player:new ArrayList<>(viewers.values()))hide(player);}
    }
    public void tick(){
        ticks++;
        if(ticks%5==0)refresh();
        if(service.config.coreParticles&&ticks%service.config.coreParticleIntervalTicks==0)particles();
    }
    public void refresh(){
        Set<UUID> active=new HashSet<>();
        if(service.config.coreGlow)for(Kingdom kingdom:service.state.kingdoms.values()){
            Core core=kingdom.core;if(core==null||core.carried)continue;
            boolean wartime=wartime(service.state,kingdom,service.now());
            if(service.config.coreGlowOnlyDuringWar&&!wartime)continue;
            ServerLevel level=level(core.dimension);BlockPos pos=KingdomCores.position(core);
            if(level==null)continue;
            List<ServerPlayer> players=wartime?service.server.getPlayerList().getPlayers().stream().filter(p->p.connection!=null&&p.level()==level).toList():nearby(level,pos);if(players.isEmpty())continue;
            int thickness=wartime?service.config.coreWarOutlineThicknessMilli:service.config.coreOutlineThicknessMilli;
            active.add(kingdom.id);Frame frame=frames.get(kingdom.id);
            if(frame==null||frame.level!=level||!frame.pos.equals(pos)||frame.color!=kingdom.highlightColor||frame.thickness!=thickness||frame.worldwide!=wartime){
                if(frame!=null)frame.clear();frame=new Frame(level,pos,kingdom.highlightColor,thickness,wartime);frames.put(kingdom.id,frame);
            }
            Set<UUID> present=new HashSet<>();
            for(ServerPlayer player:players){present.add(player.getUUID());ServerPlayer previous=frame.viewers.get(player.getUUID());if(previous!=player){if(previous!=null)frame.hide(previous);frame.show(player);}}
            for(ServerPlayer player:new ArrayList<>(frame.viewers.values()))if(!present.contains(player.getUUID()))frame.hide(player);
        }
        for(UUID id:new ArrayList<>(frames.keySet()))if(!active.contains(id))frames.remove(id).clear();
    }
    static boolean wartime(State state,Kingdom kingdom,long now){
        if(kingdom.core==null||kingdom.core.carried)return false;
        UUID host=kingdom.core.tributeTo==null?kingdom.id:kingdom.core.tributeTo;
        return Rules.war(state,host,now)!=null;
    }
    private void particles(){
        for(Kingdom kingdom:service.state.kingdoms.values()){
            Core core=kingdom.core;if(core==null||core.carried)continue;
            ServerLevel level=level(core.dimension);BlockPos pos=KingdomCores.position(core);if(level==null||!level.hasChunkAt(pos))continue;
            List<ServerPlayer> players=nearby(level,pos);if(players.isEmpty())continue;
            DustParticleOptions dust=new DustParticleOptions(kingdom.highlightColor,service.config.coreDustSizePercent/100f);
            for(Vec3 point:dustPoints(pos))for(ServerPlayer player:players)level.sendParticles(player,dust,true,false,point.x,point.y,point.z,0,0,0,0,0);
        }
    }
    private List<ServerPlayer> nearby(ServerLevel level,BlockPos pos){double range=service.config.coreHighlightRange;return service.server.getPlayerList().getPlayers().stream().filter(p->p.connection!=null&&p.level()==level&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=range*range).toList();}
    private ServerLevel level(String dimension){for(ServerLevel level:service.server.getAllLevels())if(KingdomService.dimension(level).equals(dimension))return level;return null;}
    public void leave(UUID player){for(Frame frame:frames.values())frame.viewers.remove(player);}
    public void reset(ServerPlayer player){for(Frame frame:frames.values())if(frame.viewers.containsKey(player.getUUID()))frame.hide(player);}
    public void clear(){frames.values().forEach(Frame::clear);frames.clear();}
}
