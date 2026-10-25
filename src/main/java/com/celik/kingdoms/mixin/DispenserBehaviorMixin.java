package com.celik.kingdoms.mixin;

import com.celik.kingdoms.Protection;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Check the already-selected item, before consuming it or creating a fallback dropped bucket. */
@Mixin(DispenserBlock.class)
public abstract class DispenserBehaviorMixin {
    @WrapOperation(method="dispenseFrom",at=@At(value="INVOKE",target="Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack dispense(DispenseItemBehavior behavior,BlockSource source,ItemStack stack,Operation<ItemStack> original){
        var target=source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        if(stack.getItem() instanceof BucketItem||stack.is(Items.POWDER_SNOW_BUCKET)){
            if(!Protection.fluid(source.level(),source.pos(),target))return stack;
        }else if((stack.is(Items.BONE_MEAL)||stack.is(Items.HONEYCOMB))&&!Protection.machine(source.level(),source.pos(),target))return stack;
        return original.call(behavior,source,stack);
    }
}
