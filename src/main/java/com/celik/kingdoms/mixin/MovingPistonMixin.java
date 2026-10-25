package com.celik.kingdoms.mixin;

import com.celik.kingdoms.PistonMoves;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import java.util.Set;

/** A previously accepted animation must finish even when claim flags disallow new changes. */
@Mixin(PistonMovingBlockEntity.class)
public abstract class MovingPistonMixin {
    @WrapMethod(method="tick")
    private static void finish(Level level,BlockPos pos,BlockState state,PistonMovingBlockEntity entity,Operation<Void> original){
        if(!level.getBlockState(pos).is(Blocks.MOVING_PISTON)||level.getBlockEntity(pos)!=entity){original.call(level,pos,state,entity);return;}
        try(var approved=PistonMoves.scope(level,Set.of(pos.immutable()))){original.call(level,pos,state,entity);}
    }
    @WrapMethod(method="finalTick")
    private void finishEarly(Operation<Void> original){
        BlockEntity entity=(BlockEntity)(Object)this;Level level=entity.getLevel();BlockPos pos=entity.getBlockPos();
        if(level==null||!level.getBlockState(pos).is(Blocks.MOVING_PISTON)||level.getBlockEntity(pos)!=entity){original.call();return;}
        try(var approved=PistonMoves.scope(level,Set.of(pos.immutable()))){original.call();}
    }
}
