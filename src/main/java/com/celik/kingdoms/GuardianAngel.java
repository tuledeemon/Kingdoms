package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import java.util.*;

/** CustomGear-style per-player web masking. The world is never modified or synchronously loaded. */
public final class GuardianAngel {
    private final KingdomService service;
    private static final int RADIUS=5;
    private static final BlockState MASK=mask();
    private final Map<UUID,Session> sessions=new HashMap<>();
    private boolean sending;
    private static final class Session {
        final ServerPlayer player;final ServerLevel level;final long until;
        Set<BlockPos> hidden=new HashSet<>();
        Session(ServerPlayer p,long until){player=p;level=p.level();this.until=until;}
    }
    GuardianAngel(KingdomService service){this.service=service;}
    private static BlockState mask(){BlockState s=Blocks.GLOW_LICHEN.defaultBlockState();for(Direction d:Direction.values())s=s.setValue(MultifaceBlock.getFaceProperty(d),true);return s;}
    public boolean active(ServerPlayer p){Session s=sessions.get(p.getUUID());return s!=null&&s.player==p&&s.level==p.level()&&p.isAlive()&&service.now()<s.until;}
    public void start(ServerPlayer p,long duration){clear(p);sessions.put(p.getUUID(),new Session(p,service.now()+duration));((GuardianMovement)p).kingdoms$clearWebSlowdown();sync(p,true);p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,p.getX(),p.getY()+1,p.getZ(),16,.4,.6,.4,.02);p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.CONDUIT_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,.7f,1.4f);}
    private BlockState state(ServerLevel l,BlockPos p){var chunk=l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);return chunk==null?null:chunk.getBlockState(p);}
    private void send(ServerPlayer p,BlockPos pos,BlockState state){if(state==null||p.connection==null||p.connection.player!=p)return;boolean previous=sending;sending=true;try{p.connection.send(new ClientboundBlockUpdatePacket(pos,state));}finally{sending=previous;}}
    private void sync(ServerPlayer p,boolean resend){Session s=sessions.get(p.getUUID());if(s==null)return;
        if(!active(p)){clear(p);return;}
        Set<BlockPos> desired=new HashSet<>();BlockPos center=p.blockPosition();
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-RADIUS,-2,-RADIUS),center.offset(RADIUS,3,RADIUS))){BlockState real=state(s.level,pos);if(real!=null&&real.is(Blocks.COBWEB))desired.add(pos.immutable());}
        for(BlockPos old:s.hidden)if(!desired.contains(old))send(p,old,state(s.level,old));
        for(BlockPos pos:desired)if(resend||!s.hidden.contains(pos))send(p,pos,MASK);
        s.hidden=desired;
    }
    public void afterPacket(ServerPlayer p,Packet<?> packet){if(sending||!service.server.isSameThread()||!active(p))return;
        Session s=sessions.get(p.getUUID());
        if(packet instanceof ClientboundBlockUpdatePacket update){BlockPos pos=update.getPos();if(s.hidden.contains(pos)&&update.getBlockState().is(Blocks.COBWEB))send(p,pos,MASK);}
        else if(packet instanceof ClientboundSectionBlocksUpdatePacket update)update.runUpdates((pos,state)->{if(s.hidden.contains(pos)&&state.is(Blocks.COBWEB))send(p,pos,MASK);});
        else if(packet instanceof ClientboundLevelChunkWithLightPacket chunk){for(BlockPos pos:s.hidden)if((pos.getX()>>4)==chunk.getX()&&(pos.getZ()>>4)==chunk.getZ())send(p,pos,MASK);}
    }
    public void tick(){for(Session s:List.copyOf(sessions.values()))sync(s.player,false);}
    public void clear(ServerPlayer p){Session s=sessions.remove(p.getUUID());if(s!=null&&s.player==p&&s.level==p.level())for(BlockPos pos:s.hidden)send(p,pos,state(s.level,pos));}
    public void stop(){for(Session s:List.copyOf(sessions.values()))clear(s.player);}
    boolean masks(ServerPlayer p,BlockPos pos){Session s=sessions.get(p.getUUID());return active(p)&&s.hidden.contains(pos);}
}
