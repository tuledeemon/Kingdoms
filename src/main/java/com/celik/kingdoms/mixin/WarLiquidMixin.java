package com.celik.kingdoms.mixin;
import com.celik.kingdoms.WarRestrictions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LiquidBlock.class)
public abstract class WarLiquidMixin {
    @WrapMethod(method="shouldSpreadLiquid")
    private boolean cast(Level level,BlockPos pos,BlockState state,Operation<Boolean> original){
        try(var scope=WarRestrictions.casting()){return original.call(level,pos,state);}
    }
}
