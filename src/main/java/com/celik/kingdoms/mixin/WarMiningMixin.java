package com.celik.kingdoms.mixin;
import com.celik.kingdoms.WarRestrictions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class WarMiningMixin {
    @Inject(method="getDestroyProgress",at=@At("RETURN"),cancellable=true)
    private void instant(Player player,BlockGetter view,BlockPos pos,CallbackInfoReturnable<Float> cir){
        if(player instanceof ServerPlayer serverPlayer&&WarRestrictions.instantBreak(serverPlayer,serverPlayer.level(),pos,(BlockState)(Object)this))cir.setReturnValue(1.0f);
    }
}
