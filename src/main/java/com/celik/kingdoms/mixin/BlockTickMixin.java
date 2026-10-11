package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockTickMixin {
 @WrapMethod(method={"tick","randomTick"})
 private void tick(ServerLevel level,BlockPos pos,RandomSource random,Operation<Void> original){
  Protection.Cause cause=level.getBlockState(pos).is(BlockTags.FIRE)?Protection.Cause.FIRE:Protection.Cause.BLOCK;
  if(!level.getFluidState(pos).isEmpty())cause=Protection.Cause.FLUID;
  try(var scope=Protection.scope(level,pos,Provenance.blockOwner(level,pos),null,cause)){original.call(level,pos,random);}
 }
}
