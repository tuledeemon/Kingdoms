package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectileWeaponItem.class)
public abstract class ArtifactBowMixin {
    @ModifyExpressionValue(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processProjectileCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;I)I"))
    private static int count(int original,ItemStack weapon,ItemStack ammo,LivingEntity shooter){return KingdomsMod.service!=null&&KingdomsMod.service.artifacts.archerBow(weapon)?2:original;}
    @ModifyExpressionValue(method="shoot",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processProjectileSpread(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;F)F"))
    private float spread(float original,net.minecraft.server.level.ServerLevel level,LivingEntity shooter,net.minecraft.world.InteractionHand hand,ItemStack weapon,java.util.List<ItemStack> ammo,float speed,float inaccuracy,boolean critical,LivingEntity target){return KingdomsMod.service!=null&&KingdomsMod.service.artifacts.archerBow(weapon)?Math.max(original,2.5f):original;}
}
