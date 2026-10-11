package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomCores;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HopperBlockEntity.class)
public abstract class CoreHopperMixin {
    @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private static void noCore(Container from,Container to,ItemStack stack,Direction direction,CallbackInfoReturnable<ItemStack> cir){if(KingdomCores.isToken(stack))cir.setReturnValue(stack);}
}
