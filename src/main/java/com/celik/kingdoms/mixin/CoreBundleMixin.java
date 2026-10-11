package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomCores;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BundleContents.class)
public abstract class CoreBundleMixin {
    @Inject(method="canItemBeInBundle",at=@At("HEAD"),cancellable=true)
    private static void noCore(ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(KingdomCores.isToken(stack))cir.setReturnValue(false);}
}
