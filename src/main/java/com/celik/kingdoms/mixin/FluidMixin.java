package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(FlowingFluid.class)
public abstract class FluidMixin {
 @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
 private void spread(LevelAccessor access,BlockPos pos,BlockState state,Direction direction,FluidState target,CallbackInfo ci){
  if(access instanceof Level level&&!Protection.fluid(level,pos.relative(direction.getOpposite()),pos))ci.cancel();
 }
}
