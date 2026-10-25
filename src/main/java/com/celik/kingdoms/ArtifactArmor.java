package com.celik.kingdoms;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.*;

/** Short, owned effects: stronger/longer external potion effects are never removed or downgraded. */
final class ArtifactArmor {
    private final Artifacts artifacts;
    private final Map<UUID,Map<Holder<MobEffect>,MobEffectInstance>> issued=new HashMap<>();
    ArtifactArmor(Artifacts artifacts){this.artifacts=artifacts;}
    void tick(ServerPlayer player){
        Map<Holder<MobEffect>,MobEffectInstance> previous=issued.computeIfAbsent(player.getUUID(),id->new HashMap<>());
        Map<Holder<MobEffect>,Boolean> active=Map.of(MobEffects.SPEED,worn(player,EquipmentSlot.FEET,"runners_boots"),MobEffects.REGENERATION,worn(player,EquipmentSlot.CHEST,"kings_chestplate"),MobEffects.STRENGTH,worn(player,EquipmentSlot.HEAD,"golden_crown"));
        active.forEach((effect,on)->{
            MobEffectInstance current=player.getEffect(effect),owned=previous.get(effect);
            boolean ours=current!=null&&current==owned&&current.getAmplifier()==0&&current.getDuration()>0&&current.getDuration()<=100;
            if(current==owned&&!ours)clearHiddenArmor(current);
            if(!on){if(ours)player.removeEffect(effect);previous.remove(effect);return;}
            if(current==null||ours){if(current==null||current.getDuration()<=50){MobEffectInstance next=new MobEffectInstance(effect,100,0,true,false,true);player.addEffect(next);}previous.put(effect,player.getEffect(effect));}
            else previous.remove(effect);
        });
        if(previous.isEmpty())issued.remove(player.getUUID());
    }
    private boolean worn(ServerPlayer player,EquipmentSlot slot,String type){var record=artifacts.usable(player.getItemBySlot(slot));return player.isAlive()&&!player.isSpectator()&&record!=null&&record.type.equals(type);}
    private void clearHiddenArmor(MobEffectInstance current){if(current==null)return;var parent=(com.celik.kingdoms.mixin.ArtifactEffectAccessor)(Object)current;var hidden=parent.kingdoms$hiddenEffect();while(hidden!=null){var node=(com.celik.kingdoms.mixin.ArtifactEffectAccessor)(Object)hidden;if(hidden.getAmplifier()==0&&hidden.getDuration()>0&&hidden.getDuration()<=100&&hidden.isAmbient()&&!hidden.isVisible()&&hidden.showIcon()){parent.kingdoms$hiddenEffect(node.kingdoms$hiddenEffect());hidden=parent.kingdoms$hiddenEffect();}else{parent=node;hidden=node.kingdoms$hiddenEffect();}}}
    void leave(ServerPlayer player){Map<Holder<MobEffect>,MobEffectInstance> effects=issued.remove(player.getUUID());if(effects!=null)effects.forEach((effect,owned)->{var current=player.getEffect(effect);if(current!=null&&current==owned){if(current.getAmplifier()==0&&current.getDuration()>0&&current.getDuration()<=100)player.removeEffect(effect);else clearHiddenArmor(current);}});}
}
