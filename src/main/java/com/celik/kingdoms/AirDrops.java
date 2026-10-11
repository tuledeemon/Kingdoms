package com.celik.kingdoms;

import com.celik.kingdoms.mixin.AirDropPlayerSaveInvoker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

public final class AirDrops {
    private static final TicketType TICKET=new TicketType(1200,TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION);
    final KingdomService service;
    Data data;
    private final Random random=new Random();
    private final Map<UUID,AirDropVisuals> visuals=new HashMap<>();
    public final AirDropChests chests=new AirDropChests(this);
    public final AirDropGlow glows=new AirDropGlow(this);
    private record TicketSite(ServerLevel level,ChunkPos chunk){}
    private final Map<UUID,TicketSite> tickets=new HashMap<>();
    private final Map<UUID,Map<UUID,ServerBossEvent>> bars=new HashMap<>();
    private boolean healthy=true;
    private long lastPoll,lastCheckpoint;
    private int ticks;
    private SpawnSearch search;
    private Data indexedData;
    private List<Drop> protectedZones=List.of();
    private static final class SpawnSearch {int wanted,attempts;final List<BlockPos> positions=new ArrayList<>();}
    AirDrops(KingdomService service){
        this.service=service;data=service.store.read("airdrops.json",Data.class,new Data());AirDropRules.validate(data);data.drops.forEach(AirDropRules::normalizeCapture);validateConfiguration(config());
        long now=service.now();lastPoll=now;lastCheckpoint=now;for(Drop d:data.drops){d.takeoverSide=null;d.takeoverSince=0;}
        for(Drop d:data.drops)if(d.active())for(Progress p:d.progress.values())if(p.millis>0&&p.missingSince==0)p.missingSince=d.checkedAt>0?d.checkedAt:now;
        if(config().enabled&&data.ageEpoch==0){data.ageEpoch=now;data.nextEvent=now+config().intervalMinutes*60_000L;save(data);}
    }
    AirDropConfig config(){return service.config.airDrops;}
    public void validateConfiguration(AirDropConfig cfg){AirDropLoot.validate(service.server,cfg);Identifier dim=Identifier.tryParse(cfg.dimension),block=Identifier.tryParse(cfg.crateBlock);if(dim==null||service.server.getLevel(ResourceKey.create(Registries.DIMENSION,dim))==null||block==null||BuiltInRegistries.BLOCK.getValue(block)==Blocks.AIR)throw new IllegalArgumentException("Invalid AirDrop dimension/crate block");for(ParachuteModel.Part part:ParachuteModel.parts()){String name=ParachuteModel.block(part,cfg);Identifier id=Identifier.tryParse(name);if(id==null||BuiltInRegistries.BLOCK.get(id).isEmpty()||BuiltInRegistries.BLOCK.getValue(id).defaultBlockState().isAir())throw new IllegalArgumentException("Invalid parachute block: "+name);}}
    private Data copy(){return Store.JSON.fromJson(Store.JSON.toJson(data),Data.class);}
    private List<Drop> zones(){if(indexedData!=data){protectedZones=data.drops.stream().filter(Drop::active).toList();indexedData=data;}return protectedZones;}
    /** Closed rewards are immutable here; only active capture records need a per-poll copy. */
    private Data captureCopy(){Data next=new Data();next.ageEpoch=data.ageEpoch;next.nextEvent=data.nextEvent;next.drops=new ArrayList<>();for(Drop d:data.drops)next.drops.add((d.active()||d.phase==Phase.CLAIMED)?Store.JSON.fromJson(Store.JSON.toJson(d),Drop.class):d);return next;}
    private void checkHealthy(){if(!healthy)throw new IllegalStateException("AirDrop storage needs administrator recovery; events and rewards are locked.");}
    private void save(Data next){checkHealthy();AirDropRules.validate(next);try{service.store.write("airdrops.json",next);data=next;lastCheckpoint=service.now();}catch(RuntimeException e){healthy=false;throw e;}}
    public long day(){return data.ageEpoch==0?1:AirDropRules.day(data.ageEpoch,service.now());}
    ServerLevel level(String dimension){return service.server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(dimension)));}
    static net.minecraft.network.chat.MutableComponent text(String value){return Component.literal(Ui.caps(value));}
    private void announce(String key,String fallback,String detail){service.announce(text("[airdrop] "+config().message(key,fallback).replace("{detail}",detail)).withStyle(ChatFormatting.GOLD));}
    private void sound(ServerPlayer player,String sound,float pitch){if(!config().sounds||player.connection==null)return;BuiltInRegistries.SOUND_EVENT.get(Identifier.parse(sound)).ifPresent(holder->player.connection.send(new ClientboundSoundPacket(holder,SoundSource.UI,player.getX(),player.getY(),player.getZ(),.8f,pitch,player.getRandom().nextLong())));}
    private void title(ServerPlayer p,String heading,String subtitle){if(!config().titles||p.connection==null)return;p.connection.send(new ClientboundSetTitlesAnimationPacket(5,50,10));p.connection.send(new ClientboundSetTitleTextPacket(text(heading).withStyle(ChatFormatting.GOLD)));p.connection.send(new ClientboundSetSubtitleTextPacket(text(subtitle).withStyle(ChatFormatting.YELLOW)));}
    /** A configurable horizontal build zone protects active drops; zero reserves only landing/support. */
    public boolean protectedAt(Level level,BlockPos pos){String dim=KingdomService.dimension(level);return zones().stream().anyMatch(d->d.dimension.equals(dim)&&d.protectsBlock(pos.getX(),pos.getY(),pos.getZ()));}
    /** Claims still cannot absorb a live capture objective. This does not lock terrain edits. */
    public boolean protectsChunk(Level level,BlockPos pos){int minX=(pos.getX()>>4)*16,minZ=(pos.getZ()>>4)*16;String dim=KingdomService.dimension(level);for(Drop d:zones())if(d.dimension.equals(dim)){double x=Mth.clamp(d.x+.5,minX,minX+16),z=Mth.clamp(d.z+.5,minZ,minZ+16);double dx=x-(d.x+.5),dz=z-(d.z+.5),radius=Math.max(d.captureRadius,d.protectionRadius);if(dx*dx+dz*dz<=radius*radius)return true;}return false;}
    private boolean claimsOrExclusions(ServerLevel level,int x,int z){int radius=Math.max(config().captureRadius,config().protectionRadius)+config().claimBufferBlocks;for(int cx=(x-radius)>>4;cx<=((x+radius)>>4);cx++)for(int cz=(z-radius)>>4;cz<=((z+radius)>>4);cz++)if(service.land(level,new BlockPos(cx*16,64,cz*16))!=null)return true;for(var e:config().excludedRegions)if(e.dimension.equals(KingdomService.dimension(level))&&x+radius>=e.minX&&x-radius<=e.maxX&&z+radius>=e.minZ&&z-radius<=e.maxZ)return true;return false;}
    /** One usable surface column is enough. Hills, canopy and surrounding obstacles are accepted. */
    BlockPos landingAt(ServerLevel level,int x,int z){
        int radius=Math.max(config().captureRadius,config().protectionRadius);var border=level.getWorldBorder();if(x-radius<border.getMinX()||x+radius+1>border.getMaxX()||z-radius<border.getMinZ()||z+radius+1>border.getMaxZ()||claimsOrExclusions(level,x,z))return null;
        BlockPos spawn=level.getRespawnData().pos();double spawnDistance=Math.hypot(x-spawn.getX(),z-spawn.getZ());if(spawnDistance<config().minSpawnDistance)return null;
        if(KingdomService.dimension(level).equals("minecraft:overworld")&&service.config.spawnClaimProtectionChunks>0){int chunks=service.config.spawnClaimProtectionChunks;int minX=((spawn.getX()>>4)-chunks)*16-radius,maxX=((spawn.getX()>>4)+chunks+1)*16+radius,minZ=((spawn.getZ()>>4)-chunks)*16-radius,maxZ=((spawn.getZ()>>4)+chunks+1)*16+radius;if(x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ)return null;}
        if(!config().allowChunkGeneration&&!level.hasChunkAt(new BlockPos(x,64,z)))return null;
        level.getChunkAt(new BlockPos(x,64,z));
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z);if(y<Math.max(config().minLandingY,level.getMinY()+1)||y>Math.min(config().maxLandingY,level.getMaxY()-2))return null;BlockPos pos=new BlockPos(x,y,z),ground=pos.below();
        if(level.getBiome(pos).unwrapKey().map(k->config().excludedBiomes.contains(k.identifier().toString())).orElse(false))return null;
        var support=level.getBlockState(ground);if(support.isAir()||!support.getFluidState().isEmpty()||support.getCollisionShape(level,ground).isEmpty()||support.is(Blocks.CACTUS)||support.is(Blocks.MAGMA_BLOCK)||support.is(Blocks.POWDER_SNOW)||level.getBlockEntity(ground)!=null)return null;
        if(!landingSpace(level,pos)||!landingSpace(level,pos.above()))return null;
        for(Drop d:data.drops)if(d.active()&&d.dimension.equals(KingdomService.dimension(level))&&Math.hypot(x-d.x,z-d.z)<config().minDropSeparation)return null;
        return pos;
    }
    static boolean landingSpace(Level level,BlockPos pos){var state=level.getBlockState(pos);return state.getFluidState().isEmpty()&&(state.isAir()||state.canBeReplaced())&&level.getBlockEntity(pos)==null;}
    public void requestStart(boolean force){checkHealthy();service.require(config().enabled,"AirDrops are disabled.");service.require(search==null,"An AirDrop spawn search is already running.");service.require(data.drops.stream().noneMatch(Drop::active),"Finish or cancel the current drops first.");int players=service.server.getPlayerList().getPlayerCount(),count=force?Math.max(1,config().dropsFor(players)):config().dropsFor(players);service.require(count>0,"Not enough online players for an AirDrop.");search=new SpawnSearch();search.wanted=Math.min(count,config().maxDrops);}
    private void searchTick(){
        if(search==null)return;ServerLevel level=level(config().dimension);
        for(int n=0;n<config().attemptsPerTick&&search!=null;n++){
            if(++search.attempts>config().candidateAttempts){KingdomsMod.LOG.warn("AirDrop search found {} of {} safe sites; no event announced",search.positions.size(),search.wanted);search=null;Data next=copy();next.nextEvent=service.now()+config().retrySeconds*1000L;save(next);return;}
            var origin=level.getRespawnData().pos();double angle=random.nextDouble()*Math.PI*2,radius=Math.sqrt(config().minSpawnDistance*(double)config().minSpawnDistance+random.nextDouble()*(config().maxSpawnDistance*(double)config().maxSpawnDistance-config().minSpawnDistance*(double)config().minSpawnDistance));int x=origin.getX()+(int)Math.round(Math.cos(angle)*radius),z=origin.getZ()+(int)Math.round(Math.sin(angle)*radius);
            BlockPos pos=landingAt(level,x,z);if(pos==null)continue;
            if(search.positions.stream().anyMatch(other->Math.hypot(other.getX()-x,other.getZ()-z)<config().minDropSeparation)||!search.positions.isEmpty()&&Math.hypot(search.positions.getFirst().getX()-x,search.positions.getFirst().getZ()-z)>config().maxDropSeparation)continue;
            search.positions.add(pos);if(search.positions.size()==search.wanted){List<BlockPos> positions=List.copyOf(search.positions);search=null;spawnAt(level,positions,service.now());}
        }
    }
    void spawnAt(ServerLevel level,List<BlockPos> positions,long now){spawnAt(level,positions,now,null,false);}
    private void spawnAt(ServerLevel level,List<BlockPos> positions,long now,String chosenVariant,boolean landed){
        checkHealthy();service.require(!positions.isEmpty()&&positions.size()+zones().size()<=config().maxDrops,"AirDrop count exceeds the configured active-drop cap.");Data next=copy();if(next.ageEpoch==0)next.ageEpoch=now;long age=AirDropRules.day(next.ageEpoch,now);var tier=config().tier(age);List<Drop> made=new ArrayList<>();AirDropConfig.Variant selected=chosenVariant==null||chosenVariant.equals("random")?null:config().variants.stream().filter(v->v.key.equals(chosenVariant)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown AirDrop variant."));
        for(BlockPos pos:positions){service.require(made.stream().noneMatch(d->Math.hypot(pos.getX()-d.x,pos.getZ()-d.z)<config().minDropSeparation),"AirDrops are too close together.");service.require(made.isEmpty()||Math.hypot(pos.getX()-made.getFirst().x,pos.getZ()-made.getFirst().z)<=config().maxDropSeparation,"AirDrops are too far apart.");BlockPos checked=landingAt(level,pos.getX(),pos.getZ());service.require(checked!=null&&checked.equals(pos),"AirDrop landing location is no longer safe.");Drop d=new Drop();d.settingsVersion=1;d.dimension=KingdomService.dimension(level);d.x=pos.getX();d.y=pos.getY();d.z=pos.getZ();d.createdAt=now;d.landsAt=now+config().descentSeconds*1000L;d.expiresAt=d.landsAt+config().captureExpiryMinutes*60_000L;d.startY=Math.min(level.getMaxY()-2,d.y+config().descentHeight);d.captureRadius=config().captureRadius;d.verticalRadius=config().captureVerticalRadius;d.protectionRadius=config().protectionRadius;d.requiredMembers=config().kingdomMembers;d.soloControlStrength=config().soloControlStrength;d.largerGroupTakeover=config().largerGroupTakeover;d.takeoverMillis=config().takeoverSeconds*1000L;d.scatterLoot=config().scatterLoot;d.captureMillis=config().captureSeconds*1000L;d.graceMillis=config().graceSeconds*1000L;d.soloMultiplier=config().soloMultiplier;d.interferenceMultiplier=config().interferenceMultiplier;d.tierName=tier.name;d.tierDay=tier.fromDay;var variant=selected==null?AirDropLoot.variant(config(),random):selected;d.variant=variant.key;d.variantName=variant.name;d.loot=service.artifacts.roll(d.id,age,random);boolean artifact=d.loot!=null;if(d.loot==null)d.loot=AirDropLoot.generate(service.server,config(),tier,variant,age,random);long cash=config().money(age);if(cash>0&&(!artifact||config().artifactDropsHaveMoney)){Reward reward=new Reward();reward.money=cash;reward.itemJson=AirDropLoot.encode(service.server,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));d.loot.add(reward);}next.drops.add(d);made.add(d);}
        for(Drop d:made){if(landed){d.createdAt=now-1;d.landsAt=now;d.startY=d.y;d.phase=Phase.CAPTURABLE;d.expiresAt=now+config().captureExpiryMinutes*60_000L;}}
        next.nextEvent=now+config().intervalMinutes*60_000L+random.nextLong(config().jitterMinutes*60_000L+1);save(next);
        for(Drop d:made){service.artifacts.announce(d);announce("start","Detected {detail}",d.variantName+" • "+coordinates(d)+" • lands in "+Ui.clock(d.landsAt-now));}
        for(ServerPlayer p:service.server.getPlayerList().getPlayers()){sound(p,config().startSound,1);title(p,"AirDrop detected",made.size()+" supply drop"+(made.size()==1?"":"s")+" • coordinates in chat");}
    }
    public void spawnTest(ServerLevel level,int x,int z,String variant,boolean landed){BlockPos pos=landingAt(level,x,z);service.require(pos!=null,"No usable landing spot here. Choose a dry surface outside claims, spawn protection and excluded regions. Hills and trees are allowed.");spawnAt(level,List.of(pos),service.now(),variant,landed);refreshVisuals(service.now());glows.refresh(service.now(),null);}
    public void landNow(){checkHealthy();Data next=copy();long now=service.now();int count=0;for(Drop d:next.drops)if(d.phase==Phase.DESCENDING){d.createdAt=Math.min(d.createdAt,now-1);d.landsAt=now;d.expiresAt=now+config().captureExpiryMinutes*60_000L;d.phase=Phase.CAPTURABLE;count++;}service.require(count>0,"No descending AirDrops to land.");save(next);refreshVisuals(now);glows.refresh(now,null);announce("landed","Landed! Control the area to secure {detail}",count+" supply drops");}
    String side(ServerPlayer p){var k=service.own(p.getUUID());return k==null?AirDropRules.solo(p.getUUID()):AirDropRules.kingdom(k.id);}
    int sideColor(String side){if(side==null)return 0xFFD34D;if(!side.startsWith("k:"))return 0;var kingdom=service.state.kingdoms.get(UUID.fromString(side.substring(2)));return kingdom==null?0:kingdom.highlightColor;}
    int visualColor(Drop drop){if(drop.winner!=null)return sideColor(drop.winner);if(drop.capturingSide!=null)return sideColor(drop.capturingSide);return drop.progress.entrySet().stream().max(Comparator.comparingDouble((Map.Entry<String,Progress> e)->e.getValue().millis).thenComparing(Map.Entry::getKey)).map(e->sideColor(e.getKey())).orElse(0xFFD34D);}
    String sideName(String side){if(side.startsWith("k:")){var k=service.state.kingdoms.get(UUID.fromString(side.substring(2)));return k==null?"Former kingdom":k.display();}UUID id=UUID.fromString(side.substring(2));var p=service.server.getPlayerList().getPlayer(id);return p==null?"Solo player":p.getPlainTextName();}
    boolean eligible(ServerPlayer p,Drop d){double dx=p.getX()-(d.x+.5),dz=p.getZ()-(d.z+.5);return p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&!p.getAbilities().flying&&(!config().requireGround||p.onGround())&&KingdomService.dimension(p.level()).equals(d.dimension)&&Math.abs(p.getY()-d.y)<=d.verticalRadius&&dx*dx+dz*dz<=d.captureRadius*(double)d.captureRadius;}
    List<ServerPlayer> nearby(Drop d){ServerLevel level=level(d.dimension);if(level==null)return List.of();return level.getEntitiesOfClass(ServerPlayer.class,new AABB(d.x-config().nearbyRadius,level.getMinY(),d.z-config().nearbyRadius,d.x+config().nearbyRadius+1,level.getMaxY(),d.z+config().nearbyRadius+1),p->!p.isSpectator());}
    public void tick(){ticks++;try{if(healthy&&ticks%20==0&&search!=null)searchTick();}catch(Exception e){search=null;KingdomsMod.LOG.error("AirDrop site search failed",e);}if(config().particles&&ticks%config().particleIntervalTicks==0){long time=service.now();for(Drop d:data.drops)if(d.active()||d.phase==Phase.CLAIMED&&time<d.expiresAt&&d.loot.stream().anyMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING)){ServerLevel world=level(d.dimension);if(world!=null&&world.hasChunkAt(AirDropChests.pos(d)))AirDropVisuals.fx(this,world,d,time);}}if(ticks%config().pollTicks!=0)return;long now=service.now();long elapsed=Math.min(Math.max(0,now-lastPoll),Math.max(1000,config().pollTicks*50L+1000));lastPoll=now;
        try {update(now,elapsed,null);if(config().enabled&&data.ageEpoch==0){Data next=copy();next.ageEpoch=now;next.nextEvent=now+config().intervalMinutes*60_000L;save(next);}if(healthy&&config().enabled&&search==null&&now>=data.nextEvent&&data.drops.stream().noneMatch(Drop::active)){if(config().dropsFor(service.server.getPlayerList().getPlayerCount())>0)requestStart(false);else{Data next=copy();next.nextEvent=now+config().retrySeconds*1000L;save(next);}}refreshVisuals(now);}catch(Exception e){KingdomsMod.LOG.error("AirDrop update failed; rewards remain locked when storage is unhealthy",e);}
    }
    /** The optional player list is used by the dedicated-server integration probe. */
    void update(long now,long elapsed,List<ServerPlayer> testPlayers){
        checkHealthy();Data next=captureCopy();boolean transition=false,dirty=false;List<Runnable> notices=new ArrayList<>();
        for(Drop d:next.drops){if(!d.active()&&d.phase!=Phase.CLAIMED)continue;Drop old=find(data,d.id);dirty=true;
            if(now>=d.expiresAt){d.phase=Phase.EXPIRED;d.closedAt=now;for(Reward r:d.loot)if(r.status==LootStatus.AVAILABLE)r.status=LootStatus.EXPIRED;transition=true;notices.add(()->announce("expired","Expired {detail}",coordinates(d)+" • remaining loot disappeared"));continue;}
            if(d.phase==Phase.CLAIMED)continue;
            if(d.phase==Phase.DESCENDING&&now>=d.landsAt){d.phase=Phase.CAPTURABLE;transition=true;notices.add(()->{announce("landed","Landed! Control the area to secure {detail}",coordinates(d));for(ServerPlayer p:nearby(d)){title(p,"AirDrop landed",d.variantName);sound(p,config().landingSound,.8f);}});}
            if(d.phase!=Phase.CAPTURABLE)continue;
            List<ServerPlayer> players=testPlayers==null?nearby(d):testPlayers;Map<String,Integer> presence=new LinkedHashMap<>();for(ServerPlayer p:players)if(eligible(p,d))presence.merge(side(p),1,Integer::sum);
            String winner=AirDropRules.advance(d,presence,old.phase==Phase.DESCENDING?0:elapsed,now);d.checkedAt=now;if(!Objects.equals(old.takeoverSide,d.takeoverSide)&&d.takeoverSide!=null){transition=true;notices.add(()->nearby(d).forEach(p->Ui.tell(p,"[airdrop] "+sideName(d.takeoverSide)+" is challenging control. Takeover in "+d.takeoverMillis/1000+" seconds; capture paused.",false)));}if(!Objects.equals(old.capturingSide,d.capturingSide)&&old.capturingSide!=null){String control=d.capturingSide==null?"Capture control reset.":sideName(d.capturingSide)+" took capture control. Progress starts from zero.";transition=true;notices.add(()->nearby(d).forEach(p->Ui.tell(p,"[airdrop] "+control,false)));}
            for(var entry:d.progress.entrySet()){String side=entry.getKey();Progress current=entry.getValue(),previous=old.progress.get(side);String notice=null;if(current.millis>0&&(previous==null||previous.millis==0))notice=sideName(side)+" is securing the AirDrop.";else if(current.missingSince!=0&&(previous==null||previous.missingSince==0))notice="Capture interrupted for "+sideName(side)+". Restore presence within "+d.graceMillis/1000+" seconds.";else if(current.contested&&(previous==null||!previous.contested))notice="Rival presence detected. Capture is slowed for "+sideName(side)+".";else if(previous!=null&&previous.millis>0&&current.millis==0)notice="Capture reset for "+sideName(side)+".";if(notice!=null){String message=notice;transition=true;notices.add(()->nearby(d).forEach(p->Ui.tell(p,"[airdrop] "+message,false)));}}
            if(winner!=null){d.phase=Phase.CLAIMED;d.closedAt=now;d.winner=winner;d.winnerName=sideName(winner);if(winner.startsWith("k:")){var k=service.state.kingdoms.get(UUID.fromString(winner.substring(2)));if(k!=null)d.recipients.addAll(k.members.keySet());}else d.recipients.add(UUID.fromString(winner.substring(2)));service.require(!d.recipients.isEmpty(),"Winning side no longer exists.");transition=true;notices.add(()->{announce("claimed","Secured by {detail}",d.winnerName+" • "+coordinates(d));for(ServerPlayer p:service.server.getPlayerList().getPlayers())if(d.recipients.contains(p.getUUID())){title(p,"AirDrop secured",d.variantName+" • open the chest");sound(p,config().captureSound,1);Ui.tell(p,"Your side won. Open the chest at "+coordinates(d)+" to collect the shared loot.",false);}});}
        }
        boolean removed=next.drops.removeIf(d->!d.active()&&d.closedAt>0&&now-d.closedAt>config().historyDays*86_400_000L&&d.loot.stream().noneMatch(r->r.status==LootStatus.PENDING)&&(d.phase!=Phase.CLAIMED||d.loot.stream().allMatch(r->r.status==LootStatus.COLLECTED)));transition|=removed;
        if(transition||dirty&&now-lastCheckpoint>=config().checkpointSeconds*1000L)save(next);else if(dirty)data=next;
        chests.sync(now);glows.refresh(now,testPlayers);notices.forEach(Runnable::run);
    }
    private void refreshVisuals(long now){chests.sync(now);Set<UUID> active=new HashSet<>(),descending=new HashSet<>();for(Drop d:data.drops)if(d.active()||d.phase==Phase.CLAIMED&&now<d.expiresAt&&d.loot.stream().anyMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING)){
        ServerLevel level=level(d.dimension);if(level==null)continue;active.add(d.id);tickets.computeIfAbsent(d.id,id->{ChunkPos chunk=new ChunkPos(d.x>>4,d.z>>4);level.getChunkSource().addTicketWithRadius(TICKET,chunk,1);return new TicketSite(level,chunk);});if(d.phase==Phase.DESCENDING){descending.add(d.id);AirDropVisuals v=visuals.get(d.id);if(v==null||v.removed()){if(v!=null)v.clear();v=new AirDropVisuals(level,d,config());visuals.put(d.id,v);}v.move(d,now);}if(d.active()&&config().bossBars)updateBars(d,nearby(d),now);else clearBars(d.id);
    }for(UUID id:new HashSet<>(visuals.keySet()))if(!descending.contains(id))visuals.remove(id).clear();for(UUID id:new HashSet<>(tickets.keySet()))if(!active.contains(id)){var site=tickets.remove(id);site.level.getChunkSource().removeTicketWithRadius(TICKET,site.chunk,1);clearBars(id);}}
    void updateBars(Drop d,List<ServerPlayer> players,long now){Map<UUID,ServerBossEvent> viewers=bars.computeIfAbsent(d.id,k->new HashMap<>());Set<UUID> nearby=new HashSet<>();for(ServerPlayer p:players){if(p.connection==null)continue;nearby.add(p.getUUID());ServerBossEvent bar=viewers.computeIfAbsent(p.getUUID(),id->{var b=new ServerBossEvent(UUID.randomUUID(),text("airdrop"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS);b.addPlayer(p);return b;});Progress progress=d.capturingSide==null?null:d.progress.get(d.capturingSide);String status;double fraction;if(d.phase==Phase.DESCENDING){status="arriving "+Ui.clock(d.landsAt-now);fraction=1-(d.landsAt-now)/(double)(d.landsAt-d.createdAt);}else if(progress==null){status="hold area • "+d.requiredMembers+" kingdom members or solo";fraction=0;}else{fraction=progress.millis/d.required(d.capturingSide);status=sideName(d.capturingSide)+" • "+Math.round(fraction*100)+"%"+(progress.missingSince!=0?" • paused "+Ui.clock(d.graceMillis-(now-progress.missingSince)):progress.contested?" • contested":" • capturing");}if(d.takeoverSide!=null)status=sideName(d.takeoverSide)+" • takeover "+Ui.clock(d.takeoverMillis-(now-d.takeoverSince))+" • capture paused";bar.setName(text(d.variantName+" • "+status));bar.setProgress((float)Math.max(0,Math.min(1,fraction)));bar.setColor(progress!=null&&progress.missingSince!=0?BossEvent.BossBarColor.RED:progress!=null&&progress.contested?BossEvent.BossBarColor.YELLOW:BossEvent.BossBarColor.GREEN);}for(UUID id:new HashSet<>(viewers.keySet()))if(!nearby.contains(id)){viewers.remove(id).removeAllPlayers();}}
    private void clearBars(UUID id){var viewers=bars.remove(id);if(viewers!=null)viewers.values().forEach(ServerBossEvent::removeAllPlayers);}
    public void leave(UUID player){chests.leave(player);glows.leave(player);bars.values().forEach(viewers->{var b=viewers.remove(player);if(b!=null)b.removeAllPlayers();});}
    public boolean use(ServerPlayer p,Entity entity){UUID id=entity instanceof AirDropVisuals.HitBox h?h.drop:entity instanceof AirDropVisuals.Crate c?c.drop:null;if(id==null)return false;Drop d=find(data,id);if(d==null){Ui.tell(p,"This AirDrop is no longer available.",true);return true;}if(d.phase==Phase.DESCENDING)Ui.tell(p,"AirDrop arriving in "+Ui.clock(d.landsAt-service.now())+". Loot is sealed.",false);else chests.use(p,AirDropChests.pos(d));return true;}
    static Drop find(Data data,UUID id){return data.drops.stream().filter(d->d.id.equals(id)).findFirst().orElse(null);}
    Drop drop(UUID id){Drop d=find(data,id);service.require(d!=null,"AirDrop not found.");return d;}
    public static String coordinates(Drop d){return "X "+d.x+" Y "+d.y+" Z "+d.z+" • "+d.dimension.replace("minecraft:","");}
    public void cancel(){checkHealthy();search=null;Data next=copy();for(Drop d:next.drops)if(d.active()){d.phase=Phase.CANCELLED;d.closedAt=service.now();}next.nextEvent=service.now()+config().intervalMinutes*60_000L;save(next);refreshVisuals(service.now());glows.refresh(service.now(),null);announce("cancelled","An administrator cancelled active AirDrops.{detail}","");}
    public void setDay(int day){checkHealthy();service.require(day>=1&&day<=Math.min(36500,service.now()/86_400_000L+1),"Choose a real-world day within the supported timestamp range.");Data next=copy();next.ageEpoch=service.now()-(day-1)*86_400_000L;save(next);}
    public void collect(ServerPlayer player,UUID dropId,UUID rewardId){
        checkHealthy();Drop d=drop(dropId);service.require(d.phase==Phase.CLAIMED&&service.now()<d.expiresAt&&d.recipients.contains(player.getUUID()),"Only the winning roster can collect this AirDrop.");service.require(chests.canUse(player,d),"Visit the AirDrop chest to collect its loot.");Reward r=d.loot.stream().filter(q->q.id.equals(rewardId)).findFirst().orElseThrow(()->new IllegalArgumentException("Reward not found."));service.require(r.status!=LootStatus.COLLECTED&&r.status!=LootStatus.EXPIRED,"That reward is no longer available.");if(r.status==LootStatus.PENDING){service.require(player.getUUID().equals(r.receiver),"That reward is reserved for another winner.");recover(player);return;}
        service.require(r.money>0||emptySlot(player)>=0,"Keep an empty inventory slot before collecting a reward.");Data next=copy();Reward pending=find(next,dropId).loot.stream().filter(q->q.id.equals(rewardId)).findFirst().orElseThrow();pending.status=LootStatus.PENDING;pending.receiver=player.getUUID();save(next);deliver(player,dropId,rewardId);
    }
    private int emptySlot(ServerPlayer p){for(int slot=0;slot<36;slot++)if(p.getInventory().getItem(slot).isEmpty())return slot;return -1;}
    private void deliver(ServerPlayer player,UUID dropId,UUID rewardId){
        var receipts=((AirDropReceipts)player).kingdoms$airDropReceipts();Reward reward=drop(dropId).loot.stream().filter(r->r.id.equals(rewardId)).findFirst().orElseThrow();String receipt=rewardId.toString();if(reward.money>0){deliverMoney(player,dropId,reward);return;}
        if(!receipts.contains(receipt)&&service.now()>=drop(dropId).expiresAt){Data next=copy();find(next,dropId).loot.stream().filter(r->r.id.equals(rewardId)).findFirst().orElseThrow().status=LootStatus.EXPIRED;save(next);return;}
        if(!receipts.contains(receipt)){int slot=emptySlot(player);if(slot<0)return;player.getInventory().setItem(slot,AirDropLoot.decode(service.server,reward.itemJson));receipts.add(receipt);}
        ((AirDropPlayerSaveInvoker)service.server.getPlayerList()).kingdoms$saveAirDropPlayer(player);
        try{var file=service.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat");var saved=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());if(!Arrays.asList(saved.getStringOr("kingdoms_airdrop_receipts","").split(",")).contains(receipt))throw new IllegalStateException("Player inventory/receipt save could not be verified");}catch(Exception e){healthy=false;throw new IllegalStateException("AirDrop delivery is reserved pending safe player-save recovery",e);}
        Data next=copy();Reward completed=find(next,dropId).loot.stream().filter(r->r.id.equals(rewardId)).findFirst().orElseThrow();completed.status=LootStatus.COLLECTED;save(next);receipts.remove(receipt);if(player.connection!=null)player.containerMenu.broadcastChanges();
    }
    private void deliverMoney(ServerPlayer player,UUID dropId,Reward reward){Drop drop=drop(dropId);if(!service.state.airDropMoneyReceipts.contains(reward.id)&&service.now()>=drop.expiresAt){Data next=copy();find(next,dropId).loot.stream().filter(r->r.id.equals(reward.id)).findFirst().orElseThrow().status=LootStatus.EXPIRED;save(next);return;}UUID kingdom=drop.winner.startsWith("k:")?UUID.fromString(drop.winner.substring(2)):null;service.airDropMoney(reward.id,kingdom,player.getUUID(),reward.money);Data next=copy();find(next,dropId).loot.stream().filter(r->r.id.equals(reward.id)).findFirst().orElseThrow().status=LootStatus.COLLECTED;save(next);Ui.tell(player,"AirDrop cash: "+service.money(reward.money)+(kingdom==null?" paid to your wallet":" deposited into the winning kingdom treasury")+". Tribute applies where owed.",false);}
    public void recover(ServerPlayer player){checkHealthy();List<UUID[]> pending=new ArrayList<>();for(Drop d:data.drops)for(Reward r:d.loot)if(r.status==LootStatus.PENDING&&player.getUUID().equals(r.receiver))pending.add(new UUID[]{d.id,r.id});for(UUID[] item:pending)deliver(player,item[0],item[1]);Set<String> reserved=new HashSet<>();for(Drop d:data.drops)for(Reward r:d.loot)if(r.status==LootStatus.PENDING&&player.getUUID().equals(r.receiver))reserved.add(r.id.toString());((AirDropReceipts)player).kingdoms$airDropReceipts().removeIf(receipt->!receipt.startsWith("artifact-")&&!reserved.contains(receipt));}
    public void recoverStorage(){Data loaded=service.store.read("airdrops.json",Data.class,new Data());AirDropRules.validate(loaded);loaded.drops.forEach(AirDropRules::normalizeCapture);data=loaded;healthy=true;for(ServerPlayer p:service.server.getPlayerList().getPlayers())recover(p);}
    public void stop(){glows.clear();chests.stop();if(healthy)save(data);visuals.values().forEach(AirDropVisuals::clear);visuals.clear();tickets.values().forEach(site->site.level.getChunkSource().removeTicketWithRadius(TICKET,site.chunk,1));tickets.clear();for(UUID id:new HashSet<>(bars.keySet()))clearBars(id);}
}
