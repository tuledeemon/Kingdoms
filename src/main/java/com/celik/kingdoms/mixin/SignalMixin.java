package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.level.*;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SignalGetter.class)
public interface SignalMixin {
 @Inject(method={"getSignal","getDirectSignal"},at=@At("HEAD"),cancellable=true)
 private void borderSignal(BlockPos pos,Direction direction,CallbackInfoReturnable<Integer> cir){
  if((Object)this instanceof Level level&&!Protection.sameLand(level,pos,pos.relative(direction.getOpposite()))&&!Protection.environmental(level,pos,pos.relative(direction.getOpposite()),Rules.Flag.AUTOMATION))cir.setReturnValue(0);
 }
}
