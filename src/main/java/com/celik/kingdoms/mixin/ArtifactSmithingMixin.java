package com.celik.kingdoms.mixin;
import com.celik.kingdoms.Artifacts;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SmithingMenu.class)
public abstract class ArtifactSmithingMixin extends ItemCombinerMenu {
 protected ArtifactSmithingMixin(){super(null,0,null,null,null);}
 @Inject(method="createResult",at=@At("HEAD"),cancellable=true)
 private void block(CallbackInfo ci){for(int i=0;i<inputSlots.getContainerSize();i++)if(Artifacts.marked(inputSlots.getItem(i))||Artifacts.fixerMarked(inputSlots.getItem(i))){resultSlots.setItem(0,net.minecraft.world.item.ItemStack.EMPTY);ci.cancel();return;}}
}
