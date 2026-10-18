package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class WarPearlMixin {
    @Inject(method="onHit",at=@At("HEAD"),cancellable=true)
    private void noTeleport(HitResult hit,CallbackInfo ci){
        ThrownEnderpearl pearl=(ThrownEnderpearl)(Object)this;
        if(hit.getType()!=HitResult.Type.MISS&&(WarRestrictions.pearlBlocked(pearl.getOwner(),pearl.level(),BlockPos.containing(hit.getLocation()))||WarRestrictions.pearlBlocked(pearl.getOwner(),pearl.level(),pearl.blockPosition()))){
            pearl.discard();ci.cancel();
            if(pearl.getOwner() instanceof ServerPlayer player&&player.connection!=null)Ui.tell(player,"Ender pearl teleporting is disabled by the administrator here.",true);
        }
    }
}
