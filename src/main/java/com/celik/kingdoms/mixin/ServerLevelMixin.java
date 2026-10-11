package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
 @WrapMethod(method="tickNonPassenger")
 private void entityTick(Entity e,Operation<Void> original){
  Protection.inspectMob(e); if(e.isRemoved())return;
  try(var scope=Protection.scope((ServerLevel)(Object)this,e.blockPosition(),Protection.owner(e),e,Protection.Cause.ENTITY)){original.call(e);}
  Protection.inspectMob(e);
 }
 @WrapMethod(method="tickPassenger")
 private void passengerTick(Entity vehicle,Entity e,Operation<Void> original){Protection.inspectMob(e);if(e.isRemoved())return;try(var scope=Protection.scope((ServerLevel)(Object)this,e.blockPosition(),Protection.owner(e),e,Protection.Cause.ENTITY)){original.call(vehicle,e);}Protection.inspectMob(e);}
 @WrapMethod(method="tickFluid")
 private void fluidTick(BlockPos p,Fluid f,Operation<Void> original){
  try(var scope=Protection.scope((ServerLevel)(Object)this,p,Provenance.blockOwner((ServerLevel)(Object)this,p),null,Protection.Cause.FLUID)){original.call(p,f);}
 }
 @Inject(method="addFreshEntity",at=@At("HEAD"),cancellable=true)
 private void spawn(Entity e,CallbackInfoReturnable<Boolean> cir){if(!Protection.admitSpawn(e))cir.setReturnValue(false);}
}
