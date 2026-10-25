package com.celik.kingdoms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.*;
import static com.celik.kingdoms.Rules.*;

public final class KingdomsMod implements ModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("kingdoms");
    public static KingdomService service;
    public static Store configStore;
    private int ticks;
    @Override public void onInitialize() {
        KingdomPlaceholders.register();
        configStore=new Store(FabricLoader.getInstance().getConfigDir().resolve("kingdoms"));
        Config config=configStore.read("config.json",Config.class,new Config());config.validate();configStore.write("config.json",config);
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE,net.minecraft.resources.Identifier.parse("kingdoms:ready"));
        ServerLifecycleEvents.SERVER_STARTED.register(net.minecraft.resources.Identifier.parse("kingdoms:ready"),server->{service=new KingdomService(server,server.getWorldPath(LevelResource.ROOT).resolve("kingdoms"),config);service.startTribute();service.tick();service.cores.tick();service.tickBeaconEffects();LOG.info("Kingdoms enabled: {} kingdoms, server-only 26.2",service.state.kingdoms.size());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{if(service!=null){service.maps.stop();service.artifacts.stop();service.cores.highlights.clear();service.airDrops.stop();service.stopTribute();Provenance.flush(service.store);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{service=null;Protection.reset();Provenance.reset();});
        ServerChunkEvents.CHUNK_LOAD.register((level,chunk,generated)->{if(service!=null)service.airDrops.chests.chunkLoaded(level,chunk);});
        ServerChunkEvents.CHUNK_UNLOAD.register((level,chunk)->{if(service!=null)service.airDrops.chests.chunkUnloaded(level,chunk);});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(service==null)return;service.maps.tick();service.cores.highlights.tick();service.airDrops.tick();service.artifacts.tick();service.tickBeaconTerritories();ticks++;if(ticks%20==0){try{service.tick();service.cores.tick();Provenance.flush(service.store);}catch(Exception e){LOG.error("Kingdom upkeep failed; claims remain protected",e);}}if(ticks%service.config.territoryUpdateTicks==0)service.feedback.tick();if(ticks%service.config.beaconRefreshTicks==0)service.tickBeaconEffects();if(ticks%service.config.warningReopenTicks==0)for(ServerPlayer p:server.getPlayerList().getPlayers()){if(service.needsWarning(p)&&(!(p.containerMenu instanceof Ui.Menu m)||!m.warning))Ui.run(p,()->Ui.warning(p));else if(!service.needsWarning(p)&&p.containerMenu instanceof Ui.Menu m&&m.warning)Ui.run(p,()->Ui.home(p));}});
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->KingdomCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{if(service!=null)server.execute(()->{Ui.run(handler.player,()->service.airDrops.recover(handler.player));Ui.run(handler.player,()->service.artifacts.recover(handler.player));service.feedback.join(handler.player);service.cores.join(handler.player);service.applyBeaconEffects(handler.player);});});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(service!=null){service.artifacts.leave(handler.player);service.airDrops.leave(handler.player.getUUID());service.cores.leave(handler.player.getUUID());service.leaveBeaconTerritory(handler.player);service.feedback.leave(handler.player.getUUID());}});
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(service!=null&&entity instanceof ServerPlayer player)try{service.artifacts.crown.death(player,source);}catch(RuntimeException failure){LOG.error("Crown death state could not be saved",failure);}});
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{if(service!=null){service.artifacts.respawn(oldPlayer,newPlayer);service.airDrops.leave(newPlayer.getUUID());service.cores.highlights.reset(newPlayer);}});
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player,origin,destination)->{if(service!=null){service.airDrops.leave(player.getUUID());service.cores.highlights.reset(player);}});
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->Protection.permitted(level,pos,player.getUUID(),Permission.BREAK));
        AttackBlockCallback.EVENT.register((player,level,hand,pos,direction)->Protection.permitted(level,pos,player.getUUID(),Permission.BREAK)?InteractionResult.PASS:InteractionResult.FAIL);
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(service==null||!(player instanceof ServerPlayer p))return InteractionResult.PASS;
            if(service.artifacts.itemDisabled(p))return InteractionResult.FAIL;
            if(service.airDrops.chests.use(p,hit.getBlockPos()))return InteractionResult.SUCCESS;
            if(service.cores.use(p,hand,hit.getBlockPos()))return InteractionResult.SUCCESS;
            var state=level.getBlockState(hit.getBlockPos());Item held=player.getItemInHand(hand).getItem();
            if(held instanceof BucketItem&&state.getBlock() instanceof net.minecraft.world.level.block.AbstractCauldronBlock)return Protection.permitted(level,hit.getBlockPos(),p.getUUID(),held==Items.BUCKET?Permission.BREAK:Permission.PLACE)?InteractionResult.PASS:denied(p);
            if(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("shelf"))for(int slot=0;slot<9;slot++)if(KingdomCores.isToken(p.getInventory().getItem(slot)))return denied(p);
            boolean container=level.getBlockEntity(hit.getBlockPos()) instanceof net.minecraft.world.Container;
            Permission access=container?Permission.CONTAINERS:Permission.INTERACT;
            if(container&&(!(held instanceof BlockItem)||!player.isShiftKeyDown())&&!Protection.container(level,hit.getBlockPos(),p.getUUID()))return denied(p);
            if(!(held instanceof BlockItem)||(container&&!player.isShiftKeyDown()))if(!Protection.permitted(level,hit.getBlockPos(),p.getUUID(),access))return denied(p);
            BlockPos destination=state.canBeReplaced()?hit.getBlockPos():hit.getBlockPos().relative(hit.getDirection());
            if(held instanceof BlockItem||held instanceof SpawnEggItem||held instanceof ArmorStandItem||held instanceof HangingEntityItem)if(!Protection.permitted(level,destination,p.getUUID(),Permission.PLACE))return denied(p);
            if((held instanceof FlintAndSteelItem||held instanceof FireChargeItem)&&!Protection.flag(level,destination,Flag.FIRE))return denied(p);
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            if(service==null||!(player instanceof ServerPlayer p))return InteractionResult.PASS;
            if(service.artifacts.itemDisabled(p))return InteractionResult.FAIL;
            if(service.airDrops.use(p,entity))return InteractionResult.SUCCESS;
            if(KingdomCores.isToken(p.getItemInHand(hand)))return denied(p);
            Kingdom k=service.own(p.getUUID());
            if(p.isShiftKeyDown()&&entity instanceof Mob&&k!=null&&k.mayor(p.getUUID())){Ui.run(p,()->{service.edit(k.id,n->n.allowedMobs.add(entity.getUUID()));Ui.tell(p,"This mob is now allowed in "+k.name+".",false);});return InteractionResult.SUCCESS;}
            return Protection.permitted(level,entity.blockPosition(),p.getUUID(),Permission.ENTITIES)?InteractionResult.PASS:denied(p);
        });
        AttackEntityCallback.EVENT.register((player,level,hand,entity,hit)->!(player instanceof ServerPlayer p&&service!=null&&service.artifacts.preventsAttack(p))&&Protection.damage(entity,player.damageSources().playerAttack(player))?InteractionResult.PASS:InteractionResult.FAIL);
    }
    private static InteractionResult denied(ServerPlayer p){Ui.tell(p,"This kingdom protects that action.",true);return InteractionResult.FAIL;}
}
