package com.celik.kingdoms;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Crown progress belongs to a player's current life, never to the transferable item. */
public final class CrownPowers {
    public static final Identifier HEALTH=Identifier.parse("kingdoms:crown_health");
    private final Artifacts artifacts;
    private final KingdomService service;
    CrownPowers(Artifacts artifacts,KingdomService service){this.artifacts=artifacts;this.service=service;}
    private ArtifactRules.Record crown(ItemStack stack){var r=artifacts.record(stack);return r!=null&&r.type.equals("golden_crown")?r:null;}
    public boolean locked(ServerPlayer player){return player.isAlive()&&crown(player.getItemBySlot(EquipmentSlot.HEAD))!=null;}
    public boolean mayReplace(ServerPlayer player,ItemStack next){if(!locked(player))return true;var old=crown(player.getItemBySlot(EquipmentSlot.HEAD));var replacement=crown(next);return replacement!=null&&replacement.id.equals(old.id);}
    public void sync(ServerPlayer player){
        var record=artifacts.usable(player.getItemBySlot(EquipmentSlot.HEAD));
        boolean active=player.isAlive()&&record!=null&&record.type.equals("golden_crown");
        ArtifactRules.CrownLife life=artifacts.data.crownLives.get(player.getUUID());
        if(active&&(life==null||!life.crown.equals(record.id))){
            boolean fresh=life==null;if(fresh)life=new ArtifactRules.CrownLife();life.crown=record.id;artifacts.data.crownLives.put(player.getUUID(),life);
            try{artifacts.saveCrown();}catch(RuntimeException failure){if(fresh)artifacts.data.crownLives.remove(player.getUUID());throw failure;}
        }
        double bonus=active?ArtifactRules.crownBonus(life,artifacts.config()):0;
        var health=player.getAttribute(Attributes.MAX_HEALTH);var modifier=health.getModifier(HEALTH);
        if(modifier!=null&&modifier.amount()!=bonus){health.removeModifier(HEALTH);modifier=null;}
        if(bonus>0&&modifier==null)health.addPermanentModifier(new AttributeModifier(HEALTH,bonus,AttributeModifier.Operation.ADD_VALUE));
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
        if(active){var glow=player.getEffect(MobEffects.GLOWING);if(glow==null||!glow.isInfiniteDuration()&&glow.getDuration()<40)player.addEffect(new MobEffectInstance(MobEffects.GLOWING,100,0,true,false,true));}
    }
    public void tick(){for(ServerPlayer player:List.copyOf(service.server.getPlayerList().getPlayers()))sync(player);}
    public void killed(ServerPlayer killer,UUID victim){
        if(killer.getUUID().equals(victim)||!locked(killer)||artifacts.usable(killer.getItemBySlot(EquipmentSlot.HEAD))==null)return;
        float oldMax=killer.getMaxHealth();sync(killer);var life=artifacts.data.crownLives.get(killer.getUUID());
        if(ArtifactRules.crownBonus(life,artifacts.config())>=artifacts.config().crownMaxBonusHealth||!life.victims.add(victim))return;
        try{artifacts.saveCrown();}catch(RuntimeException failure){life.victims.remove(victim);throw failure;}
        sync(killer);killer.setHealth(Math.min(killer.getMaxHealth(),killer.getHealth()+Math.max(0,killer.getMaxHealth()-oldMax)));
    }
    public void died(ServerPlayer player){
        try{if(artifacts.data.crownLives.remove(player.getUUID())!=null)artifacts.saveCrown();}
        finally{player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH);}
    }
    public void death(ServerPlayer victim,DamageSource source){
        ServerPlayer killer=source.getEntity() instanceof ServerPlayer direct?direct:victim.getKillCredit() instanceof ServerPlayer credited?credited:null;
        died(victim);if(killer!=null&&killer!=victim)killed(killer,victim.getUUID());
    }
}
