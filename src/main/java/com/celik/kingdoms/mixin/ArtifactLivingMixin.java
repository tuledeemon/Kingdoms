package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
@Mixin(LivingEntity.class)
public abstract class ArtifactLivingMixin {
 @Inject(method="startUsingItem",at=@At("HEAD"),cancellable=true)
 private void voidUse(net.minecraft.world.InteractionHand hand,CallbackInfo ci){if((Object)this instanceof net.minecraft.server.level.ServerPlayer player&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.itemDisabled(player))ci.cancel();}
 @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
 private void livingSpearTarget(EquipmentSlot slot,Entity target,float damage,boolean dealsDamage,boolean knockback,boolean dismounts,CallbackInfoReturnable<Boolean> cir){
  LivingEntity self=(LivingEntity)(Object)this;
  if(KingdomsMod.service!=null&&!self.level().isClientSide()&&(!KingdomsMod.service.artifacts.allowsAttack(self.getItemBySlot(slot),target)||self instanceof net.minecraft.server.level.ServerPlayer p&&KingdomsMod.service.artifacts.preventsAttack(p)))cir.setReturnValue(false);
 }
 @WrapMethod(method="hurtServer")
 private boolean hurt(ServerLevel level,DamageSource source,float damage,Operation<Boolean> original){return KingdomsMod.service==null?original.call(level,source,damage):KingdomsMod.service.artifacts.hurt((LivingEntity)(Object)this,level,source,damage,n->original.call(level,source,n));}
 @WrapMethod(method="applyItemBlocking")
 private float block(ServerLevel level,DamageSource source,float incoming,Operation<Float> original){return KingdomsMod.service==null?original.call(level,source,incoming):KingdomsMod.service.artifacts.block((LivingEntity)(Object)this,source,incoming,()->original.call(level,source,incoming));}
}
