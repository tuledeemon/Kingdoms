package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(FireBlock.class)
public abstract class FireMixin {
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void stopDisabledFire(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci){if(!Protection.flag(level,pos,Rules.Flag.FIRE))ci.cancel();}
 @Inject(method="checkBurnOut",at=@At("HEAD"),cancellable=true)
 private void beforeBurn(Level level,BlockPos pos,int chance,RandomSource random,int age,CallbackInfo ci){
  var context=Protection.current();BlockPos from=context==null?pos:context.origin();
  if(!Protection.environmental(level,from,pos,Rules.Flag.FIRE))ci.cancel();
 }
}
