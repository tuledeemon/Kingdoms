package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class ArtifactRepairRecipeMixin {
    private static boolean blocked(CraftingInput input){
        int artifacts=0;
        for(ItemStack stack:input.items()){if(Artifacts.fixerMarked(stack))return true;if(Artifacts.marked(stack))artifacts++;}
        return artifacts>1;
    }
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true)
    private void matches(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir){if(blocked(input))cir.setReturnValue(false);}
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"),cancellable=true)
    private void preserve(CraftingInput input,CallbackInfoReturnable<ItemStack> cir){
        if(blocked(input)){cir.setReturnValue(ItemStack.EMPTY);return;}
        for(ItemStack stack:input.items())if(Artifacts.marked(stack)){
            cir.setReturnValue(KingdomsMod.service==null?ItemStack.EMPTY:KingdomsMod.service.artifacts.enchantPreview(stack,cir.getReturnValue()));return;
        }
    }
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"))
    private void prepare(CraftingInput input,CallbackInfoReturnable<ItemStack> cir){if(KingdomsMod.service!=null)KingdomsMod.service.artifacts.prepareCombination(input.items().toArray(ItemStack[]::new));}
}
