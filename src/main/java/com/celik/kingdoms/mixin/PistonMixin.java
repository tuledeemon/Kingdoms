package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(PistonBaseBlock.class)
public abstract class PistonMixin {
 @WrapMethod(method="triggerEvent")
 private boolean move(BlockState state,Level level,BlockPos pos,int event,int data,Operation<Boolean> original){
  if(event<0||event>2)return original.call(state,level,pos,event,data);
  var positions=PistonMoves.prepare(level,pos,state,event);
  if(positions==null)return false;
  try(var approved=PistonMoves.scope(level,positions);var scope=Protection.scope(level,pos,null,null,Protection.Cause.PISTON)){
   return original.call(state,level,pos,event,data);
  }
 }
}
