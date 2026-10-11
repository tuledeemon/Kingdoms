package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
public abstract class MobMixin {
 @Inject(method="checkDespawn",at=@At("HEAD"),cancellable=true)
 private void retain(CallbackInfo ci){
  Mob mob=(Mob)(Object)this;var s=Protection.service(mob.level());if(s==null)return;
  var k=s.land(mob.level(),mob.blockPosition());if(k!=null&&k.allowedMobs.contains(mob.getUUID()))ci.cancel();
 }
}
