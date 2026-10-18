package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class EntityMixin implements AirDropGlowEntity {
 @Inject(method="thunderHit",at=@At("HEAD"),cancellable=true)
 private void lightning(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.LightningBolt bolt,CallbackInfo ci){Entity self=(Entity)(Object)this;if(!Protection.damage(self,self.damageSources().lightningBolt()))ci.cancel();else if(!Protection.flag(level,self.blockPosition(),com.celik.kingdoms.Rules.Flag.FIRE)){self.hurtServer(level,self.damageSources().lightningBolt(),5);ci.cancel();}}
 @Shadow @Final protected static EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;
 @Shadow protected abstract void setSharedFlag(int bit,boolean value);
 public SynchedEntityData.DataValue<Byte> kingdoms$refreshAirDropGlow(){Entity self=(Entity)(Object)this;setSharedFlag(6,self.isCurrentlyGlowing());return SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID,self.getEntityData().get(DATA_SHARED_FLAGS_ID));}
 @Inject(method="isCurrentlyGlowing",at=@At("RETURN"),cancellable=true)
 private void captureGlow(CallbackInfoReturnable<Boolean> cir){if(!cir.getReturnValue()&&KingdomsMod.service!=null&&KingdomsMod.service.airDrops!=null&&KingdomsMod.service.airDrops.glows.glowing((Entity)(Object)this))cir.setReturnValue(true);}
 @Inject(method="remove",at=@At("HEAD"))
 private void removeOwner(Entity.RemovalReason reason,CallbackInfo ci){if(reason.shouldDestroy())Provenance.removed(((Entity)(Object)this).getUUID());}
 @Inject(method="isInvulnerableToBase",at=@At("HEAD"),cancellable=true)
 private void damage(DamageSource source,CallbackInfoReturnable<Boolean> cir){
  if(!Protection.damage((Entity)(Object)this,source))cir.setReturnValue(true);
 }
 @Inject(method="ignoreExplosion",at=@At("HEAD"),cancellable=true)
 private void explosion(Explosion explosion,CallbackInfoReturnable<Boolean> cir){
  Entity self=(Entity)(Object)this;
  if(WarRestrictions.preventBlast(self.level(),net.minecraft.core.BlockPos.containing(explosion.center()),self.blockPosition(),false)){cir.setReturnValue(true);return;}
  if(!Protection.explosionDamage(self.level(),net.minecraft.core.BlockPos.containing(explosion.center()),self.blockPosition(),Protection.owner(explosion.getIndirectSourceEntity())))cir.setReturnValue(true);
 }
}
