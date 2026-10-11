package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** The spear's built-in enchantment is restored after grinding and must never yield repeatable XP. */
@Mixin(targets="net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class ArtifactGrindstoneXpMixin {
 @WrapMethod(method="getExperienceFromItem")
 private int experience(ItemStack stack,Operation<Integer> original){
  if(KingdomsMod.service==null||!KingdomsMod.service.artifacts.fixedEnchantment(stack))return original.call(stack);
  ItemStack copy=stack.copy();EnchantmentHelper.updateEnchantments(copy,m->m.removeIf(h->h.is(Enchantments.WIND_BURST)));
  return original.call(copy);
 }
}
