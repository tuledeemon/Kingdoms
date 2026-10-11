package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PhysicalInteractionMixin {
 @WrapMethod(method="entityInside")
 private void physical(Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier applier,boolean precise,Operation<Void> original){
  var block=level.getBlockState(pos).getBlock();
  if(block instanceof net.minecraft.world.level.block.BasePressurePlateBlock||block instanceof net.minecraft.world.level.block.TripWireBlock||block instanceof net.minecraft.world.level.block.DetectorRailBlock){
   var actor=Protection.owner(entity);
   if(actor!=null&&!Protection.permitted(level,pos,actor,Rules.Permission.INTERACT))return;
   if(actor==null&&!Protection.flag(level,pos,Rules.Flag.MOB_GRIEFING))return;
  }
  try(var scope=Protection.scope(level,entity.blockPosition(),Protection.owner(entity),entity,entity instanceof Player?Protection.Cause.PLAYER:Protection.Cause.ENTITY)){original.call(level,pos,entity,applier,precise);}
 }
}
