package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
 @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
 private void potion(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){
  LivingEntity self=(LivingEntity)(Object)this;
  if(!effect.getEffect().value().isBeneficial()&&source!=null&&!Protection.damage(self,self.damageSources().indirectMagic(source,source)))cir.setReturnValue(false);
 }
}
