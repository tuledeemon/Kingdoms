package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.item.*;
import net.minecraft.server.level.*;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ItemStack.class)
public abstract class ArtifactItemMixin {
 @WrapMethod(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V")
 private void durability(int amount,ServerLevel level,ServerPlayer player,Consumer<Item> onBreak,Operation<Void> original){
  ItemStack stack=(ItemStack)(Object)this;
  if(KingdomsMod.service!=null&&KingdomsMod.service.artifacts.freeDurability(stack,level))return;
  ItemStack before=Artifacts.marked(stack)?stack.copy():ItemStack.EMPTY;
  original.call(amount,level,player,onBreak);
  if(stack.isEmpty()&&!before.isEmpty()&&KingdomsMod.service!=null)KingdomsMod.service.artifacts.destroyed(before);
 }
}
