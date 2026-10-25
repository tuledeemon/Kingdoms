package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Inventory.class)
public abstract class ArtifactInventoryMixin {
 @Shadow @Final public Player player;
 @Inject(method="setItem",at=@At("HEAD"),cancellable=true)
 private void crownSlot(int slot,ItemStack stack,CallbackInfo ci){if(slot==39&&player instanceof ServerPlayer p&&KingdomsMod.service!=null&&!KingdomsMod.service.artifacts.crown.mayReplace(p,stack))ci.cancel();}
 @Inject(method="removeItem",at=@At("HEAD"),cancellable=true)
 private void crownRemoval(int slot,int count,CallbackInfoReturnable<ItemStack> ci){if(slot==39&&player instanceof ServerPlayer p&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.crown.locked(p))ci.setReturnValue(ItemStack.EMPTY);}
 @Inject(method="removeItemNoUpdate",at=@At("HEAD"),cancellable=true)
 private void crownTake(int slot,CallbackInfoReturnable<ItemStack> ci){if(slot==39&&player instanceof ServerPlayer p&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.crown.locked(p))ci.setReturnValue(ItemStack.EMPTY);}
 @WrapMethod(method="dropAll")
 private void death(Operation<Void> original){if(KingdomsMod.service==null||!(player instanceof ServerPlayer p)){original.call();return;}List<ItemStack> kept=KingdomsMod.service.artifacts.keepOnDeath(p);try{original.call();}finally{KingdomsMod.service.artifacts.returnAfterDeath(p,kept);}}
}
