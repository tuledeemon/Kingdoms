package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class ArtifactEntityMixin {
 @Inject(method="hurtServer",at=@At("RETURN"))
 private void destroyed(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){ItemEntity self=(ItemEntity)(Object)this;if(self.isRemoved()&&KingdomsMod.service!=null)KingdomsMod.service.artifacts.destroyed(self.getItem());}
 @Inject(method="tick",at=@At("RETURN"))
 private void expiry(CallbackInfo ci){ItemEntity self=(ItemEntity)(Object)this;if(KingdomsMod.service!=null&&self.isRemoved()&&(self.getAge()>=6000||self.getY()<self.level().getMinY()-64))KingdomsMod.service.artifacts.destroyed(self.getItem());}
}
