package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AnvilMenu.class)
public abstract class ArtifactAnvilMixin extends ItemCombinerMenu {
 @Shadow @Final private DataSlot cost;
 @Shadow private int repairItemCountCost;
 protected ArtifactAnvilMixin(){super(null,0,null,null,null);}
 @Inject(method="createResult",at=@At("HEAD"),cancellable=true)
 private void repair(CallbackInfo ci){ItemStack left=inputSlots.getItem(0),right=inputSlots.getItem(1);if(KingdomsMod.service==null)return;if(!Artifacts.fixerMarked(left)&&!Artifacts.fixerMarked(right))return;resultSlots.setItem(0,KingdomsMod.service.artifacts.repairPreview(left,right));cost.set(KingdomsMod.service.config.artifacts.repairLevels);repairItemCountCost=1;ci.cancel();}
 @Inject(method="createResult",at=@At("RETURN"))
 private void enchanting(CallbackInfo ci){if(KingdomsMod.service!=null&&Artifacts.marked(inputSlots.getItem(0))&&!Artifacts.fixerMarked(inputSlots.getItem(1)))resultSlots.setItem(0,KingdomsMod.service.artifacts.enchantPreview(inputSlots.getItem(0),resultSlots.getItem(0)));}
 @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
 private void canTake(Player p,boolean hasItem,CallbackInfoReturnable<Boolean> cir){if(Artifacts.marked(inputSlots.getItem(0))&&(resultSlots.getItem(0).isEmpty()||p.getInventory().getFreeSlot()<0))cir.setReturnValue(false);}
 @WrapMethod(method="onTake")
 private void take(Player player,ItemStack output,Operation<Void> original){if(KingdomsMod.service==null||!(player instanceof ServerPlayer p)||!Artifacts.marked(inputSlots.getItem(0))||!Artifacts.fixerMarked(inputSlots.getItem(1))){original.call(player,output);return;}Artifacts.Journal journal=KingdomsMod.service.artifacts.prepareRepair(p,inputSlots.getItem(0),inputSlots.getItem(1));original.call(player,output);if(Artifacts.marked(p.containerMenu.getCarried())){p.getInventory().add(p.containerMenu.getCarried());p.containerMenu.setCarried(ItemStack.EMPTY);}KingdomsMod.service.artifacts.finish(p,journal);}
}
