package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Level.class)
public abstract class LevelMixin {
 @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
 private void beforeDrops(BlockPos pos,boolean drops,net.minecraft.world.entity.Entity entity,int depth,CallbackInfoReturnable<Boolean> cir){
  if(!Protection.mutation((Level)(Object)this,pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()))cir.setReturnValue(false);
 }
 @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("RETURN"))
 private void provenance(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValueZ()){Provenance.changed((Level)(Object)this,pos,state);if(KingdomsMod.service!=null)KingdomsMod.service.artifacts.blockChanged((Level)(Object)this,pos,state);}}
 @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
 private void guard(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> cir){
  if(!Protection.mutation((Level)(Object)this,pos,state))cir.setReturnValue(false);
 }
}
