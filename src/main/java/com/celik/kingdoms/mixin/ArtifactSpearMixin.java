package com.celik.kingdoms.mixin;

import com.celik.kingdoms.KingdomsMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiercingWeapon.class)
public abstract class ArtifactSpearMixin {
    // Both jabs and charged spear contact use this filter. Skip props before hit bookkeeping.
    @Inject(method="canHitEntity",at=@At("HEAD"),cancellable=true)
    private static void livingTarget(Entity attacker,Entity target,CallbackInfoReturnable<Boolean> cir){
        if(KingdomsMod.service==null||!(attacker instanceof LivingEntity living)||attacker.level().isClientSide())return;
        var stack=living.isUsingItem()?living.getUseItem():living.getWeaponItem();
        if(!KingdomsMod.service.artifacts.allowsAttack(stack,target))cir.setReturnValue(false);
    }
}
