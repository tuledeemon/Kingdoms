package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
 @Inject(method="ejectItems",at=@At("HEAD"),cancellable=true)
 private static void push(Level level,BlockPos pos,HopperBlockEntity hopper,CallbackInfoReturnable<Boolean> cir){
  if(!Protection.automation(level,pos,pos.relative(hopper.getBlockState().getValue(HopperBlock.FACING))))cir.setReturnValue(false);
 }
 @Inject(method="suckInItems",at=@At("HEAD"),cancellable=true)
 private static void pull(Level level,Hopper hopper,CallbackInfoReturnable<Boolean> cir){
  BlockPos pos=BlockPos.containing(hopper.getLevelX(),hopper.getLevelY(),hopper.getLevelZ());
  if(!Protection.automation(level,pos,pos.above()))cir.setReturnValue(false);
 }
}
