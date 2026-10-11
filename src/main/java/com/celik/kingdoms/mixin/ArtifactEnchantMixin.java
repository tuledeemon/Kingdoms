package com.celik.kingdoms.mixin;
import com.celik.kingdoms.Artifacts;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentMenu.class)
public abstract class ArtifactEnchantMixin {
 @Shadow @Final private Container enchantSlots;
 @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
 private void enchanting(Player player,int button,CallbackInfoReturnable<Boolean> cir){if(Artifacts.fixerMarked(enchantSlots.getItem(0)))cir.setReturnValue(false);}
}
