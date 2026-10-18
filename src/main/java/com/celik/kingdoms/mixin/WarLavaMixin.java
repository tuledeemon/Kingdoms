package com.celik.kingdoms.mixin;
import com.celik.kingdoms.WarRestrictions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LavaFluid.class)
public abstract class WarLavaMixin {
    @WrapMethod(method="spreadTo")
    private void cast(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState fluid,Operation<Void> original){
        try(var scope=WarRestrictions.casting()){original.call(level,pos,state,direction,fluid);}
    }
}
