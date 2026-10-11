package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.LightningBolt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LightningBolt.class)
public abstract class ArtifactLightningMixin {
 @WrapMethod(method="tick")
 private void tick(Operation<Void> original){LightningBolt bolt=(LightningBolt)(Object)this;
  if(bolt.getCause()==null||Provenance.entityOwner(bolt.getUUID())==null){original.call();return;}
  try(var scope=Protection.scope(bolt.level(),bolt.blockPosition(),bolt.getCause().getUUID(),bolt.getCause(),Protection.Cause.PLAYER)){original.call();}
 }
}
