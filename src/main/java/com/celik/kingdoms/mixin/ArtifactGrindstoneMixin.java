package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GrindstoneMenu.class)
public abstract class ArtifactGrindstoneMixin {
 @Shadow @Final private Container repairSlots;
 @Shadow @Final private Container resultSlots;
 @Inject(method="createResult",at=@At("HEAD"),cancellable=true)
 private void block(CallbackInfo ci){for(int i=0;i<2;i++)if(Artifacts.fixerMarked(repairSlots.getItem(i))){resultSlots.setItem(0,ItemStack.EMPTY);ci.cancel();return;}}
 @Inject(method="createResult",at=@At("RETURN"))
 private void result(CallbackInfo ci){if(KingdomsMod.service!=null)for(int i=0;i<2;i++)if(Artifacts.marked(repairSlots.getItem(i))){resultSlots.setItem(0,KingdomsMod.service.artifacts.enchantPreview(repairSlots.getItem(i),resultSlots.getItem(0)));return;}}
}
