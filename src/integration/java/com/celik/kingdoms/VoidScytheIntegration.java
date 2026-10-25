package com.celik.kingdoms;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.BiConsumer;

final class VoidScytheIntegration {
    static void run(KingdomService s,ServerPlayer attacker,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ItemStack hand=attacker.getMainHandItem();var position=attacker.position();ServerLevel level=attacker.level();
        ServerPlayer victim=new ServerPlayer(s.server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VoidVictim"),ClientInformation.createDefault());
        var connection=new ArtifactIntegration.RecordingConnection();var handler=new ServerGamePacketListenerImpl(s.server,connection,victim,CommonListenerCookie.createInitial(victim.getGameProfile(),false));
        Zombie target=new Zombie(EntityTypes.ZOMBIE,level);
        try{
            var loaded=ServerGamePacketListenerImpl.class.getDeclaredField("clientLoadedTimeoutTimer");loaded.setAccessible(true);loaded.setInt(handler,0);var waiting=ServerGamePacketListenerImpl.class.getDeclaredField("waitingForRespawn");waiting.setAccessible(true);waiting.setBoolean(handler,false);s.server.getPlayerList().getPlayers().add(victim);
            attacker.setPos(9008.5,205,7010.5);victim.setPos(9008.5,205,7008.5);victim.setHealth(20);victim.setYRot(0);victim.setYHeadRot(0);attacker.setOnGround(true);attacker.fallDistance=0;
            attacker.setItemInHand(InteractionHand.MAIN_HAND,s.artifacts.create(ArtifactIntegration.issue(s.artifacts,cfg,"void_scythe")));
            victim.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));victim.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));victim.startUsingItem(InteractionHand.OFF_HAND);
            var use=LivingEntity.class.getDeclaredField("useItemRemaining");use.setAccessible(true);use.setInt(victim,Items.SHIELD.getDefaultInstance().getUseDuration(victim)-20);
            var ticker=LivingEntity.class.getDeclaredField("attackStrengthTicker");ticker.setAccessible(true);ticker.setInt(attacker,100);attacker.attack(victim);
            check.accept(victim.getHealth()==20&&s.artifacts.itemDisabled(victim)&&!victim.isUsingItem(),"real shield-blocked Void Scythe hit interrupts blocking and disables held actions");
            check.accept(victim.getCooldowns().isOnCooldown(victim.getMainHandItem())&&victim.getCooldowns().isOnCooldown(victim.getOffhandItem())&&connection.packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundCooldownPacket),"Void applies visible native cooldowns to both held items");
            target.setPos(victim.getX(),victim.getY(),victim.getZ()+1);target.setHealth(20);ticker.setInt(victim,100);victim.attack(target);
            check.accept(target.getHealth()==20,"Void lock blocks actual melee attacks against living targets");
            victim.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SPEAR));check.accept(!victim.stabAttack(EquipmentSlot.MAINHAND,target,8,true,true,true)&&target.getHealth()==20,"Void lock blocks native spear stabs instead of only ordinary swings");
            s.artifacts.tick();check.accept(victim.getCooldowns().isOnCooldown(victim.getMainHandItem()),"switching held items during a Void lock applies the remaining visible cooldown");
            victim.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));victim.startUsingItem(InteractionHand.MAIN_HAND);check.accept(!victim.isUsingItem(),"Void lock blocks direct native start-use actions after switching held items");
            check.accept(victim.gameMode.useItem(victim,level,new ItemStack(Items.ENDER_PEARL),InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"Void lock blocks native right-click item use");
            s.artifacts.leave(victim);victim.stopUsingItem();victim.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);victim.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));victim.setHealth(20);victim.invulnerableTime=0;victim.getCooldowns().addCooldown(victim.getMainHandItem(),200);ticker.setInt(attacker,100);attacker.attack(victim);
            check.accept(victim.getHealth()<20&&s.artifacts.itemDisabled(victim),"ordinary unblocked native Scythe hits also activate the item disable");
            for(int tick=0;tick<40;tick++)victim.getCooldowns().tick();check.accept(victim.getCooldowns().isOnCooldown(victim.getMainHandItem()),"Void does not shorten an existing longer item cooldown");victim.getCooldowns().removeCooldown(victim.getCooldowns().getCooldownGroup(victim.getMainHandItem()));
            Thread.sleep(2100);check.accept(!s.artifacts.itemDisabled(victim),"Void's default two-second disable expires rather than persisting indefinitely");ticker.setInt(victim,100);target.invulnerableTime=0;target.setHealth(20);victim.attack(target);check.accept(target.getHealth()<20,"native melee attacks work again after the Void disable expires");
        }catch(ReflectiveOperationException|InterruptedException e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new AssertionError(e);}
        finally{s.artifacts.leave(victim);s.server.getPlayerList().getPlayers().remove(victim);target.discard();attacker.setItemInHand(InteractionHand.MAIN_HAND,hand);attacker.setPos(position);}
    }
}
