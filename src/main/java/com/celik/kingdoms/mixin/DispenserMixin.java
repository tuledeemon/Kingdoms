package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin({DispenserBlock.class,DropperBlock.class})
public abstract class DispenserMixin {
 @WrapMethod(method="dispenseFrom")
 private void dispense(ServerLevel level,BlockState state,BlockPos pos,Operation<Void> original){
  if((Object)this instanceof DropperBlock&&!Protection.automation(level,pos,pos.relative(state.getValue(DispenserBlock.FACING))))return;
  try(var scope=Protection.scope(level,pos,null,null,Protection.Cause.AUTOMATION)){original.call(level,state,pos);}
 }
}
