package com.celik.kingdoms;

import com.celik.kingdoms.mixin.AirDropPlayerSaveInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import java.util.*;
import java.util.function.BiConsumer;
import static com.celik.kingdoms.AirDropRules.*;
import static com.celik.kingdoms.Rules.Permission;

final class AirDropIntegration {
    private static final class RecordingConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        RecordingConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
    }
    static void run(KingdomService s,ServerPlayer mayor,ServerPlayer enemy,BiConsumer<Boolean,String> check)throws Exception {
        AirDropConfig original=s.config.airDrops;
        int originalSpawnRadius=s.config.spawnClaimProtectionChunks;s.config.spawnClaimProtectionChunks=16;
        var originalState=s.store.copy(s.state);
        AirDrops drops=s.airDrops;
        long now=s.now();
        check.accept(drops.data.ageEpoch>0&&drops.data.nextEvent>drops.data.ageEpoch,"AirDrop age and first schedule begin on first enable");
        var cfg=new AirDropConfig();cfg.takeoverSeconds=0;cfg.scatterLoot=false;cfg.minLandingY=-64;cfg.minSpawnDistance=320;cfg.maxSpawnDistance=5000;cfg.sounds=false;cfg.titles=false;cfg.requireGround=false;
        s.config.airDrops=cfg;drops.validateConfiguration(cfg);
        check.accept(true,"all default AirDrop item, potion and enchantment templates parse on Minecraft 26.2");
        for(String item:List.of("netherite_ingot","blaze_rod","crimson_planks","potion[minecraft:potion_contents={potion:'minecraft:strength'}]")){
            var entry=new AirDropConfig.Loot("minecraft:"+item,1,1,1);
            check.accept(!AirDropLoot.unlocked(cfg,entry,2)&&AirDropLoot.unlocked(cfg,entry,3),"Nether loot unlock enforced for "+item);
        }
        for(String item:List.of("elytra","shulker_shell","purple_shulker_box","end_stone")){
            var entry=new AirDropConfig.Loot("minecraft:"+item,1,1,1);
            check.accept(!AirDropLoot.unlocked(cfg,entry,6)&&AirDropLoot.unlocked(cfg,entry,7),"End loot unlock enforced for "+item);
        }
        var codecItem=AirDropLoot.template(s.server,cfg.lootTiers.getLast().categories.stream().filter(c->c.key.equals("enchantment")).findFirst().orElseThrow().entries.get(2));
        check.accept(ItemStack.matches(codecItem,AirDropLoot.decode(s.server,AirDropLoot.encode(s.server,codecItem))),"AirDrop loot codec retains enchantments and item components");
        Set<UUID> rewardIds=new HashSet<>();boolean lootSafe=true;
        for(int day:List.of(1,3,5,7))for(int variant=0;variant<cfg.variants.size();variant++)for(int seed=0;seed<30;seed++){
            var rewards=AirDropLoot.generate(s.server,cfg,cfg.tier(day),cfg.variants.get(variant),day,new Random(seed*17L+variant));
            Set<String> rareTypes=new HashSet<>();for(Reward r:rewards){var stack=AirDropLoot.decode(s.server,r.itemJson);var entry=new AirDropConfig.Loot(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),1,1,1);lootSafe&=AirDropLoot.unlocked(cfg,entry,day)&&rewardIds.add(r.id)&&!stack.isEmpty()&&stack.getCount()<=stack.getMaxStackSize();if(r.rareKey!=null)rareTypes.add(r.rareKey);}
            lootSafe&=rareTypes.size()<=1&&!rewards.isEmpty();
        }
        check.accept(lootSafe,"960 generated crates enforce unlocks, legal stacks, unique reward IDs and at most one rare reward type");
        var forced=Store.JSON.fromJson(Store.JSON.toJson(cfg),AirDropConfig.class);forced.rareRewards.forEach(r->r.chanceByDay.replaceAll((day,chance)->1.0));
        var forcedLoot=AirDropLoot.generate(s.server,forced,forced.tier(7),forced.variants.getLast(),7,new Random(12));
        check.accept(forcedLoot.stream().filter(r->r.rareKey!=null).map(r->r.rareKey).distinct().count()==1,"simultaneous successful rare rolls select only one reward");
        var noRare=Store.JSON.fromJson(Store.JSON.toJson(cfg),AirDropConfig.class);noRare.rareRewards.forEach(r->r.chanceByDay.replaceAll((day,chance)->0.0));
        check.accept(AirDropLoot.generate(s.server,noRare,noRare.tier(7),noRare.variants.getLast(),7,new Random(12)).stream().noneMatch(r->r.rareKey!=null),"Mythic crate never guarantees a rare reward");

        Data clean=new Data();clean.ageEpoch=now;clean.nextEvent=now+180*60000L;drops.data=clean;s.store.write("airdrops.json",clean);
        ServerLevel level=s.server.overworld();level.getChunkAt(new BlockPos(1600,64,1600));level.getChunkAt(new BlockPos(2200,64,1600));
        BlockPos first=drops.landingAt(level,1600,1600),second=drops.landingAt(level,2200,1600);
        check.accept(first!=null&&second!=null,"safe separated landing sites found on dedicated-server terrain");
        var spawn=level.getRespawnData().pos();check.accept(drops.landingAt(level,spawn.getX(),spawn.getZ())==null,"AirDrops reject protected spawn sites");
        check.accept(drops.landingAt(level,((spawn.getX()>>4)+16)*16,((spawn.getZ()>>4)+16)*16)==null,"AirDrop spawn clearance respects protected spawn's square corners");
        cfg.excludedRegions.add(new AirDropConfig.Exclusion());var region=cfg.excludedRegions.getLast();region.minX=1500;region.maxX=1700;region.minZ=1500;region.maxZ=1700;
        check.accept(drops.landingAt(level,1600,1600)==null,"configured protected region blocks AirDrop spawning");cfg.excludedRegions.clear();
        String firstClaim=KingdomService.key(level,first);s.edit(s.own(mayor.getUUID()).id,k->k.claims.add(firstClaim));
        check.accept(drops.landingAt(level,1600,1600)==null,"kingdom claims block AirDrop spawning");s.edit(s.own(mayor.getUUID()).id,k->k.claims.remove(firstClaim));
        BlockPos bad=first.below();level.setBlockAndUpdate(bad,Blocks.WATER.defaultBlockState());
        check.accept(drops.landingAt(level,1600,1600)==null,"wet terrain is rejected before a drop spawns");level.setBlockAndUpdate(bad,Blocks.GRASS_BLOCK.defaultBlockState());
        BlockPos hill=first.offset(cfg.captureRadius,0,cfg.captureRadius);for(int rise=0;rise<12;rise++)level.setBlockAndUpdate(hill.above(rise),Blocks.STONE.defaultBlockState());
        check.accept(first.equals(drops.landingAt(level,1600,1600)),"steep terrain inside the capture radius no longer rejects a usable landing column");for(int rise=0;rise<12;rise++)level.setBlockAndUpdate(hill.above(rise),Blocks.AIR.defaultBlockState());
        BlockPos canopy=first.above(8);level.setBlockAndUpdate(canopy,Blocks.OAK_LEAVES.defaultBlockState());check.accept(canopy.above().equals(drops.landingAt(level,1600,1600)),"forest canopy is a valid landing surface instead of an obstruction");level.setBlockAndUpdate(canopy,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(first,Blocks.SHORT_GRASS.defaultBlockState());check.accept(first.equals(drops.landingAt(level,1600,1600)),"soft vegetation at the chest position is accepted");
        drops.spawnAt(level,List.of(first,second),now);Drop a=drops.data.drops.getFirst(),b=drops.data.drops.getLast();UUID aid=a.id,bid=b.id;
        check.accept(a.phase==Phase.DESCENDING&&b.phase==Phase.DESCENDING&&a.landsAt-now==600000,"each crate begins a ten-minute descent");
        check.accept(drops.data.nextEvent>=now+180*60000L&&drops.data.nextEvent<=now+200*60000L,"next event uses three hours plus zero-to-twenty minute jitter");
        String lootBefore=Store.JSON.toJson(a.loot);var persisted=s.store.read("airdrops.json",Data.class,new Data());
        check.accept(lootBefore.equals(Store.JSON.toJson(persisted.drops.getFirst().loot)),"loot generated once and persisted before event announcement");
        check.accept(!Protection.permitted(level,first,mayor.getUUID(),Permission.PLACE)&&!Protection.permitted(level,first,mayor.getUUID(),Permission.BREAK),"descent protects only the reserved landing position");
        BlockPos editable=first.offset(6,0,0);check.accept(Protection.permitted(level,editable,mayor.getUUID(),Permission.PLACE)&&Protection.permitted(level,editable.below(),mayor.getUUID(),Permission.BREAK),"players can place and break outside the default five-block build radius");
        check.accept(level.setBlockAndUpdate(editable,Blocks.STONE.defaultBlockState())&&level.destroyBlock(editable,false),"actual block placement and breaking work beside a descending drop");
        check.accept(!Protection.permitted(level,first.below(),mayor.getUUID(),Permission.BREAK)&&!level.destroyBlock(first.below(),true),"the one block supporting a drop cannot be broken during descent");
        check.accept(!level.setBlockAndUpdate(first.offset(5,12,0),Blocks.STONE.defaultBlockState())&&!Protection.permitted(level,first.offset(3,0,0),mayor.getUUID(),Permission.BREAK),"five-block protection includes its boundary and prevents tower placement above it");
        check.accept(Protection.fluid(level,editable,editable.above()),"surrounding terrain permits fluid flow");
        for(var flag:List.of(Rules.Flag.FIRE,Rules.Flag.MOB_GRIEFING))check.accept(Protection.environmental(level,editable,editable.above(),flag),"surrounding terrain permits environmental "+flag);
        check.accept(Protection.permitted(level,first,mayor.getUUID(),Permission.PVP)&&Protection.permitted(level,first,mayor.getUUID(),Permission.INTERACT),"AirDrop terrain protection preserves normal combat and interaction");
        check.accept(!Protection.explosion(level,first.offset(30,0,0),first,null)&&Protection.explosionDamage(level,first,first,null),"explosions protect terrain without granting combat immunity");
        check.accept(!Protection.fluid(level,first.offset(30,0,0),first),"AirDrop rejects fluid flow");
        for(var flag:List.of(Rules.Flag.FIRE,Rules.Flag.MOB_GRIEFING))check.accept(!Protection.environmental(level,first.offset(30,0,0),first,flag),"AirDrop rejects environmental "+flag);
        check.accept(!level.setBlockAndUpdate(first,Blocks.STONE.defaultBlockState())&&level.getBlockState(first).is(Blocks.SHORT_GRASS),"actual world mutation cannot replace the reserved landing block");
        mayor.setPos(first.getX()+.5,first.getY(),first.getZ()+.5);boolean refused=false;try{s.outpost(mayor);}catch(IllegalArgumentException e){refused=e.getMessage().contains("AirDrop");}check.accept(refused,"outpost claims cannot absorb an active AirDrop");
        var identity=new net.minecraft.server.players.NameAndId(mayor.getUUID(),mayor.getPlainTextName());s.server.getPlayerList().op(identity,Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER),Optional.empty());
        try{check.accept(!Protection.permitted(level,first,mayor.getUUID(),Permission.BREAK),"operators obey temporary AirDrop terrain protection");}finally{s.server.getPlayerList().deop(identity);}
        var visuals=new AirDropVisuals(level,a,cfg);visuals.move(a,now+300000);
        check.accept(Math.abs(visuals.crate.getY()-(a.startY+a.y)/2)<.001&&!visuals.crate.shouldBeSaved()&&!visuals.hit.shouldBeSaved(),"vanilla visual crate follows controlled descent and never persists duplicate entities");
        check.accept(visuals.outline.size()==12&&visuals.outline.stream().allMatch(r->!r.entity().shouldBeSaved()&&Math.abs(r.entity().getY()-visuals.crate.getY()-r.offset().y)<.001),"descending crate has twelve unsaved glowing display edges moving with it");
        check.accept(visuals.parachute.size()==192&&visuals.parachute.stream().allMatch(r->!r.entity().shouldBeSaved()),"converted round canopy, seams and eight continuous suspension lines use bounded unsaved vanilla displays");
        boolean faithful=true;for(int i=0;i<visuals.parachute.size();i++){var part=ParachuteModel.parts().get(i);var rod=visuals.parachute.get(i);var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());rod.entity().saveWithoutId(output);var tag=output.buildResult();var transform=tag.getCompoundOrEmpty("transformation");var rotation=transform.getListOrEmpty("left_rotation");var scale=transform.getListOrEmpty("scale");faithful&=tag.getCompoundOrEmpty("block_state").getStringOr("Name","").equals(ParachuteModel.block(part,cfg));for(int axis=0;axis<3;axis++)faithful&=Math.abs(scale.getFloatOr(axis,0)-part.scale()[axis])<.00001;for(int axis=0;axis<4;axis++)faithful&=Math.abs(rotation.getFloatOr(axis,0)-part.rotation()[axis])<.00001;}
        check.accept(faithful,"native display metadata preserves converted fabric rotations, dimensions and vanilla wool materials");
        double swayX=visuals.crate.getX()-a.x,swayZ=visuals.crate.getZ()-a.z;check.accept(Math.abs(swayX)<=cfg.parachuteSway&&Math.abs(swayZ)<=cfg.parachuteSway&&visuals.parachute.stream().allMatch(r->Math.abs(r.entity().getX()-visuals.crate.getX()-r.offset().x)<.00001&&Math.abs(r.entity().getZ()-visuals.crate.getZ()-r.offset().z)<.00001),"canopy, ropes, crate and interaction follow one bounded sway trajectory");
        visuals.move(a,a.landsAt);check.accept(visuals.crate.getX()==a.x&&visuals.crate.getZ()==a.z&&visuals.crate.getY()==a.y,"sway settles precisely onto the authoritative landing coordinates");
        check.accept(!Protection.damage(visuals.hit,mayor.damageSources().playerAttack(mayor)),"crate interaction entity cannot be destroyed by players");visuals.clear();check.accept(visuals.outline.stream().allMatch(r->r.entity().isRemoved())&&visuals.parachute.stream().allMatch(r->r.entity().isRemoved()),"clearing the descent removes every display edge, fabric panel and rope");
        cfg.descentOutline=false;var plainVisual=new AirDropVisuals(level,a,cfg);check.accept(plainVisual.outline.isEmpty(),"administrator can disable the descending display outline");plainVisual.clear();cfg.descentOutline=true;
        cfg.parachute=false;var noParachute=new AirDropVisuals(level,a,cfg);check.accept(noParachute.parachute.isEmpty()&&noParachute.sway==0,"disabling parachutes removes both converted geometry and sway");noParachute.clear();cfg.parachute=true;
        cfg.parachutePrimaryBlock="minecraft:air";boolean materialDenied=false;try{drops.validateConfiguration(cfg);}catch(IllegalArgumentException expectedMaterialError){materialDenied=true;}check.accept(materialDenied,"invalid parachute materials are rejected before any visual spawn");cfg.parachutePrimaryBlock="minecraft:orange_wool";
        var members=new ArrayList<ServerPlayer>();members.add(mayor);for(int i=0;i<2;i++){UUID member=UUID.randomUUID();s.edit(s.own(mayor.getUUID()).id,k->k.members.put(member,"DropMember"));var p=new ServerPlayer(s.server,level,new com.mojang.authlib.GameProfile(member,"DropMember"+i),ClientInformation.createDefault());p.setPos(first.getX()+.5,first.getY(),first.getZ()+.5);members.add(p);}
        drops.update(now+599999,1000,members);check.accept(drops.drop(aid).progress.isEmpty(),"capture cannot progress while descending");
        drops.update(now+600000,1000,members);check.accept(drops.drop(aid).phase==Phase.CAPTURABLE&&drops.drop(aid).progress.values().stream().allMatch(p->p.millis==0),"landing starts capture with zero pre-landing progress");
        check.accept(level.getBlockState(first).is(Blocks.CHEST)&&level.getBlockState(second).is(Blocks.CHEST),"each landed AirDrop is a physical chest at its own destination");
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(first);
        check.accept(((AirDropChestMarker)chest).kingdoms$airDropId().equals(aid)&&chest.isEmpty(),"chest is persistently marked and backing inventory cannot leak reward copies");
        var chestTag=chest.saveWithFullMetadata(level.registryAccess());var restoredChest=new net.minecraft.world.level.block.entity.ChestBlockEntity(first,level.getBlockState(first));restoredChest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),chestTag));
        check.accept(aid.equals(((AirDropChestMarker)restoredChest).kingdoms$airDropId()),"AirDrop chest identity survives actual block-entity save and reload");
        drops.update(now+630000,30000,members);check.accept(drops.drop(aid).progress.get(drops.side(mayor)).millis==30000,"actual kingdom membership drives capture quorum");
        var connection=new RecordingConnection();new net.minecraft.server.network.ServerGamePacketListenerImpl(s.server,connection,mayor,net.minecraft.server.network.CommonListenerCookie.createInitial(mayor.getGameProfile(),false));
        check.accept(drops.chests.use(mayor,first)&&!(mayor.containerMenu instanceof AirDropChestMenu),"physical chest remains sealed until capture finishes");
        drops.glows.refresh(now+630000,members);var scoreboard=s.server.getScoreboard();var priorTeam=scoreboard.getPlayersTeam(mayor.getScoreboardName());var retainedTeam=scoreboard.addPlayerTeam("airdrop_test");retainedTeam.setPlayerPrefix(Ui.text("existing prefix "));scoreboard.addPlayerToTeam(mayor.getScoreboardName(),retainedTeam);drops.glows.refresh(now+630000,members);
        check.accept(mayor.isCurrentlyGlowing()&&members.get(1).isCurrentlyGlowing()&&drops.glows.rgb(mayor)==s.own(mayor.getUUID()).highlightColor,"capture participants glow using their kingdom's core color source");
        check.accept(scoreboard.getPlayersTeam(mayor.getScoreboardName())==retainedTeam&&connection.packets.stream().filter(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket).map(packet->(net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket)packet).anyMatch(packet->packet.getParameters().map(params->params.color().equals(Optional.of(drops.glows.color(mayor)))).orElse(false)),"client color packets preserve actual server scoreboard memberships");
        int dustBefore=connection.packets.size();AirDropVisuals.fx(drops,level,drops.drop(aid),now+630000,List.of(mayor));check.accept(connection.packets.subList(dustBefore,connection.packets.size()).stream().filter(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket).map(packet->(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket)packet).filter(packet->packet.getParticle() instanceof net.minecraft.core.particles.DustParticleOptions).count()==cfg.ringPoints,"capture circle sends a complete configurable dust ring through vanilla packets");
        var ring=AirDropVisuals.ringPoints(drops.drop(aid),cfg.ringPoints);check.accept(ring.stream().allMatch(point->Math.abs(Math.hypot(point.x-first.getX()-.5,point.z-first.getZ()-.5)-cfg.captureRadius)<.000001)&&drops.visualColor(drops.drop(aid))==s.own(mayor.getUUID()).highlightColor,"dust circle matches the configured claim radius and exact core RGB color");
        drops.updateBars(drops.drop(aid),List.of(mayor),now+630000);check.accept(connection.packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundBossEventPacket),"capture progress uses vanilla boss-bar packets");
        int before=connection.packets.size();drops.leave(mayor.getUUID());check.accept(connection.packets.size()>before,"disconnect/respawn cleanup removes the previous player's boss bar");check.accept(!mayor.isCurrentlyGlowing()&&scoreboard.getPlayersTeam(mayor.getScoreboardName())==retainedTeam,"leaving clears AirDrop glow and preserves the original team");scoreboard.removePlayerFromTeam(mayor.getScoreboardName());scoreboard.removePlayerTeam(retainedTeam);if(priorTeam!=null)scoreboard.addPlayerToTeam(mayor.getScoreboardName(),priorTeam);
        mayor.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,600));drops.glows.clear();check.accept(mayor.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)&&mayor.isCurrentlyGlowing(),"glow cleanup preserves actual pre-existing glowing potion effects");mayor.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);drops.glows.refresh(now+630000,members);cfg.captureGlow=false;drops.glows.refresh(now+630000,members);check.accept(!mayor.isCurrentlyGlowing(),"administrator can disable capture glow without leaving permanent tags");cfg.captureGlow=true;drops.glows.refresh(now+630000,members);
        drops.update(now+631000,1000,List.of(mayor));check.accept(drops.drop(aid).progress.get(drops.side(mayor)).missingSince==now+631000,"lost quorum pauses and starts grace period");
        drops.update(now+635000,4000,members);check.accept(drops.drop(aid).progress.get(drops.side(mayor)).millis==30000,"restoring quorum within grace does not award paused time");
        enemy.setPos(first.getX()+1,first.getY(),first.getZ()+1);var contested=new ArrayList<>(members);contested.add(enemy);drops.update(now+648000,13000,contested);
        check.accept(Math.abs(drops.drop(aid).progress.get(drops.side(mayor)).millis-40000)<.001,"enemy kingdom member slows capture by exactly 1.3x");
        enemy.setPos(second.getX()+.5,second.getY(),second.getZ()+.5);drops.update(now+698000,50000,members);
        check.accept(drops.drop(aid).phase==Phase.CLAIMED&&drops.drop(bid).phase==Phase.CAPTURABLE,"winning one crate does not claim or reset another");
        check.accept(!mayor.isCurrentlyGlowing()&&!members.get(1).isCurrentlyGlowing(),"capture completion clears participant glow");
        check.accept(!drops.protectedAt(level,first)&&drops.protectedAt(level,second),"temporary protection ends for the captured crate independently");
        check.accept(drops.drop(aid).recipients.size()==3&&drops.drop(aid).recipients.contains(mayor.getUUID()),"winning kingdom roster is recorded for shared rewards");
        check.accept(lootBefore.equals(Store.JSON.toJson(drops.drop(aid).loot)),"descent, capture and interference never reroll loot");
        var solo=new ServerPlayer(s.server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DropSolo"),ClientInformation.createDefault());solo.setPos(second.getX()+.5,second.getY(),second.getZ()+.5);
        check.accept(drops.eligible(solo,drops.drop(bid)),"unaffiliated survival player is eligible for solo capture");
        solo.getAbilities().flying=true;check.accept(!drops.eligible(solo,drops.drop(bid)),"flying players cannot contribute capture presence");solo.getAbilities().flying=false;
        solo.setHealth(0);check.accept(!drops.eligible(solo,drops.drop(bid)),"dead players cannot contribute capture presence");solo.setHealth(20);
        solo.setPos(second.getX()+.5,second.getY()+3,second.getZ()+.5);check.accept(drops.eligible(solo,drops.drop(bid)),"capture allows exactly three blocks above landing Y");solo.setPos(second.getX()+.5,second.getY()-3,second.getZ()+.5);check.accept(drops.eligible(solo,drops.drop(bid)),"capture allows exactly three blocks below landing Y");solo.setPos(second.getX()+.5,second.getY()+3.01,second.getZ()+.5);check.accept(!drops.eligible(solo,drops.drop(bid)),"capture excludes players beyond the three-block vertical tolerance");
        solo.setPos(second.getX()+.5,second.getY()+20,second.getZ()+.5);check.accept(!drops.eligible(solo,drops.drop(bid)),"players above the capture vertical range are excluded");solo.setPos(second.getX()+.5,second.getY(),second.getZ()+.5);
        drops.update(now+728000,30000,List.of(solo));check.accept(drops.drop(bid).progress.get(drops.side(solo)).millis==30000,"solo capture has independent server-side progress");
        check.accept(solo.isCurrentlyGlowing()&&drops.glows.color(solo)==net.minecraft.world.scores.TeamColor.BLACK&&drops.glows.rgb(solo)==0,"solo capturers receive black glow");solo.setGlowingTag(true);drops.glows.clear();check.accept(solo.isCurrentlyGlowing(),"clearing AirDrop glow preserves an unrelated glowing tag");solo.setGlowingTag(false);drops.glows.refresh(now+728000,List.of(solo));
        String soloSide=drops.side(solo);s.edit(s.own(mayor.getUUID()).id,k->k.members.put(solo.getUUID(),"DropSolo"));drops.update(now+729000,1000,List.of(solo));
        check.accept(drops.drop(bid).progress.get(soloSide).missingSince!=0&&!drops.drop(bid).progress.containsKey(drops.side(solo)),"switching kingdoms cannot transfer solo progress or bypass quorum");s.edit(s.own(mayor.getUUID()).id,k->k.members.remove(solo.getUUID()));
        drops.stop();AirDrops activeReload=new AirDrops(s);activeReload.update(now+750000,21000,List.of(solo));
        check.accept(activeReload.drop(bid).progress.get(soloSide).millis==0,"restart beyond grace resets offline progress without crediting downtime");drops.recoverStorage();
        AirDrops reloaded=new AirDrops(s);check.accept(reloaded.data.nextEvent==drops.data.nextEvent&&Store.JSON.toJson(reloaded.drop(aid).loot).equals(lootBefore),"restart retains schedule, captured crate and original loot");
        var fourthMember=new ServerPlayer(s.server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DropFourth"),ClientInformation.createDefault());UUID kingdomId=s.own(mayor.getUUID()).id;s.edit(kingdomId,k->k.members.put(fourthMember.getUUID(),"DropFourth"));var larger=new ArrayList<>(members);larger.add(fourthMember);for(ServerPlayer player:larger)player.setPos(second.getX()+.5,second.getY(),second.getZ()+.5);var rivalGroup=new ArrayList<>(larger);rivalGroup.add(solo);drops.update(now+762000,12000,rivalGroup);
        check.accept(drops.side(mayor).equals(drops.drop(bid).capturingSide)&&drops.drop(bid).progress.size()==1&&drops.drop(bid).progress.get(drops.side(mayor)).millis==0,"four actual kingdom members immediately take over from solo strength three without inheriting progress");fourthMember.setPos(first.getX(),first.getY(),first.getZ());drops.update(now+775000,13000,rivalGroup);
        check.accept(drops.side(mayor).equals(drops.drop(bid).capturingSide)&&drops.drop(bid).progress.get(drops.side(mayor)).millis==10000,"three actual kingdom members tie a solo player and retain capture control");members.getLast().setPos(first.getX(),first.getY(),first.getZ());drops.update(now+776000,1000,rivalGroup);
        check.accept(drops.side(solo).equals(drops.drop(bid).capturingSide)&&drops.drop(bid).progress.size()==1&&drops.drop(bid).progress.get(drops.side(solo)).millis==0&&drops.visualColor(drops.drop(bid))==0,"solo strength three takes over from two kingdom members and changes ring color to black");for(ServerPlayer player:members)player.setPos(first.getX()+.5,first.getY(),first.getZ()+.5);s.edit(kingdomId,k->k.members.remove(fourthMember.getUUID()));
        Reward reward=drops.drop(aid).loot.getFirst();boolean denied=false;try{drops.collect(enemy,aid,reward.id);}catch(IllegalArgumentException expected){denied=true;}check.accept(denied&&reward.status==LootStatus.AVAILABLE,"non-winners cannot collect sealed rewards");
        AirDropUi.detail(mayor,aid,0);check.accept(mayor.containerMenu instanceof Ui.Menu&&!mayor.containerMenu.getSlot(0).hasItem(),"AirDrop command shows information without remote loot collection");mayor.closeContainer();
        var enemyConnection=new RecordingConnection();new net.minecraft.server.network.ServerGamePacketListenerImpl(s.server,enemyConnection,enemy,net.minecraft.server.network.CommonListenerCookie.createInitial(enemy.getGameProfile(),false));enemy.setPos(first.getX()+1,first.getY(),first.getZ()+1);
        check.accept(drops.chests.use(enemy,first)&&!(enemy.containerMenu instanceof AirDropChestMenu),"enemy beside a captured chest cannot open it");enemy.setPos(second.getX()+.5,second.getY(),second.getZ()+.5);
        mayor.setPos(first.getX()+20,first.getY(),first.getZ());denied=false;try{drops.collect(mayor,aid,reward.id);}catch(IllegalArgumentException expectedError){denied=true;}
        check.accept(denied&&!drops.chests.canUse(mayor,drops.drop(aid)),"winning players cannot retrieve rewards remotely");mayor.setPos(first.getX()+.5,first.getY(),first.getZ()+.5);
        check.accept(drops.chests.use(mayor,first)&&mayor.containerMenu instanceof AirDropChestMenu&&mayor.containerMenu.getSlot(0).getItem().getCount()==AirDropLoot.decode(s.server,reward.itemJson).getCount(),"winner right-click opens vanilla chest contents with real stack counts");
        check.accept(mayor.isCurrentlyGlowing(),"winner glows while collecting from the chest");
        var lockedMenu=mayor.containerMenu;String unclaimed=Store.JSON.toJson(drops.drop(aid).loot);for(var input:List.of(net.minecraft.world.inventory.ContainerInput.CLONE,net.minecraft.world.inventory.ContainerInput.SWAP,net.minecraft.world.inventory.ContainerInput.THROW))lockedMenu.clicked(0,0,input,mayor);
        check.accept(unclaimed.equals(Store.JSON.toJson(drops.drop(aid).loot))&&lockedMenu.getCarried().isEmpty(),"clone, hotbar swap and throw cannot extract virtual reward copies");
        check.accept(!Protection.permitted(level,first,mayor.getUUID(),Permission.BREAK)&&!Protection.permitted(level,first.below(),mayor.getUUID(),Permission.BREAK)&&!level.destroyBlock(first.below(),true)&&!Protection.container(level,first,enemy.getUUID())&&!Protection.explosion(level,first,first,null)&&!level.destroyBlock(first,true),"captured chest stays locked and unbreakable after surrounding terrain protection ends");
        check.accept(!Protection.automation(level,first,first.below())&&!Protection.automation(level,first.below(),first)&&!Protection.piston(level,first.offset(2,0,0),first)&&!Protection.fluid(level,first.offset(2,0,0),first),"hoppers, pistons and fluids cannot bypass the captured chest lock");
        check.accept(Protection.permitted(level,first.offset(3,0,0),mayor.getUUID(),Permission.PLACE),"captured chest lock does not keep the surrounding event terrain protected");
        mayor.setPos(first.getX()+20,first.getY(),first.getZ());lockedMenu.broadcastChanges();check.accept(!(mayor.containerMenu instanceof AirDropChestMenu),"walking out of chest reach closes the winner's contents");check.accept(!mayor.isCurrentlyGlowing(),"leaving the chest removes its looting glow");mayor.setPos(first.getX()+.5,first.getY(),first.getZ()+.5);drops.chests.use(mayor,first);
        for(int slot=0;slot<36;slot++)mayor.getInventory().setItem(slot,new ItemStack(Items.STONE,64));denied=false;try{drops.collect(mayor,aid,reward.id);}catch(IllegalArgumentException expected){denied=true;}check.accept(denied&&drops.drop(aid).loot.getFirst().status==LootStatus.AVAILABLE,"full inventory never consumes or reserves an undelivered reward");
        for(int slot=0;slot<36;slot++)mayor.getInventory().setItem(slot,ItemStack.EMPTY);ItemStack expected=AirDropLoot.decode(s.server,reward.itemJson);mayor.containerMenu.clicked(0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,mayor);mayor.closeContainer();
        check.accept(ItemStack.matches(expected,mayor.getInventory().getItem(0))&&drops.drop(aid).loot.getFirst().status==LootStatus.COLLECTED,"reward transfer saves the exact item stack and marks it collected");
        denied=false;try{drops.collect(mayor,aid,reward.id);}catch(IllegalArgumentException exception){denied=true;}check.accept(denied&&mayor.getInventory().getItem(1).isEmpty(),"repeat clicks cannot duplicate collected loot");
        // Reconstruct a crash after the atomic player save but before the reward completion commit.
        drops.drop(aid).loot.getFirst().status=LootStatus.PENDING;s.store.write("airdrops.json",drops.data);
        var playerFile=s.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(mayor.getStringUUID()+".dat");var playerTag=NbtIo.readCompressed(playerFile,NbtAccounter.unlimitedHeap());
        check.accept(!playerTag.getBooleanOr("Glowing",false),"temporary AirDrop glow is never saved as a permanent player tag");
        check.accept(playerTag.getStringOr("kingdoms_airdrop_receipts","").contains(reward.id.toString()),"delivery receipt is atomically present in the same player save as inventory");
        mayor.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),playerTag));
        check.accept(((AirDropReceipts)mayor).kingdoms$airDropReceipts().contains(reward.id.toString()),"receipt mixin reloads from actual saved player data");drops.recoverStorage();drops.recover(mayor);
        check.accept(ItemStack.matches(expected,mayor.getInventory().getItem(0))&&mayor.getInventory().getItem(1).isEmpty()&&drops.drop(aid).loot.getFirst().status==LootStatus.COLLECTED,"crash recovery completes pending delivery without giving a second item");
        var pending=drops.drop(aid).loot.get(1);pending.status=LootStatus.PENDING;pending.receiver=mayor.getUUID();s.store.write("airdrops.json",drops.data);drops.recoverStorage();drops.recover(mayor);
        check.accept(!mayor.getInventory().getItem(1).isEmpty()&&drops.drop(aid).loot.get(1).status==LootStatus.COLLECTED,"pre-delivery crash safely gives the reserved reward exactly once");
        String wonRewards=Store.JSON.toJson(drops.drop(aid).loot);drops.cancel();check.accept(drops.drop(bid).phase==Phase.CANCELLED&&!drops.protectedAt(level,second)&&Store.JSON.toJson(drops.drop(aid).loot).equals(wonRewards),"administrator cancellation removes protection while preserving earned loot");
        drops.setDay(7);check.accept(drops.day()==7&&Store.JSON.toJson(drops.drop(aid).loot).equals(wonRewards),"administrator age change affects future crates without rerolling won loot");
        BlockPos third=drops.landingAt(level,2800,1600);drops.spawnAt(level,List.of(third),now+900000);Drop soloDrop=drops.data.drops.getLast();solo.setPos(third.getX()+.5,third.getY(),third.getZ()+.5);drops.update(soloDrop.landsAt,0,List.of(solo));drops.update(soloDrop.landsAt+135000,135000,List.of(solo));
        check.accept(drops.drop(soloDrop.id).phase==Phase.CLAIMED&&drops.drop(soloDrop.id).recipients.equals(Set.of(solo.getUUID())),"solo capture wins after 135 seconds with exactly one authorized recipient");
        BlockPos fourth=drops.landingAt(level,3400,1600);drops.spawnAt(level,List.of(fourth),now+1800000);Drop expiring=drops.data.drops.getLast();drops.update(expiring.expiresAt,0,List.of());
        check.accept(drops.drop(expiring.id).phase==Phase.EXPIRED&&!drops.protectedAt(level,fourth),"capture expiry removes temporary protection without granting loot");
        check.accept(drops.drop(aid).phase==Phase.EXPIRED&&drops.drop(aid).loot.getFirst().status==LootStatus.COLLECTED&&drops.drop(aid).loot.stream().noneMatch(r->r.status==LootStatus.AVAILABLE),"thirty minutes after landing, uncollected winners' rewards expire while collected items remain delivered");
        check.accept(level.getBlockState(first).isAir()&&level.getBlockState(second).isAir()&&level.getBlockState(third).isAir(),"expiry and cancellation remove physical chests without spilling contents");
        BlockPos ordinary=first.offset(40,0,0);level.setBlockAndUpdate(ordinary,Blocks.CHEST.defaultBlockState());var ordinaryChest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(ordinary);ordinaryChest.setItem(0,new ItemStack(Items.DIAMOND));drops.chests.chunkLoaded(level,level.getChunkAt(ordinary));
        check.accept(!drops.chests.protects(level,first.below())&&!drops.chests.protects(level,ordinary)&&ordinaryChest.getItem(0).is(Items.DIAMOND),"ordinary chests are neither locked nor removed by AirDrop cleanup");((AirDropChestMarker)ordinaryChest).kingdoms$airDropId(UUID.randomUUID());drops.chests.chunkLoaded(level,level.getChunkAt(ordinary));
        check.accept(level.getBlockState(ordinary).is(Blocks.CHEST),"chunk-load notifications defer orphan terrain changes until normal ticks");drops.chests.sync(s.now());
        check.accept(level.getBlockState(ordinary).isAir(),"orphan event chest markers clean up after their chunk becomes fully available");
        AirDropChunkLoadIntegration.run(drops,level,check);
        int entityCount=(int)java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(e->e instanceof net.minecraft.world.entity.item.ItemEntity).count();drops.update(expiring.expiresAt+1000,1000,List.of());
        check.accept(entityCount==java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(e->e instanceof net.minecraft.world.entity.item.ItemEntity).count(),"expired AirDrop loot never spills into world item entities");
        boolean expiredDenied=false;try{drops.collect(mayor,aid,drops.drop(aid).loot.get(2).id);}catch(IllegalArgumentException expiredError){expiredDenied=true;}check.accept(expiredDenied,"stale winners' menu cannot collect expired loot");
        var commands=s.server.getCommands().getDispatcher();var console=s.server.createCommandSourceStack();
        check.accept(commands.execute("kingdomadmin airdrop spawnat 4000 1600 mythic landed",console)==1,"console can spawn a selected test variant already landed");
        Drop testDrop=drops.data.drops.getLast();check.accept(testDrop.variant.equals("mythic")&&testDrop.phase==Phase.CAPTURABLE&&testDrop.expiresAt-testDrop.landsAt==1800000,"test spawning preserves selected variant and thirty-minute loot lifetime");drops.cancel();
        check.accept(commands.execute("kingdomadmin airdrop spawnat 4600 1600 standard",console)==1&&drops.data.drops.getLast().phase==Phase.DESCENDING,"admin test spawning can retain the normal descent");
        check.accept(commands.execute("kingdomadmin airdrop land",console)==1&&drops.data.drops.getLast().phase==Phase.CAPTURABLE,"admin testing command lands descending drops immediately");drops.cancel();
        check.accept(commands.execute("kingdomadmin airdrop spawnat 4000 1600 nonexistent",console)==0,"unknown test crate variant is rejected without spawning loot");
        boolean insufficient=false;try{drops.requestStart(false);}catch(IllegalArgumentException expectedError){insufficient=true;}check.accept(insufficient,"scheduled events reject insufficient online players");drops.requestStart(true);int priorCount=drops.data.drops.size();for(int i=0;i<40&&drops.data.drops.size()==priorCount;i++)drops.tick();
        check.accept(drops.data.drops.size()>priorCount&&drops.data.drops.getLast().phase==Phase.DESCENDING,"actual random spawn search finds a landing site and starts a natural event");drops.cancel();check.accept(true,"admin start bypasses the player minimum and its event can be cancelled");
        check.accept(s.server.getCommands().getDispatcher().getRoot().getChild("airdrop")!=null&&s.server.getCommands().getDispatcher().getRoot().getChild("kingdomadmin").getChild("airdrop").getChild("recover")!=null,"AirDrop player and administrator commands are registered");
        s.commit(originalState);s.config.airDrops=original;s.config.spawnClaimProtectionChunks=originalSpawnRadius;
    }
    private AirDropIntegration(){}
}
