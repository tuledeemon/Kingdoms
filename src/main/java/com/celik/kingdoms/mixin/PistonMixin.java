package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(PistonBaseBlock.class)
public abstract class PistonMixin {
 @WrapMethod(method="moveBlocks")
 private boolean move(Level level,BlockPos pos,Direction direction,boolean extending,Operation<Boolean> original){
  if(!Protection.flag(level,pos,Rules.Flag.PISTONS))return false;
  PistonStructureResolver resolver=new PistonStructureResolver(level,pos,direction,extending);
  if(!resolver.resolve())return false;
  Direction motion=extending?direction:direction.getOpposite();
  if(!Protection.environmental(level,pos,pos.relative(direction),Rules.Flag.PISTONS))return false;
  for(BlockPos p:resolver.getToPush())if(!Protection.environmental(level,pos,p,Rules.Flag.PISTONS)||!Protection.environmental(level,pos,p.relative(motion),Rules.Flag.PISTONS))return false;
  for(BlockPos p:resolver.getToDestroy())if(!Protection.environmental(level,pos,p,Rules.Flag.PISTONS))return false;
  try(var scope=Protection.scope(level,pos,null,null,Protection.Cause.PISTON)){return original.call(level,pos,direction,extending);}
 }
}
